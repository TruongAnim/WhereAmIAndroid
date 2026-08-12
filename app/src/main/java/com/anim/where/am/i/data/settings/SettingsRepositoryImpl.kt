package com.anim.where.am.i.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.anim.where.am.i.BuildConfig
import com.anim.where.am.i.di.IoDispatcher
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random
import javax.inject.Inject

// Supplied by `whereami.serverUrl` in local.properties; falls back to the
// public Traccar demo server when that is absent. Only applies on a fresh
// install - once a URL is persisted, DataStore wins.
private val DEFAULT_URL = BuildConfig.DEFAULT_SERVER_URL

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {

    override fun observeSettings(): Flow<TrackingSettings> =
        dataStore.data.map { prefs -> prefs.toSettings() }

    override suspend fun save(settings: TrackingSettings) = withContext(io) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.URL] = settings.serverUrl
            prefs[SettingsKeys.ID] = settings.deviceId
            prefs[SettingsKeys.ACCURACY] = accuracyToKey(settings.accuracy)
            prefs[SettingsKeys.DISTANCE] = settings.distanceMeters
            prefs[SettingsKeys.INTERVAL] = settings.intervalSeconds
            prefs[SettingsKeys.ANGLE] = settings.angleDegrees
            prefs[SettingsKeys.HEARTBEAT] = settings.heartbeatSeconds
            prefs[SettingsKeys.BUFFER] = settings.buffer
            prefs[SettingsKeys.WAKELOCK] = settings.wakeLock
            prefs[SettingsKeys.STOP_DETECTION] = settings.stopDetection
            prefs[SettingsKeys.PREFER_PLATFORM] = settings.preferPlatformProviders
            prefs[SettingsKeys.DETAIL_LOG] = settings.detailLogSeconds
        }
        Unit
    }

    private fun Preferences.toSettings(): TrackingSettings {
        val id = this[SettingsKeys.ID] ?: Random.nextInt(10_000_000, 100_000_000).toString()
        return TrackingSettings(
            serverUrl = this[SettingsKeys.URL] ?: DEFAULT_URL,
            deviceId = id,
            accuracy = accuracyFromKey(this[SettingsKeys.ACCURACY]),
            distanceMeters = this[SettingsKeys.DISTANCE] ?: 75,
            intervalSeconds = this[SettingsKeys.INTERVAL] ?: 300,
            angleDegrees = this[SettingsKeys.ANGLE] ?: 0,
            heartbeatSeconds = this[SettingsKeys.HEARTBEAT] ?: 0,
            buffer = this[SettingsKeys.BUFFER] ?: true,
            wakeLock = this[SettingsKeys.WAKELOCK] ?: false,
            stopDetection = this[SettingsKeys.STOP_DETECTION] ?: true,
            preferPlatformProviders = this[SettingsKeys.PREFER_PLATFORM] ?: false,
            detailLogSeconds = this[SettingsKeys.DETAIL_LOG] ?: 5,
        )
    }
}
