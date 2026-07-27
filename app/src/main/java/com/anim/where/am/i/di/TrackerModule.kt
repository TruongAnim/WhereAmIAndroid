package com.anim.where.am.i.di

import android.content.Context
import com.anim.where.am.i.R
import com.anim.where.am.i.data.log.LogRepositoryImpl
import com.anim.where.am.i.data.tracker.TrackerRepositoryImpl
import com.anim.where.am.i.data.tracker.toConfig
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.LogRepository
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.traccar.client.sharedTracker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackerModule {

    // Single shared bootstrap snapshot: read the persisted (or freshly-seeded) settings once
    // and persist them once, so every consumer (tracker + log repositories) derives the same
    // deviceId regardless of which one resolves first.
    @Provides @Singleton
    fun bootstrapSettings(settingsRepository: SettingsRepository): TrackingSettings {
        val settings = runBlocking { settingsRepository.observeSettings().first() }
        runBlocking { settingsRepository.save(settings) } // persist seeded random deviceId once
        return settings
    }

    @Provides @Singleton
    fun trackerRepository(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
        bootstrapSettings: TrackingSettings,
    ): TrackerRepository = TrackerRepositoryImpl(
        context = context,
        io = io,
        notificationText = context.getString(R.string.notification_text),
        bootstrapSettings = bootstrapSettings,
    )

    @Provides @Singleton
    fun logRepository(
        @ApplicationContext context: Context,
        bootstrapSettings: TrackingSettings,
    ): LogRepository = LogRepositoryImpl(
        trackerProvider = {
            sharedTracker() ?: sharedTracker(bootstrapSettings.toConfig(context.getString(R.string.notification_text)))
        },
    )
}
