package com.matrix.synapse.network.auth

import com.matrix.synapse.network.RetrofitFactory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.IOException
import javax.inject.Inject

interface AuthMetadataService {
    @GET
    suspend fun fetch(@Url url: String): AuthMetadata
}

sealed class MasDiscoveryResult {
    data class Mas(val metadata: AuthMetadata) : MasDiscoveryResult()
    object NotMas : MasDiscoveryResult()
    data class NetworkError(val cause: Throwable) : MasDiscoveryResult()
}

class MasDiscoveryService @Inject constructor(
    private val factory: RetrofitFactory,
    private val json: Json,
) {
    suspend fun discover(homeserverUrl: String): MasDiscoveryResult {
        val wellKnownService = factory.create<WellKnownService>(homeserverUrl)
        val wellKnown = try {
            wellKnownService.fetch("$homeserverUrl/.well-known/matrix/client")
        } catch (e: IOException) {
            return MasDiscoveryResult.NetworkError(e)
        } catch (e: HttpException) {
            return MasDiscoveryResult.NetworkError(e)
        } catch (e: SerializationException) {
            // Well-known body wasn't valid JSON (HTML 404 page, redirect HTML,
            // empty body). Per MSC2965 a malformed well-known is equivalent to
            // no well-known — fall back to legacy password auth.
            return MasDiscoveryResult.NotMas
        } catch (e: IllegalArgumentException) {
            // okhttp/retrofit can wrap malformed-URL or empty-body parse failures
            // as IllegalArgumentException — same fallback.
            return MasDiscoveryResult.NotMas
        }

        val authMetadataService = factory.create<AuthMetadataService>(homeserverUrl)

        if (wellKnown.orgMatrixMsc2965Authentication != null) {
            return try {
                val metadata = authMetadataService.fetch("$homeserverUrl/_matrix/client/v1/auth_metadata")
                MasDiscoveryResult.Mas(metadata)
            } catch (e: IOException) {
                MasDiscoveryResult.NetworkError(e)
            } catch (e: HttpException) {
                MasDiscoveryResult.NetworkError(e)
            }
        }

        // Well-known is valid JSON but does not advertise MSC2965. Some real-world
        // MAS-fronted Synapse deployments under-advertise the discovery key while
        // still serving auth metadata directly — issue #4 (matrix.libre.tw) is one
        // such server. Probe the stable auth_metadata path, then the unstable
        // MSC2965 path, before giving up.
        return probeAuthMetadata(authMetadataService, "$homeserverUrl/_matrix/client/v1/auth_metadata")
            ?: probeAuthMetadata(authMetadataService, "$homeserverUrl/_matrix/client/unstable/org.matrix.msc2965/auth_metadata")
            ?: MasDiscoveryResult.NotMas
    }

    private suspend fun probeAuthMetadata(
        service: AuthMetadataService,
        url: String,
    ): MasDiscoveryResult.Mas? = try {
        MasDiscoveryResult.Mas(service.fetch(url))
    } catch (_: HttpException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
