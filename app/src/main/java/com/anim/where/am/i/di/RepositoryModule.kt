package com.anim.where.am.i.di

import com.anim.where.am.i.data.config.ConfigLinkBuilder
import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.settings.SettingsRepositoryImpl
import com.anim.where.am.i.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides @Singleton fun configLinkParser() = ConfigLinkParser()
        @Provides @Singleton fun configLinkBuilder() = ConfigLinkBuilder()
    }
}
