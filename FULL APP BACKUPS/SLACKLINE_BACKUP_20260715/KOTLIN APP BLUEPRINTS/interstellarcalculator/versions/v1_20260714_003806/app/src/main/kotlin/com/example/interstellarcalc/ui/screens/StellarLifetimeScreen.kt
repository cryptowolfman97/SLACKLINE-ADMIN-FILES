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

private data class StarPreset(val name: String, val massSolar: Double)
private val STAR_PRESETS = listOf(
    StarPreset("Red Dwarf (0.1 M☉)",  0.1),
    StarPreset("Sun (1 M☉)",          1.0),
    StarPreset("Sirius (2 M☉)",       2.0),
    StarPreset("Vega (2.1 M☉)",       2.14),
    StarPreset("Rigel (21 M☉)",       21.0),
    StarPreset("Eta Car (100 M☉)",    100.0),
    StarPreset("Custom",              0.0),
)

@Composable
fun StellarLifetimeScreen(navController: NavController) {
    var selPreset  by remember { mutableStateOf("Sun (1 M☉)") }
    var massInput  by remember { mutableStateOf("1.0") }
    var result     by remember { mutableStateOf<StellarLifetimeResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Stellar Lifetime", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Star Mass Presets") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    STAR_PRESETS.forEach { p ->
                        FilterChip(
                            selected = selPreset == p.name,
                            onClick  = { selPreset = p.name; if (p.name != "Custom") massInput = p.massSolar.toString() },
                            label  = { Text(p.name, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Star mass", massInput, { massInput = it; selPreset = "Custom" }, "M☉", "Solar masses (e.g. 1.0 = our Sun)")
            }
            CalculateButton {
                val m = massInput.toDoubleOrNull() ?: return@CalculateButton
                if (m > 0) result = computeStellarLifetime(m)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Stellar Properties") {
                        ResultCard("Main Sequence Lifetime", formatTime(r.lifetimeYears), accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Spectral Class",  r.spectralClass,  modifier = Modifier.weight(1f))
                            ResultCard("Stellar End",     r.endState,        modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Luminosity vs Sun", "${"%.2e".format(r.luminositySolar)} L☉", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val massD = massInput.toDoubleOrNull() ?: 1.0
                        val context = when {
                            massD < 0.3  -> "This red dwarf burns so slowly it will outlive the current age of the universe many times over."
                            massD < 1.2  -> "A Sun-like star with a comfortable long life — plenty of time for planetary systems to develop complex life."
                            massD < 5    -> "A moderately massive star that burns bright and dies young by cosmic standards."
                            massD < 20   -> "A massive star — brilliant but short-lived. It will end in a spectacular supernova."
                            else         -> "An extreme hypergiant burning through its fuel at a ferocious rate. Cosmically speaking, it lives for an eyeblink."
                        }
                        ResultCard("Perspective", context, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
