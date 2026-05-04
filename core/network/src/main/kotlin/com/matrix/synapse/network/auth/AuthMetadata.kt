package com.matrix.synapse.network.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthMetadata(
    @SerialName("issuer")
    val issuer: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("registration_endpoint")
    val registrationEndpoint: String? = null,
    @SerialName("revocation_endpoint")
    val revocationEndpoint: String? = null,
    @SerialName("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String> = emptyList(),
    @SerialName("grant_types_supported")
    val grantTypesSupported: List<String> = emptyList(),
    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethodsSupported: List<String> = emptyList(),
    @SerialName("account_management_uri")
    val accountManagementUri: String? = null,
    @SerialName("org.matrix.matrix-authentication-service.graphql_endpoint")
    val orgMatrixMatrixAuthenticationServiceGraphqlEndpoint: String? = null,
)
