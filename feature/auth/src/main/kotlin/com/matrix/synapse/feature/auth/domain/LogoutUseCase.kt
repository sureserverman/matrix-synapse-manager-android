package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.auth.MasDiscoveryResult
import com.matrix.synapse.network.auth.MasDiscoveryService
import com.matrix.synapse.security.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

/**
 * Logout flow:
 * - For OAuth servers (MAS): POST refresh_token to MAS revocation_endpoint per RFC 7009,
 *   then clear local state.
 * - For password servers: just clear local state.
 *
 * Local clearing happens regardless of revocation outcome — we never strand the user
 * with a token they can't escape. Revocation failure is surfaced via the result type
 * for telemetry but does not block the logout.
 */
class LogoutUseCase @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
    private val discovery: MasDiscoveryService,
    @Named("refresh") private val client: OkHttpClient,
) {

    sealed interface LogoutResult {
        data object Success : LogoutResult
        data class RevocationFailed(val cause: Throwable) : LogoutResult
    }

    suspend fun logout(serverId: String, homeserverUrl: String): LogoutResult {
        val refreshToken = tokenStore.refreshTokenFlow(serverId).first()
        val clientId = tokenStore.oauthClientIdFlow(serverId).first()
        val revocationOutcome = if (refreshToken != null && clientId != null) {
            tryRevoke(homeserverUrl, refreshToken, clientId)
        } else null

        tokenStore.clearTokens(serverId)
        activeTokenHolder.set(null)

        return revocationOutcome?.exceptionOrNull()
            ?.let { LogoutResult.RevocationFailed(it) }
            ?: LogoutResult.Success
    }

    private suspend fun tryRevoke(
        homeserverUrl: String,
        refreshToken: String,
        clientId: String,
    ): Result<Unit> = runCatching {
        val metadata = when (val r = discovery.discover(homeserverUrl)) {
            is MasDiscoveryResult.Mas -> r.metadata
            else -> return@runCatching // not a MAS server, nothing to revoke
        }
        val endpoint = metadata.revocationEndpoint
            ?: return@runCatching // metadata didn't advertise revocation
        val body = FormBody.Builder()
            .add("token", refreshToken)
            .add("token_type_hint", "refresh_token")
            .add("client_id", clientId)
            .build()
        val req = Request.Builder()
            .url(endpoint)
            .post(body)
            .header("Accept", "application/json")
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Revocation HTTP ${resp.code}")
            }
        }
    }
}
