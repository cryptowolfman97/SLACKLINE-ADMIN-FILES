package com.snaploader.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.snaploader.app.manager.AppDownloadManager
import com.snaploader.app.model.MediaType
import com.snaploader.app.model.Platform
import com.snaploader.app.model.QualityOption
import com.snaploader.app.util.AdBlockList

// ── Data model ────────────────────────────────────────────────────────────────
data class DownloaderSite(
    val name    : String,
    val url     : String,
    val platform: String,
    val emoji   : String,
    val isCustom: Boolean = false
)

// ── Default pre-loaded sites ──────────────────────────────────────────────────
private val DEFAULT_SITES = listOf(
    DownloaderSite("tikdownloader.io",  "https://tikdownloader.io",           "TikTok",    "🎵"),
    DownloaderSite("SnapTik",           "https://snaptik.app/en",             "TikTok",    "🎵"),
    DownloaderSite("SSSTik",            "https://ssstik.io/en",               "TikTok",    "🎵"),
    DownloaderSite("SaveTT",            "https://savett.cc/en",               "TikTok",    "🎵"),
    DownloaderSite("SnapInsta",         "https://snapinsta.app",              "Instagram", "📸"),
    DownloaderSite("SaveIG",            "https://saveig.app",                 "Instagram", "📸"),
    DownloaderSite("Twitter Video DL",  "https://twittervideodownloader.com", "Twitter/X", "🐦"),
    DownloaderSite("Y2Mate",            "https://www.y2mate.com/en20",        "YouTube",   "▶️"),
    DownloaderSite("cobalt.tools",      "https://cobalt.tools",               "Universal", "🌐"),
)

// ── Prefs helpers ─────────────────────────────────────────────────────────────
private const val PREFS_NAME     = "web_tools_prefs"
private const val PREFS_SITES    = "custom_sites"
private const val SITE_SEPARATOR = "|||"
private const val FIELD_SEP      = "^^"

private fun loadCustomSites(context: Context): List<DownloaderSite> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_SITES, "") ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(SITE_SEPARATOR).mapNotNull { entry ->
        val p = entry.split(FIELD_SEP)
        if (p.size >= 3) DownloaderSite(p[0], p[1], p[2],
            if (p.size > 3) p[3] else "🌐", isCustom = true) else null
    }
}

private fun saveCustomSites(context: Context, sites: List<DownloaderSite>) {
    val raw = sites.filter { it.isCustom }.joinToString(SITE_SEPARATOR) {
        "${it.name}$FIELD_SEP${it.url}$FIELD_SEP${it.platform}$FIELD_SEP${it.emoji}"
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREFS_SITES, raw).apply()
}

