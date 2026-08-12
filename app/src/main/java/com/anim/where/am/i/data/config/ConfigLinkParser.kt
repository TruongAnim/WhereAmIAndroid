package com.anim.where.am.i.data.config

import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.model.TrackerAction
import java.net.URI
import java.net.URLDecoder

class ConfigLinkParser {

    fun parse(raw: String): ParsedLink? {
        val uri = try { URI(raw.trim()) } catch (_: Exception) { return null }
        val scheme = uri.scheme?.lowercase() ?: return null

        if (scheme == "whereami" && uri.host?.lowercase() == "action") {
            val action = when (uri.path.trim('/').lowercase()) {
                "start" -> TrackerAction.START
                "stop" -> TrackerAction.STOP
                "sos" -> TrackerAction.SOS
                else -> return null
            }
            return ParsedLink.Action(action)
        }

        val params = parseQuery(uri.rawQuery)
        val serverUrl = when (scheme) {
            "http", "https" -> buildString {
                append(scheme).append("://").append(uri.authority ?: uri.host ?: "")
                if (!uri.path.isNullOrEmpty()) append(uri.path)
            }
            else -> params["url"]
        }

        return ParsedLink.Config(
            ConfigLink(
                serverUrl = serverUrl,
                deviceId = params["id"],
                accuracy = params["accuracy"]?.let(::accuracyOrNull),
                distanceMeters = params["distance"]?.toIntOrNull(),
                intervalSeconds = params["interval"]?.toIntOrNull(),
                angleDegrees = params["angle"]?.toIntOrNull(),
                heartbeatSeconds = params["heartbeat"]?.toIntOrNull(),
                buffer = params["buffer"]?.let(::boolOrNull),
                wakeLock = params["wakelock"]?.let(::boolOrNull),
                stopDetection = params["stop_detection"]?.let(::boolOrNull),
                preferPlatformProviders = params["prefer_platform_providers"]?.let(::boolOrNull),
                detailLogSeconds = params["detail_log"]?.toIntOrNull(),
            ),
        )
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        return rawQuery.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = decode(pair.substring(0, idx))
            val value = decode(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    private fun decode(s: String): String =
        try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    private fun accuracyOrNull(v: String): Accuracy? = when (v.lowercase()) {
        "highest" -> Accuracy.HIGHEST
        "high" -> Accuracy.HIGH
        "medium" -> Accuracy.MEDIUM
        "low" -> Accuracy.LOW
        else -> null
    }

    private fun boolOrNull(v: String): Boolean? = when (v) {
        "true" -> true
        "false" -> false
        else -> null
    }
}
