package com.shvertex.casinotoolspro.ui.strategies

import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.shvertex.casinotoolspro.ui.montecarlo.MCResultCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StrategyViewModel(private val mgr: BankrollManager) : ViewModel() {
    var strategies = mutableStateListOf<Strategy>()
        private set

    private val _results  = MutableStateFlow<Map<String, MCResult>>(emptyMap())
    private val _running  = MutableStateFlow<Set<String>>(emptySet())
    val results  = _results.asStateFlow()
    val running  = _running.asStateFlow()

    init { reload() }

    fun reload() {
        strategies.clear()
        strategies.addAll(mgr.loadStrategies())
    }

    fun delete(id: String) { mgr.deleteStrategy(id); reload() }

    fun add(s: Strategy) { mgr.addStrategy(s); reload() }

    fun rename(id: String, newName: String) {
        val updated = strategies.map { if (it.id == id) it.copy(name = newName) else it }
        mgr.saveStrategies(updated)
        reload()
    }

    fun runSim(s: Strategy) {
        _running.value = _running.value + s.id
        viewModelScope.launch(Dispatchers.Default) {
            val cfg = MCConfig(
                bankroll          = s.bankroll,
                baseBet           = s.baseBet,
                multiplier        = s.multiplier,
                winChancePct      = s.winChancePct,
                increaseOnLossPct = s.increaseOnLossPct,
                maxBets           = s.maxBets,
                sessions          = 3_000
            )
            val res = MonteCarloEngine.simulate(cfg)
            withContext(Dispatchers.Main) {
                _results.value = _results.value + (s.id to res)
                _running.value = _running.value - s.id
            }
        }
    }

    fun stressTest(s: Strategy) {
        _running.value = _running.value + s.id
        viewModelScope.launch(Dispatchers.Default) {
            val cfg = MCConfig(
                bankroll          = s.bankroll,
                baseBet           = s.baseBet,
                multiplier        = s.multiplier,
                winChancePct      = s.winChancePct,
                increaseOnLossPct = s.increaseOnLossPct,
                maxBets           = s.maxBets,
                sessions          = 10_000
            )
            val res = MonteCarloEngine.simulate(cfg)
            withContext(Dispatchers.Main) {
                _results.value = _results.value + (s.id to res)
                _running.value = _running.value - s.id
            }
        }
    }

    fun clearResult(id: String) {
        _results.value = _results.value - id
    }

    // ── Export ────────────────────────────────────────────────────────────────

    fun exportStrategies(): String {
        return try {
            val dir  = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CasinoToolsProNew"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "strategies_export.json")
            val json = Gson().toJson(strategies.toList())
            file.writeText(json)
            "Exported ${strategies.size} strategies to:\nDownloads/CasinoToolsProNew/strategies_export.json"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    fun importStrategies(): String {
        return try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CasinoToolsProNew/strategies_export.json"
            )
            if (!file.exists()) return "File not found:\nDownloads/CasinoToolsProNew/strategies_export.json"
            val json = file.readText()
            val type = object : TypeToken<List<Strategy>>() {}.type
            val imported: List<Strategy> = Gson().fromJson(json, type) ?: emptyList()
            if (imported.isEmpty()) return "No strategies found in file"

            // Merge — skip duplicates by id
            val existing = mgr.loadStrategies()
            val existingIds = existing.map { it.id }.toSet()
            val newOnes = imported.filter { it.id !in existingIds }
            mgr.saveStrategies(existing + newOnes)
            reload()
            "Imported ${newOnes.size} new strategies (${imported.size - newOnes.size} duplicates skipped)"
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun StrategiesScreen(onBack: () -> Unit, onNavigate: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val mgr     = remember { BankrollManager.getInstance(context) }
    val vm: StrategyViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return StrategyViewModel(mgr) as T
            }
        }
    )

    val results by vm.results.collectAsState()
    val running by vm.running.collectAsState()

    var showAdd      by remember { mutableStateOf(false) }
    var filterGame   by remember { mutableStateOf("All") }
    var compareMode  by remember { mutableStateOf(false) }
    var selectedIds  by remember { mutableStateOf<Set<String>>(emptySet()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val games = listOf("All", "dice", "limbo", "keno", "mines", "general")

    // Toast snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            toastMessage = null
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData     = data,
                    containerColor   = CTPColors.Card,
                    contentColor     = CTPColors.TextPrimary,
                    actionColor      = CTPColors.Green,
                    shape            = RoundedCornerShape(10.dp)
                )
            }
        },
        containerColor = CTPColors.Black
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CTPColors.Black)
        ) {
            ScreenHeader(
                "Strategies Library",
                "${vm.strategies.size} strategies saved",
                CTPColors.Green,
                onBack
            )

            // Filter row
            ScrollableRow(games, filterGame) { filterGame = it }

            // Compare toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked       = compareMode,
                        onCheckedChange = { compareMode = it; selectedIds = emptySet() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CTPColors.Keno,
                            checkedTrackColor = CTPColors.Keno.copy(0.4f)
                        )
                    )
                    Text("Compare mode", style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
                }
                if (compareMode && selectedIds.size >= 2) {
                    CTPButton("COMPARE ${selectedIds.size}", onClick = {
                        selectedIds.forEach { id ->
                            vm.strategies.find { it.id == id }?.let { s ->
                                if (results[id] == null) vm.runSim(s)
                            }
                        }
                    }, color = CTPColors.Keno, textColor = Color.White)
                }
            }

            // List
            LazyColumn(
                Modifier.weight(1f),
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filtered = if (filterGame == "All") vm.strategies.toList()
                else vm.strategies.filter { it.game == filterGame }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No strategies found",
                                style = CTPType.BodyMedium,
                                color = CTPColors.TextMuted
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { s ->
                    StrategyCard(
                        strategy      = s,
                        result        = results[s.id],
                        isRunning     = s.id in running,
                        compareMode   = compareMode,
                        isSelected    = s.id in selectedIds,
                        onSelect      = {
                            selectedIds = if (s.id in selectedIds)
                                selectedIds - s.id else selectedIds + s.id
                        },
                        onDelete      = { vm.delete(s.id) },
                        onRename      = { newName -> vm.rename(s.id, newName) },
                        onRunSim      = { vm.runSim(s) },
                        onStress      = { vm.stressTest(s) },
                        onRunCalc     = { onNavigate?.invoke(Routes.DICE_CALC) },
                        onClearResult = { vm.clearResult(s.id) }
                    )
                }
            }

            // ── Bottom bar ────────────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CTPColors.Surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CTPButton(
                        "+ NEW STRATEGY",
                        { showAdd = true },
                        Modifier.weight(1f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export button
                    Button(
                        onClick = {
                            val msg = vm.exportStrategies()
                            toastMessage = msg
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CTPColors.Dice
                        ),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("EXPORT", style = CTPType.LabelLarge, color = Color.White)
                    }

                    // Import button
                    Button(
                        onClick = {
                            val msg = vm.importStrategies()
                            toastMessage = msg
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CTPColors.Limbo
                        ),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("IMPORT", style = CTPType.LabelLarge, color = Color.White)
                    }
                }

                // Export path hint
                Text(
                    "Export/Import path: Downloads/CasinoToolsProNew/strategies_export.json",
                    style = CTPType.LabelMedium,
                    color = CTPColors.TextMuted
                )
            }
        }
    }

    if (showAdd) {
        AddStrategyDialog(
            onDismiss = { showAdd = false },
            onSave    = { vm.add(it); showAdd = false }
        )
    }
}

