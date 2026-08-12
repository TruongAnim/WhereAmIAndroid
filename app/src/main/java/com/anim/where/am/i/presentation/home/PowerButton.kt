package com.anim.where.am.i.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anim.where.am.i.R
import com.anim.where.am.i.ui.theme.LocalStatusPalette

private const val RING_COUNT = 3

/**
 * The single control the home screen is built around.
 *
 * Rings only radiate while tracking is actually running, and the sweeping arc
 * only turns while it is: motion on screen means motion in the data. Paused
 * holds still on purpose, because the device is holding still too.
 */
@Composable
fun PowerButton(
    phase: TrackingPhase,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStatusPalette.current
    val accent = when (phase) {
        TrackingPhase.LIVE -> palette.live
        TrackingPhase.PAUSED -> palette.paused
        TrackingPhase.OFF -> palette.idle
    }
    val animatedAccent by animateColorAsState(accent, tween(500), label = "accent")

    val transition = rememberInfiniteTransition(label = "power")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "wave",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "sweep",
    )
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "breathe",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height / 2)
            val coreRadius = size.minDimension * 0.29f

            if (phase == TrackingPhase.LIVE) {
                // Ripples: each ring is the same animation offset in phase, so
                // they read as one signal leaving the device.
                repeat(RING_COUNT) { index ->
                    val progress = (wave + index.toFloat() / RING_COUNT) % 1f
                    val radius = coreRadius + progress * (size.minDimension / 2 - coreRadius)
                    drawCircle(
                        color = animatedAccent.copy(alpha = (1f - progress) * 0.35f),
                        radius = radius,
                        center = centre,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // Track ring plus, when live, a sweeping highlight over it.
            val trackRadius = coreRadius + 26.dp.toPx()
            drawCircle(
                color = animatedAccent.copy(alpha = 0.18f),
                radius = trackRadius,
                center = centre,
                style = Stroke(width = 6.dp.toPx()),
            )
            if (phase != TrackingPhase.OFF) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            animatedAccent.copy(alpha = 0.1f),
                            animatedAccent,
                        ),
                        center = centre,
                    ),
                    startAngle = if (phase == TrackingPhase.LIVE) sweep else -90f,
                    sweepAngle = if (phase == TrackingPhase.LIVE) 110f else 360f,
                    useCenter = false,
                    topLeft = Offset(centre.x - trackRadius, centre.y - trackRadius),
                    size = Size(trackRadius * 2, trackRadius * 2),
                    style = Stroke(width = 6.dp.toPx()),
                )
            }

            drawCircle(
                color = animatedAccent.copy(alpha = 0.12f),
                radius = coreRadius + 14.dp.toPx(),
                center = centre,
            )
        }

        Column(
            Modifier
                .scale(if (phase == TrackingPhase.LIVE) breathe else 1f)
                .size(150.dp)
                .background(animatedAccent, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !busy,
                    onClick = onClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(34.dp),
                    color = palette.onAccent,
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = palette.onAccent,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        if (phase == TrackingPhase.OFF) R.string.start_action
                        else R.string.stop_action,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.onAccent,
                )
            }
        }
    }
}
