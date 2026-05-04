package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.network.auth.MasDiscoveryResult
import com.matrix.synapse.network.auth.MasDiscoveryService
import javax.inject.Inject

/**
 * Thin SAM interface so unit tests can supply a hand-rolled fake without
 * subclassing the concrete [MasDiscoveryService] (which is not open).
 * The Hilt binding in [com.matrix.synapse.feature.auth.oauth.OAuthModule] provides
 * the production implementation via [MasDiscoveryServiceAdapter].
 */
fun interface DiscoveryPort {
    suspend fun discover(homeserverUrl: String): MasDiscoveryResult
}

/** Production adapter that delegates to the injected [MasDiscoveryService]. */
class MasDiscoveryServiceAdapter @Inject constructor(
    private val service: MasDiscoveryService,
) : DiscoveryPort {
    override suspend fun discover(homeserverUrl: String): MasDiscoveryResult =
        service.discover(homeserverUrl)
}

class LoginStrategyResolver @Inject constructor(
    private val discovery: DiscoveryPort,
) {
    suspend fun resolve(homeserverUrl: String): LoginStrategy =
        when (val r = discovery.discover(homeserverUrl)) {
            is MasDiscoveryResult.Mas -> LoginStrategy.Oauth(homeserverUrl, r.metadata)
            MasDiscoveryResult.NotMas -> LoginStrategy.Password(homeserverUrl)
            is MasDiscoveryResult.NetworkError -> LoginStrategy.Password(homeserverUrl)
        }
}
