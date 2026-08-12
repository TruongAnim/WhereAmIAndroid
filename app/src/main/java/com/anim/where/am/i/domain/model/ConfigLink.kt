package com.anim.where.am.i.domain.model

data class ConfigLink(
    val serverUrl: String? = null,
    val deviceId: String? = null,
    val accuracy: Accuracy? = null,
    val distanceMeters: Int? = null,
    val intervalSeconds: Int? = null,
    val angleDegrees: Int? = null,
    val heartbeatSeconds: Int? = null,
    val buffer: Boolean? = null,
    val wakeLock: Boolean? = null,
    val stopDetection: Boolean? = null,
    val preferPlatformProviders: Boolean? = null,
    val detailLogSeconds: Int? = null,
) {
    fun applyTo(settings: TrackingSettings): TrackingSettings = settings.copy(
        serverUrl = serverUrl ?: settings.serverUrl,
        deviceId = deviceId ?: settings.deviceId,
        accuracy = accuracy ?: settings.accuracy,
        distanceMeters = distanceMeters ?: settings.distanceMeters,
        intervalSeconds = intervalSeconds ?: settings.intervalSeconds,
        angleDegrees = angleDegrees ?: settings.angleDegrees,
        heartbeatSeconds = heartbeatSeconds ?: settings.heartbeatSeconds,
        buffer = buffer ?: settings.buffer,
        wakeLock = wakeLock ?: settings.wakeLock,
        stopDetection = stopDetection ?: settings.stopDetection,
        preferPlatformProviders = preferPlatformProviders ?: settings.preferPlatformProviders,
        detailLogSeconds = detailLogSeconds ?: settings.detailLogSeconds,
    )
}
