package com.shvertex.universalconv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SHVTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.6.sp),
)
