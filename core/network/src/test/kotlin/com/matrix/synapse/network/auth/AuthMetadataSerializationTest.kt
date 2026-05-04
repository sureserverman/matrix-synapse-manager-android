package com.matrix.synapse.network.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthMetadataSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parses_full_mas_metadata_fixture() {
        val stream = javaClass.getResourceAsStream("/mas/auth_metadata.json")
            ?: error("Fixture not found: /mas/auth_metadata.json")
        val raw = stream.bufferedReader().readText()
        val metadata = json.decodeFromString<AuthMetadata>(raw)

        assertTrue("issuer must be non-empty", metadata.issuer.isNotEmpty())
        assertTrue("authorization_endpoint must be non-empty", metadata.authorizationEndpoint.isNotEmpty())
        assertTrue("token_endpoint must be non-empty", metadata.tokenEndpoint.isNotEmpty())
        assertTrue("code_challenge_methods_supported must contain S256", metadata.codeChallengeMethodsSupported.contains("S256"))
        assertTrue("token_endpoint_auth_methods_supported must contain none", metadata.tokenEndpointAuthMethodsSupported.contains("none"))
        assertNotNull("MAS GraphQL endpoint must be non-null", metadata.orgMatrixMatrixAuthenticationServiceGraphqlEndpoint)
    }

    @Test
    fun round_trips_minimal_metadata() {
        val original = AuthMetadata(
            issuer = "https://auth.example.com/",
            authorizationEndpoint = "https://auth.example.com/authorize",
            tokenEndpoint = "https://auth.example.com/token",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AuthMetadata>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun tolerates_unknown_fields() {
        val raw = """{"issuer":"https://auth.example.com/","authorization_endpoint":"https://auth.example.com/authorize","token_endpoint":"https://auth.example.com/token","future_field":"x"}"""
        val metadata = json.decodeFromString<AuthMetadata>(raw)
        assertEquals("https://auth.example.com/", metadata.issuer)
    }
}
