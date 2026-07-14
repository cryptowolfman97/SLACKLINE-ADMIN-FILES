package com.example.omnicortex.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.PrivacyShieldViewModel
import com.example.omnicortex.vpn.VpnState

private val BgCard        = Color(0xFF111318)
private val BgAmoled      = Color(0xFF09090B)
private val AegisCyan     = Color(0xFF00FFD1)
private val AegisGreen    = Color(0xFF4ADE80)
private val AegisRed      = Color(0xFFFF4444)
private val AegisAmber    = Color(0xFFFFB347)
private val AegisPurple   = Color(0xFFB47FFF)
private val TextPrimary   = Color(0xFFE8EAF0)
private val TextSecondary = Color(0xFF6B7280)

@Composable
fun PrivacyShieldScreen(
    viewModel: PrivacyShieldViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Refresh permission states every time screen is entered
    LaunchedEffect(Unit) { viewModel.refreshPermissions(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Privacy Shield", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("WARP VPN + DNS-over-HTTPS", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            // PiP / floating bubble toggle
            AnimatedVisibility(visible = uiState.overlayGranted) {
                IconButton(onClick = { viewModel.togglePip(context) }) {
                    Icon(
                        if (uiState.pipActive) Icons.Default.PictureInPictureAlt
                        else Icons.Default.PictureInPicture,
                        contentDescription = "Toggle floating bubble",
                        tint = if (uiState.pipActive) AegisCyan else TextSecondary
                    )
                }
            }
        }

        // ── Permission Setup Cards ─────────────────────────────────────────────
        if (!uiState.batteryExempt || !uiState.overlayGranted) {
            PermissionSetupSection(
                batteryExempt  = uiState.batteryExempt,
                overlayGranted = uiState.overlayGranted,
                onBatteryClick = { activity?.let { viewModel.requestBatteryExemption(it) } },
                onOverlayClick = { activity?.let { viewModel.requestOverlayPermission(it) } }
            )
        }

        // ── VPN Power Button ──────────────────────────────────────────────────
        VpnPowerCard(
            vpnState = uiState.vpnState,
            onToggle = { activity?.let { viewModel.toggleVpn(it) } }
        )

        // ── Live Stats ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.vpnState is VpnState.CONNECTED,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "Queries",  uiState.queriesTotal.toString(),   Icons.Default.Search,      AegisCyan)
                    StatCard(Modifier.weight(1f), "Blocked",  uiState.queriesBlocked.toString(), Icons.Default.Block,       AegisRed)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "Data In",  viewModel.formatBytes(uiState.bytesIn),  Icons.Default.ArrowDownward, AegisGreen)
                    StatCard(Modifier.weight(1f), "Data Out", viewModel.formatBytes(uiState.bytesOut), Icons.Default.ArrowUpward,   AegisAmber)
                }
            }
        }

        // ── DNS-over-HTTPS Toggle ─────────────────────────────────────────────
        DoHCard(enabled = uiState.dohEnabled, onToggle = { viewModel.toggleDoH(context) })

        // ── Blocklist Info ────────────────────────────────────────────────────
        BlocklistCard(size = uiState.blocklistSize)

        // ── How It Works ──────────────────────────────────────────────────────
        HowItWorksCard()
    }
}

// ── Permission Setup Section ──────────────────────────────────────────────────

