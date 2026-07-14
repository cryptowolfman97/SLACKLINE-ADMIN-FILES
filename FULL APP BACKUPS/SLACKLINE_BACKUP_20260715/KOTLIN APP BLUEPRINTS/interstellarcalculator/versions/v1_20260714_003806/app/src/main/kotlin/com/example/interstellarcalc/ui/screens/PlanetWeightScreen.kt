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

private data class WeightBody(val name: String, val massKg: Double, val radiusM: Double)
private val WEIGHT_BODIES = listOf(
    WeightBody("Mercury",      3.285e23,  2.440e6),
    WeightBody("Venus",        4.867e24,  6.052e6),
    WeightBody("Earth",        5.972e24,  6.371e6),
    WeightBody("Moon",         7.342e22,  1.737e6),
    WeightBody("Mars",         6.417e23,  3.390e6),
    WeightBody("Jupiter",      1.898e27,  6.991e7),
    WeightBody("Saturn",       5.683e26,  5.823e7),
    WeightBody("Uranus",       8.681e25,  2.536e7),
    WeightBody("Neptune",      1.024e26,  2.462e7),
    WeightBody("Pluto",        1.309e22,  1.188e6),
    WeightBody("Sun",          1.989e30,  6.957e8),
    WeightBody("Neutron Star", 2.0e30,    1.0e4),
    WeightBody("White Dwarf",  1.989e30,  7.0e6),
    WeightBody("Custom",       0.0,       0.0),
)

@Composable
fun PlanetWeightScreen(navController: NavController) {
    var selBody      by remember { mutableStateOf("Earth") }
    var massInput    by remember { mutableStateOf("5.972e24") }
    var radiusInput  by remember { mutableStateOf("6371000") }
    var personInput  by remember { mutableStateOf("70") }
    var result       by remember { mutableStateOf<PlanetWeightResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Planet Weight Calculator", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Select Body") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WEIGHT_BODIES.forEach { b ->
                        FilterChip(
                            selected = selBody == b.name,
                            onClick  = {
                                selBody = b.name
                                if (b.name != "Custom") { massInput = b.massKg.toString(); radiusInput = b.radiusM.toString() }
                            },
                            label  = { Text(b.name, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Body mass",   massInput,   { massInput   = it; selBody = "Custom" }, "kg")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Body radius", radiusInput, { radiusInput = it; selBody = "Custom" }, "m")
            }
            SectionCard("Your Mass on Earth") {
                LabeledTextField("Your mass", personInput, { personInput = it }, "kg", "e.g. 70")
            }
            CalculateButton {
                val bMass  = massInput.toDoubleOrNull()   ?: return@CalculateButton
                val bRad   = radiusInput.toDoubleOrNull() ?: return@CalculateButton
                val pMass  = personInput.toDoubleOrNull() ?: return@CalculateButton
                if (bMass > 0 && bRad > 0 && pMass > 0) result = computePlanetWeight(bMass, bRad, pMass)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Results on $selBody") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Surface Gravity", "${"%.3f".format(r.surfaceGravityMs2)} m/s²", accent = true, modifier = Modifier.weight(1f))
                            ResultCard("vs Earth (1g)", "${"%.3f".format(r.relativeToEarth)}× g", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Your Weight", "${"%.1f".format(r.weightNewtons)} N", accent = true, modifier = Modifier.weight(1f))
                            ResultCard("Effective Mass", "${"%.1f".format(r.weightNewtons / 9.80665)} kg-eq", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        val desc = when {
                            r.relativeToEarth < 0.2  -> "You'd feel almost weightless — a feather-light existence."
                            r.relativeToEarth < 0.8  -> "You'd feel noticeably lighter, like a reduced-gravity workout."
                            r.relativeToEarth < 1.2  -> "Almost identical to Earth — barely noticeable difference."
                            r.relativeToEarth < 3.0  -> "Crushing — every step would require significant effort."
                            else                     -> "Extreme — you'd be unable to stand. The gravity would be lethal."
                        }
                        ResultCard("Experience", desc, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
