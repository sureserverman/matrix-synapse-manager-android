package com.matrix.synapse.security

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting per-server access tokens and current user identity.
 *
 * Scope: access tokens and user ID only. Passwords are NEVER persisted — callers
 * must not pass raw credentials here. There is intentionally no
 * savePassword method on this interface.
 */
interface SecureTokenStore {
    /**
     * Eagerly initialises the store, running any pending one-time migration. Safe to call off the
     * main thread at startup so existing data is re-encrypted before the first token read.
     */
    fun warmUp() {}

    /** Emits the current access token for [serverId], or null if none stored. */
    fun accessTokenFlow(serverId: String): Flow<String?>

    /** Persists the [accessToken] for [serverId], overwriting any existing value. */
    suspend fun saveToken(serverId: String, accessToken: String)

    /** Persists the logged-in [userId] for [serverId]. Call after successful login. */
    suspend fun saveUserId(serverId: String, userId: String)

    /** Emits the current user ID for [serverId], or null if none stored. */
    fun currentUserIdFlow(serverId: String): Flow<String?>

    /** Removes all stored tokens and user ID for [serverId]. */
    suspend fun clearTokens(serverId: String)

    /** Emits the current OAuth refresh token for [serverId], or null if none stored. */
    fun refreshTokenFlow(serverId: String): Flow<String?>

    /** Persists the refresh [token] for [serverId]. */
    suspend fun saveRefreshToken(serverId: String, token: String)

    /** Emits the registered MAS OAuth client_id for [serverId], or null if not registered. */
    fun oauthClientIdFlow(serverId: String): Flow<String?>

    /** Persists the OAuth [clientId] for [serverId]. */
    suspend fun saveOAuthClientId(serverId: String, clientId: String)

    /** Emits the access-token issued-at epoch-seconds for [serverId], or null if unknown. */
    fun tokenIssuedAtFlow(serverId: String): Flow<Long?>

    /** Persists the issued-at [epochSeconds] timestamp for [serverId]. */
    suspend fun saveTokenIssuedAt(serverId: String, epochSeconds: Long)
}
