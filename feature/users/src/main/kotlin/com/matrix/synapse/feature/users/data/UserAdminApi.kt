package com.matrix.synapse.feature.users.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserAdminApi {

    @GET("/_synapse/admin/v2/users")
    suspend fun listUsers(
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("name") name: String? = null,
        @Query("guests") guests: Boolean? = null,
        @Query("deactivated") deactivated: Boolean? = null,
    ): UsersListResponse

    @GET("/_synapse/admin/v2/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserDetail

    @PUT("/_synapse/admin/v2/users/{userId}")
    suspend fun upsertUser(
        @Path("userId") userId: String,
        @Body request: UpsertUserRequest,
    ): UserDetail

    @PUT("/_synapse/admin/v1/suspend/{userId}")
    suspend fun setSuspended(
        @Path("userId") userId: String,
        @Body request: SuspendRequest,
    )

    @POST("/_synapse/admin/v1/deactivate/{userId}")
    suspend fun deactivateUser(
        @Path("userId") userId: String,
        @Body request: DeactivateRequest,
    ): DeactivateResponse

    @GET("/_synapse/admin/v1/users/{userId}/media")
    suspend fun listUserMedia(
        @Path("userId") userId: String,
        @Query("from") from: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("dir") dir: String? = null,
        @Query("from_ts") fromTs: Long? = null,
        @Query("until_ts") untilTs: Long? = null,
    ): UserMediaListResponse

    /** Deletes local media uploaded by the user; supports the same filter/sort params as [listUserMedia]. */
    @DELETE("/_synapse/admin/v1/users/{userId}/media")
    suspend fun deleteUserMediaBulk(
        @Path("userId") userId: String,
        @Query("from") from: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("dir") dir: String? = null,
        @Query("from_ts") fromTs: Long? = null,
        @Query("until_ts") untilTs: Long? = null,
    ): UserMediaBulkDeleteResponse

    @DELETE("/_synapse/admin/v1/media/{serverName}/{mediaId}")
    suspend fun deleteMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    ): DeactivateResponse
}
