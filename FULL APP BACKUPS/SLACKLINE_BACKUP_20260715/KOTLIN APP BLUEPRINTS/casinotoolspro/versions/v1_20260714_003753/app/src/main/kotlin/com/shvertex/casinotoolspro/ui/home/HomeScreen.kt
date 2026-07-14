package com.shvertex.casinotoolspro.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.AppStrings
import com.shvertex.casinotoolspro.LocalPipMode
import com.shvertex.casinotoolspro.LocalPresentationMode
import com.shvertex.casinotoolspro.MainActivity
import com.shvertex.casinotoolspro.core.BankrollManager
import com.shvertex.casinotoolspro.license.*
import com.shvertex.casinotoolspro.navigation.Routes
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.*

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HomeViewModel(private val mgr: BankrollManager) : ViewModel() {
    val profit = mgr.sessionProfit.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0.0
    )
    val startTime = mgr.startTime.stateIn(
        viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis()
    )

    fun updateProfit(delta: Double) = viewModelScope.launch {
        val current = mgr.sessionProfit.first()
        mgr.updateProfit(current + delta)
    }
    fun reset() = viewModelScope.launch { mgr.resetSession() }
}

data class MenuTile(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val color: Color
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context  = LocalContext.current
    val mgr      = remember { BankrollManager.getInstance(context) }
    val vm: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return HomeViewModel(mgr) as T
            }
        }
    )

    // Master Presentation Toggle State
    val presentationModeState = LocalPresentationMode.current
    val isPresentation = presentationModeState.value
    val isPipMode = LocalPipMode.current

    val profit    by vm.profit.collectAsState()
    val startTime by vm.startTime.collectAsState()

    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            elapsed = System.currentTimeMillis() - startTime
            kotlinx.coroutines.delay(1000L)
        }
    }

    var profitInput by remember { mutableStateOf("") }
    var inputError  by remember { mutableStateOf(false) }
    
    // Dialog States
    var showDisclaimer by remember { mutableStateOf(false) }
    var showLicenseInfo by remember { mutableStateOf(false) }
    var deniedRoute by remember { mutableStateOf<String?>(null) }

    if (isPipMode) {
        PipDashboard(profit = profit, elapsed = elapsed, isPresentation = isPresentation)
    } else {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(if (isPresentation) Color(0xFF0F172A) else CTPColors.Black)
            ) {
                // Switch background based on mode
                if (isPresentation) {
                    StrategyBackgroundAnimation()
                } else {
                    CasinoBackgroundAnimation()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    // ── Header band: title + profit + timer + stats ────────────────
                    NewHeader(profit = profit, elapsed = elapsed, isPresentation = isPresentation)

                    Spacer(Modifier.height(6.dp))

                    // ── Input row ─────────────────────────────────────────────────
                    CompactInputBar(
                        input         = profitInput,
                        hasError      = inputError,
                        isPresentation = isPresentation,
                        onInputChange = { profitInput = it; inputError = false },
                        onUpdate = {
                            val v = profitInput.trim().toDoubleOrNull()
                            if (v != null) { vm.updateProfit(v); profitInput = ""; inputError = false }
                            else           { inputError = true }
                        },
                        onReset = { vm.reset(); profitInput = ""; inputError = false }
                    )

                    Spacer(Modifier.height(6.dp))

                    // ── Module grid scrollable ─────────────────────────────────────
                    NewModuleGrid(
                        modifier         = Modifier.weight(1f),
                        isPresentation   = isPresentation,
                        onNavigate       = { route ->
                            if (CTPAccess.hasAccess(route)) onNavigate(route)
                            else deniedRoute = route
                        },
                        onToggleMode     = { 
                            if (CTPAccess.canUsePresentation()) presentationModeState.value = !isPresentation
                            else deniedRoute = CTPAccess.PRESENTATION_MODE_KEY
                        },
                        onShowDisclaimer = { showDisclaimer = true },
                        onShowLicense    = { showLicenseInfo = true },
                        onEnterPip       = { 
                            if (CTPAccess.hasAccess(CTPAccess.PIP_MODE_KEY)) {
                                (context as? MainActivity)?.enterPipMode()
                            } else {
                                deniedRoute = CTPAccess.PIP_MODE_KEY
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    
    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            containerColor = CTPColors.Card,
            title = {
                Text(
                    text = AppStrings.disclaimerTitle(isPresentation),
                    style = CTPType.HeadlineLarge,
                    color = CTPColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = AppStrings.disclaimerText(isPresentation),
                    style = CTPType.BodyMedium,
                    color = CTPColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDisclaimer = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CTPColors.Green)
                ) {
                    Text("Understood", color = CTPColors.Black)
                }
            }
        )
    }

    if (showLicenseInfo) {
        LicenseInfoDialog(context = context, onDismiss = { showLicenseInfo = false })
    }

    deniedRoute?.let { route ->
        AccessDeniedDialog(route = route, onDismiss = { deniedRoute = null })
    }
}

// ── PiP Dashboard (Battery Friendly / Static) ─────────────────────────────────
@Composable
fun PipDashboard(profit: Double, elapsed: Long, isPresentation: Boolean) {
    val h = TimeUnit.MILLISECONDS.toHours(elapsed)
    val m = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
    val timeStr = String.format("%02d:%02d:%02d", h, m, s)

    val primaryColor = if (isPresentation) Color(0xFF38BDF8) else CTPColors.Green
    val profitColor = when {
        profit > 0 -> CTPColors.Green
        profit < 0 -> CTPColors.Red
        else       -> CTPColors.TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeStr,
                color = CTPColors.TextSecondary,
                style = CTPType.MonoLarge.copy(fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${if (profit >= 0) "+" else ""}${"%.4f".format(profit)}",
                color = profitColor,
                style = CTPType.MonoLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPresentation) "PRO+ ACTIVE" else "CTP-8E170416",
                color = primaryColor.copy(alpha = 0.7f),
                style = CTPType.LabelMedium.copy(fontSize = 10.sp)
            )
        }
    }
}