// ── Main screen ───────────────────────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebToolsScreen(viewModel: com.snaploader.app.viewmodel.MainViewModel) {
    val context         = LocalContext.current
    val customSites     = remember { mutableStateListOf<DownloaderSite>().also {
        it.addAll(loadCustomSites(context)) } }
    val allSites        = remember(customSites.toList()) { DEFAULT_SITES + customSites }

    var activeSite      by remember { mutableStateOf<DownloaderSite?>(null) }
    var webViewRef      by remember { mutableStateOf<WebView?>(null) }
    var pageTitle       by remember { mutableStateOf("") }
    var loadingProgress by remember { mutableStateOf(0) }
    var isLoading       by remember { mutableStateOf(false) }
    var showAddDialog   by remember { mutableStateOf(false) }
    var showDeleteFor   by remember { mutableStateOf<DownloaderSite?>(null) }

    val downloadManager = remember { AppDownloadManager.getInstance(context) }

    BackHandler(enabled = activeSite != null) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack()
        else { activeSite = null; webViewRef = null }
    }

    if (showAddDialog) {
        AddSiteDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { site ->
                customSites.add(site)
                saveCustomSites(context, customSites)
                showAddDialog = false
            }
        )
    }

    showDeleteFor?.let { site ->
        AlertDialog(
            onDismissRequest = { showDeleteFor = null },
            shape            = RoundedCornerShape(16.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            title = { Text("Remove site?", fontWeight = FontWeight.Bold) },
            text  = { Text("Remove \"${site.name}\" from your list?") },
            confirmButton = {
                Button(onClick = {
                    customSites.remove(site)
                    saveCustomSites(context, customSites)
                    showDeleteFor = null
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteFor = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Sites list ─────────────────────────────────────────────────────────
        AnimatedVisibility(visible = activeSite == null,
            enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            SiteListView(
                sites         = allSites,
                onSiteClick   = { activeSite = it },
                onDeleteClick = { showDeleteFor = it },
                onAddClick    = { viewModel.attemptAddCustomSite { showAddDialog = true } }
            )
        }

        // ── WebView ────────────────────────────────────────────────────────────
        AnimatedVisibility(visible = activeSite != null,
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
            exit  = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(180))) {
            val site = activeSite ?: return@AnimatedVisibility
            Column(modifier = Modifier.fillMaxSize()) {

                // Toolbar
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val wv = webViewRef
                            if (wv != null && wv.canGoBack()) wv.goBack()
                            else { activeSite = null; webViewRef = null }
                        }) { Icon(Icons.Default.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onSurface) }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(site.name, style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface)
                            if (pageTitle.isNotEmpty() && pageTitle != site.name) {
                                Text(pageTitle, style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text("🛡 Ads blocked", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }

                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, "Reload",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (isLoading && loadingProgress < 100) {
                    LinearProgressIndicator(
                        progress   = { loadingProgress / 100f },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory  = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            settings.apply {
                                javaScriptEnabled            = true
                                domStorageEnabled            = true
                                loadWithOverviewMode         = true
                                useWideViewPort              = true
                                setSupportZoom(true)
                                builtInZoomControls          = true
                                displayZoomControls          = false
                                mixedContentMode             = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                userAgentString              = UA_MOBILE
                                allowFileAccess              = false
                                mediaPlaybackRequiresUserGesture = false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView, request: WebResourceRequest
                                ): WebResourceResponse? {
                                    if (viewModel.isAdBlockAllowed() && AdBlockList.shouldBlock(request.url.toString())) {
                                        return WebResourceResponse("text/plain", "UTF-8",
                                            "".byteInputStream())
                                    }
                                    return null
                                }
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }
                                override fun onPageFinished(view: WebView, url: String?) {
                                    isLoading = false
                                    pageTitle = view.title ?: ""
                                    view.evaluateJavascript(AD_HIDE_CSS_JS, null)
                                }
                            }

                            setDownloadListener { dlUrl, _, contentDisposition, mimeType, _ ->

                                // 1. Try to get extension from the URL path itself
                                val urlPath = dlUrl.substringBefore("?").substringAfterLast("/")
                                val urlExt  = urlPath.substringAfterLast('.', "").lowercase().take(5)

                                // 2. Try to get extension from mime type
                                val mimeExt = when {
                                    mimeType.contains("mp4")                    -> "mp4"
                                    mimeType.contains("webm")                   -> "webm"
                                    mimeType.contains("mp3")                    -> "mp3"
                                    mimeType.contains("m4a") || 
                                        mimeType.contains("mp4a")               -> "m4a"
                                    mimeType.contains("aac")                    -> "aac"
                                    mimeType.contains("opus")                   -> "opus"
                                    mimeType.contains("3gpp")                   -> "3gp"
                                    mimeType.contains("ogg")                    -> "ogg"
                                    mimeType.contains("mpeg")                   -> "mp3"
                                    mimeType.contains("octet-stream")           -> ""
                                    else                                        -> ""
                                }
                            
                                // 3. Try content-disposition filename
                                val dispExt = contentDisposition
                                    ?.let { Regex("filename\\*?=['\"]?(?:UTF-8'')?([^;\"'\\n]+)", RegexOption.IGNORE_CASE)
                                        .find(it)?.groupValues?.get(1)?.trim() }
                                    ?.substringAfterLast('.')
                                    ?.lowercase()
                                    ?.take(5)
                                    ?: ""

                                // 4. Pick best extension — prefer URL ext if it looks valid
                                val validExts = setOf("mp4","webm","mp3","m4a","aac","opus","3gp","ogg","mkv","mov","flv")
                                val ext = when {
                                    urlExt  in validExts -> urlExt
                                    dispExt in validExts -> dispExt
                                    mimeExt.isNotEmpty() -> mimeExt
                                    else                 -> "mp4" // safe fallback
                                }
                            
                                // 5. Build a clean filename
                                val rawName = when {
                                    dispExt.isNotEmpty() -> contentDisposition
                                        ?.let { Regex("filename\\*?=['\"]?(?:UTF-8'')?([^;\"'\\n]+)", RegexOption.IGNORE_CASE)
                                            .find(it)?.groupValues?.get(1)?.trim() }
                                        ?.substringBeforeLast('.')
                                        ?: urlPath.substringBeforeLast('.')
                                    urlExt.isNotEmpty()  -> urlPath.substringBeforeLast('.')
                                    else                 -> "WebDownload_${System.currentTimeMillis()}"
                                }
                                val title   = rawName.ifEmpty { "WebDownload_${System.currentTimeMillis()}" }
                                val isAudio = ext in setOf("mp3", "m4a", "aac", "opus", "ogg")
                            
                                downloadManager.enqueue(
                                    url      = dlUrl,
                                    quality  = QualityOption(
                                        label     = "Web Download",
                                        quality   = "web",
                                        format    = ext,
                                        mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
                                        directUrl = dlUrl
                                    ),
                                    title    = title,
                                    platform = Platform.GENERAL
                                )
}

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    loadingProgress = newProgress
                                    if (newProgress == 100) isLoading = false
                                }
                                override fun onReceivedTitle(view: WebView, title: String?) {
                                    pageTitle = title ?: ""
                                }
                            }

                            loadUrl(site.url)
                        }
                    },
                    update = { wv ->
                        if (wv.url != site.url && !wv.url.orEmpty().startsWith(site.url))
                            wv.loadUrl(site.url)
                    }
                )
            }
        }
    }
}

