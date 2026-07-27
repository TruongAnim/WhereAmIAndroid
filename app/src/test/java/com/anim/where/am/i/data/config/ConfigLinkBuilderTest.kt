package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigLinkBuilderTest {
    @Test fun buildRoundTripsThroughParser() {
        val settings = TrackingSettings(
            serverUrl = "https://s", deviceId = "7", accuracy = Accuracy.LOW,
            distanceMeters = 50, intervalSeconds = 90, angleDegrees = 3,
            heartbeatSeconds = 60, buffer = false, wakeLock = true,
            stopDetection = false, preferPlatformProviders = true,
        )
        val uri = ConfigLinkBuilder().build(settings)
        val parsed = ConfigLinkParser().parse(uri) as ParsedLink.Config
        val applied = parsed.link.applyTo(TrackingSettings(serverUrl = "x", deviceId = "0"))
        assertEquals(settings, applied)
    }
}
