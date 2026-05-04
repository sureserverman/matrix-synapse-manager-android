package com.matrix.synapse.network.auth

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.security.SecureTokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasTokenRefresherTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var activeTokenHolder: ActiveTokenHolder
    private lateinit var refresher: MasTokenRefresher

    private val json = Json { ignoreUnknownKeys = true }
    private val fixedNow = 1234L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = FakeTokenStore()
        activeTokenHolder = ActiveTokenHolder()
        refresher = MasTokenRefresher(
            client = OkHttpClient(),
            tokenStore = tokenStore,
            activeTokenHolder = activeTokenHolder,
            json = json,
            now = { fixedNow },
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun refreshes_and_persists_new_token() = runTest {
        tokenStore.setRefreshToken("s1", "old-r")
        tokenStore.setOAuthClientId("s1", "c1")
        tokenStore.saveToken("s1", "old-a")

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token":"new-a","refresh_token":"new-r","expires_in":3600,"scope":"openid"}""")
        )

        val result = refresher.refresh("s1", server.url("/token").toString())

        assertEquals("new-a", result)
        assertEquals("new-a", tokenStore.accessTokenFlow("s1").first())
        assertEquals("new-r", tokenStore.refreshTokenFlow("s1").first())
        assertEquals(fixedNow, tokenStore.tokenIssuedAtFlow("s1").first())
        assertEquals("new-a", activeTokenHolder.get())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun single_flight_collapses_concurrent_callers() = runTest {
        tokenStore.setRefreshToken("s1", "old-r")
        tokenStore.setOAuthClientId("s1", "c1")
        tokenStore.saveToken("s1", "old-a")

        // Only one response enqueued — single-flight means only one HTTP call is made
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token":"new-a","refresh_token":"new-r","expires_in":3600,"scope":"openid"}""")
        )

        val results = coroutineScope {
            (1..5).map {
                async { refresher.refresh("s1", server.url("/token").toString()) }
            }.map { it.await() }
        }

        results.forEach { assertEquals("new-a", it) }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun propagates_typed_failure_on_4xx() = runTest {
        tokenStore.setRefreshToken("s1", "old-r")
        tokenStore.setOAuthClientId("s1", "c1")

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"invalid_grant"}""")
        )

        val ex = runCatching {
            refresher.refresh("s1", server.url("/token").toString())
        }.exceptionOrNull()

        assertTrue("Expected MasTokenRefreshException, got: ${ex?.javaClass}", ex is MasTokenRefreshException)
        val tokenEx = ex as MasTokenRefreshException
        assertEquals(400, tokenEx.code)
        assertTrue(
            "Expected body to contain invalid_grant, got: ${tokenEx.responseBody}",
            tokenEx.responseBody.contains("invalid_grant"),
        )
    }

    @Test
    fun throws_when_refresh_token_missing() = runTest {
        tokenStore.setOAuthClientId("s1", "c1")
        // No refresh token set

        val ex = runCatching {
            refresher.refresh("s1", server.url("/token").toString())
        }.exceptionOrNull()

        assertTrue("Expected MasTokenRefreshException, got: ${ex?.javaClass}", ex is MasTokenRefreshException)
        assertEquals(0, server.requestCount)
    }
}

// ---------------------------------------------------------------------------
// Minimal fake — backed by MutableStateFlow maps; no Android/DataStore deps.
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

    fun setRefreshToken(serverId: String, token: String) { refreshFlow(serverId).value = token }
    fun setOAuthClientId(serverId: String, id: String) { clientIdFlow(serverId).value = id }

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
