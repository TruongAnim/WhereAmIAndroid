package com.anim.where.am.i.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.BuildConfig
import com.anim.where.am.i.domain.usecase.DaySummary
import com.anim.where.am.i.domain.usecase.GetActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getActivity: GetActivity,
) : ViewModel() {

    private val _days = MutableStateFlow<List<DaySummary>>(emptyList())
    val days: StateFlow<List<DaySummary>> = _days.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _days.value = getActivity() }
    }

    /** The web map viewer; positions themselves live on the server, not here. */
    fun viewerUrl(): String = BuildConfig.VIEWER_URL
}
