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
import java.net.URLDecoder

class MasTokenExchangerTest {

    private lateinit var server: MockWebServer
    private lateinit var exchanger: MasTokenExchanger
    private val json = Json { ignoreUnknownKeys = true }
    private val fixedNow = 1000L

    private fun buildMetadata() = AuthMetadata(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = server.url("/token").toString(),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        exchanger = MasTokenExchanger(OkHttpClient(), json, now = { fixedNow })
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun parses_full_token_response() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"access_token":"at","refresh_token":"rt","expires_in":3600,""" +
                        """"scope":"openid urn:matrix:org.matrix.msc2967.client:api:* urn:synapse:admin:*",""" +
                        """"token_type":"Bearer"}"""
                )
        )

        val result = exchanger.exchange(
            metadata = buildMetadata(),
            clientId = "test-client",
            code = "THE_CODE",
            codeVerifier = "THE_VERIFIER",
        )

        assertTrue("Expected success, got: $result", result.isSuccess)
        val tokenSet = result.getOrThrow()
        assertEquals("at", tokenSet.accessToken)
        assertEquals("rt", tokenSet.refreshToken)
        assertEquals(3600L, tokenSet.expiresIn)
        assertEquals("Bearer", tokenSet.tokenType)
        assertEquals(fixedNow, tokenSet.issuedAtEpochSeconds)
    }

    @Test
    fun request_uses_correct_form_fields() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"access_token":"at","scope":"openid urn:matrix:org.matrix.msc2967.client:api:* urn:synapse:admin:*","token_type":"Bearer"}"""
                )
        )

        exchanger.exchange(
            metadata = buildMetadata(),
            clientId = "test-client",
            code = "THE_CODE",
            codeVerifier = "THE_VERIFIER",
        )

        val recorded = server.takeRequest()
        val body = URLDecoder.decode(recorded.body.readUtf8(), "UTF-8")
        assertTrue("Expected grant_type in: $body", body.contains("grant_type=authorization_code"))
        assertTrue("Expected code in: $body", body.contains("code=THE_CODE"))
        assertTrue("Expected code_verifier in: $body", body.contains("code_verifier=THE_VERIFIER"))
        assertTrue("Expected client_id in: $body", body.contains("client_id=test-client"))
        assertTrue(
            "Expected redirect_uri in: $body",
            body.contains("redirect_uri=com.matrix.synapse.manager://oauth/redirect"),
        )
    }

    @Test
    fun propagates_typed_failure_on_4xx() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"invalid_grant"}""")
        )

        val result = exchanger.exchange(
            metadata = buildMetadata(),
            clientId = "test-client",
            code = "bad-code",
            codeVerifier = "THE_VERIFIER",
        )

        assertTrue("Expected failure, got: $result", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(
            "Expected MasTokenExchangeException, got: ${ex?.javaClass}",
            ex is MasTokenExchangeException,
        )
        val tokenEx = ex as MasTokenExchangeException
        assertEquals(400, tokenEx.code)
        assertTrue(
            "Expected body to contain invalid_grant, got: ${tokenEx.responseBody}",
            tokenEx.responseBody.contains("invalid_grant"),
        )
    }
}
