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

private data class TransferPreset(val name: String, val cm: Double, val r1: Double, val r2: Double)
private val PRESETS = listOf(
    TransferPreset("LEO to GEO",    5.972e24, 6.571e6,  4.2164e7),
    TransferPreset("Earth to Mars", 1.989e30, 1.496e11, 2.279e11),
    TransferPreset("Earth to Moon", 5.972e24, 6.571e6,  3.844e8),
    TransferPreset("Custom",        0.0, 0.0, 0.0),
)

@Composable
fun HohmannScreen(navController: NavController) {
    var sel      by remember { mutableStateOf("LEO to GEO") }
    var centralM by remember { mutableStateOf("5.972e24") }
    var r1       by remember { mutableStateOf("6571000") }
    var r2       by remember { mutableStateOf("42164000") }
    var result   by remember { mutableStateOf<HohmannResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Hohmann Transfer", navController) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Preset Transfers") {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { p ->
                        FilterChip(
                            selected = sel == p.name,
                            onClick  = { sel = p.name; if (p.name != "Custom") { centralM = p.cm.toString(); r1 = p.r1.toString(); r2 = p.r2.toString() } },
                            label    = { Text(p.name, style = MaterialTheme.typography.labelLarge) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
            }
            SectionCard("Parameters") {
                LabeledTextField("Central body mass",        centralM, { centralM = it; sel = "Custom" }, "kg")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Initial orbit radius (r1)", r1,      { r1 = it;       sel = "Custom" }, "m")
                Spacer(Modifier.height(8.dp))
                LabeledTextField("Target orbit radius (r2)",  r2,      { r2 = it;       sel = "Custom" }, "m")
            }
            CalculateButton {
                val cM  = centralM.toDoubleOrNull() ?: return@CalculateButton
                val r1D = r1.toDoubleOrNull()       ?: return@CalculateButton
                val r2D = r2.toDoubleOrNull()       ?: return@CalculateButton
                if (cM > 0 && r1D > 0 && r2D > 0 && r1D != r2D) result = computeHohmann(cM, r1D, r2D)
            }
            AnimatedVisibility(visible = result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Results") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("dv1 (1st burn)", formatVelocityMs(r.dv1Ms), accent = true, modifier = Modifier.weight(1f))
                            ResultCard("dv2 (2nd burn)", formatVelocityMs(r.dv2Ms), modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Total dv",      formatVelocityMs(r.totalDvMs),  accent = true, modifier = Modifier.weight(1f))
                            ResultCard("Transfer Time", formatSeconds(r.transferTimeS), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
