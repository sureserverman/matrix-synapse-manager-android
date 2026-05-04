package com.matrix.synapse.feature.auth.oauth

object OAuthScopes {
    const val OPENID = "openid"
    const val MATRIX_CLIENT_API = "urn:matrix:org.matrix.msc2967.client:api:*"
    const val SYNAPSE_ADMIN = "urn:synapse:admin:*"

    fun deviceScope(deviceId: String): String =
        "urn:matrix:org.matrix.msc2967.client:device:$deviceId"

    fun buildScopeSet(deviceId: String): Set<String> = setOf(
        OPENID,
        MATRIX_CLIENT_API,
        deviceScope(deviceId),
        SYNAPSE_ADMIN,
    )
}
