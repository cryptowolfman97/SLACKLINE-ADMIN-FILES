package com.shvertex.casinotoolspro.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Card ─────────────────────────────────────────────────────────────────────
@Composable
fun CTPCard(
    modifier: Modifier = Modifier,
    accentColor: Color = CTPColors.Green,
    showAccent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CTPColors.Card)
            .then(
                if (showAccent) Modifier.drawBehind {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (showAccent) 16.dp else 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            content = content
        )
    }
}

// ── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, color: Color = CTPColors.TextMuted) {
    Text(
        text = title,
        style = CTPType.LabelLarge,
        color = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ── Primary Button ───────────────────────────────────────────────────────────
@Composable
fun CTPButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CTPColors.Green,
    textColor: Color = CTPColors.Black,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor,
            disabledContainerColor = CTPColors.Border,
            disabledContentColor = CTPColors.TextMuted
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(46.dp)
    ) {
        Text(
            text = text,
            style = CTPType.LabelLarge,
            letterSpacing = 1.sp
        )
    }
}

// ── Secondary/Outline Button ─────────────────────────────────────────────────
@Composable
fun CTPOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CTPColors.TextMuted
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.linearGradient(listOf(color.copy(0.5f), color.copy(0.5f)))
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(46.dp)
    ) {
        Text(text = text, style = CTPType.LabelLarge)
    }
}

// ── Danger Button ────────────────────────────────────────────────────────────
@Composable
fun CTPDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CTPButton(
        text = text,
        onClick = onClick,
        color = CTPColors.RedDim,
        textColor = CTPColors.TextPrimary,
        modifier = modifier
    )
}

// ── Input Field ──────────────────────────────────────────────────────────────
@Composable
fun CTPInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = CTPType.LabelMedium,
            color = CTPColors.TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = {
                Text(placeholder, color = CTPColors.TextMuted, style = CTPType.BodyMedium)
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor       = CTPColors.TextPrimary,
                unfocusedTextColor     = CTPColors.TextPrimary,
                focusedBorderColor     = CTPColors.Green,
                unfocusedBorderColor   = CTPColors.Border,
                cursorColor            = CTPColors.Green,
                focusedContainerColor  = CTPColors.Card,
                unfocusedContainerColor = CTPColors.Card,
            ),
            textStyle = CTPType.Mono,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

// ── Result Row ───────────────────────────────────────────────────────────────
@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = CTPColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = CTPType.BodyMedium,
            color = CTPColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = CTPType.Mono,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

// ── Stat Chip ────────────────────────────────────────────────────────────────
@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color = CTPColors.Green,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CTPColors.CardElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = CTPType.MonoLarge.copy(fontSize = 16.sp),
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = CTPType.LabelMedium,
            color = CTPColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ── Section Divider ──────────────────────────────────────────────────────────
@Composable
fun CTPDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = CTPColors.Divider,
        thickness = 1.dp
    )
}

// ── Loading Overlay ──────────────────────────────────────────────────────────
@Composable
fun LoadingOverlay(message: String = "Running simulation...") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = CTPColors.Green)
            Text(message, style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
        }
    }
}

// ── Progress Bar ─────────────────────────────────────────────────────────────
@Composable
fun CTPProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CTPColors.Green
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = color,
        trackColor = CTPColors.Divider
    )
}

// ── Screen Header ────────────────────────────────────────────────────────────
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String = "",
    accentColor: Color = CTPColors.Green,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CTPColors.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CTPColors.Card)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = accentColor, style = CTPType.HeadlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = CTPType.HeadlineLarge,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = CTPType.LabelMedium,
                        color = CTPColors.TextMuted
                    )
                }
            }
        }
        CTPDivider()
    }
}

// ── Verdict Badge ────────────────────────────────────────────────────────────
@Composable
fun VerdictBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = CTPType.LabelLarge,
            color = color
        )
    }
}

// ── Animated Glow Number ─────────────────────────────────────────────────────
@Composable
fun GlowNumber(
    value: String,
    color: Color = CTPColors.Green,
    style: androidx.compose.ui.text.TextStyle = CTPType.DisplayMedium
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Text(
        text = value,
        style = style,
        color = color.copy(alpha = alpha)
    )
}
