package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

@Composable
fun VelocityDilationScreen(navController: NavController) {
    var velocityInput  by remember { mutableStateOf("0.5") }
    var properTimeInput by remember { mutableStateOf("365") }
    var result         by remember { mutableStateOf<VelocityDilationResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Velocity Time Dilation", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Traveller Velocity") {
                LabeledTextField("Speed as fraction of c (0–0.9999)", velocityInput, { velocityInput = it }, "× c", "e.g. 0.5 = 50% light speed")
                val v = velocityInput.toDoubleOrNull()
                if (v != null && v in 0.0..0.9999) {
                    Spacer(Modifier.height(8.dp))
                    val gamma = 1.0 / kotlin.math.sqrt(1.0 - v * v)
                    Text("Lorentz factor (γ): ${"%.4f".format(gamma)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Velocity: ${(v * 100).fmt(2)}% of light speed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SectionCard("Traveller's Proper Time") {
                LabeledTextField("Time experienced by traveller", properTimeInput, { properTimeInput = it }, "days", "Days on the ship")
            }
            CalculateButton {
                val v = velocityInput.toDoubleOrNull() ?: return@CalculateButton
                val t = properTimeInput.toDoubleOrNull() ?: return@CalculateButton
                if (v in 0.0..0.9999 && t > 0) result = computeVelocityDilation(v, t)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Results") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Lorentz Factor (γ)", "%.6f".format(r.gamma), accent = true, modifier = Modifier.weight(1f))
                            ResultCard("Speed (v/c)", "${(r.betaFraction * 100).fmt(4)}%", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Ship Time", "${r.properTimeDays.fmt(2)} days", modifier = Modifier.weight(1f))
                            ResultCard("Earth Time", "${r.coordinateTimeDays.fmt(2)} days", accent = true, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Extra time on Earth vs ship", "${r.timeDiffDays.fmt(2)} days (${formatTime(r.timeDiffDays / 365.25)})", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Explanation", "After ${r.properTimeDays.fmt(0)} days for the traveller, ${r.coordinateTimeDays.fmt(1)} days have passed on Earth. The traveller ages ${r.timeDiffDays.fmt(1)} fewer days than people back home.", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
private fun Double.fmt(d: Int) = "%.${d}f".format(this)
