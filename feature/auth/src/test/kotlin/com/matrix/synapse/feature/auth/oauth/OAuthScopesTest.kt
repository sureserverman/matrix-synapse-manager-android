package com.matrix.synapse.feature.auth.oauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthScopesTest {

    @Test
    fun builds_four_element_set() {
        val scopes = OAuthScopes.buildScopeSet("ABC123")
        assertEquals(4, scopes.size)
    }

    @Test
    fun interpolates_device_id_verbatim() {
        val scopes = OAuthScopes.buildScopeSet("ABC123")
        assertTrue(
            "Expected device scope with verbatim device id",
            scopes.contains("urn:matrix:org.matrix.msc2967.client:device:ABC123"),
        )
    }

    @Test
    fun includes_synapse_admin() {
        val scopes = OAuthScopes.buildScopeSet("ABC123")
        assertTrue(
            "Expected urn:synapse:admin:* in scope set",
            scopes.contains(OAuthScopes.SYNAPSE_ADMIN),
        )
    }
}
