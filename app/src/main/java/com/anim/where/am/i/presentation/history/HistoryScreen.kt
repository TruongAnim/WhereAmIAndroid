package com.anim.where.am.i.presentation.history

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.ui.components.TabWindowInsets
import com.anim.where.am.i.domain.usecase.DaySummary
import com.anim.where.am.i.ui.theme.LocalStatusPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        contentWindowInsets = TabWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MapPromoCard(
                    onOpen = {
                        val url = viewModel.viewerUrl()
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    },
                )
            }

            if (days.isEmpty()) {
                item { EmptyHistory() }
            } else {
                items(days, key = { it.startOfDayMillis }) { day -> DayCard(day) }
                item {
                    Text(
                        stringResource(R.string.history_footnote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPromoCard(onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.map_promo_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.map_promo_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onOpen, shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Default.Map, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.map_promo_action))
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: DaySummary) {
    val palette = LocalStatusPalette.current
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        dayLabel(day.startOfDayMillis, dayFormat),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${timeFormat.format(Date(day.firstEventMillis))} – " +
                            timeFormat.format(Date(day.lastEventMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Metric(stringResource(R.string.stat_uploads), day.uploads.toString())
                Metric(stringResource(R.string.stat_fixes), day.fixes.toString())
                if (day.alarms > 0) {
                    Metric(
                        stringResource(R.string.sos_action),
                        day.alarms.toString(),
                        MaterialTheme.colorScheme.error,
                    )
                }
                if (day.errors > 0) {
                    Metric(
                        stringResource(R.string.stat_errors),
                        day.errors.toString(),
                        palette.paused,
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
        Text(
            label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.history_empty_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun dayLabel(startOfDayMillis: Long, format: SimpleDateFormat): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val oneDay = 24 * 60 * 60 * 1000L
    return when (startOfDayMillis) {
        today -> stringResource(R.string.day_today)
        today - oneDay -> stringResource(R.string.day_yesterday)
        else -> format.format(Date(startOfDayMillis))
    }
}
