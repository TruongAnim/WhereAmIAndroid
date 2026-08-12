package com.anim.where.am.i.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.R
import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.domain.usecase.ObserveSettings
import com.anim.where.am.i.domain.usecase.ObserveTrackingStatus
import com.anim.where.am.i.domain.usecase.RequestSos
import com.anim.where.am.i.domain.usecase.StartTracking
import com.anim.where.am.i.domain.usecase.StopTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrackingPhase { OFF, LIVE, PAUSED }

data class MainUiState(
    val deviceId: String = "",
    val serverUrl: String = "",
    val tracking: Boolean = false,
    val paused: Boolean = false,
    val lastFix: LocationFix? = null,
    val sosInFlight: Boolean = false,
    val messageRes: Int? = null,
) {
    val phase: TrackingPhase
        get() = when {
            !tracking -> TrackingPhase.OFF
            paused -> TrackingPhase.PAUSED
            else -> TrackingPhase.LIVE
        }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    observeStatus: ObserveTrackingStatus,
    observeSettings: ObserveSettings,
    private val startTracking: StartTracking,
    private val stopTracking: StopTracking,
    private val requestSos: RequestSos,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        tracking = status.enabled,
                        paused = status.paused,
                        lastFix = status.lastLocation ?: it.lastFix,
                    )
                }
            }
        }
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update {
                    it.copy(deviceId = settings.deviceId, serverUrl = settings.serverUrl)
                }
            }
        }
    }

    fun onToggleTracking(enable: Boolean) {
        viewModelScope.launch {
            try {
                if (enable) startTracking() else stopTracking()
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(tracking = false, messageRes = R.string.permission_denied) }
            }
        }
    }

    fun onRequestPosition() {
        if (_uiState.value.sosInFlight) return
        _uiState.update { it.copy(sosInFlight = true) }
        viewModelScope.launch {
            val messageRes = try {
                // requestPosition() bypasses the queue and uploads immediately,
                // so its result is worth reporting rather than discarding.
                if (requestSos()) R.string.sos_sent else R.string.sos_failed
            } catch (e: IllegalStateException) {
                R.string.permission_denied
            }
            _uiState.update { it.copy(sosInFlight = false, messageRes = messageRes) }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(messageRes = null) }
}
