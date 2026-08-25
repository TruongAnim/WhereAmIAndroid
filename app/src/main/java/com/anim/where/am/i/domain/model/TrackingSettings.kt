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
    /** Seconds between repeated detail log entries of the same kind. 0 logs every one. */
    val detailLogSeconds: Int = 5,
    /** Report a record each time the screen turns on or off. */
    val screenEvents: Boolean = true,
    /** Refuse to count a step the fix itself is not accurate enough to prove. */
    val ignoreJitter: Boolean = true,
)
