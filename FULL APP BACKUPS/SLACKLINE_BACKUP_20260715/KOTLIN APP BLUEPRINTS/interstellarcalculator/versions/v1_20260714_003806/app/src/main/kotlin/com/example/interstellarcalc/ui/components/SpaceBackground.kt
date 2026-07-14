package com.example.interstellarcalc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.*
import kotlin.random.Random

private data class Star(
    val x: Float, val y: Float, val z: Float, 
    val radius: Float, val alpha: Float, val twinkleOffset: Float, val color: Color
)
private data class NebulaCloud(val x: Float, val y: Float, val radius: Float, val color: Long, val alpha: Float)
private data class BgBlackHole(val x: Float, val y: Float, val size: Float, val diskAngle: Float)

// Realistic stellar population colors (O, B, A, F, G, K, M spectral classes)
private val STAR_COLORS = listOf(
    Color(0xFF9DB4FF), // Blue
    Color(0xFFBBCCFF), // Blue-White
    Color(0xFFFFFFFF), // White
    Color(0xFFFFFFEE), // Yellow-White
    Color(0xFFFFD2A1), // Orange
    Color(0xFFFFA371)  // Red
)

private val STARS: List<Star> = (0 until 180).map {
    val depth = Random.nextFloat() * 0.8f + 0.2f // z-index for parallax (0.2 to 1.0)
    Star(
        x             = Random.nextFloat(),
        y             = Random.nextFloat(),
        z             = depth,
        radius        = (Random.nextFloat() * 1.8f + 0.5f) * depth, // Closer stars are bigger
        alpha         = Random.nextFloat() * 0.6f + 0.3f,
        twinkleOffset = Random.nextFloat() * 2f * PI.toFloat(),
        color         = STAR_COLORS.random()
    )
}

private val NEBULAE: List<NebulaCloud> = listOf(
    NebulaCloud(0.15f, 0.25f, 0.18f, 0xFF1A0D2E, 0.35f),
    NebulaCloud(0.80f, 0.60f, 0.14f, 0xFF0D1F2E, 0.28f),
    NebulaCloud(0.50f, 0.80f, 0.12f, 0xFF1A1A0A, 0.22f),
    NebulaCloud(0.25f, 0.70f, 0.10f, 0xFF200A1A, 0.20f),
)

private val BG_BLACK_HOLES: List<BgBlackHole> = listOf(
    BgBlackHole(0.82f, 0.18f, 22f, 35f),
    BgBlackHole(0.12f, 0.62f, 16f, -20f),
)

