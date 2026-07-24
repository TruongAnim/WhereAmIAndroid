package com.anim.where.am.i.domain.repository

import com.anim.where.am.i.domain.model.TrackingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<TrackingSettings>
    suspend fun save(settings: TrackingSettings)
}
