package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.LogItem

interface LogRepository {
    suspend fun getLogs(): List<LogItem>
    suspend fun clear()
}
