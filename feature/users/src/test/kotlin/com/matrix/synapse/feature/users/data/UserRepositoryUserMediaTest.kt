package com.matrix.synapse.feature.users.data

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

class UserRepositoryUserMediaTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        repository = UserRepository(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun listUserMedia_includes_filter_query_params() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"media":[],"total":0}""")
        )
        val base = server.url("/").toString()
        repository.listUserMedia(
            serverUrl = base,
            userId = "@alice:example.com",
            from = "3",
            limit = 10,
            orderBy = "created_ts",
            dir = "b",
            fromTs = 1000L,
            untilTs = 2000L,
        )
        val path = server.takeRequest().path!!
        assertTrue(path.contains("from=3"))
        assertTrue(path.contains("limit=10"))
        assertTrue(path.contains("order_by=created_ts"))
        assertTrue(path.contains("dir=b"))
        assertTrue(path.contains("from_ts=1000"))
        assertTrue(path.contains("until_ts=2000"))
        assertTrue(path.contains("/users/"))
        assertTrue(path.contains("/media"))
    }

    @Test
    fun deleteUserMediaBulk_uses_delete_method() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"deleted_media":["a"],"total":1}""")
        )
        val base = server.url("/").toString()
        val r = repository.deleteUserMediaBulk(
            serverUrl = base,
            userId = "@alice:example.com",
            limit = 50,
        )
        assertEquals(1, r.total)
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertTrue(req.path!!.contains("limit=50"))
    }
}
