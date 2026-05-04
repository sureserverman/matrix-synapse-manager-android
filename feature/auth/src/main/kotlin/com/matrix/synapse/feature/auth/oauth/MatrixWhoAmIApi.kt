package com.matrix.synapse.feature.auth.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface MatrixWhoAmIApi {
    @GET("/_matrix/client/v3/account/whoami")
    suspend fun whoAmI(): WhoAmIResponse
}

@Serializable
data class WhoAmIResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String? = null,
)
