package com.matrix.synapse.feature.auth.oauth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matrix.synapse.network.auth.AuthMetadata
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URLDecoder

@RunWith(AndroidJUnit4::class)
class MasAuthCoordinatorRequestTest {

    private val metadata = AuthMetadata(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
    )

    @Test
    fun request_uri_contains_s256_pkce_and_required_fields() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val coordinator = MasAuthCoordinator(context)
        val req = coordinator.buildAuthorizationRequest(metadata, "test-client", "DEV1")
        val uriStr = req.toUri().toString()
        val decoded = URLDecoder.decode(uriStr, "UTF-8")

        assertTrue(
            "Expected code_challenge_method=S256 in: $uriStr",
            uriStr.contains("code_challenge_method=S256"),
        )
        assertTrue(
            "Expected client_id=test-client in: $uriStr",
            uriStr.contains("client_id=test-client"),
        )
        assertTrue(
            "Expected response_type=code in: $uriStr",
            uriStr.contains("response_type=code"),
        )
        assertTrue(
            "Expected redirect_uri encoded in: $uriStr",
            uriStr.contains("redirect_uri=com.matrix.synapse.manager%3A%2F%2Foauth%2Fredirect"),
        )

        // Scopes are URL-encoded; decode and assert each individually
        val expectedScopes = OAuthScopes.buildScopeSet("DEV1")
        for (scope in expectedScopes) {
            assertTrue(
                "Expected scope '$scope' in decoded URI: $decoded",
                decoded.contains(scope),
            )
        }

        coordinator.dispose()
    }
}
