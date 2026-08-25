package com.anim.where.am.i.data.tracker

import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import org.traccar.client.Config
import org.traccar.client.LocationConfig
import org.traccar.client.NotificationConfig
import org.traccar.client.Position
import org.traccar.client.State
import com.anim.where.am.i.domain.model.Accuracy as DomainAccuracy
import org.traccar.client.Accuracy as SdkAccuracy

fun DomainAccuracy.toSdk(): SdkAccuracy = when (this) {
    DomainAccuracy.HIGHEST -> SdkAccuracy.HIGHEST
    DomainAccuracy.HIGH -> SdkAccuracy.HIGH
    DomainAccuracy.MEDIUM -> SdkAccuracy.MEDIUM
    DomainAccuracy.LOW -> SdkAccuracy.LOW
}

fun TrackingSettings.toConfig(notificationText: String): Config = Config(
    serverUrl = serverUrl,
    deviceId = deviceId,
    location = LocationConfig(
        accuracy = accuracy.toSdk(),
        distanceMeters = distanceMeters,
        intervalSeconds = intervalSeconds,
        angleDegrees = angleDegrees,
        stopDetection = stopDetection,
        heartbeatIntervalSeconds = heartbeatSeconds,
    ),
    wakeLock = wakeLock,
    buffer = buffer,
    preferPlatformProviders = preferPlatformProviders,
    detailLogIntervalSeconds = detailLogSeconds,
    screenEvents = screenEvents,
    notification = NotificationConfig(text = notificationText),
)

fun Position.toLocationFix(): LocationFix =
    LocationFix(latitude = latitude, longitude = longitude, accuracy = accuracy, timeMillis = time)

fun State.toStatus(): TrackingStatus =
    TrackingStatus(enabled = enabled, paused = paused, lastLocation = lastAcceptedLocation?.toLocationFix())
