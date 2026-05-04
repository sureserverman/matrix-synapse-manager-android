package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.feature.auth.data.AuthApi
import com.matrix.synapse.feature.auth.data.LoginRequest
import com.matrix.synapse.feature.auth.data.UserIdentifier
import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.SecureTokenStore
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
) {
    /**
     * Authenticates with password login.
     * On success, stores the access token keyed by [serverId] and returns [LoginResult].
     * The [password] is used only during the HTTP call and is NEVER persisted.
     */
    suspend fun login(
        serverUrl: String,
        serverId: String,
        username: String,
        password: String,
    ): Result<LoginResult> {
        return try {
            val api = retrofitFactory.create<AuthApi>(serverUrl)
            val response = api.login(
                LoginRequest(
                    identifier = UserIdentifier(user = username),
                    password = password,
                )
            )
            // Store access token and user identity; password is discarded here
            tokenStore.saveToken(serverId, response.accessToken)
            tokenStore.saveUserId(serverId, response.userId)
            activeTokenHolder.set(response.accessToken)
            Result.success(LoginResult(userId = response.userId, deviceId = response.deviceId))
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
