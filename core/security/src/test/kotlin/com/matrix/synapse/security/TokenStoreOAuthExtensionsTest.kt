package com.matrix.synapse.security

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for the OAuth extension fields added to [SecureTokenStore]:
 * refresh token, OAuth client_id, and token issued-at timestamp.
 *
 * Uses [InMemoryTokenStore] to avoid Android Keystore dependency.
 */
class TokenStoreOAuthExtensionsTest {

    private lateinit var store: SecureTokenStore

    @Before
    fun setUp() {
        store = InMemoryTokenStore()
    }

    @Test
    fun roundtrips_refresh_token_per_server() = runTest {
        store.saveRefreshToken(serverId = "s1", token = "r1")
        store.saveRefreshToken(serverId = "s2", token = "r2")

        assertEquals("r1", store.refreshTokenFlow("s1").first())
        assertEquals("r2", store.refreshTokenFlow("s2").first())
        assertNotEquals(store.refreshTokenFlow("s1").first(), store.refreshTokenFlow("s2").first())
    }

    @Test
    fun roundtrips_oauth_client_id_per_server() = runTest {
        store.saveOAuthClientId(serverId = "s1", clientId = "client-aaa")
        store.saveOAuthClientId(serverId = "s2", clientId = "client-bbb")

        assertEquals("client-aaa", store.oauthClientIdFlow("s1").first())
        assertEquals("client-bbb", store.oauthClientIdFlow("s2").first())
        assertNotEquals(store.oauthClientIdFlow("s1").first(), store.oauthClientIdFlow("s2").first())
    }

    @Test
    fun roundtrips_token_issued_at_per_server() = runTest {
        store.saveTokenIssuedAt(serverId = "s1", epochSeconds = 1735689600L)
        store.saveTokenIssuedAt(serverId = "s2", epochSeconds = 1735776000L)

        assertEquals(1735689600L, store.tokenIssuedAtFlow("s1").first())
        assertEquals(1735776000L, store.tokenIssuedAtFlow("s2").first())
        assertNotEquals(store.tokenIssuedAtFlow("s1").first(), store.tokenIssuedAtFlow("s2").first())
    }

    @Test
    fun clearTokens_removes_oauth_fields_too() = runTest {
        store.saveToken(serverId = "s1", accessToken = "access-xyz")
        store.saveRefreshToken(serverId = "s1", token = "refresh-xyz")
        store.saveOAuthClientId(serverId = "s1", clientId = "client-xyz")
        store.saveTokenIssuedAt(serverId = "s1", epochSeconds = 1735689600L)
        store.saveUserId(serverId = "s1", userId = "@alice:example.com")

        store.clearTokens(serverId = "s1")

        assertNull(store.accessTokenFlow("s1").first())
        assertNull(store.refreshTokenFlow("s1").first())
        assertNull(store.oauthClientIdFlow("s1").first())
        assertNull(store.tokenIssuedAtFlow("s1").first())
        assertNull(store.currentUserIdFlow("s1").first())
    }

    @Test
    fun tokenIssuedAtFlow_returns_null_when_absent() = runTest {
        assertNull(store.tokenIssuedAtFlow("unknown-server").first())
    }
}
