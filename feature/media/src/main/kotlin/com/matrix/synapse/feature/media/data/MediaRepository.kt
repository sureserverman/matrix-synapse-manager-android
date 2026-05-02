package com.matrix.synapse.feature.media.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): MediaAdminApi = retrofitFactory.create(serverUrl)

    suspend fun listRoomMedia(serverUrl: String, roomId: String): RoomMediaResponse =
        api(serverUrl).listRoomMedia(roomId)

    suspend fun getMediaInfo(serverUrl: String, serverName: String, mediaId: String): MediaInfoResponse =
        api(serverUrl).getMediaInfo(serverName, mediaId)

    suspend fun quarantineMedia(serverUrl: String, serverName: String, mediaId: String) =
        api(serverUrl).quarantineMedia(serverName, mediaId)

    suspend fun unquarantineMedia(serverUrl: String, serverName: String, mediaId: String) =
        api(serverUrl).unquarantineMedia(serverName, mediaId)

    suspend fun protectMedia(serverUrl: String, mediaId: String) =
        api(serverUrl).protectMedia(mediaId)

    suspend fun unprotectMedia(serverUrl: String, mediaId: String) =
        api(serverUrl).unprotectMedia(mediaId)

    suspend fun deleteMedia(serverUrl: String, serverName: String, mediaId: String): DeleteMediaResponse =
        api(serverUrl).deleteMedia(serverName, mediaId)
}
