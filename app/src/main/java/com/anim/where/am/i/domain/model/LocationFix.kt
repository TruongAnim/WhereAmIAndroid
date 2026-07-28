package com.anim.where.am.i.domain.model

data class LocationFix(
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Double?,
    val timeMillis: Long,
)
