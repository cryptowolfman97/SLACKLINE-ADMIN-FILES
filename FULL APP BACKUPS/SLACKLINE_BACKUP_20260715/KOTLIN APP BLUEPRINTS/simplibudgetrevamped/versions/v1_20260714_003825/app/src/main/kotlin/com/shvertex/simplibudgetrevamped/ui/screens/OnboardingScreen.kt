package com.shvertex.simplibudgetrevamped.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Data model ────────────────────────────────────────────────────────────────

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentBrush: Brush,
    val accentColor: Color,
    val illustration: @Composable (Modifier) -> Unit
)

// ── Steps ─────────────────────────────────────────────────────────────────────

@Composable
private fun buildSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        emoji = "👋",
        title = "Welcome to\nSimpliBudget SHV",
        subtitle = "Zero-Based Budgeting",
        description = "Give every rupee a job. Assign all your income to categories until nothing is left unaccounted for. Financial clarity starts here.",
        accentBrush = GradientTeal,
        accentColor = Accent,
        illustration = { mod -> WelcomeIllustration(mod) }
    ),
    OnboardingStep(
        emoji = "💰",
        title = "Start With\nYour Income",
        subtitle = "Budget → Add Income",
        description = "Tap the Budget tab and hit '+ Add Income'. Enter your salary, freelance pay or any money coming in this month. Do this first — everything else follows.",
        accentBrush = GradientGreen,
        accentColor = GreenPos,
        illustration = { mod -> IncomeIllustration(mod) }
    ),
    OnboardingStep(
        emoji = "📊",
        title = "Assign Every\nDollar a Job",
        subtitle = "Budget → Categories",
        description = "Tap each budget category and set a planned amount. Housing, Food, Transport... keep going until your Unassigned amount reaches zero. That's the goal!",
        accentBrush = GradientBlue,
        accentColor = Accent2,
        illustration = { mod -> AssignIllustration(mod) }
    ),
    OnboardingStep(
        emoji = "🧾",
        title = "Log Your\nSpending",
        subtitle = "Log Tab → + Add",
        description = "Every time you spend, tap the Log tab and record it. Select the category, amount and account. Watch your budget bars fill up in real time.",
        accentBrush = GradientGold,
        accentColor = Warning,
        illustration = { mod -> LogIllustration(mod) }
    ),
    OnboardingStep(
        emoji = "🏦",
        title = "Track Your\nAccounts",
        subtitle = "Accounts Tab",
        description = "Add your bank accounts, e-wallets and cash. SimpliBudget tracks balances automatically as you log transactions. See your net worth grow over time.",
        accentBrush = GradientPurple,
        accentColor = Purple,
        illustration = { mod -> AccountsOnboardIllustration(mod) }
    ),
    OnboardingStep(
        emoji = "🎯",
        title = "Set Goals &\nTrack Bills",
        subtitle = "More → Goals & Bills",
        description = "Set savings goals and watch the progress bars climb. Add recurring bills and mark them paid each month. You're all set — let's build wealth! 🚀",
        accentBrush = GradientRed,
        accentColor = Pink,
        illustration = { mod -> GoalsOnboardIllustration(mod) }
    )
)

// ── Main onboarding composable ────────────────────────────────────────────────

@Composable
fun OnboardingOverlay(
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    val steps = buildSteps()
    var currentStep by remember { mutableStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Entry animation
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); entered = true }
    val overlayAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )
    val overlayScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overlayScale"
    )

    // Slide transition between steps
    var slideDir by remember { mutableStateOf(1) } // 1 = left, -1 = right
    val transition = updateTransition(currentStep, label = "step")
    val cardOffset by transition.animateFloat(
        transitionSpec = { tween(380, easing = FastOutSlowInEasing) },
        label = "cardOffset"
    ) { 0f }

    fun goTo(index: Int, dir: Int = 1) {
        if (index in steps.indices) {
            slideDir = dir
            currentStep = index
        }
    }

    fun next() { if (currentStep < steps.lastIndex) goTo(currentStep + 1, 1)
                 else onDismiss(dontShowAgain) }
    fun prev() { if (currentStep > 0) goTo(currentStep - 1, -1) }

    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
    ) {
        // Blurred background scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .blur(16.dp)
        )

        // Card content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 40.dp)
                .graphicsLayer { scaleX = overlayScale; scaleY = overlayScale },
            contentAlignment = Alignment.Center
        ) {
            // Swipe gesture detector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .pointerInput(currentStep) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -80f) next()
                                else if (dragOffset > 80f) prev()
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, delta -> dragOffset += delta }
                        )
                    }
            ) {
                val step = steps[currentStep]

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn(tween(300))).togetherWith(
                                slideOutHorizontally { -it } + fadeOut(tween(300)))
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(300))).togetherWith(
                                slideOutHorizontally { it } + fadeOut(tween(300)))
                        }
                    },
                    label = "stepContent"
                ) { stepIndex ->
                    val s = steps[stepIndex]
                    StepCard(
                        step = s,
                        stepIndex = stepIndex,
                        totalSteps = steps.size,
                        dontShowAgain = dontShowAgain,
                        onDontShowToggle = { dontShowAgain = it },
                        onPrev = ::prev,
                        onNext = ::next,
                        onSkip = { onDismiss(dontShowAgain) }
                    )
                }
            }
        }
    }
}

