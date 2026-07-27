package com.anim.where.am.i.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val STATUS = "status"
    const val QR_SCAN = "qr_scan"
    const val QR_SHARE = "qr_share"
}

@Composable
fun WhereAmINavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            com.anim.where.am.i.presentation.main.MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStatus = { navController.navigate(Routes.STATUS) },
            )
        }
        composable(Routes.SETTINGS) {
            com.anim.where.am.i.presentation.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onScanQr = { navController.navigate(Routes.QR_SCAN) },
                onShareConfig = { navController.navigate(Routes.QR_SHARE) },
            )
        }
        composable(Routes.STATUS) { com.anim.where.am.i.presentation.status.StatusScreen() }
        composable(Routes.QR_SCAN) { /* Task 17 */ }
        composable(Routes.QR_SHARE) { /* Task 17 */ }
    }
}
