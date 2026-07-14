package com.example.slacklineadminapp.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color, val card: Color, val card2: Color,
    val nav: Color, val text: Color, val subtext: Color, val isLight: Boolean
)

val DarkAppColors  = AppColors(BgBlack, CardBg, CardBg2, NavBg, TextCol, SubText, false)
val LightAppColors = AppColors(LightBg, LightCard, LightCard2, LightNav, LightText, SubText, true)
val LocalAppColors = compositionLocalOf { DarkAppColors }

private val DarkScheme = darkColorScheme(
    primary = TealCol, secondary = CyanCol,
    background = BgBlack, surface = CardBg,
    onBackground = TextCol, onSurface = TextCol
)
private val LightScheme = lightColorScheme(
    primary = TealCol, secondary = CyanCol,
    background = LightBg, surface = LightCard,
    onBackground = LightText, onSurface = LightText
)

@Composable
fun SlackLineTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),  // ← Fixes status bar (top) + nav bar (bottom) globally
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    }
}

@Composable
fun ComposeEmptyActivityTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) =
    SlackLineTheme(darkTheme, content)