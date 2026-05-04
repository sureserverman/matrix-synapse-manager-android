package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.network.auth.AuthMetadata
import com.matrix.synapse.security.InMemoryTokenStore
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MasClientRegistrarTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var registrar: MasClientRegistrar
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun buildMetadata(registrationUrl: String) = AuthMetadata(
        issuer = "https://auth.example.com",
        authorizationEndpoint = "https://auth.example.com/authorize",
        tokenEndpoint = "https://auth.example.com/token",
        registrationEndpoint = registrationUrl,
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = InMemoryTokenStore()
        registrar = MasClientRegistrar(OkHttpClient(), tokenStore, json)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun registers_via_post_and_persists_client_id() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"client_id":"test-client-abc"}""")
        )
        val spyStore = spyk(tokenStore)
        val spyRegistrar = MasClientRegistrar(OkHttpClient(), spyStore, json)

        val result = spyRegistrar.register("s1", buildMetadata(server.url("/register").toString()))

        assertEquals("test-client-abc", result)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        assert(body.contains("\"client_name\":\"Matrix Synapse Manager\"")) {
            "Expected client_name in body, got: $body"
        }
        assert(body.contains("\"application_type\":\"native\"")) {
            "Expected application_type in body, got: $body"
        }
        assert(body.contains("\"token_endpoint_auth_method\":\"none\"")) {
            "Expected token_endpoint_auth_method in body, got: $body"
        }
        assert(body.contains("com.matrix.synapse.manager://oauth/redirect")) {
            "Expected redirect URI in body, got: $body"
        }

        coVerify(exactly = 1) { spyStore.saveOAuthClientId("s1", "test-client-abc") }
        assertEquals("test-client-abc", spyStore.oauthClientIdFlow("s1").first())
    }

    @Test
    fun is_idempotent_when_client_id_already_stored() = runTest {
        tokenStore.saveOAuthClientId("s1", "existing-id")
        val spyStore = spyk(tokenStore)
        val spyRegistrar = MasClientRegistrar(OkHttpClient(), spyStore, json)

        val result = spyRegistrar.register("s1", buildMetadata(server.url("/register").toString()))

        assertEquals("existing-id", result)
        assertEquals(0, server.requestCount)
        coVerify(exactly = 0) { spyStore.saveOAuthClientId(any(), any()) }
    }

    @Test
    fun throws_typed_exception_on_non_2xx() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"invalid_redirect_uri"}""")
        )

        var thrownException: MasClientRegistrationException? = null
        try {
            registrar.register("s1", buildMetadata(server.url("/register").toString()))
        } catch (e: MasClientRegistrationException) {
            thrownException = e
        }

        assertNull("Should not return a value on error", null.also {
            assert(thrownException != null) { "Expected MasClientRegistrationException to be thrown" }
        })
        assertEquals(400, thrownException!!.code)
        assert(thrownException.body.contains("invalid_redirect_uri")) {
            "Expected body to contain error, got: ${thrownException.body}"
        }
    }
}