// ── Step card ─────────────────────────────────────────────────────────────────

@Composable
private fun StepCard(
    step: OnboardingStep,
    stepIndex: Int,
    totalSteps: Int,
    dontShowAgain: Boolean,
    onDontShowToggle: (Boolean) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val isLast = stepIndex == totalSteps - 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0E0E0E), Color(0xFF141414)))
            )
            .border(1.dp, step.accentColor.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Skip button top right
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLast) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = AmoledSubtext, fontSize = 13.sp)
                    }
                } else {
                    Spacer(Modifier.height(36.dp))
                }
            }

            // Illustration
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                step.illustration(Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(8.dp))

            // Step badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(step.accentBrush)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    step.subtitle,
                    color = AmoledBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // Title
            Text(
                text = step.title,
                style = androidx.compose.ui.text.TextStyle(
                    brush = step.accentBrush,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(12.dp))

            // Description
            Text(
                text = step.description,
                color = Color(0xFFBBBBBB),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(24.dp))

            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { i ->
                    val isActive = i == stepIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                        label = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .then(
                                if (isActive) Modifier.background(step.accentBrush)
                                else Modifier.background(Color(0xFF333333))
                            )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stepIndex > 0) {
                    OutlinedButton(
                        onClick = onPrev,
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = AmoledSubtext, modifier = Modifier.size(20.dp))
                    }
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(step.accentBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (isLast) "Let's Go! 🚀" else "Next",
                                color = AmoledBg,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (!isLast) Icon(
                                Icons.Default.ArrowForward, null,
                                tint = AmoledBg, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Don't show again toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Don't show again",
                    color = AmoledSubtext,
                    fontSize = 13.sp
                )
                Switch(
                    checked = dontShowAgain,
                    onCheckedChange = onDontShowToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmoledBg,
                        checkedTrackColor = step.accentColor,
                        uncheckedTrackColor = Color(0xFF2A2A2A),
                        uncheckedThumbColor = Color(0xFF666666)
                    )
                )
            }
        }
    }
}

// ── Per-step Canvas illustrations ─────────────────────────────────────────────

