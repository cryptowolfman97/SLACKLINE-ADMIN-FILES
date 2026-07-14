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

private data class LinePreset(val name: String, val emitted: Double, val label: String)
private val LINE_PRESETS = listOf(
    LinePreset("Hydrogen-α (Hα)",   656.3,  "Red hydrogen line"),
    LinePreset("Hydrogen-β (Hβ)",   486.1,  "Blue-green hydrogen"),
    LinePreset("Calcium K",         393.4,  "Ultraviolet calcium"),
    LinePreset("Sodium D",          589.3,  "Yellow sodium"),
    LinePreset("Oxygen [OIII]",     500.7,  "Green oxygen nebula"),
    LinePreset("Custom",            0.0,    ""),
)

@Composable
fun RedshiftScreen(navController: NavController) {
    var selLine      by remember { mutableStateOf("Hydrogen-α (Hα)") }
    var emittedInput by remember { mutableStateOf("656.3") }
    var observedInput by remember { mutableStateOf("") }
    var result       by remember { mutableStateOf<RedshiftResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Redshift Calculator", navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Spectral Line") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LINE_PRESETS.forEach { p ->
                        FilterChip(
                            selected = selLine == p.name,
                            onClick  = { selLine = p.name; if (p.name != "Custom") emittedInput = p.emitted.toString() },
                            label  = { Text(p.name, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledTextField("Emitted wavelength (rest frame)", emittedInput, { emittedInput = it; selLine = "Custom" }, "nm")
            }
            SectionCard("Observed Wavelength") {
                LabeledTextField("Observed wavelength (measured)", observedInput, { observedInput = it }, "nm", "Enter the wavelength you measured")
                val em = emittedInput.toDoubleOrNull()
                val ob = observedInput.toDoubleOrNull()
                if (em != null && ob != null && em > 0) {
                    val zPreview = (ob - em) / em
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Preview: z = ${"%.4f".format(zPreview)} (${if (zPreview > 0) "redshift — receding" else "blueshift — approaching"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CalculateButton {
                val em = emittedInput.toDoubleOrNull()  ?: return@CalculateButton
                val ob = observedInput.toDoubleOrNull() ?: return@CalculateButton
                if (em > 0 && ob > 0) result = computeRedshift(em, ob)
            }
            AnimatedVisibility(result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Redshift Results") {
                        ResultCard("Redshift (z)", "%.6f".format(r.z), accent = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Recession Speed", formatVelocityMs(r.recessionalVelocityMs), accent = true, modifier = Modifier.weight(1f))
                            ResultCard("As % of c", "${"%.4f".format(r.recessionalVelocityC * 100)}%", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        if (r.z > 0) {
                            ResultCard("Hubble Distance Estimate", "${"%.1f".format(r.distanceMly)} Mly", modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                        }
                        val context = when {
                            r.z < 0      -> "Blueshift — this object is moving toward us. Only seen in nearby galaxies within our Local Group."
                            r.z < 0.01   -> "Very low redshift — a nearby galaxy, likely within our Local Supercluster."
                            r.z < 0.1    -> "Moderate redshift — hundreds of millions of light-years away in the local universe."
                            r.z < 1.0    -> "Significant redshift — billions of light-years away, seeing the universe as it was long ago."
                            r.z < 3.0    -> "High redshift — an ancient quasar or galaxy from the early universe."
                            else         -> "Extreme redshift — this object existed when the universe was very young."
                        }
                        ResultCard("Interpretation", context, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
