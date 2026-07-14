package com.example.interstellarcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.interstellarcalc.data.ThemePreference
import com.example.interstellarcalc.ui.AppNavHost
import com.example.interstellarcalc.ui.ExitConfirmationDialog
import com.example.interstellarcalc.ui.Screen
import com.example.interstellarcalc.ui.theme.AppTheme
import com.example.interstellarcalc.ui.theme.InterstellarCalcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Theme and State Management
            val themeFlow = ThemePreference.getTheme(this)
            val appTheme by themeFlow.collectAsState(initial = AppTheme.AMOLED)
            
            var showExit by remember { mutableStateOf(false) }
            
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // NATIVE COMPOSE BACK HANDLER
            // Intercepts the back button ONLY when the user is on the Home Screen.
            BackHandler(enabled = currentRoute == Screen.Home.route) {
                showExit = true
            }

            InterstellarCalcTheme(appTheme = appTheme) {
                // Direct layout execution since license validation is removed
                AppNavHost(
                    navController = navController,
                    currentTheme  = appTheme,
                    onThemeChange = { theme ->
                        lifecycleScope.launch { ThemePreference.setTheme(this@MainActivity, theme) }
                    }
                )
                
                if (showExit) {
                    ExitConfirmationDialog(
                        onConfirm = { finishAffinity() },
                        onDismiss = { showExit = false }
                    )
                }
            }
        }
    }
}