@Composable
fun ScrollableRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { g ->
            FilterChip(
                selected = selected == g,
                onClick  = { onSelect(g) },
                label    = {
                    Text(
                        g.replaceFirstChar { it.uppercase() },
                        style = CTPType.LabelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CTPColors.Green,
                    selectedLabelColor     = Color.Black
                )
            )
        }
    }
}

// ── Strategy Card ─────────────────────────────────────────────────────────────

@Composable
fun StrategyCard(
    strategy: Strategy,
    result: MCResult?,
    isRunning: Boolean,
    compareMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onRunSim: () -> Unit,
    onStress: () -> Unit,
    onRunCalc: () -> Unit,
    onClearResult: () -> Unit
) {
    val gameColor = when (strategy.game) {
        "dice"  -> CTPColors.Dice
        "limbo" -> CTPColors.Limbo
        "keno"  -> CTPColors.Keno
        "mines" -> CTPColors.Mines
        else    -> CTPColors.TextMuted
    }

    var renaming    by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(strategy.name) }
    var expanded    by remember { mutableStateOf(false) }

    CTPCard(
        accentColor = if (compareMode && isSelected) CTPColors.Keno else gameColor,
        modifier    = Modifier
            .fillMaxWidth()
            .then(if (compareMode) Modifier.clickable { onSelect() } else Modifier)
            .border(
                width = if (compareMode && isSelected) 2.dp else 1.dp,
                color = if (compareMode && isSelected) CTPColors.Keno
                        else gameColor.copy(0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(gameColor.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    strategy.game.uppercase(),
                    style = CTPType.LabelMedium,
                    color = gameColor
                )
            }
            Spacer(Modifier.width(8.dp))

            if (renaming) {
                OutlinedTextField(
                    value         = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine    = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor        = CTPColors.TextPrimary,
                        unfocusedTextColor      = CTPColors.TextPrimary,
                        focusedBorderColor      = CTPColors.Green,
                        unfocusedBorderColor    = CTPColors.Border,
                        focusedContainerColor   = CTPColors.Card,
                        unfocusedContainerColor = CTPColors.Card
                    ),
                    textStyle = CTPType.BodyMedium,
                    shape     = RoundedCornerShape(6.dp),
                    modifier  = Modifier.weight(1f).height(40.dp)
                )
                TextButton(onClick = { onRename(renameInput); renaming = false }) {
                    Text("✓", color = CTPColors.Green)
                }
            } else {
                Text(
                    strategy.name,
                    style    = CTPType.HeadlineMedium,
                    color    = CTPColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { renameInput = strategy.name; renaming = true }) {
                    Text("RENAME", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = CTPColors.Red)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniChip("Bank: ${strategy.bankroll}",               gameColor.copy(0.7f))
            MiniChip("Base: ${strategy.baseBet}",                CTPColors.TextSecondary)
            MiniChip("${strategy.multiplier}×",                  CTPColors.Dice)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniChip("Win: ${"%.1f".format(strategy.winChancePct)}%",             CTPColors.Green)
            MiniChip("Loss+: ${"%.0f".format(strategy.increaseOnLossPct)}%",      CTPColors.Mines)
            MiniChip("MaxBets: ${strategy.maxBets}",                              CTPColors.TextSecondary)
        }

        if (strategy.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(strategy.notes, style = CTPType.BodyMedium, color = CTPColors.TextMuted)
        }

        Spacer(Modifier.height(10.dp))

        // Action buttons 2×2
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = onRunSim,
                enabled  = !isRunning,
                colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.Limbo),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (isRunning) "RUNNING..." else "RUN SIM",
                    style = CTPType.LabelLarge,
                    color = Color.White
                )
            }
            Button(
                onClick  = onRunCalc,
                colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.Keno),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("RUN CALC", style = CTPType.LabelLarge, color = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = onStress,
                enabled  = !isRunning,
                colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("STRESS TEST", style = CTPType.LabelLarge, color = CTPColors.TextSecondary)
            }
            Button(
                onClick  = { expanded = !expanded },
                colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (expanded) "HIDE" else "RESULTS",
                    style = CTPType.LabelLarge,
                    color = CTPColors.TextSecondary
                )
            }
        }

        if (isRunning) {
            Spacer(Modifier.height(8.dp))
            LoadingOverlay("Running Monte Carlo...")
        }

        if (result != null && expanded) {
            Spacer(Modifier.height(10.dp))
            CTPDivider()
            Spacer(Modifier.height(8.dp))
            InlineResultSummary(result = result, onClose = onClearResult)
        }
    }
}

