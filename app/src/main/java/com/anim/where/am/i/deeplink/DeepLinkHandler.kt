package com.anim.where.am.i.deeplink

import com.anim.where.am.i.data.config.ConfigLinkParser
import com.anim.where.am.i.data.config.ParsedLink
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.usecase.RunTrackerAction
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeepLinkResult {
    data object None : DeepLinkResult
    data object ActionHandled : DeepLinkResult
    data class ConfirmConfig(val link: ConfigLink) : DeepLinkResult
}

@Singleton
class DeepLinkHandler @Inject constructor(
    private val parser: ConfigLinkParser,
    private val runTrackerAction: RunTrackerAction,
) {
    suspend fun handle(raw: String): DeepLinkResult = when (val parsed = parser.parse(raw)) {
        is ParsedLink.Action -> {
            try { runTrackerAction(parsed.action) } catch (_: IllegalStateException) {}
            DeepLinkResult.ActionHandled
        }
        is ParsedLink.Config -> DeepLinkResult.ConfirmConfig(parsed.link)
        null -> DeepLinkResult.None
    }
}
