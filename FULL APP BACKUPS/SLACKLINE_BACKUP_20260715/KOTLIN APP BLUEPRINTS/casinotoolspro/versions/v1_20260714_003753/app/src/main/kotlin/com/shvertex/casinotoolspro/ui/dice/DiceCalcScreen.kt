package com.shvertex.casinotoolspro.ui.dice

import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shvertex.casinotoolspro.core.ProbabilityMath
import com.shvertex.casinotoolspro.theme.*

@Composable
fun diceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = CTPColors.TextPrimary,
    unfocusedTextColor      = CTPColors.TextPrimary,
    focusedBorderColor      = CTPColors.Dice,
    unfocusedBorderColor    = CTPColors.Border,
    cursorColor             = CTPColors.Dice,
    focusedContainerColor   = CTPColors.CardElevated,
    unfocusedContainerColor = CTPColors.CardElevated
)

@Composable
fun DiceCalcScreen(onBack: () -> Unit) {
    // Input States
    var balanceStr    by remember { mutableStateOf("1000") }
    var baseBetStr    by remember { mutableStateOf("1") }
    var multiplierStr by remember { mutableStateOf("2.0000") }
    var winChanceStr  by remember { mutableStateOf("49.5000") }
    var lossIncStr    by remember { mutableStateOf("100") }

    // Result States (Only updated when Calculate is pressed)
    var calculatedResults by remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }
    var activeTableParams by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) }

    // Two-way sync: multiplier ↔ win chance (matches Stake behaviour instantly)
    var syncGuard by remember { mutableStateOf(false) }

    fun onMultiChange(v: String) {
        multiplierStr = v
        if (!syncGuard) {
            syncGuard = true
            v.toDoubleOrNull()?.let { m ->
                if (m >= 1.01) winChanceStr = "%.4f".format(ProbabilityMath.diceWinChance(m))
            }
            syncGuard = false
        }
    }

    fun onChanceChange(v: String) {
        winChanceStr = v
        if (!syncGuard) {
            syncGuard = true
            v.toDoubleOrNull()?.let { c ->
                if (c > 0.0 && c < 100.0) multiplierStr = "%.4f".format(ProbabilityMath.diceMultiplier(c))
            }
            syncGuard = false
        }
    }

    // Manual Calculate Action
    fun performCalculation() {
        val bal  = balanceStr.toDoubleOrNull()    ?: return
        val bet  = baseBetStr.toDoubleOrNull()    ?: return
        val mult = multiplierStr.toDoubleOrNull() ?: return
        val winCh= winChanceStr.toDoubleOrNull()  ?: return
        val lossI= lossIncStr.toDoubleOrNull()    ?: return
        
        if (bal <= 0 || bet <= 0 || mult < 1.01 || winCh <= 0) return

        val houseEdge    = ProbabilityMath.houseEdge(mult)
        val ev           = ProbabilityMath.expectedValue(bet, mult)
        val lossMulti    = 1.0 + lossI / 100.0
        val bustBets     = ProbabilityMath.martingaleBustBets(bal, bet, lossMulti)
        val probWinIn50  = ProbabilityMath.probAtLeastOneWin(winCh, 50) * 100.0
        val profitPerWin = bet * (mult - 1.0)
        val breakEven    = ProbabilityMath.breakEvenMultiplier(winCh)
        val rtp          = 100.0 - houseEdge
        val rollOver     = 100.0 - winCh

        calculatedResults = listOf(
            Triple("Win Chance",       "%.4f%%".format(winCh),        true),
            Triple("Roll Over",        "%.2f".format(rollOver),       true),
            Triple("Multiplier",       "%.4f×".format(mult),          true),
            Triple("House Edge",       "%.2f%%".format(houseEdge),    false),
            Triple("RTP",              "%.2f%%".format(rtp),          rtp >= 95),
            Triple("Exp. Value / Bet", "${if (ev >= 0) "+" else ""}%.6f".format(ev), ev >= 0),
            Triple("Profit Per Win",   "+%.6f".format(profitPerWin),  true),
            Triple("Break-even Mult",  "%.4f×".format(breakEven),     true),
            Triple("Bust in N Bets",   "$bustBets bets",              false),
            Triple("P(Win in 50)",     "%.2f%%".format(probWinIn50),  probWinIn50 >= 50)
        )
        activeTableParams = Triple(bal, bet, lossI)
    }

    // Manual Reset Action
    fun performReset() {
        balanceStr    = "1000"
        baseBetStr    = "1"
        multiplierStr = "2.0000"
        winChanceStr  = "49.5000"
        lossIncStr    = "100"
        calculatedResults = emptyList()
        activeTableParams = null
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader(
            title       = "Dice / Limbo Calculator",
            subtitle    = "Multiplier ↔ Win Chance auto-sync",
            accentColor = CTPColors.Dice,
            onBack      = onBack
        )

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val wc = winChanceStr.toDoubleOrNull()?.toFloat() ?: 49.5f
            CompactDiceStrip(winChance = wc)

            CTPCard(accentColor = CTPColors.Dice) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Multiplier", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = multiplierStr,
                            onValueChange = { onMultiChange(it) },
                            singleLine = true,
                            trailingIcon = { Text("×", color = CTPColors.TextMuted) },
                            colors = diceFieldColors(),
                            textStyle = CTPType.Mono,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Win Chance %", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = winChanceStr,
                            onValueChange = { onChanceChange(it) },
                            singleLine = true,
                            trailingIcon = { Text("%", color = CTPColors.TextMuted) },
                            colors = diceFieldColors(),
                            textStyle = CTPType.Mono,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(balanceStr,  { balanceStr = it },  "Bankroll",        Modifier.weight(1f))
                    CTPInput(baseBetStr,  { baseBetStr = it },  "Base Bet",        Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(lossIncStr, { lossIncStr = it }, "Loss Increase %  (0 = flat bet, 100 = Martingale)")
            }

            // --- Control Buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { performReset() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CTPColors.TextSecondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CTPColors.Border)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("RESET", style = CTPType.LabelLarge)
                }

                Button(
                    onClick = { performCalculation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CTPColors.Dice
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("CALCULATE", style = CTPType.LabelLarge, color = CTPColors.Black)
                }
            }
            // -----------------------

            if (calculatedResults.isNotEmpty()) {
                CTPCard(accentColor = CTPColors.Green) {
                    Text("ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    calculatedResults.forEach { (label, value, positive) ->
                        val color = when (label) {
                            "House Edge" -> CTPColors.Red
                            "Bust in N Bets" -> CTPColors.Mines
                            else -> if (positive) CTPColors.Green else CTPColors.Red
                        }
                        ResultRow(label = label, value = value, valueColor = color)
                        CTPDivider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            activeTableParams?.let { (bal, bet, lossI) ->
                MartingaleDepthTable(bankroll = bal, baseBet = bet, lossIncreasePct = lossI)
            }
        }
    }
}

