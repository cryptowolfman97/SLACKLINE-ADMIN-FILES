package com.example.slacklineadminapp.ui.screens

//import androidx.biometric.BiometricManager
//import androidx.biometric.BiometricManager.Authenticators.*
//import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.FragmentActivity
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.ui.components.NumberPad
import com.example.slacklineadminapp.ui.theme.*
import kotlin.random.Random

// ── Matrix rain (same as dashboard) ──────────────────────────────────────────

private val MATRIX_CHARS = "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789ABCDEF"

private data class LockMatrixColumn(
    val x: Float,
    var headY: Float,
    var speed: Float,
    var length: Int,
    var chars: List<Char>,
    var opacity: Float
)

private fun lockRandomChar() = MATRIX_CHARS[Random.nextInt(MATRIX_CHARS.length)]

@Composable
private fun LockMatrixRain(modifier: Modifier = Modifier) {
    val charSize    = 14f
    val columnCount = 28

    val columns = remember {
        (0 until columnCount).map { i ->
            LockMatrixColumn(
                x       = i * (charSize + 2f),
                headY   = Random.nextFloat() * -800f,
                speed   = 3f + Random.nextFloat() * 5f,
                length  = 8 + Random.nextInt(16),
                chars   = (0..20).map { lockRandomChar() },
                opacity = 0.35f + Random.nextFloat() * 0.35f
            )
        }.toMutableList()
    }

    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(40L)
            frame++
        }
    }

    Canvas(modifier = modifier) {
        val canvasHeight = size.height
        columns.forEachIndexed { idx, col ->
            if (frame > 0) {
                columns[idx] = col.copy(
                    headY = if (col.headY - col.length * charSize > canvasHeight)
                        -col.length * charSize - Random.nextFloat() * canvasHeight * 0.5f
                    else col.headY + col.speed,
                    chars = if (Random.nextInt(8) == 0)
                        col.chars.toMutableList().also { it[Random.nextInt(it.size)] = lockRandomChar() }
                    else col.chars
                )
            }
            drawLockMatrixColumn(columns[idx], charSize)
        }
    }
}

private fun DrawScope.drawLockMatrixColumn(col: LockMatrixColumn, charSize: Float) {
    val paint = Paint().asFrameworkPaint()
    paint.isAntiAlias = true
    paint.textSize    = charSize
    for (i in 0 until col.length) {
        val y = col.headY - i * charSize
        if (y < -charSize || y > size.height + charSize) continue
        val char      = col.chars[i % col.chars.size].toString()
        val trailFade = (1f - i.toFloat() / col.length)
        val alpha     = (col.opacity * trailFade).coerceIn(0f, 1f)
        val color     = if (i == 0)
            Color(0xFF9FFFC8).copy(alpha = (col.opacity * 2.2f).coerceIn(0f, 0.95f))
        else
            TealCol.copy(alpha = (alpha * 1.4f).coerceIn(0f, 0.85f))
        drawIntoCanvas { canvas ->
            paint.color = color.toArgb()
            canvas.nativeCanvas.drawText(char, col.x, y, paint)
        }
    }
}

