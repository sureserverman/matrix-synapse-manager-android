package com.matrix.synapse.network.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WellKnownMatrixClient(
    @SerialName("m.homeserver")
    val mHomeserver: HomeserverInfo,
    @SerialName("org.matrix.msc2965.authentication")
    val orgMatrixMsc2965Authentication: Msc2965Authentication? = null,
) {
    @Serializable
    data class HomeserverInfo(
        @SerialName("base_url")
        val baseUrl: String,
    )

    @Serializable
    data class Msc2965Authentication(
        @SerialName("issuer")
        val issuer: String,
        @SerialName("account")
        val account: String? = null,
    )
}
