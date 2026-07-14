package com.shvertex.simplibudgetrevamped.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.shvertex.simplibudgetrevamped.R
import com.shvertex.simplibudgetrevamped.data.formatAmount
import com.shvertex.simplibudgetrevamped.ui.theme.*

// ── Premium Fluid Background Animations ───────────────────────────────────────
// Layered mathematical vector paths designed to stay dynamic at low opacities.

@Composable
fun HomeBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "hbg")
    val phase1 by inf.animateFloat(0f, 2f * Math.PI.toFloat(), infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "p1")
    val phase2 by inf.animateFloat(0f, 2f * Math.PI.toFloat(), infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "p2")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 1.0f
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            
            // Layer 1: Smooth flowing Neon Cyan wave across the header
            val path1 = Path().apply {
                moveTo(0f, h * 0.16f)
                for (x in 0..w.toInt() step 15) {
                    val xF = x.toFloat()
                    val y = h * 0.16f + 35f * kotlin.math.sin(xF * 0.005f + phase1)
                    lineTo(xF, y)
                }
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }
            drawPath(path1, brush = Brush.verticalGradient(listOf(Color(0xFF00E5A0).copy(alpha = 0.07f), Color.Transparent)))

            // Layer 2: Harmonious intersecting Blue wave
            val path2 = Path().apply {
                moveTo(0f, h * 0.13f)
                for (x in 0..w.toInt() step 15) {
                    val xF = x.toFloat()
                    val y = h * 0.13f + 30f * kotlin.math.cos(xF * 0.006f + phase2)
                    lineTo(xF, y)
                }
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }
            drawPath(path2, brush = Brush.verticalGradient(listOf(Color(0xFF3D8EFF).copy(alpha = 0.05f), Color.Transparent)))
            
            // Deep ambient purple radial glow spot mapping the action cards zone
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD46EFF).copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.18f),
                    radius = w * 0.35f
                ),
                radius = w * 0.35f,
                center = Offset(w * 0.75f, h * 0.18f)
            )
        }
    }
}

@Composable
fun BudgetBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "bbg")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "ang")
    val pulse by inf.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "pls")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.25f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            val cx = w * 0.85f; val cy = h * 0.12f
            
            // Ambient radial background glow behind the tracking geometry
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5A0).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.4f * pulse
                ),
                radius = w * 0.4f * pulse,
                center = Offset(cx, cy)
            )
            
            // Rotating concentric fine vector lines tracking allocation status
            rotate(angle, Offset(cx, cy)) {
                val baseRadius = w * 0.2f * pulse
                drawCircle(Color(0xFF00E5A0), radius = baseRadius, center = Offset(cx, cy), alpha = 0.15f, style = Stroke(1.5f))
                drawCircle(Color(0xFF00E5A0), radius = baseRadius * 0.6f, center = Offset(cx, cy), alpha = 0.08f)
                
                for (i in 0..5) {
                    drawArc(
                        color = Color(0xFF00E5A0).copy(alpha = 0.06f),
                        startAngle = i * 60f,
                        sweepAngle = 40f,
                        useCenter = false,
                        topLeft = Offset(cx - baseRadius * 1.3f, cy - baseRadius * 1.3f),
                        size = Size(baseRadius * 2.6f, baseRadius * 2.6f),
                        style = Stroke(2f)
                    )
                }
            }
            
            // Soft balancing stabilizing blue orb on the opposite side
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3D8EFF).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.18f),
                    radius = w * 0.25f
                ),
                radius = w * 0.25f,
                center = Offset(w * 0.15f, h * 0.18f)
            )
        }
    }
}

@Composable
fun AccountsBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "abg")
    val shift by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "sft")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            val cardW = w * 0.52f; val cardH = h * 0.11f
            val offsetFactor = kotlin.math.sin(shift * 2 * Math.PI.toFloat()) * 15f
            
            val brush1 = Brush.linearGradient(listOf(Color(0xFF00E5A0).copy(alpha = 0.16f), Color.Transparent))
            drawRoundRect(
                brush = brush1,
                topLeft = Offset(w * 0.12f, h * 0.06f + offsetFactor),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(16f),
                style = Stroke(2f)
            )
            
            val brush2 = Brush.linearGradient(listOf(Color(0xFF3D8EFF).copy(alpha = 0.12f), Color.Transparent))
            drawRoundRect(
                brush = brush2,
                topLeft = Offset(w * 0.24f, h * 0.11f + offsetFactor * 0.6f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(16f),
                style = Stroke(2f)
            )
            
            val brush3 = Brush.linearGradient(listOf(Color(0xFFD46EFF).copy(alpha = 0.10f), Color.Transparent))
            drawRoundRect(
                brush = brush3,
                topLeft = Offset(w * 0.36f, h * 0.16f + offsetFactor * 0.3f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(16f),
                style = Stroke(2f)
            )
        }
    }
}

