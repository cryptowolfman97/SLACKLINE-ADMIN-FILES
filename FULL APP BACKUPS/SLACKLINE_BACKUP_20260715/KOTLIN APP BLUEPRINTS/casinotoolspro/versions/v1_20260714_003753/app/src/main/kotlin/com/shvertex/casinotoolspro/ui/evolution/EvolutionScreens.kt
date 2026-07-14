package com.shvertex.casinotoolspro.ui.evolution

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// ── Shared Evolution ViewModel ────────────────────────────────────────────────

class EvoViewModel : ViewModel() {
    private val _diceResults  = MutableStateFlow<List<StrategyGenome>>(emptyList())
    private val _kenoResults  = MutableStateFlow<List<KenoGenome>>(emptyList())
    private val _minesResults = MutableStateFlow<List<MinesGenome>>(emptyList())
    private val _progress     = MutableStateFlow(0f)
    private val _status       = MutableStateFlow("Ready")
    private val _running      = MutableStateFlow(false)

    val diceResults  = _diceResults.asStateFlow()
    val kenoResults  = _kenoResults.asStateFlow()
    val minesResults = _minesResults.asStateFlow()
    val progress     = _progress.asStateFlow()
    val status       = _status.asStateFlow()
    val running      = _running.asStateFlow()

    fun evolveDice(bankroll: Double, generations: Int, goal: EvolutionGoal, isLimbo: Boolean = false) {
        _running.value = true
        _diceResults.value = emptyList()
        _progress.value = 0f
        _status.value = "Evolving generation 0 / $generations"

        viewModelScope.launch(Dispatchers.Default) {
            val results = EvolutionEngine.evolveDice(
                bankroll       = bankroll,
                generations    = generations,
                populationSize = 40,
                sessionsPerEval= 300,
                goal           = goal
            ) { gen, total ->
                _progress.value = gen.toFloat() / total
                _status.value   = "Generation $gen / $total"
            }
            _diceResults.value = results
            _status.value  = "Done — top ${minOf(results.size, 10)} strategies ranked"
            _running.value = false
        }
    }

    fun evolveKeno(bankroll: Double, generations: Int, goal: EvolutionGoal) {
        _running.value = true
        _kenoResults.value = emptyList()
        _progress.value = 0f
        viewModelScope.launch(Dispatchers.Default) {
            val results = EvolutionEngine.evolveKeno(
                bankroll, generations, 30, 200, goal
            ) { gen, total ->
                _progress.value = gen.toFloat() / total
                _status.value   = "Generation $gen / $total"
            }
            _kenoResults.value = results
            _status.value  = "Done — ${results.size} strategies evaluated"
            _running.value = false
        }
    }

    fun evolveMines(bankroll: Double, generations: Int, goal: EvolutionGoal) {
        _running.value = true
        _minesResults.value = emptyList()
        _progress.value = 0f
        viewModelScope.launch(Dispatchers.Default) {
            val results = EvolutionEngine.evolveMines(
                bankroll, generations, 30, 300, goal
            ) { gen, total ->
                _progress.value = gen.toFloat() / total
                _status.value   = "Generation $gen / $total"
            }
            _minesResults.value = results
            _status.value  = "Done"
            _running.value = false
        }
    }
}

// ── Shared Goal Selector ─────────────────────────────────────────────────────

