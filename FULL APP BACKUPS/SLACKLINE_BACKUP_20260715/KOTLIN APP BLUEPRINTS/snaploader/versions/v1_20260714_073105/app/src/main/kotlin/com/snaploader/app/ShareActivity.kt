package com.snaploader.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaploader.app.model.*
import com.snaploader.app.ui.theme.AccentColour
import com.snaploader.app.ui.theme.SnapLoaderTheme
import com.snaploader.app.viewmodel.ShareFetchResult
import com.snaploader.app.viewmodel.ShareViewModel

class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
                ?.trim()?.takeIf { it.startsWith("http") }
            Intent.ACTION_VIEW -> intent.data?.toString()
                ?.takeIf { it.startsWith("http") }
            else -> null
        }

        if (url == null) { finish(); return }
        viewModel.loadUrl(url)

        setContent {
            val theme by viewModel.theme.collectAsState()
            val accentColour by viewModel.accentColour.collectAsState()
            SnapLoaderTheme(appTheme = theme, accentColour = accentColour) {
                ShareBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareBottomSheet(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit
) {
    val url              by viewModel.url.collectAsState()
    val platform         by viewModel.platform.collectAsState()
    val fetchResult      by viewModel.fetchResult.collectAsState()
    val downloadStarted  by viewModel.downloadStarted.collectAsState()
    val hasSubtitles     by viewModel.hasSubtitles.collectAsState()
    val autoQuality      by viewModel.autoQualityEnabled.collectAsState()

    var downloadSubs by remember { mutableStateOf(false) }
    var showMoreFormats by remember { mutableStateOf(false) }

    // Auto-dismiss 1.5 s after download is queued (hands back to YouTube with PiP visible)
    LaunchedEffect(downloadStarted) {
        if (downloadStarted) {
            kotlinx.coroutines.delay(1_500)
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Dim the background — tap to dismiss
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = Color.Black.copy(alpha = 0.55f),
            onClick  = onDismiss
        ) {}

        // ── Sheet ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape          = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {

                // Handle bar
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(width = 40.dp, height = 4.dp),
                        shape    = RoundedCornerShape(2.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
                    ) {}
                }

                // ── Header row ────────────────────────────────────────────────
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail or platform emoji
                    val thumbnail = (fetchResult as? ShareFetchResult.Success)?.thumbnail ?: ""
                    if (thumbnail.isNotEmpty()) {
                        AsyncImage(
                            model   = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnail).crossfade(true).build(),
                            contentDescription = "Thumbnail",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier         = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(platform.emoji, fontSize = 26.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Download from ${platform.label}",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        val title = (fetchResult as? ShareFetchResult.Success)?.title
                            ?: url
                        Text(
                            title,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
                Spacer(Modifier.height(8.dp))

                // ── Body ──────────────────────────────────────────────────────
                AnimatedContent(
                    targetState    = fetchResult,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label          = "share_body"
                ) { result ->
                    when (result) {

                        is ShareFetchResult.Loading -> {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color    = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        "Fetching available qualities…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is ShareFetchResult.Error -> {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline, null,
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    result.message,
                                    style     = MaterialTheme.typography.bodyMedium,
                                    color     = MaterialTheme.colorScheme.error
                                )
                                OutlinedButton(onClick = { viewModel.retry() }) {
                                    Icon(Icons.Default.Refresh, null,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }

                        is ShareFetchResult.Success -> {

                            // If auto-quality fired, show PiP confirmation
                            if (downloadStarted && autoQuality) {
                                Column(
                                    modifier            = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "Auto-downloading in background",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Monitor via the floating bubble",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                return@AnimatedContent
                            }

                            // Quality selection
                            val videoQualities = result.qualities.filter { it.mediaType == MediaType.VIDEO }
                            val audioQualities = result.qualities.filter { it.mediaType == MediaType.AUDIO }

                            // Primary formats = MP4 videos + all audio. Secondary = non-MP4
                            val primaryVideo  = videoQualities.filter { it.format == "mp4" }
                                .ifEmpty { videoQualities.take(4) }
                            val secondaryVideo = videoQualities.filter { it.format != "mp4" }

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Subtitle toggle
                                if (hasSubtitles) {
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ClosedCaption, null,
                                            tint     = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "Download English subtitles",
                                            style    = MaterialTheme.typography.bodyMedium,
                                            color    = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked       = downloadSubs,
                                            onCheckedChange = { downloadSubs = it },
                                            colors        = SwitchDefaults.colors(
                                                checkedTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 18.dp),
                                        color    = MaterialTheme.colorScheme.outline.copy(0.2f)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }

                                LazyColumn(
                                    modifier       = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    // Primary video formats
                                    if (primaryVideo.isNotEmpty()) {
                                        item {
                                            QualityGroupLabel("Video")
                                        }
                                        items(primaryVideo, key = { it.label }) { q ->
                                            ShareQualityRow(
                                                quality    = q,
                                                onDownload = {
                                                    viewModel.startDownload(q, downloadSubs)
                                                }
                                            )
                                        }
                                    }

                                    // "More formats" expander for non-MP4 alternatives
                                    if (secondaryVideo.isNotEmpty()) {
                                        item {
                                            TextButton(
                                                onClick = { showMoreFormats = !showMoreFormats },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    if (showMoreFormats) Icons.Default.ExpandLess
                                                    else Icons.Default.ExpandMore,
                                                    null, modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (showMoreFormats) "Hide alternative formats"
                                                    else "Show ${secondaryVideo.size} more formats",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        if (showMoreFormats) {
                                            items(secondaryVideo, key = { it.label + "_alt" }) { q ->
                                                ShareQualityRow(
                                                    quality    = q,
                                                    onDownload = {
                                                        viewModel.startDownload(q, downloadSubs)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Audio formats
                                    if (audioQualities.isNotEmpty()) {
                                        item {
                                            QualityGroupLabel("Audio Only")
                                        }
                                        items(audioQualities, key = { it.label }) { q ->
                                            ShareQualityRow(
                                                quality    = q,
                                                onDownload = {
                                                    viewModel.startDownload(q, false)
                                                }
                                            )
                                        }
                                    }

                                    item { Spacer(Modifier.height(8.dp)) }
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityGroupLabel(label: String) {
    Text(
        label.uppercase(),
        style         = MaterialTheme.typography.labelSmall,
        color         = MaterialTheme.colorScheme.primary,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun ShareQualityRow(
    quality   : QualityOption,
    onDownload: () -> Unit
) {
    val isVideo = quality.mediaType == MediaType.VIDEO
    val accent  = if (isVideo) MaterialTheme.colorScheme.primary
                  else         MaterialTheme.colorScheme.secondary

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = accent.copy(alpha = 0.07f),
        border   = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                contentDescription = null,
                tint     = accent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quality.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (quality.sizeEstimate.isNotEmpty()) {
                    Text(
                        quality.sizeEstimate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalButton(
                onClick = onDownload,
                colors  = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.copy(0.18f),
                    contentColor   = accent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Download", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
