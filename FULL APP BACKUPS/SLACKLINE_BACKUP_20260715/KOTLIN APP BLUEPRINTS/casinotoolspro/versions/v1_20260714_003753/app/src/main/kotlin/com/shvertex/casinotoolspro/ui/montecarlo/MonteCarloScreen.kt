package com.shvertex.casinotoolspro.ui.montecarlo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MCViewModel : ViewModel() {
    private val _result   = MutableStateFlow<MCResult?>(null)
    private val _progress = MutableStateFlow(0f)
    private val _status   = MutableStateFlow("Ready")
    private val _running  = MutableStateFlow(false)

    val result   = _result.asStateFlow()
    val progress = _progress.asStateFlow()
    val status   = _status.asStateFlow()
    val running  = _running.asStateFlow()

    fun run(config: MCConfig) {
        _running.value  = true
        _result.value   = null
        _progress.value = 0f
        _status.value   = "Initializing..."

        viewModelScope.launch(Dispatchers.Default) {
            val res = MonteCarloEngine.simulate(config) { done, total ->
                _progress.value = done.toFloat() / total
                _status.value   = "Running... $done / $total"
            }
            _result.value   = res
            _progress.value = 1f
            _status.value   = "Complete — ${res.sessions.toFormattedString()} sessions"
            _running.value  = false
        }
    }
}

internal fun Int.toFormattedString() = "%,d".format(this)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MonteCarloScreen(onBack: () -> Unit) {
    val vm: MCViewModel = viewModel()
    val result   by vm.result.collectAsState()
    val progress by vm.progress.collectAsState()
    val status   by vm.status.collectAsState()
    val running  by vm.running.collectAsState()

    var bankrollStr  by remember { mutableStateOf("1000") }
    var baseBetStr   by remember { mutableStateOf("1") }
    var multiStr     by remember { mutableStateOf("2.00") }
    var winChStr     by remember { mutableStateOf("49.50") }
    var lossIncStr   by remember { mutableStateOf("100") }
    var stopProfStr  by remember { mutableStateOf("0") }
    var stopLossStr  by remember { mutableStateOf("0") }
    var maxBetsStr   by remember { mutableStateOf("100") }
    var sessionsStr  by remember { mutableStateOf("5000") }
    var syncGuard    by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader(
            title       = "Monte Carlo Simulator",
            subtitle    = "Multi-session probability simulation",
            accentColor = CTPColors.Limbo,
            onBack      = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Limbo) {
                Text("SIMULATION PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(baseBetStr,  { baseBetStr  = it }, "Base Bet", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(multiStr, {
                        multiStr = it
                        if (!syncGuard) {
                            syncGuard = true
                            it.toDoubleOrNull()?.let { m ->
                                if (m >= 1.01) winChStr = "%.2f".format(ProbabilityMath.diceWinChance(m))
                            }
                            syncGuard = false
                        }
                    }, "Multiplier", Modifier.weight(1f))
                    CTPInput(winChStr, {
                        winChStr = it
                        if (!syncGuard) {
                            syncGuard = true
                            it.toDoubleOrNull()?.let { c ->
                                if (c > 0) multiStr = "%.4f".format(ProbabilityMath.diceMultiplier(c))
                            }
                            syncGuard = false
                        }
                    }, "Win Chance %", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(lossIncStr, { lossIncStr = it }, "Loss Inc %",  Modifier.weight(1f))
                    CTPInput(maxBetsStr, { maxBetsStr = it }, "Max Bets",    Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(stopProfStr, { stopProfStr = it }, "Stop Profit %", Modifier.weight(1f))
                    CTPInput(stopLossStr, { stopLossStr = it }, "Stop Loss %",   Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(sessionsStr, { sessionsStr = it }, "Sessions (100–50,000)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }

            CTPButton(
                text = if (running) "RUNNING..." else "RUN SIMULATION",
                onClick = {
                    vm.run(MCConfig(
                        bankroll          = bankrollStr.toDoubleOrNull() ?: 1000.0,
                        baseBet           = baseBetStr.toDoubleOrNull()  ?: 1.0,
                        multiplier        = multiStr.toDoubleOrNull()    ?: 2.0,
                        winChancePct      = winChStr.toDoubleOrNull()    ?: 49.5,
                        increaseOnLossPct = lossIncStr.toDoubleOrNull()  ?: 100.0,
                        stopProfitPct     = stopProfStr.toDoubleOrNull() ?: 0.0,
                        stopLossPct       = stopLossStr.toDoubleOrNull() ?: 0.0,
                        maxBets           = maxBetsStr.toIntOrNull()     ?: 100,
                        sessions          = sessionsStr.toIntOrNull()?.coerceIn(1, 50_000) ?: 5_000
                    ))
                },
                enabled   = !running,
                color     = if (running) CTPColors.Border else CTPColors.Limbo,
                textColor = CTPColors.TextPrimary,
                modifier  = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CTPProgressBar(progress)
                    Text(status, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                }
            }

            result?.let { r -> MCResultCard(r) }
        }
    }
}

@Composable
fun MCResultCard(r: MCResult) {
    CTPCard(accentColor = CTPColors.Green) {
        Text("RESULTS — ${r.sessions.toFormattedString()} sessions",
            style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Avg Profit",
                "${if (r.avgProfit >= 0) "+" else ""}${"%.4f".format(r.avgProfit)}",
                if (r.avgProfit >= 0) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
            StatChip("Win Rate",  "%.1f%%".format(r.winRate),
                if (r.winRate >= 50) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
            StatChip("Bust Rate", "%.1f%%".format(r.bustRate),
                if (r.bustRate < 20) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        CTPDivider()
        Spacer(Modifier.height(8.dp))

        ResultRow("Median Profit",
            "${if (r.medianProfit >= 0) "+" else ""}${"%.4f".format(r.medianProfit)}",
            if (r.medianProfit >= 0) CTPColors.Green else CTPColors.Red)
        ResultRow("Best Session",        "+%.4f".format(r.bestSession),  CTPColors.Green)
        ResultRow("Worst Session",       "%.4f".format(r.worstSession),  CTPColors.Red)
        ResultRow("Avg Bets / Session",  "%.1f".format(r.avgBets))
        ResultRow("Longest Loss Streak", "${r.longestLossStreak} bets",  CTPColors.Mines)
        ResultRow("Std Deviation",       "%.4f".format(r.stdDev))
        ResultRow("Sharpe Ratio",        "%.4f".format(r.sharpeRatio),
            if (r.sharpeRatio > 0) CTPColors.Green else CTPColors.Red)
        ResultRow("Profit Factor",       "%.3f".format(r.profitFactor),
            if (r.profitFactor >= 1.0) CTPColors.Green else CTPColors.Red)
        ResultRow("95% VaR",             "%.4f".format(r.var95),  CTPColors.Red)

        Spacer(Modifier.height(8.dp))

        val (verdictText, verdictColor) = when {
            r.bustRate > 70   -> "HIGH RISK — Bust rate critical"           to CTPColors.Red
            r.avgProfit < 0   -> "NEGATIVE EV — Strategy bleeds long-term"  to CTPColors.Red
            r.sharpeRatio > 1 -> "STRONG EDGE — Good risk-adjusted return"  to CTPColors.Green
            r.winRate > 55    -> "POSITIVE — Above average win rate"         to CTPColors.Dice
            else              -> "NEUTRAL — Marginal edge, exercise caution" to CTPColors.TextSecondary
        }
        VerdictBadge(verdictText, verdictColor)
    }
}