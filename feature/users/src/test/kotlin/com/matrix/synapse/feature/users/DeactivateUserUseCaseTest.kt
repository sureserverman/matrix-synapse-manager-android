package com.matrix.synapse.feature.users

import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
import com.matrix.synapse.network.RetrofitFactory
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

class DeactivateUserUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: DeactivateUserUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        val repository = UserRepository(factory)
        useCase = DeactivateUserUseCase(repository)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun deactivation_deletes_media_first_when_option_enabled() = runTest {
        // 1. Bulk user-media delete (batch of deletions)
        server.enqueue(
            MockResponse().setBody("""{"deleted_media":["abc123","def456"],"total":2}""")
        )
        // 2. Second batch: nothing left
        server.enqueue(
            MockResponse().setBody("""{"deleted_media":[],"total":0}""")
        )
        // 3. Deactivate
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = true,
            confirmed = true,
        )

        assertTrue(result.isSuccess)
        assertEquals(3, server.requestCount)

        val bulk1 = server.takeRequest()
        assertEquals("DELETE", bulk1.method)
        val p1 = bulk1.path!!
        assertTrue("Expected …/users/…/media, was: $p1", p1.contains("/users/") && p1.contains("/media"))

        val bulk2 = server.takeRequest()
        assertEquals("DELETE", bulk2.method)

        val deactivateRequest = server.takeRequest()
        assertEquals("POST", deactivateRequest.method)
        assertTrue("Path should be deactivate", deactivateRequest.path!!.contains("deactivate"))
        val body = deactivateRequest.body.readUtf8()
        assertTrue("erase should be true when media deleted: $body", body.contains("\"erase\":true"))
    }

    @Test
    fun typed_confirmation_is_required_for_deactivate() = runTest {
        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = false,
            confirmed = false,
        )

        assertTrue("Deactivation without confirmation should fail", result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun deactivation_continues_after_partial_media_delete_failure() = runTest {
        // Bulk user-media delete fails
        server.enqueue(
            MockResponse().setResponseCode(404).setBody("""{"errcode":"M_NOT_FOUND"}""")
        )
        // Deactivate still proceeds
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = true,
            confirmed = true,
        )

        assertTrue("Should succeed despite bulk media delete failure", result.isSuccess)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun deactivation_without_media_cleanup_skips_media_endpoints() = runTest {
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = false,
            confirmed = true,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("deactivate"))
    }
}
