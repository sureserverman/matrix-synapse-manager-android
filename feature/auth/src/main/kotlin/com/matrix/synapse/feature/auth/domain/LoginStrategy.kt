package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.auth.AuthMetadata

sealed interface LoginStrategy {
    data class Password(val homeserverUrl: String) : LoginStrategy
    data class Oauth(val homeserverUrl: String, val metadata: AuthMetadata) : LoginStrategy
}

data class LoginResult(
    val userId: String,
    val deviceId: String?,
    val grantedScopes: Set<String>? = null,
)
