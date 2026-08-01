package com.shvertex.simplibudgetrevamped.shvgate

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shvertex.simplibudgetrevamped.license.SHVAccount
import com.shvertex.simplibudgetrevamped.license.SHVLicense

// ── AMOLED palette ────────────────────────────────────────────────────
private val Bg        = Color(0xFF000000)
private val Surface   = Color(0xFF0D0D0D)
private val Card      = Color(0xFF141414)
private val CardAlt   = Color(0xFF1A1A1A)
private val Border    = Color(0xFF262626)
private val Green     = Color(0xFF00E676)
private val Cyan      = Color(0xFF00BCD4)
private val Amber     = Color(0xFFFFB300)
private val Red       = Color(0xFFFF5252)
private val Purple    = Color(0xFFCE93D8)
private val Wa        = Color(0xFF25D366)
private val Blue      = Color(0xFF1565C0)
private val OnPrimary = Color(0xFF000000)
private val TextPri   = Color(0xFFFFFFFF)
private val TextSec   = Color(0xFF9E9E9E)
private val TextMute  = Color(0xFF616161)

// ── Injection-time baked constants ────────────────────────────────────
private const val APP_NAME      = "SIMPLI-BUDGET REVAMPED"
private const val PRO_PRICE     = "$9.99"
private const val GRACE_HOURS   = 6
private const val DISCLAIMER    = "After payment, tap your preferred contact method below. Your device code will be included automatically. We will verify your payment and deliver your Pro license key within 4 hours. Your trial will be extended immediately upon payment confirmation so you can keep using the app."
private const val BANK_NAME     = "Nations Trust Bank - Sri Lanka"
private const val BANK_ACCNAME  = "Sachith Sanka"
private const val BANK_ACCOUNT  = "200080074322"
private const val BANK_BRANCH   = "Pettah 01 / Pettah - Main Street"
private const val BANK_SWIFT    = "NTBCLKLX"
private const val USDT_BSC      = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
private const val USDT_TRC      = "TBXiwbhm59cxmzw78CtPD9kgxShZ38WSFS"
private const val USDT_PLASMA   = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
private const val ETH_ADDR      = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
private const val LTC_ADDR      = "LUmyxv1CYNEkJHXoh77ZDzN6wxKhCe8QgG"
private const val CONTACT_EMAIL = "ceo.shvertex@gmail.com"
private const val CONTACT_WA    = "+94771363462"
private const val ACCOUNT_URL   = "https://shvertex.online/account.html"

