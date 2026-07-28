package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ApplyConfigLink @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val saveSettings: SaveSettings,
) {
    suspend operator fun invoke(link: ConfigLink) {
        val current = settingsRepo.observeSettings().first()
        saveSettings(link.applyTo(current))
    }
}
