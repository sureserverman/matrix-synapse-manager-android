package com.matrix.synapse.feature.auth.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.matrix.synapse.network.auth.AuthMetadata
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.util.UUID

class MasAuthCoordinator(
    private val context: Context,
    private val authService: AuthorizationService = AuthorizationService(context),
) {

    fun newDeviceId(): String = UUID.randomUUID().toString().replace("-", "")

    fun buildAuthorizationRequest(
        metadata: AuthMetadata,
        clientId: String,
        deviceId: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
    ): AuthorizationRequest {
        val config = AuthorizationServiceConfiguration(
            Uri.parse(metadata.authorizationEndpoint),
            Uri.parse(metadata.tokenEndpoint),
            metadata.registrationEndpoint?.let { Uri.parse(it) },
            metadata.revocationEndpoint?.let { Uri.parse(it) },
        )
        val scopes = OAuthScopes.buildScopeSet(deviceId)
        return AuthorizationRequest.Builder(
            config,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        )
            .setScopes(scopes)
            .setPrompt("consent")
            // PKCE S256 is enabled by default in AppAuth. We do not call setCodeVerifier(null).
            .build()
    }

    fun authorizationIntent(request: AuthorizationRequest): Intent =
        authService.getAuthorizationRequestIntent(request)

    fun dispose() = authService.dispose()

    companion object {
        const val DEFAULT_REDIRECT_URI = "com.matrix.synapse.manager://oauth/redirect"
    }
}
