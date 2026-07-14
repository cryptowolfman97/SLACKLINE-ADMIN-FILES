package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.physics.*
import com.example.interstellarcalc.ui.components.*
import kotlin.math.pow

private data class Destination(val name: String, val ly: Double)
private val DESTINATIONS = listOf(
    Destination("Proxima Cen", 4.246),
    Destination("Alpha Cen",   4.37),
    Destination("Tau Ceti",    11.91),
    Destination("Vega",        25.04),
    Destination("Betelgeuse",  500.0),
    Destination("Gal. Center", 26000.0),
    Destination("Andromeda",   2537000.0),
    Destination("Custom",      0.0),
)
private val G_PRESETS = listOf(0.1, 0.3, 1.0, 1.5, 2.0, 4.0, 10.0)

@Composable
fun RelativisticRocketScreen(navController: NavController) {
    var distInput  by remember { mutableStateOf("11.91") }
    var gInput     by remember { mutableStateOf("1.5") }
    var brach      by remember { mutableStateOf(true) }
    var coastPct   by remember { mutableFloatStateOf(0f) }
    var dryMass    by remember { mutableStateOf("100000") }
    var exhaustPct by remember { mutableFloatStateOf(100f) }
    var selDest    by remember { mutableStateOf("Tau Ceti") }
    var result     by remember { mutableStateOf<RocketResult?>(null) }
    var adjRatio   by remember { mutableStateOf<Double?>(null) }

    fun calculate() {
        val dist  = distInput.toDoubleOrNull() ?: return
        val accel = gInput.toDoubleOrNull() ?: return
        if (dist <= 0 || accel <= 0) return
        val eff = (exhaustPct / 100.0).coerceIn(0.01, 1.0)
        val r   = computeRocket(dist, accel, brach, coastPct / 100.0)
        result    = r
        adjRatio  = (if (brach) r.massRatioBrach else r.massRatioOneWay).pow(1.0 / eff)
    }

    Scaffold(topBar = { CalcTopBar("Relativistic Rocket", navController) }) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Destination") {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DESTINATIONS.forEach { d ->
                        FilterChip(
                            selected = selDest == d.name,
                            onClick  = { selDest = d.name; if (d.name != "Custom") distInput = d.ly.toString() },
                            label    = { Text(d.name, style = MaterialTheme.typography.labelLarge) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Distance", distInput, { distInput = it; selDest = "Custom" }, "ly", "light-years")
            }

            SectionCard("Flight Mode") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to "Brachistochrone", false to "Fly-by").forEach { (b, label) ->
                        Button(
                            onClick  = { brach = b },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (brach == b) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                contentColor   = if (brach == b) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                if (brach) {
                    Spacer(Modifier.height(10.dp))
                    Text("Coast phase: ${coastPct.toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = coastPct, onValueChange = { coastPct = it }, valueRange = 0f..99f, steps = 98)
                }
            }

            SectionCard("Acceleration") {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    G_PRESETS.forEach { g ->
                        FilterChip(
                            selected = (gInput.toDoubleOrNull() ?: 0.0) == g,
                            onClick  = { gInput = g.toString() },
                            label    = { Text("${g}g", style = MaterialTheme.typography.labelLarge) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Custom acceleration", gInput, { gInput = it }, "g", "e.g. 1.5")
            }

            SectionCard("Propulsion") {
                Text("Exhaust velocity: ${exhaustPct.toInt()}% of c", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = exhaustPct, onValueChange = { exhaustPct = it }, valueRange = 1f..100f, steps = 98)
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Ship dry mass", dryMass, { dryMass = it }, "kg", "100000")
            }

            CalculateButton(onClick = { calculate() })

            AnimatedVisibility(visible = result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionCard("Journey Results") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ResultCard("Ship Time",  formatTime(r.shipTimeYears),  accent = true,  modifier = Modifier.weight(1f))
                                ResultCard("Earth Time", formatTime(r.earthTimeYears), accent = false, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ResultCard("Peak Velocity", "${(r.peakBeta*100).let { if(it>99.99) "99.99+" else "%.2f".format(it) }}% c", modifier = Modifier.weight(1f))
                                ResultCard("Lorentz Factor", r.peakGamma.let { if(it<10) "%.3f".format(it) else "%.2e".format(it) }, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            ResultCard("Time Dilation", r.dilationRatio.let { "%.2fx  (1 yr aboard = %.1f yr on Earth)".format(it, it) }, modifier = Modifier.fillMaxWidth())
                        }
                        adjRatio?.let { ar ->
                            SectionCard("Fuel & Mass") {
                                ResultCard("Mass Ratio", if (ar < 1e6) "%.1f : 1".format(ar) else "%.2e : 1".format(ar), accent = true, modifier = Modifier.fillMaxWidth())
                                val dm = dryMass.toDoubleOrNull() ?: 0.0
                                if (dm > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    val fuelKg = dm * (ar - 1)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ResultCard("Fuel Required", formatMass(fuelKg), accent = true, modifier = Modifier.weight(1f))
                                        ResultCard("Launch Mass",   formatMass(dm * ar), modifier = Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    ResultCard("Fuel Energy (mc2)", formatEnergy(fuelKg * C_SQ), modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
