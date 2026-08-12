package com.anim.where.am.i.presentation.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.ui.components.TabWindowInsets
import com.anim.where.am.i.domain.model.LocationFix
import com.anim.where.am.i.ui.theme.LocalStatusPalette
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val palette = LocalStatusPalette.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshStats()
        onPauseOrDispose { }
    }

    LaunchedEffect(state.messageRes) {
        state.messageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeMessage()
        }
    }

    val accent = when (state.phase) {
        TrackingPhase.LIVE -> palette.live
        TrackingPhase.PAUSED -> palette.paused
        TrackingPhase.OFF -> palette.idle
    }
    val glow by animateColorAsState(accent.copy(alpha = 0.14f), tween(600), label = "glow")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = TabWindowInsets,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                // A soft wash of the state colour behind everything, so the
                // whole screen answers the question, not just the button.
                .background(
                    Brush.verticalGradient(
                        listOf(glow, MaterialTheme.colorScheme.background),
                        endY = 900f,
                    ),
                ),
        ) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(28.dp))
                PowerButton(
                    phase = state.phase,
                    busy = state.busy,
                    onClick = viewModel::onToggleTracking,
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(
                        when (state.phase) {
                            TrackingPhase.LIVE -> R.string.state_live
                            TrackingPhase.PAUSED -> R.string.state_paused
                            TrackingPhase.OFF -> R.string.state_off
                        },
                    ),
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        when (state.phase) {
                            TrackingPhase.LIVE -> R.string.state_live_detail
                            TrackingPhase.PAUSED -> R.string.state_paused_detail
                            TrackingPhase.OFF -> R.string.state_off_detail
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))
                StatStrip(state = state)

                Spacer(Modifier.height(16.dp))
                LastFixCard(fix = state.lastFix)

                Spacer(Modifier.height(12.dp))
                SosRow(inFlight = state.sosInFlight, onRequest = viewModel::onRequestPosition)

                Spacer(Modifier.height(12.dp))
                DestinationRow(deviceId = state.deviceId, serverUrl = state.serverUrl)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatStrip(state: HomeUiState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            icon = Icons.Default.Schedule,
            label = stringResource(R.string.stat_session),
            value = if (state.startedAtMillis != null) {
                formatDuration(state.elapsedMillis)
            } else {
                "—"
            },
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = Icons.Default.CloudUpload,
            label = stringResource(R.string.stat_uploads_today),
            value = state.uploadsToday.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LastFixCard(fix: LocationFix?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.last_fix_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatCoordinates(fix) ?: stringResource(R.string.no_data_yet),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (fix != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        relativeTime(fix.timeMillis),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    fix.accuracy?.let {
                        Text(
                            "±${it.toInt()} m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SosRow(inFlight: Boolean, onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.request_position),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.request_position_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onRequest,
                enabled = !inFlight,
                shape = MaterialTheme.shapes.large,
            ) {
                if (inFlight) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sos_action))
            }
        }
    }
}

@Composable
private fun DestinationRow(deviceId: String, serverUrl: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.id_label) + ": " + deviceId.ifEmpty { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            hostOf(serverUrl),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(180.dp),
            textAlign = TextAlign.End,
        )
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
    return "%.5f, %.5f".format(lat, lon)
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
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
