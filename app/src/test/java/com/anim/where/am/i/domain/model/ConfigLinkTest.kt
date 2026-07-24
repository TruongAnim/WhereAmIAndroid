package com.anim.where.am.i.domain.model

import org.junit.Test
import org.junit.Assert.assertEquals

class ConfigLinkTest {
    private val base = TrackingSettings(serverUrl = "https://a", deviceId = "1")

    @Test fun applyTo_overridesOnlyPresentFields() {
        val link = ConfigLink(serverUrl = "https://b", intervalSeconds = 60)
        val result = link.applyTo(base)
        assertEquals("https://b", result.serverUrl)
        assertEquals(60, result.intervalSeconds)
        assertEquals("1", result.deviceId)        // unchanged
        assertEquals(75, result.distanceMeters)   // unchanged default
    }

    @Test fun applyTo_emptyLinkKeepsEverything() {
        assertEquals(base, ConfigLink().applyTo(base))
    }
}
