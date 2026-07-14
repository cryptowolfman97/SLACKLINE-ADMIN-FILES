package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.physics.*
import com.example.interstellarcalc.ui.components.*

private data class MassPreset(val name: String, val massKg: Double)
private val MASS_PRESETS = listOf(
    MassPreset("Sun",              1.989e30),
    MassPreset("Sgr A* (4.1M M☉)", 4.1e6 * 1.989e30),
    MassPreset("M87* (6.5B M☉)",  6.5e9 * 1.989e30),
    MassPreset("TON 618 (66B M☉)",66e9  * 1.989e30),
    MassPreset("Earth",            5.972e24),
    MassPreset("Human (70 kg)",    70.0),
    MassPreset("Custom",           0.0),
)

@Composable
fun SchwarzschildScreen(navController: NavController) {
    var selPreset by remember { mutableStateOf("Sun") }
    var massInput by remember { mutableStateOf("1.989e30") }
    var result    by remember { mutableStateOf<SchwarzschildResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Schwarzschild Radius", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Mass Presets") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MASS_PRESETS.forEach { p ->
                        FilterChip(
                            selected = selPreset == p.name,
                            onClick  = { selPreset = p.name; if (p.name != "Custom") massInput = p.massKg.toString() },
                            label  = { Text(p.name, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Mass", massInput, { massInput = it; selPreset = "Custom" }, "kg", "e.g. 1.989e30 for 1 solar mass")
            }
            CalculateButton {
                val m = massInput.toDoubleOrNull() ?: return@CalculateButton
                if (m > 0) result = computeSchwarzschild(m)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Event Horizon") {
                        ResultCard("Schwarzschild Radius", formatMeters(r.radiusM), accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("In km", "${"%.4f".format(r.radiusKm)} km", modifier = Modifier.weight(1f))
                            ResultCard("In Solar Radii", "${"%.6f".format(r.inSolarRadii)} R☉", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Avg Density Inside", formatDensity(r.densityKgM3), modifier = Modifier.weight(1f))
                            ResultCard("Hawking Temperature",
                                if (r.hawkingTempK < 1e-3) "${"%.2e".format(r.hawkingTempK)} K"
                                else "${"%.4f".format(r.hawkingTempK)} K",
                                modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("What this means", "If all of $selPreset's mass were compressed to ${formatMeters(r.radiusM)} or less, it would become a black hole from which not even light can escape.", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatDensity(d: Double): String = when {
    d > 1e18 -> "${"%.2e".format(d)} kg/m³"
    d > 1e9  -> "${"%.2e".format(d)} kg/m³"
    else     -> "${"%.1f".format(d)} kg/m³"
}
