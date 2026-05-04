package com.matrix.synapse.network.auth

import com.matrix.synapse.network.ActiveTokenHolder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator that, on a 401 with a stale Bearer token, tries one
 * refresh against MAS and retries the request once with the new token.
 *
 * Activation is per-server: callers must register the active server's
 * tokenEndpoint and serverId via [registerServer] before requests can be
 * refreshed. Without registration this is a no-op (returns null = give up).
 */
class MasReauthenticator(
    private val refresher: MasTokenRefresher,
    private val activeTokenHolder: ActiveTokenHolder,
) : Authenticator {

    @Volatile private var serverId: String? = null
    @Volatile private var tokenEndpoint: String? = null

    fun registerServer(serverId: String, tokenEndpoint: String) {
        this.serverId = serverId
        this.tokenEndpoint = tokenEndpoint
    }

    fun clearServer() {
        this.serverId = null
        this.tokenEndpoint = null
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once per request chain.
        if (response.priorResponse != null) return null
        val sid = serverId ?: return null
        val endpoint = tokenEndpoint ?: return null
        // Only retry if the failed request actually carried an Authorization header
        // matching the current active token (avoid retry storms on unauth endpoints).
        val currentToken = activeTokenHolder.get() ?: return null
        val sentHeader = response.request.header("Authorization") ?: return null
        if (sentHeader != "Bearer $currentToken") return null

        val newToken = try {
            runBlocking { refresher.refresh(sid, endpoint) }
        } catch (e: Throwable) {
            return null
        }
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
