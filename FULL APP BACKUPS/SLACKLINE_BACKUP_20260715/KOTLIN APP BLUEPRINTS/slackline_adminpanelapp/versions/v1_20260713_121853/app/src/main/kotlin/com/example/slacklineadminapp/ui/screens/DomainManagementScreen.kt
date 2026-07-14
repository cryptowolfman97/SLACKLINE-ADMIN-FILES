package com.example.slacklineadminapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.slacklineadminapp.ui.theme.*
import org.json.JSONObject
import java.io.File

// ── Constants ─────────────────────────────────────────────────────────────────

private const val NAMECHEAP_URL       = "https://ap.www.namecheap.com/"
private const val CREDENTIALS_FILE    = "namecheap_credentials.json"

// Desktop-class UA string — Namecheap's mobile view hides the domain list
// controls, DNS editor tabs, and bulk actions, so we force desktop rendering.
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

// Injected after every page load in desktop mode — some sites (Namecheap
// included) decide mobile vs desktop layout from the viewport meta tag as
// well as the UA string, so the UA swap alone isn't always enough.
private const val FORCE_DESKTOP_VIEWPORT_JS = """
    (function() {
        var meta = document.querySelector('meta[name="viewport"]');
        if (!meta) {
            meta = document.createElement('meta');
            meta.name = 'viewport';
            document.getElementsByTagName('head')[0].appendChild(meta);
        }
        meta.setAttribute('content', 'width=1280, initial-scale=0.4');
    })();
"""

// ── Credential storage (plain JSON in app's internal "root" storage) ──────────

private data class NamecheapCredentials(val username: String, val password: String)

private object NamecheapCredentialStore {
    private fun file(context: android.content.Context) = File(context.filesDir, CREDENTIALS_FILE)

    fun save(context: android.content.Context, username: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        file(context).writeText(json.toString())
    }

