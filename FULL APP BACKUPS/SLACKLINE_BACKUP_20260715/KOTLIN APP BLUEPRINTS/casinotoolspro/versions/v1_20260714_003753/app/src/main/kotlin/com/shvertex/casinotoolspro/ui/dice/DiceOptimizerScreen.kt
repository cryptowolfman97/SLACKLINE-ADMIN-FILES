package com.shvertex.casinotoolspro.ui.dice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.core.*
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Dice Optimizer ─────────────────────────────────────────────────────────────
// Grid search over multiplier × lossIncrease space

class OptimizerViewModel : ViewModel() {
    data class OptResult(val multiplier: Double, val winChance: Double, val lossInc: Double, val baseBetPct: Double, val score: Double, val result: MCResult)

    private val _results = MutableStateFlow<List<OptResult>>(emptyList())
    private val _running = MutableStateFlow(false)
    private val _progress = MutableStateFlow(0f)
    val results  = _results.asStateFlow()
    val running  = _running.asStateFlow()
    val progress = _progress.asStateFlow()

    fun run(bankroll: Double, sessions: Int, goal: EvolutionGoal) {
        _running.value = true
        _results.value = emptyList()
        _progress.value = 0f

        // Grid parameters
        val multipliers  = listOf(1.5, 2.0, 3.0, 5.0, 10.0, 20.0, 50.0)
        val lossIncs     = listOf(0.0, 50.0, 100.0, 150.0, 200.0)
        val baseBetPcts  = listOf(0.1, 0.5, 1.0, 2.0)
        val total = multipliers.size * lossIncs.size * baseBetPcts.size
        var done = 0

        viewModelScope.launch(Dispatchers.Default) {
            val allResults = mutableListOf<OptResult>()
            for (mult in multipliers) {
                for (lossInc in lossIncs) {
                    for (bpct in baseBetPcts) {
                        val cfg = MCConfig(
                            bankroll = bankroll,
                            baseBet  = bankroll * bpct / 100.0,
                            multiplier = mult,
                            winChancePct = ProbabilityMath.diceWinChance(mult),
                            increaseOnLossPct = lossInc,
                            maxBets = 100,
                            sessions = sessions
                        )
                        val r = MonteCarloEngine.simulate(cfg)
                        val score = when (goal) {
                            EvolutionGoal.MAX_PROFIT   -> r.avgProfit
                            EvolutionGoal.MIN_RISK     -> -r.bustRate
                            EvolutionGoal.BEST_SHARPE  -> r.sharpeRatio
                            EvolutionGoal.MAX_WIN_RATE -> r.winRate
                        }
                        allResults.add(OptResult(mult, ProbabilityMath.diceWinChance(mult), lossInc, bpct, score, r))
                        done++
                        _progress.value = done.toFloat() / total
                    }
                }
            }
            _results.value = allResults.sortedByDescending { it.score }
            _running.value = false
        }
    }
}

