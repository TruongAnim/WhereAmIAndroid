package com.anim.where.am.i.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.anim.where.am.i.R
import com.anim.where.am.i.presentation.history.HistoryScreen
import com.anim.where.am.i.presentation.home.HomeScreen
import com.anim.where.am.i.presentation.qr.QrScanScreen
import com.anim.where.am.i.presentation.qr.QrShareScreen
import com.anim.where.am.i.presentation.settings.SettingsScreen
import com.anim.where.am.i.presentation.status.StatusScreen

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
    const val QR_SCAN = "qr_scan"
    const val QR_SHARE = "qr_share"
}

private data class Tab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

private val Tabs = listOf(
    Tab(Routes.HOME, Icons.Default.Home, R.string.tab_home),
    Tab(Routes.HISTORY, Icons.Default.History, R.string.tab_history),
    Tab(Routes.LOGS, Icons.AutoMirrored.Filled.List, R.string.tab_logs),
    Tab(Routes.SETTINGS, Icons.Default.Settings, R.string.tab_settings),
)

@Composable
fun WhereAmINavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // The QR screens are pushed on top of a tab, so the bar slides away
    // instead of leaving a dead strip under a full-bleed camera preview.
    val showBar = currentRoute in Tabs.map { it.route }

    Scaffold(
        // The shell only reserves the bottom bar; each tab claims the status
        // bar itself, so nothing is inset twice.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.LOGS) { StatusScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onScanQr = { navController.navigate(Routes.QR_SCAN) },
                    onShareConfig = { navController.navigate(Routes.QR_SHARE) },
                )
            }
            composable(Routes.QR_SCAN) {
                QrScanScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.QR_SHARE) {
                QrShareScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Standard bottom-bar behaviour: one entry per tab on the back stack, state
 * kept when switching, and back from any tab returns to home rather than
 * walking every tab the user visited.
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
