package com.snaploader.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme { AMOLED, LIGHT }

/**
 * Builds a full ColorScheme dynamically from the chosen AppTheme + AccentColour.
 * Every screen, component, and the PiP bubble reads colours from MaterialTheme.colorScheme,
 * so changing accent here propagates everywhere automatically.
 */
fun buildColorScheme(appTheme: AppTheme, accent: AccentColour): ColorScheme {
    return when (appTheme) {
        AppTheme.AMOLED -> darkColorScheme(
            primary            = accent.darkPrimary,
            onPrimary          = accent.onDark,
            primaryContainer   = accent.darkContainer,
            onPrimaryContainer = accent.darkPrimary,
            secondary          = accent.darkSecondary,
            onSecondary        = accent.onDark,
            secondaryContainer = accent.darkContainer,
            background         = AmoledBackground,
            onBackground       = AmoledTextPrimary,
            surface            = AmoledSurface,
            onSurface          = AmoledTextPrimary,
            surfaceVariant     = AmoledCard,
            onSurfaceVariant   = AmoledTextSecond,
            outline            = AmoledBorder,
            error              = AmoledError,
            onError            = Color(0xFF000000),
        )
        AppTheme.LIGHT -> lightColorScheme(
            primary            = accent.lightPrimary,
            onPrimary          = accent.onLight,
            primaryContainer   = accent.lightContainer,
            onPrimaryContainer = accent.lightPrimary,
            secondary          = accent.lightSecondary,
            onSecondary        = accent.onLight,
            secondaryContainer = accent.lightContainer,
            background         = LightBackground,
            onBackground       = LightTextPrimary,
            surface            = LightSurface,
            onSurface          = LightTextPrimary,
            surfaceVariant     = LightCard,
            onSurfaceVariant   = LightTextSecond,
            outline            = LightBorder,
            error              = LightError,
            onError            = Color(0xFFFFFFFF),
        )
    }
}

@Composable
fun SnapLoaderTheme(
    appTheme   : AppTheme    = AppTheme.AMOLED,
    accentColour: AccentColour = AccentColour.EMERALD,
    content    : @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = buildColorScheme(appTheme, accentColour),
        typography  = SnapLoaderTypography,
        content     = content
    )
}
