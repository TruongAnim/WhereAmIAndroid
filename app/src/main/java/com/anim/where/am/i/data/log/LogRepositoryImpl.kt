package com.anim.where.am.i.data.log

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import org.traccar.client.LogEntry
import org.traccar.client.Tracker
import com.anim.where.am.i.domain.model.LogLevel as DomainLogLevel
import org.traccar.client.LogLevel as SdkLogLevel

fun LogEntry.toLogItem(): LogItem = LogItem(
    timeMillis = time,
    message = message,
    level = when (level) {
        SdkLogLevel.DETAIL -> DomainLogLevel.DETAIL
        SdkLogLevel.INFO -> DomainLogLevel.INFO
    },
)

class LogRepositoryImpl(
    private val trackerProvider: suspend () -> Tracker,
) : LogRepository {
    override suspend fun getLogs(): List<LogItem> =
        trackerProvider().getLogs().map { it.toLogItem() }

    override suspend fun clear() {
        trackerProvider().clearLogs()
    }
}
