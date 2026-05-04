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
        return try {
            val wellKnownService = factory.create<WellKnownService>(homeserverUrl)
            val wellKnown = wellKnownService.fetch("$homeserverUrl/.well-known/matrix/client")

            if (wellKnown.orgMatrixMsc2965Authentication == null) {
                return MasDiscoveryResult.NotMas
            }

            val authMetadataService = factory.create<AuthMetadataService>(homeserverUrl)
            val metadata = authMetadataService.fetch("$homeserverUrl/_matrix/client/v1/auth_metadata")

            MasDiscoveryResult.Mas(metadata)
        } catch (e: IOException) {
            MasDiscoveryResult.NetworkError(e)
        } catch (e: HttpException) {
            MasDiscoveryResult.NetworkError(e)
        } catch (e: SerializationException) {
            // Well-known body wasn't valid JSON (HTML 404 page, redirect HTML,
            // empty body). Per MSC2965 a malformed well-known is equivalent to
            // no well-known — fall back to legacy password auth.
            MasDiscoveryResult.NotMas
        } catch (e: IllegalArgumentException) {
            // okhttp/retrofit can wrap malformed-URL or empty-body parse failures
            // as IllegalArgumentException — same fallback.
            MasDiscoveryResult.NotMas
        }
    }
}
