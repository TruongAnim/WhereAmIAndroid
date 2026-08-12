package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.TrackingSettings
import java.net.URLEncoder

class ConfigLinkBuilder {
    fun build(settings: TrackingSettings): String {
        val params = listOf(
            "url" to settings.serverUrl,
            "id" to settings.deviceId,
            "accuracy" to accuracyKey(settings.accuracy),
            "distance" to settings.distanceMeters.toString(),
            "interval" to settings.intervalSeconds.toString(),
            "angle" to settings.angleDegrees.toString(),
            "heartbeat" to settings.heartbeatSeconds.toString(),
            "buffer" to settings.buffer.toString(),
            "wakelock" to settings.wakeLock.toString(),
            "stop_detection" to settings.stopDetection.toString(),
            "prefer_platform_providers" to settings.preferPlatformProviders.toString(),
            "detail_log" to settings.detailLogSeconds.toString(),
        )
        val query = params.joinToString("&") { (k, v) -> "$k=${enc(v)}" }
        return "whereami://config?$query"
    }

    private fun accuracyKey(a: Accuracy): String = a.name.lowercase()
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
