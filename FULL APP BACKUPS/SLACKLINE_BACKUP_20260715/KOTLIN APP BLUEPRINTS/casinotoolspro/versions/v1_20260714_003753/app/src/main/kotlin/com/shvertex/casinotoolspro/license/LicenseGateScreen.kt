package com.shvertex.casinotoolspro.license

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.provider.Settings
import java.util.Base64
import java.util.UUID
import java.util.zip.Inflater

// ── CTP colour tokens (mirrors app theme) ────────────────────────────────────
private val CtpBg       = Color(0xFF0A0A0A)
private val CtpSurface  = Color(0xFF111111)
private val CtpCard     = Color(0xFF1A1A1A)
private val CtpBorder   = Color(0xFF2A2A2A)
private val CtpGreen    = Color(0xFF00FF88)
private val CtpGold     = Color(0xFFFFCC00)
private val CtpRed      = Color(0xFFFF3355)
private val CtpBlue     = Color(0xFF38BDF8)
private val CtpPurple   = Color(0xFFCE93D8)
private val CtpTextPri  = Color(0xFFFFFFFF)
private val CtpTextSec  = Color(0xFF9E9E9E)
private val CtpTextMute = Color(0xFF555555)

// ── Supabase constants ────────────────────────────────────────────────────────
private const val SUPABASE_URL    = "https://ovdxetyadfsxehwnbyuz.supabase.co"
private const val PUBLISHABLE_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"
private const val PRODUCT_ID      = "2261a4c9-b6fc-41b2-a9b0-a4c0ac759fc3"
private const val APP_CODE        = "vantage_byshv"
private const val DEMO_HOURS      = 24
private const val LICENSE_FILE    = "shv_license_vantage_shv.json"
private const val SESSION_FILE    = "shv_cloud_session_vantage_shv.json"