@Composable
fun LicenseGateScreen(context: Context, onAccessGranted: () -> Unit) {
    val scope      = rememberCoroutineScope()
    val clipboard  = LocalClipboardManager.current
    val deviceCode = remember { SHVLicense.getDeviceCode(context) }

    var isLoading      by remember { mutableStateOf(true) }
    var canContinue    by remember { mutableStateOf(false) }
    var statusMsg      by remember { mutableStateOf("Verifying access\u2026") }
    var accessMode     by remember { mutableStateOf("none") }
    var licenseId      by remember { mutableStateOf("") }
    var licenseTier    by remember { mutableStateOf("") }
    var trialRemaining by remember { mutableStateOf("") }
    var accountEmail   by remember { mutableStateOf("") }
    var accountPlan    by remember { mutableStateOf("") }
    var isSignedIn     by remember { mutableStateOf(false) }

    var showActivate   by remember { mutableStateOf(false) }
    var showSignIn     by remember { mutableStateOf(false) }
    var showRequest    by remember { mutableStateOf(false) }
    var showPayBank    by remember { mutableStateOf(false) }
    var showPayCrypto  by remember { mutableStateOf(false) }
    var activationCode by remember { mutableStateOf("") }
    var emailField     by remember { mutableStateOf("") }
    var passwordField  by remember { mutableStateOf("") }
    var fieldError       by remember { mutableStateOf("") }
    var copiedKey        by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var rememberMe       by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun copyToClipboard(text: String, key: String) {
        clipboard.setText(AnnotatedString(text)); copiedKey = key
        scope.launch { delay(2000); copiedKey = "" }
    }

    fun refreshSession() {
        val s = SHVAccount.loadSession(context)
        isSignedIn = s != null; accountEmail = s?.email ?: ""; accountPlan = s?.plan ?: ""
    }

    fun checkAccess() {
        scope.launch {
            isLoading = true; fieldError = ""; refreshSession()
            val access = withContext(Dispatchers.IO) { SHVAccount.getAccessStatus(context) }
            isLoading = false; accessMode = access.mode; canContinue = access.valid
            when (access.mode) {
                "licensed" -> { licenseId = access.licenseId ?: ""; licenseTier = access.tier?.uppercase() ?: "PRO"; statusMsg = "License active." }
                "trial"    -> { trialRemaining = access.trialState?.remainingText ?: ""; statusMsg = access.trialState?.message ?: "Trial active." }
                else       -> statusMsg = access.message.ifBlank { "No active license or trial." }
            }
        }
    }

    fun requestViaEmail(mode: String = "license") {
        val subject = Uri.encode(
            if (mode == "payment") "Pro License Payment — $APP_NAME"
            else "Pro License Request — $APP_NAME")
        val body = Uri.encode(
            if (mode == "payment")
                "Hello,\n\nI have completed payment for a Pro License for $APP_NAME.\n" +
                "Device Code: $deviceCode\n\nPlease process my license key.\n\nThank you."
            else
                "Hello,\n\nI want a Pro License for $APP_NAME.\n" +
                "Device Code: $deviceCode\n\nPlease send payment instructions.\n\nThank you.")
        openUrl("mailto:$CONTACT_EMAIL?subject=$subject&body=$body")
    }

    fun requestViaWhatsApp(mode: String = "license") {
        val msg = Uri.encode(
            if (mode == "payment")
                "Hello! I have completed payment for a Pro License for *$APP_NAME*.\n" +
                "\uD83D\uDCF1 Device Code: *$deviceCode*\n\nPlease process my license key. Thank you!"
            else
                "Hello! I want a Pro License for *$APP_NAME*.\n" +
                "\uD83D\uDCF1 Device Code: *$deviceCode*\n\nPlease send payment instructions. Thank you!")
        val number = CONTACT_WA.replace("+", "").replace(" ", "")
        try { openUrl("whatsapp://send?phone=$number&text=$msg") }
        catch (e: Exception) { openUrl("https://wa.me/$number?text=$msg") }
    }

    // Load remembered email on first composition
    LaunchedEffect(Unit) {
        // Load remembered email from private prefs — stored in app's own filesDir, inaccessible to users
        val prefs = context.getSharedPreferences(
            "shv_gate_prefs_${SHVAccount.APP_CODE}", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("remembered_email", "")
        if (!saved.isNullOrBlank()) { emailField = saved; rememberMe = true }
        checkAccess()
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── App Header ────────────────────────────────────────────
            Box(modifier = Modifier.size(68.dp).clip(CircleShape)
                .background(Card).border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(if (canContinue) Icons.Default.LockOpen else Icons.Default.Lock,
                    null, tint = if (canContinue) Green else Cyan, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(APP_NAME, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = TextPri, textAlign = TextAlign.Center)
            Text("by SH Vertex", fontSize = 12.sp, color = Cyan,
                letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))

            // ── Device Code Card ──────────────────────────────────────
            GateCard {
                Label("DEVICE CODE")
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(deviceCode, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = TextPri, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                    CopyButton(
                        copied = copiedKey == "device",
                        onClick = { copyToClipboard(deviceCode, "device") }
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Border, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp),
                            color = Cyan, strokeWidth = 2.dp)
                        Text("Checking access\u2026", fontSize = 12.sp, color = TextSec)
                    }
                } else {
                    val dot = when (accessMode) { "licensed" -> Green; "trial" -> Amber; else -> Red }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dot))
                        Text(statusMsg, fontSize = 12.sp, color = dot)
                    }
                }
            }

            // ── Pro Price Card ────────────────────────────────────────
            GateCard(borderColor = Green.copy(alpha = 0.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Amber.copy(alpha = 0.1f))
                        .border(1.dp, Amber.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, null, tint = Amber, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Label("PRO LICENSE")
                        Spacer(Modifier.height(2.dp))
                        Text(PRO_PRICE, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                        Text("Lifetime \u00b7 Device-bound \u00b7 No subscription",
                            fontSize = 11.sp, color = TextSec)
                    }
                }
            }

            // ── Status cards ──────────────────────────────────────────
            AnimatedVisibility(visible = isSignedIn, enter = fadeIn(), exit = fadeOut()) {
                GateCard(borderColor = Cyan.copy(alpha = 0.25f)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Surface).border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Cyan, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Label("SH VERTEX ACCOUNT")
                            Spacer(Modifier.height(2.dp))
                            Text(accountEmail, fontSize = 13.sp, color = TextPri, fontWeight = FontWeight.Medium)
                            if (accountPlan.isNotBlank())
                                Text("Plan: $accountPlan", fontSize = 11.sp, color = TextSec)
                        }
                        StatusBadge("ACTIVE", Green)
                    }
                }
            }

            AnimatedVisibility(visible = accessMode == "licensed" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Green.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(22.dp))
                        Column {
                            Label("PRO LICENSE ACTIVE")
                            Spacer(Modifier.height(2.dp))
                            Text("Tier: $licenseTier  \u00b7  Lifetime",
                                fontSize = 14.sp, color = Green, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (licenseId.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ID: $licenseId", fontSize = 10.sp, color = TextMute,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            CopyButton(copied = copiedKey == "licid",
                                onClick = { copyToClipboard(licenseId, "licid") }, small = true)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = accessMode == "trial" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Amber.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Timer, null, tint = Amber, modifier = Modifier.size(22.dp))
                        Column {
                            Label("TRIAL SESSION")
                            Spacer(Modifier.height(2.dp))
                            Text(if (trialRemaining.isNotBlank()) "$trialRemaining remaining"
                                 else "Trial active",
                                fontSize = 14.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Upgrade to Pro for permanent access.", fontSize = 11.sp, color = TextSec)
                }
            }

            // ── Account action row ────────────────────────────────────
            AnimatedVisibility(visible = isSignedIn && !isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlatOutlineButton("Sign Out", Red, modifier = Modifier.weight(1f)) {
                        SHVAccount.clearSession(context)
                        isSignedIn = false; accountEmail = ""; accountPlan = ""
                        accessMode = "none"; canContinue = false; statusMsg = "Signed out."
                    }
                    FlatOutlineButton("Refresh", Purple, modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh) { checkAccess() }
                }
            }

            // ── Continue button ───────────────────────────────────────
            AnimatedVisibility(visible = canContinue && !isLoading) {
                Button(onClick = { onAccessGranted() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue to App", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnPrimary)
                }
            }

            // ── Section divider ───────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Border, thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            // ── Activate License ──────────────────────────────────────
            GateSectionButton("Activate License", showActivate, Green,
                icon = Icons.Default.VpnKey) {
                showActivate = !showActivate
                showSignIn = false; showRequest = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showActivate,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Green.copy(alpha = 0.25f)) {
                    Label("PASTE ACTIVATION CODE")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = activationCode,
                        onValueChange = { activationCode = it; fieldError = "" },
                        placeholder = { Text("Paste your activation code here", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        colors = gateTextFieldColors(Green))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatOutlineButton("Paste from Clipboard", Cyan, modifier = Modifier.weight(1f)) {
                            clipboard.getText()?.text?.let { if (it.isNotBlank()) activationCode = it }
                        }
                        FlatOutlineButton("Clear", Red, modifier = Modifier.weight(1f)) { activationCode = "" }
                    }
                    ErrorRow(fieldError)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        val code = activationCode.trim()
                        if (code.isBlank()) { fieldError = "Paste your activation code first."; return@Button }
                        isLoading = true; statusMsg = "Verifying\u2026"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { SHVLicense.checkLicense(code, context) }
                            isLoading = false
                            if (result.valid) {
                                withContext(Dispatchers.IO) {
                                    val (payload, _) = SHVLicense.decodeTokenPublic(code)
                                    SHVLicense.saveLicense(context, code, payload)
                                }
                                showActivate = false; checkAccess()
                            } else { fieldError = result.message; statusMsg = result.message }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(10.dp)) {
                        Text("Activate Pro License", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OnPrimary)
                    }
                }
            }

            // ── Request Pro License ───────────────────────────────────
            GateSectionButton("Request Pro License", showRequest, Amber,
                icon = Icons.Default.Send) {
                showRequest = !showRequest
                showActivate = false; showSignIn = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showRequest,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Amber.copy(alpha = 0.3f)) {
                    Label("REQUEST PRO LICENSE")
                    Spacer(Modifier.height(4.dp))
                    Text("Your device code is automatically included in the message.",
                        fontSize = 12.sp, color = TextSec)
                    Spacer(Modifier.height(10.dp))
                    // Device code preview
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(Surface, RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("YOUR DEVICE CODE", fontSize = 9.sp, color = TextMute,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(deviceCode, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = Cyan, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        }
                        CopyButton(copied = copiedKey == "reqdev", onClick = { copyToClipboard(deviceCode, "reqdev") })
                    }
                    Spacer(Modifier.height(12.dp))
                    Label("CHOOSE CONTACT METHOD")
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Request via Email", Blue, icon = Icons.Default.Email,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("license") }
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Request via WhatsApp", Wa, icon = Icons.Default.Chat,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("license") }
                }
            }

            // ── Sign In / Start Trial ─────────────────────────────────
            GateSectionButton("Sign In / Start Trial", showSignIn, Cyan,
                icon = Icons.Default.Person) {
                showSignIn = !showSignIn
                showActivate = false; showRequest = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showSignIn,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Cyan.copy(alpha = 0.25f)) {
                    Label("SH VERTEX ACCOUNT")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = emailField,
                        onValueChange = { emailField = it; fieldError = "" },
                        label = { Text("Email", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp), colors = gateTextFieldColors(Cyan))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = passwordField,
                        onValueChange = { passwordField = it; fieldError = "" },
                        label = { Text("Password", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        colors = gateTextFieldColors(Cyan),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMute, modifier = Modifier.size(18.dp)
                                )
                            }
                        })
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Cyan, uncheckedColor = TextMute,
                                checkmarkColor = Color.Black))
                        Text("Remember email", fontSize = 12.sp, color = TextSec)
                    }
                    ErrorRow(fieldError)
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Sign In", Cyan, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        if (emailField.isBlank() || passwordField.isBlank()) {
                            fieldError = "Email and password are required."; return@FlatButton }
                        isLoading = true; statusMsg = "Signing in\u2026"
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    SHVAccount.signIn(context, emailField, passwordField)
                                    val prefs = context.getSharedPreferences(
                                        "shv_gate_prefs_${SHVAccount.APP_CODE}",
                                        android.content.Context.MODE_PRIVATE)
                                    if (rememberMe) prefs.edit().putString("remembered_email", emailField.trim()).apply()
                                    else prefs.edit().remove("remembered_email").apply()
                                }
                                showSignIn = false; checkAccess()
                            } catch (e: Exception) {
                                isLoading = false; fieldError = e.message ?: "Sign-in failed."; statusMsg = fieldError
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("OR START A FREE TRIAL", fontSize = 10.sp, color = TextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    FlatOutlineButton(
                        if (isSignedIn) "Start Trial Session" else "Sign in above to start trial",
                        if (isSignedIn) Amber else TextMute,
                        icon = Icons.Default.Timer,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        if (!isSignedIn) { fieldError = "Sign in first to start a trial."; return@FlatOutlineButton }
                        isLoading = true; statusMsg = "Starting trial\u2026"
                        scope.launch {
                            try {
                                val demo = withContext(Dispatchers.IO) { SHVAccount.startDemo(context) }
                                isLoading = false
                                if (demo.valid) { showSignIn = false; checkAccess() }
                                else { fieldError = demo.message; statusMsg = demo.message }
                            } catch (e: Exception) {
                                isLoading = false; fieldError = e.message ?: "Could not start trial."; statusMsg = fieldError
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("DON'T HAVE AN ACCOUNT?", fontSize = 10.sp, color = TextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    FlatOutlineButton("Create SH Vertex Account", Purple,
                        icon = Icons.Default.PersonAdd, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        openUrl(ACCOUNT_URL)
                    }
                }
            }

            // ── Pay via Bank Transfer ─────────────────────────────────
            if (true) {
                GateSectionButton("Pay via Bank Transfer", showPayBank, Green.copy(alpha = 0.8f),
                    icon = Icons.Default.AccountBalance) {
                    showPayBank = !showPayBank
                    showActivate = false; showSignIn = false; showRequest = false; showPayCrypto = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayBank,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    GateCard(borderColor = Green.copy(alpha = 0.2f)) {
                        Label("BANK TRANSFER DETAILS")
                        Spacer(Modifier.height(4.dp))
                        Text("Transfer the Pro License amount to the account below, then notify us.",
                            fontSize = 12.sp, color = TextSec)
                        Spacer(Modifier.height(12.dp))
                        if (BANK_NAME.isNotBlank())     BankDetailRow("Bank",       BANK_NAME,    "bankname",  copiedKey, clipboard)
                        if (BANK_ACCNAME.isNotBlank())  BankDetailRow("Account Name", BANK_ACCNAME, "bankaccname", copiedKey, clipboard)
                        if (BANK_ACCOUNT.isNotBlank())  BankDetailRow("Account No.", BANK_ACCOUNT, "bankaccount", copiedKey, clipboard)
                        if (BANK_BRANCH.isNotBlank())   BankDetailRow("Branch",     BANK_BRANCH,  "bankbranch", copiedKey, clipboard)
                        if (BANK_SWIFT.isNotBlank())    BankDetailRow("SWIFT / BIC", BANK_SWIFT,  "bankswift",  copiedKey, clipboard)
                        Spacer(Modifier.height(4.dp))
                        DisclaimerBox()
                        Spacer(Modifier.height(12.dp))
                        Label("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via Email", Blue,
                            icon = Icons.Default.Email, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("payment") }
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via WhatsApp", Wa,
                            icon = Icons.Default.Chat, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("payment") }
                    }
                }
            }

            // ── Pay via Crypto ────────────────────────────────────────
            if (true) {
                GateSectionButton("Pay via Crypto", showPayCrypto, Amber.copy(alpha = 0.8f),
                    icon = Icons.Default.CurrencyBitcoin) {
                    showPayCrypto = !showPayCrypto
                    showActivate = false; showSignIn = false; showRequest = false; showPayBank = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayCrypto,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    GateCard(borderColor = Amber.copy(alpha = 0.2f)) {
                        Label("CRYPTO WALLET ADDRESSES")
                        Spacer(Modifier.height(4.dp))
                        Text("Send the Pro License amount to any address below, then notify us with your transaction hash.",
                            fontSize = 12.sp, color = TextSec)
                        Spacer(Modifier.height(12.dp))
                        if (USDT_BSC.isNotBlank())    CryptoAddressRow("\uD83D\uDFE1 USDT — BSC (BEP20)", USDT_BSC,    "usdt_bsc",    copiedKey) { copyToClipboard(USDT_BSC, "usdt_bsc") }
                        if (USDT_TRC.isNotBlank())    CryptoAddressRow("\uD83D\uDFE1 USDT — TRC20",       USDT_TRC,    "usdt_trc",    copiedKey) { copyToClipboard(USDT_TRC, "usdt_trc") }
                        if (USDT_PLASMA.isNotBlank()) CryptoAddressRow("\uD83D\uDFE1 USDT — Plasma",      USDT_PLASMA, "usdt_plasma", copiedKey) { copyToClipboard(USDT_PLASMA, "usdt_plasma") }
                        if (ETH_ADDR.isNotBlank())    CryptoAddressRow("\uD83D\uDD35 ETH",                ETH_ADDR,    "eth",         copiedKey) { copyToClipboard(ETH_ADDR, "eth") }
                        if (LTC_ADDR.isNotBlank())    CryptoAddressRow("\u26AB LTC",                      LTC_ADDR,    "ltc",         copiedKey) { copyToClipboard(LTC_ADDR, "ltc") }
                        Spacer(Modifier.height(4.dp))
                        DisclaimerBox()
                        Spacer(Modifier.height(12.dp))
                        Label("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via Email", Blue,
                            icon = Icons.Default.Email, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("payment") }
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via WhatsApp", Wa,
                            icon = Icons.Default.Chat, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("payment") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────

@Composable
private fun GateCard(
    borderColor: Color = Color(0xFF262626),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun GateSectionButton(
    label: String,
    active: Boolean,
    accent: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF141414) else Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (active) accent else Color(0xFF262626)))
    ) {
        if (icon != null) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = if (active) accent else Color(0xFF9E9E9E))
    }
}

@Composable
private fun FlatButton(
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp)) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
private fun FlatOutlineButton(
    label: String,
    accent: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(accent.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(10.dp)) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CopyButton(copied: Boolean, onClick: () -> Unit, small: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (copied) Color(0xFF00E676) else Color(0xFF00BCD4)),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                (if (copied) Color(0xFF00E676) else Color(0xFF00BCD4)).copy(alpha = 0.5f))),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = if (small) 10.dp else 14.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (copied) "COPIED!" else "COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(modifier = Modifier
        .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 10.sp, color = Color(0xFF616161),
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun ErrorRow(error: String) {
    if (error.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
            Text(error, fontSize = 12.sp, color = Color(0xFFFF5252))
        }
    }
}