@Composable
private fun PermissionSetupSection(
    batteryExempt: Boolean,
    overlayGranted: Boolean,
    onBatteryClick: () -> Unit,
    onOverlayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1207))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BuildCircle, null, tint = AegisAmber, modifier = Modifier.size(18.dp))
                Text("Recommended Setup", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AegisAmber)
            }
            Text(
                "Grant these permissions for reliable 24/7 VPN protection.",
                fontSize = 12.sp, color = TextSecondary
            )

            if (!batteryExempt) {
                PermissionRow(
                    icon        = Icons.Default.BatteryAlert,
                    color       = AegisAmber,
                    title       = "Disable Battery Optimization",
                    subtitle    = "Keeps VPN alive when screen is off",
                    granted     = false,
                    buttonLabel = "Grant",
                    onClick     = onBatteryClick
                )
            }

            if (!overlayGranted) {
                PermissionRow(
                    icon        = Icons.Default.Layers,
                    color       = AegisPurple,
                    title       = "Display Over Other Apps",
                    subtitle    = "Enables floating VPN status bubble",
                    granted     = false,
                    buttonLabel = "Grant",
                    onClick     = onOverlayClick
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0F13))
            .padding(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        if (granted) {
            Icon(Icons.Default.CheckCircle, null, tint = AegisGreen, modifier = Modifier.size(20.dp))
        } else {
            TextButton(
                onClick = onClick,
                colors  = ButtonDefaults.textButtonColors(contentColor = color)
            ) {
                Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── VPN Power Card ─────────────────────────────────────────────────────────────

@Composable
private fun VpnPowerCard(vpnState: VpnState, onToggle: () -> Unit) {
    val isConnected  = vpnState is VpnState.CONNECTED
    val isConnecting = vpnState is VpnState.CONNECTING

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            isConnected  -> AegisCyan
            isConnecting -> AegisAmber
            else         -> Color(0xFF2A2D35)
        },
        animationSpec = tween(600), label = "ringColor"
    )
    val stateLabel = when (vpnState) {
        is VpnState.CONNECTED    -> "Protected"
        is VpnState.CONNECTING   -> "Connecting…"
        is VpnState.DISCONNECTED -> "Unprotected"
        is VpnState.ERROR        -> "Error"
    }
    val stateColor = when (vpnState) {
        is VpnState.CONNECTED    -> AegisCyan
        is VpnState.CONNECTING   -> AegisAmber
        is VpnState.DISCONNECTED -> TextSecondary
        is VpnState.ERROR        -> AegisRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "CLOUDFLARE WARP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, letterSpacing = 2.sp
            )

            Box(contentAlignment = Alignment.Center) {
                if (isConnected) {
                    Canvas(Modifier.size(140.dp).scale(pulseScale)) {
                        drawCircle(color = AegisCyan.copy(alpha = 0.12f), radius = size.minDimension / 2f)
                    }
                }
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawArc(
                        color = ringColor, startAngle = -210f, sweepAngle = 240f, useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected)
                                Brush.radialGradient(listOf(AegisCyan.copy(0.25f), Color.Transparent))
                            else
                                Brush.radialGradient(listOf(Color(0xFF1E2028), Color(0xFF14161C)))
                        )
                        .border(
                            1.5.dp,
                            if (isConnected) AegisCyan.copy(0.5f) else Color(0xFF2A2D35),
                            CircleShape
                        )
                        .clickable(enabled = !isConnecting) { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp), color = AegisAmber, strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PowerSettingsNew, null,
                            tint     = if (isConnected) AegisCyan else TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stateLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = stateColor)
                if (isConnected)
                    Text("DNS encrypted via Cloudflare WARP", fontSize = 12.sp, color = TextSecondary)
                else if (vpnState is VpnState.ERROR)
                    Text(vpnState.message, fontSize = 12.sp, color = AegisRed, textAlign = TextAlign.Center)
                else
                    Text("Tap to enable privacy protection", fontSize = 12.sp, color = TextSecondary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D0F13))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ServerInfoItem("Primary",   "162.159.36.1", AegisCyan)
                Divider(modifier = Modifier.height(36.dp).width(1.dp), color = Color(0xFF2A2D35))
                ServerInfoItem("Secondary", "162.159.46.1", AegisPurple)
                Divider(modifier = Modifier.height(36.dp).width(1.dp), color = Color(0xFF2A2D35))
                ServerInfoItem("Protocol",  "WARP / DoH",   AegisGreen)
            }
        }
    }
}

@Composable
private fun ServerInfoItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ── Stat Card ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, accentColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(label, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ── DoH Card ──────────────────────────────────────────────────────────────────

@Composable
private fun DoHCard(enabled: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(AegisPurple.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Dns, null, tint = AegisPurple, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("DNS-over-HTTPS", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    if (enabled) "Active — queries encrypted to 1.1.1.1"
                    else "Encrypts all DNS queries device-wide",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = AegisPurple, checkedTrackColor   = AegisPurple.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary, uncheckedTrackColor = Color(0xFF2A2D35)
                )
            )
        }
    }
}

// ── Blocklist Card ─────────────────────────────────────────────────────────────

@Composable
private fun BlocklistCard(size: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(AegisRed.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shield, null, tint = AegisRed, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Tracker Blocklist", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("$size domains blocked at DNS level", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ads", "Trackers", "Analytics", "Telemetry", "Fingerprinting").forEach { tag ->
                    Text(tag, fontSize = 11.sp, color = AegisRed,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(AegisRed.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ── How It Works ──────────────────────────────────────────────────────────────

@Composable
private fun HowItWorksCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("How It Works", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            listOf(
                Triple(Icons.Default.VpnKey,  AegisCyan,   "WARP VPN encrypts your DNS queries through Cloudflare's global network."),
                Triple(Icons.Default.Block,   AegisRed,    "Tracker domains are blocked before any connection is made — zero data sent."),
                Triple(Icons.Default.Lock,    AegisGreen,  "Your ISP cannot see which websites you're visiting."),
                Triple(Icons.Default.Speed,   AegisAmber,  "DNS-only tunnel — no impact on download/upload speeds."),
                Triple(Icons.Default.Battery5Bar, AegisAmber, "Battery exemption keeps the shield active 24/7 even with screen off."),
                Triple(Icons.Default.Layers,  AegisPurple, "Floating bubble lets you monitor protection status from any screen.")
            ).forEach { (icon, color, text) ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                }
            }
        }
    }
}
