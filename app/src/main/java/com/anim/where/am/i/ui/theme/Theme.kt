package com.anim.where.am.i.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }

@Composable
fun WhereAmITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Off by default: the app has its own identity, and wallpaper-derived
     * colours would fight the live/paused/idle states that carry meaning here.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkScheme
        else -> LightScheme
    }

    CompositionLocalProvider(
        LocalStatusPalette provides if (darkTheme) DarkStatusPalette else LightStatusPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
