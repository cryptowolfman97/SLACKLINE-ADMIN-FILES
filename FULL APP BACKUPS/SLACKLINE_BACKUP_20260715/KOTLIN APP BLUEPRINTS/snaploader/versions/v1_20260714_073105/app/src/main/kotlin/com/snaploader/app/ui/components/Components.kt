package com.snaploader.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaploader.app.model.*
import com.snaploader.app.ui.theme.*

// ── Glowing accent divider ────────────────────────────────────────────────────
@Composable
fun GlowDivider(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    brush       = Brush.horizontalGradient(
                        listOf(Color.Transparent, primary, Color.Transparent)
                    ),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 2f,
                    cap         = StrokeCap.Round
                )
            }
    )
}

// ── Platform chip ─────────────────────────────────────────────────────────────
@Composable
fun PlatformBadge(platform: Platform, modifier: Modifier = Modifier) {
    val accent = Color(platform.color or 0xFF000000)
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(6.dp),
        color    = accent.copy(alpha = 0.15f),
        border   = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(platform.emoji, fontSize = 12.sp)
            Text(
                text  = platform.label,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

// ── Animated gradient button ──────────────────────────────────────────────────
@Composable
fun GradientButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    enabled : Boolean  = true,
    icon    : @Composable (() -> Unit)? = null
) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val alpha     = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(listOf(primary, secondary)),
                alpha = alpha
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.invoke()
            Text(
                text       = text,
                color      = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp
            )
        }
    }
}

// ── Quality option card ───────────────────────────────────────────────────────
@Composable
fun QualityCard(
    option    : QualityOption,
    onDownload: (QualityOption) -> Unit
) {
    val isVideo = option.mediaType == MediaType.VIDEO
    val icon    = when (option.mediaType) {
        MediaType.VIDEO -> Icons.Default.PlayArrow
        MediaType.AUDIO -> Icons.Default.MusicNote
        MediaType.IMAGE -> Icons.Default.Image
    }
    val accentColor = when (option.mediaType) {
        MediaType.VIDEO -> MaterialTheme.colorScheme.primary
        MediaType.AUDIO -> MaterialTheme.colorScheme.secondary
        MediaType.IMAGE -> Color(0xFFFF9800)
    }

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = option.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (option.sizeEstimate.isNotEmpty()) {
                    Text(
                        text  = option.sizeEstimate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text  = option.format.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }

            IconButton(
                onClick  = { onDownload(option) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = accentColor
                )
            }
        }
    }
}

// ── Download item row ─────────────────────────────────────────────────────────
@Composable
fun DownloadRow(
    item    : DownloadItem,
    onRemove: (String) -> Unit
) {
    val statusColor = when (item.status) {
        DownloadStatus.COMPLETED   -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED      -> MaterialTheme.colorScheme.error
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondary
        else                       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (item.status) {
        DownloadStatus.QUEUED      -> "Queued"
        DownloadStatus.DOWNLOADING -> "${item.progress}%"
        DownloadStatus.PAUSED      -> "Paused"
        DownloadStatus.COMPLETED   -> "Done ✓"
        DownloadStatus.FAILED      -> "Failed ✗"
    }

    // Format speed nicely
    val speedText = when {
        item.status != DownloadStatus.DOWNLOADING -> ""
        item.speedBps <= 0                        -> ""
        item.speedBps < 1024                      -> "${item.speedBps} B/s"
        item.speedBps < 1024 * 1024               -> "${"%.1f".format(item.speedBps / 1024f)} KB/s"
        else -> "${"%.1f".format(item.speedBps / (1024f * 1024f))} MB/s"
    }

    // Format ETA nicely
    val etaText = when {
        item.status != DownloadStatus.DOWNLOADING -> ""
        item.etaSeconds <= 0                      -> ""
        item.etaSeconds < 60                      -> "${item.etaSeconds}s left"
        item.etaSeconds < 3600                    ->
            "${item.etaSeconds / 60}m ${item.etaSeconds % 60}s left"
        else -> "${item.etaSeconds / 3600}h left"
    }

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformBadge(item.platform)
                Text(
                    text     = item.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color    = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = statusLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = statusColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick  = { onRemove(item.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress   = { item.progress / 100f },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )
                // Speed and ETA row
                if (speedText.isNotEmpty() || etaText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text  = speedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = etaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text     = "${item.quality} · ${item.format.uppercase()}",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ── Loading shimmer card ──────────────────────────────────────────────────────
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -300f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "shimmerX"
    )
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val shimmer = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(surface)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, shimmer.copy(0.4f), Color.Transparent),
                        startX = shimmerX,
                        endX   = shimmerX + 300f
                    )
                )
            }
    )
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text          = title.uppercase(),
        style         = MaterialTheme.typography.labelSmall,
        color         = MaterialTheme.colorScheme.primary,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        modifier      = modifier.padding(vertical = 8.dp)
    )
}