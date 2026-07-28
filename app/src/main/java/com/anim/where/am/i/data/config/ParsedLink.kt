package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.ConfigLink as DomainConfigLink
import com.anim.where.am.i.domain.model.TrackerAction

sealed interface ParsedLink {
    data class Action(val action: TrackerAction) : ParsedLink
    data class Config(val link: DomainConfigLink) : ParsedLink
}
