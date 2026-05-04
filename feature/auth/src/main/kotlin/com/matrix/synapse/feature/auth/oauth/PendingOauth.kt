package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.network.auth.AuthMetadata

data class PendingOauth(
    val serverId: String,
    val homeserverUrl: String,
    val metadata: AuthMetadata,
    val clientId: String,
    val deviceId: String,
    val codeVerifier: String,
    val redirectUri: String = MasAuthCoordinator.DEFAULT_REDIRECT_URI,
)
