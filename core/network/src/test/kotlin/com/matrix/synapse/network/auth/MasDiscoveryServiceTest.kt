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

    /** Verbatim live response from https://matrix.libre.tw/.well-known/matrix/client
     *  on 2026-05-22 — the server reported in issue #4. No MSC2965 key, only
     *  homeserver and rtc_foci. */
    private val wellKnownLibreTw = """
        {"m.homeserver": {"base_url": "https://matrix.libre.tw"}, "org.matrix.msc4143.rtc_foci":[ {"type": "livekit", "livekit_service_url": "https://livekit.libre.tw"}]}
    """.trimIndent()

    private val authMetadataBody = javaClass.getResourceAsStream("/mas/auth_metadata.json")
        ?.bufferedReader()?.readText()
        ?: error("Fixture not found: /mas/auth_metadata.json")

    private val authMetadataLibreBody = javaClass.getResourceAsStream("/mas/auth_metadata_libre.json")
        ?.bufferedReader()?.readText()
        ?: error("Fixture not found: /mas/auth_metadata_libre.json")

    private fun notFound() = MockResponse().setResponseCode(404).setBody("404 Not Found")

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
    fun returns_NotMas_when_well_known_lacks_msc2965_and_no_auth_metadata() = runTest {
        // Well-known is valid JSON but omits MSC2965 → fall through to direct
        // auth_metadata probes. Both probes 404 → genuinely not MAS.
        server.enqueue(MockResponse().setBody(wellKnownWithoutMsc2965))
        server.enqueue(notFound())
        server.enqueue(notFound())

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected NotMas result", result is MasDiscoveryResult.NotMas)
        assertEquals("well-known + stable probe + unstable probe", 3, server.requestCount)
    }

    @Test
    fun returns_NetworkError_on_io_failure() = runTest {
        val baseUrl = server.url("/").toString().trimEnd('/')
        server.shutdown()

        val result = service.discover(baseUrl)

        assertTrue("Expected NetworkError result", result is MasDiscoveryResult.NetworkError)
    }

    /** Regression: a server that returns an HTML 404 page (or any non-JSON body)
     *  for `/.well-known/matrix/client` must NOT crash the app — per MSC2965 a
     *  malformed well-known is equivalent to no well-known, i.e. legacy password
     *  auth. */
    @Test
    fun returns_NotMas_when_well_known_body_is_html() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("<!doctype html><html><body>404 Not Found</body></html>"),
        )

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected NotMas result", result is MasDiscoveryResult.NotMas)
    }

    @Test
    fun returns_NotMas_when_well_known_body_is_empty() = runTest {
        server.enqueue(MockResponse().setBody(""))

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected NotMas result", result is MasDiscoveryResult.NotMas)
    }

    /** Issue #4: matrix.libre.tw's well-known omits the MSC2965 key, but its
     *  stable auth_metadata endpoint returns full MAS metadata. The fallback
     *  probe must detect this and report Mas. */
    @Test
    fun returns_Mas_when_well_known_lacks_msc2965_but_stable_auth_metadata_responds() = runTest {
        server.enqueue(MockResponse().setBody(wellKnownLibreTw))
        server.enqueue(MockResponse().setBody(authMetadataLibreBody))

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected Mas result", result is MasDiscoveryResult.Mas)
        val mas = result as MasDiscoveryResult.Mas
        assertEquals("https://auth.libre.tw/", mas.metadata.issuer)
        assertEquals(
            "well-known + stable probe only",
            2,
            server.requestCount,
        )
    }

    @Test
    fun returns_Mas_when_stable_auth_metadata_404s_and_unstable_responds() = runTest {
        server.enqueue(MockResponse().setBody(wellKnownWithoutMsc2965))
        server.enqueue(notFound())
        server.enqueue(MockResponse().setBody(authMetadataBody))

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected Mas result", result is MasDiscoveryResult.Mas)
        val mas = result as MasDiscoveryResult.Mas
        assertEquals("https://auth.rollenspiel.chat/", mas.metadata.issuer)
        assertEquals(
            "well-known + stable probe (404) + unstable probe",
            3,
            server.requestCount,
        )
    }

    @Test
    fun returns_NotMas_when_both_auth_metadata_paths_404() = runTest {
        server.enqueue(MockResponse().setBody(wellKnownWithoutMsc2965))
        server.enqueue(notFound())
        server.enqueue(notFound())

        val baseUrl = server.url("/").toString().trimEnd('/')
        val result = service.discover(baseUrl)

        assertTrue("Expected NotMas result", result is MasDiscoveryResult.NotMas)
    }
}
