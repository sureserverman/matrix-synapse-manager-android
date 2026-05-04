package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.network.auth.AuthMetadata
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

class MasTokenExchangerScopeTest {

    private lateinit var server: MockWebServer
    private lateinit var exchanger: MasTokenExchanger
    private val json = Json { ignoreUnknownKeys = true }

    private fun buildMetadata() = AuthMetadata(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = server.url("/token").toString(),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        exchanger = MasTokenExchanger(OkHttpClient(), json, now = { 1000L })
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun succeeds_when_admin_scope_present() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"access_token":"at","token_type":"Bearer",""" +
                        """"scope":"openid urn:matrix:org.matrix.msc2967.client:api:* urn:synapse:admin:*"}"""
                )
        )

        val result = exchanger.exchange(
            metadata = buildMetadata(),
            clientId = "test-client",
            code = "code",
            codeVerifier = "verifier",
        )

        assertTrue("Expected success when admin scope present, got: $result", result.isSuccess)
    }

    @Test
    fun fails_with_typed_exception_when_admin_scope_absent() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"access_token":"at","token_type":"Bearer",""" +
                        """"scope":"openid urn:matrix:org.matrix.msc2967.client:api:*"}"""
                )
        )

        val result = exchanger.exchange(
            metadata = buildMetadata(),
            clientId = "test-client",
            code = "code",
            codeVerifier = "verifier",
        )

        assertTrue("Expected failure when admin scope absent, got: $result", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(
            "Expected MasAdminScopeDeniedException, got: ${ex?.javaClass}",
            ex is MasAdminScopeDeniedException,
        )
        val denied = ex as MasAdminScopeDeniedException
        assertEquals(
            setOf("openid", "urn:matrix:org.matrix.msc2967.client:api:*"),
            denied.grantedScopes,
        )
    }
}