// ── Lock screen ───────────────────────────────────────────────────────────────

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val ctx             = LocalContext.current
    var pin             by remember { mutableStateOf("") }
    var error           by remember { mutableStateOf("") }
    val appColors       = LocalAppColors.current
    val coroutineScope  = rememberCoroutineScope()

    // ── Biometric auto-trigger — disabled (was causing crashes) ───────────────
    // LaunchedEffect(Unit) {
    //     val activity = ctx as? FragmentActivity ?: run { return@LaunchedEffect }
    //     val manager  = BiometricManager.from(ctx)
    //     val canAuth  = manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    //     if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return@LaunchedEffect
    //     val executor = ContextCompat.getMainExecutor(ctx)
    //     val callback = object : BiometricPrompt.AuthenticationCallback() {
    //         override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
    //             onUnlocked()
    //         }
    //         override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { }
    //         override fun onAuthenticationFailed() { }
    //     }
    //     val prompt = BiometricPrompt(activity, executor, callback)
    //     val info   = BiometricPrompt.PromptInfo.Builder()
    //         .setTitle("SHV Secure Access")
    //         .setSubtitle("Verify your identity to continue")
    //         .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    //         .build()
    //     prompt.authenticate(info)
    // }

    // ── Shake animation on wrong PIN ──────────────────────────────────────────
    val shakeAnim = remember { Animatable(0f) }
    suspend fun triggerShake() {
        shakeAnim.snapTo(0f)
        shakeAnim.animateTo(
            targetValue   = 0f,
            animationSpec = keyframes {
                durationMillis = 400
                0f  at 0
                -12f at 50
                12f  at 100
                -10f at 150
                10f  at 200
                -6f  at 250
                6f   at 300
                0f   at 400
            }
        )
    }

    // ── Title shimmer ─────────────────────────────────────────────────────────
    val shimmerAnim   = rememberInfiniteTransition(label = "lockShimmer")
    val shimmerOffset by shimmerAnim.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lockShimmerOffset"
    )

    // ── Lock icon pulse ───────────────────────────────────────────────────────
    val iconPulse = rememberInfiniteTransition(label = "lockIconPulse")
    val iconScale by iconPulse.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lockIconScale"
    )
    val iconGlow by iconPulse.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lockIconGlow"
    )

    // ── Dot entry animation ───────────────────────────────────────────────────
    val dotScales = (0 until 4).map { idx ->
        val anim = remember { Animatable(1f) }
        LaunchedEffect(pin.length) {
            if (idx == pin.length - 1) {
                anim.snapTo(1.3f)
                anim.animateTo(
                    targetValue   = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessHigh
                    )
                )
            }
        }
        anim
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
    ) {
        // Matrix rain background
        LockMatrixRain(modifier = Modifier.fillMaxSize())

        // Dim scrim so content reads clearly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            appColors.bg.copy(alpha = 0.50f),
                            appColors.bg.copy(alpha = 0.40f),
                            appColors.bg.copy(alpha = 0.50f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP: Branding pushed high ─────────────────────────────────────
            Spacer(Modifier.weight(0.8f))

            // Lock icon with pulse glow
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size((72 * iconScale).dp)
                        .clip(CircleShape)
                        .background(TealCol.copy(alpha = 0.08f * iconGlow))
                )
                // Inner filled circle
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(TealCol.copy(alpha = 0.14f))
                        .border(1.dp, TealCol.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = null,
                        tint               = TealCol,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title with shimmer
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text          = "SHV SECURE ACCESS",
                    color         = TealCol,
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    textAlign     = TextAlign.Center,
                    letterSpacing = 1.5.sp
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweepWidth = size.width * 0.4f
                    val x = shimmerOffset * (size.width + sweepWidth) - sweepWidth
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startX = x,
                            endX   = x + sweepWidth
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text      = "Enter your 4-digit PIN to continue",
                color     = SubText,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center
            )

            // ── MIDDLE: Push PIN pad toward bottom ────────────────────────────
            Spacer(Modifier.weight(1f))

            // PIN dots with shake + bounce animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.offset(x = shakeAnim.value.dp)
            ) {
                repeat(4) { i ->
                    val filled  = i < pin.length
                    val dotSize = (dotScales.getOrNull(i)?.value ?: 1f)

                    Box(
                        modifier = Modifier
                            .size((22 * dotSize).dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) TealCol
                                else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (filled) TealCol
                                        else SubText.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Error message — occupies fixed height so layout doesn't jump
            Box(
                modifier          = Modifier.height(20.dp),
                contentAlignment  = Alignment.Center
            ) {
                if (error.isNotEmpty()) {
                    Text(
                        text       = error,
                        color      = RedCol,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Number pad — unchanged component ─────────────────────────────
            NumberPad(
                enteredDigits = pin,
                maxLen        = 4,
                onDigit       = { d ->
                    if (pin.length < 4) {
                        pin += d
                        if (pin.length == 4) {
                            val cfg = SecurityConfig.get(ctx)
                            if (pin == cfg.appPin) {
                                onUnlocked()
                            } else {
                                error = "INCORRECT PIN — ACCESS DENIED"
                                pin   = ""
                                // Trigger shake
                                coroutineScope.launch {
                                    triggerShake()
                                }
                            }
                        }
                    }
                },
                onClear = { pin = ""; error = "" },
                onBack  = { pin = pin.dropLast(1); error = "" }
            )

            Spacer(Modifier.weight(0.4f))
        }
    }
}