@Composable
fun GoalsBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "gbg")
    val phase by inf.animateFloat(0f, 2 * Math.PI.toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "phs")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            
            val hillPath1 = Path().apply {
                moveTo(0f, h * 0.24f)
                for (x in 0..w.toInt() step 20) {
                    val xF = x.toFloat()
                    val y = h * 0.24f + 25f * kotlin.math.sin(xF * 0.004f + phase)
                    lineTo(xF, y)
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath1, brush = Brush.verticalGradient(listOf(Color(0xFFFFB300).copy(alpha = 0.06f), Color.Transparent)))
            
            val hillPath2 = Path().apply {
                moveTo(0f, h * 0.20f)
                for (x in 0..w.toInt() step 20) {
                    val xF = x.toFloat()
                    val y = h * 0.20f + 20f * kotlin.math.cos(xF * 0.005f + phase + 1f)
                    lineTo(xF, y)
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath2, brush = Brush.verticalGradient(listOf(Color(0xFFFF6B35).copy(alpha = 0.04f), Color.Transparent)))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFB300).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.12f),
                    radius = w * 0.25f
                ),
                radius = w * 0.25f,
                center = Offset(w * 0.8f, h * 0.12f)
            )
        }
    }
}

@Composable
fun BillsBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "blbg")
    val phase by inf.animateFloat(0f, 2 * Math.PI.toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "phs")
    val alphaPulse by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pls")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            
            val pulsePath = Path().apply {
                moveTo(0f, h * 0.12f)
                for (x in 0..w.toInt() step 10) {
                    val xF = x.toFloat()
                    val factor = if (xF > w * 0.5f && xF < w * 0.8f) {
                        kotlin.math.sin((xF - w * 0.5f) / (w * 0.3f) * Math.PI.toFloat()) * 40f
                    } else {
                        8f
                    }
                    val y = h * 0.12f + factor * kotlin.math.sin(xF * 0.03f + phase)
                    lineTo(xF, y)
                }
            }
            drawPath(pulsePath, color = Color(0xFFFF4757).copy(alpha = 0.08f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            
            drawCircle(Color(0xFFFF4757), radius = 6f, center = Offset(w * 0.68f, h * 0.12f), alpha = alphaPulse)
            drawCircle(Color(0xFFFF4757).copy(alpha = 0.12f), radius = 16f, center = Offset(w * 0.68f, h * 0.12f))
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3D8EFF).copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.08f),
                    radius = w * 0.25f
                ),
                radius = w * 0.25f,
                center = Offset(w * 0.15f, h * 0.08f)
            )
        }
    }
}

@Composable
fun DebtsBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dbg")
    val phase by inf.animateFloat(0f, 2 * Math.PI.toFloat(), infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "phs")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            
            val path = Path().apply {
                moveTo(0f, h * 0.05f)
                for (x in 0..w.toInt() step 15) {
                    val xF = x.toFloat()
                    val baseSlant = (1f - (xF / w)) * (h * 0.15f) + (h * 0.05f)
                    val y = baseSlant + 15f * kotlin.math.sin(xF * 0.006f + phase)
                    lineTo(xF, y)
                }
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }
            drawPath(path, brush = Brush.verticalGradient(listOf(Color(0xFFFF4757).copy(alpha = 0.06f), Color.Transparent)))
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF4757).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.15f),
                    radius = w * 0.3f
                ),
                radius = w * 0.3f,
                center = Offset(w * 0.2f, h * 0.15f)
            )
        }
    }
}

@Composable
fun ReportsBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "rbg")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "t")
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.budget_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.Darken)
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            val barColors = listOf(Color(0xFF00E5A0), Color(0xFF3D8EFF), Color(0xFFFFB300), Color(0xFFD46EFF))
            val heights = listOf(0.12f, 0.19f, 0.10f, 0.16f)
            
            val areaPath = Path().apply {
                moveTo(0f, h * 0.25f)
                barColors.forEachIndexed { i, _ ->
                    val x = w * (0.15f + i * 0.22f)
                    val bh = h * heights[i] * (0.75f + 0.25f * t)
                    val y = h * 0.25f - bh
                    if (i == 0) lineTo(x, y) else cubicTo(w * (0.15f + (i - 0.5f) * 0.22f), h * 0.25f - (h * heights[i - 1] * (0.75f + 0.25f * t)), x - 20f, y, x, y)
                }
                lineTo(w * 0.9f, h * 0.25f)
                close()
            }
            drawPath(areaPath, brush = Brush.verticalGradient(listOf(Color(0xFF3D8EFF).copy(alpha = 0.08f), Color.Transparent)))
            
            barColors.forEachIndexed { i, col ->
                val x = w * (0.15f + i * 0.22f)
                val bh = h * heights[i] * (0.75f + 0.25f * t)
                val y = h * 0.25f - bh
                drawCircle(col, radius = 4f, center = Offset(x, y), alpha = 0.25f)
                drawCircle(col.copy(alpha = 0.06f), radius = 10f, center = Offset(x, y))
            }
        }
    }
}

