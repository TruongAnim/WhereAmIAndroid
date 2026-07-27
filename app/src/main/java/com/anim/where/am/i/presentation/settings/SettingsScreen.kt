package com.anim.where.am.i.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R
import com.anim.where.am.i.domain.model.Accuracy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onScanQr: () -> Unit,
    onShareConfig: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val urlError by viewModel.urlError.collectAsStateWithLifecycle()
    var advanced by remember { mutableStateOf(false) }
    val s = settings ?: return

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.serverUrl, onValueChange = { v -> viewModel.update { it.copy(serverUrl = v) } },
                label = { Text(stringResource(R.string.server_url_label)) },
                isError = urlError, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.deviceId, onValueChange = { v -> viewModel.update { it.copy(deviceId = v) } },
                label = { Text(stringResource(R.string.id_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            AccuracyDropdown(s.accuracy) { a -> viewModel.update { it.copy(accuracy = a) } }
            IntField(R.string.distance_label, s.distanceMeters) { v -> viewModel.update { it.copy(distanceMeters = v) } }
            IntField(R.string.interval_label, s.intervalSeconds) { v -> viewModel.update { it.copy(intervalSeconds = v) } }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.advanced_label), Modifier.weight(1f))
                Switch(checked = advanced, onCheckedChange = { advanced = it })
            }

            if (advanced) {
                IntField(R.string.angle_label, s.angleDegrees) { v -> viewModel.update { it.copy(angleDegrees = v) } }
                IntField(R.string.heartbeat_label, s.heartbeatSeconds) { v -> viewModel.update { it.copy(heartbeatSeconds = v) } }
                BoolRow(R.string.buffer_label, s.buffer) { v -> viewModel.update { it.copy(buffer = v) } }
                BoolRow(R.string.wakelock_label, s.wakeLock) { v -> viewModel.update { it.copy(wakeLock = v) } }
                BoolRow(R.string.stop_detection_label, s.stopDetection) { v -> viewModel.update { it.copy(stopDetection = v) } }
                BoolRow(R.string.prefer_platform_label, s.preferPlatformProviders) { v -> viewModel.update { it.copy(preferPlatformProviders = v) } }
            }

            Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_qr)) }
            Button(onClick = onShareConfig, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.share_config)) }
            Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_button)) }
        }
    }
}

@Composable
private fun IntField(labelRes: Int, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onChange) ?: if (it.isEmpty()) onChange(0) else Unit },
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BoolRow(labelRes: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccuracyDropdown(value: Accuracy, onChange: (Accuracy) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = accuracyLabel(value), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.accuracy_label)) },
            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Accuracy.entries.forEach { a ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(accuracyLabel(a)) },
                    onClick = { onChange(a); expanded = false },
                )
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
    }
)