@Composable
fun GoalSelector(selected: EvolutionGoal, onSelect: (EvolutionGoal) -> Unit) {
    Column {
        Text("OPTIMIZATION GOAL", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EvolutionGoal.entries.forEach { goal ->
                val label = when (goal) {
                    EvolutionGoal.MAX_PROFIT  -> "Max Profit"
                    EvolutionGoal.MIN_RISK    -> "Min Risk"
                    EvolutionGoal.BEST_SHARPE -> "Best Sharpe"
                    EvolutionGoal.MAX_WIN_RATE -> "Win Rate"
                }
                FilterChip(
                    selected = selected == goal,
                    onClick = { onSelect(goal) },
                    label = { Text(label, style = CTPType.LabelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CTPColors.Dice,
                        selectedLabelColor = Color.White,
                        labelColor = CTPColors.TextSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Dice Evolution Screen ─────────────────────────────────────────────────────

@Composable
fun DiceEvolutionScreen(onBack: () -> Unit, isLimbo: Boolean = false) {
    val vm: EvoViewModel = viewModel()
    val results  by vm.diceResults.collectAsState()
    val progress by vm.progress.collectAsState()
    val status   by vm.status.collectAsState()
    val running  by vm.running.collectAsState()

    var bankrollStr   by remember { mutableStateOf("1000") }
    var generationsStr by remember { mutableStateOf("20") }
    var goal          by remember { mutableStateOf(EvolutionGoal.BEST_SHARPE) }

    val accent = if (isLimbo) CTPColors.Limbo else CTPColors.Dice
    val title  = if (isLimbo) "Limbo Evolution" else "Dice Evolution"

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader(title, "Genetic algorithm strategy optimizer", accent, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvoExplainerCard(accent)

            CTPCard(accentColor = accent) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr,    { bankrollStr = it },    "Bankroll",    Modifier.weight(1f))
                    CTPInput(generationsStr, { generationsStr = it }, "Generations", Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                GoalSelector(goal, { goal = it })
            }

            CTPButton(
                text = if (running) "EVOLVING..." else "START EVOLUTION",
                onClick = {
                    vm.evolveDice(
                        bankrollStr.toDoubleOrNull() ?: 1000.0,
                        generationsStr.toIntOrNull()?.coerceIn(5, 50) ?: 20,
                        goal, isLimbo
                    )
                },
                enabled = !running,
                color = if (running) CTPColors.Border else accent,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CTPProgressBar(progress, color = accent)
                    Text(status, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                }
            }

            if (running) LoadingOverlay("Evolving strategies across generations...")

            if (results.isNotEmpty()) {
                DiceEvoResultsCard(results.take(10), accent)
            }
        }
    }
}

@Composable
fun DiceEvoResultsCard(results: List<StrategyGenome>, accent: Color) {
    CTPCard(accentColor = accent) {
        Text("TOP ${results.size} EVOLVED STRATEGIES", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth()) {
            Text("#", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.5f))
            Text("Win%", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Multi", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Loss+%", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Score", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
        }
        CTPDivider(Modifier.padding(vertical = 4.dp))

        results.forEachIndexed { i, g ->
            val rankColor = when (i) {
                0 -> CTPColors.Gold
                1 -> CTPColors.TextSecondary
                2 -> CTPColors.Mines
                else -> CTPColors.TextPrimary
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${i + 1}", style = CTPType.Mono, color = rankColor, modifier = Modifier.weight(0.5f))
                Text("%.1f%%".format(g.winChancePct), style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("%.2f×".format(g.multiplier), style = CTPType.Mono, color = CTPColors.Dice, modifier = Modifier.weight(1f))
                Text("%.0f%%".format(g.increaseOnLossPct), style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("%.3f".format(g.score), style = CTPType.Mono, color = rankColor, modifier = Modifier.weight(1f))
            }

            // Expanded detail for top 3
            if (i < 3 && g.result != null) {
                val r = g.result
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp)) {
                    Text("Win Rate: ${"%.1f".format(r.winRate)}%  •  Bust: ${"%.1f".format(r.bustRate)}%  •  Avg: ${if (r.avgProfit >= 0) "+" else ""}${"%.2f".format(r.avgProfit)}",
                        style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                }
            }
            if (i < results.size - 1) CTPDivider(Modifier.padding(vertical = 2.dp))
        }
    }
}

// ── Keno Evolution Screen ─────────────────────────────────────────────────────

@Composable
fun KenoEvolutionScreen(onBack: () -> Unit) {
    val vm: EvoViewModel = viewModel()
    val results  by vm.kenoResults.collectAsState()
    val progress by vm.progress.collectAsState()
    val status   by vm.status.collectAsState()
    val running  by vm.running.collectAsState()

    var bankrollStr    by remember { mutableStateOf("1000") }
    var generationsStr by remember { mutableStateOf("15") }
    var goal           by remember { mutableStateOf(EvolutionGoal.BEST_SHARPE) }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Keno Evolution", "Evolve optimal keno pick strategies", CTPColors.Keno, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvoExplainerCard(CTPColors.Keno)

            CTPCard(accentColor = CTPColors.Keno) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(generationsStr, { generationsStr = it }, "Generations", Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                GoalSelector(goal, { goal = it })
            }

            CTPButton(
                text = if (running) "EVOLVING..." else "EVOLVE KENO STRATEGY",
                onClick = {
                    vm.evolveKeno(bankrollStr.toDoubleOrNull() ?: 1000.0,
                        generationsStr.toIntOrNull()?.coerceIn(5, 30) ?: 15, goal)
                },
                enabled = !running,
                color = if (running) CTPColors.Border else CTPColors.Keno,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                CTPProgressBar(progress, color = CTPColors.Keno)
                Text(status, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
            }

            if (running) LoadingOverlay("Evolving keno pick configurations...")

            if (results.isNotEmpty()) {
                CTPCard(accentColor = CTPColors.Green) {
                    Text("TOP KENO STRATEGIES", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("#",        style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.5f))
                        Text("Picks",    style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.8f))
                        Text("Min Hits", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                        Text("Win%",     style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                        Text("Score",    style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                    }
                    CTPDivider(Modifier.padding(vertical = 4.dp))
                    results.take(10).forEachIndexed { i, g ->
                        val winPct = g.result?.winProbability ?: (ProbabilityMath.kenoWinProbability(g.picks, g.minHits) * 100.0)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${i+1}", style = CTPType.Mono, color = if (i == 0) CTPColors.Gold else CTPColors.TextPrimary, modifier = Modifier.weight(0.5f))
                            Text("${g.picks}", style = CTPType.Mono, color = CTPColors.Keno, modifier = Modifier.weight(0.8f))
                            Text("${g.minHits}+", style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text("%.2f%%".format(winPct), style = CTPType.Mono, color = CTPColors.Green, modifier = Modifier.weight(1f))
                            Text("%.3f".format(g.score), style = CTPType.Mono, color = if (i == 0) CTPColors.Gold else CTPColors.TextSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Mines Evolution Screen ────────────────────────────────────────────────────

@Composable
fun MinesEvolutionScreen(onBack: () -> Unit) {
    val vm: EvoViewModel = viewModel()
    val results  by vm.minesResults.collectAsState()
    val progress by vm.progress.collectAsState()
    val status   by vm.status.collectAsState()
    val running  by vm.running.collectAsState()

    var bankrollStr    by remember { mutableStateOf("1000") }
    var generationsStr by remember { mutableStateOf("15") }
    var goal           by remember { mutableStateOf(EvolutionGoal.BEST_SHARPE) }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Mines Evolution", "Evolve optimal mines configurations", CTPColors.Mines, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvoExplainerCard(CTPColors.Mines)

            CTPCard(accentColor = CTPColors.Mines) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(generationsStr, { generationsStr = it }, "Generations", Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                GoalSelector(goal, { goal = it })
            }

            CTPButton(
                text = if (running) "EVOLVING..." else "EVOLVE MINES STRATEGY",
                onClick = {
                    vm.evolveMines(bankrollStr.toDoubleOrNull() ?: 1000.0,
                        generationsStr.toIntOrNull()?.coerceIn(5, 30) ?: 15, goal)
                },
                enabled = !running,
                color = if (running) CTPColors.Border else CTPColors.Mines,
                textColor = CTPColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            if (running || progress > 0f) {
                CTPProgressBar(progress, color = CTPColors.Mines)
                Text(status, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
            }

            if (running) LoadingOverlay("Evolving mines configurations...")

            if (results.isNotEmpty()) {
                CTPCard(accentColor = CTPColors.Green) {
                    Text("TOP MINES CONFIGURATIONS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("#",       style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.5f))
                        Text("Mines",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.8f))
                        Text("Picks",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.8f))
                        Text("Win%",    style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                        Text("Bust%",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                        Text("Score",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                    }
                    CTPDivider(Modifier.padding(vertical = 4.dp))
                    results.take(10).forEachIndexed { i, g ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${i+1}", style = CTPType.Mono, color = if (i == 0) CTPColors.Gold else CTPColors.TextPrimary, modifier = Modifier.weight(0.5f))
                            Text("${g.mines}", style = CTPType.Mono, color = CTPColors.Red, modifier = Modifier.weight(0.8f))
                            Text("${g.picks}", style = CTPType.Mono, color = CTPColors.Mines, modifier = Modifier.weight(0.8f))
                            Text("%.1f%%".format(g.winRate), style = CTPType.Mono, color = CTPColors.Green, modifier = Modifier.weight(1f))
                            Text("%.1f%%".format(g.bustRate), style = CTPType.Mono, color = CTPColors.Red, modifier = Modifier.weight(1f))
                            Text("%.3f".format(g.score), style = CTPType.Mono, color = if (i == 0) CTPColors.Gold else CTPColors.TextSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Explainer Card ────────────────────────────────────────────────────────────

@Composable
fun EvoExplainerCard(accent: Color) {
    CTPCard(accentColor = accent, showAccent = false) {
        Text(
            "The genetic algorithm starts with random strategies, evaluates each via Monte Carlo simulation, " +
            "keeps the best performers, then mutates and breeds them across generations. " +
            "This finds parameter combinations a human would never think to test.",
            style = CTPType.BodyMedium, color = CTPColors.TextSecondary
        )
    }
}