@Composable
fun AnimatedSpaceBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "space")

    // The Master Clock: 60-second loop to drive parallax, comet, and warp events
    val globalTime by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "global"
    )

    // Rocket moves across screen every 12 seconds
    val rocketProgress by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "rocket"
    )
    
    val flameFlicker by inf.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame"
    )
    
    val twinkle by inf.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )
    
    val pulsarRot by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulsar"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Calculate Hyperspace Warp state (happens between 0.85 and 0.90 of the global 60s loop)
        var warpIntensity = 0f
        if (globalTime in 0.85f..0.90f) {
            warpIntensity = when {
                globalTime < 0.86f -> (globalTime - 0.85f) * 100f // Ramp up
                globalTime > 0.89f -> 1f - (globalTime - 0.89f) * 100f // Ramp down
                else -> 1f // Hold warp
            }
        }

        // Deep space gradient background (flashes slightly blue during warp)
        val bgAlpha = 1f - (warpIntensity * 0.3f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF000000).copy(alpha = bgAlpha), 
                    Color(0xFF04040F).copy(alpha = bgAlpha), 
                    Color(0xFF080408).copy(alpha = bgAlpha), 
                    Color(0xFF000000).copy(alpha = bgAlpha)
                )
            )
        )

        // Nebula clouds with parallax drift
        NEBULAE.forEach { neb ->
            val driftX = (globalTime * w * 0.2f) % w
            val cx = (neb.x * w + driftX) % (w * 1.5f) - (w * 0.25f)
            val cy = neb.y * h
            val r  = neb.radius * size.minDimension
            val color = Color(neb.color).copy(alpha = neb.alpha)
            drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), Offset(cx, cy), r),
                radius = r, center = Offset(cx, cy)
            )
        }

        // Deep Space Anomaly: Pulsar
        val px = w * 0.75f; val py = h * 0.85f
        drawCircle(Color.White, 3f, Offset(px, py))
        rotate(pulsarRot, Offset(px, py)) {
            val beamBrush = Brush.linearGradient(
                listOf(Color(0xFF7DD3FC).copy(alpha = 0.6f), Color.Transparent),
                start = Offset(px, py), end = Offset(px + w * 0.4f, py)
            )
            val beamPath = Path().apply {
                moveTo(px, py); lineTo(px + w * 0.4f, py - 40f); lineTo(px + w * 0.4f, py + 40f); close()
            }
            drawPath(beamPath, beamBrush)
            val beamPath2 = Path().apply {
                moveTo(px, py); lineTo(px - w * 0.4f, py - 40f); lineTo(px - w * 0.4f, py + 40f); close()
            }
            drawPath(beamPath2, Brush.linearGradient(
                listOf(Color(0xFF7DD3FC).copy(alpha = 0.6f), Color.Transparent),
                start = Offset(px, py), end = Offset(px - w * 0.4f, py)
            ))
        }

        // Stars with Parallax and Warp effect
        val centerX = w / 2f
        val centerY = h / 2f

        STARS.forEach { star ->
            // Base parallax movement
            val driftX = (globalTime * w * star.z * 1.5f) % w
            val driftY = (globalTime * h * star.z * 0.5f) % h
            
            var sx = (star.x * w + driftX) % w
            var sy = (star.y * h + driftY) % h
            var sRadius = star.radius

            // Twinkle logic
            val a = (sin(twinkle + star.twinkleOffset) * 0.35f + 0.65f) * star.alpha

            // Hyperspace Stretching
            if (warpIntensity > 0f) {
                val dx = sx - centerX
                val dy = sy - centerY
                val dist = sqrt(dx*dx + dy*dy) + 1f
                
                // Push stars outward and stretch them
                val push = dist * warpIntensity * 0.5f
                sx += (dx / dist) * push
                sy += (dy / dist) * push
                
                val stretchLen = push * 2f
                if (stretchLen > 2f) {
                    drawLine(
                        color = star.color.copy(alpha = a),
                        start = Offset(sx, sy),
                        end = Offset(sx + (dx/dist)*stretchLen, sy + (dy/dist)*stretchLen),
                        strokeWidth = sRadius,
                        cap = StrokeCap.Round
                    )
                    return@forEach // Skip normal circle draw if stretched
                }
            }

            // Normal star draw
            drawCircle(star.color.copy(alpha = a), sRadius, Offset(sx, sy))
            
            // Halo for brighter stars
            if (sRadius > 1.5f && warpIntensity == 0f) {
                val halo = sRadius * 2.5f * a
                drawLine(star.color.copy(alpha = a * 0.4f), Offset(sx - halo, sy), Offset(sx + halo, sy), 0.8f)
                drawLine(star.color.copy(alpha = a * 0.4f), Offset(sx, sy - halo), Offset(sx, sy + halo), 0.8f)
            }
        }

        // Background Black Holes
        val bhAlpha = 1f - warpIntensity // Fade out during warp
        if (bhAlpha > 0f) {
            BG_BLACK_HOLES.forEach { bh ->
                val cx = bh.x * w
                val cy = bh.y * h
                val r  = bh.size
                rotate(pulsarRot * 0.1f + bh.diskAngle, Offset(cx, cy)) {
                    listOf(r * 2.8f to 0.18f, r * 2.2f to 0.28f, r * 1.7f to 0.40f).forEach { (rw, alpha) ->
                        drawOval(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFFF8C00).copy(alpha * bhAlpha), Color.Transparent),
                                Offset(cx, cy), rw
                            ),
                            topLeft = Offset(cx - rw, cy - rw * 0.32f),
                            size    = Size(rw * 2f, rw * 0.64f)
                        )
                    }
                }
                drawCircle(Color.Black, r, Offset(cx, cy))
                drawCircle(Color(0xFFFFD700).copy(0.35f * bhAlpha), r * 1.08f, Offset(cx, cy), style = Stroke(r * 0.08f))
            }
        }

        // Deep Space Anomaly: Slow Comet (Appears between 0.2 and 0.4 of the global loop)
        if (globalTime in 0.2f..0.4f && warpIntensity == 0f) {
            val cometT = (globalTime - 0.2f) / 0.2f
            val cx = w * 0.9f - (w * 0.8f * cometT)
            val cy = h * 0.2f + (h * 0.3f * cometT)
            val tailLen = w * 0.15f
            
            // Comet Head
            drawCircle(Color.White, 3f, Offset(cx, cy))
            // Comet Tail
            drawLine(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFBAE6FD).copy(alpha = 0.8f), Color.Transparent),
                    start = Offset(cx, cy), end = Offset(cx + tailLen, cy - tailLen * 0.4f)
                ),
                start = Offset(cx, cy), end = Offset(cx + tailLen, cy - tailLen * 0.4f),
                strokeWidth = 6f, cap = StrokeCap.Round
            )
        }

        // Realistic Orbital Rocket Animation
        if (warpIntensity < 0.5f) { // Hide rocket if mid-warp
            val startX = w * 0.10f; val startY = h * 0.95f
            val endX   = w * 0.90f; val endY   = h * 0.05f
            
            // Replaced the wobbly "kite" wave with a single, massive orbital arc
            val arc = sin(rocketProgress * PI.toFloat()) * (w * 0.12f)
            val rX = startX + (endX - startX) * rocketProgress - arc
            val rY = startY + (endY - startY) * rocketProgress + arc
            
            // The rocket slowly pitches over as it flies (gravity turn)
            // Starts pointing mostly UP (15 degrees) and slowly pitches RIGHT (up to 55 degrees)
            val rRotation = 15f + (40f * rocketProgress)
            
            drawDiagonalRocket(rX, rY, flameFlicker, rRotation)
        }
    }
}

