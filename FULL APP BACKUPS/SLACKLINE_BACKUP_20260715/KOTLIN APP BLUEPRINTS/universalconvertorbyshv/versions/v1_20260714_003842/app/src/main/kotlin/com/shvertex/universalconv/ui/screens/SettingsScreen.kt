package com.shvertex.universalconv.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.universalconv.ui.components.SHVTopBar
import com.shvertex.universalconv.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit) {
    Scaffold(
        topBar = { SHVTopBar("Settings", onBack = onBack) },
        containerColor = Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Theme section header
            Text(
                "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(2.dp))

            // AMOLED theme card (always active – single theme)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Teal.copy(alpha = 0.10f))
                    .border(1.5.dp, Teal.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Teal.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("AMOLED Dark", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Pure black – optimised for OLED screens", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = Teal, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.height(10.dp))

            // About section header
            Text(
                "ABOUT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(2.dp))

            // Features & About row
            SettingsRow(
                icon = Icons.Rounded.Info,
                iconTint = Blue,
                label = "Features & About",
                description = "All 45 modules, version info & credits",
                onClick = onAbout,
            )

            Spacer(Modifier.height(10.dp))

            // App info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Teal.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔄", fontSize = 20.sp)
                }
                Column {
                    Text("Universal Calculator by SHV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("v1.0  •  45 modules  •  Kotlin + Jetpack Compose", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("by SH Vertex Technologies", style = MaterialTheme.typography.bodySmall, color = Teal)
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

// ── About Screen ───────────────────────────────────────────────────
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { SHVTopBar("Features & About", onBack = onBack) },
        containerColor = Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Teal.copy(alpha = 0.09f))
                    .border(1.dp, Teal.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("🔄", fontSize = 36.sp)
                Text("Universal Calculator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Teal)
                Text("by SH Vertex Technologies", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("v1.0  •  45 modules  •  Kotlin + Jetpack Compose", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            // Feature sections
            val sections = listOf(
                Triple("Everyday Conversions", Teal, "Length, Weight, Temperature, Volume, Area, Time, Speed, Fuel Economy, Cooking, Clothing & Shoe Sizes"),
                Triple("Currency (Live)", Gold, "51 currencies with live mid-market rates, offline fallback, favourites, and swap-source"),
                Triple("Everyday Tools", Green, "Time Zones (28), Date & Age, Tip Calc, Discount/%, BMI/BMR, Shoe Sizes, Ring Sizes, Blood Glucose, Tire Decoder"),
                Triple("Science & Engineering", Color(0xFFFFCC33), "Pressure, Energy, Power, Torque, Force, Density, Flow Rate, Viscosity, Angle, Frequency, Radioactivity, Sound, Concentration, Radiation Dose, Humidity"),
                Triple("Electronics & Digital", Blue, "Electric Units (V/A/Ω), Magnetic Field, Data Storage, Typography, Color Codes (HEX/RGB/HSL/CMYK)"),
                Triple("Numeral Systems", Cyan, "Decimal, Binary, Hex, Octal, ASCII/Text, Roman Numerals — all bidirectional"),
                Triple("Light & Optics", Color(0xFFFFEE44), "Illuminance (lux/fc) and Luminance (cd/m²/nit/fL)"),
                Triple("Printing & Design", Purple, "Paper Sizes: A0–A7, B0–B5, Letter, Legal, Tabloid, Executive, Envelopes"),
            )

            sections.forEach { (title, col, body) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(col.copy(alpha = 0.07f))
                        .border(1.dp, col.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = col)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            // Home screen features
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Home Screen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                listOf(
                    "🔍  Full-text search across all 45 modules",
                    "⏱️  Recently used modules bar (last 6)",
                    "🗂️  Categorised grid with colour-coded tiles",
                    "⚡  Instant navigation — no loading screens",
                ).forEach { item ->
                    Text(item, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