    fun load(context: android.content.Context): NamecheapCredentials? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            val json = JSONObject(f.readText())
            val u = json.optString("username", "")
            val p = json.optString("password", "")
            if (u.isBlank() && p.isBlank()) null else NamecheapCredentials(u, p)
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: android.content.Context) {
        file(context).delete()
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DomainManagementScreen(
    onNavigateBack: () -> Unit,
    onCloseToDashboard: () -> Unit = onNavigateBack
) {

    val appColors = LocalAppColors.current
    val context   = LocalContext.current

    var webViewRef   by remember { mutableStateOf<WebView?>(null) }
    var isLoading    by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var pageTitle    by remember { mutableStateOf("ap.www.namecheap.com") }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(true) }

    val savedCredentials = remember { mutableStateOf(NamecheapCredentialStore.load(context)) }

    // Applies the current desktop/mobile mode to the WebView and reloads.
    fun applyViewMode(wv: WebView, desktop: Boolean) {
        wv.settings.apply {
            userAgentString = if (desktop) DESKTOP_USER_AGENT else WebSettings.getDefaultUserAgent(context)
            useWideViewPort  = true
            loadWithOverviewMode = true
        }
        wv.reload()
    }

    // Hardware back — go back inside WebView first, then exit screen
    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack()
        else onNavigateBack()
    }

    val progressAlpha by animateFloatAsState(
        targetValue   = if (isLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label         = "progressAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(0.dp),
            colors    = CardDefaults.cardColors(containerColor = appColors.card),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val wv = webViewRef
                            if (wv != null && wv.canGoBack()) wv.goBack()
                            else onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = BlueCol
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text          = "DOMAIN MANAGEMENT",
                            color         = BlueCol,
                            fontWeight    = FontWeight.ExtraBold,
                            fontSize      = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text     = pageTitle,
                            color    = SubText,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = { showCredentialsDialog = true }) {
                        Icon(
                            imageVector        = Icons.Default.Lock,
                            contentDescription = "Saved credentials",
                            tint               = BlueCol
                        )
                    }

                    IconButton(
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewRef?.let { applyViewMode(it, isDesktopMode) }
                        }
                    ) {
                        Icon(
                            imageVector        = if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                            contentDescription = if (isDesktopMode) "Switch to mobile view" else "Switch to desktop view",
                            tint               = BlueCol
                        )
                    }

                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint               = if (isLoading) BlueCol.copy(alpha = 0.35f) else BlueCol
                        )
                    }

                    IconButton(onClick = onCloseToDashboard) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close module",
                            tint               = RedCol
                        )
                    }
                }

                if (progressAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(appColors.card)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = loadProgress / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                .background(BlueCol.copy(alpha = progressAlpha))
                        )
                    }
                }
            }
        }

        // ── WebView ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled    = true
                            domStorageEnabled    = true
                            databaseEnabled      = true
                            loadWithOverviewMode = true
                            useWideViewPort      = true
                            setSupportZoom(true)
                            builtInZoomControls  = true
                            displayZoomControls  = false
                            // Force the desktop layout — Namecheap's mobile
                            // site hides DNS/nameserver editing and bulk
                            // domain tools that live only in the desktop UI.
                            userAgentString      = if (isDesktopMode) DESKTOP_USER_AGENT else WebSettings.getDefaultUserAgent(ctx)
                            cacheMode            = WebSettings.LOAD_DEFAULT
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView, url: String) {
                                isLoading = false
                                CookieManager.getInstance().flush()
                                if (isDesktopMode) view.evaluateJavascript(FORCE_DESKTOP_VIEWPORT_JS, null)
                            }
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = false  // Keep all navigation inside the WebView
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress == 100) isLoading = false
                            }
                            override fun onReceivedTitle(view: WebView, title: String) {
                                pageTitle = title.ifBlank { "ap.www.namecheap.com" }
                            }
                        }
                        webViewRef = this
                        loadUrl(NAMECHEAP_URL)
                    }
                },
                update = { wv -> webViewRef = wv }
            )

            if (isLoading && loadProgress < 20) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appColors.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color       = BlueCol,
                            modifier    = Modifier.size(38.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text          = "LOADING NAMECHEAP",
                            color         = SubText,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text     = "Desktop view — ap.www.namecheap.com",
                            color    = BlueCol.copy(alpha = 0.45f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }

    if (showCredentialsDialog) {
        NamecheapCredentialsDialog(
            existing = savedCredentials.value,
            onDismiss = { showCredentialsDialog = false },
            onSave = { user, pass ->
                NamecheapCredentialStore.save(context, user, pass)
                savedCredentials.value = NamecheapCredentials(user, pass)
                showCredentialsDialog = false
            },
            onClear = {
                NamecheapCredentialStore.clear(context)
                savedCredentials.value = null
                showCredentialsDialog = false
            },
            onGoToNamecheap = {
                showCredentialsDialog = false
                // One-click jump straight back to the login page inside the WebView.
                webViewRef?.loadUrl(NAMECHEAP_URL)
            }
        )
    }
}

// ── Credentials dialog ────────────────────────────────────────────────────────

@Composable
private fun NamecheapCredentialsDialog(
    existing: NamecheapCredentials?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onGoToNamecheap: () -> Unit
) {
    val appColors = LocalAppColors.current
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.card)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.Language,
                        contentDescription = null,
                        tint               = BlueCol,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text          = "NAMECHEAP CREDENTIALS",
                        color         = BlueCol,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text     = "Stored locally on-device. Used only to help you jump straight to the Namecheap login page.",
                    color    = SubText,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username / Email") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BlueCol,
                        unfocusedBorderColor = SubText.copy(alpha = 0.4f),
                        focusedLabelColor    = BlueCol,
                        cursorColor          = BlueCol
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = SubText
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BlueCol,
                        unfocusedBorderColor = SubText.copy(alpha = 0.4f),
                        focusedLabelColor    = BlueCol,
                        cursorColor          = BlueCol
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onClear,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedCol)
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }
                    Button(
                        onClick  = { onSave(username.trim(), password) },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = BlueCol)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick  = onGoToNamecheap,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = TealCol)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go to Namecheap Login", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = SubText, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "Note: Namecheap login fields aren't auto-filled for security reasons — copy your saved password in from here.",
                    color    = SubText.copy(alpha = 0.7f),
                    fontSize = 9.5.sp
                )
            }
        }
    }
}
