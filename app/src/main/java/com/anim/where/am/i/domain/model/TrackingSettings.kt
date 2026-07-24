package com.anim.where.am.i.domain.model

data class TrackingSettings(
    val serverUrl: String,
    val deviceId: String,
    val accuracy: Accuracy = Accuracy.MEDIUM,
    val distanceMeters: Int = 75,
    val intervalSeconds: Int = 300,
    val angleDegrees: Int = 0,
    val heartbeatSeconds: Int = 0,
    val buffer: Boolean = true,
    val wakeLock: Boolean = false,
    val stopDetection: Boolean = true,
    val preferPlatformProviders: Boolean = false,
)
