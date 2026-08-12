package com.anim.where.am.i.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Insets for a screen that sits inside the tab shell.
 *
 * The shell's Scaffold already reserves room for the navigation bar, so a tab
 * must claim the top and sides only. Letting it apply the full safe-drawing
 * insets would pad the bottom twice, and the status bar would be inset once by
 * the shell and again by the tab's own top app bar.
 */
val TabWindowInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
    )