@Composable
fun InlineResultSummary(result: MCResult, onClose: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "RESULTS  (${"%,d".format(result.sessions)} sessions)",
                style = CTPType.LabelLarge,
                color = CTPColors.TextMuted
            )
            TextButton(onClick = onClose) {
                Text("✕", color = CTPColors.TextMuted)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip(
                "Avg P/L",
                "${if (result.avgProfit >= 0) "+" else ""}${"%.4f".format(result.avgProfit)}",
                if (result.avgProfit >= 0) CTPColors.Green else CTPColors.Red,
                Modifier.weight(1f)
            )
            StatChip(
                "Win Rate",
                "${"%.1f".format(result.winRate)}%",
                if (result.winRate >= 50) CTPColors.Green else CTPColors.Red,
                Modifier.weight(1f)
            )
            StatChip(
                "Bust %",
                "${"%.1f".format(result.bustRate)}%",
                if (result.bustRate < 20) CTPColors.Green else CTPColors.Red,
                Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        ResultRow("Sharpe Ratio",    "%.4f".format(result.sharpeRatio),
            if (result.sharpeRatio > 0) CTPColors.Green else CTPColors.Red)
        ResultRow("Profit Factor",   "%.3f".format(result.profitFactor),
            if (result.profitFactor >= 1) CTPColors.Green else CTPColors.Red)
        ResultRow("Max Loss Streak", "${result.longestLossStreak}", CTPColors.Mines)
        Spacer(Modifier.height(4.dp))
        val (verdict, vc) = when {
            result.bustRate > 70   -> "HIGH RISK"    to CTPColors.Red
            result.avgProfit < 0   -> "NEGATIVE EV"  to CTPColors.Red
            result.sharpeRatio > 1 -> "STRONG EDGE"  to CTPColors.Green
            else                   -> "MARGINAL"     to CTPColors.TextSecondary
        }
        VerdictBadge(verdict, vc)
    }
}

