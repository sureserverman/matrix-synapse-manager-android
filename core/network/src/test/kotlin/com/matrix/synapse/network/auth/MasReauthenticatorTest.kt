package com.matrix.synapse.network.auth

import com.matrix.synapse.network.ActiveTokenHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MasReauthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var activeTokenHolder: ActiveTokenHolder
    private lateinit var refresher: MasTokenRefresher
    private lateinit var authenticator: MasReauthenticator
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        activeTokenHolder = ActiveTokenHolder()
        refresher = mockk()
        authenticator = MasReauthenticator(refresher, activeTokenHolder)

        client = OkHttpClient.Builder()
            .authenticator(authenticator)
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun retries_request_with_new_token_after_401() {
        activeTokenHolder.set("stale")
        authenticator.registerServer("s1", server.url("/token").toString())
        coEvery { refresher.refresh(any(), any()) } returns "fresh-token"

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val response = client.newCall(
            Request.Builder()
                .url(server.url("/api/resource"))
                .header("Authorization", "Bearer stale")
                .build()
        ).execute()

        assertEquals(200, response.code)
        // Second request should carry the fresh token
        val secondRequest = server.takeRequest() // first (401)
        val retryRequest = server.takeRequest()  // second (200)
        assertEquals("Bearer fresh-token", retryRequest.getHeader("Authorization"))
        coVerify(exactly = 1) { refresher.refresh(any(), any()) }
    }

    @Test
    fun does_not_retry_when_no_server_registered() {
        activeTokenHolder.set("stale")
        // intentionally skip authenticator.registerServer(...)
        coEvery { refresher.refresh(any(), any()) } returns "fresh-token"

        server.enqueue(MockResponse().setResponseCode(401))

        val response = client.newCall(
            Request.Builder()
                .url(server.url("/api/resource"))
                .header("Authorization", "Bearer stale")
                .build()
        ).execute()

        assertEquals(401, response.code)
        coVerify(exactly = 0) { refresher.refresh(any(), any()) }
    }

    @Test
    fun does_not_retry_twice() {
        activeTokenHolder.set("stale")
        authenticator.registerServer("s1", server.url("/token").toString())
        coEvery { refresher.refresh(any(), any()) } returns "fresh"

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client.newCall(
            Request.Builder()
                .url(server.url("/api/resource"))
                .header("Authorization", "Bearer stale")
                .build()
        ).execute()

        assertEquals(401, response.code)
        coVerify(exactly = 1) { refresher.refresh(any(), any()) }
    }
}
