package com.shvertex.casinotoolspro.ui.utilities

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.core.*
import com.shvertex.casinotoolspro.navigation.Routes
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// ── Compound Growth ───────────────────────────────────────────────────────────

@Composable
fun CompoundGrowthScreen(onBack: () -> Unit) {
    var principalStr by remember { mutableStateOf("1000") }
    var rateStr      by remember { mutableStateOf("5") }
    var periodsStr   by remember { mutableStateOf("12") }
    var targetStr    by remember { mutableStateOf("") }

    val table = remember(principalStr, rateStr, periodsStr) {
        val p = principalStr.toDoubleOrNull() ?: return@remember emptyList<Triple<Int, Double, Double>>()
        val r = rateStr.toDoubleOrNull()      ?: return@remember emptyList()
        val n = periodsStr.toIntOrNull()      ?: return@remember emptyList()
        if (p <= 0 || r == 0.0 || n <= 0 || n > 1000) return@remember emptyList()
        (1..minOf(n, 100)).map { period ->
            Triple(
                period,
                ProbabilityMath.compoundGrowth(p, r, period),
                ProbabilityMath.compoundGrowth(p, r, period) - p
            )
        }
    }

    val periodsToTarget = remember(principalStr, rateStr, targetStr) {
        val p = principalStr.toDoubleOrNull() ?: return@remember null
        val r = rateStr.toDoubleOrNull()      ?: return@remember null
        val t = targetStr.toDoubleOrNull()    ?: return@remember null
        if (r <= 0 || p <= 0 || t <= p) return@remember null
        ProbabilityMath.periodsToTarget(p, t, r)
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Compound Growth", "Bankroll growth projections", CTPColors.Green, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Green) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(
                        principalStr, { principalStr = it },
                        "Starting Bankroll", Modifier.weight(1f)
                    )
                    CTPInput(
                        rateStr, { rateStr = it },
                        "Growth % / Period", Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(
                        periodsStr, { periodsStr = it },
                        "Periods", Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    CTPInput(
                        targetStr, { targetStr = it },
                        "Target (optional)", Modifier.weight(1f)
                    )
                }
            }

            periodsToTarget?.let {
                CTPCard(accentColor = CTPColors.Dice) {
                    ResultRow(
                        "Periods to reach target",
                        "$it",
                        CTPColors.Green
                    )
                }
            }

            if (table.isNotEmpty()) {
                CTPCard(accentColor = CTPColors.Green) {
                    Text("GROWTH TABLE", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Period",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                        Text("Balance", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f))
                        Text("Profit",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f))
                        Text("2×",      style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.8f))
                    }
                    CTPDivider(Modifier.padding(vertical = 4.dp))
                    val principal = principalStr.toDoubleOrNull() ?: 0.0
                    table.forEach { (period, balance, profit) ->
                        val isDouble = balance >= principal * 2.0 &&
                                (period == 1 || table[period - 2].second < principal * 2.0)
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("$period",              style = CTPType.Mono, color = CTPColors.TextPrimary,   modifier = Modifier.weight(1f))
                            Text("%.2f".format(balance), style = CTPType.Mono, color = CTPColors.TextPrimary,   modifier = Modifier.weight(1.5f))
                            Text("+%.2f".format(profit), style = CTPType.Mono, color = CTPColors.Green,         modifier = Modifier.weight(1.5f))
                            Text(if (isDouble) "✓" else "", style = CTPType.Mono, color = CTPColors.Gold,       modifier = Modifier.weight(0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ── Pattern Master ────────────────────────────────────────────────────────────

@Composable
fun PatternMasterScreen(onBack: () -> Unit) {
    var input    by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<Map<String, String>?>(null) }

    fun analyze() {
        val tokens = input.trim().split(Regex("[,\\s]+")).filter { it.isNotBlank() }
        if (tokens.size < 2) { analysis = null; return }

        val nums = tokens.mapNotNull { it.toDoubleOrNull() }
        if (nums.size < 2) {
            val wins   = tokens.count { it.uppercase() in listOf("W", "WIN", "1") }
            val losses = tokens.count { it.uppercase() in listOf("L", "LOSS", "LOSE", "0") }
            val total  = tokens.size
            var maxWin = 0; var curWin = 0
            var maxLoss = 0; var curLoss = 0
            tokens.forEach { t ->
                if (t.uppercase() in listOf("W", "WIN", "1")) {
                    curWin++
                    maxWin  = maxOf(maxWin, curWin)
                    curLoss = 0
                } else {
                    curLoss++
                    maxLoss = maxOf(maxLoss, curLoss)
                    curWin  = 0
                }
            }
            analysis = mapOf(
                "Total Rounds"    to "$total",
                "Wins"            to "$wins",
                "Losses"          to "$losses",
                "Win Rate"        to "${"%.1f".format(wins.toDouble() / total * 100)}%",
                "Max Win Streak"  to "$maxWin",
                "Max Loss Streak" to "$maxLoss",
                "Last 5"          to tokens.takeLast(5).joinToString(" ")
            )
        } else {
            val mean  = nums.average()
            val std   = sqrt(nums.map { (it - mean).pow(2) }.average())
            val min   = nums.min()
            val max   = nums.max()
            val sum   = nums.sum()
            val diffs = nums.zipWithNext().map { (a, b) -> b - a }
            val trend = when {
                diffs.average() > 0  -> "↑ Upward"
                diffs.average() < 0  -> "↓ Downward"
                else                 -> "→ Flat"
            }
            analysis = mapOf(
                "Count"      to "${nums.size}",
                "Sum"        to "%.4f".format(sum),
                "Mean"       to "%.4f".format(mean),
                "Std Dev"    to "%.4f".format(std),
                "Min"        to "%.4f".format(min),
                "Max"        to "%.4f".format(max),
                "Range"      to "%.4f".format(max - min),
                "Trend"      to trend,
                "Last 5 Avg" to "%.4f".format(nums.takeLast(5).average())
            )
        }
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Pattern Master", "Sequence & streak analysis", CTPColors.TextSecondary, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Dice, showAccent = false) {
                Text(
                    "Enter a sequence of numbers or W/L results separated by commas or spaces.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                )
                Text(
                    "Examples:  \"1.2, 0.8, 2.1\"  or  \"W L L W W L\"",
                    style = CTPType.LabelMedium, color = CTPColors.TextMuted
                )
            }

            OutlinedTextField(
                value         = input,
                onValueChange = { input = it },
                placeholder   = { Text("W L W W L L W L...", color = CTPColors.TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = CTPColors.TextPrimary,
                    unfocusedTextColor      = CTPColors.TextPrimary,
                    focusedBorderColor      = CTPColors.Dice,
                    unfocusedBorderColor    = CTPColors.Border,
                    cursorColor             = CTPColors.Dice,
                    focusedContainerColor   = CTPColors.Card,
                    unfocusedContainerColor = CTPColors.Card
                ),
                textStyle = CTPType.Mono,
                shape     = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                minLines  = 3,
                maxLines  = 6,
                modifier  = Modifier.fillMaxWidth()
            )

            CTPButton(
                "ANALYZE PATTERN",
                onClick   = { analyze() },
                modifier  = Modifier.fillMaxWidth(),
                color     = CTPColors.Dice,
                textColor = Color.White
            )

            analysis?.let { map ->
                CTPCard(accentColor = CTPColors.Green) {
                    Text("PATTERN ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    map.forEach { (k, v) ->
                        val color = when {
                            k.contains("Win") && !k.contains("Streak") -> CTPColors.Green
                            k.contains("Loss")  -> CTPColors.Red
                            k.contains("Trend") -> if (v.contains("↑")) CTPColors.Green else CTPColors.Red
                            else                -> CTPColors.TextPrimary
                        }
                        ResultRow(k, v, color)
                        CTPDivider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ── Session History ───────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val mgr     = remember { BankrollManager.getInstance(context) }
    val history = remember { mgr.loadHistory() }

    val totalProfit = history.sumOf { it.amount }
    val wins        = history.count { it.amount > 0 }
    val losses      = history.count { it.amount < 0 }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Session History", "${history.size} entries logged", CTPColors.TextMuted, onBack)

        if (history.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    "Total P/L",
                    "${if (totalProfit >= 0) "+" else ""}${"%.2f".format(totalProfit)}",
                    if (totalProfit >= 0) CTPColors.Green else CTPColors.Red,
                    Modifier.weight(1f)
                )
                StatChip("Wins",   "$wins",   CTPColors.Green, Modifier.weight(1f))
                StatChip("Losses", "$losses", CTPColors.Red,   Modifier.weight(1f))
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No history yet.\nUpdate your profit from the home screen.",
                            style = CTPType.BodyMedium,
                            color = CTPColors.TextMuted
                        )
                    }
                }
            } else {
                items(history.reversed()) { entry ->
                    val color = if (entry.amount >= 0) CTPColors.Green else CTPColors.Red
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.timestamp,
                            style = CTPType.LabelMedium,
                            color = CTPColors.TextMuted
                        )
                        Text(
                            "${if (entry.amount >= 0) "+" else ""}${"%.4f".format(entry.amount)}",
                            style = CTPType.Mono,
                            color = color
                        )
                    }
                    CTPDivider()
                }
            }
        }
    }
}