// ── Compact dice strip ────────────────────────────────────────────────────────

@Composable
fun CompactDiceStrip(winChance: Float) {
    val lossZone = 100f - winChance
    Box(
        Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val splitX = lossZone / 100f * size.width
            drawRect(
                CTPColors.Red.copy(alpha = 0.7f),
                topLeft = Offset(0f, 0f),
                size    = Size(splitX, size.height)
            )
            drawRect(
                CTPColors.Green.copy(alpha = 0.7f),
                topLeft = Offset(splitX, 0f),
                size    = Size(size.width - splitX, size.height)
            )
        }
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                " LOSS  ${"%.1f".format(100f - winChance)}%",
                style    = CTPType.LabelMedium,
                color    = Color.White.copy(0.9f),
                modifier = Modifier.padding(start = 10.dp)
            )
            Text(
                "WIN  ${"%.4f".format(winChance)}% ",
                style    = CTPType.LabelMedium,
                color    = Color.White.copy(0.9f),
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
}

// ── Martingale depth table ────────────────────────────────────────────────────

@Composable
fun MartingaleDepthTable(bankroll: Double, baseBet: Double, lossIncreasePct: Double) {
    val lossMulti = 1.0 + lossIncreasePct / 100.0
    val rows = mutableListOf<Triple<Int, Double, Double>>()
    var bet = baseBet
    var totalRisk = 0.0
    var i = 1
    
    // Loop continues until the accumulated risk exceeds the available bankroll
    while (true) {
        totalRisk += bet
        rows.add(Triple(i, bet, totalRisk))
        
        // Break out of the loop once they bust (total risk exceeds bankroll)
        if (totalRisk >= bankroll) break
        
        bet *= lossMulti
        i++
        
        // Safety cap to prevent the app from freezing (ANR) with extreme inputs
        // e.g., $1M bankroll, $0.01 bet, 0% increase = infinite loop without this.
        if (i > 5000) break 
    }

    CTPCard(accentColor = CTPColors.Mines) {
        Text("LOSS STREAK DEPTH", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Streak",     style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Bet Size",   style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f))
            Text("Total Risk", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f))
            Text("% Bank",     style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
        }
        CTPDivider(Modifier.padding(vertical = 4.dp))
        
        rows.forEach { (streak, betSize, totalR) ->
            val pct = (totalR / bankroll) * 100.0
            val rowColor = when {
                pct > 100 -> CTPColors.Red   // Highlights the exact bet that busts the bankroll
                pct > 80 -> CTPColors.Red
                pct > 50 -> CTPColors.Mines
                else     -> CTPColors.TextPrimary
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("#$streak",                style = CTPType.Mono, color = rowColor, modifier = Modifier.weight(1f))
                Text("%.6f".format(betSize),    style = CTPType.Mono, color = rowColor, modifier = Modifier.weight(1.5f))
                Text("%.4f".format(totalR),     style = CTPType.Mono, color = rowColor, modifier = Modifier.weight(1.5f))
                Text("${"%.1f".format(pct)}%",  style = CTPType.Mono, color = rowColor, modifier = Modifier.weight(1f))
            }
        }

        // Show a warning if the safety cap was hit
        if (rows.size >= 5000) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Table capped at 5000 results to maintain device performance.",
                style = CTPType.LabelMedium,
                color = CTPColors.RedDim
            )
        }
    }
}
