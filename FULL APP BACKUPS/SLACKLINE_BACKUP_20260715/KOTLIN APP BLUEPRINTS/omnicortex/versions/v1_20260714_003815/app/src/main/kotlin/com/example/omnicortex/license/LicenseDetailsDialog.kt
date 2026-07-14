package com.example.omnicortex.license

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.omnicortex.ui.theme.*

private enum class GateTab { STATUS, LOGIN, ACTIVATE, REQUEST, BANK, CRYPTO }

@Composable
fun LicenseDetailsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val ui by LicenseState.state.collectAsState()

    var tab by remember { mutableStateOf(GateTab.STATUS) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    val deviceCode = remember { SHVLicense.getDeviceCode(context) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(BgAmoled)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // ── Header ──────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(LicenseGateConfig.APP_NAME, color = AegisGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("License Details", color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Status card ──────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, color) = when {
                            !ui.loggedIn -> Icons.Default.LockPerson to AegisAmber
                            ui.revoked   -> Icons.Default.Block to AegisRed
                            else         -> Icons.Default.CheckCircle to AegisGreen
                        }
                        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                !ui.loggedIn -> "Not signed in"
                                ui.revoked   -> "License revoked"
                                else         -> "Tier: ${ui.tier.label}"
                            },
                            color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(ui.message, color = TextSecondary, fontSize = 12.sp)
                    if (ui.loggedIn && ui.email.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ui.email, color = TextMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgElevated).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DEVICE CODE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(deviceCode, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        CopyChip(deviceCode)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ui.loggedIn) {
                            OutlinedButton(onClick = {
                                SHVAccount.signOut(context)
                                LicenseState.refresh(context.applicationContext, scope)
                            }, shape = RoundedCornerShape(8.dp)) { Text("Sign Out", color = AegisRed) }
                        }
                        Button(
                            onClick = { LicenseState.refresh(context.applicationContext, scope) },
                            colors = ButtonDefaults.buttonColors(containerColor = BgElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = AegisCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Refresh", color = AegisCyan)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Pricing cards ─────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PriceCard("PRO", LicenseGateConfig.PRO_PRICE_LABEL, AegisCyan, Modifier.weight(1f))
                    PriceCard("PRO+", LicenseGateConfig.PRO_PLUS_PRICE_LABEL, AegisPurple, Modifier.weight(1f))
                }
                Text(LicenseGateConfig.PRICE_TERMS, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.height(14.dp))

                // ── Action tabs ─────────────────────────────────────
                GateActionRow(
                    icon = Icons.Default.VpnKey, label = "Activate License", accent = AegisGreen,
                    expanded = tab == GateTab.ACTIVATE
                ) { tab = if (tab == GateTab.ACTIVATE) GateTab.STATUS else GateTab.ACTIVATE }
                if (tab == GateTab.ACTIVATE) {
                    ActivateSection(context, scope) { msg -> info = msg; error = "" }
                }

                if (!ui.loggedIn) {
                    GateActionRow(
                        icon = Icons.Default.Person, label = "Sign In / Create Account", accent = AegisBlue,
                        expanded = tab == GateTab.LOGIN
                    ) { tab = if (tab == GateTab.LOGIN) GateTab.STATUS else GateTab.LOGIN }
                    if (tab == GateTab.LOGIN) {
                        AccountSection(context, scope, busy, onBusyChange = { busy = it }, onError = { error = it })
                    }
                }

                GateActionRow(
                    icon = Icons.Default.Send, label = "Request Pro License", accent = AegisAmber,
                    expanded = tab == GateTab.REQUEST
                ) { tab = if (tab == GateTab.REQUEST) GateTab.STATUS else GateTab.REQUEST }
                if (tab == GateTab.REQUEST) {
                    RequestSection(context, deviceCode)
                }

                GateActionRow(
                    icon = Icons.Default.AccountBalance, label = "Pay via Bank Transfer", accent = AegisGreen,
                    expanded = tab == GateTab.BANK
                ) { tab = if (tab == GateTab.BANK) GateTab.STATUS else GateTab.BANK }
                if (tab == GateTab.BANK) {
                    BankSection(context, deviceCode)
                }

                if (listOf(
                        LicenseGateConfig.CRYPTO_USDT_BSC, LicenseGateConfig.CRYPTO_USDT_TRC20,
                        LicenseGateConfig.CRYPTO_ETH, LicenseGateConfig.CRYPTO_LTC, LicenseGateConfig.CRYPTO_BTC
                    ).any { it.isNotBlank() }
                ) {
                    GateActionRow(
                        icon = Icons.Default.CurrencyBitcoin, label = "Pay via Crypto", accent = AegisOrange,
                        expanded = tab == GateTab.CRYPTO
                    ) { tab = if (tab == GateTab.CRYPTO) GateTab.STATUS else GateTab.CRYPTO }
                    if (tab == GateTab.CRYPTO) {
                        CryptoSection(deviceCode)
                    }
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = AegisRed, fontSize = 12.sp)
                }
                if (info.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(info, color = AegisGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PriceCard(label: String, price: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(price, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GateActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (expanded) accent.copy(alpha = 0.10f) else BgCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            null, tint = accent, modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CopyChip(text: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = { scope.launch { clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(ClipData.newPlainText("copy", text))) } },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Default.ContentCopy, null, tint = AegisCyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp)); Text("Copy", color = AegisCyan, fontSize = 12.sp)
    }
}

