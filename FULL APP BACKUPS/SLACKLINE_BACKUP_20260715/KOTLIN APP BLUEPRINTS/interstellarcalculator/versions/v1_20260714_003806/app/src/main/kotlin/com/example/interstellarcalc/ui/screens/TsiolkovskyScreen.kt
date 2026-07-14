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
fun TsiolkovskyScreen(navController: NavController) {
    var mode    by remember { mutableStateOf("Find DeltaV") }
    var ve      by remember { mutableStateOf("4500") }
    var wetMass by remember { mutableStateOf("300000") }
    var dryMass by remember { mutableStateOf("100000") }
    var deltaV  by remember { mutableStateOf("9500") }
    var result  by remember { mutableStateOf<TsiolkovskyResult?>(null) }

    Scaffold(topBar = { CalcTopBar("Tsiolkovsky Rocket", navController) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Solve For") {
                ChipGroup(listOf("Find DeltaV", "Find Wet Mass"), mode) { mode = it; result = null }
            }
            SectionCard("Engine") {
                LabeledTextField("Exhaust velocity", ve, { ve = it }, "m/s", "e.g. 4500")
            }
            SectionCard("Mass") {
                if (mode == "Find DeltaV") {
                    LabeledTextField("Wet mass (total)", wetMass, { wetMass = it }, "kg")
                    Spacer(Modifier.height(8.dp))
                    LabeledTextField("Dry mass",         dryMass, { dryMass = it }, "kg")
                } else {
                    LabeledTextField("Desired delta-V", deltaV,  { deltaV  = it }, "m/s")
                    Spacer(Modifier.height(8.dp))
                    LabeledTextField("Dry mass",        dryMass, { dryMass = it }, "kg")
                }
            }
            CalculateButton {
                val veD  = ve.toDoubleOrNull()      ?: return@CalculateButton
                val dryD = dryMass.toDoubleOrNull() ?: return@CalculateButton
                result = if (mode == "Find DeltaV") {
                    val wetD = wetMass.toDoubleOrNull() ?: return@CalculateButton
                    if (veD > 0 && wetD > dryD && dryD > 0) computeTsiolkovsky(veD, wetD, dryD) else null
                } else {
                    val dvD = deltaV.toDoubleOrNull() ?: return@CalculateButton
                    if (veD > 0 && dvD > 0 && dryD > 0) computeTsiolkovskyInverse(veD, dvD, dryD) else null
                }
            }
            AnimatedVisibility(visible = result != null, enter = fadeIn(tween(400)) + expandVertically(tween(400))) {
                result?.let { r ->
                    SectionCard("Results") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultCard("Delta-V",    formatVelocityMs(r.deltaVMs),         accent = true, modifier = Modifier.weight(1f))
                            ResultCard("Mass Ratio", "%.2f : 1".format(r.massRatio),       modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ResultCard("Fuel Mass", formatMass(r.fuelMassKg), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
