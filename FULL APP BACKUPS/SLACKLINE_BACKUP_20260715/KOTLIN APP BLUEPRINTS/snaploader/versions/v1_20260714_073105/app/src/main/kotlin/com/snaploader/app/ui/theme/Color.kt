package com.snaploader.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Accent colour definitions ─────────────────────────────────────────────────
enum class AccentColour(
    val displayName  : String,
    val emoji        : String,
    val darkPrimary  : Color,   // used in AMOLED theme
    val darkSecondary: Color,   // slightly lighter variant
    val darkContainer: Color,   // tinted card background
    val lightPrimary : Color,   // used in Light theme
    val lightSecondary: Color,
    val lightContainer: Color,
    val onDark       : Color = Color(0xFF000000),   // text on filled buttons (dark theme)
    val onLight      : Color = Color(0xFFFFFFFF),   // text on filled buttons (light theme)
) {
    EMERALD(
        displayName   = "Emerald",
        emoji         = "💚",
        darkPrimary   = Color(0xFF00C853),
        darkSecondary = Color(0xFF00E676),
        darkContainer = Color(0xFF003314),
        lightPrimary  = Color(0xFF2E7D32),
        lightSecondary = Color(0xFF00C853),
        lightContainer = Color(0xFFC8E6C9),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    CRIMSON(
        displayName   = "Crimson",
        emoji         = "❤️",
        darkPrimary   = Color(0xFFEF5350),
        darkSecondary = Color(0xFFFF7043),
        darkContainer = Color(0xFF3E0000),
        lightPrimary  = Color(0xFFC62828),
        lightSecondary = Color(0xFFD32F2F),
        lightContainer = Color(0xFFFFCDD2),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    VIOLET(
        displayName   = "Violet",
        emoji         = "💜",
        darkPrimary   = Color(0xFFCE93D8),
        darkSecondary = Color(0xFFE040FB),
        darkContainer = Color(0xFF2A0040),
        lightPrimary  = Color(0xFF6A1B9A),
        lightSecondary = Color(0xFF8E24AA),
        lightContainer = Color(0xFFE1BEE7),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    AMBER(
        displayName   = "Amber",
        emoji         = "🌙",
        darkPrimary   = Color(0xFFFFCA28),
        darkSecondary = Color(0xFFFFD54F),
        darkContainer = Color(0xFF3D2700),
        lightPrimary  = Color(0xFFE65100),
        lightSecondary = Color(0xFFF57C00),
        lightContainer = Color(0xFFFFE0B2),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    COBALT(
        displayName   = "Cobalt",
        emoji         = "💙",
        darkPrimary   = Color(0xFF5C9EFF),
        darkSecondary = Color(0xFF82B1FF),
        darkContainer = Color(0xFF001A45),
        lightPrimary  = Color(0xFF1565C0),
        lightSecondary = Color(0xFF1976D2),
        lightContainer = Color(0xFFBBDEFB),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    ROSE(
        displayName   = "Rose",
        emoji         = "🌸",
        darkPrimary   = Color(0xFFF48FB1),
        darkSecondary = Color(0xFFF06292),
        darkContainer = Color(0xFF3E0020),
        lightPrimary  = Color(0xFFC2185B),
        lightSecondary = Color(0xFFAD1457),
        lightContainer = Color(0xFFFCE4EC),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    TEAL(
        displayName   = "Teal",
        emoji         = "🩵",
        darkPrimary   = Color(0xFF4DB6AC),
        darkSecondary = Color(0xFF80CBC4),
        darkContainer = Color(0xFF00251A),
        lightPrimary  = Color(0xFF00695C),
        lightSecondary = Color(0xFF00796B),
        lightContainer = Color(0xFFB2DFDB),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
    GRAPHITE(
        displayName   = "Graphite",
        emoji         = "🩶",
        darkPrimary   = Color(0xFFBDBDBD),
        darkSecondary = Color(0xFFE0E0E0),
        darkContainer = Color(0xFF212121),
        lightPrimary  = Color(0xFF424242),
        lightSecondary = Color(0xFF616161),
        lightContainer = Color(0xFFEEEEEE),
        onDark        = Color(0xFF000000),
        onLight       = Color(0xFFFFFFFF),
    ),
}

// ── Shared neutral colours (not accent-dependent) ─────────────────────────────
val AmoledBackground  = Color(0xFF000000)
val AmoledSurface     = Color(0xFF0D0D0D)
val AmoledCard        = Color(0xFF141414)
val AmoledBorder      = Color(0xFF2A2A2A)
val AmoledTextPrimary = Color(0xFFFFFFFF)
val AmoledTextSecond  = Color(0xFF9E9E9E)
val AmoledError       = Color(0xFFFF5252)

val LightBackground   = Color(0xFFF5F5F5)
val LightSurface      = Color(0xFFFFFFFF)
val LightCard         = Color(0xFFFFFFFF)
val LightBorder       = Color(0xFFE0E0E0)
val LightTextPrimary  = Color(0xFF121212)
val LightTextSecond   = Color(0xFF616161)
val LightError        = Color(0xFFD32F2F)