// ── Strategy Background Animation (Clean / Scientific) ────────────────────────
@Composable
fun StrategyBackgroundAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "strategy_bg")

    val drift by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label         = "drift"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 0.6f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label         = "pulse"
    )

    val gridColor = Color(0xFF38BDF8) // Soft Blue

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw a slow moving grid
        val gridSize = 80f
        val xOffset = (drift * gridSize) % gridSize
        val yOffset = (drift * gridSize * 0.5f) % gridSize

        for (x in 0..(w / gridSize + 1).toInt()) {
            drawLine(
                color = gridColor.copy(alpha = 0.05f),
                start = Offset(x * gridSize - xOffset, 0f),
                end = Offset(x * gridSize - xOffset, h),
                strokeWidth = 1f
            )
        }
        for (y in 0..(h / gridSize + 1).toInt()) {
            drawLine(
                color = gridColor.copy(alpha = 0.05f),
                start = Offset(0f, y * gridSize - yOffset),
                end = Offset(w, y * gridSize - yOffset),
                strokeWidth = 1f
            )
        }

        // Draw connecting nodes (Data simulation)
        val nodes = listOf(
            Offset(w * 0.2f, h * 0.15f), Offset(w * 0.7f, h * 0.1f),
            Offset(w * 0.85f, h * 0.35f), Offset(w * 0.3f, h * 0.4f),
            Offset(w * 0.15f, h * 0.65f), Offset(w * 0.6f, h * 0.7f),
            Offset(w * 0.8f, h * 0.85f), Offset(w * 0.4f, h * 0.9f)
        )

        // Draw lines between some nodes
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[0], nodes[3], strokeWidth = 2f)
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[1], nodes[2], strokeWidth = 2f)
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[3], nodes[4], strokeWidth = 2f)
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[3], nodes[5], strokeWidth = 2f)
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[5], nodes[6], strokeWidth = 2f)
        drawLine(gridColor.copy(alpha = pulse * 0.4f), nodes[5], nodes[7], strokeWidth = 2f)

        // Draw nodes
        nodes.forEachIndexed { index, node ->
            val nodePulse = (pulse + (index * 0.1f)) % 1f
            drawCircle(gridColor.copy(alpha = nodePulse * 0.2f), radius = 15f, center = node)
            drawCircle(gridColor.copy(alpha = 0.8f), radius = 4f, center = node)
        }
    }
}


