package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.auth.AuthMetadata
import com.matrix.synapse.network.auth.MasDiscoveryResult
import com.matrix.synapse.network.auth.MasDiscoveryService
import com.matrix.synapse.security.SecureTokenStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

class LogoutUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var activeTokenHolder: ActiveTokenHolder
    private lateinit var discovery: MasDiscoveryService
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        tokenStore = FakeTokenStore()
        activeTokenHolder = ActiveTokenHolder()
        discovery = mockk()

        useCase = LogoutUseCase(
            tokenStore = tokenStore,
            activeTokenHolder = activeTokenHolder,
            discovery = discovery,
            client = OkHttpClient(),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private fun masMetadata(revocationUrl: String) = AuthMetadata(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        revocationEndpoint = revocationUrl,
    )

    @Test
    fun revokes_refresh_token_then_clears_for_oauth_server() = runTest {
        coEvery { discovery.discover(any()) } returns
            MasDiscoveryResult.Mas(masMetadata(server.url("/revoke").toString()))

        tokenStore.saveRefreshToken("s1", "rt")
        tokenStore.saveOAuthClientId("s1", "cid")
        tokenStore.saveToken("s1", "at")
        tokenStore.saveUserId("s1", "@user:example.com")
        activeTokenHolder.set("at")

        server.enqueue(MockResponse().setResponseCode(200))

        val result = useCase.logout("s1", "https://matrix.example.com")

        // Server received revocation POST
        assertEquals(1, server.requestCount)
        val recorded = server.takeRequest()
        assertEquals("/revoke", recorded.path)
        val body = URLDecoder.decode(recorded.body.readUtf8(), "UTF-8")
        assertTrue("Expected token in body, got: $body", body.contains("token=rt"))
        assertTrue("Expected token_type_hint in body, got: $body", body.contains("token_type_hint=refresh_token"))
        assertTrue("Expected client_id in body, got: $body", body.contains("client_id=cid"))

        // Local state cleared
        assertNull(tokenStore.accessTokenFlow("s1").first())
        assertNull(tokenStore.refreshTokenFlow("s1").first())
        assertNull(tokenStore.oauthClientIdFlow("s1").first())
        assertNull(activeTokenHolder.get())

        assertTrue("Expected Success, got: $result", result is LogoutUseCase.LogoutResult.Success)
    }

    @Test
    fun clears_state_locally_for_password_server() = runTest {
        coEvery { discovery.discover(any()) } returns MasDiscoveryResult.NotMas

        tokenStore.saveToken("s1", "at")
        tokenStore.saveUserId("s1", "@user:example.com")
        activeTokenHolder.set("at")

        val result = useCase.logout("s1", "https://matrix.example.com")

        assertEquals(0, server.requestCount)
        assertNull(tokenStore.accessTokenFlow("s1").first())
        assertNull(activeTokenHolder.get())
        assertTrue("Expected Success, got: $result", result is LogoutUseCase.LogoutResult.Success)
    }

    @Test
    fun clears_local_state_even_if_revocation_fails() = runTest {
        coEvery { discovery.discover(any()) } returns
            MasDiscoveryResult.Mas(masMetadata(server.url("/revoke").toString()))

        tokenStore.saveRefreshToken("s1", "rt")
        tokenStore.saveOAuthClientId("s1", "cid")
        tokenStore.saveToken("s1", "at")
        activeTokenHolder.set("at")

        server.enqueue(MockResponse().setResponseCode(500))

        val result = useCase.logout("s1", "https://matrix.example.com")

        // Local state still cleared
        assertNull(tokenStore.accessTokenFlow("s1").first())
        assertNull(tokenStore.refreshTokenFlow("s1").first())
        assertNull(activeTokenHolder.get())

        assertTrue(
            "Expected RevocationFailed, got: $result",
            result is LogoutUseCase.LogoutResult.RevocationFailed,
        )
    }
}

// ---------------------------------------------------------------------------
// Minimal fake token store
// ---------------------------------------------------------------------------
private class FakeTokenStore : SecureTokenStore {
    private val accessTokens = mutableMapOf<String, MutableStateFlow<String?>>()
    private val refreshTokens = mutableMapOf<String, MutableStateFlow<String?>>()
    private val clientIds = mutableMapOf<String, MutableStateFlow<String?>>()
    private val issuedAts = mutableMapOf<String, MutableStateFlow<Long?>>()
    private val userIds = mutableMapOf<String, MutableStateFlow<String?>>()

    private fun accessFlow(id: String) = accessTokens.getOrPut(id) { MutableStateFlow(null) }
    private fun refreshFlow(id: String) = refreshTokens.getOrPut(id) { MutableStateFlow(null) }
    private fun clientIdFlow(id: String) = clientIds.getOrPut(id) { MutableStateFlow(null) }
    private fun issuedAtFlow(id: String) = issuedAts.getOrPut(id) { MutableStateFlow(null) }
    private fun userIdFlow(id: String) = userIds.getOrPut(id) { MutableStateFlow(null) }

    override fun accessTokenFlow(serverId: String): Flow<String?> = accessFlow(serverId)
    override suspend fun saveToken(serverId: String, accessToken: String) { accessFlow(serverId).value = accessToken }
    override fun refreshTokenFlow(serverId: String): Flow<String?> = refreshFlow(serverId)
    override suspend fun saveRefreshToken(serverId: String, token: String) { refreshFlow(serverId).value = token }
    override fun oauthClientIdFlow(serverId: String): Flow<String?> = clientIdFlow(serverId)
    override suspend fun saveOAuthClientId(serverId: String, clientId: String) { clientIdFlow(serverId).value = clientId }
    override fun tokenIssuedAtFlow(serverId: String): Flow<Long?> = issuedAtFlow(serverId)
    override suspend fun saveTokenIssuedAt(serverId: String, epochSeconds: Long) { issuedAtFlow(serverId).value = epochSeconds }
    override fun currentUserIdFlow(serverId: String): Flow<String?> = userIdFlow(serverId)
    override suspend fun saveUserId(serverId: String, userId: String) { userIdFlow(serverId).value = userId }
    override suspend fun clearTokens(serverId: String) {
        accessFlow(serverId).value = null
        refreshFlow(serverId).value = null
        clientIdFlow(serverId).value = null
        issuedAtFlow(serverId).value = null
        userIdFlow(serverId).value = null
    }
}
