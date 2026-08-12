package com.anim.where.am.i.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.model.LogLevel
import com.anim.where.am.i.domain.usecase.ClearLogs
import com.anim.where.am.i.domain.usecase.GetLogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val getLogs: GetLogs,
    private val clearLogs: ClearLogs,
) : ViewModel() {

    private val all = MutableStateFlow<List<LogItem>>(emptyList())

    private val _detailMode = MutableStateFlow(false)
    val detailMode: StateFlow<Boolean> = _detailMode.asStateFlow()

    /**
     * Both levels are always loaded; the toggle only changes what is shown.
     * The SDK records detail entries whether or not anyone is watching, so
     * switching the mode on explains what already happened rather than
     * starting a fresh recording.
     */
    val logs: StateFlow<List<LogItem>> = combine(all, _detailMode) { entries, detail ->
        if (detail) entries else entries.filter { it.level == LogLevel.INFO }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detailCount: StateFlow<Int> = all
        .combine(_detailMode) { entries, _ -> entries.count { it.level == LogLevel.DETAIL } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { all.value = getLogs().reversed() }
    }

    fun setDetailMode(enabled: Boolean) {
        _detailMode.value = enabled
    }

    fun clear() {
        viewModelScope.launch {
            clearLogs()
            all.value = emptyList()
        }
    }

    /** Shares what is on screen, so a report matches what the user was reading. */
    fun formatShare(): String = logs.value.joinToString("\n") {
        val marker = if (it.level == LogLevel.DETAIL) " ·" else "  "
        "${fullFormat.format(Date(it.timeMillis))}$marker ${it.message}"
    }
}
