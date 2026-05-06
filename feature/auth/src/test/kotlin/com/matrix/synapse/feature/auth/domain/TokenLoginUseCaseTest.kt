package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.AuthHeaderInterceptor
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.InMemoryTokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenLoginUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var holder: ActiveTokenHolder
    private lateinit var useCase: TokenLoginUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = InMemoryTokenStore()
        holder = ActiveTokenHolder()
        // Mirror the production OkHttp setup so the auth header is actually attached
        // by the interceptor — same wiring as NetworkModule.provideOkHttpClient.
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { holder.get() })
            .build()
        val factory = RetrofitFactory(client, Json { ignoreUnknownKeys = true })
        useCase = TokenLoginUseCase(factory, tokenStore, holder)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun valid_token_is_accepted_and_persisted() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"user_id":"@admin:example.com","device_id":"DEV1"}"""
            )
        )
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "s1",
            token = "syt_admin_token_abc",
        )
        assertNotNull("Login should succeed", result.getOrNull())
        assertEquals("@admin:example.com", result.getOrNull()?.userId)
        assertEquals("DEV1", result.getOrNull()?.deviceId)
        assertEquals("syt_admin_token_abc", tokenStore.accessTokenFlow("s1").first())
        assertEquals("@admin:example.com", tokenStore.currentUserIdFlow("s1").first())
        assertEquals("syt_admin_token_abc", holder.get())
    }

    @Test
    fun token_is_trimmed_before_use() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"user_id":"@admin:example.com"}"""
            )
        )
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "s1",
            token = "  syt_padded_token  \n",
        )
        assertNotNull(result.getOrNull())
        assertEquals("syt_padded_token", tokenStore.accessTokenFlow("s1").first())
        assertEquals("syt_padded_token", holder.get())
    }

    @Test
    fun whoami_request_carries_authorization_header() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"user_id":"@admin:example.com"}"""
            )
        )
        useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "s1",
            token = "syt_xyz",
        )
        val recorded = server.takeRequest()
        assertEquals("Bearer syt_xyz", recorded.getHeader("Authorization"))
        assertEquals("/_matrix/client/v3/account/whoami", recorded.path)
    }

    @Test
    fun empty_token_is_rejected_without_hitting_server() = runTest {
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "s1",
            token = "   ",
        )
        assertTrue("Empty token should fail", result.isFailure)
        assertEquals(0, server.requestCount)
        assertNull("Holder should not be populated on empty token", holder.get())
    }

    @Test
    fun rejected_token_clears_active_holder_and_returns_typed_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"errcode":"M_UNKNOWN_TOKEN","error":"Bad token"}""")
        )
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "s1",
            token = "syt_bad",
        )
        assertTrue("401 should produce a failure", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Failure should be IllegalArgumentException", ex is IllegalArgumentException)
        assertTrue(
            "Message should mention M_UNKNOWN_TOKEN",
            ex?.message?.contains("M_UNKNOWN_TOKEN") == true,
        )
        assertNull("Holder must be cleared on failure", holder.get())
        assertNull("Token must NOT be persisted on failure", tokenStore.accessTokenFlow("s1").first())
    }
}
