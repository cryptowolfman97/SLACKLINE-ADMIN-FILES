package com.snaploader.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaploader.app.model.StorageStats
import com.snaploader.app.ui.theme.AccentColour
import com.snaploader.app.ui.theme.AppTheme
import com.snaploader.app.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val theme               by viewModel.theme.collectAsState()
    val accentColour        by viewModel.accentColour.collectAsState()
    val confirmExit         by viewModel.confirmExit.collectAsState()
    val wifiOnly            by viewModel.wifiOnly.collectAsState()
    val downloadPath        by viewModel.downloadPath.collectAsState()
    val sequentialQueue     by viewModel.sequentialQueue.collectAsState()
    val filenameTemplate    by viewModel.filenameTemplate.collectAsState()
    val downloadSubtitles   by viewModel.downloadSubtitles.collectAsState()
    val autoQualityEnabled  by viewModel.autoQualityEnabled.collectAsState()
    val preferredResolution by viewModel.preferredResolution.collectAsState()
    val preferredFormat     by viewModel.preferredFormat.collectAsState()
    val maxConcurrent       by viewModel.maxConcurrent.collectAsState()
    val storageStats        by viewModel.storageStats.collectAsState()

    var showTemplateDialog   by remember { mutableStateOf(false) }
    var showClearDialog      by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshStorageStats() }

    if (showTemplateDialog) {
        FilenameTemplateDialog(
            current   = filenameTemplate,
            onSave    = { viewModel.setFilenameTemplate(it); showTemplateDialog = false },
            onDismiss = { showTemplateDialog = false }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape            = RoundedCornerShape(16.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            icon  = { Icon(Icons.Default.DeleteSweep, null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Clear Downloads", fontWeight = FontWeight.Bold) },
            text  = { Text("Removes all items from the list. Files on storage are NOT deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(onClick = { viewModel.clearAll(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Clear all", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { OutlinedButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    if (showResolutionDialog) {
        ResolutionPickerDialog(
            current          = preferredResolution,
            currentFormat    = preferredFormat,
            onSaveResolution = { viewModel.setPreferredResolution(it) },
            onSaveFormat     = { viewModel.setPreferredFormat(it) },
            onDismiss        = { showResolutionDialog = false }
        )
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsGroupHeader("Appearance", Icons.Default.Palette)
        ThemeSelector(current = theme, onSelect = viewModel::setTheme)
        Spacer(Modifier.height(4.dp))

        // ── Accent Colour ─────────────────────────────────────────────────────
        AccentColourPicker(
            current  = accentColour,
            onSelect = viewModel::attemptSetAccentColour
        )
        Spacer(Modifier.height(4.dp))

        // ── Downloads ─────────────────────────────────────────────────────────
        SettingsGroupHeader("Downloads", Icons.Default.Download)
        ToggleRow(Icons.Default.Wifi,        "Wi-Fi only",
            "Pause downloads on mobile data",       wifiOnly,        viewModel::setWifiOnly)
        ToggleRow(Icons.Default.LinearScale, "Sequential queue",
            "Download one file at a time",          sequentialQueue,
            { v -> viewModel.attemptAdvancedSetting { viewModel.setSequentialQueue(v) } })
        ToggleRow(Icons.Default.ClosedCaption, "Download subtitles",
            "Auto-download English SRT with videos (yt-dlp only)",
            downloadSubtitles, { v -> viewModel.attemptAdvancedSetting { viewModel.setDownloadSubtitles(v) } })
        ConcurrentDownloadsRow(current = maxConcurrent, onChange = viewModel::setMaxConcurrent)
        Spacer(Modifier.height(4.dp))

        // ── Auto-quality ──────────────────────────────────────────────────────
        SettingsGroupHeader("Auto Quality", Icons.Default.Bolt)
        ToggleRow(Icons.Default.Bolt, "Auto-quality (skip picker)",
            "Instantly download at your preferred resolution when sharing",
            autoQualityEnabled, { v -> viewModel.attemptAdvancedSetting { viewModel.setAutoQualityEnabled(v) } })
        if (autoQualityEnabled) {
            ActionRow(Icons.Default.Hd, "Preferred resolution",
                "Currently: $preferredResolution · ${preferredFormat.uppercase()}",
                "Change") { viewModel.attemptAdvancedSetting { showResolutionDialog = true } }
        }
        Spacer(Modifier.height(4.dp))

        // ── Storage ───────────────────────────────────────────────────────────
        SettingsGroupHeader("Storage", Icons.Default.Storage)
        PathRow(Icons.Default.FolderOpen, "Download folder",
            downloadPath.ifEmpty { "/sdcard/Download/UDbySHV" }) {}
        storageStats?.let { StorageStatsCard(it) }
        ActionRow(Icons.Default.DriveFileRenameOutline, "Filename template",
            "Current: $filenameTemplate", "Edit") { viewModel.attemptAdvancedSetting { showTemplateDialog = true } }
        ActionRow(Icons.Default.DeleteSweep, "Clear downloads list",
            "Remove all from list (files kept on storage)", "Clear",
            labelColor = MaterialTheme.colorScheme.error) { showClearDialog = true }
        Spacer(Modifier.height(4.dp))

        // ── Behaviour ─────────────────────────────────────────────────────────
        SettingsGroupHeader("App Behaviour", Icons.Default.Settings)
        ToggleRow(Icons.AutoMirrored.Filled.ExitToApp, "Exit confirmation",
            "Show dialog when closing the app", confirmExit, viewModel::setConfirmExit)
        Spacer(Modifier.height(4.dp))

        // ── About ─────────────────────────────────────────────────────────────
        SettingsGroupHeader("About", Icons.Default.Info)
        AboutRow()
        Spacer(Modifier.height(80.dp))
    }
}

// ── Accent Colour Picker ──────────────────────────────────────────────────────
@Composable
private fun AccentColourPicker(
    current : AccentColour,
    onSelect: (AccentColour) -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ColorLens, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Column {
                    Text("Accent colour", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("${current.emoji} ${current.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Colour circles — 2 rows of 4
            val accents = AccentColour.values()
            accents.toList().chunked(4).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { accent ->
                        AccentCircle(
                            accent   = accent,
                            selected = accent == current,
                            onClick  = { onSelect(accent) }
                        )
                    }
                    // Pad incomplete last row
                    repeat(4 - row.size) {
                        Spacer(Modifier.size(56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentCircle(
    accent  : AccentColour,
    selected: Boolean,
    onClick : () -> Unit
) {
    val accentColor = accent.darkPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        // Outer ring for selected state
        if (selected) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(2.dp, accentColor),
                        CircleShape
                    )
            )
        }

        // Colour circle
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor)
                .then(
                    if (selected)
                        Modifier.shadow(6.dp, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check, null,
                    tint     = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Label below
        Text(
            text     = accent.displayName.take(6),
            fontSize = 7.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
        )
    }
}

// ── Resolution Picker Dialog ──────────────────────────────────────────────────
@Composable
private fun ResolutionPickerDialog(
    current         : String,
    currentFormat   : String,
    onSaveResolution: (String) -> Unit,
    onSaveFormat    : (String) -> Unit,
    onDismiss       : () -> Unit
) {
    var selectedRes    by remember { mutableStateOf(current) }
    var selectedFormat by remember { mutableStateOf(currentFormat) }
    val resolutions = listOf("2160p", "1440p", "1080p", "720p", "480p", "360p")
    val formats     = listOf("mp4", "webm")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(16.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        title  = { Text("Preferred Quality", fontWeight = FontWeight.Bold) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Resolution", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                resolutions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { res ->
                            val sel = selectedRes == res
                            Surface(modifier = Modifier.weight(1f).clickable { selectedRes = res },
                                shape  = RoundedCornerShape(8.dp),
                                color  = if (sel) MaterialTheme.colorScheme.primary.copy(0.15f)
                                         else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(if (sel) 2.dp else 1.dp,
                                    if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline)) {
                                Text(res, modifier = Modifier.padding(8.dp),
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (sel) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.onSurface,
                                    fontSize   = 12.sp)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
                Text("Format", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { fmt ->
                        FilterChip(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt },
                            label  = { Text(fmt.uppercase()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.2f),
                                selectedLabelColor     = MaterialTheme.colorScheme.primary))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveResolution(selectedRes); onSaveFormat(selectedFormat); onDismiss() }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Concurrent Downloads Row ──────────────────────────────────────────────────
@Composable
private fun ConcurrentDownloadsRow(current: Int, onChange: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Speed, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Max concurrent downloads", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Currently: $current at once", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Slider(value = current.toFloat(), onValueChange = { onChange(it.toInt()) },
                valueRange = 1f..5f, steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor       = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { n ->
                    Text("$n", style = MaterialTheme.typography.labelSmall,
                        color      = if (n == current) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (n == current) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// ── Storage Stats Card ────────────────────────────────────────────────────────
@Composable
private fun StorageStatsCard(stats: StorageStats) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Storage, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Storage Usage", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatChip("Total",  stats.displaySize)
                StatChip("Videos","${stats.videoCount}")
                StatChip("Audio", "${stats.audioCount}")
                StatChip("Files", "${stats.fileCount}")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Filename Template Dialog ──────────────────────────────────────────────────
@Composable
private fun FilenameTemplateDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var template by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(16.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        title  = { Text("Filename Template", fontWeight = FontWeight.Bold) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Available tokens:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("{title}" to "Video title", "{date}" to "Date (YYYYMMDD)",
                    "{platform}" to "Platform name").forEach { (token, desc) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(0.12f)) {
                            Text(token, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(value = template, onValueChange = { template = it },
                    label = { Text("Template") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("{title}") })
                if (template.isNotEmpty()) {
                    Text("Preview: ${template.replace("{title}", "Example Video")
                        .replace("{date}", "20260624").replace("{platform}", "TikTok")}.mp4",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(template.ifEmpty { "{title}" }) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Reusable rows ─────────────────────────────────────────────────────────────
@Composable
private fun ActionRow(
    icon      : ImageVector, title: String, subtitle: String, label: String,
    labelColor: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            TextButton(onClick = onClick) {
                Text(label, color = labelColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ThemeSelector(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeChip("AMOLED", "🌑", current == AppTheme.AMOLED,
                listOf(Color(0xFF000000), Color(0xFF1A1A2E)), Color.White,
                Modifier.weight(1f)) { onSelect(AppTheme.AMOLED) }
            ThemeChip("Light", "☀️", current == AppTheme.LIGHT,
                listOf(Color(0xFFF5F5F5), Color(0xFFE3F2FD)), Color(0xFF121212),
                Modifier.weight(1f)) { onSelect(AppTheme.LIGHT) }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String, emoji: String, selected: Boolean,
    gradient: List<Color>, textColor: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(Brush.linearGradient(gradient))
        .border(border, RoundedCornerShape(10.dp))
        .clickable { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = textColor, fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) {
                Spacer(Modifier.height(2.dp))
                Text("✓ Active", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun PathRow(icon: ImageVector, title: String, path: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(path, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutRow() {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⬇", fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("SHV Downloader by SHV",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Version 1.1.0 · Jetpack Compose · yt-dlp powered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String, icon: ImageVector) {
    Row(
        modifier              = Modifier.padding(start = 2.dp, bottom = 4.dp, top = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp))
        Text(title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = MaterialTheme.colorScheme.primary,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp)
    }
}
