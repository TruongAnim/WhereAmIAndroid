package com.anim.where.am.i.domain.model

data class TrackingStatus(
    val enabled: Boolean = false,
    val paused: Boolean = false,
    val lastLocation: LocationFix? = null,
)
