package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.shizuku.ShizukuManager
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.ShizukuViewModel

enum class ShizukuTab {
    FIREWALL, PERMISSIONS, NET_MONITOR;

    companion object {
        fun fromKey(key: String?): ShizukuTab = when (key) {
            "permissions" -> PERMISSIONS
            "netmonitor"  -> NET_MONITOR
            else          -> FIREWALL
        }
    }
}

@Composable
fun ShizukuModeScreen(onBack: () -> Unit, initialTab: ShizukuTab = ShizukuTab.FIREWALL) {
    val vm: ShizukuViewModel = viewModel()
    val availability by vm.availability.collectAsState()
    var tab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(availability) {
        if (availability == ShizukuManager.Availability.Ready) vm.loadApps()
    }

    Column(modifier = Modifier.fillMaxSize().background(BgAmoled)) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = AegisPurple)
            }
            Column(Modifier.weight(1f)) {
                Text("Shizuku Mode", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Advanced controls via Shizuku", color = TextMuted, fontSize = 11.sp)
            }
            InfoBadge("PRO+", AegisPurple)
        }

        when (availability) {
            ShizukuManager.Availability.Ready -> ShizukuContent(vm, tab, onTabChange = { tab = it })
            else -> ShizukuGateView(availability, onRequestPermission = { vm.requestShizukuPermission() }, onRetry = { vm.refreshAvailability() })
        }
    }
}

@Composable
private fun ShizukuContent(vm: ShizukuViewModel, tab: ShizukuTab, onTabChange: (ShizukuTab) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // ── Tab selector ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabChip("Firewall", tab == ShizukuTab.FIREWALL) { onTabChange(ShizukuTab.FIREWALL) }
            TabChip("Permissions", tab == ShizukuTab.PERMISSIONS) { onTabChange(ShizukuTab.PERMISSIONS) }
            TabChip("Net Monitor", tab == ShizukuTab.NET_MONITOR) { onTabChange(ShizukuTab.NET_MONITOR) }
        }

        when (tab) {
            ShizukuTab.FIREWALL     -> FirewallTab(vm)
            ShizukuTab.PERMISSIONS  -> PermissionsTab(vm)
            ShizukuTab.NET_MONITOR  -> NetMonitorTab(vm)
        }
    }
}

@Composable
private fun RowScope.TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AegisPurple.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AegisPurple else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Firewall tab ──────────────────────────────────────────────────────────────

