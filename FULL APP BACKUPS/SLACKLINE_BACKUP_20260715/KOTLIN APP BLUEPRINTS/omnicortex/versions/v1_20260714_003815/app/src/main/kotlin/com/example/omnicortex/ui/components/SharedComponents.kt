package com.example.omnicortex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.ui.theme.*

// ── Aegis Score Ring ──────────────────────────────────────────────────────────
@Composable
fun AegisScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    animateFrom: Int = 0
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "score"
    )

    val arcColor = when {
        score >= 75 -> AegisGreen
        score >= 50 -> AegisAmber
        else        -> AegisRed
    }
    val sweepAngle = (animatedScore / 100f) * 270f

    val scale = (ringSize / 180.dp).coerceIn(0.3f, 1f)
    val scoreFontSize = (40 * scale).sp
    val labelFontSize = (11 * scale).coerceAtLeast(8f).sp

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(ringSize)
                .drawBehind {
                    val sw     = strokeWidth.toPx()
                    val radius = (size.minDimension - sw) / 2f
                    val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
                    val arcSize = Size(radius * 2, radius * 2)
                    // Track
                    drawArc(
                        color      = BgCardBorder,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(sw, cap = StrokeCap.Round)
                    )
                    // Score arc
                    drawArc(
                        color      = arcColor,
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
        )

        // FIXED: Cleaned layout arrangement parameters. Removed custom line-height/font padding modifiers 
        // that caused alignment shifting on rendering engines, and hidden structural '/ 100' when size is small.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = if (ringSize < 100.dp) 2.dp else 0.dp)
        ) {
            Text(
                text = "$animatedScore",
                color = arcColor,
                fontSize = scoreFontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = PostureScoreLabel(score),
                color = arcColor.copy(alpha = 0.85f),
                fontSize = labelFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (ringSize >= 100.dp) {
                val subFontSize = (10 * scale).coerceAtLeast(7f).sp
                Text(
                    text = "/ 100",
                    color = TextMuted,
                    fontSize = subFontSize,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun PostureScoreLabel(score: Int) = when {
    score >= 90 -> "EXCELLENT"
    score >= 75 -> "GOOD"
    score >= 60 -> "FAIR"
    score >= 40 -> "AT RISK"
    else        -> "CRITICAL"
}

// ── Module card — used on Home dashboard ─────────────────────────────────────
@Composable
fun ModuleStatusCard(
    title: String,
    subtitle: String,
    score: Int?,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BgCardBorder, BgCardBorder))
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            if (score != null) {
                val scoreColor = when {
                    score >= 75 -> AegisGreen
                    score >= 50 -> AegisAmber
                    else        -> AegisRed
                }
                Text(
                    "$score",
                    color = scoreColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ── Finding row — used in Posture and App Permission screens ──────────────────
@Composable
fun FindingRow(
    title: String,
    detail: String,
    severity: Severity,
    passed: Boolean,
    fixAdvice: String = "",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val severityColor = severityColor(severity, passed)
    val icon = if (passed) Icons.Default.CheckCircle else severityIcon(severity)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgElevated)
            .border(1.dp, if (!passed) severityColor.copy(alpha = 0.3f) else BgCardBorder, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = severityColor, modifier = Modifier.size(18.dp))
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            SeverityChip(severity, passed)
        }
        if (expanded) {
            Text(detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            if (!passed && fixAdvice.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(AegisGreen.copy(alpha = 0.08f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("FIX:", color = AegisGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(fixAdvice, color = AegisGreen.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// ── Severity chip ─────────────────────────────────────────────────────────────
@Composable
fun SeverityChip(severity: Severity, passed: Boolean) {
    val (label, color) = if (passed) {
        "PASS" to AegisGreen
    } else {
        severity.name to severityColor(severity, false)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Stat strip — row of 3 or 4 numbers ───────────────────────────────────────
@Composable
fun StatStrip(stats: List<Triple<String, String, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { (value, label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, color: Color = AegisGreen, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
    }
}

// ── Aegis card wrapper ────────────────────────────────────────────────────────
@Composable
fun AegisCard(
    modifier: Modifier = Modifier,
    accentColor: Color = BgCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.5f), BgCardBorder))
        )
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

// ── Scanning pulse animation ──────────────────────────────────────────────────
@Composable
fun ScanningIndicator(label: String = "Scanning…", color: Color = AegisGreen) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = color.copy(alpha = alpha),
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(18.dp)
        )
        Text(label, color = color.copy(alpha = alpha), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Text(title, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AegisGreen),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text(actionLabel, color = BgAmoled, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
fun severityColor(severity: Severity, passed: Boolean): Color = if (passed) AegisGreen else when (severity) {
    Severity.CRITICAL -> AegisRed
    Severity.HIGH     -> AegisOrange
    Severity.MEDIUM   -> AegisAmber
    Severity.LOW      -> AegisBlue
    Severity.INFO     -> TextSecondary
}

fun severityIcon(severity: Severity) = when (severity) {
    Severity.CRITICAL, Severity.HIGH -> Icons.Default.Error
    Severity.MEDIUM                  -> Icons.Default.Warning
    else                             -> Icons.Default.Info
}
