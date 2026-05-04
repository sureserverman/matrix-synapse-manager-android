package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.auth.AuthMetadata
import com.matrix.synapse.network.auth.MasDiscoveryResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private val canonicalMetadata = AuthMetadata(
    issuer = "https://auth.example.com",
    authorizationEndpoint = "https://auth.example.com/authorize",
    tokenEndpoint = "https://auth.example.com/token",
    registrationEndpoint = "https://auth.example.com/registration",
)

private class FakeDiscoveryPort(
    private val result: MasDiscoveryResult,
) : DiscoveryPort {
    override suspend fun discover(homeserverUrl: String): MasDiscoveryResult = result
}

class LoginStrategyResolverTest {

    @Test
    fun selects_Oauth_for_mas_servers() = runTest {
        val resolver = LoginStrategyResolver(FakeDiscoveryPort(MasDiscoveryResult.Mas(canonicalMetadata)))
        val result = resolver.resolve("https://mas.example.com")
        assertTrue("Expected Oauth strategy", result is LoginStrategy.Oauth)
        val oauth = result as LoginStrategy.Oauth
        assertEquals("https://mas.example.com", oauth.homeserverUrl)
        assertEquals(canonicalMetadata, oauth.metadata)
    }

    @Test
    fun selects_Password_for_non_mas_servers() = runTest {
        val resolver = LoginStrategyResolver(FakeDiscoveryPort(MasDiscoveryResult.NotMas))
        val result = resolver.resolve("https://legacy.example.com")
        assertTrue("Expected Password strategy", result is LoginStrategy.Password)
        assertEquals("https://legacy.example.com", (result as LoginStrategy.Password).homeserverUrl)
    }

    @Test
    fun defaults_to_Password_on_network_error() = runTest {
        val resolver = LoginStrategyResolver(
            FakeDiscoveryPort(MasDiscoveryResult.NetworkError(IOException("timeout")))
        )
        val result = resolver.resolve("https://unreachable.example.com")
        assertTrue("Expected Password strategy on network error", result is LoginStrategy.Password)
    }
}