@Composable
private fun WelcomeIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "w")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "r")
    val pulse by inf.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "p")
    val glow by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "g")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.minDimension * 0.38f

        // Outer glow ring
        drawCircle(Color(0xFF00E5A0).copy(alpha = glow * 0.15f), r * 1.35f * pulse, Offset(cx, cy))
        // Spinning dashed ring
        rotate(rot, Offset(cx, cy)) {
            for (i in 0..11) {
                val angle = Math.toRadians(i * 30.0)
                val x = cx + (r * 1.2f * kotlin.math.cos(angle)).toFloat()
                val y = cy + (r * 1.2f * kotlin.math.sin(angle)).toFloat()
                drawCircle(Color(0xFF00E5A0).copy(alpha = if (i % 2 == 0) 0.7f else 0.25f), 4f, Offset(x, y))
            }
        }
        // Main circle
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1A2E26), Color(0xFF0A0A0A)), Offset(cx, cy), r), r, Offset(cx, cy))
        drawCircle(Color(0xFF00E5A0), r, Offset(cx, cy), style = Stroke(2.5f))
        // Inner symbol — S shape approximated as two arcs
        drawArc(Color(0xFF00E5A0), 180f, 180f, false,
            Offset(cx - r * 0.3f, cy - r * 0.42f),
            androidx.compose.ui.geometry.Size(r * 0.6f, r * 0.4f),
            style = Stroke(5f, cap = StrokeCap.Round))
        drawArc(Color(0xFF3D8EFF), 0f, 180f, false,
            Offset(cx - r * 0.3f, cy + r * 0.02f),
            androidx.compose.ui.geometry.Size(r * 0.6f, r * 0.4f),
            style = Stroke(5f, cap = StrokeCap.Round))
        // Sparkles
        val sparkPositions = listOf(
            Offset(cx - r * 1.0f, cy - r * 0.7f),
            Offset(cx + r * 1.0f, cy - r * 0.5f),
            Offset(cx + r * 0.8f, cy + r * 0.8f),
            Offset(cx - r * 0.9f, cy + r * 0.6f)
        )
        sparkPositions.forEachIndexed { i, pos ->
            val a = if ((i % 2 == 0)) glow else (1f - glow)
            drawLine(Color(0xFFFFB300).copy(alpha = a * 0.9f), Offset(pos.x - 6f, pos.y), Offset(pos.x + 6f, pos.y), 2.5f)
            drawLine(Color(0xFFFFB300).copy(alpha = a * 0.9f), Offset(pos.x, pos.y - 6f), Offset(pos.x, pos.y + 6f), 2.5f)
        }
    }
}

@Composable
private fun IncomeIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "inc")
    val rise by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "rise")
    val glow by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val baseY = cy + size.height * 0.15f

        // Coin stack shadows
        for (i in 3 downTo 0) {
            val stackY = baseY - i * size.height * 0.07f - rise * size.height * 0.06f
            drawCircle(Color(0xFFFFB300).copy(alpha = 0.12f + i * 0.04f),
                size.width * 0.25f, Offset(cx, stackY + 6f))
            drawCircle(Brush.radialGradient(
                listOf(Color(0xFFFFD700), Color(0xFFFF8C00)),
                Offset(cx, stackY), size.width * 0.25f),
                size.width * 0.22f, Offset(cx, stackY))
            drawCircle(Color(0xFFFFB300), size.width * 0.22f, Offset(cx, stackY), style = Stroke(2f))
            // $ symbol hint
            if (i == 3) {
                drawLine(Color(0xFFFFFFFF).copy(alpha = 0.6f),
                    Offset(cx, stackY - size.width * 0.12f),
                    Offset(cx, stackY + size.width * 0.12f), 3f)
            }
        }
        // Rising arrow
        val arrowY = baseY - rise * size.height * 0.28f - size.height * 0.25f
        drawLine(Color(0xFF2ED573).copy(alpha = glow), Offset(cx, baseY - size.height * 0.05f), Offset(cx, arrowY), 4f)
        drawLine(Color(0xFF2ED573).copy(alpha = glow), Offset(cx, arrowY), Offset(cx - 12f, arrowY + 14f), 4f)
        drawLine(Color(0xFF2ED573).copy(alpha = glow), Offset(cx, arrowY), Offset(cx + 12f, arrowY + 14f), 4f)
        // Glow
        drawCircle(Color(0xFF2ED573).copy(alpha = glow * 0.15f), size.width * 0.35f, Offset(cx, arrowY))
    }
}

@Composable
private fun AssignIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "asgn")
    val prog by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "p")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val barColors = listOf(Color(0xFF00E5A0), Color(0xFF3D8EFF), Color(0xFFFFB300), Color(0xFFD46EFF))
        val labels = listOf(0.85f, 0.60f, 0.40f, 0.75f)
        val barH = h * 0.09f; val barMaxW = w * 0.72f
        val startX = w * 0.14f; val startY = h * 0.14f

        barColors.forEachIndexed { i, color ->
            val y = startY + i * (barH + h * 0.07f)
            val fillPct = (labels[i] * prog).coerceIn(0f, 1f)
            // Track
            drawRoundRect(Color(0xFF1E1E1E), Offset(startX, y),
                androidx.compose.ui.geometry.Size(barMaxW, barH),
                androidx.compose.ui.geometry.CornerRadius(barH / 2))
            // Fill
            if (fillPct > 0.01f) drawRoundRect(
                Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)),
                    startX = startX, endX = startX + barMaxW),
                Offset(startX, y),
                androidx.compose.ui.geometry.Size(barMaxW * fillPct, barH),
                androidx.compose.ui.geometry.CornerRadius(barH / 2))
            // Dot indicator
            drawCircle(color, barH * 0.55f, Offset(startX + barMaxW * fillPct, y + barH / 2))
            drawCircle(Color.White.copy(alpha = 0.6f), barH * 0.25f, Offset(startX + barMaxW * fillPct, y + barH / 2))
        }
    }
}

