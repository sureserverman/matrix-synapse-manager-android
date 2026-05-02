package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response body for `DELETE /_synapse/admin/v1/users/{userId}/media` (bulk delete with filters). */
@Serializable
data class UserMediaBulkDeleteResponse(
    @SerialName("deleted_media") val deletedMedia: List<String> = emptyList(),
    val total: Int = 0,
)
