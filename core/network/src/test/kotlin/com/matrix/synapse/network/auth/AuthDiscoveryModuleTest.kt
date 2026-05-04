package com.matrix.synapse.network.auth

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.matrix.synapse.network.RetrofitFactory

class AuthDiscoveryModuleTest {

    @Test
    fun provides_non_null_MasDiscoveryService() {
        val json = Json { ignoreUnknownKeys = true }
        val factory = RetrofitFactory(OkHttpClient(), json)
        val service = AuthDiscoveryModule.provideMasDiscoveryService(factory, json)
        assertNotNull(service)
    }
}
