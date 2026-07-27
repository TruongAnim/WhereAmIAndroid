package com.anim.where.am.i.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class MainUiState(
    val deviceId: String = "",
    val tracking: Boolean = false,
    val message: String? = null,
)

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
            observeStatus().collect { status -> _uiState.update { it.copy(tracking = status.enabled) } }
        }
        viewModelScope.launch {
            observeSettings().collect { s -> _uiState.update { it.copy(deviceId = s.deviceId) } }
        }
    }

    fun onToggleTracking(enable: Boolean) {
        viewModelScope.launch {
            try {
                if (enable) startTracking() else stopTracking()
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(tracking = false, message = e.message) }
            }
        }
    }

    fun onRequestPosition() {
        viewModelScope.launch {
            try { requestSos() } catch (e: IllegalStateException) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
