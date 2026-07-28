package com.anim.where.am.i.data.log

import org.junit.Assert.assertEquals
import org.junit.Test
import org.traccar.client.LogEntry

class LogMapperTest {
    @Test fun mapsFields() {
        val item = LogEntry(time = 123L, message = "hello").toLogItem()
        assertEquals(123L, item.timeMillis)
        assertEquals("hello", item.message)
    }
}