@Composable
fun DiceOptimizerScreen(onBack: () -> Unit) {
    val vm: OptimizerViewModel = viewModel()
    val results  by vm.results.collectAsState()
    val running  by vm.running.collectAsState()
    val progress by vm.progress.collectAsState()

    var bankrollStr by remember { mutableStateOf("1000") }
    var sessionsStr by remember { mutableStateOf("500") }
    var goal by remember { mutableStateOf(EvolutionGoal.BEST_SHARPE) }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Dice Optimizer", "Grid search across parameter space", CTPColors.Limbo, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Limbo, showAccent = false) {
                Text("Evaluates 140 parameter combinations via Monte Carlo to find the optimal multiplier and bet sizing for your bankroll.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
            }

            CTPCard(accentColor = CTPColors.Limbo) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(sessionsStr, { sessionsStr = it }, "Sessions/Config", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                com.shvertex.casinotoolspro.ui.evolution.GoalSelector(goal, { goal = it })
            }

            CTPButton(
                text = if (running) "OPTIMIZING (140 configs)..." else "RUN OPTIMIZER",
                onClick = { vm.run(bankrollStr.toDoubleOrNull() ?: 1000.0, sessionsStr.toIntOrNull()?.coerceIn(100, 2000) ?: 500, goal) },
                enabled = !running,
                color = if (running) CTPColors.Border else CTPColors.Limbo,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                CTPProgressBar(progress, color = CTPColors.Limbo)
                Text("${"%.0f".format(progress * 100)}% complete — evaluating configurations...", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
            }
            if (running) LoadingOverlay("Grid searching...")

            if (results.isNotEmpty()) {
                CTPCard(accentColor = CTPColors.Green) {
                    Text("OPTIMIZER RESULTS — TOP 15", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("#",      style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.5f))
                        Text("Multi",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.9f))
                        Text("Loss+",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.9f))
                        Text("Bet%",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.9f))
                        Text("Win%",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.9f))
                        Text("Score",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                    }
                    CTPDivider(Modifier.padding(vertical = 4.dp))
                    results.take(15).forEachIndexed { i, r ->
                        val c = if (i == 0) CTPColors.Gold else CTPColors.TextPrimary
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${i+1}",                  style = CTPType.Mono, color = c, modifier = Modifier.weight(0.5f))
                            Text("${r.multiplier}×",        style = CTPType.Mono, color = CTPColors.Dice, modifier = Modifier.weight(0.9f))
                            Text("${"%.0f".format(r.lossInc)}%", style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(0.9f))
                            Text("${r.baseBetPct}%",        style = CTPType.Mono, color = CTPColors.TextSecondary, modifier = Modifier.weight(0.9f))
                            Text("${"%.1f".format(r.result.winRate)}%", style = CTPType.Mono, color = CTPColors.Green, modifier = Modifier.weight(0.9f))
                            Text("${"%.3f".format(r.score)}", style = CTPType.Mono, color = c, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Dice Auto Generator ────────────────────────────────────────────────────────
// Quick wrapper that just calls EvolutionEngine with more generations

@Composable
fun DiceGeneratorScreen(onBack: () -> Unit) {
    val vm: com.shvertex.casinotoolspro.ui.evolution.EvoViewModel = viewModel()
    val results  by vm.diceResults.collectAsState()
    val progress by vm.progress.collectAsState()
    val status   by vm.status.collectAsState()
    val running  by vm.running.collectAsState()

    var bankrollStr by remember { mutableStateOf("1000") }
    var goal by remember { mutableStateOf(EvolutionGoal.MAX_PROFIT) }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Dice Auto Generator", "Full auto strategy discovery — 30 generations", CTPColors.Keno, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Keno, showAccent = false) {
                Text("Fully autonomous: no tuning required. The engine generates, tests and ranks strategies automatically across 30 generations.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
            }

            CTPCard(accentColor = CTPColors.Keno) {
                CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll")
                Spacer(Modifier.height(10.dp))
                com.shvertex.casinotoolspro.ui.evolution.GoalSelector(goal, { goal = it })
            }

            CTPButton(
                text = if (running) "GENERATING..." else "AUTO-GENERATE STRATEGIES",
                onClick = { vm.evolveDice(bankrollStr.toDoubleOrNull() ?: 1000.0, 30, goal) },
                enabled = !running,
                color = if (running) CTPColors.Border else CTPColors.Keno,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                CTPProgressBar(progress, color = CTPColors.Keno)
                Text(status, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                if (running) LoadingOverlay("Generating across 30 generations...")
            }

            if (results.isNotEmpty()) {
                com.shvertex.casinotoolspro.ui.evolution.DiceEvoResultsCard(results.take(10), CTPColors.Keno)
            }
        }
    }
}

// ── Strategy Forge ─────────────────────────────────────────────────────────────
// Build and test a custom strategy, get MC analysis

@Composable
fun StrategyForgeScreen(onBack: () -> Unit) {
    val vm: StressTestForgeViewModel = viewModel()
    val result  by vm.result.collectAsState()
    val running by vm.running.collectAsState()

    var nameStr      by remember { mutableStateOf("My Strategy") }
    var bankrollStr  by remember { mutableStateOf("1000") }
    var baseBetStr   by remember { mutableStateOf("1") }
    var multiStr     by remember { mutableStateOf("2.0") }
    var winChStr     by remember { mutableStateOf("49.5") }
    var lossIncStr   by remember { mutableStateOf("100") }
    var winIncStr    by remember { mutableStateOf("0") }
    var stopProfStr  by remember { mutableStateOf("0") }
    var stopLossStr  by remember { mutableStateOf("0") }
    var maxBetsStr   by remember { mutableStateOf("100") }
    var sessionsStr  by remember { mutableStateOf("5000") }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Strategy Forge", "Build & test any custom strategy", CTPColors.Dice, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Dice) {
                Text("STRATEGY BUILDER", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                CTPInput(nameStr, { nameStr = it }, "Strategy Name", keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll",   Modifier.weight(1f))
                    CTPInput(baseBetStr,  { baseBetStr = it },  "Base Bet",  Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(multiStr,   { multiStr = it },   "Multiplier",  Modifier.weight(1f))
                    CTPInput(winChStr,   { winChStr = it },   "Win %",       Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(lossIncStr, { lossIncStr = it }, "Loss Inc %",  Modifier.weight(1f))
                    CTPInput(winIncStr,  { winIncStr = it },  "Win Inc %",   Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(stopProfStr,{ stopProfStr = it },"Stop Profit %", Modifier.weight(1f))
                    CTPInput(stopLossStr,{ stopLossStr = it },"Stop Loss %",  Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(maxBetsStr, { maxBetsStr = it }, "Max Bets",    Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    CTPInput(sessionsStr,{ sessionsStr = it },"Sessions",    Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
            }

            CTPButton(
                text = if (running) "TESTING..." else "TEST STRATEGY",
                onClick = {
                    vm.run(MCConfig(
                        bankroll          = bankrollStr.toDoubleOrNull() ?: 1000.0,
                        baseBet           = baseBetStr.toDoubleOrNull()  ?: 1.0,
                        multiplier        = multiStr.toDoubleOrNull()    ?: 2.0,
                        winChancePct      = winChStr.toDoubleOrNull()    ?: 49.5,
                        increaseOnLossPct = lossIncStr.toDoubleOrNull()  ?: 100.0,
                        increaseOnWinPct  = winIncStr.toDoubleOrNull()   ?: 0.0,
                        stopProfitPct     = stopProfStr.toDoubleOrNull() ?: 0.0,
                        stopLossPct       = stopLossStr.toDoubleOrNull() ?: 0.0,
                        maxBets           = maxBetsStr.toIntOrNull()     ?: 100,
                        sessions          = sessionsStr.toIntOrNull()?.coerceIn(1, 20_000) ?: 5_000
                    ))
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            )

            if (running) LoadingOverlay("Forging and testing strategy...")
            result?.let { r -> com.shvertex.casinotoolspro.ui.montecarlo.MCResultCard(r) }
        }
    }
}

class StressTestForgeViewModel : ViewModel() {
    private val _result  = MutableStateFlow<MCResult?>(null)
    private val _running = MutableStateFlow(false)
    val result  = _result.asStateFlow()
    val running = _running.asStateFlow()

    fun run(config: MCConfig) {
        _running.value = true
        _result.value  = null
        viewModelScope.launch(Dispatchers.Default) {
            _result.value  = MonteCarloEngine.simulate(config)
            _running.value = false
        }
    }
}