// ── Clean Card ────────────────────────────────────────────────────────────────

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    accentColor: Color = AmoledBorder,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) { Column(content = content) }
}

// ── Gradient Card ─────────────────────────────────────────────────────────────

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = GradientTeal,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(Color(0xFF0D1810), Color(0xFF080D18))))
            .border(1.dp, Brush.linearGradient(listOf(
                Color(0xFF00E5A0).copy(alpha = 0.3f), Color(0xFF3D8EFF).copy(alpha = 0.15f)
            )), RoundedCornerShape(cornerRadius)),
        content = content
    )
}

// ── Animated Amounts ──────────────────────────────────────────────────────────

@Composable
fun AnimatedAmount(
    target: Double, currency: String, color: Color = Accent,
    fontSize: Int = 22, fontWeight: FontWeight = FontWeight.Bold
) {
    val v by animateFloatAsState(target.toFloat(), tween(700, easing = FastOutSlowInEasing), label = "amt")
    Text(formatAmount(v.toDouble(), currency), color = color, fontSize = fontSize.sp, fontWeight = fontWeight)
}

@Composable
fun GradientAmountText(target: Double, currency: String, brush: Brush = GradientTeal, fontSize: Int = 28) {
    val v by animateFloatAsState(target.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "gamt")
    Text(formatAmount(v.toDouble(), currency), style = androidx.compose.ui.text.TextStyle(
        brush = brush, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold))
}

// ── Progress Bars ─────────────────────────────────────────────────────────────

@Composable
fun AnimatedBar(progress: Float, color: Color = Accent, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f),
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "bar")
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(AmoledBorder)) {
        if (anim > 0f) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(height)).background(color))
    }
}

@Composable
fun GradientBar(progress: Float, brush: Brush = GradientTeal, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f),
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow), label = "gbar")
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(AmoledBorder)) {
        if (anim > 0f) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(height)).background(brush))
    }
}

// ── Donut Chart ───────────────────────────────────────────────────────────────

@Composable
fun DonutChart(segments: List<Triple<String, Double, Color>>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    val anim by animateFloatAsState(1f, tween(900, easing = FastOutSlowInEasing), label = "donut")
    androidx.compose.foundation.Canvas(modifier = modifier.aspectRatio(1f)) {
        val sw = size.minDimension * 0.13f
        var startAngle = -90f
        drawArc(AmoledBorder, 0f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round))
        segments.forEach { (_, value, color) ->
            val sweep = (value.toFloat() / total) * 360f * anim
            if (sweep > 0.5f) drawArc(color, startAngle, sweep - 1.5f, false, style = Stroke(sw, cap = StrokeCap.Round))
            startAngle += (value.toFloat() / total) * 360f
        }
    }
}

// ── Summary Card ──────────────────────────────────────────────────────────────

@Composable
fun SummaryCard(label: String, amount: Double, currency: String, color: Color = Accent,
                subtitle: String = "", modifier: Modifier = Modifier, brush: Brush? = null) {
    AppCard(modifier = modifier, accentColor = color) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            if (brush != null) GradientAmountText(amount, currency, brush, 19)
            else AnimatedAmount(amount, currency, color, 19)
            if (subtitle.isNotEmpty()) Text(subtitle, color = AmoledSubtext, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Month Chip Row ────────────────────────────────────────────────────────────

@Composable
fun MonthChipRow(months: List<String>, selected: String, onSelect: (String) -> Unit, labelOf: (String) -> String) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        months.forEach { ym ->
            val sel = ym == selected
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .then(if (sel) Modifier.background(GradientTeal) else Modifier.background(AmoledNavBtn))
                .clickable { onSelect(ym) }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                Text(labelOf(ym), color = if (sel) AmoledBg else AmoledSubtext,
                    fontSize = 12.sp, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal)
            }
        }
    }
}