@Composable
fun MiniChip(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CTPColors.CardElevated)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = CTPType.LabelMedium, color = color)
    }
}

// ── Add Strategy Dialog ───────────────────────────────────────────────────────

@Composable
fun AddStrategyDialog(onDismiss: () -> Unit, onSave: (Strategy) -> Unit) {
    var name       by remember { mutableStateOf("") }
    var game       by remember { mutableStateOf("dice") }
    var category   by remember { mutableStateOf("Custom") }
    var bankroll   by remember { mutableStateOf("1000") }
    var baseBet    by remember { mutableStateOf("1") }
    var multiplier by remember { mutableStateOf("2.0") }
    var winChance  by remember { mutableStateOf("49.5") }
    var lossInc    by remember { mutableStateOf("100") }
    var maxBets    by remember { mutableStateOf("100") }
    var notes      by remember { mutableStateOf("") }
    var risk       by remember { mutableStateOf("Medium") }

    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = CTPColors.Card,
        titleContentColor = CTPColors.TextPrimary,
        title = { Text("New Strategy", style = CTPType.HeadlineLarge) },
        text  = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CTPInput(name,     { name = it },     "Strategy Name", keyboardType = KeyboardType.Text)
                CTPInput(category, { category = it }, "Category",      keyboardType = KeyboardType.Text)

                Text("Game", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("dice", "limbo", "keno", "mines", "general").forEach { g ->
                        FilterChip(
                            selected = game == g,
                            onClick  = { game = g },
                            label    = { Text(g, style = CTPType.LabelMedium) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CTPColors.Green,
                                selectedLabelColor     = Color.Black
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CTPInput(bankroll,   { bankroll = it },   "Bankroll",    Modifier.weight(1f))
                    CTPInput(baseBet,    { baseBet = it },    "Base Bet",    Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CTPInput(multiplier, { multiplier = it }, "Multiplier",  Modifier.weight(1f))
                    CTPInput(winChance,  { winChance = it },  "Win Chance%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CTPInput(lossInc,    { lossInc = it },    "Loss Inc%",   Modifier.weight(1f))
                    CTPInput(maxBets,    { maxBets = it },    "Max Bets",    Modifier.weight(1f),
                        keyboardType = KeyboardType.Number)
                }

                Text("Risk Level", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Safe", "Medium", "High", "Extreme").forEach { r ->
                        val rc = when (r) {
                            "Safe"    -> CTPColors.Green
                            "High"    -> CTPColors.Mines
                            "Extreme" -> CTPColors.Red
                            else      -> CTPColors.Dice
                        }
                        FilterChip(
                            selected = risk == r,
                            onClick  = { risk = r },
                            label    = { Text(r, style = CTPType.LabelMedium) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = rc,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }

                CTPInput(notes, { notes = it }, "Notes (optional)", keyboardType = KeyboardType.Text)
            }
        },
        confirmButton = {
            CTPButton("SAVE", onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        Strategy(
                            name              = name,
                            game              = game,
                            category          = "$category — Risk: $risk",
                            bankroll          = bankroll.toDoubleOrNull()  ?: 1000.0,
                            baseBet           = baseBet.toDoubleOrNull()   ?: 1.0,
                            multiplier        = multiplier.toDoubleOrNull() ?: 2.0,
                            winChancePct      = winChance.toDoubleOrNull() ?: 49.5,
                            increaseOnLossPct = lossInc.toDoubleOrNull()   ?: 100.0,
                            maxBets           = maxBets.toIntOrNull()       ?: 100,
                            notes             = notes
                        )
                    )
                }
            })
        },
        dismissButton = { CTPOutlineButton("CANCEL", onDismiss) }
    )
}