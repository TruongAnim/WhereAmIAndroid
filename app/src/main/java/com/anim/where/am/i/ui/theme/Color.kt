package com.anim.where.am.i.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A teal-leaning scheme: the app is about a signal coming back from a device,
// and teal reads as instrument rather than as decoration.

val LightScheme = lightColorScheme(
    primary = Color(0xFF006877),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5EEFF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF4A6268),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EE),
    onSecondaryContainer = Color(0xFF051F24),
    tertiary = Color(0xFF545D7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDAE1FF),
    onTertiaryContainer = Color(0xFF101A37),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFC),
    onBackground = Color(0xFF171C1E),
    surface = Color(0xFFF5FAFC),
    onSurface = Color(0xFF171C1E),
    surfaceVariant = Color(0xFFDBE4E7),
    onSurfaceVariant = Color(0xFF3F484B),
    outline = Color(0xFF6F797B),
    outlineVariant = Color(0xFFBFC8CB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4F7),
    surfaceContainer = Color(0xFFE9EFF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E6),
    inverseSurface = Color(0xFF2B3134),
    inverseOnSurface = Color(0xFFECF2F4),
    inversePrimary = Color(0xFF85D2E4),
    scrim = Color(0xFF000000),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF85D2E4),
    onPrimary = Color(0xFF003641),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFA5EEFF),
    secondary = Color(0xFFB1CBD2),
    onSecondary = Color(0xFF1C3439),
    secondaryContainer = Color(0xFF334A50),
    onSecondaryContainer = Color(0xFFCDE7EE),
    tertiary = Color(0xFFBDC5EA),
    onTertiary = Color(0xFF262F4D),
    tertiaryContainer = Color(0xFF3D4665),
    onTertiaryContainer = Color(0xFFDAE1FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1416),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0E1416),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF3F484B),
    onSurfaceVariant = Color(0xFFBFC8CB),
    outline = Color(0xFF899295),
    outlineVariant = Color(0xFF3F484B),
    surfaceContainerLowest = Color(0xFF090F11),
    surfaceContainerLow = Color(0xFF171C1E),
    surfaceContainer = Color(0xFF1B2122),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303638),
    inverseSurface = Color(0xFFDEE3E6),
    inverseOnSurface = Color(0xFF2B3134),
    inversePrimary = Color(0xFF006877),
    scrim = Color(0xFF000000),
)

/**
 * Tracking has three states the Material roles cannot express on their own:
 * running, paused by stop detection, and off. Each gets its own container so
 * the hero card can say which one it is by colour alone.
 */
data class StatusPalette(
    val liveContainer: Color,
    val onLiveContainer: Color,
    val live: Color,
    val pausedContainer: Color,
    val onPausedContainer: Color,
    val paused: Color,
    val idleContainer: Color,
    val onIdleContainer: Color,
    val idle: Color,
    // Content sitting directly on the accent. Dark theme accents are light,
    // so white would fail contrast there.
    val onAccent: Color,
)

val LightStatusPalette = StatusPalette(
    liveContainer = Color(0xFFB7F0C6),
    onLiveContainer = Color(0xFF04210F),
    live = Color(0xFF116A38),
    pausedContainer = Color(0xFFFFDF9B),
    onPausedContainer = Color(0xFF261A00),
    paused = Color(0xFF7A5900),
    idleContainer = Color(0xFFDBE4E7),
    onIdleContainer = Color(0xFF171C1E),
    idle = Color(0xFF5A6467),
    onAccent = Color(0xFFFFFFFF),
)

val DarkStatusPalette = StatusPalette(
    liveContainer = Color(0xFF0B4A28),
    onLiveContainer = Color(0xFFB7F0C6),
    live = Color(0xFF7BDBA0),
    pausedContainer = Color(0xFF4F3B00),
    onPausedContainer = Color(0xFFFFDF9B),
    paused = Color(0xFFF5C242),
    idleContainer = Color(0xFF252B2D),
    onIdleContainer = Color(0xFFDEE3E6),
    idle = Color(0xFF9AA4A7),
    onAccent = Color(0xFF0E1416),
)
