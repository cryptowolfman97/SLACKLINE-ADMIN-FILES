package com.shvertex.universalconv.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SHVDarkColorScheme = darkColorScheme(
    primary            = Teal,
    onPrimary          = Black,
    primaryContainer   = TealBg,
    onPrimaryContainer = Teal,
    secondary          = Blue,
    onSecondary        = Black,
    secondaryContainer = BlueBg,
    onSecondaryContainer = Blue,
    tertiary           = Purple,
    background         = Black,
    surface            = Surface1,
    surfaceVariant     = Surface2,
    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,
    outline            = Border,
    error              = Error,
)

@Composable
fun SHVTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = SHVDarkColorScheme,
        typography  = SHVTypography,
        content     = content,
    )
}
