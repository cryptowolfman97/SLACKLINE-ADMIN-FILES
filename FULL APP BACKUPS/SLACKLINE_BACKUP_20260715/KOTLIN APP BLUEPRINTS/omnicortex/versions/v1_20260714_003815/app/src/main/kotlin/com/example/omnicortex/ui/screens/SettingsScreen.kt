package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val appPin       by vm.appPin.collectAsState(initial = "")
    val hasBiometric by vm.biometric.collectAsState(initial = false)
    val notifs       by vm.notifications.collectAsState(initial = true)
    val darkTheme    by vm.darkTheme.collectAsState(initial = true)
    val pinState     by vm.pinSetupState.collectAsState()
    val toast        by vm.toast.collectAsState()
    val snack        = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) { snack.showSnackbar(toast); vm.consumeToast() }
    }

    if (pinState !is SettingsViewModel.PinSetupState.Idle &&
        pinState !is SettingsViewModel.PinSetupState.Success) {
        PinSetupDialog(
            state           = pinState,
            onSubmitCurrent = { vm.submitCurrentPin(it, appPin) },
            onSubmitNew     = { vm.submitNewPin(it) },
            onConfirmNew    = { vm.confirmNewPin(it) },
            onDismiss       = { vm.resetPinSetup() }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snack) },
        containerColor = BgAmoled
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BgAmoled)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = AegisGreen)
                }
                Column {
                    Text("Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("SHV Omni-Cortex preferences", color = TextMuted, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Security ──────────────────────────────────────────────────
                SettingsSection(title = "Security", icon = Icons.Default.Lock, color = AegisGreen) {
                    SettingsRow(
                        icon     = Icons.Default.Pin,
                        color    = AegisGreen,
                        title    = "App PIN Lock",
                        subtitle = if (appPin.isNotEmpty()) "PIN is set — tap to change" else "No PIN set — tap to create",
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                if (appPin.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick  = { vm.removePin() },
                                        shape    = RoundedCornerShape(8.dp),
                                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = AegisRed),
                                        border   = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(AegisRed.copy(alpha = 0.5f))
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick  = { vm.startPinSetup(appPin) },
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        if (appPin.isNotEmpty()) "Change" else "Set PIN",
                                        color = BgAmoled, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                    
                    HorizontalDivider(color = BgCardBorder)
                    
                    SettingsRow(
                        icon     = Icons.Default.Fingerprint,
                        color    = AegisGreen,
                        title    = "Biometric Unlock",
                        subtitle = if (appPin.isEmpty()) "Set a PIN first to enable" else "Unlock with fingerprint/face",
                        trailing = {
                            Switch(
                                checked         = hasBiometric,
                                onCheckedChange = { vm.setBiometric(it) },
                                enabled         = appPin.isNotEmpty(), // Only allowed if PIN exists
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor   = BgAmoled,
                                    checkedTrackColor   = AegisGreen,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BgElevated,
                                    disabledCheckedTrackColor = BgElevated,
                                    disabledUncheckedTrackColor = BgElevated
                                )
                            )
                        }
                    )
                }

                // ── Notifications ─────────────────────────────────────────────
                SettingsSection(title = "Notifications", icon = Icons.Default.Notifications, color = AegisAmber) {
                    SettingsRow(
                        icon     = Icons.Default.NotificationsActive,
                        color    = AegisAmber,
                        title    = "Breach Alerts",
                        subtitle = if (notifs) "Push alerts when new breaches are found" else "Breach notifications disabled",
                        trailing = {
                            Switch(
                                checked         = notifs,
                                onCheckedChange = { vm.setNotifications(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor   = BgAmoled,
                                    checkedTrackColor   = AegisAmber,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BgElevated
                                )
                            )
                        }
                    )
                }

                // ── Appearance ────────────────────────────────────────────────
                SettingsSection(title = "Appearance", icon = Icons.Default.Palette, color = AegisPurple) {
                    SettingsRow(
                        icon     = Icons.Default.DarkMode,
                        color    = AegisPurple,
                        title    = "Dark Theme",
                        subtitle = if (darkTheme) "AMOLED dark mode active" else "Light mode active",
                        trailing = {
                            Switch(
                                checked         = darkTheme,
                                onCheckedChange = { vm.setDarkTheme(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor   = BgAmoled,
                                    checkedTrackColor   = AegisPurple,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BgElevated
                                )
                            )
                        }
                    )
                }

                // ── About ─────────────────────────────────────────────────────
                SettingsSection(title = "About", icon = Icons.Default.Info, color = AegisBlue) {
                    SettingsRow(
                        icon     = Icons.Default.Shield,
                        color    = AegisGreen,
                        title    = "SHV Omni-Cortex",
                        subtitle = "Version 1.0  •  by SHV Vertex Technologies"
                    )
                    HorizontalDivider(color = BgCardBorder)
                    SettingsRow(
                        icon     = Icons.Default.PrivacyTip,
                        color    = AegisBlue,
                        title    = "Privacy",
                        subtitle = "All data stays on-device. No telemetry. No ads. Ever."
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Settings section card ─────────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String, icon: ImageVector, color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Text(title.uppercase(), color = color, fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
        }
    }
}

// ── Single settings row ───────────────────────────────────────────────────────
@Composable
private fun SettingsRow(
    icon: ImageVector, color: Color, title: String, subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title,    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted,   fontSize = 11.sp, lineHeight = 15.sp)
        }
        trailing?.invoke()
    }
}

// ── PIN setup dialog ──────────────────────────────────────────────────────────
@Composable
private fun PinSetupDialog(
    state: SettingsViewModel.PinSetupState,
    onSubmitCurrent: (String) -> Unit,
    onSubmitNew: (String) -> Unit,
    onConfirmNew: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    LaunchedEffect(state) { input = "" }

    val (title, subtitle, buttonLabel) = when (state) {
        is SettingsViewModel.PinSetupState.EnterCurrent -> Triple("Change PIN",  "Enter your current PIN to continue",  "Verify")
        is SettingsViewModel.PinSetupState.EnterNew     -> Triple("Set New PIN", "Enter a new 4–6 digit PIN",           "Continue")
        is SettingsViewModel.PinSetupState.ConfirmNew   -> Triple("Confirm PIN", "Enter your new PIN again to confirm", "Confirm")
        is SettingsViewModel.PinSetupState.Error        -> Triple("Error",       state.msg,                             "Retry")
        else -> Triple("PIN Setup", "", "Continue")
    }

    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = BgCard,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    subtitle,
                    color    = if (state is SettingsViewModel.PinSetupState.Error) AegisRed else TextSecondary,
                    fontSize = 13.sp
                )
                if (state !is SettingsViewModel.PinSetupState.Error) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(6) { i ->
                            Box(
                                modifier = Modifier.padding(4.dp).size(14.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (i < input.length) AegisGreen else BgCardBorder)
                            )
                        }
                    }
                    PinPad(
                        onDigit  = { if (input.length < 6) input += it },
                        onDelete = { if (input.isNotEmpty()) input = input.dropLast(1) },
                        onClear  = { input = "" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (state) {
                        is SettingsViewModel.PinSetupState.EnterCurrent -> onSubmitCurrent(input)
                        is SettingsViewModel.PinSetupState.EnterNew     -> onSubmitNew(input)
                        is SettingsViewModel.PinSetupState.ConfirmNew   -> onConfirmNew(input)
                        is SettingsViewModel.PinSetupState.Error        -> onDismiss()
                        else -> {}
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                shape  = RoundedCornerShape(8.dp)
            ) { Text(buttonLabel, color = BgAmoled, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit, onClear: () -> Unit) {
    val rows = listOf(
        listOf("1","2","3"), listOf("4","5","6"),
        listOf("7","8","9"), listOf("C","0","⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Button(
                        onClick  = { when (key) { "C" -> onClear(); "⌫" -> onDelete(); else -> onDigit(key) } },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = when (key) {
                                "C"  -> AegisRed.copy(alpha = 0.2f)
                                "⌫" -> AegisAmber.copy(alpha = 0.2f)
                                else -> BgElevated
                            }
                        )
                    ) {
                        Text(
                            key,
                            color      = when (key) { "C" -> AegisRed; "⌫" -> AegisAmber; else -> TextPrimary },
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
