package com.anim.where.am.i.presentation.status

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.ui.components.TabWindowInsets
import com.anim.where.am.i.domain.model.LogItem
import com.anim.where.am.i.domain.model.LogLevel
import com.anim.where.am.i.ui.theme.LocalStatusPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The SDK writes plain sentences to its log. Classifying them here lets the
 * list show at a glance whether something went wrong, without changing the
 * SDK's own format.
 */
private enum class LogKind { ERROR, SUCCESS, MOTION, INFO }

private fun classify(message: String): LogKind {
    val text = message.lowercase(Locale.US)
    return when {
        "denied" in text || "error" in text || "failed" in text ||
            "missing" in text || "blocked" in text || "not allowed" in text -> LogKind.ERROR

        "response 2" in text || "accepted" in text || "restored" in text -> LogKind.SUCCESS
        "stationary" in text || "geofence" in text || "stop detection" in text ||
            "activity" in text || "heartbeat" in text -> LogKind.MOTION

        else -> LogKind.INFO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(viewModel: StatusViewModel = hiltViewModel()) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val detailMode by viewModel.detailMode.collectAsStateWithLifecycle()
    val detailCount by viewModel.detailCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val displayFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    // New entries are prepended. LazyColumn anchors on the item you are
    // looking at, so without this they pile up above the viewport and the
    // screen looks frozen until you scroll back up. Only follow when already
    // at the top, so reading older entries is never interrupted.
    LaunchedEffect(logs.firstOrNull()?.timeMillis) {
        if (listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        contentWindowInsets = TabWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.status_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                        Icon(Icons.Default.VerticalAlignTop, stringResource(R.string.jump_to_newest))
                    }
                    IconButton(onClick = {
                        val text = viewModel.formatShare()
                        if (text.isNotEmpty()) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    }) {
                        Icon(Icons.Default.Share, stringResource(R.string.share_logs))
                    }
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.Delete, stringResource(R.string.clear_logs))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = detailMode,
                    onClick = { viewModel.setDetailMode(!detailMode) },
                    label = {
                        Text(
                            if (detailMode) {
                                stringResource(R.string.log_mode_detail)
                            } else {
                                stringResource(R.string.log_mode_main)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Troubleshoot,
                            contentDescription = null,
                            Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (detailMode) {
                        stringResource(R.string.log_mode_detail_hint)
                    } else {
                        stringResource(R.string.log_mode_main_hint, detailCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (logs.isEmpty()) {
                EmptyLogs()
                return@Column
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(logs, key = { "${it.timeMillis}-${it.message.hashCode()}" }) { entry ->
                    LogRow(entry = entry, time = displayFormat.format(Date(entry.timeMillis)))
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogItem, time: String) {
    val palette = LocalStatusPalette.current
    val accent: Color = when (classify(entry.message)) {
        LogKind.ERROR -> MaterialTheme.colorScheme.error
        LogKind.SUCCESS -> palette.live
        LogKind.MOTION -> palette.paused
        LogKind.INFO -> MaterialTheme.colorScheme.outline
    }

    val isDetail = entry.level == LogLevel.DETAIL

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (isDetail) 5.dp else 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // A timeline rail: the dot carries the severity, the line ties the
        // entries together so a burst of activity reads as one episode.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp),
        ) {
            Spacer(Modifier.height(5.dp))
            if (isDetail) {
                Box(
                    Modifier
                        .size(7.dp)
                        .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape),
                )
            } else {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(accent, CircleShape),
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDetail) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                time,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyLogs(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.logs_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.logs_empty_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
