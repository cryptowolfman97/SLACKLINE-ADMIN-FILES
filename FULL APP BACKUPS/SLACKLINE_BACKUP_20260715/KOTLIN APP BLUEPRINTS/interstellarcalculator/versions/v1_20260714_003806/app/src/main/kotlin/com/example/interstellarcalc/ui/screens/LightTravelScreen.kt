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

private data class DistPreset(val name: String, val metres: Double)
private val DIST_PRESETS = listOf(
    DistPreset("Earth–Moon",       3.844e8),
    DistPreset("Earth–Sun (1 AU)", 1.496e11),
    DistPreset("Sun–Mars",         2.279e11),
    DistPreset("Sun–Jupiter",      7.783e11),
    DistPreset("Sun–Neptune",      4.495e12),
    DistPreset("1 Light Year",     9.461e15),
    DistPreset("Proxima Cen",      4.0175e16),
    DistPreset("Andromeda Galaxy", 2.537e6 * 9.461e15),
    DistPreset("Custom",           0.0),
)

@Composable
fun LightTravelScreen(navController: NavController) {
    var selPreset by remember { mutableStateOf("Earth–Sun (1 AU)") }
    var distInput by remember { mutableStateOf("1.496e11") }
    var result    by remember { mutableStateOf<LightTravelResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Light Travel Time", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Distance Presets") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DIST_PRESETS.forEach { p ->
                        FilterChip(
                            selected = selPreset == p.name,
                            onClick  = { selPreset = p.name; if (p.name != "Custom") distInput = p.metres.toString() },
                            label  = { Text(p.name, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Distance", distInput, { distInput = it; selPreset = "Custom" }, "m", "Enter distance in metres")
            }
            CalculateButton {
                val d = distInput.toDoubleOrNull() ?: return@CalculateButton
                if (d > 0) result = computeLightTravel(d)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Light Travel Time") {
                        ResultCard("Light Seconds", "${"%.4f".format(r.lightSeconds)} s", accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Light Minutes", "${"%.4f".format(r.lightMinutes)} min", modifier = Modifier.weight(1f))
                            ResultCard("Light Hours",   "${"%.4f".format(r.lightHours)} hr", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Light Years", "${"%.6f".format(r.lightYears)} ly", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val context = when {
                            r.lightSeconds < 2    -> "This is the distance signals travel in under 2 seconds — almost instant communication."
                            r.lightMinutes < 1    -> "Light covers this in ${r.lightSeconds.toInt()} seconds. Radio signals to the Moon take about 1.3 s."
                            r.lightMinutes < 60   -> "Light takes ${r.lightMinutes.toInt()} minutes — any signal sent now won't arrive for that long."
                            r.lightHours < 24     -> "At ${r.lightHours.toInt()} light-hours, this rivals the size of our solar system."
                            r.lightYears < 10     -> "This is stellar territory — ${r.lightYears.fmt(2)} light-years away, a cosmic neighbour."
                            else                  -> "At ${r.lightYears.fmt(0)} light-years, you are looking deep into the cosmos."
                        }
                        ResultCard("Context", context, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
private fun Double.fmt(d: Int) = "%.${d}f".format(this)
