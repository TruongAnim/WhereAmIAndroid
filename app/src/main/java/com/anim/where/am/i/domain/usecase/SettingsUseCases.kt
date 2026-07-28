package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.repository.SettingsRepository
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettings @Inject constructor(private val repo: SettingsRepository) {
    operator fun invoke(): Flow<TrackingSettings> = repo.observeSettings()
}

class SaveSettings @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val trackerRepo: TrackerRepository,
) {
    suspend operator fun invoke(settings: TrackingSettings) {
        val normalized = settings.copy(
            heartbeatSeconds = if (settings.heartbeatSeconds in 1..59) 60 else settings.heartbeatSeconds,
        )
        settingsRepo.save(normalized)
        trackerRepo.updateConfig(normalized)
    }
}
