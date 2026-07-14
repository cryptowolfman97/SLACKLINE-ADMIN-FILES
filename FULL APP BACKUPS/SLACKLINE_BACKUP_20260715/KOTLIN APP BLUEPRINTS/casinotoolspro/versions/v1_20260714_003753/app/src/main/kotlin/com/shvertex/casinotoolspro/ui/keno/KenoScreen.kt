package com.shvertex.casinotoolspro.ui.keno

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.core.*
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class KenoViewModel : ViewModel() {
    private val _result  = MutableStateFlow<KenoMCResult?>(null)
    private val _running = MutableStateFlow(false)
    val result  = _result.asStateFlow()
    val running = _running.asStateFlow()

    fun run(config: KenoMCConfig) {
        _running.value = true
        _result.value  = null
        viewModelScope.launch(Dispatchers.Default) {
            _result.value  = KenoMonteCarloEngine.simulate(config)
            _running.value = false
        }
    }
}

@Composable
fun KenoMCScreen(onBack: () -> Unit) {
    val vm: KenoViewModel = viewModel()
    val result  by vm.result.collectAsState()
    val running by vm.running.collectAsState()

    var bankrollStr  by remember { mutableStateOf("1000") }
    var picksStr     by remember { mutableStateOf("5") }
    var minHitsStr   by remember { mutableStateOf("3") }
    var baseBetStr   by remember { mutableStateOf("1") }
    var lossIncStr   by remember { mutableStateOf("50") }
    var maxBetsStr   by remember { mutableStateOf("50") }
    var sessionsStr  by remember { mutableStateOf("3000") }

    val picks    = picksStr.toIntOrNull()?.coerceIn(1, 10) ?: 5
    val minHits  = minHitsStr.toIntOrNull()?.coerceIn(1, picks) ?: 3

    // Live probability preview
    val liveWinProb = remember(picks, minHits) {
        ProbabilityMath.kenoWinProbability(picks, minHits) * 100.0
    }
    val livePayout = remember(picks, minHits) {
        ProbabilityMath.kenoFairMultiplier(picks, minHits)
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Keno Monte Carlo", "Session simulator for keno strategies", CTPColors.Keno, onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live preview card
            CTPCard(accentColor = CTPColors.Keno, showAccent = false) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Win Chance", "%.2f%%".format(liveWinProb), CTPColors.Keno, Modifier.weight(1f))
                    StatChip("Payout", "%.2f×".format(livePayout), CTPColors.Green, Modifier.weight(1f))
                    StatChip("House Edge", "%.1f%%".format(6.0), CTPColors.Red, Modifier.weight(1f))
                }
            }

            // Inputs
            CTPCard(accentColor = CTPColors.Keno) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(baseBetStr,  { baseBetStr = it },  "Base Bet", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))

                Text("Picks: $picks", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Slider(
                    value = picks.toFloat(),
                    onValueChange = { picksStr = it.toInt().toString() },
                    valueRange = 1f..10f, steps = 8,
                    colors = SliderDefaults.colors(thumbColor = CTPColors.Keno, activeTrackColor = CTPColors.Keno)
                )

                Text("Min Hits to Win: $minHits", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Slider(
                    value = minHits.toFloat(),
                    onValueChange = { minHitsStr = it.toInt().coerceAtMost(picks).toString() },
                    valueRange = 1f..picks.toFloat(), steps = maxOf(0, picks - 2),
                    colors = SliderDefaults.colors(thumbColor = CTPColors.Green, activeTrackColor = CTPColors.Green)
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(lossIncStr,  { lossIncStr = it },  "Loss Inc %",  Modifier.weight(1f))
                    CTPInput(maxBetsStr,  { maxBetsStr = it },  "Max Bets",    Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(sessionsStr, { sessionsStr = it }, "Sessions", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }

            CTPButton(
                text = if (running) "SIMULATING..." else "RUN KENO SIMULATION",
                onClick = {
                    vm.run(KenoMCConfig(
                        bankroll          = bankrollStr.toDoubleOrNull() ?: 1000.0,
                        picks             = picks,
                        minHits           = minHits,
                        baseBet           = baseBetStr.toDoubleOrNull() ?: 1.0,
                        increaseOnLossPct = lossIncStr.toDoubleOrNull() ?: 50.0,
                        maxBets           = maxBetsStr.toIntOrNull()    ?: 50,
                        sessions          = sessionsStr.toIntOrNull()?.coerceIn(1, 20_000) ?: 3_000
                    ))
                },
                enabled = !running,
                color = if (running) CTPColors.Border else CTPColors.Keno,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running) { LoadingOverlay("Running Keno simulation...") }

            result?.let { r ->
                CTPCard(accentColor = CTPColors.Green) {
                    Text("RESULTS — ${"%,d".format(r.sessions)} sessions", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("Win Rate",  "%.1f%%".format(r.winRate),  if (r.winRate > 40) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
                        StatChip("Bust Rate", "%.1f%%".format(r.bustRate), if (r.bustRate < 30) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    ResultRow("Picks / Min Hits",   "${r.picks} picks, ${r.minHits}+ hits")
                    ResultRow("Win Probability",     "%.2f%%".format(r.winProbability), CTPColors.Dice)
                    ResultRow("Payout Multiplier",   "%.4f×".format(r.payoutMultiplier), CTPColors.Green)
                    ResultRow("Avg Profit",          "${if (r.avgProfit >= 0) "+" else ""}${"%.4f".format(r.avgProfit)}", if (r.avgProfit >= 0) CTPColors.Green else CTPColors.Red)
                    ResultRow("Median Profit",       "${if (r.medianProfit >= 0) "+" else ""}${"%.4f".format(r.medianProfit)}", if (r.medianProfit >= 0) CTPColors.Green else CTPColors.Red)
                    ResultRow("Longest Loss Streak", "${r.longestLossStreak} rounds", CTPColors.Mines)
                }
            }

            // Keno probability table for current picks
            KenoHitTable(picks = picks)
        }
    }
}

@Composable
fun KenoHitTable(picks: Int) {
    CTPCard(accentColor = CTPColors.Keno) {
        Text("HIT PROBABILITY TABLE — $picks picks", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Hits", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.8f))
            Text("P(Exactly)", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.3f))
            Text("P(At Least)", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.3f))
            Text("Fair Mult", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
        }
        CTPDivider(Modifier.padding(vertical = 4.dp))
        (0..minOf(picks, 10)).forEach { hits ->
            val exact   = ProbabilityMath.kenoHypergeometric(picks, hits) * 100.0
            val atLeast = ProbabilityMath.kenoWinProbability(picks, hits) * 100.0
            val mult    = if (hits > 0) ProbabilityMath.kenoFairMultiplier(picks, hits) else 0.0
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("$hits", style = CTPType.Mono, color = CTPColors.Keno, modifier = Modifier.weight(0.8f))
                Text("%.2f%%".format(exact), style = CTPType.Mono, color = CTPColors.TextSecondary, modifier = Modifier.weight(1.3f))
                Text("%.2f%%".format(atLeast), style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(1.3f))
                Text(if (mult > 0) "%.2f×".format(mult) else "—", style = CTPType.Mono, color = CTPColors.Green, modifier = Modifier.weight(1f))
            }
        }
    }
}
