package com.anim.where.am.i.data.tracker

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.traccar.client.Accuracy as SdkAccuracy

class ConfigMapperTest {
    @Test fun mapsCoreFields() {
        val settings = TrackingSettings(
            serverUrl = "https://x", deviceId = "42",
            accuracy = Accuracy.HIGH, distanceMeters = 10, intervalSeconds = 120,
            angleDegrees = 5, heartbeatSeconds = 60, buffer = false, wakeLock = true,
            stopDetection = false, preferPlatformProviders = true,
        )
        val config = settings.toConfig("Tracking")
        assertEquals("https://x", config.serverUrl)
        assertEquals("42", config.deviceId)
        assertEquals(SdkAccuracy.HIGH, config.location.accuracy)
        assertEquals(10, config.location.distanceMeters)
        assertEquals(120, config.location.intervalSeconds)
        assertEquals(5, config.location.angleDegrees)
        assertEquals(60, config.location.heartbeatIntervalSeconds)
        assertEquals(false, config.location.stopDetection)
        assertEquals(false, config.buffer)
        assertEquals(true, config.wakeLock)
        assertEquals(true, config.preferPlatformProviders)
        assertEquals("Tracking", config.notification.text)
    }
}
