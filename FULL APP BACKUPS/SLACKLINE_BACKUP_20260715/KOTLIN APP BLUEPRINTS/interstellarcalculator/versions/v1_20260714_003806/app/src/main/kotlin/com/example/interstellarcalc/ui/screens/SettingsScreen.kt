package com.example.interstellarcalc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.ui.components.CalcTopBar
import com.example.interstellarcalc.ui.theme.AppTheme

private data class ThemeOption(val theme: AppTheme, val label: String, val description: String, val swatch: Color)
private val THEME_OPTIONS = listOf(
    ThemeOption(AppTheme.AMOLED, "AMOLED Black", "True black + green accent",         Color(0xFF000000)),
    ThemeOption(AppTheme.DARK,   "Dark",          "Deep zinc + green accent",          Color(0xFF09090B)),
    ThemeOption(AppTheme.COSMIC, "Cosmic",        "Deep-space purple nebula palette",  Color(0xFF04030F)),
    ThemeOption(AppTheme.LIGHT,  "Light",         "Clean white + green accent",        Color(0xFFF8FAFC)),
)

@Composable
fun SettingsScreen(navController: NavController, currentTheme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    Scaffold(topBar = { CalcTopBar("Settings", navController) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            THEME_OPTIONS.forEach { opt ->
                val isSelected = currentTheme == opt.theme
                OutlinedCard(
                    onClick  = { onThemeChange(opt.theme) },
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    colors   = CardDefaults.outlinedCardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = opt.swatch, tonalElevation = 0.dp) {}
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.label,       style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(opt.description, style = MaterialTheme.typography.bodySmall,   color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) RadioButton(selected = true, onClick = null)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Interstellar Calc+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("by SHV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Version 1.0 - Phase 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Calculators: Relativistic Rocket, Orbital Mechanics, Tsiolkovsky, Hohmann Transfer, Gravity Time Dilation",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