// ── Casino Background Animation (Original) ────────────────────────────────────
@Composable
fun CasinoBackgroundAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "casino_bg")
    val wheelRotation by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label         = "wheel_rot"
    )
    val ballAngle by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label         = "ball_angle"
    )
    val drift by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label         = "drift"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.35f, targetValue   = 0.75f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label         = "glow"
    )
    val sparkle by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label         = "sparkle"
    )

    val green  = Color(0xFF00FF88)
    val gold   = Color(0xFFFFCC00)
    val red    = Color(0xFFFF3355)
    val white  = Color(0xFFFFFFFF)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val particlePositions = listOf(
            Offset(w * 0.10f, h * 0.08f), Offset(w * 0.28f, h * 0.05f),
            Offset(w * 0.55f, h * 0.10f), Offset(w * 0.78f, h * 0.07f),
            Offset(w * 0.92f, h * 0.18f), Offset(w * 0.05f, h * 0.30f),
            Offset(w * 0.95f, h * 0.38f), Offset(w * 0.03f, h * 0.55f),
            Offset(w * 0.97f, h * 0.60f), Offset(w * 0.12f, h * 0.72f),
            Offset(w * 0.88f, h * 0.75f), Offset(w * 0.40f, h * 0.95f),
            Offset(w * 0.65f, h * 0.92f), Offset(w * 0.20f, h * 0.90f),
        )
        particlePositions.forEachIndexed { i, base ->
            val twinklePhase = (sparkle + i * 0.07f) % 1f
            val alpha = (sin(twinklePhase * 2 * PI.toFloat()) * 0.5f + 0.5f) * 0.55f + 0.15f
            val radius = 2.5f + (i % 3) * 1.5f
            val color = when (i % 3) { 0 -> green; 1 -> gold; else -> white }
            drawCircle(color = color.copy(alpha = alpha * 0.3f), radius = radius * 2.8f, center = base)
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = base)
        }

        val wheelCx = w * 0.82f
        val wheelCy = h * 0.16f
        val wheelR  = w * 0.22f

        drawCircle(
            brush  = Brush.radialGradient(listOf(gold.copy(alpha = glow * 0.4f), Color.Transparent), Offset(wheelCx, wheelCy), wheelR * 1.5f),
            radius = wheelR * 1.5f, center = Offset(wheelCx, wheelCy)
        )
        drawCircle(color = gold.copy(alpha = 0.70f), radius = wheelR, center = Offset(wheelCx, wheelCy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

        val segCount = 18
        val segSweep = 360f / segCount
        for (seg in 0 until segCount) {
            val startAngle = seg * segSweep + wheelRotation
            val segColor = when {
                seg == 0 -> green.copy(alpha = 0.65f)
                seg % 2 == 0 -> red.copy(alpha = 0.50f)
                else -> Color(0xFF222222).copy(alpha = 0.80f)
            }
            drawArc(
                color = segColor, startAngle = startAngle, sweepAngle = segSweep - 1.5f, useCenter = true,
                topLeft = Offset(wheelCx - wheelR * 0.85f, wheelCy - wheelR * 0.85f), size = Size(wheelR * 1.70f, wheelR * 1.70f)
            )
        }

        for (seg in 0 until segCount) {
            val angle = Math.toRadians((seg * segSweep + wheelRotation).toDouble())
            drawLine(
                color = gold.copy(alpha = 0.40f), start = Offset(wheelCx, wheelCy),
                end = Offset(wheelCx + cos(angle).toFloat() * wheelR * 0.85f, wheelCy + sin(angle).toFloat() * wheelR * 0.85f),
                strokeWidth = 1.2f
            )
        }

        drawCircle(color = gold.copy(alpha = 0.55f), radius = wheelR * 0.14f, center = Offset(wheelCx, wheelCy))
        drawCircle(color = gold.copy(alpha = 0.85f), radius = wheelR * 0.14f, center = Offset(wheelCx, wheelCy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        val ballRad    = wheelR * 0.93f
        val ballRadian = Math.toRadians(ballAngle.toDouble())
        val ballX      = wheelCx + cos(ballRadian).toFloat() * ballRad
        val ballY      = wheelCy + sin(ballRadian).toFloat() * ballRad
        drawCircle(color = white.copy(alpha = 0.35f), radius = 9f, center = Offset(ballX, ballY))
        drawCircle(color = white.copy(alpha = 0.90f), radius = 5f, center = Offset(ballX, ballY))

        data class CardDef(val bx: Float, val by: Float, val suit: String, val color: Color, val speed: Float, val phase: Float)
        val cards = listOf(
            CardDef(w * 0.08f, h * 0.42f, "♠", white,  1.0f, 0.00f),
            CardDef(w * 0.88f, h * 0.50f, "♥", red,    0.7f, 0.33f),
            CardDef(w * 0.05f, h * 0.76f, "♦", gold,   1.3f, 0.66f),
        )
        cards.forEach { card ->
            val bobPhase  = (drift * card.speed + card.phase) % 1f
            val yOffset   = sin(bobPhase * 2 * PI.toFloat()) * 14f
            val cx        = card.bx
            val cy        = card.by + yOffset
            val cw        = 38f
            val ch        = 54f
            val alpha     = 0.62f

            drawRoundRect(color = card.color.copy(alpha = alpha * 0.20f), topLeft = Offset(cx - cw / 2 - 4f, cy - ch / 2 - 4f), size = Size(cw + 8f, ch + 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            drawRoundRect(color = Color(0xFF1A1A2E).copy(alpha = alpha + 0.10f), topLeft = Offset(cx - cw / 2, cy - ch / 2), size = Size(cw, ch), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
            drawRoundRect(color = card.color.copy(alpha = alpha + 0.15f), topLeft = Offset(cx - cw / 2, cy - ch / 2), size = Size(cw, ch), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f))
            
            when (card.suit) {
                "♦" -> {
                    val path = Path().apply { moveTo(cx, cy - 10f); lineTo(cx + 8f, cy); lineTo(cx, cy + 10f); lineTo(cx - 8f, cy); close() }
                    drawPath(path, color = card.color.copy(alpha = alpha + 0.20f))
                }
                "♠" -> {
                    val path = Path().apply { moveTo(cx, cy + 10f); cubicTo(cx - 12f, cy + 2f, cx - 12f, cy - 8f, cx, cy - 2f); cubicTo(cx + 12f, cy - 8f, cx + 12f, cy + 2f, cx, cy + 10f); close() }
                    drawPath(path, color = card.color.copy(alpha = alpha + 0.20f))
                    drawLine(color = card.color.copy(alpha = alpha), start = Offset(cx, cy + 10f), end = Offset(cx, cy + 16f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                }
                else -> {
                    val path = Path().apply { moveTo(cx, cy + 10f); cubicTo(cx - 14f, cy - 2f, cx - 14f, cy - 14f, cx, cy - 6f); cubicTo(cx + 14f, cy - 14f, cx + 14f, cy - 2f, cx, cy + 10f); close() }
                    drawPath(path, color = card.color.copy(alpha = alpha + 0.20f))
                }
            }
        }

        data class DiceDef(val bx: Float, val by: Float, val pips: Int, val speed: Float, val phase: Float)
        val dice = listOf(
            DiceDef(w * 0.14f, h * 0.25f, 6, 0.6f, 0.00f),
            DiceDef(w * 0.80f, h * 0.68f, 3, 0.9f, 0.50f),
        )
        dice.forEach { die ->
            val bobPhase = (drift * die.speed + die.phase) % 1f
            val yOffset  = sin(bobPhase * 2 * PI.toFloat()) * 16f
            val cx       = die.bx
            val cy       = die.by + yOffset
            val r        = 26f
            val alpha    = 0.70f

            drawRoundRect(color = green.copy(alpha = alpha * 0.22f), topLeft = Offset(cx - r - 6f, cy - r - 6f), size = Size((r + 6f) * 2, (r + 6f) * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.35f))
            drawRoundRect(color = Color(0xFF0D1F0D).copy(alpha = 0.88f), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.28f))
            drawRoundRect(color = green.copy(alpha = alpha), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.28f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))

            val pipOffset = r * 0.45f
            val pipPositions = when (die.pips) {
                1 -> listOf(Offset(cx, cy))
                2 -> listOf(Offset(cx - pipOffset, cy - pipOffset), Offset(cx + pipOffset, cy + pipOffset))
                3 -> listOf(Offset(cx - pipOffset, cy - pipOffset), Offset(cx, cy), Offset(cx + pipOffset, cy + pipOffset))
                4 -> listOf(Offset(cx - pipOffset, cy - pipOffset), Offset(cx + pipOffset, cy - pipOffset), Offset(cx - pipOffset, cy + pipOffset), Offset(cx + pipOffset, cy + pipOffset))
                5 -> listOf(Offset(cx - pipOffset, cy - pipOffset), Offset(cx + pipOffset, cy - pipOffset), Offset(cx, cy), Offset(cx - pipOffset, cy + pipOffset), Offset(cx + pipOffset, cy + pipOffset))
                else -> listOf(Offset(cx - pipOffset, cy - pipOffset), Offset(cx + pipOffset, cy - pipOffset), Offset(cx - pipOffset, cy), Offset(cx + pipOffset, cy), Offset(cx - pipOffset, cy + pipOffset), Offset(cx + pipOffset, cy + pipOffset))
            }
            pipPositions.forEach { pip ->
                drawCircle(color = green.copy(alpha = alpha * 0.40f), radius = 5.5f,  center = pip)
                drawCircle(color = green.copy(alpha = alpha), radius = 3.5f,  center = pip)
            }
        }

        data class ChipDef(val cx: Float, val cy: Float, val color: Color)
        val chips = listOf(
            ChipDef(w * 0.50f, h * 0.06f, gold),
            ChipDef(w * 0.92f, h * 0.88f, red),
        )
        chips.forEach { chip ->
            val bobPhase = (drift * 0.8f + chips.indexOf(chip) * 0.5f) % 1f
            val yOff     = sin(bobPhase * 2 * PI.toFloat()) * 10f
            val cx       = chip.cx
            val cy       = chip.cy + yOff
            val stackH   = 4
            val chipR    = 20f
            val chipThk  = 7f

            repeat(stackH) { layer ->
                val ly     = cy + layer * chipThk
                val lAlpha = 0.70f - layer * 0.08f
                drawOval(color = chip.color.copy(alpha = lAlpha * 0.85f), topLeft = Offset(cx - chipR, ly - chipThk * 0.4f), size = Size(chipR * 2, chipThk * 0.8f))
                drawOval(color = chip.color.copy(alpha = lAlpha), topLeft = Offset(cx - chipR, ly - chipThk * 0.4f), size = Size(chipR * 2, chipThk * 0.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                drawCircle(color = chip.color.copy(alpha = lAlpha), radius = 4f, center = Offset(cx, ly))
            }
            drawCircle(brush = Brush.radialGradient(listOf(chip.color.copy(alpha = glow * 0.35f), Color.Transparent), Offset(cx, cy + stackH * chipThk), chipR * 2.2f), radius = chipR * 2.2f, center = Offset(cx, cy + stackH * chipThk))
        }

        val edgeAlpha = glow * 0.35f
        drawLine(color = green.copy(alpha = edgeAlpha), start = Offset(0f, 60f), end = Offset(0f, 0f), strokeWidth = 2.5f)
        drawLine(color = green.copy(alpha = edgeAlpha), start = Offset(0f, 0f), end = Offset(60f, 0f), strokeWidth = 2.5f)
        drawLine(color = gold.copy(alpha = edgeAlpha), start = Offset(w, h - 60f), end = Offset(w, h), strokeWidth = 2.5f)
        drawLine(color = gold.copy(alpha = edgeAlpha), start = Offset(w, h), end = Offset(w - 60f, h), strokeWidth = 2.5f)
    }
}

// ── New Header ────────────────────────────────────────────────────────────────

@Composable
fun NewHeader(profit: Double, elapsed: Long, isPresentation: Boolean) {
    val h = TimeUnit.MILLISECONDS.toHours(elapsed)
    val m = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
    val timeStr = String.format("%02d:%02d:%02d", h, m, s)

    val profitColor = when {
        profit > 0 -> CTPColors.Green
        profit < 0 -> CTPColors.Red
        else       -> CTPColors.TextMuted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label         = "dot_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text  = AppStrings.appName(isPresentation),
                style = CTPType.HeadlineLarge.copy(fontSize = 30.sp),
                color = if (isPresentation) Color(0xFF38BDF8) else CTPColors.Green
            )
            Text(
                text  = AppStrings.appSubtitle(isPresentation),
                style = CTPType.LabelMedium,
                color = CTPColors.TextSecondary
            )
            Text(
                text  = AppStrings.statusText(isPresentation),
                style = CTPType.LabelMedium.copy(fontSize = 9.sp),
                color = CTPColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(profitColor.copy(alpha = 0.08f))
                .border(1.dp, profitColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text          = AppStrings.profitLabel(isPresentation),
                    style         = CTPType.LabelMedium.copy(fontSize = 8.sp),
                    color         = CTPColors.TextMuted,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text  = "${if (profit >= 0) "+" else ""}${"%.4f".format(profit)}",
                    style = CTPType.MonoLarge.copy(fontSize = 18.sp),
                    color = profitColor
                )
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    NewStatsStrip()
}

// ── Stats Strip ───────────────────────────────────────────────────────────────

@Composable
fun NewStatsStrip() {
    val context = LocalContext.current
    val mgr     = remember { BankrollManager.getInstance(context) }
    val history = remember { mgr.loadHistory() }
    val wins    = history.count { it.amount > 0 }
    val losses  = history.count { it.amount < 0 }
    val winRate = if (history.isNotEmpty()) wins.toFloat() / history.size * 100f else 0f

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        StatChip(value = "${history.size}", label = "entries",  color = CTPColors.TextSecondary, modifier = Modifier.weight(1f))
        StatChip(value = "${"%.0f".format(winRate)}%", label = "win rate",
            color = if (winRate >= 50) CTPColors.Green else CTPColors.Red, modifier = Modifier.weight(1f))
        StatChip(value = "$wins",   label = "wins",   color = CTPColors.Green, modifier = Modifier.weight(1f))
        StatChip(value = "$losses", label = "losses", color = CTPColors.Red,   modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatChip(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(CTPColors.Card)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = CTPType.LabelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = color)
        Text(text = label, style = CTPType.LabelMedium.copy(fontSize = 9.sp), color = CTPColors.TextMuted)
    }
}

// ── Compact Input Bar ─────────────────────────────────────────────────────────

@Composable
fun CompactInputBar(
    input: String, hasError: Boolean, isPresentation: Boolean,
    onInputChange: (String) -> Unit, onUpdate: () -> Unit, onReset: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryColor = if (isPresentation) Color(0xFF38BDF8) else CTPColors.Green

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = onInputChange,
                singleLine    = true,
                isError       = hasError,
                placeholder   = { Text(AppStrings.inputPlaceholder(isPresentation), color = CTPColors.TextMuted, style = CTPType.BodyMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = CTPColors.TextPrimary, unfocusedTextColor = CTPColors.TextPrimary,
                    focusedBorderColor      = if (hasError) CTPColors.Red else primaryColor,
                    unfocusedBorderColor    = if (hasError) CTPColors.Red else CTPColors.Border,
                    cursorColor             = primaryColor,
                    focusedContainerColor   = CTPColors.Card, unfocusedContainerColor = CTPColors.Card,
                ),
                textStyle = CTPType.Mono, shape = RoundedCornerShape(8.dp),
                modifier  = Modifier.weight(1f).height(44.dp)
            )
            Button(
                onClick        = { keyboardController?.hide(); onUpdate() },
                colors         = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape          = RoundedCornerShape(8.dp),
                modifier       = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("UPDATE", style = CTPType.LabelLarge, color = CTPColors.Black)
            }
            IconButton(
                onClick  = { keyboardController?.hide(); onReset() },
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(CTPColors.Card)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = CTPColors.TextMuted)
            }
        }
        if (hasError) {
            Text(
                text = "Enter a valid number e.g. 5.50 or -12.00", style = CTPType.LabelMedium, color = CTPColors.Red,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

// ── New Module Grid & Bottom Buttons ──────────────────────────────────────────

@Composable
fun NewModuleGrid(
        modifier: Modifier = Modifier,
        isPresentation: Boolean,
        onNavigate: (String) -> Unit,
        onToggleMode: () -> Unit,
        onShowDisclaimer: () -> Unit,
        onShowLicense: () -> Unit,
        onEnterPip: () -> Unit
    ) {
        val scrollState = rememberScrollState()

    Column(
        modifier            = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val coreColor = if (isPresentation) Color(0xFF60A5FA) else CTPColors.Green
        // ── CORE TOOLS — 3 + 3 (2 rows) ──────────────────────────────────
        DividerLabel("CORE TOOLS", if (isPresentation) Color.Blue else CTPColors.Green)
        SymmetricRow(coreTiles(isPresentation).take(3), onNavigate)
        SymmetricRow(coreTiles(isPresentation).drop(3), onNavigate)


        val evoColor = if (isPresentation) Color(0xFF818CF8) else CTPColors.Dice
        DividerLabel("EVOLUTION LAB", evoColor)
        SymmetricRow(evolutionTiles(isPresentation).take(3), onNavigate)
        SymmetricRow(evolutionTiles(isPresentation).drop(3), onNavigate, fillEmpty = true)

        val analyticsColor = if (isPresentation) Color(0xFFA78BFA) else CTPColors.Keno
        val analyticsHeader = if (isPresentation) "PROBABILITY LAB" else "GAME ANALYTICS"
        DividerLabel(analyticsHeader, analyticsColor)
        SymmetricRow(analyticsTiles(isPresentation), onNavigate)

        val sportsColor = if (isPresentation) Color(0xFFF472B6) else CTPColors.Sports
        val sportsHeader = if (isPresentation) "MARKET MODELS" else "SPORTS"
        DividerLabel(sportsHeader, sportsColor)
        SymmetricRow(sportsTiles(isPresentation), onNavigate, tileStyle = TileStyle.Compact)

        val utilColor = if (isPresentation) Color(0xFF94A3B8) else CTPColors.TextMuted
        DividerLabel("UTILITIES", utilColor)
        SymmetricRow(utilityTiles(isPresentation), onNavigate, tileStyle = TileStyle.Compact)

        Spacer(Modifier.height(24.dp))

        // ── Bottom Action Buttons (2x2 Grid) ──
        
        // Row 1: Presentation | License Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onToggleMode() },
                modifier = Modifier.weight(1f).height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isPresentation) "Exit Presentation" else "Presentation Mode",
                    color = if (isPresentation) Color(0xFF60A5FA) else CTPColors.Green,
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Button(
                onClick = { onShowLicense() },
                modifier = Modifier.weight(1f).height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "License Info",
                    color = CTPColors.TextPrimary,
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Row 2: PiP Mode | About & Disclaimer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onEnterPip() },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "PiP Window",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Click to minimize",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Button(
                onClick = { onShowDisclaimer() },
                modifier = Modifier.weight(1f).height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF660000)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "About",
                    color = Color.White,
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
    }
}

// ── Tile definitions (Mapped for both modes) ──────────────────────────────────

fun coreTiles(isPres: Boolean) = listOf(
    MenuTile(if (isPres) "Logic\nTemplates" else "Strategies\nLibrary",  Routes.STRATEGIES,     Icons.Default.LibraryBooks,   if (isPres) Color(0xFF60A5FA) else CTPColors.Green),
    MenuTile(if (isPres) "RNG Variance\nEngine" else "Dice\nSimulator",      Routes.DICE_SIM,        Icons.Default.Casino,          if (isPres) Color(0xFF60A5FA) else CTPColors.Dice),
    MenuTile(if (isPres) "Threshold\nMultiplier" else "Dic/Limbo\nCalc",     Routes.DICE_CALC,       Icons.Default.Calculate,       if (isPres) Color(0xFF60A5FA) else CTPColors.Dice),
    MenuTile(if (isPres) "Monte Carlo\nSimulator" else "Monte\nCarlo",         Routes.MONTE_CARLO,     Icons.Default.BarChart,        if (isPres) Color(0xFF60A5FA) else CTPColors.Limbo),
    MenuTile(if (isPres) "Model Efficiency\nLab" else "Dice\nOptimizer",      Routes.DICE_OPTIMIZER,  Icons.Default.TrendingUp,      if (isPres) Color(0xFF60A5FA) else CTPColors.Limbo),
    MenuTile(if (isPres) "Sequence\nAutomator" else "Auto\nGenerator",      Routes.DICE_GENERATOR,  Icons.Default.AutoAwesome,     if (isPres) Color(0xFF60A5FA) else CTPColors.Keno),
)

fun evolutionTiles(isPres: Boolean) = listOf(
    MenuTile(if (isPres) "Logic\nBuilder" else "Strategy\nForge",  Routes.STRATEGY_FORGE,  Icons.Default.Build,      if (isPres) Color(0xFF818CF8) else CTPColors.Dice),
    MenuTile(if (isPres) "Stress\nTest" else "Stress\nTest",     Routes.STRESS_TEST,     Icons.Default.Speed,      if (isPres) Color(0xFF818CF8) else CTPColors.Limbo),
    MenuTile(if (isPres) "Probability\nEvolution" else "Dice\nEvolution",  Routes.DICE_EVOLUTION,  Icons.Default.Biotech,    if (isPres) Color(0xFF818CF8) else CTPColors.Dice),
    MenuTile(if (isPres) "Exponential\nGrowth Lab" else "Limbo\nEvolution", Routes.LIMBO_EVOLUTION, Icons.Default.ShowChart,  if (isPres) Color(0xFF818CF8) else CTPColors.Limbo),
    MenuTile(if (isPres) "Pattern\nEvolution" else "Keno\nEvolution",  Routes.KENO_EVOLUTION,  Icons.Default.GridOn,     if (isPres) Color(0xFF818CF8) else CTPColors.Keno),
    MenuTile(if (isPres) "Grid-Risk\nEvolution" else "Mines\nEvolution", Routes.MINES_EVOLUTION, Icons.Default.Dangerous,  if (isPres) Color(0xFF818CF8) else CTPColors.Mines),
)

fun analyticsTiles(isPres: Boolean) = listOf(
    MenuTile(if (isPres) "Spatial Distribution\nLab" else "Keno\nMonte Carlo", Routes.KENO_MC,         Icons.Default.GridView,  if (isPres) Color(0xFFA78BFA) else CTPColors.Keno),
    MenuTile(if (isPres) "Probability Path\nAnalysis" else "Mines\nAnalytics",  Routes.MINES_ANALYTICS, Icons.Default.Dangerous, if (isPres) Color(0xFFA78BFA) else CTPColors.Mines),
    MenuTile(if (isPres) "Statistical Deck\nEngine" else "Blackjack\nHub",    Routes.BLACKJACK,       Icons.Default.Style,     if (isPres) Color(0xFFA78BFA) else CTPColors.Limbo),
)

fun sportsTiles(isPres: Boolean) = listOf(
    MenuTile(if (isPres) "Athletic Data\nLab" else "Sports\nBetting",  Routes.SPORTS_LAB, Icons.Default.SportsSoccer, if (isPres) Color(0xFFF472B6) else CTPColors.Sports),
    MenuTile(if (isPres) "Kelly Criterion\nTool" else "Kelly\nCalc",      Routes.KELLY_CALC, Icons.Default.Functions,    if (isPres) Color(0xFFF472B6) else CTPColors.Sports),
    MenuTile(if (isPres) "Compounded Risk\nAnalyst" else "Parlay\nAnalyzer", Routes.PARLAY,     Icons.Default.AccountTree,  if (isPres) Color(0xFFF472B6) else CTPColors.Sports),
    MenuTile(if (isPres) "Edge Discovery\nTool" else "Value\nBet",       Routes.VALUE_BET,  Icons.Default.TrendingUp,   if (isPres) Color(0xFFF472B6) else CTPColors.Sports),
    MenuTile(if (isPres) "Market Convergence\nCalc" else "Arbitrage\nCalc",  Routes.ARBITRAGE,  Icons.Default.SwapHoriz,    if (isPres) Color(0xFFF472B6) else CTPColors.Sports),
)

fun utilityTiles(isPres: Boolean) = listOf(
    MenuTile(if (isPres) "Capital\nSustainability" else "Bankroll\nSurvival", Routes.BANKROLL_LAB, Icons.Default.AccountBalance, if (isPres) Color(0xFF94A3B8) else CTPColors.Green),
    MenuTile(if (isPres) "Compound\nGrowth" else "Compound\nGrowth",  Routes.COMPOUND,  Icons.Default.ShowChart,       if (isPres) Color(0xFF94A3B8) else CTPColors.Utility),
    MenuTile(if (isPres) "Pattern\nMaster" else "Pattern\nMaster",   Routes.PATTERN,   Icons.Default.Pattern,         if (isPres) Color(0xFF94A3B8) else CTPColors.Utility),
    MenuTile(if (isPres) "Crypto\nConverter" else "Crypto\nConverter", Routes.CONVERTER, Icons.Default.CurrencyBitcoin, if (isPres) Color(0xFF94A3B8) else CTPColors.Gold),
    MenuTile(if (isPres) "Session\nHistory" else "Session\nHistory",  Routes.HISTORY,   Icons.Default.History,         if (isPres) Color(0xFF94A3B8) else CTPColors.TextMuted),
)

// ── Symmetric Row ─────────────────────────────────────────────────────────────

enum class TileStyle { Wide, Compact }

@Composable
fun SymmetricRow(
    tiles: List<MenuTile>,
    onNavigate: (String) -> Unit,
    fillEmpty: Boolean   = false,
    tileStyle: TileStyle = TileStyle.Wide
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        tiles.forEach { tile ->
            when (tileStyle) {
                TileStyle.Wide    -> WideTile(tile = tile, onClick = { onNavigate(tile.route) }, modifier = Modifier.weight(1f))
                TileStyle.Compact -> CompactTile(tile = tile, onClick = { onNavigate(tile.route) }, modifier = Modifier.weight(1f))
            }
        }
        if (fillEmpty) {
            val deficit = 3 - tiles.size
            repeat(deficit) {
                Spacer(Modifier.weight(1f).height(56.dp))
            }
        }
    }
}

// ── Divider Label ─────────────────────────────────────────────────────────────

@Composable
fun DividerLabel(title: String, color: Color) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = color.copy(alpha = 0.30f))
        Text(text = title, style = CTPType.LabelMedium.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = color, letterSpacing = 1.5.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = color.copy(alpha = 0.30f))
    }
}

// ── Wide Tile ─────────────────────────────────────────────────────────────────

@Composable
fun WideTile(tile: MenuTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) CTPColors.CardElevated else CTPColors.Card)
            .border(1.dp, tile.color.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .drawBehind {
                drawRect(color = tile.color, topLeft = Offset(0f, 0f), size = Size(2.5.dp.toPx(), size.height))
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(25.dp))
            Text(
                text = tile.label.replace("\n", " "),
                style = CTPType.LabelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = CTPColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp
            )
        }
    }
}

// ── Compact Tile ──────────────────────────────────────────────────────────────

@Composable
fun CompactTile(tile: MenuTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) CTPColors.CardElevated else CTPColors.Card)
            .border(1.dp, tile.color.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .drawBehind { drawRect(color = tile.color, topLeft = Offset(0f, 0f), size = Size(2.5.dp.toPx(), size.height)) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                text = tile.label,
                style = CTPType.LabelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = CTPColors.TextPrimary, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.5.sp
            )
        }
    }
}