@Composable
private fun FirewallTab(vm: ShizukuViewModel) {
    val state by vm.uiState.collectAsState()
    when (val s = state) {
        is ShizukuViewModel.UiState.Loading -> LoadingCenter()
        is ShizukuViewModel.UiState.Error -> ErrorCenter(s.message) { vm.loadApps() }
        is ShizukuViewModel.UiState.Loaded -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisPurple.copy(alpha = 0.08f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = AegisPurple, modifier = Modifier.size(16.dp))
                        Text(
                            "Blocking an app here cuts ALL its network access (WiFi + cellular) at the system level — no VPN needed.",
                            color = AegisPurple, fontSize = 11.sp, lineHeight = 15.sp
                        )
                    }
                }
                items(s.apps, key = { it.packageName }) { app ->
                    FirewallAppRow(app, onToggle = { vm.toggleFirewall(app) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun FirewallAppRow(app: ShizukuViewModel.AppEntry, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (app.blocked) Icons.Default.Block else Icons.Default.Wifi,
            null,
            tint = if (app.blocked) AegisRed else TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(app.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(app.packageName, color = TextMuted, fontSize = 10.sp)
        }
        Switch(
            checked = app.blocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = AegisRed, checkedTrackColor = AegisRed.copy(alpha = 0.4f))
        )
    }
}

// ── Permissions tab ───────────────────────────────────────────────────────────

@Composable
private fun PermissionsTab(vm: ShizukuViewModel) {
    val state by vm.uiState.collectAsState()
    var expandedPkg by remember { mutableStateOf<String?>(null) }

    when (val s = state) {
        is ShizukuViewModel.UiState.Loading -> LoadingCenter()
        is ShizukuViewModel.UiState.Error -> ErrorCenter(s.message) { vm.loadApps() }
        is ShizukuViewModel.UiState.Loaded -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.apps.filter { it.requestedPermissions.isNotEmpty() }, key = { it.packageName }) { app ->
                    val expanded = expandedPkg == app.packageName
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgCard)
                            .clickable { expandedPkg = if (expanded) null else app.packageName }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.AdminPanelSettings, null, tint = AegisPurple, modifier = Modifier.size(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${app.requestedPermissions.size} permissions requested", color = TextMuted, fontSize = 10.sp)
                            }
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = TextMuted, modifier = Modifier.size(18.dp)
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(8.dp))
                            app.requestedPermissions.forEach { perm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        perm.removePrefix("android.permission."),
                                        color = TextSecondary, fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SmallActionChip("Grant", AegisGreen) { vm.setPermission(app, perm, grant = true) }
                                        SmallActionChip("Revoke", AegisRed) { vm.setPermission(app, perm, grant = false) }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SmallActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Net monitor tab ───────────────────────────────────────────────────────────

@Composable
private fun NetMonitorTab(vm: ShizukuViewModel) {
    val usage by vm.netUsage.collectAsState()

    DisposableEffect(Unit) {
        vm.startNetworkMonitor()
        onDispose { vm.stopNetworkMonitor() }
    }

    if (usage.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = AegisPurple, modifier = Modifier.size(28.dp))
                Text("Gathering live network stats…", color = TextMuted, fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("Live Data Usage (updates every 4s)", AegisPurple) }
        items(usage, key = { it.uid }) { u ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(u.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("↓${formatBytes(u.rxBytes)}", color = AegisCyan, fontSize = 11.sp)
                    Text("↑${formatBytes(u.txBytes)}", color = AegisAmber, fontSize = 11.sp)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1fGB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000     -> "%.1fMB".format(bytes / 1_000_000.0)
    bytes >= 1_000         -> "%.1fKB".format(bytes / 1_000.0)
    else                   -> "${bytes}B"
}

// ── Gate / empty states ─────────────────────────────────────────────────────

@Composable
private fun ShizukuGateView(
    availability: ShizukuManager.Availability,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit
) {
    val (icon, title, message, actionLabel, action) = when (availability) {
        ShizukuManager.Availability.NotInstalled -> ShizukuGateContent(
            Icons.Default.Extension,
            "Shizuku Not Installed",
            "Shizuku Mode needs the Shizuku app to grant advanced privileges without root — it works over wireless debugging (ADB) too, no root required. Install it from the Play Store or GitHub, then come back here.",
            "Learn About Shizuku",
            onRetry
        )
        ShizukuManager.Availability.NotRunning -> ShizukuGateContent(
            Icons.Default.PowerOff,
            "Shizuku Service Not Running",
            "Shizuku is installed but its service isn't active. Open the Shizuku app and start it — either via root or by pairing wireless debugging — then return here.",
            "I've Started It — Retry",
            onRetry
        )
        ShizukuManager.Availability.PermissionDenied -> ShizukuGateContent(
            Icons.Default.Lock,
            "Permission Needed",
            "SHV Omni-Cortex needs your permission through Shizuku to enable per-app firewall, permission management, and live network monitoring.",
            "Grant Permission",
            onRequestPermission
        )
        else -> ShizukuGateContent(Icons.Default.Extension, "", "", "", onRetry)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AegisPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AegisPurple, modifier = Modifier.size(40.dp))
            }
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
            Button(
                onClick = action,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(actionLabel, color = BgAmoled, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class ShizukuGateContent(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val actionLabel: String,
    val action: () -> Unit
)

@Composable
private fun LoadingCenter() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AegisPurple)
    }
}

@Composable
private fun ErrorCenter(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Default.Error,
            title = "Something Went Wrong",
            subtitle = message,
            actionLabel = "Retry",
            onAction = onRetry
        )
    }
}

@Composable
private fun InfoBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
