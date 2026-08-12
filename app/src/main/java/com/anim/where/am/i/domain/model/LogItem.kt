package com.anim.where.am.i.domain.model

/**
 * [INFO] is what the tracker did; [DETAIL] is why, including the decisions
 * that led to nothing being sent. The log screen shows INFO alone until the
 * user asks for the rest.
 */
enum class LogLevel { INFO, DETAIL }

data class LogItem(
    val timeMillis: Long,
    val message: String,
    val level: LogLevel = LogLevel.INFO,
)
