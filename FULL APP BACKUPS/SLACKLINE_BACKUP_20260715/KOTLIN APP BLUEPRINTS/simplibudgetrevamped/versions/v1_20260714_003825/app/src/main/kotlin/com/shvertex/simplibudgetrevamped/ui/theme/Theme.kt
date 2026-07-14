package com.shvertex.simplibudgetrevamped.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── AMOLED base ───────────────────────────────────────────────────────────────
val AmoledBg      = Color(0xFF000000)
val AmoledSurface = Color(0xFF080808)
val AmoledCard    = Color(0xFF0F0F0F)
val AmoledNavBtn  = Color(0xFF1A1A1A)
val AmoledBorder  = Color(0xFF252525)
val AmoledText    = Color(0xFFF5F5F5)
val AmoledSubtext = Color(0xFF777777)
val AmoledInputBg = Color(0xFF111111)
val GlassWhite    = Color(0x14FFFFFF)
val GlassBorder   = Color(0x22FFFFFF)

// ── Vibrant accents ───────────────────────────────────────────────────────────
val Accent        = Color(0xFF00E5A0)   // electric teal
val Accent2       = Color(0xFF3D8EFF)   // vivid blue
val Danger        = Color(0xFFFF4757)   // hot red
val Warning       = Color(0xFFFFB300)   // amber
val GreenPos      = Color(0xFF2ED573)   // neon green
val Purple        = Color(0xFFD46EFF)   // electric purple
val Pink          = Color(0xFFFF6B9D)   // hot pink
val Cyan          = Color(0xFF00D4FF)   // electric cyan

// ── Gradient brushes ──────────────────────────────────────────────────────────
val GradientTeal   = Brush.linearGradient(listOf(Color(0xFF00E5A0), Color(0xFF00B4D8)))
val GradientBlue   = Brush.linearGradient(listOf(Color(0xFF3D8EFF), Color(0xFFB44FFF)))
val GradientRed    = Brush.linearGradient(listOf(Color(0xFFFF4757), Color(0xFFFF6B9D)))
val GradientGold   = Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6B35)))
val GradientGreen  = Brush.linearGradient(listOf(Color(0xFF2ED573), Color(0xFF00E5A0)))
val GradientPurple = Brush.linearGradient(listOf(Color(0xFFD46EFF), Color(0xFF3D8EFF)))
val GradientCyan   = Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF2ED573)))

// ── Card gradient overlays (very subtle) ─────────────────────────────────────
val CardGlowTeal   = Brush.radialGradient(listOf(Color(0x2200E5A0), Color(0x000000000)))
val CardGlowBlue   = Brush.radialGradient(listOf(Color(0x223D8EFF), Color(0x000000000)))
val CardGlowPurple = Brush.radialGradient(listOf(Color(0x22D46EFF), Color(0x000000000)))
val CardGlowRed    = Brush.radialGradient(listOf(Color(0x22FF4757), Color(0x000000000)))

private val AppColorScheme = darkColorScheme(
    primary        = Accent,
    secondary      = Accent2,
    tertiary       = Purple,
    background     = AmoledBg,
    surface        = AmoledSurface,
    surfaceVariant = AmoledCard,
    onBackground   = AmoledText,
    onSurface      = AmoledText,
    onPrimary      = AmoledBg,
    onSecondary    = AmoledBg,
    error          = Danger,
    outline        = AmoledBorder
)

@Composable
fun SimpliBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