// ── Stress Test ───────────────────────────────────────────────────────────────

class StressViewModel : ViewModel() {
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

@Composable
fun StressTestScreen(onBack: () -> Unit) {
    val vm: StressViewModel = viewModel()
    val result  by vm.result.collectAsState()
    val running by vm.running.collectAsState()

    var bankrollStr by remember { mutableStateOf("1000") }
    var baseBetStr  by remember { mutableStateOf("1") }
    var multiStr    by remember { mutableStateOf("2.0") }
    var winChStr    by remember { mutableStateOf("49.5") }
    var lossIncStr  by remember { mutableStateOf("100") }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader(
            "Strategy Stress Test",
            "10,000 session extreme pressure test",
            CTPColors.Limbo,
            onBack
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Limbo, showAccent = false) {
                Text(
                    "Runs your strategy through 10,000 sessions with extreme parameters " +
                    "to expose weaknesses before you play with real money.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                )
            }

            CTPCard(accentColor = CTPColors.Limbo) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll",    Modifier.weight(1f))
                    CTPInput(baseBetStr,  { baseBetStr = it },  "Base Bet",   Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(multiStr,  { multiStr = it },  "Multiplier",  Modifier.weight(1f))
                    CTPInput(winChStr,  { winChStr = it },  "Win Chance%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(lossIncStr, { lossIncStr = it }, "Loss Increase %")
            }

            CTPButton(
                text = if (running) "STRESS TESTING..." else "RUN 10,000 SESSION STRESS TEST",
                onClick = {
                    vm.run(
                        MCConfig(
                            bankroll          = bankrollStr.toDoubleOrNull() ?: 1000.0,
                            baseBet           = baseBetStr.toDoubleOrNull()  ?: 1.0,
                            multiplier        = multiStr.toDoubleOrNull()    ?: 2.0,
                            winChancePct      = winChStr.toDoubleOrNull()    ?: 49.5,
                            increaseOnLossPct = lossIncStr.toDoubleOrNull()  ?: 100.0,
                            maxBets           = 200,
                            sessions          = 10_000
                        )
                    )
                },
                enabled   = !running,
                color     = if (running) CTPColors.Border else CTPColors.Limbo,
                textColor = Color.White,
                modifier  = Modifier.fillMaxWidth()
            )

            if (running) LoadingOverlay("Running 10,000 sessions — this may take a moment...")
            result?.let { r ->
                com.shvertex.casinotoolspro.ui.montecarlo.MCResultCard(r)
            }
        }
    }
}

// ── Bankroll Survival Lab ─────────────────────────────────────────────────────

@Composable
fun BankrollLabScreen(onBack: () -> Unit) {
    var startStr  by remember { mutableStateOf("100") }
    var targetStr by remember { mutableStateOf("200") }
    var winPctStr by remember { mutableStateOf("49.5") }

    val results = remember(startStr, targetStr, winPctStr) {
        val start  = startStr.toDoubleOrNull()?.toInt()  ?: return@remember null
        val target = targetStr.toDoubleOrNull()?.toInt() ?: return@remember null
        val prob   = winPctStr.toDoubleOrNull()?.div(100.0) ?: return@remember null
        if (start <= 0 || target <= start || prob <= 0) return@remember null

        listOf(0.25, 0.5, 0.75, 1.0).map { fraction ->
            val curr = (start * fraction).toInt().coerceAtLeast(1)
            val surv = ProbabilityMath.gamblerRuinSurvival(curr, target, prob) * 100.0
            Pair(fraction, surv)
        }
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Bankroll Survival Lab", "Ruin probability analysis", CTPColors.Green, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Green) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(
                        startStr,  { startStr = it },
                        "Starting Units", Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    CTPInput(
                        targetStr, { targetStr = it },
                        "Target Units",   Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(winPctStr, { winPctStr = it }, "Win Probability % per bet")
            }

            results?.let { rows ->
                CTPCard(accentColor = CTPColors.Green) {
                    Text("SURVIVAL PROBABILITY", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Probability of reaching target before busting from different starting levels:",
                        style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    rows.forEach { (fraction, survival) ->
                        val startUnits = (startStr.toDoubleOrNull() ?: 100.0) * fraction
                        val ruin       = 100.0 - survival
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "From ${"%.0f".format(startUnits)} units",
                                style = CTPType.Mono,
                                color = CTPColors.TextPrimary
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Survive: ${"%.1f".format(survival)}%", style = CTPType.Mono,        color = CTPColors.Green)
                                Text("Bust:    ${"%.1f".format(ruin)}%",     style = CTPType.LabelMedium, color = CTPColors.Red)
                            }
                        }
                        CTPProgressBar(
                            progress = survival.toFloat() / 100f,
                            color    = if (survival > 50) CTPColors.Green else CTPColors.Red
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ── Sports Lab Overview ───────────────────────────────────────────────────────

@Composable
fun SportsLabScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val tools = listOf(
        "Kelly Calculator" to Routes.KELLY_CALC,
        "Parlay Analyzer"  to Routes.PARLAY,
        "Value Bet Calc"   to Routes.VALUE_BET,
        "Arbitrage Calc"   to Routes.ARBITRAGE,
    )

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader(
            "Sports Betting Lab",
            "Professional sports analytics suite",
            CTPColors.Sports,
            onBack
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Sports, showAccent = false) {
                Text(
                    "Professional-grade sports betting mathematics. Always bet with an edge.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                )
            }

            tools.forEach { (name, route) ->
                CTPButton(
                    name,
                    { onNavigate(route) },
                    Modifier.fillMaxWidth(),
                    CTPColors.Sports,
                    Color.White
                )
            }

            SportsFormulasCard()
        }
    }
}

@Composable
fun SportsFormulasCard() {
    CTPCard(accentColor = CTPColors.Sports) {
        Text("KEY FORMULAS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        val formulas = listOf(
            "Kelly Fraction"  to "f = (bp - q) / b",
            "Expected Value"  to "EV = (P×W) - (Q×L)",
            "True Probability" to "p = 1 / decimal_odds",
            "Betting Edge"    to "edge = p_est - p_implied",
            "Arb inverse sum" to "Σ(1/odds) < 1.0",
            "Parlay odds"     to "Π(leg₁ × leg₂ × ...)",
        )
        formulas.forEach { (name, formula) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Text(name,    style = CTPType.LabelMedium, color = CTPColors.TextSecondary, modifier = Modifier.weight(1f))
                Text(formula, style = CTPType.Mono,        color = CTPColors.Sports)
            }
        }
    }
}