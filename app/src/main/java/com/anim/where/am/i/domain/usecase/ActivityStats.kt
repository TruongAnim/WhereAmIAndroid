package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import java.util.Calendar
import javax.inject.Inject

/** One day's worth of activity, newest day first. */
data class DaySummary(
    val startOfDayMillis: Long,
    val uploads: Int,
    val fixes: Int,
    val alarms: Int,
    val errors: Int,
    val firstEventMillis: Long,
    val lastEventMillis: Long,
)

private val UPLOAD_OK = Regex("""upload response 2\d\d""")

/**
 * Derives activity from the SDK's own log rather than from a second copy of
 * the data. The log is capped by the SDK, so older days fall off - that is
 * accurate for "recent activity" and avoids the app storing positions twice.
 */
class GetActivity @Inject constructor(private val repo: LogRepository) {

    suspend operator fun invoke(): List<DaySummary> {
        val entries = repo.getLogs()
        if (entries.isEmpty()) return emptyList()

        return entries
            .groupBy { startOfDay(it.timeMillis) }
            .map { (day, items) -> summarise(day, items) }
            .sortedByDescending { it.startOfDayMillis }
    }

    private fun summarise(day: Long, items: List<LogItem>): DaySummary {
        var uploads = 0
        var fixes = 0
        var alarms = 0
        var errors = 0
        for (item in items) {
            val text = item.message.lowercase()
            when {
                UPLOAD_OK.containsMatchIn(text) -> uploads++
                "accepted" in text -> fixes++
            }
            if ("alarm=sos" in text || "position requested" in text) alarms++
            if ("error" in text || "failed" in text || "denied" in text) errors++
        }
        return DaySummary(
            startOfDayMillis = day,
            uploads = uploads,
            fixes = fixes,
            alarms = alarms,
            errors = errors,
            firstEventMillis = items.minOf { it.timeMillis },
            lastEventMillis = items.maxOf { it.timeMillis },
        )
    }
}

internal fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
