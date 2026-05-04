package com.matrix.synapse.network.auth

import retrofit2.http.GET
import retrofit2.http.Url

interface WellKnownService {
    @GET
    suspend fun fetch(@Url url: String): WellKnownMatrixClient
}
