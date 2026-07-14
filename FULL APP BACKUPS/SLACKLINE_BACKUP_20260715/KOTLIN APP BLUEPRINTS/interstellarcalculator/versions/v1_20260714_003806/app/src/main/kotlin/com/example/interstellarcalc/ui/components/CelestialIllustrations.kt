package com.example.interstellarcalc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.interstellarcalc.data.BlackHoleType
import com.example.interstellarcalc.data.StarType
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Public entry points
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StarIllustration(starType: StarType, size: Dp = 180.dp, modifier: Modifier = Modifier) {
    when (starType) {
        StarType.BLUE_SUPERGIANT -> BlueSupergiantCanvas(size, modifier)
        StarType.YELLOW_DWARF    -> YellowDwarfCanvas(size, modifier)
        StarType.RED_GIANT       -> RedGiantCanvas(size, modifier)
        StarType.RED_DWARF       -> RedDwarfCanvas(size, modifier)
        StarType.WHITE_DWARF     -> WhiteDwarfCanvas(size, modifier)
        StarType.NEUTRON_STAR    -> NeutronStarCanvas(size, modifier)
    }
}

@Composable
fun BlackHoleIllustration(bhType: BlackHoleType, size: Dp = 180.dp, modifier: Modifier = Modifier) {
    when (bhType) {
        BlackHoleType.STELLAR        -> StellarBlackHoleCanvas(size, modifier)
        BlackHoleType.INTERMEDIATE   -> IntermediateBlackHoleCanvas(size, modifier)
        BlackHoleType.SUPERMASSIVE   -> SupermassiveBlackHoleCanvas(size, modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Blue Supergiant
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BlueSupergiantCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "bsg")
    val pulse by inf.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val rayRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "ray")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.28f * pulse
        // Outer corona
        drawCircle(Brush.radialGradient(listOf(Color(0xFF93C5FD).copy(0.18f), Color.Transparent), Offset(cx,cy), r*3.2f), r*3.2f, Offset(cx,cy))
        // Mid glow
        drawCircle(Brush.radialGradient(listOf(Color(0xFF60A5FA).copy(0.45f), Color.Transparent), Offset(cx,cy), r*2.0f), r*2.0f, Offset(cx,cy))
        // Star body
        drawCircle(Brush.radialGradient(listOf(Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF1D4ED8)), Offset(cx-r*0.2f,cy-r*0.2f), r), r, Offset(cx,cy))
        // Rays
        rotate(rayRot, Offset(cx,cy)) {
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45.0)).toFloat()
                val x1 = cx + cos(angle) * r * 1.15f; val y1 = cy + sin(angle) * r * 1.15f
                val x2 = cx + cos(angle) * r * 2.2f;  val y2 = cy + sin(angle) * r * 2.2f
                drawLine(Color(0xFF93C5FD).copy(alpha = 0.55f), Offset(x1,y1), Offset(x2,y2), r*0.10f, cap = StrokeCap.Round)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Yellow Dwarf (Sun-like)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun YellowDwarfCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "yd")
    val pulse by inf.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val rayRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "r")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.28f * pulse
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFEF08A).copy(0.18f), Color.Transparent), Offset(cx,cy), r*3.2f), r*3.2f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(0.40f), Color.Transparent), Offset(cx,cy), r*2.0f), r*2.0f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706)), Offset(cx-r*0.2f,cy-r*0.2f), r), r, Offset(cx,cy))
        rotate(rayRot, Offset(cx,cy)) {
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30.0)).toFloat()
                val len = if (i % 2 == 0) r * 2.0f else r * 1.55f
                val x1 = cx + cos(angle) * r * 1.12f; val y1 = cy + sin(angle) * r * 1.12f
                val x2 = cx + cos(angle) * len;        val y2 = cy + sin(angle) * len
                drawLine(Color(0xFFFBBF24).copy(alpha = 0.5f), Offset(x1,y1), Offset(x2,y2), r*0.08f, cap = StrokeCap.Round)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Red Giant
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RedGiantCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "rg")
    val pulse by inf.animateFloat(0.90f, 1.10f, infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val shimmer by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.36f * pulse
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFCA5A5).copy(0.12f), Color.Transparent), Offset(cx,cy), r*2.8f), r*2.8f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF87171).copy(0.38f), Color.Transparent), Offset(cx,cy), r*1.7f), r*1.7f, Offset(cx,cy))
        // Convection shimmer on surface
        val shimmerAlpha = (0.12f + shimmer * 0.12f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFEF2F2), Color(0xFFEF4444), Color(0xFF7F1D1D)), Offset(cx-r*0.25f,cy-r*0.25f), r), r, Offset(cx,cy))
        drawCircle(Color(0xFFFB923C).copy(shimmerAlpha), r*0.55f, Offset(cx + r*0.15f, cy - r*0.15f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Red Dwarf
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RedDwarfCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "rd")
    val pulse by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val flare by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.22f * pulse
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFCA5A5).copy(0.10f), Color.Transparent), Offset(cx,cy), r*3.0f), r*3.0f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFDC2626).copy(0.35f), Color.Transparent), Offset(cx,cy), r*1.8f), r*1.8f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFF7F1D1D)), Offset(cx-r*0.2f,cy-r*0.2f), r), r, Offset(cx,cy))
        // Occasional stellar flare
        if (flare > 0.75f) {
            val flareAlpha = ((flare - 0.75f) / 0.25f) * 0.7f
            val angle = -PI.toFloat() / 4f
            val fx = cx + cos(angle) * r * 1.5f; val fy = cy + sin(angle) * r * 1.5f
            drawCircle(Color(0xFFFCA5A5).copy(flareAlpha), r * 0.4f, Offset(fx, fy))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. White Dwarf
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WhiteDwarfCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "wd")
    val pulse by inf.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val shimmer by inf.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "s")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.16f * pulse
        // Faint blue-white halo
        drawCircle(Brush.radialGradient(listOf(Color(0xFFDBEAFE).copy(0.22f), Color.Transparent), Offset(cx,cy), r*4.0f), r*4.0f, Offset(cx,cy))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFBFDBFE).copy(0.50f), Color.Transparent), Offset(cx,cy), r*2.2f), r*2.2f, Offset(cx,cy))
        // Tiny but intense body
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDBEAFE), Color(0xFF93C5FD)), Offset(cx,cy), r), r, Offset(cx,cy))
        // Diffraction spike
        val spikeAlpha = (sin(shimmer) * 0.15f + 0.35f)
        for (angle in listOf(0f, 90f)) {
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            drawLine(Color.White.copy(spikeAlpha), Offset(cx - cos(rad)*r*3.5f, cy - sin(rad)*r*3.5f), Offset(cx + cos(rad)*r*3.5f, cy + sin(rad)*r*3.5f), r*0.12f, cap = StrokeCap.Round)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Neutron Star
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NeutronStarCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "ns")
    val beamRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "beam")
    val pulse   by inf.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.13f * pulse
        // Magnetic field lines glow
        drawCircle(Brush.radialGradient(listOf(Color(0xFF818CF8).copy(0.18f), Color.Transparent), Offset(cx,cy), r*4.5f), r*4.5f, Offset(cx,cy))
        // Pulsed jet beams
        rotate(beamRot, Offset(cx,cy)) {
            for (sign in listOf(1f, -1f)) {
                val beamPath = Path().apply {
                    moveTo(cx - r*0.25f, cy)
                    cubicTo(cx - r*1.5f, cy + sign*r*2.5f, cx + r*1.5f, cy + sign*r*2.5f, cx + r*0.25f, cy)
                    close()
                }
                drawPath(beamPath, Brush.radialGradient(listOf(Color(0xFFA5B4FC).copy(0.7f), Color.Transparent), Offset(cx, cy), r*3f))
            }
        }
        // Star body — tiny, blueish-white
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFC7D2FE), Color(0xFF4338CA)), Offset(cx,cy), r), r, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Stellar Black Hole
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StellarBlackHoleCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "sbh")
    val diskRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "d")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.20f
        drawAccretionDisk(cx, cy, r, diskRot, diskColor1 = Color(0xFFFF8C00), diskColor2 = Color(0xFFFFA500), diskColor3 = Color(0xFFFFD700))
        // Event horizon
        drawCircle(Color.Black, r, Offset(cx, cy))
        // Photon ring
        drawCircle(Color(0xFFFFD700).copy(0.55f), r*1.08f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.06f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Intermediate Black Hole
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IntermediateBlackHoleCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "ibh")
    val diskRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "d")
    val jetPulse by inf.animateFloat(0.6f, 1.0f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "j")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.24f
        // Relativistic jets
        for (sign in listOf(1f, -1f)) {
            drawPath(Path().apply {
                moveTo(cx - r*0.15f, cy); cubicTo(cx - r*0.8f, cy + sign*r*2.8f, cx + r*0.8f, cy + sign*r*2.8f, cx + r*0.15f, cy); close()
            }, Brush.radialGradient(listOf(Color(0xFF38BDF8).copy(jetPulse * 0.6f), Color.Transparent), Offset(cx, cy), r*3f))
        }
        drawAccretionDisk(cx, cy, r, diskRot, diskColor1 = Color(0xFFFF6B35), diskColor2 = Color(0xFFFF8C00), diskColor3 = Color(0xFFFFD700))
        drawCircle(Color.Black, r, Offset(cx, cy))
        drawCircle(Color(0xFFFFD700).copy(0.45f), r*1.08f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.07f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. Supermassive Black Hole
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SupermassiveBlackHoleCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "smbh")
    val diskRot  by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "d")
    val coronaPulse by inf.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c")
    val jetPulse by inf.animateFloat(0.5f, 1.0f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "j")
    Canvas(modifier.size(size)) {
        val cx = this.size.width/2f; val cy = this.size.height/2f
        val r  = this.size.minDimension * 0.28f
        // Outer corona
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFF8C00).copy(0.12f * coronaPulse), Color.Transparent), Offset(cx,cy), r*3.8f), r*3.8f, Offset(cx,cy))
        // Relativistic jets
        for (sign in listOf(1f, -1f)) {
            drawPath(Path().apply {
                moveTo(cx - r*0.12f, cy); cubicTo(cx - r*1.2f, cy + sign*r*3.5f, cx + r*1.2f, cy + sign*r*3.5f, cx + r*0.12f, cy); close()
            }, Brush.radialGradient(listOf(Color(0xFF7DD3FC).copy(jetPulse * 0.55f), Color.Transparent), Offset(cx, cy), r*4f))
        }
        drawAccretionDisk(cx, cy, r, diskRot, diskColor1 = Color(0xFFFF4500), diskColor2 = Color(0xFFFF8C00), diskColor3 = Color(0xFFFFD700))
        drawCircle(Color.Black, r, Offset(cx, cy))
        // Bright photon ring (M87* inspired)
        drawCircle(Color(0xFFFFD700).copy(0.70f), r*1.10f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.09f))
        // Inner shadow gradient
        drawCircle(Brush.radialGradient(listOf(Color.Black, Color.Black, Color(0xFF1A0A00).copy(0.0f)), Offset(cx,cy), r*1.5f), r*1.5f, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helper — accretion disk drawn as flattened elliptical rings
// ─────────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawAccretionDisk(
    cx: Float, cy: Float, r: Float, rotDeg: Float,
    diskColor1: Color, diskColor2: Color, diskColor3: Color
) {
    rotate(rotDeg, Offset(cx, cy)) {
        val layers = listOf(
            Triple(r * 3.0f, r * 0.55f, diskColor1.copy(0.25f)),
            Triple(r * 2.4f, r * 0.45f, diskColor2.copy(0.40f)),
            Triple(r * 1.9f, r * 0.35f, diskColor3.copy(0.60f)),
            Triple(r * 1.5f, r * 0.25f, diskColor3.copy(0.80f)),
        )
        layers.forEach { (rw, rh, color) ->
            drawOval(
                brush = Brush.radialGradient(listOf(color, color.copy(0f)), Offset(cx, cy), rw),
                topLeft = Offset(cx - rw, cy - rh),
                size = Size(rw * 2f, rh * 2f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Universe Catalogue illustration entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UniverseIllustration(objectType: com.example.interstellarcalc.data.UniverseObjectType, size: Dp = 180.dp, modifier: Modifier = Modifier) {
    when (objectType) {
        com.example.interstellarcalc.data.UniverseObjectType.STAR                -> YellowDwarfCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.BLACK_HOLE          -> SupermassiveBlackHoleCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.GALAXY_SPIRAL       -> SpiralGalaxyCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.GALAXY_ELLIPTICAL   -> EllipticalGalaxyCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.GALAXY_IRREGULAR    -> IrregularGalaxyCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.NEBULA_EMISSION     -> EmissionNebulaCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.NEBULA_PLANETARY    -> PlanetaryNebulaCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.NEBULA_SUPERNOVA    -> SupernovaRemnantCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.CLUSTER_OPEN        -> OpenClusterCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.CLUSTER_GLOBULAR    -> GlobularClusterCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.QUASAR              -> QuasarCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.PULSAR              -> NeutronStarCanvas(size, modifier)
        com.example.interstellarcalc.data.UniverseObjectType.UNKNOWN             -> UnknownObjectCanvas(size, modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. Spiral Galaxy
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SpiralGalaxyCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "sg")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "r")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.38f
        // Outer halo
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(0.08f), Color.Transparent), Offset(cx,cy), r*2.5f), r*2.5f, Offset(cx,cy))
        rotate(rot, Offset(cx,cy)) {
            // Spiral arms — 2 arms drawn as curved paths
            for (arm in 0..1) {
                val armOffset = if (arm == 0) 0f else PI.toFloat()
                val armPath = Path()
                var first = true
                for (i in 0..80) {
                    val t     = i / 80f
                    val angle = armOffset + t * 3.5f * PI.toFloat()
                    val rad   = r * 0.08f + r * 0.85f * t
                    val px    = cx + cos(angle) * rad
                    val py    = cy + sin(angle) * rad * 0.38f
                    if (first) { armPath.moveTo(px, py); first = false } else armPath.lineTo(px, py)
                }
                drawPath(armPath, Color(0xFFFDE68A).copy(0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.18f))
                drawPath(armPath, Color(0xFFBFDBFE).copy(0.18f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f))
            }
            // Bright core
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A).copy(0.6f), Color.Transparent), Offset(cx,cy), r*0.22f), r*0.22f, Offset(cx,cy))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. Elliptical Galaxy
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EllipticalGalaxyCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "eg")
    val pulse by inf.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val rx = this.size.width  * 0.42f * pulse
        val ry = this.size.height * 0.28f * pulse
        drawOval(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(0.10f), Color.Transparent), Offset(cx,cy), rx), topLeft = Offset(cx-rx*1.4f, cy-ry*1.4f), size = Size(rx*2.8f, ry*2.8f))
        drawOval(Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(0.30f), Color.Transparent), Offset(cx,cy), rx), topLeft = Offset(cx-rx*1.0f, cy-ry*1.0f), size = Size(rx*2.0f, ry*2.0f))
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706).copy(0.0f)), Offset(cx,cy), rx), topLeft = Offset(cx-rx, cy-ry), size = Size(rx*2f, ry*2f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. Irregular Galaxy
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IrregularGalaxyCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "ig")
    val shimmer by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.36f
        // Scattered star-forming regions
        val regions = listOf(
            Triple(cx - r*0.3f, cy + r*0.1f, r*0.30f),
            Triple(cx + r*0.4f, cy - r*0.2f, r*0.22f),
            Triple(cx - r*0.1f, cy - r*0.35f, r*0.18f),
            Triple(cx + r*0.1f, cy + r*0.38f, r*0.16f),
            Triple(cx - r*0.5f, cy - r*0.1f, r*0.14f),
        )
        regions.forEachIndexed { i, (x, y, rad) ->
            val alpha = (0.25f + (shimmer + i * 0.2f) % 1.0f * 0.20f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF93C5FD).copy(alpha), Color(0xFFE879F9).copy(alpha*0.5f), Color.Transparent), Offset(x,y), rad), rad, Offset(x,y))
        }
        // Overall faint envelope
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(0.08f), Color.Transparent), Offset(cx,cy), r*1.3f), r*1.3f, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 13. Emission Nebula
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmissionNebulaCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "en")
    val drift by inf.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "d")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.40f
        // Layered glowing cloud shapes
        val offsets = listOf(Offset(cx-r*0.15f, cy+r*0.10f), Offset(cx+r*0.20f, cy-r*0.15f), Offset(cx-r*0.05f, cy-r*0.05f))
        val colors  = listOf(Color(0xFFE879F9), Color(0xFFF43F5E), Color(0xFF818CF8))
        offsets.forEachIndexed { i, off ->
            val pulse = sin(drift + i * 2.1f) * 0.05f + 0.95f
            drawCircle(Brush.radialGradient(listOf(colors[i].copy(0.38f), colors[i].copy(0.12f), Color.Transparent), off, r * 0.7f * pulse), r * 0.7f * pulse, off)
        }
        // Central bright star
        drawCircle(Color.White.copy(0.90f), r * 0.06f, Offset(cx, cy))
        drawCircle(Color(0xFF818CF8).copy(0.40f), r * 0.18f, Offset(cx, cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 14. Planetary Nebula
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlanetaryNebulaCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "pn")
    val expand by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.36f * expand
        // Outer shell rings
        drawCircle(Color(0xFF22D3EE).copy(0.18f), r,       Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.18f))
        drawCircle(Color(0xFF06B6D4).copy(0.40f), r*0.82f, Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.08f))
        drawCircle(Color(0xFFA78BFA).copy(0.30f), r*0.65f, Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.12f))
        // Faint fill
        drawCircle(Brush.radialGradient(listOf(Color(0xFF818CF8).copy(0.12f), Color(0xFF22D3EE).copy(0.06f), Color.Transparent), Offset(cx,cy), r), r, Offset(cx,cy))
        // Central white dwarf
        drawCircle(Color.White.copy(0.95f), r*0.07f, Offset(cx,cy))
        drawCircle(Color(0xFFBAE6FD).copy(0.55f), r*0.15f, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 15. Supernova Remnant
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SupernovaRemnantCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "snr")
    val rot   by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "r")
    val pulse by inf.animateFloat(0.93f, 1.07f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.38f * pulse
        rotate(rot, Offset(cx,cy)) {
            // Filamentary shock shell
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30.0)).toFloat()
                val x1 = cx + cos(angle) * r * 0.70f; val y1 = cy + sin(angle) * r * 0.70f
                val x2 = cx + cos(angle) * r;          val y2 = cy + sin(angle) * r
                drawLine(Color(0xFFF97316).copy(0.55f), Offset(x1,y1), Offset(x2,y2), r*0.14f, cap = StrokeCap.Round)
            }
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFEF3C7).copy(0.12f), Color(0xFFF97316).copy(0.20f), Color.Transparent), Offset(cx,cy), r), r, Offset(cx,cy))
        // Central pulsar remnant
        drawCircle(Color.White.copy(0.85f), r*0.06f, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 16. Open Cluster
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OpenClusterCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "oc")
    val twinkle by inf.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t")
    // Fixed star positions for the cluster
    val stars = remember {
        listOf(
            Pair(0.50f, 0.50f), Pair(0.38f, 0.42f), Pair(0.62f, 0.44f), Pair(0.45f, 0.62f),
            Pair(0.57f, 0.60f), Pair(0.30f, 0.55f), Pair(0.70f, 0.52f), Pair(0.48f, 0.34f),
            Pair(0.55f, 0.70f), Pair(0.35f, 0.65f), Pair(0.65f, 0.65f), Pair(0.42f, 0.28f),
            Pair(0.60f, 0.30f), Pair(0.25f, 0.45f), Pair(0.75f, 0.40f), Pair(0.52f, 0.78f)
        )
    }
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        stars.forEachIndexed { i, (fx, fy) ->
            val alpha = (sin(twinkle + i * 0.7f) * 0.3f + 0.7f)
            val r     = if (i == 0) w * 0.030f else w * 0.012f + (i % 3) * w * 0.006f
            val color = if (i % 3 == 0) Color(0xFF93C5FD) else if (i % 3 == 1) Color(0xFFFDE68A) else Color.White
            drawCircle(color.copy(alpha), r, Offset(fx * w, fy * h))
        }
        // Faint envelope
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(0.06f), Color.Transparent), Offset(w/2f,h/2f), w*0.35f), w*0.35f, Offset(w/2f,h/2f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 17. Globular Cluster
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlobularClusterCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "gc")
    val twinkle by inf.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t")
    val starPositions = remember {
        val rng = kotlin.random.Random(42)
        (0 until 80).map {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val rad   = rng.nextFloat().pow(0.5f) * 0.42f
            Pair(0.5f + cos(angle) * rad, 0.5f + sin(angle) * rad)
        }
    }
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        // Outer glow
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(0.25f), Color(0xFFFBBF24).copy(0.08f), Color.Transparent), Offset(w/2f,h/2f), w*0.42f), w*0.42f, Offset(w/2f,h/2f))
        starPositions.forEachIndexed { i, (fx, fy) ->
            val alpha = (sin(twinkle + i * 0.4f) * 0.25f + 0.75f)
            val r     = w * 0.008f + (i % 4) * w * 0.003f
            drawCircle(Color(0xFFFDE68A).copy(alpha), r, Offset(fx * w, fy * h))
        }
        // Bright core
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A).copy(0.0f)), Offset(w/2f,h/2f), w*0.12f), w*0.12f, Offset(w/2f,h/2f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 18. Quasar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuasarCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "qsr")
    val jetPulse by inf.animateFloat(0.5f, 1.0f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "j")
    val diskRot  by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "d")
    val corePulse by inf.animateFloat(0.88f, 1.12f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.18f
        // Outer blaze
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC084FC).copy(0.15f), Color.Transparent), Offset(cx,cy), r*5f), r*5f, Offset(cx,cy))
        // Jets
        for (sign in listOf(1f, -1f)) {
            drawPath(Path().apply {
                moveTo(cx - r*0.10f, cy); cubicTo(cx - r*1.5f, cy + sign*r*4.5f, cx + r*1.5f, cy + sign*r*4.5f, cx + r*0.10f, cy); close()
            }, Brush.radialGradient(listOf(Color(0xFFC084FC).copy(jetPulse * 0.65f), Color.Transparent), Offset(cx,cy), r*5f))
        }
        drawAccretionDisk(cx, cy, r, diskRot, Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFC084FC))
        // Blazing core
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFC084FC), Color(0xFF7C3AED).copy(0f)), Offset(cx,cy), r*corePulse), r*corePulse, Offset(cx,cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 19. Unknown Object
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UnknownObjectCanvas(size: Dp, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "unk")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "r")
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r  = this.size.minDimension * 0.28f
        drawCircle(Brush.radialGradient(listOf(Color(0xFF94A3B8).copy(0.12f), Color.Transparent), Offset(cx,cy), r*2.5f), r*2.5f, Offset(cx,cy))
        rotate(rot, Offset(cx,cy)) {
            for (i in 0 until 6) {
                val angle = Math.toRadians((i * 60.0)).toFloat()
                drawLine(Color(0xFF64748B).copy(0.40f), Offset(cx + cos(angle)*r*0.5f, cy + sin(angle)*r*0.5f), Offset(cx + cos(angle)*r*1.2f, cy + sin(angle)*r*1.2f), r*0.08f, cap = StrokeCap.Round)
            }
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFF1E293B)), Offset(cx,cy), r), r, Offset(cx,cy))
        drawCircle(Color(0xFF94A3B8).copy(0.30f), r*1.05f, Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(r*0.06f))
    }
}

// Helper extension used in GlobularCluster
private fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()