@Composable
private fun LogIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "log")
    val tick by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "t")
    val glow by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "g")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cardW = w * 0.78f; val cardH = h * 0.72f
        val cardX = (w - cardW) / 2f; val cardY = (h - cardH) / 2f

        // Card bg
        drawRoundRect(Color(0xFF111111), Offset(cardX, cardY),
            androidx.compose.ui.geometry.Size(cardW, cardH),
            androidx.compose.ui.geometry.CornerRadius(20f))
        drawRoundRect(Color(0xFF2A2A2A), Offset(cardX, cardY),
            androidx.compose.ui.geometry.Size(cardW, cardH),
            androidx.compose.ui.geometry.CornerRadius(20f), style = Stroke(1.5f))

        // Header strip
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFFF4757), Color(0xFFFF6B9D))),
            Offset(cardX, cardY),
            androidx.compose.ui.geometry.Size(cardW, cardH * 0.22f),
            androidx.compose.ui.geometry.CornerRadius(20f, 0f))

        // Transaction rows
        val rowColors = listOf(Color(0xFFFF4757), Color(0xFF2ED573), Color(0xFFFFB300))
        val rowAlphas = listOf(1f, 0.7f, 0.4f)
        for (i in 0..2) {
            val ry = cardY + cardH * 0.28f + i * cardH * 0.22f
            // indicator dot
            drawCircle(rowColors[i].copy(alpha = rowAlphas[i]), 5f, Offset(cardX + cardW * 0.12f, ry + 10f))
            // name bar
            drawRoundRect(Color(0xFF2A2A2A).copy(alpha = rowAlphas[i]),
                Offset(cardX + cardW * 0.22f, ry),
                androidx.compose.ui.geometry.Size(cardW * 0.38f, 8f),
                androidx.compose.ui.geometry.CornerRadius(4f))
            // amount bar
            drawRoundRect(rowColors[i].copy(alpha = rowAlphas[i] * 0.6f),
                Offset(cardX + cardW * 0.72f, ry),
                androidx.compose.ui.geometry.Size(cardW * 0.18f, 8f),
                androidx.compose.ui.geometry.CornerRadius(4f))
        }

        // Animated + button
        val btnR = w * 0.11f
        val btnX = cardX + cardW - btnR * 0.5f
        val btnY = cardY + cardH - btnR * 0.5f
        drawCircle(Color(0xFFFFB300).copy(alpha = glow), btnR + 6f, Offset(btnX, btnY))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00)),
            Offset(btnX, btnY), btnR), btnR, Offset(btnX, btnY))
        drawLine(Color.White, Offset(btnX - 7f, btnY), Offset(btnX + 7f, btnY), 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(btnX, btnY - 7f), Offset(btnX, btnY + 7f), 3f, cap = StrokeCap.Round)

        // Animated tick appearing on latest row when tick > 0.5
        if (tick > 0.5f) {
            val ta = ((tick - 0.5f) / 0.5f).coerceIn(0f, 1f)
            val ry = cardY + cardH * 0.28f
            drawLine(Color(0xFF2ED573).copy(alpha = ta),
                Offset(cardX + cardW * 0.82f, ry + 4f),
                Offset(cardX + cardW * 0.88f, ry + 10f), 3f, cap = StrokeCap.Round)
            drawLine(Color(0xFF2ED573).copy(alpha = ta),
                Offset(cardX + cardW * 0.88f, ry + 10f),
                Offset(cardX + cardW * 0.96f, ry + 1f), 3f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun AccountsOnboardIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "accob")
    val y1 by inf.animateFloat(-8f, 8f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "y1")
    val y2 by inf.animateFloat(6f, -6f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "y2")
    val y3 by inf.animateFloat(-4f, 10f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "y3")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cw = w * 0.68f; val ch = h * 0.28f; val rx = 16f

        // Card 3 (back)
        drawRoundRect(Color(0xFF1A1A2E), Offset(w * 0.2f + 6f, h * 0.12f + y3 + 12f),
            androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(rx))

        // Card 2 (middle)
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF2D1B69), Color(0xFF11998E))),
            Offset(w * 0.2f + 3f, h * 0.12f + y2 + 6f),
            androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(rx))

        // Card 1 (front)
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF00E5A0), Color(0xFF3D8EFF))),
            Offset(w * 0.2f, h * 0.12f + y1),
            androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(rx))
        // Chip
        drawRoundRect(Color(0xFFFFB300).copy(alpha = 0.9f),
            Offset(w * 0.28f, h * 0.22f + y1),
            androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.1f),
            androidx.compose.ui.geometry.CornerRadius(4f))
        // Stripe line
        drawLine(Color.White.copy(alpha = 0.25f),
            Offset(w * 0.2f, h * 0.33f + y1),
            Offset(w * 0.2f + cw, h * 0.33f + y1), 1.5f)
        // Card number dots
        for (i in 0..3) {
            for (j in 0..3) {
                drawCircle(Color.White.copy(alpha = 0.4f), 2.5f,
                    Offset(w * 0.28f + i * (w * 0.14f / 3f) + j * 5f, h * 0.37f + y1))
            }
        }

        // Net worth label below
        val nwy = h * 0.52f
        drawRoundRect(Color(0xFF1A1A1A), Offset(w * 0.15f, nwy),
            androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.15f),
            androidx.compose.ui.geometry.CornerRadius(10f))
        drawLine(Color(0xFF00E5A0), Offset(w * 0.15f, nwy + h * 0.15f),
            Offset(w * 0.85f, nwy + h * 0.15f), 2f)
    }
}

