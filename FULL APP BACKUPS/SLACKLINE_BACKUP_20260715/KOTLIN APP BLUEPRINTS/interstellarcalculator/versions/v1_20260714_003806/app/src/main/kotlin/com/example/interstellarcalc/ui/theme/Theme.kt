package com.example.interstellarcalc.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Shared green accent palette ───────────────────────────────
val GreenPrimary        = Color(0xFF22C55E)   // bright emerald
val GreenOnPrimary      = Color(0xFF000000)
val GreenPrimaryContainer   = Color(0xFF052E16)
val GreenOnPrimaryContainer = Color(0xFF86EFAC)
val GreenSecondary      = Color(0xFF4ADE80)
val GreenOnSecondary    = Color(0xFF000000)

// ── AMOLED ────────────────────────────────────────────────────
private val AmoledDarkColors = darkColorScheme(
    primary             = GreenPrimary,
    onPrimary           = GreenOnPrimary,
    primaryContainer    = GreenPrimaryContainer,
    onPrimaryContainer  = GreenOnPrimaryContainer,
    secondary           = GreenSecondary,
    onSecondary         = GreenOnSecondary,
    background          = Color(0xFF000000),
    onBackground        = Color(0xFFE4E4E7),
    surface             = Color(0xFF0A0A0A),
    onSurface           = Color(0xFFE4E4E7),
    surfaceVariant      = Color(0xFF18181B),
    onSurfaceVariant    = Color(0xFFA1A1AA),
    outline             = Color(0xFF3F3F46),
    error               = Color(0xFFEF4444),
)

// ── Standard Dark ─────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary             = GreenPrimary,
    onPrimary           = GreenOnPrimary,
    primaryContainer    = GreenPrimaryContainer,
    onPrimaryContainer  = GreenOnPrimaryContainer,
    secondary           = GreenSecondary,
    onSecondary         = GreenOnSecondary,
    background          = Color(0xFF09090B),
    onBackground        = Color(0xFFE4E4E7),
    surface             = Color(0xFF18181B),
    onSurface           = Color(0xFFE4E4E7),
    surfaceVariant      = Color(0xFF27272A),
    onSurfaceVariant    = Color(0xFFA1A1AA),
    outline             = Color(0xFF52525B),
    error               = Color(0xFFEF4444),
)

// ── Cosmic — keeps purple ─────────────────────────────────────
private val CosmicColors = darkColorScheme(
    primary             = Color(0xFFC084FC),
    onPrimary           = Color(0xFF1A0033),
    primaryContainer    = Color(0xFF3B0066),
    onPrimaryContainer  = Color(0xFFE9D5FF),
    secondary           = Color(0xFF818CF8),
    onSecondary         = Color(0xFF0F0F3D),
    background          = Color(0xFF04030F),
    onBackground        = Color(0xFFE9E9F4),
    surface             = Color(0xFF0A0820),
    onSurface           = Color(0xFFE9E9F4),
    surfaceVariant      = Color(0xFF14103A),
    onSurfaceVariant    = Color(0xFFB0AACC),
    outline             = Color(0xFF2E2A52),
    error               = Color(0xFFF472B6),
)

// ── Light ─────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary             = Color(0xFF16A34A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFDCFCE7),
    onPrimaryContainer  = Color(0xFF052E16),
    secondary           = Color(0xFF15803D),
    onSecondary         = Color(0xFFFFFFFF),
    background          = Color(0xFFF8FAFC),
    onBackground        = Color(0xFF0F172A),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF0F172A),
    surfaceVariant      = Color(0xFFF1F5F9),
    onSurfaceVariant    = Color(0xFF475569),
    outline             = Color(0xFFCBD5E1),
    error               = Color(0xFFDC2626),
)

enum class AppTheme { AMOLED, DARK, COSMIC, LIGHT }

@Composable
fun InterstellarCalcTheme(
    appTheme: AppTheme = AppTheme.AMOLED,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when (appTheme) {
            AppTheme.AMOLED  -> AmoledDarkColors
            AppTheme.DARK    -> DarkColors
            AppTheme.COSMIC  -> CosmicColors
            AppTheme.LIGHT   -> LightColors
        },
        typography = Typography(),
        content    = content
    )
}
