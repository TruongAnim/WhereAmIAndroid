package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLogs @Inject constructor(private val repo: LogRepository) {
    suspend operator fun invoke(): List<LogItem> = repo.getLogs()
}

class ObserveLogs @Inject constructor(private val repo: LogRepository) {
    operator fun invoke(): Flow<List<LogItem>> = repo.observeLogs()
}

class ClearLogs @Inject constructor(private val repo: LogRepository) {
    suspend operator fun invoke() = repo.clear()
}
