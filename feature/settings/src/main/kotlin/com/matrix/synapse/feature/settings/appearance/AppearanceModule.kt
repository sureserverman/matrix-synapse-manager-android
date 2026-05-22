package com.matrix.synapse.feature.settings.appearance

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppearanceModule {
    @Binds
    @Singleton
    abstract fun bindThemeModeRepository(impl: DefaultThemeModeRepository): ThemeModeRepository
}
