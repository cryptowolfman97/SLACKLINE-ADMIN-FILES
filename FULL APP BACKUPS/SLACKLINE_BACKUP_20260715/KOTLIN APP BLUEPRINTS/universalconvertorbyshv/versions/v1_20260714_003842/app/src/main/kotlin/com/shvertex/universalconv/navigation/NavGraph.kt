package com.shvertex.universalconv.navigation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shvertex.universalconv.overlay.FloatingCalcService
import com.shvertex.universalconv.ui.components.PipCalcFAB
import com.shvertex.universalconv.ui.components.SciCalcFAB
import com.shvertex.universalconv.ui.screens.*
import com.shvertex.universalconv.ui.theme.*

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val navController: NavHostController = rememberNavController()
    val recents = remember { mutableStateListOf<String>() }
    var showCalc by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }

    // Launcher that opens the SYSTEM_ALERT_WINDOW settings page.
    // When the user returns we re-check and start the service if granted.
    val overlayPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            context.startService(Intent(context, FloatingCalcService::class.java))
        }
    }

    fun launchFloatingCalc() {
        if (Settings.canDrawOverlays(context)) {
            context.startService(Intent(context, FloatingCalcService::class.java))
        } else {
            showOverlayDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
    ) {
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier
                .fillMaxSize()
                .background(Black),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    recents    = recents,
                    onNavigate = { moduleId ->
                        recents.remove(moduleId)
                        recents.add(moduleId)
                        navController.navigate(Screen.Converter.create(moduleId))
                    },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack  = { navController.popBackStack() },
                    onAbout = { navController.navigate(Screen.About.route) },
                )
            }
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Converter.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: "length"
                ConverterScreen(
                    moduleId = moduleId,
                    onBack   = { navController.popBackStack() },
                )
            }
        }

        // Floating action buttons — always visible over every screen
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PipCalcFAB(onClick = ::launchFloatingCalc)
            SciCalcFAB(
                showCalc  = showCalc,
                onToggle  = { showCalc = !showCalc },
                onDismiss = { showCalc = false },
            )
        }

        // ── Overlay permission dialog ───────────────────────────────
        if (showOverlayDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayDialog = false },
                containerColor   = Surface1,
                title = {
                    Text(
                        "Permission needed",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                },
                text = {
                    Text(
                        "To show the floating calculator over other apps, please grant the \"Display over other apps\" permission.",
                        color = TextSecondary,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOverlayDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        overlayPermLauncher.launch(intent)
                    }) {
                        Text("Open Settings", color = Teal, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
            )
        }
    }
}
