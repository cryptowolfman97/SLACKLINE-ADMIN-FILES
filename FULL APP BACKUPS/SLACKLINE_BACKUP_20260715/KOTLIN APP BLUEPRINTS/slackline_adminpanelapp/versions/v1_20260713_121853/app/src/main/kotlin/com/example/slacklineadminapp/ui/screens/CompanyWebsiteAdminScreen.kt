package com.example.slacklineadminapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.slacklineadminapp.ui.theme.*

private const val ADMIN_URL = "https://shvertex.online/admin.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CompanyWebsiteAdminScreen(onNavigateBack: () -> Unit) {

    val appColors = LocalAppColors.current

    var webViewRef   by remember { mutableStateOf<WebView?>(null) }
    var isLoading    by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var pageTitle    by remember { mutableStateOf("shvertex.online/admin.html") }

    // Hardware back — go back inside WebView first, then exit screen
    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack()
        else onNavigateBack()
    }

    // Progress bar fades out on load complete
    val progressAlpha by animateFloatAsState(
        targetValue    = if (isLoading) 1f else 0f,
        animationSpec  = tween(durationMillis = 500),
        label          = "progressAlpha"
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
                            tint               = TealCol
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text          = "COMPANY WEBSITE ADMIN",
                            color         = TealCol,
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

                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint               = if (isLoading) TealCol.copy(alpha = 0.35f) else TealCol
                        )
                    }
                }

                // Animated progress bar
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
                                .background(TealCol.copy(alpha = progressAlpha))
                        )
                    }
                }
            }
        }

        // ── WebView ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled   = true   // Required — site JS + Supabase
                            domStorageEnabled    = true   // Required — Supabase auth session storage
                            databaseEnabled      = true
                            loadWithOverviewMode = true
                            useWideViewPort      = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }
                        // Cookies — needed for Supabase session to persist across pages
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView, url: String) {
                                isLoading = false
                                CookieManager.getInstance().flush()
                            }
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = false  // All URLs load inside the WebView
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress == 100) isLoading = false
                            }
                            override fun onReceivedTitle(view: WebView, title: String) {
                                // Show the page section title in the topbar subtitle
                                pageTitle = title.ifBlank { "shvertex.online/admin.html" }
                            }
                        }
                        webViewRef = this
                        loadUrl(ADMIN_URL)
                    }
                },
                update = { wv -> webViewRef = wv }
            )

            // Initial loading overlay — disappears once page starts rendering
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
                            color       = TealCol,
                            modifier    = Modifier.size(38.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text          = "LOADING ADMIN PANEL",
                            color         = SubText,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text     = "shvertex.online",
                            color    = TealCol.copy(alpha = 0.45f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
