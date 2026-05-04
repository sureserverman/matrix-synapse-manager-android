package com.matrix.synapse.network.auth

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
import com.matrix.synapse.network.RetrofitFactory

class MasDiscoveryServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: MasDiscoveryService

    private val wellKnownWithMsc2965 = """
        {
          "m.homeserver": {"base_url": "https://matrix.rollenspiel.chat"},
          "org.matrix.msc2965.authentication": {
            "issuer": "https://auth.rollenspiel.chat/",
            "account": "https://auth.rollenspiel.chat/account/"
          }
        }
    """.trimIndent()

    private val wellKnownWithoutMsc2965 = """
        {
          "m.homeserver": {"base_url": "https://matrix.example.com"}
        }
    """.trimIndent()

    private val authMetadataBody = javaClass.getResourceAsStream("/mas/auth_metadata.json")
        ?.bufferedReader()?.readText()
        ?: error("Fixture not found: /mas/auth_metadata.json")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val factory = RetrofitFactory(OkHttpClient(), json)
        service = MasDiscoveryService(factory, json)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun returns_Mas_when_well_known_advertises_msc2965() = runTest {
        server.enqueue(MockResponse().setBody(wellKnownWithMsc2965))
        server.enqueue(MockResponse().setBody(authMetadataBody))

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected Mas result", result is MasDiscoveryResult.Mas)
        val mas = result as MasDiscoveryResult.Mas
        assertEquals("https://auth.rollenspiel.chat/", mas.metadata.issuer)
    }

    @Test
    fun returns_NotMas_when_well_known_lacks_msc2965() = runTest {
        server.enqueue(MockResponse().setBody(wellKnownWithoutMsc2965))

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected NotMas result", result is MasDiscoveryResult.NotMas)
        assertEquals("Only one request should have been made", 1, server.requestCount)
    }

    @Test
    fun returns_NetworkError_on_io_failure() = runTest {
        val baseUrl = server.url("/").toString().trimEnd('/')
        server.shutdown()

        val result = service.discover(baseUrl)

        assertTrue("Expected NetworkError result", result is MasDiscoveryResult.NetworkError)
    }
}
