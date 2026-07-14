package com.shvertex.casinotoolspro.ui.mines

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.casinotoolspro.core.ProbabilityMath
import com.shvertex.casinotoolspro.theme.*

@Composable
fun MinesAnalyticsScreen(onBack: () -> Unit) {
    var minesStr by remember { mutableStateOf("3") }
    var betStr   by remember { mutableStateOf("1.00") }

    val mines = minesStr.toIntOrNull()?.coerceIn(1, 24) ?: 3
    val bet   = betStr.toDoubleOrNull() ?: 1.0

    // Build pick-by-pick table
    val tableRows = remember(mines, bet) {
        (1..(25 - mines)).map { picks ->
            val survProb = ProbabilityMath.minesSurvivalProbability(mines, picks)
            val multi    = ProbabilityMath.minesMultiplier(mines, picks)
            val ev       = ProbabilityMath.minesExpectedValue(bet, mines, picks)
            Triple(picks, survProb * 100.0, multi to ev)
        }
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Mines Analytics", "Survival probability & payout matrix", CTPColors.Mines, onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Inputs
            CTPCard(accentColor = CTPColors.Mines) {
                Text("PARAMETERS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Mines Count", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = mines.toFloat(),
                            onValueChange = { minesStr = it.toInt().toString() },
                            valueRange = 1f..24f,
                            steps = 22,
                            colors = SliderDefaults.colors(thumbColor = CTPColors.Mines, activeTrackColor = CTPColors.Mines)
                        )
                        Text("$mines mines", style = CTPType.Mono, color = CTPColors.Mines)
                    }
                    CTPInput(betStr, { betStr = it }, "Bet Amount", Modifier.weight(1f))
                }
            }

            // Visual grid
            MinesGrid(mines = mines)

            // Pick table
            CTPCard(accentColor = CTPColors.Mines) {
                Text("PICK-BY-PICK ANALYSIS (96% RTP)", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(6.dp))

                // Header
                Row(Modifier.fillMaxWidth()) {
                    Text("Pick", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(0.7f))
                    Text("Survive %", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.2f))
                    Text("Multiplier", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.2f))
                    Text("EV", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
                }
                CTPDivider(Modifier.padding(vertical = 4.dp))

                tableRows.forEach { (pick, survPct, pair) ->
                    val (multi, ev) = pair
                    val riskColor = when {
                        survPct > 80 -> CTPColors.Green
                        survPct > 50 -> CTPColors.Dice
                        survPct > 25 -> CTPColors.Mines
                        else         -> CTPColors.Red
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#$pick", style = CTPType.Mono, color = riskColor, modifier = Modifier.weight(0.7f))
                        Text("${"%.2f".format(survPct)}%", style = CTPType.Mono, color = riskColor, modifier = Modifier.weight(1.2f))
                        Text("${"%.4f".format(multi)}×", style = CTPType.Mono, color = CTPColors.TextPrimary, modifier = Modifier.weight(1.2f))
                        Text("${if (ev >= 0) "+" else ""}${"%.4f".format(ev)}", style = CTPType.Mono,
                            color = if (ev >= 0) CTPColors.Green else CTPColors.Red, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Key stats for current mines setting
            CTPCard(accentColor = CTPColors.Green) {
                Text("QUICK STATS — $mines MINES", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                val safe = 25 - mines
                ResultRow("Safe Tiles", "$safe / 25")
                ResultRow("1-Pick Survive", "${"%.2f".format(ProbabilityMath.minesSurvivalProbability(mines, 1) * 100)}%")
                ResultRow("1-Pick Multiplier", "${"%.4f".format(ProbabilityMath.minesMultiplier(mines, 1))}×", CTPColors.Dice)
                ResultRow("5-Pick Survive", "${"%.2f".format(ProbabilityMath.minesSurvivalProbability(mines, 5) * 100)}%")
                ResultRow("5-Pick Multiplier", "${"%.4f".format(ProbabilityMath.minesMultiplier(mines, 5))}×", CTPColors.Dice)
                val optPick = tableRows.maxByOrNull { (_, _, p) -> p.second }?.first ?: 1
                ResultRow("Best EV Pick", "#$optPick", CTPColors.Green)
            }
        }
    }
}

@Composable
fun MinesGrid(mines: Int) {
    val tiles = 25
    val minePositions = remember(mines) { (0 until tiles).shuffled().take(mines).toSet() }

    CTPCard(accentColor = CTPColors.Mines, showAccent = false) {
        Text("SAMPLE BOARD — $mines mines", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            (0 until 5).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0 until 5).forEach { col ->
                        val idx = row * 5 + col
                        val isMine = idx in minePositions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isMine) CTPColors.RedDim else CTPColors.CardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isMine) "💣" else "💎",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("*Board layout randomizes on each view", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
    }
}
