package com.matrix.synapse.feature.auth.domain

import android.content.Intent
import com.matrix.synapse.feature.auth.oauth.MasAuthCoordinator
import com.matrix.synapse.feature.auth.oauth.MasClientRegistrar
import com.matrix.synapse.feature.auth.oauth.MasTokenExchanger
import com.matrix.synapse.feature.auth.oauth.MatrixWhoAmIApi
import com.matrix.synapse.feature.auth.oauth.PendingOauth
import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.SecureTokenStore
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

data class OauthBegin(
    val authIntent: Intent,
    val pending: PendingOauth,
)

class OAuthLoginUseCase @Inject constructor(
    private val registrar: MasClientRegistrar,
    private val coordinator: MasAuthCoordinator,
    private val exchanger: MasTokenExchanger,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
    private val retrofitFactory: RetrofitFactory,
) {
    suspend fun beginOauth(serverId: String, strategy: LoginStrategy.Oauth): OauthBegin {
        val clientId = registrar.register(serverId, strategy.metadata)
        val deviceId = coordinator.newDeviceId()
        val request = coordinator.buildAuthorizationRequest(
            metadata = strategy.metadata,
            clientId = clientId,
            deviceId = deviceId,
        )
        val codeVerifier = request.codeVerifier
            ?: throw IllegalStateException("AppAuth did not generate a code verifier — PKCE misconfiguration")
        val pending = PendingOauth(
            serverId = serverId,
            homeserverUrl = strategy.homeserverUrl,
            metadata = strategy.metadata,
            clientId = clientId,
            deviceId = deviceId,
            codeVerifier = codeVerifier,
        )
        return OauthBegin(
            authIntent = coordinator.authorizationIntent(request),
            pending = pending,
        )
    }

    suspend fun completeOauth(pending: PendingOauth, response: AuthorizationResponse): Result<LoginResult> {
        val code = response.authorizationCode
            ?: return Result.failure(IllegalStateException("Authorization code missing from response"))
        val tokenResult = exchanger.exchange(
            metadata = pending.metadata,
            clientId = pending.clientId,
            code = code,
            codeVerifier = pending.codeVerifier,
            redirectUri = pending.redirectUri,
        )
        return tokenResult.mapCatching { tokenSet ->
            tokenStore.saveToken(pending.serverId, tokenSet.accessToken)
            tokenSet.refreshToken?.let { tokenStore.saveRefreshToken(pending.serverId, it) }
            tokenStore.saveTokenIssuedAt(pending.serverId, tokenSet.issuedAtEpochSeconds)
            activeTokenHolder.set(tokenSet.accessToken)

            val whoAmI = runCatching {
                retrofitFactory.create<MatrixWhoAmIApi>(pending.homeserverUrl).whoAmI()
            }.getOrNull()

            LoginResult(
                userId = whoAmI?.userId ?: "",
                deviceId = whoAmI?.deviceId ?: pending.deviceId,
                grantedScopes = tokenSet.scope.trim().split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }.toSet(),
            )
        }
    }
}
