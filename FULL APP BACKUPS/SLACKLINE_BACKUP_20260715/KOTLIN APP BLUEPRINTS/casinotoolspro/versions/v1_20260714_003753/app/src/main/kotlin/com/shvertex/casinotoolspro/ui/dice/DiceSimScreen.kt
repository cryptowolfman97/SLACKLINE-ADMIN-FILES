package com.shvertex.casinotoolspro.ui.dice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.core.ProbabilityMath
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class BetEntry(
    val roll: Double,
    val win: Boolean,
    val betSize: Double,
    val profit: Double
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DiceSimViewModel : ViewModel() {
    private val _bets          = MutableStateFlow<List<BetEntry>>(emptyList())
    private val _balance       = MutableStateFlow(0.0)
    private val _startBankroll = MutableStateFlow(0.0)
    private val _totalBets     = MutableStateFlow(0)
    private val _running       = MutableStateFlow(false)
    private val _netProfit     = MutableStateFlow(0.0)
    private val _lossStreak    = MutableStateFlow(0)
    private val _winStreak     = MutableStateFlow(0)
    private val _maxStreak     = MutableStateFlow(0)
    private val _lastRoll      = MutableStateFlow<Double?>(null)
    private val _lastWin       = MutableStateFlow<Boolean?>(null)
    private val _sessionActive = MutableStateFlow(false)
    private val _stopReason    = MutableStateFlow<String?>(null)
    private val _currentSpeed  = MutableStateFlow(200L) // Dynamic speed state
    
    // New Pro Stats
    private val _peakProfit    = MutableStateFlow(0.0)
    private val _maxDrawdown   = MutableStateFlow(0.0)

    val bets          = _bets.asStateFlow()
    val balance       = _balance.asStateFlow()
    val startBankroll = _startBankroll.asStateFlow()
    val totalBets     = _totalBets.asStateFlow()
    val running       = _running.asStateFlow()
    val netProfit     = _netProfit.asStateFlow()
    val lossStreak    = _lossStreak.asStateFlow()
    val winStreak     = _winStreak.asStateFlow()
    val maxStreak     = _maxStreak.asStateFlow()
    val lastRoll      = _lastRoll.asStateFlow()
    val lastWin       = _lastWin.asStateFlow()
    val sessionActive = _sessionActive.asStateFlow()
    val stopReason    = _stopReason.asStateFlow()
    val peakProfit    = _peakProfit.asStateFlow()
    val maxDrawdown   = _maxDrawdown.asStateFlow()

    private var job: Job? = null

    private var sessionBankroll = 1000.0
    private var sessionBalance  = 1000.0
    private var sessionBets     = 0
    private var sessionBetList  = mutableListOf<BetEntry>()
    private var sessionWinSt    = 0
    private var sessionLossSt   = 0
    private var sessionMaxSt    = 0
    private var sessionPeakBal  = 1000.0
    private var sessionMaxDD    = 0.0

    fun setSpeed(ms: Long) {
        _currentSpeed.value = ms
    }

    fun start(
        bankroll: Double,
        baseBet: Double,
        multiplier: Double,
        winChance: Double,
        rollOver: Boolean,
        lossIncreasePct: Double,
        winIncreasePct: Double,
        numberOfBets: Int,
        isNewSession: Boolean,
        stopOnProfitAmt: Double,
        stopOnLossAmt: Double
    ) {
        if (isNewSession) {
            sessionBankroll       = bankroll
            sessionBalance        = bankroll
            sessionBets           = 0
            sessionBetList        = mutableListOf()
            sessionWinSt          = 0
            sessionLossSt         = 0
            sessionMaxSt          = 0
            sessionPeakBal        = bankroll
            sessionMaxDD          = 0.0
            
            _startBankroll.value  = bankroll
            _balance.value        = bankroll
            _bets.value           = emptyList()
            _totalBets.value      = 0
            _netProfit.value      = 0.0
            _lossStreak.value     = 0
            _winStreak.value      = 0
            _maxStreak.value      = 0
            _peakProfit.value     = 0.0
            _maxDrawdown.value    = 0.0
            _lastRoll.value       = null
            _lastWin.value        = null
            _stopReason.value     = null
        }

        _running.value       = true
        _sessionActive.value = true
        _stopReason.value    = null

        val threshold = if (rollOver) 100.0 - winChance else winChance
        val stopProfitTarget = if (stopOnProfitAmt > 0) stopOnProfitAmt else Double.MAX_VALUE
        val stopLossTarget   = if (stopOnLossAmt   > 0) stopOnLossAmt   else Double.MAX_VALUE

        val rng     = Random(System.nanoTime())
        var curBet  = baseBet
        val maxBets = if (numberOfBets <= 0) Int.MAX_VALUE else numberOfBets

        job = viewModelScope.launch(Dispatchers.Default) {
            var done    = 0
            var stopMsg: String? = null

            while (
                isActive &&
                sessionBalance > 0 &&
                curBet <= sessionBalance &&
                done < maxBets
            ) {
                val currentProfit = sessionBalance - sessionBankroll
                if (stopOnProfitAmt > 0 && currentProfit >= stopProfitTarget) {
                    stopMsg = "✓ Stop profit reached: +${"%.4f".format(currentProfit)}"
                    break
                }
                if (stopOnLossAmt > 0 && -currentProfit >= stopLossTarget) {
                    stopMsg = "✗ Stop loss reached: ${"%.4f".format(currentProfit)}"
                    break
                }

                val finalRoll = rng.nextDouble() * 100.0
                val win = if (rollOver) finalRoll > threshold else finalRoll < threshold
                val actualBet = minOf(curBet, sessionBalance)
                val profit: Double

                if (win) {
                    profit         = actualBet * (multiplier - 1.0)
                    sessionBalance += profit
                    sessionWinSt++
                    sessionLossSt  = 0
                    if (sessionWinSt > sessionMaxSt) sessionMaxSt = sessionWinSt
                    curBet = if (winIncreasePct > 0)
                        baseBet * (1.0 + winIncreasePct / 100.0) else baseBet
                } else {
                    profit         = -actualBet
                    sessionBalance += profit
                    sessionLossSt++
                    sessionWinSt   = 0
                    if (sessionLossSt > sessionMaxSt) sessionMaxSt = sessionLossSt
                    curBet = actualBet * (1.0 + lossIncreasePct / 100.0)
                }
                
                sessionBets++
                done++
                
                // Track Peak and Drawdown
                if (sessionBalance > sessionPeakBal) {
                    sessionPeakBal = sessionBalance
                }
                val currentDrawdown = sessionPeakBal - sessionBalance
                if (currentDrawdown > sessionMaxDD) {
                    sessionMaxDD = currentDrawdown
                }

                val entry = BetEntry(finalRoll, win, actualBet, profit)
                sessionBetList.add(0, entry)
                if (sessionBetList.size > 100) sessionBetList.removeAt(sessionBetList.size - 1)

                withContext(Dispatchers.Main) {
                    _bets.value         = sessionBetList.toList()
                    _balance.value      = sessionBalance
                    _netProfit.value    = sessionBalance - sessionBankroll
                    _totalBets.value    = sessionBets
                    _lossStreak.value   = sessionLossSt
                    _winStreak.value    = sessionWinSt
                    _maxStreak.value    = sessionMaxSt
                    _peakProfit.value   = sessionPeakBal - sessionBankroll
                    _maxDrawdown.value  = sessionMaxDD
                    _lastRoll.value     = finalRoll
                    _lastWin.value      = win
                }
                
                // Delay uses the dynamic StateFlow value
                delay(_currentSpeed.value)
            }

            withContext(Dispatchers.Main) {
                _running.value    = false
                if (stopMsg != null) _stopReason.value = stopMsg
                else if (sessionBalance <= 0 || curBet > sessionBalance) _stopReason.value = "✗ Bankroll exhausted"
            }
        }
    }

    fun stop() {
        job?.cancel()
        _running.value = false
    }

    fun resetSession() {
        job?.cancel()
        _running.value        = false
        _sessionActive.value  = false
        _stopReason.value     = null
        sessionBalance        = sessionBankroll
        sessionBets           = 0
        sessionBetList        = mutableListOf()
        sessionWinSt          = 0
        sessionLossSt         = 0
        sessionMaxSt          = 0
        sessionPeakBal        = sessionBankroll
        sessionMaxDD          = 0.0
        
        _balance.value        = sessionBankroll
        _bets.value           = emptyList()
        _totalBets.value      = 0
        _netProfit.value      = 0.0
        _lossStreak.value     = 0
        _winStreak.value      = 0
        _maxStreak.value      = 0
        _peakProfit.value     = 0.0
        _maxDrawdown.value    = 0.0
        _lastRoll.value       = null
        _lastWin.value        = null
    }

    override fun onCleared() { super.onCleared(); job?.cancel() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun DiceSimScreen(onBack: () -> Unit) {
    val vm: DiceSimViewModel = viewModel()

    val bets          by vm.bets.collectAsState()
    val balance       by vm.balance.collectAsState()
    val totalBets     by vm.totalBets.collectAsState()
    val running       by vm.running.collectAsState()
    val netProfit     by vm.netProfit.collectAsState()
    val lossStreak    by vm.lossStreak.collectAsState()
    val winStreak     by vm.winStreak.collectAsState()
    val peakProfit    by vm.peakProfit.collectAsState()
    val maxDrawdown   by vm.maxDrawdown.collectAsState()
    val lastRoll      by vm.lastRoll.collectAsState()
    val lastWin       by vm.lastWin.collectAsState()
    val sessionActive by vm.sessionActive.collectAsState()
    val stopReason    by vm.stopReason.collectAsState()

    var bankrollStr   by remember { mutableStateOf("1000") }
    var baseBetStr    by remember { mutableStateOf("1") }
    var multiStr      by remember { mutableStateOf("2.0000") }
    var winChStr      by remember { mutableStateOf("49.5000") }
    var lossIncStr    by remember { mutableStateOf("100") }
    var winIncStr     by remember { mutableStateOf("0") }
    var numBetsStr    by remember { mutableStateOf("0") }
    var stopProfitStr by remember { mutableStateOf("0") }
    var stopLossStr   by remember { mutableStateOf("0") }
    var speedIdx      by remember { mutableIntStateOf(1) }
    var rollOver      by remember { mutableStateOf(true) }

    val speedLabels = listOf("Slow", "Normal", "Fast", "Turbo")
    val speedMsList = listOf(800L, 200L, 50L, 5L)

    // Sync speed automatically when changed, even while running
    LaunchedEffect(speedIdx) {
        vm.setSpeed(speedMsList[speedIdx])
    }

    var syncGuard by remember { mutableStateOf(false) }

    fun onMultiChange(v: String) {
        multiStr = v
        if (!syncGuard) {
            syncGuard = true
            v.toDoubleOrNull()?.let { m ->
                if (m >= 1.01) winChStr = "%.4f".format(ProbabilityMath.diceWinChance(m))
            }
            syncGuard = false
        }
    }

    fun onWinChChange(v: String) {
        winChStr = v
        if (!syncGuard) {
            syncGuard = true
            v.toDoubleOrNull()?.let { c ->
                if (c > 0 && c < 100)
                    multiStr = "%.4f".format(ProbabilityMath.diceMultiplier(c))
            }
            syncGuard = false
        }
    }

    fun onSliderChange(pct: Float) {
        if (!syncGuard) {
            syncGuard = true
            val wc = if (rollOver) 100f - pct else pct
            winChStr = "%.4f".format(wc.toDouble().coerceIn(0.01, 99.99))
            multiStr = "%.4f".format(
                ProbabilityMath.diceMultiplier(wc.toDouble().coerceIn(0.01, 99.99))
            )
            syncGuard = false
        }
    }

    val winChFloat  = winChStr.toFloatOrNull() ?: 49.5f
    val rollOverVal = if (rollOver) 100f - winChFloat else winChFloat
    val sliderPos   = rollOverVal

    Column(
        Modifier
            .fillMaxWidth()
            .background(CTPColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        ScreenHeader("Dice Simulator", "Live auto-bet simulation", CTPColors.Dice, onBack)

        // ── STATIC CONFIG ─────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(CTPColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DiceRollVisualizer(
                rollValue    = lastRoll?.toFloat() ?: sliderPos,
                winChance    = winChFloat,
                rollOver     = rollOver,
                lastWin      = lastWin,
                running      = running,
                onSliderDrag = { pct -> if (!running) onSliderChange(pct) }
            )

            // --- STRATEGY GROUP ---
            Text("CORE STRATEGY", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiceFieldBox(
                    label    = "Multiplier",
                    value    = multiStr,
                    onChange = { if (!running) onMultiChange(it) },
                    suffix   = "×",
                    modifier = Modifier.weight(1f)
                )
                Column(Modifier.weight(1f)) {
                    Text("Roll Over", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CTPColors.CardElevated)
                            .border(1.dp, CTPColors.Border, RoundedCornerShape(8.dp))
                            .clickable { if (!running) rollOver = !rollOver },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("%.2f".format(rollOverVal), style = CTPType.Mono, color = CTPColors.TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Sync, contentDescription = null, tint = CTPColors.Dice, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(if (rollOver) "OVER" else "UNDER", style = CTPType.LabelMedium, color = CTPColors.Dice)
                }
                DiceFieldBox(
                    label    = "Win Chance",
                    value    = winChStr,
                    onChange = { if (!running) onWinChChange(it) },
                    suffix   = "%",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiceFieldBox(
                    label    = "Bankroll",
                    value    = bankrollStr,
                    onChange = { if (!running && !sessionActive) bankrollStr = it },
                    modifier = Modifier.weight(1f)
                )
                DiceFieldBox(
                    label    = "Bet Amount",
                    value    = baseBetStr,
                    onChange = { if (!running) baseBetStr = it },
                    modifier = Modifier.weight(1.5f),
                    trailingContent = {
                        Row(modifier = Modifier.padding(end = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(CTPColors.Surface).clickable {
                                if (!running) {
                                    val cur = baseBetStr.toDoubleOrNull() ?: 0.0
                                    baseBetStr = "%.4f".format(cur / 2.0).trimEnd('0').trimEnd('.')
                                }
                            }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text("½", color = CTPColors.TextMuted, style = CTPType.LabelMedium) }
                            
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(CTPColors.Surface).clickable {
                                if (!running) {
                                    val cur = baseBetStr.toDoubleOrNull() ?: 0.0
                                    baseBetStr = "%.4f".format(cur * 2.0).trimEnd('0').trimEnd('.')
                                }
                            }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text("2×", color = CTPColors.TextMuted, style = CTPType.LabelMedium) }
                        }
                    }
                )
            }
            
            Divider(color = CTPColors.Border, thickness = 1.dp)

            // --- AUTOMATION RULES GROUP ---
            Text("AUTOMATION RULES", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiceFieldBox(
                    label    = "On Win Inc %",
                    value    = winIncStr,
                    onChange = { if (!running) winIncStr = it },
                    modifier = Modifier.weight(1f)
                )
                DiceFieldBox(
                    label    = "On Loss Inc %",
                    value    = lossIncStr,
                    onChange = { if (!running) lossIncStr = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiceFieldBox(
                    label    = "Stop Profit Amt",
                    value    = stopProfitStr,
                    onChange = { if (!running) stopProfitStr = it },
                    modifier = Modifier.weight(1f)
                )
                DiceFieldBox(
                    label    = "Stop Loss Amt",
                    value    = stopLossStr,
                    onChange = { if (!running) stopLossStr = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                DiceFieldBox(
                    label    = "# Bets (0=∞)",
                    value    = numBetsStr,
                    onChange = { if (!running) numBetsStr = it },
                    modifier = Modifier.weight(0.8f),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
                Column(Modifier.weight(1.2f)) {
                    Text("Speed (Live Adjustable)", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    
                    // Unified Segmented Controller
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(CTPColors.CardElevated),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        speedLabels.forEachIndexed { i, label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (speedIdx == i) CTPColors.Dice else Color.Transparent)
                                    .clickable { speedIdx = i }, // Always clickable
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = CTPType.LabelMedium.copy(fontSize = 10.sp),
                                    color = if (speedIdx == i) CTPColors.Black else CTPColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            stopReason?.let { msg ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (msg.startsWith("✓")) CTPColors.GreenGlow
                            else CTPColors.RedGlow
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        msg,
                        style = CTPType.BodyMedium,
                        color = if (msg.startsWith("✓")) CTPColors.Green else CTPColors.Red
                    )
                }
            }

            // Start / Stop / Reset
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!running) {
                    Button(
                        onClick = {
                            vm.start(
                                bankroll        = bankrollStr.toDoubleOrNull()    ?: 1000.0,
                                baseBet         = baseBetStr.toDoubleOrNull()     ?: 1.0,
                                multiplier      = multiStr.toDoubleOrNull()       ?: 2.0,
                                winChance       = winChFloat.toDouble(),
                                rollOver        = rollOver,
                                lossIncreasePct = lossIncStr.toDoubleOrNull()     ?: 100.0,
                                winIncreasePct  = winIncStr.toDoubleOrNull()      ?: 0.0,
                                numberOfBets    = numBetsStr.toIntOrNull()        ?: 0,
                                isNewSession    = !sessionActive,
                                stopOnProfitAmt = stopProfitStr.toDoubleOrNull()  ?: 0.0,
                                stopOnLossAmt   = stopLossStr.toDoubleOrNull()    ?: 0.0
                            )
                        },
                        colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.Green),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(
                            if (sessionActive) "RESUME" else "START AUTOBET",
                            style = CTPType.LabelLarge,
                            color = CTPColors.Black
                        )
                    }
                } else {
                    Button(
                        onClick  = { vm.stop() },
                        colors   = ButtonDefaults.buttonColors(containerColor = CTPColors.RedDim),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("STOP", style = CTPType.LabelLarge, color = Color.White)
                    }
                }

                if (sessionActive || totalBets > 0) {
                    Button(
                        onClick = { vm.resetSession() },
                        colors  = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = CTPColors.TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RESET", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    }
                }
            }
        }

        // ── LIVE FEED ─────────────────────────────────────────────────────────
        // ── LIVE FEED ─────────────────────────────────────────────────────────
        if (totalBets > 0) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up all remaining screen space
                    .padding(horizontal = 6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                // 1. Wrap the Main Stats in an item block
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip("Balance", "%.4f".format(balance), CTPColors.Dice, Modifier.weight(1f))
                        StatChip("P/L", "${if (netProfit >= 0) "+" else ""}${"%.4f".format(netProfit)}", if (netProfit >= 0) CTPColors.Green else CTPColors.Red, Modifier.weight(1f))
                        StatChip("Bets", "%,d".format(totalBets), CTPColors.TextSecondary, Modifier.weight(0.8f))
                    }
                }
                
                // 2. Wrap the Advanced Pro Stats in an item block
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip("Peak Profit", "+%.4f".format(peakProfit), CTPColors.Green, Modifier.weight(1f))
                        StatChip("Max Drawdown", "-%.4f".format(maxDrawdown), CTPColors.Mines, Modifier.weight(1f))
                        StatChip("Streaks (W/L)", "$winStreak / $lossStreak", CTPColors.TextMuted, Modifier.weight(1f))
                    }
                    
                    Text("LIVE FEED", style = CTPType.LabelLarge, color = CTPColors.TextMuted, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
                }

                // 3. Keep your list of bets
                itemsIndexed(bets) { index, bet ->
                    val rowBackground = if (index % 2 == 0) Color.Transparent else CTPColors.CardElevated.copy(alpha = 0.4f)
                    
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(rowBackground)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.width(36.dp).height(22.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (bet.win) CTPColors.GreenGlow else CTPColors.RedGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (bet.win) "WIN" else "LOSS",
                                style = CTPType.LabelMedium.copy(fontSize = 9.sp),
                                color = if (bet.win) CTPColors.Green else CTPColors.Red
                            )
                        }
                        Text("%.2f".format(bet.roll), style = CTPType.Mono, color = CTPColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text("%.6f".format(bet.betSize), style = CTPType.Mono, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f))
                        Text(
                            "${if (bet.profit >= 0) "+" else ""}${"%.6f".format(bet.profit)}",
                            style = CTPType.Mono,
                            color = if (bet.profit >= 0) CTPColors.Green else CTPColors.Red
                        )
                    }
                }
            }
        }
    }
}    

// ── Visualizer ────────────────────────────────────────────────────────────────

@Composable
fun DiceRollVisualizer(
    rollValue: Float,
    winChance: Float,
    rollOver: Boolean,
    lastWin: Boolean?,
    running: Boolean,
    onSliderDrag: (Float) -> Unit
) {
    val threshold = if (rollOver) 100f - winChance else winChance

    val animatedRoll by animateFloatAsState(
        targetValue   = rollValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "roll_pos"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "pulse"
    )

    val resultColor = when (lastWin) {
        true  -> CTPColors.Green
        false -> CTPColors.Red
        null  -> CTPColors.TextMuted
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CTPColors.Card)
            .padding(12.dp)
    ) {
        // Scale labels
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "25", "50", "75", "100").forEach { label ->
                Text(label, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
            }
        }

        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp)) {
            val trackWidthPx = constraints.maxWidth.toFloat()

            Canvas(
                Modifier.fillMaxSize().pointerInput(running) {
                    if (!running) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        change.consume()
                                        val pct = (change.position.x / trackWidthPx * 100f).coerceIn(0.1f, 99.9f)
                                        onSliderDrag(pct)
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                val trackH = 14.dp.toPx()
                val trackY = (size.height - trackH) / 2f
                val thumbX = (animatedRoll / 100f * size.width).coerceIn(0f, size.width)
                val splitX = (threshold / 100f * size.width).coerceIn(0f, size.width)

                drawRoundRect(
                    color        = CTPColors.CardElevated,
                    topLeft      = Offset(0f, trackY),
                    size         = Size(size.width, trackH),
                    cornerRadius = CornerRadius(7.dp.toPx())
                )

                if (rollOver) {
                    drawRoundRect(
                        color        = CTPColors.Red.copy(alpha = 0.75f),
                        topLeft      = Offset(0f, trackY),
                        size         = Size(splitX, trackH),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                    drawRoundRect(
                        color        = CTPColors.Green.copy(alpha = 0.75f),
                        topLeft      = Offset(splitX, trackY),
                        size         = Size(size.width - splitX, trackH),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        color        = CTPColors.Green.copy(alpha = 0.75f),
                        topLeft      = Offset(0f, trackY),
                        size         = Size(splitX, trackH),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                    drawRoundRect(
                        color        = CTPColors.Red.copy(alpha = 0.75f),
                        topLeft      = Offset(splitX, trackY),
                        size         = Size(size.width - splitX, trackH),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                }

                drawLine(
                    color       = Color.White.copy(alpha = 0.4f),
                    start       = Offset(splitX, trackY - 4.dp.toPx()),
                    end         = Offset(splitX, trackY + trackH + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap         = StrokeCap.Round
                )

                if (running && lastWin != null) {
                    drawCircle(
                        color  = resultColor.copy(alpha = pulseAlpha * 0.35f),
                        radius = 20.dp.toPx(),
                        center = Offset(thumbX, size.height / 2f)
                    )
                }

                drawCircle(color = Color.White, radius = 16.dp.toPx(), center = Offset(thumbX, size.height / 2f))

                for (i in -1..1) {
                    drawLine(
                        color       = Color.Gray.copy(alpha = 0.5f),
                        start       = Offset(thumbX + i * 4.dp.toPx(), size.height / 2f - 5.dp.toPx()),
                        end         = Offset(thumbX + i * 4.dp.toPx(), size.height / 2f + 5.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        cap         = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("ROLL", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Text("%.2f".format(rollValue), style = CTPType.MonoLarge.copy(fontSize = 26.sp), color = resultColor)
            }

            if (lastWin != null) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(resultColor.copy(alpha = if (running) pulseAlpha * 0.2f else 0.15f))
                        .border(1.dp, resultColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(if (lastWin == true) "WIN" else "LOSS", style = CTPType.HeadlineLarge, color = resultColor)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("WIN IF ${if (rollOver) ">" else "<"} ${"%.1f".format(threshold)}", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Text("${"%.4f".format(winChance)}%", style = CTPType.Mono, color = CTPColors.Green)
            }
        }
    }
}

// ── Field helper ──────────────────────────────────────────────────────────────

@Composable
fun DiceFieldBox(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
) {
    Column(modifier) {
        Text(label, style = CTPType.LabelMedium, color = CTPColors.TextMuted)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onChange,
            singleLine    = true,
            trailingIcon  = trailingContent ?: if (suffix.isNotEmpty()) {
                { Text(suffix, color = CTPColors.TextMuted, style = CTPType.BodyMedium) }
            } else null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = CTPColors.TextPrimary,
                unfocusedTextColor      = CTPColors.TextPrimary,
                focusedBorderColor      = CTPColors.Dice,
                unfocusedBorderColor    = CTPColors.Border,
                cursorColor             = CTPColors.Dice,
                focusedContainerColor   = CTPColors.CardElevated,
                unfocusedContainerColor = CTPColors.CardElevated
            ),
            textStyle = CTPType.Mono,
            shape     = RoundedCornerShape(8.dp),
            modifier  = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}
