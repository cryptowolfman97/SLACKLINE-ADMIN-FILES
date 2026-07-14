package com.shvertex.supaadmin.ui.license

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shvertex.supaadmin.data.SupabaseUiState
import com.shvertex.supaadmin.data.SupabaseViewModel
import com.shvertex.supaadmin.license.LicenseGateConfig
import com.shvertex.supaadmin.license.SupaLicense
import com.shvertex.supaadmin.ui.components.*
import com.shvertex.supaadmin.ui.theme.*

// ── Mandatory account gate ──────────────────────────────────────────────────
// Shown whenever there's no signed-in SHVertex account. Blocks the whole app —
// applies to every tier (Free included) per spec. Not dismissable.

@Composable
fun AccountGateDialog(vm: SupabaseViewModel, state: SupabaseUiState) {
    val uriHandler = LocalUriHandler.current
    Dialog(
        onDismissRequest = { /* not dismissable */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SupaGreen.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Lock, null, tint = SupaGreen, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("Supa Studio by SHV", color = SupaGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sign in to your SH Vertex account to continue.",
                    color = SubText, fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                SCard(modifier = Modifier.fillMaxWidth()) {
                    Text("SH VERTEX ACCOUNT", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    STextField(
                        value = state.loginEmailDraft,
                        onValueChange = vm::onLoginEmailChange,
                        label = "Email",
                        hint = "you@example.com"
                    )
                    Spacer(Modifier.height(10.dp))
                    STextField(
                        value = state.loginPasswordDraft,
                        onValueChange = vm::onLoginPasswordChange,
                        label = "Password",
                        isPassword = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.loginRememberMe,
                            onCheckedChange = vm::onToggleRememberMe,
                            colors = CheckboxDefaults.colors(checkedColor = SupaGreen)
                        )
                        Text("Remember me", color = SubText, fontSize = 13.sp)
                    }
                    if (state.loginError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(state.loginError, color = ErrorCol, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        if (state.loginBusy) "Signing in…" else "Sign In",
                        onClick = vm::signIn,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loginBusy
                    )
                    Spacer(Modifier.height(10.dp))
                    SDivider()
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        "Create SH Vertex Account",
                        onClick = { uriHandler.openUri(LicenseGateConfig.ACCOUNT_URL) },
                        modifier = Modifier.fillMaxWidth(),
                        color = PurpleCol
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── License details popup — opened from the "License" button on Dashboard ──

@Composable
fun LicenseDetailsDialog(vm: SupabaseViewModel, state: SupabaseUiState) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = { vm.dismissLicenseDialog() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, CardBg3, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = LicenseGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("License", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.dismissLicenseDialog() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = SubText, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))

                val tierLabel = when (state.licenseTier) {
                    SupaLicense.Tier.PRO_PLUS -> "PRO+"
                    SupaLicense.Tier.PRO -> "PRO"
                    SupaLicense.Tier.FREE -> "FREE"
                }
                val tierColor = when (state.licenseTier) {
                    SupaLicense.Tier.PRO_PLUS -> LicenseGold
                    SupaLicense.Tier.PRO -> SupaGreen
                    SupaLicense.Tier.FREE -> SubText
                }

                SCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (state.licenseTier != SupaLicense.Tier.FREE) Icons.Default.CheckCircle else Icons.Default.Info,
                            null, tint = tierColor, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Tier: $tierLabel", color = tierColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (state.accountEmail != null) {
                                Text(state.accountEmail, color = SubText, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Sign Out", onClick = { vm.signOut() }, modifier = Modifier.weight(1f), color = ErrorCol)
                    SecondaryButton(
                        if (state.licenseChecking) "Checking…" else "Refresh",
                        onClick = { vm.refreshAccessSilently(forceUiSpinner = true) },
                        modifier = Modifier.weight(1f),
                        color = InfoCol
                    )
                }

                Spacer(Modifier.height(16.dp))
                SDivider()
                Spacer(Modifier.height(16.dp))

                // ── Activate license ────────────────────────────────────────
                SectionHeader("Activate License")
                Spacer(Modifier.height(8.dp))
                STextField(
                    value = state.activationCodeDraft,
                    onValueChange = vm::onActivationCodeChange,
                    label = "Paste your activation code here",
                    singleLine = false
                )
                if (state.activationMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.activationMessage, color = if (state.activationMessage.contains("verified", true)) SuccessCol else ErrorCol, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        "Paste from Clipboard",
                        onClick = { clipboard.getText()?.text?.let { vm.onActivationCodeChange(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton("Clear", onClick = { vm.onActivationCodeChange("") }, modifier = Modifier.weight(1f), color = ErrorCol)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    if (state.activationBusy) "Activating…" else "Activate License",
                    onClick = vm::activateLicense,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.activationBusy
                )

                Spacer(Modifier.height(16.dp))
                SDivider()
                Spacer(Modifier.height(16.dp))

                // ── Request a license ───────────────────────────────────────
                SectionHeader("Request Pro / Pro+ License")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your device code is automatically included in the message.",
                    color = SubText, fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(CardBg2).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YOUR DEVICE CODE", color = MutedText, fontSize = 10.sp)
                        Text(state.deviceCode, color = InfoCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(state.deviceCode)) }) {
                        Icon(Icons.Default.ContentCopy, null, tint = InfoCol, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("License tier requested:", color = SubText, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClickChip("PRO — ${LicenseGateConfig.PRO_PRICE}", if (state.requestTierSelected == SupaLicense.Tier.PRO) SupaGreen else SubText) {
                        vm.onSelectRequestTier(SupaLicense.Tier.PRO)
                    }
                    ClickChip("PRO+ — ${LicenseGateConfig.PRO_PLUS_PRICE}", if (state.requestTierSelected == SupaLicense.Tier.PRO_PLUS) LicenseGold else SubText) {
                        vm.onSelectRequestTier(SupaLicense.Tier.PRO_PLUS)
                    }
                }
                Spacer(Modifier.height(10.dp))

                val requestSubject = "Supa Studio ${if (state.requestTierSelected == SupaLicense.Tier.PRO_PLUS) "Pro+" else "Pro"} License Request"
                val requestBody = "Device Code: ${state.deviceCode}\nAccount: ${state.accountEmail ?: ""}\nTier: ${state.requestTierSelected}"
                val encSubject = java.net.URLEncoder.encode(requestSubject, "UTF-8").replace("+", "%20")
                val encBody = java.net.URLEncoder.encode(requestBody, "UTF-8").replace("+", "%20")

                PrimaryButton(
                    "Request via Email",
                    onClick = { uriHandler.openUri("mailto:${LicenseGateConfig.CONTACT_EMAIL}?subject=$encSubject&body=$encBody") },
                    modifier = Modifier.fillMaxWidth(),
                    color = InfoCol, textColor = Color.White
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    "Request via WhatsApp",
                    onClick = {
                        val num = LicenseGateConfig.CONTACT_WHATSAPP_NUMBER.filter { it.isDigit() }
                        uriHandler.openUri("https://wa.me/$num?text=$encBody")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = SuccessCol, textColor = Color.Black
                )

                Spacer(Modifier.height(16.dp))
                SDivider()
                Spacer(Modifier.height(16.dp))

                // ── Payment panels ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { vm.toggleBankPanel() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalance, null, tint = SupaGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay via Bank Transfer", color = SupaGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                if (state.showBankPanel) {
                    Spacer(Modifier.height(8.dp))
                    SCard2(modifier = Modifier.fillMaxWidth()) {
                        CopyRow("Bank", LicenseGateConfig.BANK_NAME)
                        CopyRow("Account Name", LicenseGateConfig.BANK_ACCOUNT_NAME)
                        CopyRow("Account No.", LicenseGateConfig.BANK_ACCOUNT_NO)
                        CopyRow("Branch", LicenseGateConfig.BANK_BRANCH)
                        CopyRow("SWIFT / BIC", LicenseGateConfig.BANK_SWIFT)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { vm.toggleCryptoPanel() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CurrencyBitcoin, null, tint = LicenseGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay via Crypto", color = LicenseGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                if (state.showCryptoPanel) {
                    Spacer(Modifier.height(8.dp))
                    SCard2(modifier = Modifier.fillMaxWidth()) {
                        CopyRow("USDT — BSC (BEP20)", LicenseGateConfig.CRYPTO_USDT_BEP20)
                        CopyRow("USDT — TRC20", LicenseGateConfig.CRYPTO_USDT_TRC20)
                        CopyRow("USDT — Plasma", LicenseGateConfig.CRYPTO_USDT_PLASMA)
                        CopyRow("ETH", LicenseGateConfig.CRYPTO_ETH)
                        CopyRow("LTC", LicenseGateConfig.CRYPTO_LTC)
                    }
                }

                Spacer(Modifier.height(14.dp))
                InfoBanner(LicenseGateConfig.PAYMENT_DISCLAIMER, color = LicenseGold)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Locked feature dialog ────────────────────────────────────────────────────

@Composable
fun LockedFeatureDialog(requiredTier: SupaLicense.Tier, vm: SupabaseViewModel) {
    val label = if (requiredTier == SupaLicense.Tier.PRO_PLUS) "Pro+" else "Pro"
    AlertDialog(
        onDismissRequest = { vm.dismissLockedDialog() },
        containerColor = CardBg2,
        icon = { Icon(Icons.Default.Lock, null, tint = LicenseGold) },
        title = { Text("$label Feature", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This feature requires a $label license. Upgrade to unlock it.",
                color = SubText, fontSize = 13.sp
            )
        },
        confirmButton = {
            PrimaryButton("View License Options", onClick = {
                vm.dismissLockedDialog()
                vm.showLicenseDialog()
            }, color = LicenseGold, textColor = Color.Black)
        },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissLockedDialog() }) }
    )
}
