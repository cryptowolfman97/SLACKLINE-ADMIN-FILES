package com.snaploader.app.ui.license

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snaploader.app.license.LicenseGateConfig
import com.snaploader.app.license.SHVLicense
import com.snaploader.app.viewmodel.MainViewModel

private val LicenseGold = androidx.compose.ui.graphics.Color(0xFFC9A227)

// ── Mandatory account gate — blocks the whole app for every tier ───────────

@Composable
fun AccountGateDialog(vm: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    val email by vm.loginEmailDraft.collectAsState()
    val password by vm.loginPasswordDraft.collectAsState()
    val remember by vm.loginRememberMe.collectAsState()
    val busy by vm.loginBusy.collectAsState()
    val error by vm.loginError.collectAsState()
    var showPw by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* not dismissable */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(LicenseGateConfig.APP_DISPLAY_NAME, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sign in to your SH Vertex account to continue.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SH VERTEX ACCOUNT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = email, onValueChange = vm::onLoginEmailChange,
                            label = { Text("Email") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password, onValueChange = vm::onLoginPasswordChange,
                            label = { Text("Password") }, singleLine = true,
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPw = !showPw }) {
                                    Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remember, onCheckedChange = vm::onToggleRememberMe)
                            Text("Remember me", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        if (error != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = vm::signIn, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Signing in…" else "Sign In", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { uriHandler.openUri(LicenseGateConfig.ACCOUNT_URL) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Create SH Vertex Account", fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── License details popup ───────────────────────────────────────────────────

@Composable
fun LicenseDetailsDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val tier by vm.licenseTier.collectAsState()
    val accountEmail by vm.accountEmail.collectAsState()
    val deviceCode by vm.deviceCode.collectAsState()
    val checking by vm.licenseChecking.collectAsState()
    val activationCode by vm.activationCodeDraft.collectAsState()
    val activationMessage by vm.activationMessage.collectAsState()
    val activationBusy by vm.activationBusy.collectAsState()
    val requestTier by vm.requestTierSelected.collectAsState()
    var showBank by remember { mutableStateOf(false) }
    var showCrypto by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = LicenseGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("License", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))

                val tierLabel = when (tier) { SHVLicense.Tier.PRO_PLUS -> "PRO+"; SHVLicense.Tier.PRO -> "PRO"; SHVLicense.Tier.FREE -> "FREE" }
                val tierColor = when (tier) { SHVLicense.Tier.PRO_PLUS -> LicenseGold; SHVLicense.Tier.PRO -> MaterialTheme.colorScheme.primary; SHVLicense.Tier.FREE -> MaterialTheme.colorScheme.onSurfaceVariant }

                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (tier != SHVLicense.Tier.FREE) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = tierColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Tier: $tierLabel", color = tierColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (accountEmail != null) Text(accountEmail ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.weight(1f)) { Text("Sign Out") }
                    OutlinedButton(onClick = { vm.refreshAccessSilently(forceUiSpinner = true) }, modifier = Modifier.weight(1f)) {
                        Text(if (checking) "Checking…" else "Refresh")
                    }
                }

                Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

                Text("Activate License", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = activationCode, onValueChange = vm::onActivationCodeChange,
                    label = { Text("Paste your activation code here") }, singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                if (activationMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(activationMessage ?: "", color = if ((activationMessage ?: "").contains("verified", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { clipboard.getText()?.text?.let { vm.onActivationCodeChange(it) } }, modifier = Modifier.weight(1f)) { Text("Paste") }
                    OutlinedButton(onClick = { vm.onActivationCodeChange("") }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::activateLicense, enabled = !activationBusy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (activationBusy) "Activating…" else "Activate License", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

                Text("Request Pro / Pro+ License", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("Your device code is automatically included in the message.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("YOUR DEVICE CODE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(deviceCode, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(deviceCode)) }) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = requestTier == SHVLicense.Tier.PRO,
                        onClick = { vm.onSelectRequestTier(SHVLicense.Tier.PRO) },
                        label = { Text("PRO — ${LicenseGateConfig.PRO_PRICE}") }
                    )
                    FilterChip(
                        selected = requestTier == SHVLicense.Tier.PRO_PLUS,
                        onClick = { vm.onSelectRequestTier(SHVLicense.Tier.PRO_PLUS) },
                        label = { Text("PRO+ — ${LicenseGateConfig.PRO_PLUS_PRICE}") }
                    )
                }
                Spacer(Modifier.height(10.dp))

                val subject = "SHV Downloader ${if (requestTier == SHVLicense.Tier.PRO_PLUS) "Pro+" else "Pro"} License Request"
                val body = "Device Code: $deviceCode\nAccount: ${accountEmail ?: ""}\nTier: $requestTier"
                val encSubject = java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
                val encBody = java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20")

                Button(
                    onClick = { uriHandler.openUri("mailto:${LicenseGateConfig.CONTACT_EMAIL}?subject=$encSubject&body=$encBody") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Request via Email", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val num = LicenseGateConfig.CONTACT_WHATSAPP_NUMBER.filter { it.isDigit() }
                        uriHandler.openUri("https://wa.me/$num?text=$encBody")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Request via WhatsApp", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White) }

                Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

                TextButton(onClick = { showBank = !showBank; showCrypto = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay via Bank Transfer")
                }
                if (showBank) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            CopyRow("Bank", LicenseGateConfig.BANK_NAME, clipboard)
                            CopyRow("Account Name", LicenseGateConfig.BANK_ACCOUNT_NAME, clipboard)
                            CopyRow("Account No.", LicenseGateConfig.BANK_ACCOUNT_NO, clipboard)
                            CopyRow("Branch", LicenseGateConfig.BANK_BRANCH, clipboard)
                            CopyRow("SWIFT / BIC", LicenseGateConfig.BANK_SWIFT, clipboard)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showCrypto = !showCrypto; showBank = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CurrencyBitcoin, null, tint = LicenseGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay via Crypto", color = LicenseGold)
                }
                if (showCrypto) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            CopyRow("USDT — BSC (BEP20)", LicenseGateConfig.CRYPTO_USDT_BEP20, clipboard)
                            CopyRow("USDT — TRC20", LicenseGateConfig.CRYPTO_USDT_TRC20, clipboard)
                            CopyRow("USDT — Plasma", LicenseGateConfig.CRYPTO_USDT_PLASMA, clipboard)
                            CopyRow("ETH", LicenseGateConfig.CRYPTO_ETH, clipboard)
                            CopyRow("LTC", LicenseGateConfig.CRYPTO_LTC, clipboard)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(LicenseGateConfig.PAYMENT_DISCLAIMER, color = LicenseGold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CopyRow(label: String, value: String, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Locked feature dialog ────────────────────────────────────────────────────

@Composable
fun LockedFeatureDialog(requiredTier: SHVLicense.Tier, onDismiss: () -> Unit, onViewLicense: () -> Unit) {
    val label = if (requiredTier == SHVLicense.Tier.PRO_PLUS) "Pro+" else "Pro"
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = { Icon(Icons.Default.Lock, null, tint = LicenseGold) },
        title = { Text("$label Feature", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = { Text("This feature requires a $label license. Upgrade to unlock it.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
        confirmButton = {
            Button(onClick = { onDismiss(); onViewLicense() }, colors = ButtonDefaults.buttonColors(containerColor = LicenseGold)) {
                Text("View License Options", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