@Composable
private fun ActivateSection(context: Context, scope: kotlinx.coroutines.CoroutineScope, onResult: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = code, onValueChange = { code = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste your activation code here") },
            minLines = 3
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val payload: JSONObject = try { SHVLicense.decodeTokenPublic(code).first } catch (e: Exception) { JSONObject() }
                val result = SHVLicense.checkLicense(code, context, checkRevocation = true)
                if (result.valid) {
                    SHVLicense.saveLicense(context, code, payload)
                    onResult("License activated: ${result.tier}")
                    LicenseState.refresh(context.applicationContext, scope)
                } else {
                    err = result.message
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AegisGreen),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Activate License", color = BgAmoled, fontWeight = FontWeight.Bold) }
        if (err.isNotBlank()) Text(err, color = AegisRed, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun AccountSection(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                onBusyChange(true)
                scope.launch {
                    try {
                        SHVAccount.signIn(context, email, password)
                        LicenseState.refresh(context.applicationContext, scope)
                    } catch (e: Exception) { onError(e.message ?: "Sign in failed") }
                    onBusyChange(false)
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AegisBlue),
            shape = RoundedCornerShape(10.dp)
        ) { Text(if (busy) "Please wait…" else "Sign In", color = BgAmoled, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                onBusyChange(true)
                scope.launch {
                    try {
                        SHVAccount.signUp(context, email, password)
                        LicenseState.refresh(context.applicationContext, scope)
                    } catch (e: Exception) { onError(e.message ?: "Sign up failed") }
                    onBusyChange(false)
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Create SH Vertex Account", color = AegisPurple) }
    }
}

@Composable
private fun RequestSection(context: Context, deviceCode: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Your device code is automatically included in the message.", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgCard).padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(deviceCode, color = AegisCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            CopyChip(deviceCode)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val body = "I'd like to request a Pro/Pro+ license for ${LicenseGateConfig.APP_NAME}. Device code: $deviceCode"
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${LicenseGateConfig.CONTACT_EMAIL}")
                    putExtra(Intent.EXTRA_SUBJECT, "${LicenseGateConfig.APP_NAME} — License Request")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AegisBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Email, null, tint = BgAmoled, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp)); Text("Request via Email", color = BgAmoled, fontWeight = FontWeight.Bold)
        }
        if (LicenseGateConfig.CONTACT_WHATSAPP_NUMBER.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val body = "I'd like to request a Pro/Pro+ license for ${LicenseGateConfig.APP_NAME}. Device code: $deviceCode"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/${LicenseGateConfig.CONTACT_WHATSAPP_NUMBER}?text=${Uri.encode(body)}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Chat, null, tint = BgAmoled, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Request via WhatsApp", color = BgAmoled, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BankSection(context: Context, deviceCode: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            "Transfer the license amount to the account below, then notify us.",
            color = TextSecondary, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        BankRow("BANK", LicenseGateConfig.BANK_NAME)
        BankRow("ACCOUNT NAME", LicenseGateConfig.BANK_ACCOUNT_NAME)
        BankRow("ACCOUNT NO.", LicenseGateConfig.BANK_ACCOUNT_NO)
        BankRow("BRANCH", LicenseGateConfig.BANK_BRANCH)
        BankRow("SWIFT / BIC", LicenseGateConfig.BANK_SWIFT)
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(AegisAmber.copy(alpha = 0.08f)).padding(12.dp)
        ) {
            Text(LicenseGateConfig.PAYMENT_DISCLAIMER, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val body = "I've paid for ${LicenseGateConfig.APP_NAME} via bank transfer. Device code: $deviceCode"
                context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${LicenseGateConfig.CONTACT_EMAIL}")
                    putExtra(Intent.EXTRA_SUBJECT, "${LicenseGateConfig.APP_NAME} — Payment Notification")
                    putExtra(Intent.EXTRA_TEXT, body)
                })
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AegisBlue),
            shape = RoundedCornerShape(10.dp)
        ) { Text("I've Paid — Notify via Email", color = BgAmoled, fontWeight = FontWeight.Bold) }
        if (LicenseGateConfig.CONTACT_WHATSAPP_NUMBER.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val body = "I've paid for ${LicenseGateConfig.APP_NAME} via bank transfer. Device code: $deviceCode"
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/${LicenseGateConfig.CONTACT_WHATSAPP_NUMBER}?text=${Uri.encode(body)}")
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text("I've Paid — Notify via WhatsApp", color = BgAmoled, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BankRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
            .background(BgCard).padding(10.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CryptoSection(deviceCode: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        CryptoRow("USDT (BEP20)", LicenseGateConfig.CRYPTO_USDT_BSC)
        CryptoRow("USDT (TRC20)", LicenseGateConfig.CRYPTO_USDT_TRC20)
        CryptoRow("ETH", LicenseGateConfig.CRYPTO_ETH)
        CryptoRow("LTC", LicenseGateConfig.CRYPTO_LTC)
        CryptoRow("BTC", LicenseGateConfig.CRYPTO_BTC)
    }
}

@Composable
private fun CryptoRow(label: String, address: String) {
    if (address.isBlank()) return
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
            .background(BgCard).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(address, color = TextPrimary, fontSize = 12.sp, maxLines = 1)
        }
        CopyChip(address)
    }
}