@Composable
private fun BankDetailRow(
    label: String, value: String, key: String, copiedKey: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth()
        .padding(vertical = 4.dp)
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), fontSize = 9.sp, color = Color(0xFF616161),
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = Color(0xFFFFFFFF), fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(value)) },
            modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, null,
                tint = if (copiedKey == key) Color(0xFF00E676) else Color(0xFF00BCD4),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CryptoAddressRow(
    label: String, address: String, key: String, copiedKey: String, onCopy: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()
        .padding(vertical = 4.dp)
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
            CopyButton(copied = copiedKey == key, onClick = onCopy, small = true)
        }
        Spacer(Modifier.height(4.dp))
        Text(address, fontSize = 11.sp, color = Color(0xFF9E9E9E),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DisclaimerBox() {
    if (DISCLAIMER.isBlank()) return
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.fillMaxWidth()
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = Color(0xFFFFB300),
                modifier = Modifier.size(14.dp).padding(top = 1.dp))
            Text(DISCLAIMER, fontSize = 11.sp, color = Color(0xFF9E9E9E), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun gateTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF262626),
    focusedTextColor = Color(0xFFFFFFFF), unfocusedTextColor = Color(0xFFFFFFFF),
    cursorColor = accent, focusedContainerColor = Color(0xFF0D0D0D),
    unfocusedContainerColor = Color(0xFF0D0D0D), focusedLabelColor = accent,
    unfocusedLabelColor = Color(0xFF616161))