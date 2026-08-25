package com.anim.where.am.i.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.anim.where.am.i.BuildConfig
import com.anim.where.am.i.R
import com.anim.where.am.i.ui.components.TabWindowInsets
import com.anim.where.am.i.domain.model.Accuracy
import com.anim.where.am.i.domain.model.typicalErrorMeters
import com.anim.where.am.i.ui.components.RowDivider
import com.anim.where.am.i.ui.components.SectionCard
import com.anim.where.am.i.ui.components.SettingRow
import com.anim.where.am.i.ui.components.SwitchRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onScanQr: () -> Unit,
    onShareConfig: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val urlError by viewModel.urlError.collectAsStateWithLifecycle()
    var advanced by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.reload()
        onPauseOrDispose { }
    }

    val s = settings ?: return

    Scaffold(
        contentWindowInsets = TabWindowInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        viewModel.save {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.settings_saved),
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                ) {
                    Text(
                        stringResource(R.string.save_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionCard(
                title = stringResource(R.string.section_destination),
                icon = Icons.Default.Cloud,
            ) {
                OutlinedTextField(
                    value = s.serverUrl,
                    onValueChange = { v -> viewModel.update { it.copy(serverUrl = v) } },
                    label = { Text(stringResource(R.string.server_url_label)) },
                    isError = urlError,
                    supportingText = {
                        Text(
                            if (urlError) stringResource(R.string.invalid_url)
                            else stringResource(R.string.server_url_hint),
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                OutlinedTextField(
                    value = s.deviceId,
                    onValueChange = { v -> viewModel.update { it.copy(deviceId = v) } },
                    label = { Text(stringResource(R.string.id_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onScanQr,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.scan_qr))
                    }
                    FilledTonalButton(
                        onClick = onShareConfig,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.QrCode, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share_config))
                    }
                }
            }

            SectionCard(
                title = stringResource(R.string.section_location),
                icon = Icons.Default.Explore,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.accuracy_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    AccuracySelector(s.accuracy) { a -> viewModel.update { it.copy(accuracy = a) } }
                }
                RowDivider()
                NumberRow(
                    label = stringResource(R.string.distance_label),
                    subtitle = stringResource(R.string.distance_hint),
                    value = s.distanceMeters,
                    // A threshold below the error of the measurement it filters
                    // on is met by the receiver's own drift, so a phone lying
                    // still reports a journey.
                    warning = if (s.distanceMeters in 1 until s.accuracy.typicalErrorMeters) {
                        stringResource(
                            R.string.distance_below_accuracy_warning,
                            s.accuracy.typicalErrorMeters,
                        )
                    } else {
                        null
                    },
                    warningIsError = true,
                ) { v -> viewModel.update { it.copy(distanceMeters = v) } }
                RowDivider()
                NumberRow(
                    label = stringResource(R.string.interval_label),
                    subtitle = stringResource(R.string.interval_hint),
                    value = s.intervalSeconds,
                    // The SDK cannot ask Android for "every N metres OR every N
                    // seconds" in one request, so a distance filter drops the
                    // interval. Saying so beats leaving a live-looking field
                    // that quietly does nothing.
                    warning = if (
                        s.intervalSeconds > 0 &&
                        s.distanceMeters > 0 &&
                        s.accuracy != Accuracy.HIGHEST
                    ) {
                        stringResource(R.string.interval_ignored_warning)
                    } else {
                        null
                    },
                ) { v -> viewModel.update { it.copy(intervalSeconds = v) } }
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.stop_detection_label),
                    subtitle = stringResource(R.string.stop_detection_hint),
                    checked = s.stopDetection,
                ) { v -> viewModel.update { it.copy(stopDetection = v) } }
            }

            SectionCard(
                title = stringResource(R.string.advanced_label),
                icon = Icons.Default.Tune,
            ) {
                SwitchRow(
                    title = stringResource(R.string.show_advanced_label),
                    subtitle = stringResource(R.string.show_advanced_hint),
                    checked = advanced,
                    onCheckedChange = { advanced = it },
                )
                AnimatedVisibility(visible = advanced) {
                    Column {
                        RowDivider()
                        NumberRow(
                            label = stringResource(R.string.angle_label),
                            subtitle = stringResource(R.string.angle_hint),
                            value = s.angleDegrees,
                        ) { v -> viewModel.update { it.copy(angleDegrees = v) } }
                        RowDivider()
                        NumberRow(
                            label = stringResource(R.string.heartbeat_label),
                            subtitle = stringResource(R.string.heartbeat_hint),
                            value = s.heartbeatSeconds,
                        ) { v -> viewModel.update { it.copy(heartbeatSeconds = v) } }
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.buffer_label),
                            subtitle = stringResource(R.string.buffer_hint),
                            checked = s.buffer,
                        ) { v -> viewModel.update { it.copy(buffer = v) } }
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.wakelock_label),
                            subtitle = stringResource(R.string.wakelock_hint),
                            checked = s.wakeLock,
                        ) { v -> viewModel.update { it.copy(wakeLock = v) } }
                        RowDivider()
                        NumberRow(
                            label = stringResource(R.string.detail_log_label),
                            subtitle = stringResource(R.string.detail_log_hint),
                            value = s.detailLogSeconds,
                        ) { v -> viewModel.update { it.copy(detailLogSeconds = v) } }
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.ignore_jitter_label),
                            subtitle = stringResource(R.string.ignore_jitter_hint),
                            checked = s.ignoreJitter,
                        ) { v -> viewModel.update { it.copy(ignoreJitter = v) } }
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.screen_events_label),
                            subtitle = stringResource(R.string.screen_events_hint),
                            checked = s.screenEvents,
                        ) { v -> viewModel.update { it.copy(screenEvents = v) } }
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.prefer_platform_label),
                            subtitle = stringResource(R.string.prefer_platform_hint),
                            checked = s.preferPlatformProviders,
                        ) { v -> viewModel.update { it.copy(preferPlatformProviders = v) } }
                    }
                }
            }

            SectionCard(
                title = stringResource(R.string.section_help),
                icon = Icons.Default.MenuBook,
            ) {
                SettingRow(
                    title = stringResource(R.string.guide_label),
                    subtitle = stringResource(R.string.guide_hint),
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, BuildConfig.GUIDE_URL.toUri()),
                        )
                    },
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NumberRow(
    label: String,
    subtitle: String,
    value: Int,
    warning: String? = null,
    /** Red for "this will corrupt your data", muted for "this does nothing". */
    warningIsError: Boolean = false,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (warning != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (warningIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { text ->
                if (text.isEmpty()) onChange(0) else text.toIntOrNull()?.let(onChange)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(104.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccuracySelector(value: Accuracy, onChange: (Accuracy) -> Unit) {
    val entries = Accuracy.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, accuracy ->
            SegmentedButton(
                selected = accuracy == value,
                onClick = { onChange(accuracy) },
                shape = SegmentedButtonDefaults.itemShape(index, entries.size),
            ) {
                Text(accuracyLabel(accuracy), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun accuracyLabel(a: Accuracy): String = stringResource(
    when (a) {
        Accuracy.HIGHEST -> R.string.highest_accuracy_label
        Accuracy.HIGH -> R.string.high_accuracy_label
        Accuracy.MEDIUM -> R.string.medium_accuracy_label
        Accuracy.LOW -> R.string.low_accuracy_label
    },
)
