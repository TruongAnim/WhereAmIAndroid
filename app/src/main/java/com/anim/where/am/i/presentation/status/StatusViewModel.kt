package com.anim.where.am.i.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.model.LogLevel
import com.anim.where.am.i.domain.usecase.ClearLogs
import com.anim.where.am.i.domain.usecase.ObserveLogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    observeLogs: ObserveLogs,
    private val clearLogs: ClearLogs,
) : ViewModel() {

    /**
     * Backed by a database query that re-emits on every write, so the screen
     * follows the tracker without polling. WhileSubscribed stops the query
     * once nothing is watching - the log is written constantly, and an
     * unobserved listener would re-read it for nobody.
     */
    private val all: StateFlow<List<LogItem>> = observeLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _detailMode = MutableStateFlow(false)
    val detailMode: StateFlow<Boolean> = _detailMode.asStateFlow()

    /** Both levels stream in; the toggle only changes what is shown. */
    val logs: StateFlow<List<LogItem>> = combine(all, _detailMode) { entries, detail ->
        if (detail) entries else entries.filter { it.level == LogLevel.INFO }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detailCount: StateFlow<Int> = all
        .map { entries -> entries.count { it.level == LogLevel.DETAIL } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun setDetailMode(enabled: Boolean) {
        _detailMode.value = enabled
    }

    fun clear() {
        viewModelScope.launch { clearLogs() }
    }

    /** Shares what is on screen, so a report matches what the user was reading. */
    fun formatShare(): String = logs.value.joinToString("\n") {
        val marker = if (it.level == LogLevel.DETAIL) " ·" else "  "
        "${fullFormat.format(Date(it.timeMillis))}$marker ${it.message}"
    }
}
