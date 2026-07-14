package com.example.omnicortex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AegisColorScheme = darkColorScheme(
    primary          = AegisGreen,
    secondary        = AegisCyan,
    tertiary         = AegisPurple,
    background       = BgAmoled,
    surface          = BgCard,
    surfaceVariant   = BgElevated,
    onPrimary        = BgAmoled,
    onSecondary      = BgAmoled,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = AegisRed,
    onError          = BgAmoled,
    outline          = BgCardBorder
)

@Composable
fun ComposeEmptyActivityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AegisColorScheme,
        typography  = Typography,
        content     = content
    )
}
