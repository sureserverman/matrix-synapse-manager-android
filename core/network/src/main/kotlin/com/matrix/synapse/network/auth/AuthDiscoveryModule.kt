package com.matrix.synapse.network.auth

import com.matrix.synapse.network.RetrofitFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthDiscoveryModule {

    @Provides
    @Singleton
    fun provideMasDiscoveryService(factory: RetrofitFactory, json: Json): MasDiscoveryService =
        MasDiscoveryService(factory, json)
}
