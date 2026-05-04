package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.network.auth.AuthMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

data class MasTokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long?,
    val scope: String,
    val tokenType: String,
    val issuedAtEpochSeconds: Long,
)

class MasTokenExchanger(
    private val client: OkHttpClient,
    private val json: Json,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    suspend fun exchange(
        metadata: AuthMetadata,
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String = MasAuthCoordinator.DEFAULT_REDIRECT_URI,
    ): Result<MasTokenSet> = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("client_id", clientId)
            .add("code_verifier", codeVerifier)
            .build()
        val req = Request.Builder()
            .url(metadata.tokenEndpoint)
            .post(body)
            .header("Accept", "application/json")
            .build()
        runCatching {
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MasTokenExchangeException(response.code, text)
                }
                val parsed = json.decodeFromString(TokenResponse.serializer(), text)
                val grantedScopes = parsed.scope.orEmpty()
                    .trim()
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }
                    .toSet()
                if (!grantedScopes.contains(OAuthScopes.SYNAPSE_ADMIN)) {
                    throw MasAdminScopeDeniedException(grantedScopes)
                }
                MasTokenSet(
                    accessToken = parsed.accessToken,
                    refreshToken = parsed.refreshToken,
                    expiresIn = parsed.expiresIn,
                    scope = parsed.scope.orEmpty(),
                    tokenType = parsed.tokenType.orEmpty(),
                    issuedAtEpochSeconds = now(),
                )
            }
        }
    }

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("scope") val scope: String? = null,
        @SerialName("token_type") val tokenType: String? = null,
    )
}

class MasTokenExchangeException(val code: Int, val responseBody: String) :
    RuntimeException("Token exchange failed: HTTP $code — $responseBody")

class MasAdminScopeDeniedException(val grantedScopes: Set<String>) :
    RuntimeException(
        "MAS issued a token, but did not grant urn:synapse:admin:* — granted: $grantedScopes. " +
            "Operator must run: mas-cli manage set-can-request-admin <user> --admin"
    )
