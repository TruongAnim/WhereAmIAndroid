package com.anim.where.am.i.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.usecase.ObserveSettings
import com.anim.where.am.i.domain.usecase.SaveSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettings: ObserveSettings,
    private val saveSettings: SaveSettings,
) : ViewModel() {

    private val _settings = MutableStateFlow<TrackingSettings?>(null)
    val settings: StateFlow<TrackingSettings?> = _settings.asStateFlow()

    private val _urlError = MutableStateFlow(false)
    val urlError: StateFlow<Boolean> = _urlError.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _settings.value = observeSettings().first()
            _urlError.value = false
        }
    }

    fun update(transform: (TrackingSettings) -> TrackingSettings) {
        _settings.value = _settings.value?.let(transform)
        _urlError.value = false
    }

    fun save(onDone: () -> Unit) {
        val current = _settings.value ?: return
        if (!isValidUrl(current.serverUrl)) { _urlError.value = true; return }
        viewModelScope.launch { saveSettings(current); onDone() }
    }

    private fun isValidUrl(url: String): Boolean =
        url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
}
