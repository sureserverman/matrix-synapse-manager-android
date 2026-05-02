package com.matrix.synapse.feature.media.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomMediaResponse(
    val local: List<String> = emptyList(),
    val remote: List<String> = emptyList(),
)

@Serializable
data class MediaInfoResponse(
    @SerialName("media_id") val mediaId: String,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_length") val mediaLength: Long = 0L,
    @SerialName("upload_name") val uploadName: String? = null,
    @SerialName("created_ts") val createdTs: Long = 0L,
    @SerialName("last_access_ts") val lastAccessTs: Long = 0L,
    @SerialName("quarantined_by") val quarantinedBy: String? = null,
    @SerialName("safe_from_quarantine") val safeFromQuarantine: Boolean = false,
)

@Serializable
data class QuarantineResponse(
    @SerialName("num_quarantined") val numQuarantined: Int = 0,
)

@Serializable
data class DeleteMediaResponse(
    @SerialName("deleted_media") val deletedMedia: List<String> = emptyList(),
    val total: Int = 0,
)