private fun DrawScope.drawDiagonalRocket(cx: Float, cy: Float, flameFlicker: Float, rotDegrees: Float) {
    val s = 52f
    rotate(degrees = rotDegrees, pivot = Offset(cx, cy)) {
        translate(cx, cy) {
            val flameLen = s * 1.6f * flameFlicker
            val flamePath = Path().apply {
                moveTo(-s*0.22f, s*1.05f)
                cubicTo(-s*0.55f, s*1.05f+flameLen*0.4f, s*0f, s*1.05f+flameLen, s*0f, s*1.05f+flameLen)
                cubicTo(s*0f, s*1.05f+flameLen, s*0.55f, s*1.05f+flameLen*0.4f, s*0.22f, s*1.05f)
                close()
            }
            drawPath(flamePath, Brush.verticalGradient(
                listOf(Color(0xFFFFFFCC), Color(0xFFFF9900), Color(0xFFFF4400).copy(alpha=0f)),
                s*1.05f, s*1.05f+flameLen))
            
            val innerFlame = Path().apply {
                moveTo(-s*0.10f, s*1.05f)
                cubicTo(-s*0.20f, s*1.05f+flameLen*0.3f, s*0f, s*1.05f+flameLen*0.6f, s*0f, s*1.05f+flameLen*0.6f)
                cubicTo(s*0f, s*1.05f+flameLen*0.6f, s*0.20f, s*1.05f+flameLen*0.3f, s*0.10f, s*1.05f)
                close()
            }
            drawPath(innerFlame, Brush.verticalGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFFFFDD44).copy(alpha=0.8f)),
                s*1.05f, s*1.05f+flameLen*0.6f))
                
            val bodyPath = Path().apply {
                val top=-s*1.2f; val bot=s*1.05f; val hw=s*0.38f; val r=s*0.28f
                moveTo(-hw+r,top); lineTo(hw-r,top); quadraticTo(hw,top,hw,top+r)
                lineTo(hw,bot-r); quadraticTo(hw,bot,hw-r,bot)
                lineTo(-hw+r,bot); quadraticTo(-hw,bot,-hw,bot-r)
                lineTo(-hw,top+r); quadraticTo(-hw,top,-hw+r,top); close()
            }
            drawPath(bodyPath, Brush.verticalGradient(listOf(Color(0xFFE2E8F0),Color(0xFF94A3B8)),-s*1.2f,s*1.05f))
            drawRect(Color.White.copy(0.18f), Offset(-s*0.13f,-s*1.1f), Size(s*0.13f,s*1.9f))
            
            val nosePath = Path().apply { moveTo(0f,-s*2.1f); lineTo(s*0.38f,-s*1.2f); lineTo(-s*0.38f,-s*1.2f); close() }
            drawPath(nosePath, Color(0xFF22C55E))
            
            val noseShinePath = Path().apply { moveTo(-s*0.06f,-s*2.05f); lineTo(s*0f,-s*1.25f); lineTo(-s*0.18f,-s*1.25f); close() }
            drawPath(noseShinePath, Color.White.copy(0.25f))
            
            drawCircle(Color(0xFF7DD3FC).copy(0.9f), s*0.24f, Offset(0f,-s*0.30f))
            drawCircle(Color(0xFFBAE6FD).copy(0.55f), s*0.13f, Offset(-s*0.07f,-s*0.34f))
            
            val finL = Path().apply { moveTo(-s*0.38f,s*0.35f); lineTo(-s*0.90f,s*1.05f); lineTo(-s*0.38f,s*1.05f); close() }
            drawPath(finL, Color(0xFF16A34A))
            
            val finR = Path().apply { moveTo(s*0.38f,s*0.35f); lineTo(s*0.90f,s*1.05f); lineTo(s*0.38f,s*1.05f); close() }
            drawPath(finR, Color(0xFF16A34A))
        }
    }
}
