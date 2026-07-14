package com.snaploader.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaploader.app.model.DownloadItem
import com.snaploader.app.model.DownloadStatus
import com.snaploader.app.model.MediaType
import com.snaploader.app.ui.components.GlowDivider
import com.snaploader.app.ui.components.SectionHeader
import com.snaploader.app.viewmodel.MainViewModel
import java.io.File

@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val active    = downloads.filter {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }
    val paused    = downloads.filter { it.status == DownloadStatus.PAUSED }
    val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
    val failed    = downloads.filter { it.status == DownloadStatus.FAILED }

    if (downloads.isEmpty()) {
        EmptyDownloads()
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding      = PaddingValues(vertical = 16.dp)
    ) {
        if (active.isNotEmpty()) {
            item { SectionHeader("Active (${active.size})") }
            items(active, key = { it.id }) { item ->
                DownloadCard(item = item,
                    onPause      = { viewModel.pauseDownload(item.id) },
                    onResume     = { viewModel.resumeDownload(item.id) },
                    onRetry      = { viewModel.retryDownload(item.id) },
                    onRemove     = { viewModel.removeDownload(item.id) },
                    onMoveUp     = { viewModel.moveDownloadUp(item.id) },
                    onMoveDown   = { viewModel.moveDownloadDown(item.id) },
                    onDeleteFile = { viewModel.deleteDownloadFile(item.id) })
            }
            item { GlowDivider(Modifier.padding(vertical = 4.dp)) }
        }

        if (paused.isNotEmpty()) {
            item { SectionHeader("Paused (${paused.size})") }
            items(paused, key = { it.id }) { item ->
                DownloadCard(item = item,
                    onPause      = { viewModel.pauseDownload(item.id) },
                    onResume     = { viewModel.resumeDownload(item.id) },
                    onRetry      = { viewModel.retryDownload(item.id) },
                    onRemove     = { viewModel.removeDownload(item.id) },
                    onMoveUp     = { viewModel.moveDownloadUp(item.id) },
                    onMoveDown   = { viewModel.moveDownloadDown(item.id) },
                    onDeleteFile = { viewModel.deleteDownloadFile(item.id) })
            }
            item { GlowDivider(Modifier.padding(vertical = 4.dp)) }
        }

        if (completed.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically) {
                    SectionHeader("Completed (${completed.size})")
                    TextButton(onClick = { viewModel.clearCompleted() }) {
                        Text("Clear all", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            items(completed, key = { it.id }) { item ->
                DownloadCard(item = item,
                    onPause      = { viewModel.pauseDownload(item.id) },
                    onResume     = { viewModel.resumeDownload(item.id) },
                    onRetry      = { viewModel.retryDownload(item.id) },
                    onRemove     = { viewModel.removeDownload(item.id) },
                    onMoveUp     = { viewModel.moveDownloadUp(item.id) },
                    onMoveDown   = { viewModel.moveDownloadDown(item.id) },
                    onDeleteFile = { viewModel.deleteDownloadFile(item.id) })
            }
        }

        if (failed.isNotEmpty()) {
            item { SectionHeader("Failed (${failed.size})") }
            items(failed, key = { it.id }) { item ->
                DownloadCard(item = item,
                    onPause      = { viewModel.pauseDownload(item.id) },
                    onResume     = { viewModel.resumeDownload(item.id) },
                    onRetry      = { viewModel.retryDownload(item.id) },
                    onRemove     = { viewModel.removeDownload(item.id) },
                    onMoveUp     = { viewModel.moveDownloadUp(item.id) },
                    onMoveDown   = { viewModel.moveDownloadDown(item.id) },
                    onDeleteFile = { viewModel.deleteDownloadFile(item.id) })
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Download Card ─────────────────────────────────────────────────────────────
@Composable
private fun DownloadCard(
    item        : DownloadItem,
    onPause     : () -> Unit,
    onResume    : () -> Unit,
    onRetry     : () -> Unit,
    onRemove    : () -> Unit,
    onMoveUp    : () -> Unit,
    onMoveDown  : () -> Unit,
    onDeleteFile: () -> Unit
) {
    val context  = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val error  = MaterialTheme.colorScheme.error

    val statusColor by animateColorAsState(
        targetValue = when (item.status) {
            DownloadStatus.COMPLETED   -> accent
            DownloadStatus.FAILED      -> error
            DownloadStatus.PAUSED      -> MaterialTheme.colorScheme.tertiary
            DownloadStatus.DOWNLOADING -> accent
            DownloadStatus.QUEUED      -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when (item.status) {
            DownloadStatus.DOWNLOADING -> accent.copy(0.5f)
            DownloadStatus.FAILED      -> error.copy(0.4f)
            DownloadStatus.PAUSED      -> MaterialTheme.colorScheme.tertiary.copy(0.4f)
            else                       -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    // Animated progress bar colour: interpolates Red→Amber→Green based on progress %
    val progressColor by animateColorAsState(
        targetValue = when {
            item.progress < 30  -> lerp(error,                               Color(0xFFFFA726), item.progress / 30f)
            item.progress < 70  -> lerp(Color(0xFFFFA726),                   accent,            (item.progress - 30) / 40f)
            else                -> accent
        },
        animationSpec = tween(400),
        label = "progressColor"
    )

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Top row ───────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                // Thumbnail or fallback emoji
                if (item.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model   = ImageRequest.Builder(context)
                            .data(item.thumbnailUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Box(modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text     = if (item.mediaType == MediaType.AUDIO) "🎵" else item.platform.emoji,
                            fontSize = 20.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = item.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = "${item.platform.label} · ${item.quality.uppercase()} · ${item.format.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip
                Surface(shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        text = when (item.status) {
                            DownloadStatus.COMPLETED   -> "✓ Done"
                            DownloadStatus.FAILED      -> "✗ Failed"
                            DownloadStatus.PAUSED      -> "⏸ Paused"
                            DownloadStatus.DOWNLOADING -> "${item.progress}%"
                            DownloadStatus.QUEUED      -> "Queued"
                        },
                        style      = MaterialTheme.typography.labelSmall,
                        color      = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Overflow menu
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, "More",
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu,
                        onDismissRequest = { showMenu = false }) {
                        if (item.status == DownloadStatus.COMPLETED && item.filePath.isNotEmpty()) {
                            DropdownMenuItem(
                                text        = { Text("Open file") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                onClick     = {
                                    showMenu = false
                                    val file = File(item.filePath)
                                    if (file.exists()) {
                                        val mime   = if (item.mediaType == MediaType.AUDIO) "audio/*" else "video/*"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.fromFile(file), mime)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(text = { Text("Move up") },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) },
                            onClick = { showMenu = false; onMoveUp() })
                        DropdownMenuItem(text = { Text("Move down") },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                            onClick = { showMenu = false; onMoveDown() })
                        HorizontalDivider()
                        if (item.status == DownloadStatus.COMPLETED) {
                            DropdownMenuItem(
                                text        = { Text("Delete file", color = error) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = error) },
                                onClick     = { showMenu = false; onDeleteFile() }
                            )
                        }
                        DropdownMenuItem(text = { Text("Remove from list") },
                            leadingIcon = { Icon(Icons.Default.Close, null) },
                            onClick     = { showMenu = false; onRemove() })
                    }
                }
            }

            // ── Animated progress bar ─────────────────────────────────────────
            if (item.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress      = { item.progress / 100f },
                    modifier      = Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color         = progressColor,
                    trackColor    = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap     = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Speed + ETA row
                if (item.speedBps > 0 || item.etaSeconds > 0) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text  = formatSpeed(item.speedBps),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        if (item.etaSeconds > 0) {
                            Text(
                                text  = "ETA ${formatEta(item.etaSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            // Active / Queued / Paused controls
            if (item.status == DownloadStatus.DOWNLOADING ||
                item.status == DownloadStatus.PAUSED ||
                item.status == DownloadStatus.QUEUED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        OutlinedButton(
                            onClick  = onPause,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp)
                        }
                    }
                    if (item.status == DownloadStatus.PAUSED) {
                        Button(
                            onClick  = onResume,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick  = onRemove,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        border   = BorderStroke(1.dp, error.copy(0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = error)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }

            // ── Failed: inline Retry + Remove row ────────────────────────────
            if (item.status == DownloadStatus.FAILED) {
                if (item.errorMessage.isNotEmpty()) {
                    Text(
                        text  = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = error.copy(0.8f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = onRetry,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick  = onRemove,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyDownloads() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyBounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = -12f,
        animationSpec  = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "arrowBounce"
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text     = "⬇",
            fontSize = 64.sp,
            modifier = Modifier.offset(y = offsetY.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "No downloads yet",
            style      = MaterialTheme.typography.headlineMedium,
            color      = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Paste a link on the Home tab, or share a video directly into SnapLoader",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = MaterialTheme.colorScheme.primary.copy(0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
        ) {
            Row(
                modifier       = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Share, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp))
                Text(
                    "Share any video → tap SnapLoader",
                    style  = MaterialTheme.typography.bodySmall,
                    color  = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────
private fun formatSpeed(bps: Long): String = when {
    bps <= 0             -> ""
    bps < 1024 * 1024    -> "${bps / 1024} KB/s"
    else                 -> "${"%.1f".format(bps / (1024f * 1024f))} MB/s"
}

private fun formatEta(seconds: Long): String = when {
    seconds <= 0   -> ""
    seconds < 60   -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else           -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}
