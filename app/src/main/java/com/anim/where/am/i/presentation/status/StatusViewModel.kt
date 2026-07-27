package com.anim.where.am.i.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.usecase.ClearLogs
import com.anim.where.am.i.domain.usecase.GetLogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()
    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _logs.value = getLogs().reversed() }
    }

    fun clear() {
        viewModelScope.launch { clearLogs(); _logs.value = emptyList() }
    }

    fun formatShare(): String = _logs.value.joinToString("\n") {
        "${fullFormat.format(Date(it.timeMillis))} ${it.message}"
    }
}