@Composable
private fun GoalsOnboardIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "gob")
    val prog by inf.animateFloat(0.2f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse), label = "p")
    val starRot by inf.animateFloat(-10f, 10f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "sr")
    val glow by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "g")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f

        // Goal bar
        val barY = h * 0.65f; val barH = h * 0.08f; val barW = w * 0.78f
        val barX = (w - barW) / 2f
        drawRoundRect(Color(0xFF1E1E1E), Offset(barX, barY),
            androidx.compose.ui.geometry.Size(barW, barH), androidx.compose.ui.geometry.CornerRadius(barH / 2))
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFFFF6B9D), Color(0xFFFF4757)),
                startX = barX, endX = barX + barW),
            Offset(barX, barY),
            androidx.compose.ui.geometry.Size(barW * prog, barH),
            androidx.compose.ui.geometry.CornerRadius(barH / 2))
        // Progress dot
        drawCircle(Color(0xFFFF4757), barH * 0.7f, Offset(barX + barW * prog, barY + barH / 2))
        drawCircle(Color.White.copy(alpha = 0.8f), barH * 0.3f, Offset(barX + barW * prog, barY + barH / 2))

        // Star above
        val starCx = cx; val starCy = h * 0.28f
        rotate(starRot, Offset(starCx, starCy)) {
            val r1 = w * 0.22f; val r2 = w * 0.10f
            val path = Path()
            for (i in 0 until 10) {
                val angle = Math.toRadians((i * 36.0) - 90.0)
                val r = if (i % 2 == 0) r1 else r2
                val px = starCx + (r * kotlin.math.cos(angle)).toFloat()
                val py = starCy + (r * kotlin.math.sin(angle)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, Brush.radialGradient(
                listOf(Color(0xFFFFD700), Color(0xFFFF8C00)),
                Offset(starCx, starCy), r1))
            drawCircle(Color(0xFFFFB300).copy(alpha = glow * 0.25f), r1 * 1.3f, Offset(starCx, starCy))
        }

        // Bill indicator top right
        val bx = w * 0.78f; val by2 = h * 0.08f
        drawRoundRect(Color(0xFF1A1A1A), Offset(bx, by2),
            androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.12f),
            androidx.compose.ui.geometry.CornerRadius(8f))
        drawCircle(Color(0xFFFF4757).copy(alpha = glow), 5f, Offset(bx + w * 0.12f, by2 + h * 0.04f))
    }
}
