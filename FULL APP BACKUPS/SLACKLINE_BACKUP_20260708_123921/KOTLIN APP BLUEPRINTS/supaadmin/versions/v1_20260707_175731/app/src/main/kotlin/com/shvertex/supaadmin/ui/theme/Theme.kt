package com.shvertex.supaadmin.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val card: Color,
    val card2: Color,
    val card3: Color,
    val nav: Color,
    val text: Color,
    val subtext: Color,
    val muted: Color,
    val isLight: Boolean
)

val DarkAppColors = AppColors(
    bg      = BgBlack,
    card    = CardBg,
    card2   = CardBg2,
    card3   = CardBg3,
    nav     = NavBg,
    text    = TextCol,
    subtext = SubText,
    muted   = MutedText,
    isLight = false
)

val LightAppColors = AppColors(
    bg      = LightBg,
    card    = LightCard,
    card2   = LightCard2,
    card3   = LightCard,
    nav     = LightCard,
    text    = LightText,
    subtext = LightSub,
    muted   = LightSub,
    isLight = true
)

val LocalAppColors = compositionLocalOf { DarkAppColors }

private val DarkScheme = darkColorScheme(
    primary        = SupaGreen,
    secondary      = SupaTeal,
    background     = BgBlack,
    surface        = CardBg,
    onPrimary      = Color.Black,
    onSecondary    = Color.Black,
    onBackground   = TextCol,
    onSurface      = TextCol,
    error          = ErrorCol,
    surfaceVariant = CardBg2
)

private val LightScheme = lightColorScheme(
    primary      = SupaGreenDim,
    secondary    = SupaTeal,
    background   = LightBg,
    surface      = LightCard,
    onPrimary    = Color.White,
    onSecondary  = Color.White,
    onBackground = LightText,
    onSurface    = LightText,
    error        = ErrorCol
)

@Composable
fun SupaAdminTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography  = Typography
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                color    = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    }
}
