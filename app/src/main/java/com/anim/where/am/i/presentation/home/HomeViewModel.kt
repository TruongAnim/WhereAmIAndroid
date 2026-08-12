package com.anim.where.am.i.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.R
import com.anim.where.am.i.data.session.SessionStore
import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.domain.usecase.GetActivity
import com.anim.where.am.i.domain.usecase.ObserveSettings
import com.anim.where.am.i.domain.usecase.ObserveTrackingStatus
import com.anim.where.am.i.domain.usecase.RequestSos
import com.anim.where.am.i.domain.usecase.StartTracking
import com.anim.where.am.i.domain.usecase.StopTracking
import com.anim.where.am.i.domain.usecase.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

enum class TrackingPhase { OFF, LIVE, PAUSED }

data class HomeUiState(
    val deviceId: String = "",
    val serverUrl: String = "",
    val tracking: Boolean = false,
    val paused: Boolean = false,
    val lastFix: LocationFix? = null,
    val startedAtMillis: Long? = null,
    val elapsedMillis: Long = 0L,
    val uploadsToday: Int = 0,
    val busy: Boolean = false,
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
class HomeViewModel @Inject constructor(
    observeStatus: ObserveTrackingStatus,
    observeSettings: ObserveSettings,
    private val sessionStore: SessionStore,
    private val getActivity: GetActivity,
    private val startTracking: StartTracking,
    private val stopTracking: StopTracking,
    private val requestSos: RequestSos,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
                // Tracking survives the app being killed, so reconcile the
                // stored session with whatever the SDK reports on launch.
                if (status.enabled) sessionStore.markStarted() else sessionStore.markStopped()
            }
        }
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update {
                    it.copy(deviceId = settings.deviceId, serverUrl = settings.serverUrl)
                }
            }
        }
        viewModelScope.launch {
            sessionStore.observeStartedAt().collect { startedAt ->
                _uiState.update { it.copy(startedAtMillis = startedAt) }
            }
        }
        viewModelScope.launch { tick() }
    }

    private suspend fun tick() {
        while (coroutineContext.isActive) {
            val startedAt = _uiState.value.startedAtMillis
            _uiState.update {
                it.copy(
                    elapsedMillis = if (startedAt != null) {
                        (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                    } else {
                        0L
                    },
                )
            }
            delay(1000)
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val today = startOfDay(System.currentTimeMillis())
            val uploads = getActivity().firstOrNull { it.startOfDayMillis == today }?.uploads ?: 0
            _uiState.update { it.copy(uploadsToday = uploads) }
        }
    }

    fun onToggleTracking() {
        if (_uiState.value.busy) return
        val enable = !_uiState.value.tracking
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (enable) startTracking() else stopTracking()
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(tracking = false, messageRes = R.string.permission_denied) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun onRequestPosition() {
        if (_uiState.value.sosInFlight) return
        _uiState.update { it.copy(sosInFlight = true) }
        viewModelScope.launch {
            val messageRes = try {
                if (requestSos()) R.string.sos_sent else R.string.sos_failed
            } catch (e: IllegalStateException) {
                R.string.permission_denied
            }
            _uiState.update { it.copy(sosInFlight = false, messageRes = messageRes) }
            refreshStats()
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(messageRes = null) }
}
