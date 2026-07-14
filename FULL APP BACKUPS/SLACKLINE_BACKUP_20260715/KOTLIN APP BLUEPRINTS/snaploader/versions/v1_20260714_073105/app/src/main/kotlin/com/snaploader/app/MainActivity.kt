package com.snaploader.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.snaploader.app.model.DownloadStatus
import com.snaploader.app.ui.screens.DownloadsScreen
import com.snaploader.app.ui.screens.HomeScreen
import com.snaploader.app.ui.screens.SettingsScreen
import com.snaploader.app.ui.screens.WebToolsScreen
import com.snaploader.app.ui.theme.AccentColour
import com.snaploader.app.ui.theme.SnapLoaderTheme
import com.snaploader.app.ui.license.AccountGateDialog
import com.snaploader.app.ui.license.LicenseDetailsDialog
import com.snaploader.app.ui.license.LockedFeatureDialog
import com.snaploader.app.viewmodel.MainViewModel

// ── Navigation destinations ───────────────────────────────────────────────────
sealed class NavDest(val route: String, val label: String, val icon: ImageVector) {
    object Home      : NavDest("home",      "Home",      Icons.Default.Home)
    object WebTools  : NavDest("webtools",  "Web Tools", Icons.Default.Language)
    object Downloads : NavDest("downloads", "Downloads", Icons.Default.Download)
    object Settings  : NavDest("settings",  "Settings",  Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled silently */ }

    // Returns from the system overlay permission Settings page
    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onOverlayPermissionResult(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        setContent {
            val theme by viewModel.theme.collectAsState()
            val accentColour by viewModel.accentColour.collectAsState()
            SnapLoaderTheme(appTheme = theme, accentColour = accentColour) {
                SnapLoaderApp(
                    viewModel          = viewModel,
                    onExit             = { finish() },
                    onRequestOverlayPerm = { requestOverlayPermission() }
                )
            }
        }
    }

    // singleTop — called when app is already running and receives a new share intent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    // Auto-detect clipboard link when app comes to foreground
    override fun onResume() {
        super.onResume()
        viewModel.checkClipboardOnResume(this)
    }


    private fun requestRequiredPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.POST_NOTIFICATIONS
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayPermLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }
    }
}

