package com.example.omnicortex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.ui.components.AegisScoreRing
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.HomeViewModel
import com.example.omnicortex.license.LicenseDetailsDialog
import com.example.omnicortex.license.LicenseState
import com.example.omnicortex.license.LoginRequiredDialog
import com.example.omnicortex.license.Tier
import com.example.omnicortex.license.UpgradeLockDialog
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    onGoPosture: () -> Unit,
    onGoApps: () -> Unit,
    onGoNetwork: () -> Unit,
    onGoBreach: () -> Unit,
    onGoComms: () -> Unit,
    onGoPrivacyShield: () -> Unit,
    onGoHttpRecon: () -> Unit,
    onGoPortScan: () -> Unit,
    onGoDns: () -> Unit,
    onGoApkAnalyser: () -> Unit,
    onGoShizukuFirewall: () -> Unit,
    onGoShizukuPermissions: () -> Unit,
    onGoShizukuNetMonitor: () -> Unit,
    onGoSettings: () -> Unit = {},
    onExit: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { homeViewModel.loadScore() }

    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    // ── License gate wiring (additive; does not alter any module behaviour) ──
    val gateContext = LocalContext.current
    val gateScope = rememberCoroutineScope()
    val licenseUi by LicenseState.state.collectAsState()
    var showLicenseDetails by remember { mutableStateOf(false) }
    var showLoginRequired by remember { mutableStateOf(false) }
    var lockedModule by remember { mutableStateOf<Pair<String, Tier>?>(null) }

    fun gated(moduleName: String, required: Tier, action: () -> Unit): () -> Unit = {
        when {
            !licenseUi.loggedIn -> showLoginRequired = true
            licenseUi.revoked   -> showLoginRequired = true
            licenseUi.tier.rank < required.rank -> lockedModule = moduleName to required
            else -> action()
        }
    }

    if (showLicenseDetails) {
        LicenseDetailsDialog(onDismiss = { showLicenseDetails = false })
    }
    if (showLoginRequired) {
        LoginRequiredDialog(
            onDismiss = { showLoginRequired = false },
            onOpenLicenseDetails = { showLicenseDetails = true }
        )
    }
    lockedModule?.let { (name, tier) ->
        UpgradeLockDialog(
            requiredTier = tier,
            moduleName = name,
            onDismiss = { lockedModule = null },
            onOpenLicenseDetails = { lockedModule = null; showLicenseDetails = true }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor   = BgCard,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            title = { Text("Exit OmniCortex", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to exit?") },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false; onExit() },
                    colors  = ButtonDefaults.buttonColors(containerColor = AegisRed),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Exit", color = BgAmoled, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }, shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "SHV Omni-Cortex", color = AegisGreen, fontSize = 25.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp
                )
                Text("Security Operations Console", color = TextMuted, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGoSettings, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Settings, null, tint = TextMuted, modifier = Modifier.size(19.dp))
                }
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                        .background(AegisGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = AegisGreen, modifier = Modifier.size(19.dp))
                }
            }
        }

        // ── Score box + Shizuku panel ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(172.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isCritical = uiState.score in 0..39
            Card(
                modifier = Modifier.width(112.dp).fillMaxHeight(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .then(
                            if (isCritical) Modifier.border(
                                1.dp, AegisRed.copy(alpha = glowAlpha * 0.6f), RoundedCornerShape(14.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "AEGIS", color = TextMuted, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                    )
                    Box(contentAlignment = Alignment.Center) {
                        if (isCritical) {
                            Box(
                                Modifier.size(70.dp).clip(RoundedCornerShape(35.dp))
                                    .background(AegisRed.copy(alpha = glowAlpha * 0.15f))
                            )
                        }
                        AegisScoreRing(
                            score    = if (uiState.score >= 0) uiState.score else 0,
                            ringSize = 70.dp,
                            strokeWidth = 6.dp
                        )
                    }
                    Text(
                        if (uiState.score >= 0 && uiState.lastScan.isNotEmpty()) uiState.lastScan else "no scan",
                        color = TextMuted, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1
                    )
                    Button(
                        onClick  = gated("Device Posture", Tier.FREE, onGoPosture),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = BgAmoled, modifier = Modifier.size(11.dp))
                        Text("Scan", color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AegisPurple.copy(alpha = 0.10f))
                    .border(1.4.dp, AegisPurple.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, tint = AegisPurple, modifier = Modifier.size(12.dp))
                    Text(
                        "SHIZUKU · PRO+", color = AegisPurple, fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                    )
                }
                // UPDATED: Subtitles removed, arguments updated to match new ShizukuCard signature
                ShizukuCard("Firewall", Icons.Default.Shield, glowAlpha, gated("Shizuku Firewall", Tier.PRO_PLUS, onGoShizukuFirewall), Modifier.weight(1f))
                ShizukuCard("Permissions", Icons.Default.AdminPanelSettings, glowAlpha, gated("Shizuku Permissions", Tier.PRO_PLUS, onGoShizukuPermissions), Modifier.weight(1f))
                ShizukuCard("Net Monitor", Icons.Default.Speed, glowAlpha, gated("Shizuku Net Monitor", Tier.PRO_PLUS, onGoShizukuNetMonitor), Modifier.weight(1f))
            }
        }

        // ── Defensive Tools (2 Columns Grid) ──────────────────────────────
        ModuleSection(
            title = "DEFENSIVE TOOLS",
            color = AegisGreen,
            modules = listOf(
                ModuleDef("Device Posture",  Icons.Default.Shield,   AegisGreen,  gated("Device Posture", Tier.FREE, onGoPosture)),
                ModuleDef("App Permissions", Icons.Default.Apps,     AegisPurple, gated("App Permissions", Tier.PRO, onGoApps)),
                ModuleDef("Network Intel",   Icons.Default.Wifi,     AegisCyan,   gated("Network Intel", Tier.PRO, onGoNetwork)),
                ModuleDef("Breach Monitor",  Icons.Default.Security, AegisPurple, gated("Breach Monitor", Tier.FREE, onGoBreach)),
                ModuleDef("Comms Validator", Icons.Default.Lock,     AegisBlue,   gated("Comms Validator", Tier.PRO, onGoComms)),
                ModuleDef("Privacy Shield",  Icons.Default.VpnKey,   AegisCyan,   gated("Privacy Shield", Tier.PRO_PLUS, onGoPrivacyShield), highlighted = true),
            ),
            glowAlpha = glowAlpha
        )

        // ── Recon & Ethical Hacking (2 Columns Grid) ──────────────────────
        ModuleSection(
            title = "RECON & ETHICAL HACKING",
            color = AegisAmber,
            modules = listOf(
                ModuleDef("HTTP Recon",   Icons.Default.Http,    AegisAmber,  gated("HTTP Recon", Tier.FREE, onGoHttpRecon)),
                ModuleDef("Port Scanner", Icons.Default.Radar,   AegisOrange, gated("Port Scanner", Tier.PRO, onGoPortScan)),
                ModuleDef("DNS Intel",    Icons.Default.Dns,     AegisCyan,   gated("DNS Intel", Tier.PRO, onGoDns)),
                ModuleDef("APK Analyser", Icons.Default.Android, AegisPurple, gated("APK Analyser", Tier.PRO, onGoApkAnalyser))
            ),
            glowAlpha = glowAlpha
        )

        // ── Privacy strip ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BgCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PrivacyTip, null, tint = TextMuted, modifier = Modifier.size(12.dp))
            Text(
                "On-device analysis. Use recon tools only on systems you own or have permission to test.",
                color = TextMuted, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 2
            )
        }

        // ── License Details button (dark yellow / amber, per spec) ─────────────
        Button(
            onClick = {
                LicenseState.refresh(gateContext.applicationContext, gateScope)
                showLicenseDetails = true
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AegisAmber)
        ) {
            Icon(Icons.Default.WorkspacePremium, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("License Details", color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private data class ModuleDef(
    val title: String,
    val icon: ImageVector, val color: Color, val onClick: () -> Unit,
    val highlighted: Boolean = false
)

@Composable
private fun ModuleSection(title: String, color: Color, modules: List<ModuleDef>, glowAlpha: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title, color = color, fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
        )
        modules.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { mod -> ModuleTile(mod, glowAlpha, Modifier.weight(1f)) }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// UPDATED: Subtitle parameter removed
@Composable
private fun ShizukuCard(
    title: String, icon: ImageVector, glowAlpha: Float,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(11.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard),
        border   = androidx.compose.foundation.BorderStroke(1.dp, AegisPurple.copy(alpha = glowAlpha * 0.5f))
    ) {
        Row(
            modifier              = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier         = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                    .background(AegisPurple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AegisPurple, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            // UPDATED: Column removed for true vertical centering, fontSize increased to 15.sp
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ModuleTile(mod: ModuleDef, glowAlpha: Float, modifier: Modifier) {
    Card(
        modifier = modifier.height(54.dp).clickable(onClick = mod.onClick),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard),
        border   = if (mod.highlighted)
            androidx.compose.foundation.BorderStroke(1.2.dp, mod.color.copy(alpha = glowAlpha))
        else null
    ) {
        Row(
            modifier              = Modifier
                .fillMaxSize()
                .then(
                    if (mod.highlighted) Modifier.background(mod.color.copy(alpha = 0.06f)) else Modifier
                )
                .padding(horizontal = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(mod.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(mod.icon, null, tint = mod.color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = mod.title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
