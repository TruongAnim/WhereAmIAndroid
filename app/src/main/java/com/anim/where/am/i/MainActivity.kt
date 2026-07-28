package com.anim.where.am.i

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.anim.where.am.i.deeplink.DeepLinkHandler
import com.anim.where.am.i.deeplink.DeepLinkResult
import com.anim.where.am.i.domain.model.ConfigLink
import com.anim.where.am.i.domain.usecase.ApplyConfigLink
import com.anim.where.am.i.presentation.navigation.WhereAmINavHost
import com.anim.where.am.i.ui.theme.WhereAmITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    @Inject
    lateinit var applyConfigLink: ApplyConfigLink

    private val pendingConfig = MutableStateFlow<ConfigLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            WhereAmITheme {
                val navController = rememberNavController()
                WhereAmINavHost(navController)

                val config by pendingConfig.collectAsStateWithLifecycle()
                config?.let { link ->
                    AlertDialog(
                        onDismissRequest = { pendingConfig.value = null },
                        confirmButton = {
                            TextButton(onClick = {
                                lifecycleScope.launch { applyConfigLink(link) }
                                pendingConfig.value = null
                            }) {
                                Text(stringResource(R.string.ok_button))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingConfig.value = null }) {
                                Text(stringResource(R.string.cancel_button))
                            }
                        },
                        text = { Text(stringResource(R.string.configuration_message)) },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        lifecycleScope.launch {
            when (val result = deepLinkHandler.handle(data)) {
                is DeepLinkResult.ConfirmConfig -> pendingConfig.value = result.link
                else -> Unit
            }
        }
    }
}