// ── Root composable ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapLoaderApp(
    viewModel            : MainViewModel,
    onExit               : () -> Unit,
    onRequestOverlayPerm : () -> Unit
) {
    var currentDest by remember {
        mutableStateOf<NavDest>(NavDest.Home)
    }
    var showExitDlg by remember { mutableStateOf(false) }
    val confirmExit by viewModel.confirmExit.collectAsState()
    val downloads   by viewModel.downloads.collectAsState()
    val activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }
    val isFloating  by viewModel.isFloating.collectAsState()
    val needsPerm   by viewModel.needsOverlayPermission.collectAsState()
    val context     = LocalContext.current

    // ── License / account gate state (additive) ─────────────────────────────
    val accountChecking by viewModel.accountChecking.collectAsState()
    val accountLoggedIn by viewModel.accountLoggedIn.collectAsState()
    val showLicenseDialog by viewModel.showLicenseDialogFlow.collectAsState()
    val lockedFeatureTier by viewModel.lockedFeatureTier.collectAsState()

    fun selectDest(dest: NavDest) {
        currentDest = dest
        if (dest == NavDest.Home) viewModel.refreshAccessSilently()
    }

    // ── Mandatory SHVertex account gate (additive) — applies to every tier ──
    if (accountChecking) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (!accountLoggedIn) {
        AccountGateDialog(viewModel)
        return
    }

    // Overlay permission dialog
    if (needsPerm) {
        AlertDialog(
            onDismissRequest = { viewModel.onOverlayPermissionResult(context) },
            shape            = RoundedCornerShape(16.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            icon  = { Text("🪟", style = MaterialTheme.typography.headlineMedium) },
            title = {
                Text("Overlay Permission Required", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text  = {
                Text(
                    "To show the floating window, SnapLoader needs the " +
                    "'Display over other apps' permission.\n\nTap Grant to open Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onOverlayPermissionResult(context)
                    onRequestOverlayPerm()
                }) { Text("Grant Permission", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onOverlayPermissionResult(context) }) {
                    Text("Not now")
                }
            }
        )
    }

    BackHandler {
        if (confirmExit) showExitDlg = true else onExit()
    }

    if (showExitDlg) {
        ExitConfirmDialog(onConfirm = onExit, onDismiss = { showExitDlg = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentDest) {
                            NavDest.Home      -> "SHV Downloader"
                            NavDest.WebTools  -> "Web Tools"
                            NavDest.Downloads -> "Downloads"
                            NavDest.Settings  -> "Settings"
                            else              -> "SHV Downloader"
                        },
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Floating window toggle — visible in all tabs, Pro+ gated
                    IconButton(onClick = { viewModel.attemptToggleFloatingWindow(context) }) {
                        Icon(
                            imageVector = if (isFloating) Icons.Default.PictureInPicture
                                          else            Icons.Default.PictureInPictureAlt,
                            contentDescription = if (isFloating) "Stop floating window"
                                                 else            "Start floating window",
                            tint = if (isFloating) MaterialTheme.colorScheme.primary
                                   else            MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (currentDest == NavDest.Home) {
                        Surface(
                            onClick = { viewModel.showLicenseDialog() },
                            color = androidx.compose.ui.graphics.Color(0xFFC9A227).copy(alpha = 0.18f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WorkspacePremium, null,
                                    tint = androidx.compose.ui.graphics.Color(0xFFC9A227),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "License",
                                    color = androidx.compose.ui.graphics.Color(0xFFC9A227),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(onClick = { selectDest(NavDest.Settings) }) {
                            Icon(Icons.Default.Settings, "Settings",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            )
        },
        bottomBar = {
            SnapBottomBar(
                current     = currentDest,
                activeCount = activeCount,
                onSelect    = { selectDest(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState    = currentDest,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label          = "screen"
            ) { dest ->
                when (dest) {
                    NavDest.Home      -> HomeScreen(viewModel)
                    NavDest.WebTools  -> WebToolsScreen(viewModel)
                    NavDest.Downloads -> DownloadsScreen(viewModel)
                    NavDest.Settings  -> SettingsScreen(viewModel)
                    else              -> HomeScreen(viewModel)
                }
            }
        }
    }

    if (showLicenseDialog) {
        LicenseDetailsDialog(viewModel, onDismiss = { viewModel.dismissLicenseDialog() })
    }
    lockedFeatureTier?.let { tier ->
        LockedFeatureDialog(
            requiredTier = tier,
            onDismiss = { viewModel.dismissLockedDialog() },
            onViewLicense = { viewModel.showLicenseDialog() }
        )
    }
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────
@Composable
fun SnapBottomBar(
    current    : NavDest,
    activeCount: Int,
    onSelect   : (NavDest) -> Unit
) {
    val destinations = listOf(NavDest.Home, NavDest.WebTools, NavDest.Downloads, NavDest.Settings)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        destinations.forEach { dest ->
            val selected = current == dest
            NavigationBarItem(
                selected = selected,
                onClick  = { onSelect(dest) },
                icon     = {
                    BadgedBox(badge = {
                        if (dest == NavDest.Downloads && activeCount > 0)
                            Badge { Text("$activeCount") }
                    }) { Icon(dest.icon, contentDescription = dest.label) }
                },
                label  = {
                    Text(dest.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(0.12f)
                )
            )
        }
    }
}

// ── Exit dialog ───────────────────────────────────────────────────────────────
@Composable
fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(16.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        icon  = { Text("👋", style = MaterialTheme.typography.headlineMedium) },
        title = {
            Text("Exit SnapLoader?", color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Active downloads will be stopped. Are you sure you want to exit?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)) {
                Text("Exit", color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Stay", fontWeight = FontWeight.Medium)
            }
        }
    )
}