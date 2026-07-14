package com.example.omnicortex.ui.screens

import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.example.omnicortex.data.models.NetworkProfile
import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.engine.NetworkScanEngine
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.NetworkViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NetworkIntelScreen(onBack: () -> Unit) {
    val vm: NetworkViewModel = viewModel()
    val state    by vm.state.collectAsState()
    val profiles by vm.networkProfiles.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val locationPerm = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // ── Location services (system toggle) check ──────────────────────────────
    // WiFi scan results on Android are gated by BOTH the runtime location
    // permission AND the device-level Location toggle being on — even if
    // permission is granted, an empty/stale scan is returned silently if
    // Location services are off. We check on first composition and again
    // every time the screen resumes (e.g. after the user comes back from
    // the Settings screen), so the warning clears automatically once they
    // switch it on.
    fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    var locationServicesEnabled by remember { mutableStateOf(isLocationServicesEnabled()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationServicesEnabled = isLocationServicesEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showLocationOffDialog by remember { mutableStateOf(false) }

    // Show the popup whenever we land on this screen (or resume back into
    // it) with permission granted but the system Location toggle off.
    LaunchedEffect(locationPerm.status.isGranted, locationServicesEnabled) {
        showLocationOffDialog = locationPerm.status.isGranted && !locationServicesEnabled
    }

    if (showLocationOffDialog) {
        LocationServicesOffDialog(
            onDismiss = { showLocationOffDialog = false },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            
    ) {
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
                Icon(Icons.Default.ArrowBack, null, tint = AegisCyan)
            }
            Column(Modifier.weight(1f)) {
                Text("Network Intelligence", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("WiFi threat detection", color = TextMuted, fontSize = 11.sp)
            }
            if (locationPerm.status.isGranted) {
                Button(
                    onClick  = { vm.scan() },
                    enabled  = state !is NetworkViewModel.State.Scanning,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AegisCyan,
                        disabledContainerColor = AegisCyan.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        if (state is NetworkViewModel.State.Scanning) "Scanning…" else "Scan",
                        color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }

        // ── Permission gate ───────────────────────────────────────────────────
        if (!locationPerm.status.isGranted) {
            PermissionGateView(locationPerm)
            return@Column
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (val s = state) {
            is NetworkViewModel.State.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon        = Icons.Default.Wifi,
                        title       = "Network Intelligence",
                        subtitle    = "Scan your WiFi environment for evil twin\nattacks, rogue APs and weak encryption.",
                        actionLabel = "Start Scan",
                        onAction    = { vm.scan() }
                    )
                }
            }
            is NetworkViewModel.State.Scanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ScanningIndicator("Scanning WiFi environment…", AegisCyan)
                        Text(
                            "Profiling nearby networks\nand analysing for threats",
                            color = TextMuted, fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is NetworkViewModel.State.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon        = Icons.Default.WifiOff,
                        title       = "Scan Failed",
                        subtitle    = s.msg,
                        actionLabel = "Retry",
                        onAction    = { vm.scan() }
                    )
                }
            }
            is NetworkViewModel.State.Done -> {
                NetworkResultView(s.result, profiles, onToggleTrust = { vm.toggleTrust(it) })
            }
        }
    }
}

