package com.anim.where.am.i.presentation.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.ui.components.MetricCell
import com.anim.where.am.i.ui.theme.LocalStatusPalette
import java.net.URI
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenStatus: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.messageRes) {
        state.messageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = onOpenStatus) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            stringResource(R.string.status_title),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusHero(state = state, onToggle = viewModel::onToggleTracking)

            SosCard(
                inFlight = state.sosInFlight,
                onRequest = viewModel::onRequestPosition,
            )

            DestinationCard(serverUrl = state.serverUrl, deviceId = state.deviceId)

            Text(
                stringResource(R.string.disclosure_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusHero(state: MainUiState, onToggle: (Boolean) -> Unit) {
    val palette = LocalStatusPalette.current
    val (container, onContainer, accent) = when (state.phase) {
        TrackingPhase.LIVE -> Triple(palette.liveContainer, palette.onLiveContainer, palette.live)
        TrackingPhase.PAUSED ->
            Triple(palette.pausedContainer, palette.onPausedContainer, palette.paused)

        TrackingPhase.OFF -> Triple(palette.idleContainer, palette.onIdleContainer, palette.idle)
    }

    val animatedContainer by animateColorAsState(container, tween(400), label = "container")
    val animatedOn by animateColorAsState(onContainer, tween(400), label = "onContainer")

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(phase = state.phase, accent = accent, onAccent = palette.onAccent)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when (state.phase) {
                                TrackingPhase.LIVE -> R.string.state_live
                                TrackingPhase.PAUSED -> R.string.state_paused
                                TrackingPhase.OFF -> R.string.state_off
                            },
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = animatedOn,
                    )
                    Text(
                        stringResource(
                            when (state.phase) {
                                TrackingPhase.LIVE -> R.string.state_live_detail
                                TrackingPhase.PAUSED -> R.string.state_paused_detail
                                TrackingPhase.OFF -> R.string.state_off_detail
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = animatedOn.copy(alpha = 0.75f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            LastFixRow(fix = state.lastFix, tint = animatedOn)
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onToggle(!state.tracking) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = palette.onAccent,
                ),
            ) {
                Icon(
                    if (state.tracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (state.tracking) R.string.stop_action else R.string.start_action,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * The dot pulses only while tracking is actually running, so movement on
 * screen always means the same thing as movement in the data.
 */
@Composable
private fun StatusBadge(phase: TrackingPhase, accent: Color, onAccent: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val fade by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        if (phase == TrackingPhase.LIVE) {
            Box(
                Modifier
                    .size(56.dp)
                    .scale(pulse)
                    .alpha(fade)
                    .background(accent, CircleShape),
            )
        }
        Box(
            Modifier
                .size(56.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when (phase) {
                    TrackingPhase.LIVE -> Icons.Default.MyLocation
                    TrackingPhase.PAUSED -> Icons.Default.Pause
                    TrackingPhase.OFF -> Icons.Default.LocationOn
                },
                contentDescription = null,
                tint = onAccent,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun LastFixRow(fix: LocationFix?, tint: Color) {
    Row(Modifier.fillMaxWidth()) {
        MetricCell(
            label = stringResource(R.string.last_fix_label),
            value = fix?.let { relativeTime(it.timeMillis) }
                ?: stringResource(R.string.no_data_yet),
            modifier = Modifier.weight(1f),
            labelColor = tint.copy(alpha = 0.7f),
            valueColor = tint,
        )
        MetricCell(
            label = stringResource(R.string.coordinates_label),
            value = formatCoordinates(fix) ?: "—",
            modifier = Modifier.weight(1.4f),
            labelColor = tint.copy(alpha = 0.7f),
            valueColor = tint,
        )
        MetricCell(
            label = stringResource(R.string.accuracy_label),
            value = fix?.accuracy?.let { "${it.toInt()} m" } ?: "—",
            modifier = Modifier.weight(0.7f),
            labelColor = tint.copy(alpha = 0.7f),
            valueColor = tint,
        )
    }
}

@Composable
private fun SosCard(inFlight: Boolean, onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.request_position),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.request_position_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(
                onClick = onRequest,
                enabled = !inFlight,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sos_action))
            }
        }
    }
}

@Composable
private fun DestinationCard(serverUrl: String, deviceId: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricCell(
                label = stringResource(R.string.id_label),
                value = deviceId.ifEmpty { "—" },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            MetricCell(
                label = stringResource(R.string.server_label),
                value = hostOf(serverUrl),
                modifier = Modifier.weight(1.6f),
            )
        }
    }
}

/** Shows only the host: the rest of the URL carries the ingest secret. */
private fun hostOf(url: String): String = try {
    URI(url).host ?: url.ifEmpty { "—" }
} catch (e: Exception) {
    url.ifEmpty { "—" }
}

private fun formatCoordinates(fix: LocationFix?): String? {
    val lat = fix?.latitude ?: return null
    val lon = fix.longitude ?: return null
    return "%.4f, %.4f".format(lat, lon)
}

@Composable
private fun relativeTime(timeMillis: Long): String {
    val elapsed = System.currentTimeMillis() - timeMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> stringResource(R.string.time_minutes_ago, minutes)
        hours < 24 -> stringResource(R.string.time_hours_ago, hours)
        else -> stringResource(R.string.time_days_ago, days)
    }
}