private val PUBLIC_KEY_PEM = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAov+qG7wCe0y4txX4L39V
giz1y3w8webv2j5GEM14J0pRPxUSZqKUmcnsCLm/G73WFfCqAWw9bZO54LIb5CoT
l36n9wrBP08/+aBgjHH6CXH591Qpsi+zqyVEH8xg4JhJ7aBlaMa6HOOhbIn7yrUh
bzeIwbwxBIDq7KRAudnF865b6P6C0vuubewPA7VpPmmnN8If/Nq3N1z72s9Eo1+X
2sZFZaZnsNCN2i55S2hERsyAVXfT8WwEhQMnr15a4zQErLeNPem8iFZT0eSfYVEQ
BpI8qtyePS0LEriDmx++wWxah7JphFSqFmKVnedoN6PauNq0nZTS8CF+KrfEmiKw
jwIDAQAB
-----END RSA PUBLIC KEY-----
""".trimIndent()

// ── Data classes ──────────────────────────────────────────────────────────────

internal data class LicenseResult(
    val valid: Boolean,
    val tier: String = "",
    val message: String = "",
    val licenseId: String = "",
    val revoked: Boolean = false
)

internal data class Session(
    val accessToken: String,
    val email: String,
    val plan: String
)

private data class DemoState(
    val valid: Boolean,
    val signedIn: Boolean,
    val startAllowed: Boolean,
    val remainingText: String,
    val message: String,
    val status: String
)

// ── Main Gate Screen ──────────────────────────────────────────────────────────

@Composable
fun LicenseGateScreen(
    context: Context,
    onAccessGranted: (tier: String) -> Unit
) {
    val scope     = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val deviceCode = remember { getDeviceCode(context) }

    var isLoading      by remember { mutableStateOf(true) }
    var statusMsg      by remember { mutableStateOf("Verifying access…") }
    var accessMode     by remember { mutableStateOf("none") }
    var currentTier    by remember { mutableStateOf("") }
    var trialRemaining by remember { mutableStateOf("") }
    var isSignedIn     by remember { mutableStateOf(false) }
    var accountEmail   by remember { mutableStateOf("") }
    var licenseId      by remember { mutableStateOf("") }
    var licenseTier    by remember { mutableStateOf("") }

    var showActivate   by remember { mutableStateOf(false) }
    var showSignIn     by remember { mutableStateOf(false) }
    var showRequest    by remember { mutableStateOf(false) }
    var showPayBank    by remember { mutableStateOf(false) }
    var showPayCrypto  by remember { mutableStateOf(false) }
    var activationCode by remember { mutableStateOf("") }
    var emailField     by remember { mutableStateOf("") }
    var passwordField  by remember { mutableStateOf("") }
    var fieldError       by remember { mutableStateOf("") }
    var codeCopied       by remember { mutableStateOf(false) }
    var passwordVisible  by remember { mutableStateOf(false) }
    var rememberMe       by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun requestViaEmail() {
        val subject = Uri.encode("License Request — Vantage by SHV")
        val body = Uri.encode(
            "Hello,\n\nI would like to request a license for Vantage by SHV (Casino Tools Pro).\n" +
            "Device Code: $deviceCode\n\nPlease send payment instructions.\n\nThank you."
        )
        openUrl("mailto:${CTPConfig.CONTACT_EMAIL}?subject=$subject&body=$body")
    }

    fun requestViaWhatsApp() {
        val msg = Uri.encode(
            "Hello! I want a license for *Vantage by SHV* (Casino Tools Pro).\n" +
            "📱 Device Code: *$deviceCode*\n\nPlease send payment instructions. Thank you!"
        )
        val number = CTPConfig.CONTACT_WA.replace("+", "").replace(" ", "")
        try { openUrl("whatsapp://send?phone=$number&text=$msg") }
        catch (e: Exception) { openUrl("https://wa.me/$number?text=$msg") }
    }

    fun checkAccess() {
        scope.launch {
            isLoading = true; fieldError = ""
            val session = loadSession(context)
            isSignedIn   = session != null
            accountEmail = session?.email ?: ""

            if (!isSignedIn) {
                isLoading = false
                accessMode = "none"
                statusMsg = "Sign in to access Vantage by SHV."
                return@launch
            }

            // Check product still exists
            val productActive = withContext(Dispatchers.IO) { isProductActive() }
            if (!productActive) {
                isLoading = false
                accessMode = "none"
                statusMsg = "Product unavailable. Contact support."
                return@launch
            }

            // Check saved license
            val lic = loadLicense(context)
            if (lic != null) {
                val result = withContext(Dispatchers.IO) {
                    checkLicense(lic.optString("activation_code"), context, checkRevocation = true)
                }
                if (result.valid) {
                    isLoading   = false
                    accessMode  = "licensed"
                    currentTier = result.tier
                    licenseId   = result.licenseId
                    licenseTier = result.tier.uppercase()
                    statusMsg   = "License active."
                    return@launch
                } else {
                    deleteLicense(context)
                }
            }

            // Check demo
            val demo = withContext(Dispatchers.IO) { getDemoStatus(context) }
            isLoading = false
            if (demo.valid) {
                accessMode     = "trial"
                currentTier    = "demo"
                trialRemaining = demo.remainingText
                statusMsg      = demo.message
            } else if (demo.signedIn) {
                // Signed in but no license and no active trial — free tier
                accessMode  = "free"
                currentTier = "free"
                statusMsg   = "Free tier active. Limited modules available."
            } else {
                accessMode = "none"
                statusMsg  = "Sign in to continue."
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("ctp_gate_prefs", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("remembered_email", "")
        if (!saved.isNullOrBlank()) { emailField = saved; rememberMe = true }
        checkAccess()
    }
    LaunchedEffect(codeCopied) { if (codeCopied) { delay(2000); codeCopied = false } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF0D1A0D), Color(0xFF0A0A0A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Continue Button — shown at TOP when access is granted ──────────
            AnimatedVisibility(visible = accessMode in listOf("licensed","trial","free") && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Button(
                    onClick = { onAccessGranted(currentTier) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (accessMode) {
                            "licensed" -> CtpGreen; "trial" -> CtpGold; else -> CtpBlue }),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null,
                        tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when (accessMode) {
                            "licensed" -> "Enter — ${licenseTier} Active"
                            "trial"    -> "Enter — Trial Active"
                            "free"     -> "Enter — Free Tier"
                            else       -> "Enter"
                        },
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black
                    )
                }
            }

            // ── App Header ────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1A2A1A), Color(0xFF0D1F0D)))
                    )
                    .border(1.dp, CtpGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♠", fontSize = 36.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Vantage by SHV",
                fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = CtpGreen, textAlign = TextAlign.Center
            )
            Text(
                "Casino Tools Pro  ·  Strategy Suite Pro",
                fontSize = 12.sp, color = CtpTextSec,
                textAlign = TextAlign.Center, letterSpacing = 0.5.sp
            )
            Text(
                "by SH Vertex Technologies",
                fontSize = 10.sp, color = CtpTextMute,
                textAlign = TextAlign.Center, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            // ── Device Code Card ──────────────────────────────────────────────
            CtpCard {
                CtpLabel("DEVICE CODE")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        deviceCode, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = CtpTextPri, fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(deviceCode)); codeCopied = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (codeCopied) CtpGreen else CtpBlue),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                (if (codeCopied) CtpGreen else CtpBlue).copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (codeCopied) "COPIED!" else "COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CtpBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp),
                            color = CtpGreen, strokeWidth = 2.dp)
                        Text("Checking access…", fontSize = 12.sp, color = CtpTextSec)
                    }
                } else {
                    val dotColor = when (accessMode) {
                        "licensed" -> CtpGreen; "trial" -> CtpGold; "free" -> CtpBlue; else -> CtpRed
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
                        Text(statusMsg, fontSize = 12.sp, color = dotColor)
                    }
                }
            }

            // ── Tier Cards ────────────────────────────────────────────────────

            // Licensed card
            AnimatedVisibility(visible = accessMode == "licensed" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpGreen.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(CtpGreen.copy(alpha = 0.1f))
                            .border(1.dp, CtpGreen.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = CtpGreen, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            CtpLabel("LICENSE ACTIVE")
                            Spacer(Modifier.height(2.dp))
                            Text("Tier: $licenseTier  ·  Lifetime  ·  Device-bound",
                                fontSize = 14.sp, color = CtpGreen, fontWeight = FontWeight.Bold)
                            if (licenseId.isNotBlank())
                                Text("ID: $licenseId", fontSize = 10.sp, color = CtpTextMute,
                                    fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Trial card
            AnimatedVisibility(visible = accessMode == "trial" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpGold.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(CtpGold.copy(alpha = 0.1f))
                            .border(1.dp, CtpGold.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Timer, null, tint = CtpGold, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            CtpLabel("TRIAL SESSION — FULL PRO+ ACCESS")
                            Spacer(Modifier.height(2.dp))
                            Text(if (trialRemaining.isNotBlank()) "$trialRemaining remaining" else "Trial active",
                                fontSize = 14.sp, color = CtpGold, fontWeight = FontWeight.Bold)
                            Text("All modules unlocked for trial period.",
                                fontSize = 11.sp, color = CtpTextSec)
                        }
                    }
                }
            }

            // Free card
            AnimatedVisibility(visible = accessMode == "free" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpBlue.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(CtpBlue.copy(alpha = 0.1f))
                            .border(1.dp, CtpBlue.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = CtpBlue, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            CtpLabel("FREE TIER — LIMITED ACCESS")
                            Spacer(Modifier.height(2.dp))
                            Text("5 modules available", fontSize = 14.sp, color = CtpBlue, fontWeight = FontWeight.Bold)
                            Text("Upgrade to Pro for full access.", fontSize = 11.sp, color = CtpTextSec)
                        }
                    }
                }
            }

            // Account card
            AnimatedVisibility(visible = isSignedIn, enter = fadeIn(), exit = fadeOut()) {
                CtpCard(borderColor = CtpGreen.copy(alpha = 0.2f)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Person, null, tint = CtpGreen, modifier = Modifier.size(16.dp))
                        Text(accountEmail, fontSize = 13.sp, color = CtpTextPri,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier
                            .background(CtpGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, CtpGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("SIGNED IN", fontSize = 9.sp, color = CtpGreen,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // ── Tier comparison table ─────────────────────────────────────────
            CtpCard {
                CtpLabel("ACCESS TIERS")
                Spacer(Modifier.height(12.dp))
                TierRow("FREE", "Always", CtpBlue,
                    "Dice/Limbo Calc, Mines Analytics,\nCompound Growth, Pattern Master, Crypto Converter")
                Spacer(Modifier.height(8.dp))
                TierRow("TRIAL", "Demo period", CtpGold,
                    "Full Pro+ access for ${DEMO_HOURS}h\nAll modules + Evolution Lab + Presentation Mode")
                Spacer(Modifier.height(8.dp))
                TierRow("PRO", CTPConfig.PRICE_PRO, CtpGreen,
                    "All Core Tools, Game Analytics,\nSports, Utilities — Lifetime")
                Spacer(Modifier.height(8.dp))
                TierRow("PRO+", CTPConfig.PRICE_PRO_PLUS, CtpGold,
                    "Everything in Pro + Evolution Lab\n(6 modules) + Presentation Mode — Lifetime")
            }

            // ── Action row when signed in ─────────────────────────────────────
            AnimatedVisibility(visible = isSignedIn && !isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            clearSession(context); isSignedIn = false; accountEmail = ""
                            accessMode = "none"; currentTier = ""; statusMsg = "Signed out."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CtpRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CtpRed.copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Sign Out", fontSize = 13.sp) }

                    OutlinedButton(
                        onClick = { checkAccess() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CtpPurple),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CtpPurple.copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Refresh", fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = CtpBorder.copy(alpha = 0.5f), thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp))

            // ── Activate License ──────────────────────────────────────────────
            CtpSectionButton(
                label = if (showActivate) "Hide Activation" else "Activate License",
                icon  = Icons.Default.VpnKey,
                color = CtpGreen, active = showActivate
            ) {
                showActivate = !showActivate
                showSignIn = false; showRequest = false; fieldError = ""
            }
            AnimatedVisibility(visible = showActivate,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpGreen.copy(alpha = 0.25f)) {
                    CtpLabel("PASTE ACTIVATION CODE")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = { activationCode = it; fieldError = "" },
                        placeholder = { Text("Paste your activation code here",
                            color = CtpTextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false, minLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        colors = ctpTextFieldColors(CtpGreen)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { clipboard.getText()?.text?.let {
                                if (it.isNotBlank()) activationCode = it } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CtpBlue),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CtpBlue.copy(alpha = 0.4f))),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Paste", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { activationCode = "" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CtpRed),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CtpRed.copy(alpha = 0.4f))),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Clear", fontSize = 13.sp) }
                    }
                    if (fieldError.isNotBlank()) CtpErrorRow(fieldError)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val code = activationCode.trim()
                            if (code.isBlank()) { fieldError = "Paste your activation code first."; return@Button }
                            isLoading = true; statusMsg = "Verifying…"
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { checkLicense(code, context) }
                                isLoading = false
                                if (result.valid) {
                                    withContext(Dispatchers.IO) {
                                        val (payload, _) = decodeToken(code)
                                        saveLicense(context, code, payload)
                                    }
                                    showActivate = false
                                    checkAccess()
                                } else { fieldError = result.message; statusMsg = result.message }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CtpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Activate License", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = Color.Black)
                    }
                }
            }

            // ── Request License ───────────────────────────────────────────────
            CtpSectionButton(
                label = if (showRequest) "Hide Request" else "Request License",
                icon  = Icons.Default.Send,
                color = CtpGold, active = showRequest
            ) {
                showRequest = !showRequest
                showActivate = false; showSignIn = false; fieldError = ""
            }
            AnimatedVisibility(visible = showRequest,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpGold.copy(alpha = 0.3f)) {
                    CtpLabel("REQUEST A LICENSE")
                    Spacer(Modifier.height(4.dp))
                    Text("Your device code is automatically included in the message.",
                        fontSize = 12.sp, color = CtpTextSec)
                    Spacer(Modifier.height(10.dp))
                    // Device code preview
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(CtpSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, CtpBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            CtpLabel("YOUR DEVICE CODE")
                            Text(deviceCode, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = CtpGreen, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(deviceCode)); codeCopied = true }) {
                            Icon(Icons.Default.ContentCopy, null, tint = CtpGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    CtpLabel("CHOOSE CONTACT METHOD")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { requestViaEmail() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Request via Email", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { requestViaWhatsApp() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("💬", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Request via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            // ── Sign In / Trial ───────────────────────────────────────────────
            CtpSectionButton(
                label = if (showSignIn) "Hide Sign In" else "Sign In / Start Trial",
                icon  = Icons.Default.Person,
                color = CtpBlue, active = showSignIn
            ) {
                showSignIn = !showSignIn
                showActivate = false; showRequest = false; fieldError = ""
            }
            AnimatedVisibility(visible = showSignIn,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CtpCard(borderColor = CtpBlue.copy(alpha = 0.25f)) {
                    CtpLabel("SH VERTEX ACCOUNT")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailField, onValueChange = { emailField = it; fieldError = "" },
                        label = { Text("Email", color = CtpTextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp), colors = ctpTextFieldColors(CtpBlue)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordField, onValueChange = { passwordField = it; fieldError = "" },
                        label = { Text("Password", color = CtpTextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ctpTextFieldColors(CtpBlue),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CtpTextMute, modifier = Modifier.size(18.dp)
                                )
                            }
                        })
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CtpBlue, uncheckedColor = CtpTextMute,
                                checkmarkColor = Color.Black))
                        Text("Remember email", fontSize = 12.sp, color = CtpTextSec)
                    }
                    if (fieldError.isNotBlank()) CtpErrorRow(fieldError)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (emailField.isBlank() || passwordField.isBlank()) {
                                fieldError = "Email and password are required."; return@Button }
                            isLoading = true; statusMsg = "Signing in…"
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        signIn(context, emailField, passwordField)
                                        val prefs = context.getSharedPreferences(
                                            "ctp_gate_prefs", android.content.Context.MODE_PRIVATE)
                                        if (rememberMe) prefs.edit().putString("remembered_email", emailField.trim()).apply()
                                        else prefs.edit().remove("remembered_email").apply()
                                    }
                                    showSignIn = false; checkAccess()
                                } catch (e: Exception) {
                                    isLoading = false
                                    fieldError = e.message ?: "Sign-in failed."
                                    statusMsg  = fieldError
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CtpBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black) }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = CtpBorder)
                    Spacer(Modifier.height(8.dp))

                    Text("OR START A FREE TRIAL", fontSize = 10.sp, color = CtpTextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!isSignedIn) { fieldError = "Sign in first to start a trial."; return@OutlinedButton }
                            isLoading = true; statusMsg = "Starting trial…"
                            scope.launch {
                                try {
                                    val demo = withContext(Dispatchers.IO) { startDemo(context) }
                                    isLoading = false
                                    if (demo.valid) { showSignIn = false; checkAccess() }
                                    else { fieldError = demo.message; statusMsg = demo.message }
                                } catch (e: Exception) {
                                    isLoading = false
                                    fieldError = e.message ?: "Could not start trial."
                                    statusMsg  = fieldError
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSignedIn) CtpGold else CtpTextMute),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSignedIn) CtpGold.copy(alpha = 0.5f) else CtpBorder)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isSignedIn) "Start ${DEMO_HOURS}h Trial — Full Pro+ Access"
                            else "Sign in above to start trial",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = CtpBorder)
                    Spacer(Modifier.height(8.dp))
                    Text("DON'T HAVE AN ACCOUNT?", fontSize = 10.sp, color = CtpTextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openUrl(CTPConfig.ACCOUNT_URL) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CtpPurple),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CtpPurple.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create SH Vertex Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ── Pay via Bank Transfer ─────────────────────────────────────────
            if (CTPConfig.hasBankDetails) {
                CtpSectionButton("Pay via Bank Transfer", Icons.Default.AccountBalance,
                    CtpGreen.copy(alpha = 0.8f), showPayBank) {
                    showPayBank = !showPayBank
                    showActivate = false; showSignIn = false; showRequest = false
                    showPayCrypto = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayBank,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    CtpCard(borderColor = CtpGreen.copy(alpha = 0.2f)) {
                        CtpLabel("BANK TRANSFER DETAILS")
                        Spacer(Modifier.height(4.dp))
                        Text("Transfer the Pro License amount below, then notify us.",
                            fontSize = 12.sp, color = CtpTextSec)
                        Spacer(Modifier.height(10.dp))
                        if (CTPConfig.BANK_NAME.isNotBlank())    CtpDetailRow("Bank",         CTPConfig.BANK_NAME)
                        if (CTPConfig.BANK_ACCNAME.isNotBlank()) CtpDetailRow("Account Name", CTPConfig.BANK_ACCNAME)
                        if (CTPConfig.BANK_ACCOUNT.isNotBlank()) CtpDetailRow("Account No.",  CTPConfig.BANK_ACCOUNT)
                        if (CTPConfig.BANK_BRANCH.isNotBlank())  CtpDetailRow("Branch",       CTPConfig.BANK_BRANCH)
                        if (CTPConfig.BANK_SWIFT.isNotBlank())   CtpDetailRow("SWIFT / BIC",  CTPConfig.BANK_SWIFT)
                        if (CTPConfig.PAYMENT_DISCLAIMER.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()
                                .background(CtpSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, CtpGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, null, tint = CtpGold,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                Text(CTPConfig.PAYMENT_DISCLAIMER, fontSize = 11.sp,
                                    color = CtpTextSec, lineHeight = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        CtpLabel("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestViaEmail() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("I've Paid — Notify via Email", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestViaWhatsApp() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("💬", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("I've Paid — Notify via WhatsApp", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }

            // ── Pay via Crypto ────────────────────────────────────────────────
            if (CTPConfig.hasCrypto) {
                CtpSectionButton("Pay via Crypto", Icons.Default.CurrencyBitcoin,
                    CtpGold.copy(alpha = 0.8f), showPayCrypto) {
                    showPayCrypto = !showPayCrypto
                    showActivate = false; showSignIn = false; showRequest = false
                    showPayBank = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayCrypto,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    CtpCard(borderColor = CtpGold.copy(alpha = 0.2f)) {
                        CtpLabel("CRYPTO WALLET ADDRESSES")
                        Spacer(Modifier.height(4.dp))
                        Text("Send to any address below, then notify us with your transaction hash.",
                            fontSize = 12.sp, color = CtpTextSec)
                        Spacer(Modifier.height(10.dp))
                        if (CTPConfig.USDT_BSC.isNotBlank())    CtpCryptoRow("🟡 USDT — BSC (BEP20)", CTPConfig.USDT_BSC, clipboard)
                        if (CTPConfig.USDT_TRC.isNotBlank())    CtpCryptoRow("🟡 USDT — TRC20",       CTPConfig.USDT_TRC, clipboard)
                        if (CTPConfig.USDT_PLASMA.isNotBlank()) CtpCryptoRow("🟡 USDT — Plasma",      CTPConfig.USDT_PLASMA, clipboard)
                        if (CTPConfig.ETH_ADDR.isNotBlank())    CtpCryptoRow("🔵 ETH",                CTPConfig.ETH_ADDR, clipboard)
                        if (CTPConfig.LTC_ADDR.isNotBlank())    CtpCryptoRow("⚫ LTC",                CTPConfig.LTC_ADDR, clipboard)
                        if (CTPConfig.PAYMENT_DISCLAIMER.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()
                                .background(CtpSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, CtpGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, null, tint = CtpGold,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                Text(CTPConfig.PAYMENT_DISCLAIMER, fontSize = 11.sp,
                                    color = CtpTextSec, lineHeight = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        CtpLabel("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestViaEmail() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("I've Paid — Notify via Email", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestViaWhatsApp() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("💬", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("I've Paid — Notify via WhatsApp", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Detail + Crypto row composables ──────────────────────────────────────────

@Composable
private fun CtpDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
        .background(CtpSurface, RoundedCornerShape(8.dp))
        .border(1.dp, CtpBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), fontSize = 9.sp, color = CtpTextMute,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, fontSize = 13.sp, color = CtpTextPri, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CtpCryptoRow(
    label: String, address: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    var copied by remember { mutableStateOf(false) }
    val scope  = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
        .background(CtpSurface, RoundedCornerShape(8.dp))
        .border(1.dp, CtpBorder, RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = CtpGold, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(address))
                    copied = true
                    scope.launch { delay(2000); copied = false }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (copied) CtpGreen else CtpBlue),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        (if (copied) CtpGreen else CtpBlue).copy(alpha = 0.5f))),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (copied) "COPIED!" else "COPY",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(address, fontSize = 11.sp, color = CtpTextSec,
            fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TierRow(tier: String, price: String, color: Color, features: String) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(CtpSurface, RoundedCornerShape(10.dp))
        .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp)) {
            Box(modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)) {
                Text(tier, fontSize = 10.sp, color = color,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(price, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Text(features, fontSize = 12.sp, color = CtpTextSec, lineHeight = 16.sp,
            modifier = Modifier.weight(1f))
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

@Composable
private fun CtpCard(
    borderColor: Color = Color(0xFF2A2A2A),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun CtpLabel(text: String) {
    Text(text, fontSize = 10.sp, color = CtpTextMute,
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun CtpErrorRow(error: String) {
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Warning, null, tint = CtpRed, modifier = Modifier.size(14.dp))
        Text(error, fontSize = 12.sp, color = CtpRed)
    }
}

@Composable
private fun CtpSectionButton(
    label: String, icon: ImageVector, color: Color, active: Boolean, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF1A1A1A) else Color(0xFF161616)),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (active) color else Color(0xFF2A2A2A)))
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = if (active) color else CtpTextSec)
    }
}

@Composable
private fun ctpTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent, unfocusedBorderColor = CtpBorder,
    focusedTextColor = CtpTextPri, unfocusedTextColor = CtpTextPri,
    cursorColor = accent, focusedContainerColor = CtpSurface,
    unfocusedContainerColor = CtpSurface, focusedLabelColor = accent,
    unfocusedLabelColor = CtpTextMute
)

// ── License Info Dialog (called from HomeScreen) ──────────────────────────────

@Composable
fun LicenseInfoDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val deviceCode = remember { getDeviceCode(context) }
    val tier       = CTPAccess.tier
    val lic        = remember { loadLicense(context) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, when (tier) {
                    "pro+"  -> CtpGold.copy(alpha = 0.4f)
                    "pro"   -> CtpGreen.copy(alpha = 0.4f)
                    "demo"  -> CtpGold.copy(alpha = 0.3f)
                    "free"  -> CtpBlue.copy(alpha = 0.3f)
                    else    -> Color(0xFF2A2A2A)
                }, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Shield, null,
                    tint = when (tier) { "pro+", "demo" -> CtpGold; "pro" -> CtpGreen; else -> CtpBlue },
                    modifier = Modifier.size(24.dp))
                Text("License Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CtpTextPri)
            }

            HorizontalDivider(color = Color(0xFF2A2A2A))

            LicInfoRow("App", "Vantage by SHV")
            LicInfoRow("Device Code", deviceCode, mono = true)
            LicInfoRow("Current Tier", CTPAccess.tierLabel(),
                valueColor = when (tier) { "pro+", "demo" -> CtpGold; "pro" -> CtpGreen; "free" -> CtpBlue; else -> CtpRed })

            if (tier in listOf("pro", "pro+")) {
                lic?.let {
                    if (it.optString("license_id").isNotBlank())
                        LicInfoRow("License ID", it.optString("license_id"), mono = true)
                    if (it.optString("saved_at").isNotBlank())
                        LicInfoRow("Activated", it.optString("saved_at").take(10))
                }
            }

            if (tier == "demo") {
                LicInfoRow("Trial Access", "Full Pro+ — All modules", valueColor = CtpGold)
            }

            if (tier == "free") {
                LicInfoRow("Free Modules", "5 of ${getTotalModuleCount()}", valueColor = CtpBlue)
            }

            HorizontalDivider(color = Color(0xFF2A2A2A))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtpGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Close", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black) }
        }
    }
}

@Composable
private fun LicInfoRow(label: String, value: String, mono: Boolean = false, valueColor: Color = Color(0xFFFFFFFF)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = Color(0xFF555555), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = valueColor,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium)
    }
}

private fun getTotalModuleCount() = 25

// ── Access Denied Dialog ──────────────────────────────────────────────────────

@Composable
fun AccessDeniedDialog(route: String, onDismiss: () -> Unit) {
    // Presentation Mode key is not a nav route — treat it as pro+ requirement
    val required = if (route == CTPAccess.PRESENTATION_MODE_KEY) "pro+" else CTPAccess.tierRequired(route)
    val color    = if (required == "pro+") CtpGold else CtpGreen

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Text(
                if (required == "pro+") "Pro+ Required" else "Pro Required",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = CtpTextPri, textAlign = TextAlign.Center
            )
            Text(
                if (route == CTPAccess.PRESENTATION_MODE_KEY) CTPAccess.presentationLockedMessage()
                else CTPAccess.lockedMessage(route),
                fontSize = 13.sp, color = CtpTextSec,
                textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Got it", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black) }
        }
    }
}

// ── License crypto & network logic ────────────────────────────────────────────

internal fun getDeviceCode(context: Context): String {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "fallback"
    return MessageDigest.getInstance("SHA-256")
        .digest(androidId.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(8).uppercase()
}

private fun decodeToken(code: String): Pair<JSONObject, String> {
    val prefix = "CTPSSP2026-"
    var cleaned = code.trim().replace("\n", "").replace(" ", "")
    if (cleaned.startsWith(prefix)) cleaned = cleaned.removePrefix(prefix)
    cleaned = cleaned.replace(".", "")
    val padded     = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
    val compressed = Base64.getUrlDecoder().decode(padded)
    val inflater   = Inflater(); inflater.setInput(compressed)
    val output = ByteArray(65536); val len = inflater.inflate(output); inflater.end()
    val json = JSONObject(String(output, 0, len, Charsets.UTF_8))
    return Pair(json.getJSONObject("p"), json.getString("s"))
}

private fun verify(payload: JSONObject, sigB64: String): Boolean {
    val canonical   = buildCanonicalJson(payload)
    val pemStripped = PUBLIC_KEY_PEM
        .replace(Regex("-----.*?-----"), "")
        .replace(Regex("\\s+"), "")
    val keyBytes = Base64.getDecoder().decode(pemStripped)
    val pubKey = try {
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
    } catch (e: Exception) {
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(wrapPkcs1InX509(keyBytes)))
    }
    val sig = Signature.getInstance("SHA256withRSA")
    sig.initVerify(pubKey)
    sig.update(canonical.toByteArray(Charsets.UTF_8))
    val sigBytes = try { Base64.getUrlDecoder().decode(sigB64) }
                   catch (e: Exception) { Base64.getDecoder().decode(sigB64) }
    return sig.verify(sigBytes)
}

private fun wrapPkcs1InX509(pkcs1: ByteArray): ByteArray {
    val oid = byteArrayOf(0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,
        0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00)
    return derEncode(0x30, oid + derEncode(0x03, byteArrayOf(0x00) + pkcs1))
}

private fun derEncode(tag: Int, content: ByteArray): ByteArray {
    val len = content.size
    val lb  = when { len < 128 -> byteArrayOf(len.toByte()); len < 256 -> byteArrayOf(0x81.toByte(), len.toByte()); else -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xff).toByte()) }
    return byteArrayOf(tag.toByte()) + lb + content
}

private fun buildCanonicalJson(obj: JSONObject): String {
    val parts = obj.keys().asSequence().sorted()
        .map { k -> "\"$k\":${canonicalValue(obj.get(k))}" }.toList()
    return "{${parts.joinToString(",")}}"
}

private fun canonicalValue(value: Any?): String = when (value) {
    is JSONObject -> buildCanonicalJson(value)
    is String     -> "\"$value\""
    is Boolean    -> if (value) "true" else "false"
    null, JSONObject.NULL -> "null"
    else          -> value.toString()
}

internal fun checkLicense(code: String, context: Context, checkRevocation: Boolean = false): LicenseResult {
    if (code.isBlank()) return LicenseResult(false, message = "No activation code.")
    val deviceCode = getDeviceCode(context)
    return try {
        val (payload, sigB64) = decodeToken(code)
        if (!verify(payload, sigB64)) return LicenseResult(false, message = "Signature invalid.")
        if (payload.optString("app").lowercase() != APP_CODE.lowercase())
            return LicenseResult(false, message = "Wrong product.")
        val bound = payload.optString("device_code").trim().uppercase()
        if (bound.isNotEmpty() && bound != deviceCode.uppercase())
            return LicenseResult(false, message = "Device mismatch. Your code: $deviceCode")
        val expiry = payload.optString("expires_at").ifBlank { payload.optString("expiry") }
        if (expiry.isNotBlank()) {
            try {
                val exp = java.time.Instant.parse(expiry.replace(" ", "T").let { if (!it.endsWith("Z")) "${it}Z" else it })
                if (java.time.Instant.now().isAfter(exp)) return LicenseResult(false, message = "License expired.")
            } catch (e: Exception) { }
        }
        val licId = payload.optString("license_id")
        if (checkRevocation && licId.isNotBlank()) {
            if (isRevokedOnServer(licId)) return LicenseResult(false, message = "License revoked.", revoked = true)
        }
        LicenseResult(true, payload.optString("tier", "pro").lowercase(), "License verified.", licId)
    } catch (e: Exception) {
        LicenseResult(false, message = "Decode error: ${e.message}")
    }
}

private fun isProductActive(): Boolean {
    return try {
        val url = "$SUPABASE_URL/rest/v1/kl_products?product_id=eq.${PRODUCT_ID}&select=product_id&limit=1"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
        conn.connectTimeout = 8_000; conn.readTimeout = 8_000
        val code = conn.responseCode
        val resp = if (code in 200..299) conn.inputStream.bufferedReader().readText() else "[]"
        conn.disconnect()
        JSONArray(resp).length() > 0
    } catch (e: Exception) { true }
}

private fun isRevokedOnServer(licenseId: String): Boolean {
    return try {
        val url = "$SUPABASE_URL/rest/v1/kl_licenses?license_id=eq.${licenseId}&select=status&limit=1"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
        conn.connectTimeout = 8_000; conn.readTimeout = 8_000
        val resp = conn.inputStream.bufferedReader().readText()
        val arr  = JSONArray(resp)
        if (arr.length() > 0 && arr.getJSONObject(0).optString("status") == "revoked") return true
        val revUrl = "$SUPABASE_URL/rest/v1/kl_revocations?payload->>license_id=eq.${licenseId}&select=id&limit=1"
        val revConn = URL(revUrl).openConnection() as HttpURLConnection
        revConn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        revConn.setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
        revConn.connectTimeout = 8_000; revConn.readTimeout = 8_000
        JSONArray(revConn.inputStream.bufferedReader().readText()).length() > 0
    } catch (e: Exception) { false }
}

internal fun saveLicense(context: Context, code: String, payload: JSONObject) {
    File(context.filesDir, LICENSE_FILE).writeText(JSONObject().apply {
        put("activation_code", code)
        put("license_id", payload.optString("license_id"))
        put("product_id", APP_CODE)
        put("tier", payload.optString("tier", "pro"))
        put("payload", payload)
        put("saved_at", java.time.Instant.now().toString())
    }.toString())
}

internal fun loadLicense(context: Context): JSONObject? = try {
    val f = File(context.filesDir, LICENSE_FILE)
    if (f.exists()) JSONObject(f.readText()) else null
} catch (e: Exception) { null }

private fun deleteLicense(context: Context) { File(context.filesDir, LICENSE_FILE).delete() }

private fun sessionFile(context: Context) = File(context.filesDir, SESSION_FILE)

internal fun loadSession(context: Context): Session? = try {
    val j = JSONObject(sessionFile(context).readText())
    val t = j.optString("access_token")
    if (t.isBlank()) null
    else Session(t,
        j.optJSONObject("user")?.optString("email") ?: "",
        j.optJSONObject("user")?.optJSONObject("user_metadata")?.optString("plan") ?: "Standard")
} catch (e: Exception) { null }

private fun clearSession(context: Context) { sessionFile(context).delete() }

private fun signIn(context: Context, email: String, password: String) {
    val conn = URL("$SUPABASE_URL/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
    conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
    conn.outputStream.use { it.write(JSONObject().put("email", email.trim()).put("password", password).toString().toByteArray()) }
    val code = conn.responseCode
    val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: "{}"
    if (code !in 200..299) throw RuntimeException(try { JSONObject(resp).optString("message", resp) } catch (e: Exception) { resp })
    sessionFile(context).writeText(resp)
}

private fun getDemoStatus(context: Context): DemoState {
    val session = loadSession(context) ?: return DemoState(false, false, false, "0m", "Sign in first.", "none")
    return try {
        val deviceCode = getDeviceCode(context)
        val url = "$SUPABASE_URL/rest/v1/kl_demo_sessions?product_id=eq.${PRODUCT_ID}&device_code=eq.${deviceCode}&select=id,is_active,demo_started_at,demo_expires_at&order=demo_started_at.desc&limit=1"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        conn.connectTimeout = 14_000; conn.readTimeout = 14_000
        val arr = JSONArray(conn.inputStream.bufferedReader().readText())
        if (arr.length() == 0) return DemoState(false, true, true, "0m", "No trial started yet.", "none")
        parseDemoRow(arr.getJSONObject(0))
    } catch (e: Exception) {
        DemoState(false, true, false, "0m", "Could not verify demo: ${e.message}", "error")
    }
}

private fun startDemo(context: Context): DemoState {
    val session    = loadSession(context) ?: throw RuntimeException("Sign in first.")
    val deviceCode = getDeviceCode(context)
    val checkUrl   = "$SUPABASE_URL/rest/v1/kl_demo_sessions?product_id=eq.${PRODUCT_ID}&device_code=eq.${deviceCode}&select=id&limit=1"
    val checkConn  = URL(checkUrl).openConnection() as HttpURLConnection
    checkConn.setRequestProperty("apikey", PUBLISHABLE_KEY)
    checkConn.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
    checkConn.connectTimeout = 14_000; checkConn.readTimeout = 14_000
    if (JSONArray(checkConn.inputStream.bufferedReader().readText()).length() > 0) return getDemoStatus(context)

    val now = java.time.Instant.now()
    val exp = now.plusSeconds(DEMO_HOURS.toLong() * 3600L)
    val payload = JSONObject().apply {
        put("id", UUID.randomUUID().toString()); put("product_id", PRODUCT_ID)
        put("device_code", deviceCode); put("demo_started_at", now.toString())
        put("demo_expires_at", exp.toString()); put("is_active", true)
    }
    val conn = URL("$SUPABASE_URL/rest/v1/kl_demo_sessions").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
    conn.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
    conn.setRequestProperty("Prefer", "return=representation")
    conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
    val code = conn.responseCode
    val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: "[]"
    if (code !in 200..299) throw RuntimeException("Failed to start demo: $resp")
    val arr = JSONArray(resp)
    return if (arr.length() > 0) parseDemoRow(arr.getJSONObject(0))
    else {
        val remSec = DEMO_HOURS.toLong() * 3600L
        DemoState(true, true, false, formatRemaining(remSec), "Trial started. ${formatRemaining(remSec)} remaining.", "active")
    }
}

private fun parseDemoRow(row: JSONObject): DemoState {
    val isActive  = row.optBoolean("is_active", false)
    val expiresAt = parseIso(row.optString("demo_expires_at"))
    val now       = java.time.Instant.now()
    val valid     = isActive && expiresAt != null && expiresAt.isAfter(now)
    val remSec    = if (valid && expiresAt != null) maxOf(0L, expiresAt.epochSecond - now.epochSecond) else 0L
    val status    = if (valid) "active" else if (expiresAt != null) "expired" else "none"
    return DemoState(
        valid, true, false,
        formatRemaining(remSec),
        if (valid) "Trial active: ${formatRemaining(remSec)} remaining."
        else if (status == "expired") "Your trial has expired. Activate a license to continue."
        else "No trial found.",
        status
    )
}

private fun parseIso(s: String?): java.time.Instant? {
    if (s.isNullOrBlank()) return null
    return try { java.time.Instant.parse(s.trim().replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it }) }
    catch (e: Exception) { null }
}

private fun formatRemaining(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val d = seconds / 86400; val h = (seconds % 86400) / 3600; val m = (seconds % 3600) / 60
    return when { d > 0 -> "${d}d ${h}h"; h > 0 -> "${h}h ${m}m"; else -> "${m}m" }
}
