package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    fun observeStatus(): Flow<TrackingStatus>
    suspend fun start()
    suspend fun stop()
    suspend fun requestPosition(alarm: String? = null): Boolean
    suspend fun updateConfig(settings: TrackingSettings)
}
