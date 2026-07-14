@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.slacklineadminapp.ui.screens.shvstore

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.slacklineadminapp.data.StoreSupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private val BG        = Color(0xFF000000)
private val CARD      = Color(0xFF0E0E0E)
private val CARD2     = Color(0xFF141414)
private val BORDER    = Color(0xFF1C1C1C)
private val ACCENT    = Color(0xFF00E5CC)
private val GOLD      = Color(0xFFFFB300)
private val AMBER     = Color(0xFFFF8C00)
private val TXT       = Color(0xFFF2F4F6)
private val TXT_MUTED = Color(0xFF4A5260)
private val TXT_SUB   = Color(0xFF8A929C)

private val ScreenshotsPanelHeight = 279.dp
private val ButtonPanelHeight      = 74.dp

private data class DisplayTier(val tier: String, val price: String, val type: String)

private fun parsePricing(json: String?): List<DisplayTier> {
    if (json.isNullOrBlank() || json == "[]") return emptyList()
    return try {
        Json.parseToJsonElement(json).jsonArray.mapNotNull { el ->
            val obj = el.jsonObject
            DisplayTier(
                tier  = obj["tier"]?.jsonPrimitive?.content  ?: return@mapNotNull null,
                price = obj["price"]?.jsonPrimitive?.content ?: "Free",
                type  = obj["type"]?.jsonPrimitive?.content  ?: "free"
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun typeLabel(type: String) = when (type) {
    "one_time" -> "" // Omitted to maximize space in compact layout
    "monthly"  -> "/mo"
    "yearly"   -> "/yr"
    else       -> ""
}

private fun tierColor(tier: String) = when (tier) {
    "Pro"  -> Color(0xFF00E5CC)
    "Pro+" -> Color(0xFFFFB300)
    else   -> Color(0xFF8A929C)
}

private fun tierIcon(tier: String): ImageVector = when (tier) {
    "Pro"  -> Icons.Default.Star
    "Pro+" -> Icons.Default.WorkspacePremium
    else   -> Icons.Default.Preview
}

@Composable
fun AppDetailScreen(
    appId: String,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    var app                by remember { mutableStateOf<JsonObject?>(null) }
    var ratings            by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var myRating           by remember { mutableStateOf<JsonObject?>(null) }
    var installing         by remember { mutableStateOf(false) }
    var progress           by remember { mutableFloatStateOf(0f) }
    
    // Dialog states
    var showLoginDialog    by remember { mutableStateOf(false) }
    var showRatingDialog   by remember { mutableStateOf(false) }
    var showMaintDialog    by remember { mutableStateOf(false) }
    var showReviewsDialog  by remember { mutableStateOf(false) }
    var showInstallGuide   by remember { mutableStateOf(false) }

    LaunchedEffect(appId) {
        val apps = StoreSupabaseApi.getApps(onlyPublished = true)
        app      = apps.find { it["id"]?.jsonPrimitive?.content == appId }
        ratings  = StoreSupabaseApi.getRatings(appId)
        myRating = if (StoreSupabaseApi.userId != null) StoreSupabaseApi.getMyRating(appId) else null
    }

    if (app == null) {
        Box(Modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ACCENT, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        return
    }

    val item             = app!!
    val appName          = item["name"]?.jsonPrimitive?.content ?: ""
    val tagline          = item["tagline"]?.jsonPrimitive?.content ?: ""
    val description      = item["description"]?.jsonPrimitive?.content ?: ""
    val version          = item["version"]?.jsonPrimitive?.content ?: ""
    val apkUrl           = item["apk_url"]?.jsonPrimitive?.content
    val iconUrl          = item["icon_url"]?.jsonPrimitive?.content ?: ""
    val rawShots         = item["screenshots"]?.jsonPrimitive?.content ?: ""
    val shots            = rawShots.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val hasUpdate        = item["has_update"]?.jsonPrimitive?.boolean ?: false
    val underMaintenance = item["is_under_maintenance"]?.jsonPrimitive?.boolean ?: false
    val pricingTiers     = parsePricing(item["pricing"]?.let {
        try { it.jsonArray.toString() } catch (_: Exception) { it.jsonPrimitive.content }
    })

    val hasScreenshots = shots.isNotEmpty()
    val hasApk         = !apkUrl.isNullOrEmpty()
    val hasPricing     = pricingTiers.isNotEmpty()
    val avgRating      = if (ratings.isEmpty()) 0f
                         else ratings.mapNotNull { it["stars"]?.jsonPrimitive?.int }.average().toFloat()

    // Download Logic Extracted
    val triggerDownload = {
        scope.launch {
            installing = true
            withContext(Dispatchers.IO) {
                try {
                    val cacheDir = File(context.cacheDir, "apks").also { if (!it.exists()) it.mkdirs() }
                    val file = File(cacheDir, "app_$appId.apk")
                    val conn = URL(apkUrl).openConnection() as HttpURLConnection
                    conn.connect()
                    val total = conn.contentLength
                    val input = conn.inputStream
                    val out   = FileOutputStream(file)
                    val buf   = ByteArray(4096)
                    var dl    = 0L; var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read); dl += read
                        progress = if (total > 0) dl.toFloat() / total else 0f
                    }
                    out.close(); input.close()
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                } catch (e: Exception) { e.printStackTrace() }
            }
            installing = false; progress = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(appName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TXT) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(ACCENT.copy(alpha = 0.1f))
                                    .border(1.dp, ACCENT.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ACCENT, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {

                // ── Scrollable body ──────────────────────────────────────────
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    // ── Update banner ────────────────────────────────────────
                    if (hasUpdate) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF1A1200), Color(0xFF0E0E00))))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(GOLD.copy(alpha = 0.15f))
                                    .border(1.dp, GOLD.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, tint = GOLD, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Update Available", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GOLD)
                                Text("A new version is ready. Download below to update.", fontSize = 11.sp, color = GOLD.copy(alpha = 0.7f), lineHeight = 15.sp)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, GOLD.copy(alpha = 0.5f), Color.Transparent))))
                    }

                    // ── Hero section ─────────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A0A), BG)))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Icon + name + badges row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                                    .background(CARD2).border(1.dp, BORDER, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(iconUrl).crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.Apps, null, tint = TXT_MUTED, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(appName, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TXT)
                                Text(tagline, fontSize = 13.sp, color = TXT_SUB, lineHeight = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InfoBadge(Icons.Default.Info, "v$version", ACCENT)
                                    if (underMaintenance) InfoBadge(Icons.Default.Build, "Maintenance", AMBER)
                                    // Reviews badge
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(GOLD.copy(alpha = 0.1f))
                                            .border(1.dp, GOLD.copy(alpha = 0.25f), RoundedCornerShape(7.dp))
                                            .clickable { showReviewsDialog = true }
                                            .padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Star, null, tint = GOLD, modifier = Modifier.size(10.dp))
                                        Text(
                                            if (ratings.isEmpty()) "Reviews"
                                            else "${"%.1f".format(avgRating)} (${ratings.size})",
                                            fontSize = 10.sp, color = GOLD,
                                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ── Ultra-Compact Inline Horizontal Pricing Rows ──
                        if (hasPricing) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pricingTiers.forEach { t ->
                                    val color = tierColor(t.tier)
                                    val displayPrice = if (t.type == "free" || t.price.lowercase() == "free") "Free" else "$${t.price}${typeLabel(t.type)}"
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.05f))
                                            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = tierIcon(t.tier),
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = t.tier,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = displayPrice,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TXT.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = BORDER)
                        Spacer(Modifier.height(14.dp))

                        // About / Description
                        Text("About this app", fontSize = 12.sp, color = TXT_MUTED, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(description, fontSize = 14.sp, color = TXT_SUB, lineHeight = 22.sp)
                    }

                    // Spacers for pinned panels below
                    if (hasScreenshots) Spacer(Modifier.height(ScreenshotsPanelHeight))
                    if (hasApk)         Spacer(Modifier.height(ButtonPanelHeight))
                }

                // ── Pinned bottom panels ─────────────────────────────────────
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {

                    // Screenshots panel
                    if (hasScreenshots) {
                        HorizontalDivider(color = BORDER)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFF080808).copy(alpha = 0.95f))
                                .padding(top = 10.dp, bottom = 10.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(ACCENT))
                                Text("SCREENSHOTS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ACCENT, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(shots) { url ->
                                    Box(
                                        modifier = Modifier.width(137.dp).height(218.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, BORDER, RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Download button panel
                    if (hasApk) {
                        HorizontalDivider(color = BORDER)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFF080808).copy(alpha = 0.95f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (installing) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Downloading…", fontSize = 11.sp, color = TXT_MUTED)
                                        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = ACCENT, fontWeight = FontWeight.Black)
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                        color = ACCENT, trackColor = BORDER
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                            }

                            val btnGradient = when {
                                underMaintenance -> Brush.horizontalGradient(listOf(AMBER.copy(alpha = 0.4f), AMBER.copy(alpha = 0.25f)))
                                hasUpdate        -> Brush.horizontalGradient(listOf(GOLD, Color(0xFFFFD040)))
                                installing       -> Brush.horizontalGradient(listOf(ACCENT.copy(alpha = 0.4f), Color(0xFF00BFA8).copy(alpha = 0.4f)))
                                else             -> Brush.horizontalGradient(listOf(ACCENT, Color(0xFF00BFA8)))
                            }
                            val btnLabel = when {
                                underMaintenance -> "Under Maintenance"
                                installing       -> "Downloading…"
                                hasUpdate        -> "Update Available — Install"
                                else             -> "Download & Install"
                            }
                            val btnIcon = when {
                                underMaintenance -> Icons.Default.Build
                                hasUpdate        -> Icons.Default.SystemUpdate
                                else             -> Icons.Default.Download
                            }
                            val btnTextColor = if (underMaintenance) AMBER else Color.Black

                            Button(
                                onClick = {
                                    when {
                                        underMaintenance -> showMaintDialog = true
                                        StoreSupabaseApi.userId == null -> showLoginDialog = true
                                        else -> showInstallGuide = true // Trigger the guide instead of downloading directly
                                    }
                                },
                                enabled = !installing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(btnGradient, shape = RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (installing) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        else Icon(btnIcon, null, tint = btnTextColor, modifier = Modifier.size(18.dp))
                                        Text(btnLabel, fontWeight = FontWeight.Black, fontSize = 15.sp, color = btnTextColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Installation Guide Dialog ──────────────────────────────────────────────
    if (showInstallGuide) {
        AlertDialog(
            onDismissRequest = { showInstallGuide = false },
            containerColor = CARD, shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(ACCENT.copy(alpha = 0.12f)).border(1.dp, ACCENT.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, null, tint = ACCENT, modifier = Modifier.size(18.dp))
                    }
                    Text("Installation Guide", color = TXT, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Since this app is not on the Play Store yet, your phone might ask you to confirm the installation.",
                        color = TXT_SUB, fontSize = 13.sp, lineHeight = 20.sp
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(CARD2), contentAlignment = Alignment.Center) {
                                Text("1", color = ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Wait for the download to finish.", color = TXT_MUTED, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(CARD2), contentAlignment = Alignment.Center) {
                                Text("2", color = ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("If prompted, tap Settings and turn on 'Allow from this source'.", color = TXT_MUTED, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(CARD2), contentAlignment = Alignment.Center) {
                                Text("3", color = ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Tap Install and you're good to go!", color = TXT_MUTED, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showInstallGuide = false
                        triggerDownload() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT), 
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got it, Download", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallGuide = false }) { Text("Cancel", color = TXT_MUTED) }
            }
        )
    }

    // ── Maintenance dialog ─────────────────────────────────────────────────────
    if (showMaintDialog) {
        AlertDialog(
            onDismissRequest = { showMaintDialog = false },
            containerColor = CARD, shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(AMBER.copy(alpha = 0.12f)).border(1.dp, AMBER.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Build, null, tint = AMBER, modifier = Modifier.size(18.dp))
                    }
                    Text("Under Maintenance", color = TXT, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text("This app is currently undergoing maintenance and is temporarily unavailable for download. Please check back later — we'll have it ready for you soon!",
                    color = TXT_SUB, fontSize = 14.sp, lineHeight = 21.sp)
            },
            confirmButton = {
                Button(onClick = { showMaintDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AMBER), shape = RoundedCornerShape(10.dp)) {
                    Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Reviews dialog ─────────────────────────────────────────────────────────
    if (showReviewsDialog) {
        AlertDialog(
            onDismissRequest = { showReviewsDialog = false },
            containerColor = CARD, shape = RoundedCornerShape(20.dp),
            title = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Star, null, tint = GOLD, modifier = Modifier.size(16.dp))
                        Text("Reviews", color = TXT, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (ratings.isNotEmpty())
                            Text("${"%.1f".format(avgRating)} · ${ratings.size}", color = TXT_MUTED, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (myRating != null) CARD2 else ACCENT.copy(alpha = 0.1f))
                            .border(1.dp, if (myRating != null) BORDER else ACCENT.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable {
                                showReviewsDialog = false
                                if (StoreSupabaseApi.userId == null) showLoginDialog = true else showRatingDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(if (myRating != null) "Edit" else "Rate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (myRating != null) TXT_MUTED else ACCENT)
                    }
                }
            },
            text = {
                if (ratings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("No reviews yet. Be the first!", fontSize = 13.sp, color = TXT_MUTED)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("%.1f".format(avgRating), fontSize = 36.sp, fontWeight = FontWeight.Black, color = GOLD)
                                StarRow(avgRating, 13.dp)
                                Text("${ratings.size} review${if (ratings.size != 1) "s" else ""}", fontSize = 10.sp, color = TXT_MUTED)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                (5 downTo 1).forEach { star ->
                                    val count = ratings.count { it["stars"]?.jsonPrimitive?.int == star }
                                    val frac  = if (ratings.isEmpty()) 0f else count.toFloat() / ratings.size
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("$star", fontSize = 10.sp, color = TXT_MUTED, modifier = Modifier.width(8.dp), textAlign = TextAlign.End)
                                        Icon(Icons.Default.Star, null, tint = GOLD, modifier = Modifier.size(9.dp))
                                        Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(BORDER)) {
                                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(frac)
                                                .background(Brush.horizontalGradient(listOf(GOLD, GOLD.copy(alpha = 0.6f)))))
                                        }
                                        Text("$count", fontSize = 10.sp, color = TXT_MUTED, modifier = Modifier.width(14.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = BORDER)
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ratings) { r ->
                                ReviewCard(
                                    stars  = r["stars"]?.jsonPrimitive?.int ?: 0,
                                    review = r["review"]?.jsonPrimitive?.content ?: "",
                                    date   = r["created_at"]?.jsonPrimitive?.content?.take(10) ?: ""
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReviewsDialog = false }) { Text("Close", color = TXT_MUTED) } }
        )
    }

    // ── Rating dialog ──────────────────────────────────────────────────────────
    if (showRatingDialog) {
        RatingDialog(
            existing  = myRating,
            onDismiss = { showRatingDialog = false },
            onSubmit  = { stars, review ->
                scope.launch {
                    val ex = myRating
                    if (ex != null) StoreSupabaseApi.updateRating(ex["id"]?.jsonPrimitive?.content ?: return@launch, stars, review)
                    else            StoreSupabaseApi.insertRating(appId, stars, review)
                    ratings  = StoreSupabaseApi.getRatings(appId)
                    myRating = StoreSupabaseApi.getMyRating(appId)
                    showRatingDialog = false
                }
            }
        )
    }

    // ── Login dialog ───────────────────────────────────────────────────────────
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            containerColor = CARD, shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, null, tint = ACCENT, modifier = Modifier.size(18.dp))
                    Text("Sign in required", color = TXT, fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("You need to sign in to download apps.", color = TXT_SUB, fontSize = 14.sp) },
            confirmButton = {
                Button(onClick = { showLoginDialog = false; onRequireLogin() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT), shape = RoundedCornerShape(10.dp)) {
                    Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showLoginDialog = false }) { Text("Cancel", color = TXT_MUTED) } }
        )
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
    }
}

@Composable
private fun StarRow(stars: Float, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        (1..5).forEach { i ->
            Icon(Icons.Default.Star, null, tint = if (i <= stars) GOLD else BORDER, modifier = Modifier.size(size))
        }
    }
}

@Composable
private fun ReviewCard(stars: Int, review: String, date: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(CARD2).border(1.dp, BORDER, RoundedCornerShape(12.dp)).padding(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StarRow(stars.toFloat(), 13.dp)
                Text(date, fontSize = 9.sp, color = TXT_MUTED, letterSpacing = 0.3.sp)
            }
            if (review.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(review, fontSize = 12.sp, color = TXT_SUB, lineHeight = 17.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RatingDialog(existing: JsonObject?, onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var selectedStars by remember { mutableIntStateOf(existing?.get("stars")?.jsonPrimitive?.int ?: 0) }
    var review        by remember { mutableStateOf(existing?.get("review")?.jsonPrimitive?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CARD, shape = RoundedCornerShape(20.dp),
        title = { Text(if (existing != null) "Edit Your Review" else "Rate this App", color = TXT, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { i ->
                        Icon(Icons.Default.Star, null,
                            tint = if (i <= selectedStars) GOLD else BORDER,
                            modifier = Modifier.size(36.dp).clickable { selectedStars = i })
                    }
                }
                OutlinedTextField(
                    value = review, onValueChange = { review = it },
                    label = { Text("Write a review (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    maxLines = 5, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ACCENT.copy(alpha = 0.7f), unfocusedBorderColor = BORDER,
                        focusedTextColor = TXT, unfocusedTextColor = TXT, cursorColor = ACCENT,
                        focusedLabelColor = ACCENT, unfocusedLabelColor = TXT_MUTED,
                        focusedContainerColor = Color(0xFF0A0A0A), unfocusedContainerColor = Color(0xFF0A0A0A)
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (selectedStars > 0) onSubmit(selectedStars, review) },
                enabled = selectedStars > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT), shape = RoundedCornerShape(10.dp)) {
                Text("Submit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TXT_MUTED) } }
    )
}
