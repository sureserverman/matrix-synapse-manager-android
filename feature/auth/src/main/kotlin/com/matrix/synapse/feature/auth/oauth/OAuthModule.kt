package com.matrix.synapse.feature.auth.oauth

import com.matrix.synapse.security.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
