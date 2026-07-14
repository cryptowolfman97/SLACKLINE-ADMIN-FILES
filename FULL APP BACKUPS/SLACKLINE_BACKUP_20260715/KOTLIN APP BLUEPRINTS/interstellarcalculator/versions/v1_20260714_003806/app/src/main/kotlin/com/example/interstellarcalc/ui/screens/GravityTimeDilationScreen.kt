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

private data class GravBody(val name: String, val massKg: Double, val radiusM: Double)
private val GRAV_BODIES = listOf(
    GravBody("Earth",        5.972e24,       6.371e6),
    GravBody("Neutron Star", 2.0 * 1.989e30, 1.0e4),
    GravBody("Sun",          1.989e30,       6.957e8),
    GravBody("White Dwarf",  1.989e30,       7.0e6),
    GravBody("Custom",       0.0,            0.0),
)

@Composable
fun GravityTimeDilationScreen(navController: NavController) {
    var selBody  by remember { mutableStateOf("Earth") }
    var massIn   by remember { mutableStateOf("5.972e24") }
    var radIn    by remember { mutableStateOf("6371000") }
    var heightIn by remember { mutableStateOf("10000") }
    var dilation by remember { mutableStateOf<Double?>(null) }

    Scaffold(topBar = { CalcTopBar("Gravity Time Dilation", navController) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Gravitational Body") {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GRAV_BODIES.forEach { b ->
                        FilterChip(
                            selected = selBody == b.name,
                            onClick  = { selBody = b.name; if (b.name != "Custom") { massIn = b.massKg.toString(); radIn = b.radiusM.toString() } },
                            label    = { Text(b.name, style = MaterialTheme.typography.labelLarge) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Mass",   massIn, { massIn  = it; selBody = "Custom" }, "kg")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Radius", radIn,  { radIn   = it; selBody = "Custom" }, "m")
            }
            SectionCard("Observer Height") {
                LabeledTextField("Height above surface", heightIn, { heightIn = it }, "m", "e.g. 10000")
                val mass = massIn.toDoubleOrNull() ?: 0.0
                if (mass > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Schwarzschild radius: ${formatMeters(schwarzschildRadius(mass))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CalculateButton {
                val mass   = massIn.toDoubleOrNull()   ?: return@CalculateButton
                val rad    = radIn.toDoubleOrNull()    ?: return@CalculateButton
                val height = heightIn.toDoubleOrNull() ?: return@CalculateButton
                val rs     = schwarzschildRadius(mass)
                if (mass > 0 && rad > rs && height >= 0) dilation = gravitationalTimeDilation(mass, rad, height)
            }
            AnimatedVisibility(visible = dilation != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                dilation?.let { d ->
                    SectionCard("Results") {
                        ResultCard("Clock ratio (surface / height)", "%.8f".format(d), accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val usPerDay = (1.0 - 1.0 / d) * 86400 * 1e6
                        ResultCard("Time lost at surface per day", if (usPerDay.isFinite()) "%.4f us".format(usPerDay) else "---", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val mass = massIn.toDoubleOrNull() ?: 0.0
                        val rad  = radIn.toDoubleOrNull()  ?: 0.0
                        if (mass > 0 && rad > 0) ResultCard("Escape Velocity", formatVelocityMs(escapeVelocity(mass, rad)), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