// ── Action Buttons ────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                  color: Color = Accent, contentColor: Color = AmoledBg, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor), enabled = enabled) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AmoledSubtext) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AmoledBorder)) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ── Input Fields & Dropdowns ──────────────────────────────────────────────────

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String,
                 modifier: Modifier = Modifier,
                 keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                 singleLine: Boolean = true, maxLines: Int = 1, trailingIcon: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, color = AmoledSubtext, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, maxLines = maxLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AmoledText, unfocusedTextColor = AmoledText,
            focusedContainerColor = AmoledInputBg, unfocusedContainerColor = AmoledInputBg,
            focusedBorderColor = Accent, unfocusedBorderColor = AmoledBorder, cursorColor = Accent),
        shape = RoundedCornerShape(12.dp), trailingIcon = trailingIcon)
}

@Composable
fun AppDropdown(selected: String, options: List<String>, onSelect: (String) -> Unit,
                label: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(value = selected, onValueChange = {}, label = { Text(label, color = AmoledSubtext, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }, readOnly = true, enabled = false,
            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = AmoledText, disabledContainerColor = AmoledInputBg,
                disabledBorderColor = AmoledBorder, disabledLabelColor = AmoledSubtext),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = AmoledSubtext) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(AmoledCard)) {
            options.forEach { DropdownMenuItem(text = { Text(it, color = AmoledText) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}

// ── Transaction Rows ──────────────────────────────────────────────────────────

@Composable
fun TransactionRow(name: String, category: String, date: String, amount: Double, type: String,
                   currency: String, onEdit: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val isExpense = type == "expense"
    val amtColor = if (isExpense) Danger else GreenPos
    AppCard(accentColor = amtColor, cornerRadius = 14.dp) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(amtColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(if (isExpense) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    null, tint = amtColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$category  •  $date", color = AmoledSubtext, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (isExpense) "-" else "+"}${formatAmount(amount, currency)}", color = amtColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(2.dp)) { Text("Edit", color = Accent2, fontSize = 11.sp) }
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(2.dp)) { Text("Del", color = Danger, fontSize = 11.sp) }
                }
            }
        }
    }
}

// ── Floating Nav Pill ─────────────────────────────────────────────────────────

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun BottomNavPill(items: List<NavItem>, currentRoute: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        .clip(RoundedCornerShape(32.dp))
        .background(Color(0xFF111111))
        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(32.dp))) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val scale by animateFloatAsState(if (selected) 1f else 0.95f, spring(), label = "ns")
                Box(modifier = Modifier
                    .then(Modifier.scale(scale))
                    .clip(RoundedCornerShape(24.dp))
                    .then(if (selected) Modifier.background(GradientTeal) else Modifier.background(Color.Transparent))
                    .clickable { onNavigate(item.route) }
                    .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    if (selected) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(item.icon, item.label, tint = AmoledBg, modifier = Modifier.size(17.dp))
                        Text(item.label, color = AmoledBg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    } else Icon(item.icon, item.label, tint = AmoledSubtext, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Dialogs & Headers ─────────────────────────────────────────────────────────

@Composable
fun ExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Exit SimpliBudget?", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Text("Your data is saved automatically.", color = AmoledSubtext) },
        confirmButton = { PrimaryButton("Exit", onConfirm, color = Danger, contentColor = AmoledText, modifier = Modifier.width(110.dp)) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.width(110.dp)) })
}

@Composable
fun ScreenHeader(title: String, subtitle: String = "", onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().background(AmoledSurface).padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Accent) }
        else Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = androidx.compose.ui.text.TextStyle(brush = GradientTeal, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold))
            if (subtitle.isNotEmpty()) Text(subtitle, color = AmoledSubtext, fontSize = 12.sp)
        }
        action?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = androidx.compose.ui.text.TextStyle(brush = GradientTeal, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

// ── Native High-End Empty State (Zero-Dependency) ────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "empty_state")
    val pulse by inf.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )
    val rotation by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "ring_rotate"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            // Elegant geometric tracking ring
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                rotate(rotation) {
                    drawCircle(
                        color = Color(0xFF222222),
                        radius = size.minDimension / 2.4f,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                        )
                    )
                }
            }
            // Soft pulsating core icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Accent.copy(alpha = 0.35f),
                modifier = Modifier.size(40.dp).scale(pulse)
            )
        }
        Text(title, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = AmoledSubtext, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// ── Fine Details ──────────────────────────────────────────────────────────────

@Composable
fun ColorDot(colorHex: String, size: Dp = 10.dp) {
    val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Accent }
    Box(modifier = Modifier.size(size).clip(CircleShape).background(c))
}

@Composable
fun AddChip(onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GradientTeal)
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text("+ Add", color = AmoledBg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
