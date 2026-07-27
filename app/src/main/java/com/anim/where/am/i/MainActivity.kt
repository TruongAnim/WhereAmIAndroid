package com.anim.where.am.i

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.anim.where.am.i.presentation.navigation.WhereAmINavHost
import com.anim.where.am.i.ui.theme.WhereAmITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereAmITheme {
                val navController = rememberNavController()
                WhereAmINavHost(navController)
            }
        }
    }
}
