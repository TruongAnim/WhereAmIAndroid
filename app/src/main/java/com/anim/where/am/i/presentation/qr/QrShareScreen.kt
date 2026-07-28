package com.anim.where.am.i.presentation.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anim.where.am.i.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(viewModel: QrViewModel = hiltViewModel()) {
    val uri by viewModel.shareUri.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.share_config)) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            uri?.let { text ->
                val bitmap = remember(text) { QrCodeGenerator.encode(text, 640) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(280.dp))
                Text(text, Modifier.padding(16.dp))
            }
        }
    }
}
