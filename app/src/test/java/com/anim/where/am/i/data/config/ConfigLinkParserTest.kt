package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackerAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigLinkParserTest {
    private val parser = ConfigLinkParser()

    @Test fun parsesStartAction() {
        val r = parser.parse("whereami://action/start")
        assertEquals(ParsedLink.Action(TrackerAction.START), r)
    }

    @Test fun parsesSosAction() {
        assertEquals(ParsedLink.Action(TrackerAction.SOS), parser.parse("whereami://action/sos"))
    }

    @Test fun parsesStopAction() {
        assertEquals(ParsedLink.Action(TrackerAction.STOP), parser.parse("whereami://action/stop"))
    }

    @Test fun parsesHttpUrlAsServer() {
        val r = parser.parse("https://server.example.com:8082/path?id=99&accuracy=high") as ParsedLink.Config
        assertEquals("https://server.example.com:8082/path", r.link.serverUrl)
        assertEquals("99", r.link.deviceId)
        assertEquals(Accuracy.HIGH, r.link.accuracy)
    }

    @Test fun parsesHttpUrlWithUnparseableHostAsServer() {
        val r = parser.parse("http://my_host.example.com/path") as ParsedLink.Config
        assertEquals("http://my_host.example.com/path", r.link.serverUrl)
    }

    @Test fun parsesHttpUrlWithNoPortAndNoPath() {
        val r = parser.parse("https://example.com") as ParsedLink.Config
        assertEquals("https://example.com", r.link.serverUrl)
    }

    @Test fun parsesConfigSchemeWithParams() {
        val r = parser.parse(
            "whereami://config?url=https://s&id=7&distance=50&interval=90&angle=3" +
                "&heartbeat=60&buffer=false&wakelock=true&stop_detection=false&prefer_platform_providers=true"
        ) as ParsedLink.Config
        val link = r.link
        assertEquals("https://s", link.serverUrl)
        assertEquals("7", link.deviceId)
        assertEquals(50, link.distanceMeters)
        assertEquals(90, link.intervalSeconds)
        assertEquals(3, link.angleDegrees)
        assertEquals(60, link.heartbeatSeconds)
        assertEquals(false, link.buffer)
        assertEquals(true, link.wakeLock)
        assertEquals(false, link.stopDetection)
        assertEquals(true, link.preferPlatformProviders)
    }

    @Test fun ignoresBadBooleanAndInt() {
        val r = parser.parse("whereami://config?buffer=maybe&distance=xyz") as ParsedLink.Config
        assertNull(r.link.buffer)
        assertNull(r.link.distanceMeters)
    }

    @Test fun returnsNullForGarbage() {
        assertNull(parser.parse("not a uri at all ::: %%%"))
    }

    @Test fun unknownActionIsNull() {
        assertTrue(parser.parse("whereami://action/dance") == null)
    }
}
