package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.LogItem
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    /** Everything kept, oldest first. Used for summaries and sharing. */
    suspend fun getLogs(): List<LogItem>

    /** The most recent entries, newest first, re-emitted as they are written. */
    fun observeLogs(): Flow<List<LogItem>>

    suspend fun clear()
}
