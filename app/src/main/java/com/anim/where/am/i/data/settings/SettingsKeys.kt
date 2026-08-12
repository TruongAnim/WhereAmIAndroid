package com.anim.where.am.i.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anim.where.am.i.domain.model.Accuracy

internal object SettingsKeys {
    val URL = stringPreferencesKey("url")
    val ID = stringPreferencesKey("id")
    val ACCURACY = stringPreferencesKey("accuracy")
    val DISTANCE = intPreferencesKey("distance")
    val INTERVAL = intPreferencesKey("interval")
    val ANGLE = intPreferencesKey("angle")
    val HEARTBEAT = intPreferencesKey("heartbeat")
    val BUFFER = booleanPreferencesKey("buffer")
    val WAKELOCK = booleanPreferencesKey("wakelock")
    val STOP_DETECTION = booleanPreferencesKey("stop_detection")
    val PREFER_PLATFORM = booleanPreferencesKey("prefer_platform_providers")
    val DETAIL_LOG = intPreferencesKey("detail_log_seconds")
}

internal fun accuracyToKey(a: Accuracy): String = a.name.lowercase()

internal fun accuracyFromKey(key: String?): Accuracy = when (key) {
    "highest" -> Accuracy.HIGHEST
    "high" -> Accuracy.HIGH
    "low" -> Accuracy.LOW
    else -> Accuracy.MEDIUM
}