@Composable
private fun NetworkResultView(
    result: NetworkScanEngine.ScanResult,
    profiles: List<NetworkProfile>,
    onToggleTrust: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Current network card ──────────────────────────────────────────────
        item {
            val secColor = securityUiColor(result.currentSecurity)
            AegisCard(accentColor = secColor) {
                SectionHeader("Connected Network", AegisCyan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(result.currentSsid, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(result.currentBssid, color = TextMuted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoBadge(result.currentSecurity, secColor)
                            InfoBadge("${result.currentFrequency} MHz", AegisCyan)
                            InfoBadge("Signal ${result.signalLevel}%",
                                if (result.signalLevel > 60) AegisGreen else AegisAmber)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (result.threats.isEmpty()) AegisGreen.copy(alpha = 0.15f)
                                else AegisRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (result.threats.isEmpty()) Icons.Default.CheckCircle
                            else Icons.Default.Warning,
                            null,
                            tint = if (result.threats.isEmpty()) AegisGreen else AegisRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // ── Threats ───────────────────────────────────────────────────────────
        if (result.threats.isNotEmpty()) {
            item { SectionHeader("Threats Detected", AegisRed) }
            items(result.threats) { threat ->
                val color = when (threat.severity) {
                    Severity.CRITICAL -> AegisRed
                    Severity.HIGH     -> AegisOrange
                    Severity.MEDIUM   -> AegisAmber
                    else              -> AegisBlue
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = color, modifier = Modifier.size(18.dp))
                        Text(
                            threat.type.replace("_", " ").uppercase(),
                            color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.weight(1f))
                        SeverityChip(threat.severity, false)
                    }
                    Text(threat.detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AegisGreen.copy(alpha = 0.08f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = AegisGreen, modifier = Modifier.size(18.dp))
                    Text("No threats detected on this network.", color = AegisGreen, fontSize = 13.sp)
                }
            }
        }

        // ── Stats ─────────────────────────────────────────────────────────────
        item {
            StatStrip(listOf(
                Triple("${result.nearbyNetworks.size}", "Nearby", TextSecondary),
                Triple("${result.nearbyNetworks.count { it.security == "WPA3" }}", "WPA3", AegisGreen),
                Triple("${result.nearbyNetworks.count { it.security == "OPEN" }}", "Open", AegisRed),
                Triple("${result.nearbyNetworks.count { it.isTrusted }}", "Trusted", AegisCyan)
            ))
        }

        // ── Nearby networks ───────────────────────────────────────────────────
        item { SectionHeader("Nearby Networks", AegisCyan) }
        items(result.nearbyNetworks.take(20), key = { it.bssid }) { net ->
            NearbyNetworkRow(net, onToggleTrust = { onToggleTrust(net.bssid) })
        }

        // ── Trusted registry ──────────────────────────────────────────────────
        val trusted = profiles.filter { it.trusted }
        if (trusted.isNotEmpty()) {
            item { SectionHeader("Trusted Registry", AegisGreen) }
            items(trusted, key = { "tr_${it.bssid}" }) { p ->
                TrustedNetworkRow(p, onRemoveTrust = { onToggleTrust(p.bssid) })
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun NearbyNetworkRow(net: NetworkScanEngine.NearbyNetwork, onToggleTrust: () -> Unit) {
    val secColor = securityUiColor(net.security)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Wifi, null,
            tint = secColor,
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                net.ssid.ifBlank { "[Hidden]" },
                color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(net.security, color = secColor, fontSize = 10.sp)
                Text("•", color = TextMuted, fontSize = 10.sp)
                Text("${net.frequency}MHz", color = TextMuted, fontSize = 10.sp)
                Text("•", color = TextMuted, fontSize = 10.sp)
                Text("${net.signalLevel}%", color = TextMuted, fontSize = 10.sp)
            }
        }
        IconButton(onClick = onToggleTrust, modifier = Modifier.size(32.dp)) {
            Icon(
                if (net.isTrusted) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                tint = if (net.isTrusted) AegisGreen else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TrustedNetworkRow(profile: NetworkProfile, onRemoveTrust: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AegisGreen.copy(alpha = 0.07f))
            .border(1.dp, AegisGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Star, null, tint = AegisGreen, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.ssid, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(profile.bssid, color = TextMuted, fontSize = 10.sp)
        }
        IconButton(onClick = onRemoveTrust, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LocationServicesOffDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AegisAmber.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOff, null, tint = AegisAmber, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Location Is Turned Off",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "Android requires the device Location toggle to be on in order to scan nearby WiFi networks, even though OmniCortex already has permission. Turn on Location to continue using Network Intelligence.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
            ) {
                Icon(Icons.Default.Settings, null, tint = BgAmoled, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Location Settings", color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now", color = TextMuted, fontSize = 13.sp)
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGateView(perm: PermissionState) {
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
                    .background(AegisCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = AegisCyan, modifier = Modifier.size(40.dp))
            }
            Text(
                "Location Permission Required",
                color = TextPrimary, fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Android requires Location permission to scan nearby WiFi networks. This is used exclusively for network threat analysis — no location data is stored or transmitted.",
                color = TextSecondary, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AegisAmber.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = AegisAmber, modifier = Modifier.size(15.dp))
                Text(
                    "This is a one-time grant. OmniCortex never uses location in the background.",
                    color = AegisAmber, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Button(
                onClick  = { perm.launchPermissionRequest() },
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Grant Location Permission", color = BgAmoled, fontWeight = FontWeight.Bold)
            }
        }
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

private fun securityUiColor(security: String) = when (security) {
    "WPA3" -> AegisGreen
    "WPA2" -> AegisCyan
    "WPA"  -> AegisAmber
    else   -> AegisRed
}
