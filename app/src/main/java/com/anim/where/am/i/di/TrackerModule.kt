package com.anim.where.am.i.di

import android.content.Context
import com.anim.where.am.i.R
import com.anim.where.am.i.data.log.LogRepositoryImpl
import com.anim.where.am.i.data.tracker.TrackerRepositoryImpl
import com.anim.where.am.i.data.tracker.toConfig
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

    @Provides @Singleton
    fun trackerRepository(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
        settingsRepository: SettingsRepository,
    ): TrackerRepository {
        val bootstrap = runBlocking { settingsRepository.observeSettings().first() }
        // Persist once so the seeded random deviceId is stored.
        runBlocking { settingsRepository.save(bootstrap) }
        return TrackerRepositoryImpl(
            context = context,
            io = io,
            notificationText = context.getString(R.string.notification_text),
            bootstrapSettings = bootstrap,
        )
    }

    @Provides @Singleton
    fun logRepository(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
    ): LogRepository = LogRepositoryImpl(
        trackerProvider = {
            sharedTracker() ?: run {
                val s = settingsRepository.observeSettings().first()
                sharedTracker(s.toConfig(context.getString(R.string.notification_text)))
            }
        },
    )
}
