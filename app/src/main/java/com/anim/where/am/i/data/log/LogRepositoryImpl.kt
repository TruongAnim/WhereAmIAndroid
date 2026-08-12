package com.anim.where.am.i.data.log

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    // flow { } defers resolving the tracker until collection, so observing
    // does not force the SDK to start up before anything asks to watch.
    override fun observeLogs(): Flow<List<LogItem>> = flow {
        emitAll(trackerProvider().observeLogs().map { entries -> entries.map { it.toLogItem() } })
    }

    override suspend fun clear() {
        trackerProvider().clearLogs()
    }
}
