package com.anim.where.am.i.presentation.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anim.where.am.i.data.config.ConfigLinkBuilder
import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.config.ParsedLink
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.usecase.ApplyConfigLink
import com.anim.where.am.i.domain.usecase.ObserveSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(
    private val parser: ConfigLinkParser,
    private val builder: ConfigLinkBuilder,
    private val applyConfigLink: ApplyConfigLink,
    private val observeSettings: ObserveSettings,
) : ViewModel() {

    private val _shareUri = MutableStateFlow<String?>(null)
    val shareUri: StateFlow<String?> = _shareUri.asStateFlow()

    init {
        viewModelScope.launch { _shareUri.value = builder.build(observeSettings().first()) }
    }

    /** Returns the parsed ConfigLink for a confirmation dialog, or null if not a config link. */
    fun parseConfig(raw: String): ConfigLink? =
        (parser.parse(raw) as? ParsedLink.Config)?.link

    fun applyConfig(link: ConfigLink) {
        viewModelScope.launch { applyConfigLink(link) }
    }
}
