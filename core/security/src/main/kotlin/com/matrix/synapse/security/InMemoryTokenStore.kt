package com.matrix.synapse.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory implementation of [SecureTokenStore] for use in unit tests.
 * Not suitable for production — tokens are not encrypted and not persisted.
 */
class InMemoryTokenStore : SecureTokenStore {

    private val tokens = MutableStateFlow<Map<String, String>>(emptyMap())
    private val userIds = MutableStateFlow<Map<String, String>>(emptyMap())
    private val refreshTokens = MutableStateFlow<Map<String, String>>(emptyMap())
    private val oauthClientIds = MutableStateFlow<Map<String, String>>(emptyMap())
    private val issuedAts = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun accessTokenFlow(serverId: String): Flow<String?> =
        tokens.map { it[serverId] }

    override suspend fun saveToken(serverId: String, accessToken: String) {
        tokens.value = tokens.value + (serverId to accessToken)
    }

    override suspend fun saveUserId(serverId: String, userId: String) {
        userIds.value = userIds.value + (serverId to userId)
    }

    override fun currentUserIdFlow(serverId: String): Flow<String?> =
        userIds.map { it[serverId] }

    override suspend fun clearTokens(serverId: String) {
        tokens.value = tokens.value - serverId
        userIds.value = userIds.value - serverId
        refreshTokens.value = refreshTokens.value - serverId
        oauthClientIds.value = oauthClientIds.value - serverId
        issuedAts.value = issuedAts.value - serverId
    }

    override fun refreshTokenFlow(serverId: String): Flow<String?> =
        refreshTokens.map { it[serverId] }

    override suspend fun saveRefreshToken(serverId: String, token: String) {
        refreshTokens.value = refreshTokens.value + (serverId to token)
    }

    override fun oauthClientIdFlow(serverId: String): Flow<String?> =
        oauthClientIds.map { it[serverId] }

    override suspend fun saveOAuthClientId(serverId: String, clientId: String) {
        oauthClientIds.value = oauthClientIds.value + (serverId to clientId)
    }

    override fun tokenIssuedAtFlow(serverId: String): Flow<Long?> =
        issuedAts.map { it[serverId] }

    override suspend fun saveTokenIssuedAt(serverId: String, epochSeconds: Long) {
        issuedAts.value = issuedAts.value + (serverId to epochSeconds)
    }
}
