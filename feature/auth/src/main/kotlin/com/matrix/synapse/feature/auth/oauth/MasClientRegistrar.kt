package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.network.auth.AuthMetadata
import com.matrix.synapse.security.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MasClientRegistrationException(val code: Int, val body: String) :
    Exception("MAS client registration failed with HTTP $code: $body")

@Serializable
private data class RegistrationRequest(
    @SerialName("client_name")
    val clientName: String,
    @SerialName("application_type")
    val applicationType: String,
    @SerialName("redirect_uris")
    val redirectUris: List<String>,
    @SerialName("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: String,
    @SerialName("grant_types")
    val grantTypes: List<String>,
    @SerialName("response_types")
    val responseTypes: List<String>,
)

@Serializable
private data class RegistrationResponse(
    @SerialName("client_id")
    val clientId: String,
)

class MasClientRegistrar(
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
    private val json: Json,
) {
    suspend fun register(serverId: String, metadata: AuthMetadata): String {
        val existing = tokenStore.oauthClientIdFlow(serverId).first()
        if (existing != null) return existing

        val registrationEndpoint = metadata.registrationEndpoint
            ?: throw IllegalStateException("registration_endpoint is null in AuthMetadata for serverId=$serverId")

        val requestBody = RegistrationRequest(
            clientName = "Matrix Synapse Manager",
            applicationType = "native",
            redirectUris = listOf("com.matrix.synapse.manager://oauth/redirect"),
            tokenEndpointAuthMethod = "none",
            grantTypes = listOf("authorization_code", "refresh_token"),
            responseTypes = listOf("code"),
        )

        val requestBodyJson = json.encodeToString(RegistrationRequest.serializer(), requestBody)

        val httpRequest = Request.Builder()
            .url(registrationEndpoint)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val responseBodyString = withContext(Dispatchers.IO) {
            client.newCall(httpRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw MasClientRegistrationException(response.code, bodyStr)
                }
                bodyStr
            }
        }

        val clientId = json.decodeFromString(RegistrationResponse.serializer(), responseBodyString).clientId
        tokenStore.saveOAuthClientId(serverId, clientId)
        return clientId
    }
}