// ── Sites list ────────────────────────────────────────────────────────────────
@Composable
private fun SiteListView(
    sites        : List<DownloaderSite>,
    onSiteClick  : (DownloaderSite) -> Unit,
    onDeleteClick: (DownloaderSite) -> Unit,
    onAddClick   : () -> Unit
) {
    val grouped = sites.groupBy { it.platform }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Downloader Sites", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Text("Tap a site to open with ads blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, "Add site", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add site")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        grouped.forEach { (platform, sitesInGroup) ->
            item {
                Text(platform, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
            items(sitesInGroup, key = { it.url }) { site ->
                SiteCard(site = site, onClick = { onSiteClick(site) },
                    onDeleteClick = if (site.isCustom) ({ onDeleteClick(site) }) else null)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SiteCard(site: DownloaderSite, onClick: () -> Unit, onDeleteClick: (() -> Unit)?) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center) {
                Text(site.emoji, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(site.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(site.url.removePrefix("https://").removePrefix("www."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (site.isCustom && onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
                Icon(Icons.Default.OpenInBrowser, "Open",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Add site dialog ───────────────────────────────────────────────────────────
@Composable
private fun AddSiteDialog(onDismiss: () -> Unit, onAdd: (DownloaderSite) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var url      by remember { mutableStateOf("https://") }
    var platform by remember { mutableStateOf("Universal") }
    var emoji    by remember { mutableStateOf("🌐") }
    var urlError by remember { mutableStateOf(false) }
    val platforms = listOf("TikTok", "Instagram", "YouTube", "Twitter/X", "Facebook", "Universal")

    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Downloader Site", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Site name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it; urlError = false },
                    label = { Text("URL") }, singleLine = true, isError = urlError,
                    supportingText = if (urlError) ({ Text("Enter a valid https:// URL") }) else null,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji icon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Platform", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                platforms.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { p ->
                            FilterChip(selected = platform == p, onClick = { platform = p },
                                label = { Text(p, fontSize = 11.sp) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!url.startsWith("https://") && !url.startsWith("http://")) {
                    urlError = true; return@Button
                }
                onAdd(DownloaderSite(
                    name     = name.ifEmpty { url.removePrefix("https://").take(30) },
                    url      = url.trimEnd('/'), platform = platform,
                    emoji    = emoji.ifEmpty { "🌐" }, isCustom = true))
            }) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Constants ─────────────────────────────────────────────────────────────────
private const val UA_MOBILE =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"

private val AD_HIDE_CSS_JS = """
(function() {
  var css = `
    [id*="ad-"],[id*="_ad_"],[id*="-ad-"],
    [class*="ad-banner"],[class*="ad-container"],[class*="adsbygoogle"],
    [class*="advertisement"],[class*="advertising"],[class*="sponsor-"],
    [class*="popup-overlay"],[class*="pop-up"],
    [class*="modal-overlay"]:not([class*="download"]),
    [class*="interstitial"],[id*="popup"],[id*="interstitial"],
    iframe[src*="ad"],iframe[src*="banner"],
    ins.adsbygoogle { display: none !important; }
    body { overflow: auto !important; }
    html { overflow: auto !important; }
  `;
  var style = document.getElementById('snaploader-adblock');
  if (!style) { style = document.createElement('style'); style.id = 'snaploader-adblock';
    document.head.appendChild(style); }
  style.textContent = css;
})();
""".trimIndent()