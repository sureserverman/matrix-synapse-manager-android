package com.matrix.synapse.feature.auth.oauth

import android.content.Context
import com.matrix.synapse.feature.auth.domain.DiscoveryPort
import com.matrix.synapse.feature.auth.domain.MasDiscoveryServiceAdapter
import com.matrix.synapse.network.auth.MasDiscoveryService
import com.matrix.synapse.security.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OAuthModule {

    @Provides
    @Singleton
    fun provideMasClientRegistrar(
        client: OkHttpClient,
        tokenStore: SecureTokenStore,
        json: Json,
    ): MasClientRegistrar = MasClientRegistrar(client, tokenStore, json)

    @Provides
    @Singleton
    fun provideMasAuthCoordinator(
        @ApplicationContext context: Context,
    ): MasAuthCoordinator = MasAuthCoordinator(context)

    @Provides
    @Singleton
    fun provideMasTokenExchanger(
        client: OkHttpClient,
        json: Json,
    ): MasTokenExchanger = MasTokenExchanger(client, json)

    @Provides
    @Singleton
    fun provideDiscoveryPort(
        service: MasDiscoveryService,
    ): DiscoveryPort = MasDiscoveryServiceAdapter(service)
}
