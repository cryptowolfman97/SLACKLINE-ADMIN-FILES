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

private data class EscBody(val name: String, val massKg: Double, val radiusM: Double)
private val ESC_BODIES = listOf(
    EscBody("Moon",         7.342e22,    1.737e6),
    EscBody("Earth",        5.972e24,    6.371e6),
    EscBody("Mars",         6.417e23,    3.390e6),
    EscBody("Jupiter",      1.898e27,    6.991e7),
    EscBody("Sun",          1.989e30,    6.957e8),
    EscBody("White Dwarf",  1.989e30,    7.0e6),
    EscBody("Neutron Star", 2.0e30,      1.0e4),
    EscBody("Custom",       0.0,         0.0),
)

@Composable
fun EscapeVelocityScreen(navController: NavController) {
    var selBody     by remember { mutableStateOf("Earth") }
    var massInput   by remember { mutableStateOf("5.972e24") }
    var radiusInput by remember { mutableStateOf("6371000") }
    var altInput    by remember { mutableStateOf("0") }
    var result      by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) }

    Scaffold(topBar = { CalcTopBar("Escape Velocity", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Body Presets") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ESC_BODIES.forEach { b ->
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
                LabeledTextField("Mass",   massInput,   { massInput   = it; selBody = "Custom" }, "kg")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Radius", radiusInput, { radiusInput = it; selBody = "Custom" }, "m")
            }
            SectionCard("Altitude") {
                LabeledTextField("Altitude above surface", altInput, { altInput = it }, "m", "0 = surface")
            }
            CalculateButton {
                val m   = massInput.toDoubleOrNull()   ?: return@CalculateButton
                val r   = radiusInput.toDoubleOrNull() ?: return@CalculateButton
                val alt = altInput.toDoubleOrNull()    ?: 0.0
                if (m > 0 && r > 0) {
                    val ve       = escapeVelocity(m, r + alt)
                    val vOrbit   = kotlin.math.sqrt(6.674e-11 * m / (r + alt))
                    val rs       = schwarzschildRadius(m)
                    result = Triple(ve, vOrbit, rs)
                }
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { (ve, vo, rs) ->
                    SectionCard("Results") {
                        ResultCard("Escape Velocity", formatVelocityMs(ve), accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Orbital Velocity", formatVelocityMs(vo), modifier = Modifier.weight(1f))
                            ResultCard("Schwarzschild r",  formatMeters(rs), modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        val fracC = ve / C_MS * 100
                        ResultCard("As fraction of c", "${"%.4f".format(fracC)}% of light speed", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val context = when {
                            fracC > 99   -> "Effectively a black hole — escape velocity exceeds the speed of light at this radius."
                            fracC > 10   -> "Extreme gravity — only exotic compact objects like neutron stars reach this."
                            fracC > 1    -> "Very strong gravity. This rivals white dwarfs and the Sun's surface."
                            else         -> "Reachable with chemical rockets. Earth's escape velocity is about 11.2 km/s."
                        }
                        ResultCard("Context", context, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
