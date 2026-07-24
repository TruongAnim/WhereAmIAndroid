package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import com.anim.where.am.i.domain.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class SaveSettingsTest {
    private var saved: TrackingSettings? = null
    private var pushed: TrackingSettings? = null

    private val settingsRepo = object : SettingsRepository {
        override fun observeSettings(): Flow<TrackingSettings> = MutableStateFlow(base)
        override suspend fun save(settings: TrackingSettings) { saved = settings }
    }
    private val trackerRepo = object : TrackerRepository {
        override fun observeStatus(): Flow<TrackingStatus> = MutableStateFlow(TrackingStatus())
        override suspend fun start() {}
        override suspend fun stop() {}
        override suspend fun requestPosition(alarm: String?) = true
        override suspend fun updateConfig(settings: TrackingSettings) { pushed = settings }
    }
    private val base = TrackingSettings(serverUrl = "https://a", deviceId = "1")

    @Test fun clampsHeartbeatBelowMinimum() = runTest {
        SaveSettings(settingsRepo, trackerRepo)(base.copy(heartbeatSeconds = 30))
        assertEquals(60, saved?.heartbeatSeconds)
        assertEquals(60, pushed?.heartbeatSeconds)
    }

    @Test fun keepsZeroHeartbeat() = runTest {
        SaveSettings(settingsRepo, trackerRepo)(base.copy(heartbeatSeconds = 0))
        assertEquals(0, saved?.heartbeatSeconds)
    }
}
