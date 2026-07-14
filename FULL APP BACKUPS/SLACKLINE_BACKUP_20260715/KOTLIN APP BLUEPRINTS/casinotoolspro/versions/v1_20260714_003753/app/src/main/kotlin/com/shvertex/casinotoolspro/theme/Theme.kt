package com.shvertex.casinotoolspro.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Color Palette ────────────────────────────────────────────────────────────
object CTPColors {
    val Black        = Color(0xFF000000)
    val Surface      = Color(0xFF0A0A0A)
    val Card         = Color(0xFF111111)
    val CardElevated = Color(0xFF181818)
    val Divider      = Color(0xFF1E1E1E)
    val Border       = Color(0xFF252525)

    val Green        = Color(0xFF00E701)   // Stake-style neon green
    val GreenDim     = Color(0xFF00A001)
    val GreenGlow    = Color(0x3300E701)

    val Red          = Color(0xFFFF4E4E)
    val RedDim       = Color(0xFF7A0C0C)
    val RedGlow      = Color(0x33FF4E4E)

    val TextPrimary  = Color(0xFFE8EAF0)
    val TextSecondary = Color(0xFFB1BAD3)
    val TextMuted    = Color(0xFF6B7280)

    // Module accent colors
    val Dice         = Color(0xFF1ABC9C)   // Teal
    val Limbo        = Color(0xFF3498DB)   // Blue
    val Keno         = Color(0xFF9B59B6)   // Purple
    val Mines        = Color(0xFFE67E22)   // Orange
    val Sports       = Color(0xFFE74C3C)   // Red-orange
    val Utility      = Color(0xFF2C3E50)   // Dark slate
    val Gold         = Color(0xFFFFD700)   // Gold for rankings

    // Chart colors
    val ChartLine    = Color(0xFF6C63FF)
    val ChartFill    = Color(0x1A6C63FF)
    val ChartGrid    = Color(0xFF1A1A2E)
}

// ── Color Scheme ─────────────────────────────────────────────────────────────
val CTPColorScheme = darkColorScheme(
    primary          = CTPColors.Green,
    onPrimary        = CTPColors.Black,
    primaryContainer = CTPColors.GreenGlow,
    secondary        = CTPColors.Dice,
    onSecondary      = CTPColors.Black,
    background       = CTPColors.Black,
    onBackground     = CTPColors.TextPrimary,
    surface          = CTPColors.Surface,
    onSurface        = CTPColors.TextPrimary,
    surfaceVariant   = CTPColors.Card,
    onSurfaceVariant = CTPColors.TextSecondary,
    error            = CTPColors.Red,
    onError          = CTPColors.Black,
    outline          = CTPColors.Border,
)

// ── Typography ───────────────────────────────────────────────────────────────
// Using system fonts — no custom font files needed for build
object CTPType {
    val DisplayLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.Black,
        fontSize    = 32.sp,
        letterSpacing = (-0.5).sp
    )
    val DisplayMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.ExtraBold,
        fontSize    = 24.sp,
        letterSpacing = (-0.3).sp
    )
    val HeadlineLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.Bold,
        fontSize    = 20.sp
    )
    val HeadlineMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 16.sp
    )
    val BodyLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.Normal,
        fontSize    = 14.sp
    )
    val BodyMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.Normal,
        fontSize    = 13.sp
    )
    val LabelLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 12.sp,
        letterSpacing = 0.5.sp
    )
    val LabelMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.Medium,
        fontSize    = 11.sp,
        letterSpacing = 0.4.sp
    )
    val Mono = TextStyle(
        fontFamily  = FontFamily.Monospace,
        fontWeight  = FontWeight.Medium,
        fontSize    = 13.sp
    )
    val MonoLarge = TextStyle(
        fontFamily  = FontFamily.Monospace,
        fontWeight  = FontWeight.Bold,
        fontSize    = 20.sp
    )
}

// ── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun CasinoToolsProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CTPColorScheme,
        content = content
    )
}
