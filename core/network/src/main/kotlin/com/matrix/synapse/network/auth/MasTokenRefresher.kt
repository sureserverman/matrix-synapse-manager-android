package com.matrix.synapse.network.auth

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.security.SecureTokenStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Refreshes a MAS access token using the stored refresh_token + client_id.
 * Single-flight per instance: concurrent callers share one in-flight HTTP refresh
 * via a shared [CompletableDeferred]. Only the first caller hits the network;
 * subsequent concurrent callers await the same result.
 *
 * Lives in :core:network so the OkHttp Authenticator (also here) can use it
 * without crossing module boundaries.
 */
class MasTokenRefresher(
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
    private val json: Json,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    private val mutex = Mutex()

    // Holds the in-flight refresh deferred; null when no refresh is running.
    @Volatile private var inFlight: CompletableDeferred<String>? = null

    /**
     * Refreshes the access token for [serverId] using the stored refresh token,
     * client_id, and the [tokenEndpoint] from the homeserver's auth metadata.
     * Returns the new access token; the new refresh token (if any) and issued-at
     * are persisted as a side-effect.
     *
     * Concurrent callers share the same in-flight HTTP request.
     * Throws [MasTokenRefreshException] on HTTP failure or if prerequisites are missing.
     */
    suspend fun refresh(serverId: String, tokenEndpoint: String): String {
        // Fast-path: join an already-running refresh.
        mutex.withLock { inFlight }?.let { return it.await() }

        val deferred = CompletableDeferred<String>()
        // Register so other callers can join.
        mutex.withLock { inFlight = deferred }

        return try {
            val result = doRefresh(serverId, tokenEndpoint)
            deferred.complete(result)
            result
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            mutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }

    private suspend fun doRefresh(serverId: String, tokenEndpoint: String): String {
        val refreshToken = tokenStore.refreshTokenFlow(serverId).first()
            ?: throw MasTokenRefreshException(0, "No refresh token stored for server $serverId")
        val clientId = tokenStore.oauthClientIdFlow(serverId).first()
            ?: throw MasTokenRefreshException(0, "No OAuth client_id stored for server $serverId")

        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", clientId)
            .build()
        val req = Request.Builder()
            .url(tokenEndpoint)
            .post(body)
            .header("Accept", "application/json")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MasTokenRefreshException(response.code, text)
                }
                val parsed = json.decodeFromString(RefreshResponse.serializer(), text)
                tokenStore.saveToken(serverId, parsed.accessToken)
                parsed.refreshToken?.let { tokenStore.saveRefreshToken(serverId, it) }
                tokenStore.saveTokenIssuedAt(serverId, now())
                activeTokenHolder.set(parsed.accessToken)
                parsed.accessToken
            }
        }
    }

    @Serializable
    private data class RefreshResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("scope") val scope: String? = null,
        @SerialName("token_type") val tokenType: String? = null,
    )
}

class MasTokenRefreshException(val code: Int, val responseBody: String) :
    RuntimeException("Refresh failed: HTTP $code — $responseBody")
