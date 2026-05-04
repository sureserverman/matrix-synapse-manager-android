package com.matrix.synapse.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.matrix.synapse.network.auth.MasReauthenticator
import com.matrix.synapse.network.auth.MasTokenRefresher
import com.matrix.synapse.security.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthHeaderInterceptor =
        AuthHeaderInterceptor { tokenProvider.currentToken() }

    /**
     * A bare OkHttpClient for token refresh calls — no auth interceptor, no authenticator,
     * to avoid an infinite 401 loop if MAS itself returns 401 on the refresh endpoint.
     */
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideMasTokenRefresher(
        @Named("refresh") client: OkHttpClient,
        tokenStore: SecureTokenStore,
        activeTokenHolder: ActiveTokenHolder,
        json: Json,
    ): MasTokenRefresher = MasTokenRefresher(client, tokenStore, activeTokenHolder, json)

    @Provides
    @Singleton
    fun provideMasReauthenticator(
        refresher: MasTokenRefresher,
        activeTokenHolder: ActiveTokenHolder,
    ): MasReauthenticator = MasReauthenticator(refresher, activeTokenHolder)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthHeaderInterceptor,
        authenticator: MasReauthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
