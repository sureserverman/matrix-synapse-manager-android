package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.feature.auth.oauth.MatrixWhoAmIApi
import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.SecureTokenStore
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Lets the user paste an existing access token (e.g. one issued by Element,
 * by `register_new_matrix_user`, or by an admin script that predates a MAS
 * migration) and use it directly. Bypasses `/_matrix/client/v3/login`
 * entirely.
 *
 * Validated by calling `/_matrix/client/v3/account/whoami`, which is cheap
 * and confirms both that the token is valid and that we know the bound
 * user-id (needed by every screen that surfaces "currentUser").
 *
 * On any failure the [activeTokenHolder] is cleared so a stale value cannot
 * leak into the auth interceptor.
 */
class TokenLoginUseCase @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
) {
    suspend fun login(
        serverUrl: String,
        serverId: String,
        token: String,
    ): Result<LoginResult> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Access token must not be empty"))
        }
        // Set the holder first so the auth interceptor includes the new token
        // on the whoami probe.
        activeTokenHolder.set(trimmed)
        return try {
            val api = retrofitFactory.create<MatrixWhoAmIApi>(serverUrl)
            val whoami = api.whoAmI()
            tokenStore.saveToken(serverId, trimmed)
            tokenStore.saveUserId(serverId, whoami.userId)
            Result.success(
                LoginResult(
                    userId = whoami.userId,
                    deviceId = whoami.deviceId,
                ),
            )
        } catch (e: HttpException) {
            activeTokenHolder.set(null)
            val msg = when (e.code()) {
                401 -> "Token rejected by homeserver (M_UNKNOWN_TOKEN)"
                403 -> "Token rejected by homeserver (M_FORBIDDEN)"
                else -> "Token validation failed: HTTP ${e.code()}"
            }
            Result.failure(IllegalArgumentException(msg, e))
        } catch (e: IOException) {
            activeTokenHolder.set(null)
            Result.failure(e)
        }
    }
}
