package com.snaploader.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaploader.app.model.*
import com.snaploader.app.ui.components.*
import com.snaploader.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context     = LocalContext.current
    val keyboard    = LocalSoftwareKeyboardController.current
    val urlInput    by viewModel.urlInput.collectAsState()
    val platform    by viewModel.detectedPlatform.collectAsState()
    val fetchResult by viewModel.fetchResult.collectAsState()
    val showBatch   by viewModel.showBatchDialog.collectAsState()
    val batchEntries by viewModel.batchEntries.collectAsState()
    val pendingClip by viewModel.pendingClipboardUrl.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var extraUrls by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    if (showBatch && batchEntries.isNotEmpty()) {
        BatchDownloadDialog(
            entries    = batchEntries,
            onToggle   = viewModel::toggleBatchEntry,
            onDownload = { viewModel.startBatchDownload() },
            onDismiss  = viewModel::dismissBatchDialog
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        // Clipboard paste confirmation banner
        if (pendingClip != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.acceptClipboardUrl() }) {
                            Text(
                                "Paste",
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissAction = {
                        IconButton(onClick = { viewModel.dismissClipboardUrl() }) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                ) {
                    Text(
                        text  = "📋 ${pendingClip!!.take(45)}…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding      = PaddingValues(vertical = 16.dp)
        ) {
            item { HomeHeader() }

            item {
                SectionHeader("Choose Platform")
                PlatformGrid(selected = platform)
            }

            item { GlowDivider() }

            item {
                SectionHeader("Paste Link")
                MultiUrlInputCard(
                    primaryUrl       = urlInput,
                    extraUrls        = extraUrls,
                    platform         = platform,
                    onPrimaryChanged = viewModel::onUrlChanged,
                    onExtraChanged   = { idx, value ->
                        extraUrls = extraUrls.toMutableList().also { it[idx] = value }
                    },
                    onAddSlot    = { if (extraUrls.size < 4) extraUrls = extraUrls + "" },
                    onRemoveSlot = { idx ->
                        extraUrls = extraUrls.toMutableList().also { it.removeAt(idx) }
                    },
                    onPaste = {
                        val cm   = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                   as ClipboardManager
                        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (text.isNotEmpty()) viewModel.attemptPasteFromClipboard(text)
                    },
                    onClear = {
                        viewModel.clearUrl()
                        extraUrls = listOf()
                    },
                    onFetch = {
                        keyboard?.hide()
                        val validExtras = extraUrls.filter { it.startsWith("http") }
                        if (validExtras.isNotEmpty()) {
                            val allUrls = listOf(urlInput) + extraUrls
                            viewModel.attemptBatchDownload(
                                allUrls.filter { it.startsWith("http") }
                            )
                        } else {
                            viewModel.fetchQualities()
                        }
                    }
                )
            }

            when (val result = fetchResult) {
                is FetchResult.Loading -> {
                    item { SectionHeader("Available Qualities") }
                    items(3) { ShimmerCard() }
                }
                is FetchResult.Success -> {
                    item {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            SectionHeader("Available Qualities")
                            PlatformBadge(result.platform)
                        }
                        if (result.title.isNotEmpty()) {
                            Text(
                                text     = result.title,
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (result.qualities.isEmpty()) {
                        item {
                            Text(
                                "No downloadable formats found.",
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(result.qualities, key = { it.label }) { option ->
                            QualityCard(option = option, onDownload = viewModel::startDownload)
                        }
                    }
                }
                is FetchResult.Error -> {
                    item { ErrorCard(message = result.message) }
                }
                null -> {}
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Multi-URL Input Card ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiUrlInputCard(
    primaryUrl      : String,
    extraUrls       : List<String>,
    platform        : Platform,
    onPrimaryChanged: (String) -> Unit,
    onExtraChanged  : (Int, String) -> Unit,
    onAddSlot       : () -> Unit,
    onRemoveSlot    : (Int) -> Unit,
    onPaste         : () -> Unit,
    onClear         : () -> Unit,
    onFetch         : () -> Unit
) {
    val totalUrls = 1 + extraUrls.size
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UrlTextField(
                value     = primaryUrl,
                index     = 1,
                total     = totalUrls,
                platform  = platform,
                onChanged = onPrimaryChanged,
                onClear   = onClear,
                onRemove  = null,
                onFetch   = onFetch
            )

            extraUrls.forEachIndexed { idx, url ->
                UrlTextField(
                    value     = url,
                    index     = idx + 2,
                    total     = totalUrls,
                    platform  = Platform.GENERAL,
                    onChanged = { onExtraChanged(idx, it) },
                    onClear   = { onExtraChanged(idx, "") },
                    onRemove  = { onRemoveSlot(idx) },
                    onFetch   = onFetch
                )
            }

            if (totalUrls < 5) {
                TextButton(
                    onClick  = onAddSlot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Add another link ($totalUrls/5)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onPaste,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paste", fontWeight = FontWeight.Medium)
                }
                GradientButton(
                    text     = if (totalUrls > 1) "Get All ($totalUrls)" else "Get Qualities",
                    onClick  = onFetch,
                    enabled  = primaryUrl.isNotEmpty(),
                    modifier = Modifier.weight(2f),
                    icon     = {
                        Icon(
                            Icons.Default.Search, null,
                            tint     = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlTextField(
    value   : String,
    index   : Int,
    total   : Int,
    platform: Platform,
    onChanged: (String) -> Unit,
    onClear : () -> Unit,
    onRemove: (() -> Unit)?,
    onFetch : () -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChanged,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = {
            Text(
                if (index == 1) "https://... (paste link)" else "https://... (link $index)",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
            )
        },
        leadingIcon = {
            if (total > 1) {
                Text(
                    "$index",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    platform.emoji,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        },
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Remove, "Remove link",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction    = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onFetch() }),
        singleLine      = true,
        shape           = RoundedCornerShape(10.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor     = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor   = MaterialTheme.colorScheme.onSurface
        )
    )
}

// ── Batch Download Dialog ─────────────────────────────────────────────────────
@Composable
private fun BatchDownloadDialog(
    entries   : List<BatchEntry>,
    onToggle  : (String, Boolean) -> Unit,
    onDownload: () -> Unit,
    onDismiss : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(16.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "${entries.size} Links Detected",
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select which links to download:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                entries.forEach { entry ->
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked         = entry.selected,
                                onCheckedChange = { onToggle(entry.url, it) },
                                colors          = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = entry.platform.label,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text     = entry.url.take(50) +
                                               if (entry.url.length > 50) "…" else "",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                enabled = entries.any { it.selected }
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Download ${entries.count { it.selected }} selected",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Platform accent colours ───────────────────────────────────────────────────
private fun platformAccent(platform: Platform): Color = when (platform) {
    Platform.YOUTUBE   -> Color(0xFFFF0000)
    Platform.TIKTOK    -> Color(0xFF69C9D0)
    Platform.INSTAGRAM -> Color(0xFFE1306C)
    Platform.FACEBOOK  -> Color(0xFF1877F2)
    Platform.TWITTER   -> Color(0xFF1DA1F2)
    Platform.GENERAL   -> Color(0xFF00C853)
    Platform.UNKNOWN   -> Color(0xFF757575)
}

// ── Header with rainbow animated icon ────────────────────────────────────────
@Composable
private fun HomeHeader() {
    val infinite = rememberInfiniteTransition(label = "header")

    val hue by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label         = "hue"
    )
    val pulse by infinite.animateFloat(
        initialValue  = 0.82f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val arrowOffset by infinite.animateFloat(
        initialValue  = -5f,
        targetValue   = 5f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "arrow"
    )

    val c1 = Color(android.graphics.Color.HSVToColor(
        floatArrayOf(hue % 360f, 0.85f, 1f)))
    val c2 = Color(android.graphics.Color.HSVToColor(
        floatArrayOf((hue + 120f) % 360f, 0.85f, 1f)))
    val c3 = Color(android.graphics.Color.HSVToColor(
        floatArrayOf((hue + 240f) % 360f, 0.85f, 1f)))

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer rainbow glow ring
            Box(
                modifier = Modifier
                    .size((90f * pulse).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(c1.copy(0.30f), c2.copy(0.12f), Color.Transparent)
                        )
                    )
            )
            // Mid glow
            Box(
                modifier = Modifier
                    .size((74f * pulse).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(c2.copy(0.20f), Color.Transparent)
                        )
                    )
            )
            // Icon circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(c1, c2, c3),
                            Offset(0f, 0f), Offset(180f, 180f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(32.dp)) {
                    val w  = size.width
                    val h  = size.height
                    val sw = 3.8f

                    // Arrow shaft
                    drawLine(Color.White,
                        Offset(w / 2f, 2f + arrowOffset),
                        Offset(w / 2f, h * 0.62f + arrowOffset),
                        sw, StrokeCap.Round)
                    // Arrow left chevron
                    drawLine(Color.White,
                        Offset(w / 2f, h * 0.62f + arrowOffset),
                        Offset(w * 0.28f, h * 0.40f + arrowOffset),
                        sw, StrokeCap.Round)
                    // Arrow right chevron
                    drawLine(Color.White,
                        Offset(w / 2f, h * 0.62f + arrowOffset),
                        Offset(w * 0.72f, h * 0.40f + arrowOffset),
                        sw, StrokeCap.Round)
                    // Tray base
                    drawLine(Color.White,
                        Offset(w * 0.15f, h * 0.80f),
                        Offset(w * 0.85f, h * 0.80f),
                        sw, StrokeCap.Round)
                    // Tray left leg
                    drawLine(Color.White,
                        Offset(w * 0.15f, h * 0.62f),
                        Offset(w * 0.15f, h * 0.80f),
                        sw, StrokeCap.Round)
                    // Tray right leg
                    drawLine(Color.White,
                        Offset(w * 0.85f, h * 0.62f),
                        Offset(w * 0.85f, h * 0.80f),
                        sw, StrokeCap.Round)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "SHV Downloader",
            style      = MaterialTheme.typography.displayLarge,
            color      = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black
        )
        Text(
            "by SHV · Download anything, anywhere",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Platform Grid ─────────────────────────────────────────────────────────────
@Composable
private fun PlatformGrid(selected: Platform) {
    val rows = Platform.values().toList().chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                row.forEach { p ->
                    PlatformTile(
                        platform   = p,
                        isSelected = selected == p,
                        modifier   = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PlatformTile(
    platform  : Platform,
    isSelected: Boolean,
    modifier  : Modifier = Modifier
) {
    val accent = platformAccent(platform)
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = if (isSelected) accent.copy(alpha = 0.18f)
                   else MaterialTheme.colorScheme.surfaceVariant,
        border   = if (isSelected) BorderStroke(1.5.dp, accent)
                   else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(platform.emoji, fontSize = 22.sp)
            Text(
                text       = platform.label,
                style      = MaterialTheme.typography.labelSmall,
                color      = if (isSelected) accent
                             else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ── Error Card ────────────────────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.error.copy(0.1f),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, null,
                tint = MaterialTheme.colorScheme.error)
            Text(
                text  = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}