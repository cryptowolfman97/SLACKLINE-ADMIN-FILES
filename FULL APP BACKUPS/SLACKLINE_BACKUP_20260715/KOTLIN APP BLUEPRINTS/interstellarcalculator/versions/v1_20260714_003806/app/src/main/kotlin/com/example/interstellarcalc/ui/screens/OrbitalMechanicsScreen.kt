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

private data class Body(val name: String, val massKg: Double, val radiusM: Double)
private val BODIES = listOf(
    Body("Earth",   5.972e24, 6.371e6),
    Body("Mars",    6.417e23, 3.390e6),
    Body("Moon",    7.342e22, 1.737e6),
    Body("Jupiter", 1.898e27, 6.991e7),
    Body("Sun",     1.989e30, 6.957e8),
    Body("Custom",  0.0, 0.0),
)

@Composable
fun OrbitalMechanicsScreen(navController: NavController) {
    var selBody   by remember { mutableStateOf("Earth") }
    var massInput by remember { mutableStateOf("5.972e24") }
    var radInput  by remember { mutableStateOf("6371000") }
    var altInput  by remember { mutableStateOf("400000") }
    var result    by remember { mutableStateOf<OrbitalResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Orbital Mechanics", navController) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Central Body") {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BODIES.forEach { b ->
                        FilterChip(
                            selected = selBody == b.name,
                            onClick  = { selBody = b.name; if (b.name != "Custom") { massInput = b.massKg.toString(); radInput = b.radiusM.toString() } },
                            label    = { Text(b.name, style = MaterialTheme.typography.labelLarge) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Body mass",   massInput, { massInput = it; selBody = "Custom" }, "kg")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Body radius", radInput,  { radInput  = it; selBody = "Custom" }, "m")
            }
            SectionCard("Orbit Parameters") {
                LabeledTextField("Altitude above surface", altInput, { altInput = it }, "m", "e.g. 400000")
            }
            CalculateButton {
                val m = massInput.toDoubleOrNull() ?: return@CalculateButton
                val r = radInput.toDoubleOrNull()  ?: return@CalculateButton
                val a = altInput.toDoubleOrNull()  ?: return@CalculateButton
                if (m > 0 && r > 0 && a >= 0) result = computeOrbit(m, r, a)
            }
            AnimatedVisibility(visible = result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Results") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Orbital Velocity", formatVelocityMs(r.orbitalVelocityMs), accent = true,  modifier = Modifier.weight(1f))
                            ResultCard("Escape Velocity",  formatVelocityMs(r.escapeVelocityMs),  modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Orbital Period",       formatSeconds(r.periodSeconds),                                   modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Schwarzschild Radius", formatMeters(schwarzschildRadius(massInput.toDoubleOrNull() ?: 0.0)), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
