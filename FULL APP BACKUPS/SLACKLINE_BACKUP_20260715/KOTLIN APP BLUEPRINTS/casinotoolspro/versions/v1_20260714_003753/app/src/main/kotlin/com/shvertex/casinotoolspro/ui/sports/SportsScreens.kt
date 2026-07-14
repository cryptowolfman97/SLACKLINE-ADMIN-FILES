package com.shvertex.casinotoolspro.ui.sports

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shvertex.casinotoolspro.core.ProbabilityMath
import com.shvertex.casinotoolspro.theme.*

// ── Kelly Calculator ──────────────────────────────────────────────────────────

@Composable
fun KellyScreen(onBack: () -> Unit) {
    var bankrollStr by remember { mutableStateOf("1000") }
    var oddsStr     by remember { mutableStateOf("2.10") }
    var winProbStr  by remember { mutableStateOf("55") }
    var fractionStr by remember { mutableStateOf("25") }  // % of full Kelly to use

    data class KellyResult(
        val fullKellyPct: Double, val fractionalKellyPct: Double, val betAmount: Double,
        val edge: Double, val ev: Double, val verdict: String
    )

    val result: KellyResult? = remember(bankrollStr, oddsStr, winProbStr, fractionStr) {
        val bank = bankrollStr.toDoubleOrNull() ?: return@remember null
        val odds = oddsStr.toDoubleOrNull()     ?: return@remember null
        val prob = (winProbStr.toDoubleOrNull() ?: return@remember null) / 100.0
        val frac = (fractionStr.toDoubleOrNull() ?: 25.0) / 100.0
        if (odds <= 1.0 || prob <= 0 || prob >= 1) return@remember null

        val fullKelly = ProbabilityMath.kellyCriterion(odds, prob)
        val betAmt    = ProbabilityMath.kellyBetAmount(bank, odds, prob, frac)
        val edge      = ProbabilityMath.bettingEdge(prob, odds) * 100.0
        val ev        = ProbabilityMath.sportsBetEV(betAmt, odds, prob)
        val verdict   = when {
            edge < 0     -> "NO EDGE — Avoid this bet"
            edge < 2     -> "MARGINAL — Thin edge, small stake only"
            edge < 5     -> "GOOD VALUE — Solid bet"
            else         -> "STRONG EDGE — High confidence"
        }
        KellyResult(fullKelly * 100, fullKelly * frac * 100, betAmt, edge, ev, verdict)
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Kelly Calculator", "Optimal bet sizing", CTPColors.Sports, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Sports) {
                Text("KELLY CRITERION", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The Kelly Criterion finds the bet size that maximizes long-run bankroll growth. Use a fraction (e.g. 25%) for safety.",
                    style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                )
            }

            CTPCard(accentColor = CTPColors.Sports) {
                Text("INPUTS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(bankrollStr, { bankrollStr = it }, "Bankroll", Modifier.weight(1f))
                    CTPInput(oddsStr,     { oddsStr = it },     "Decimal Odds", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(winProbStr,  { winProbStr = it },  "Estimated Win %", Modifier.weight(1f))
                    CTPInput(fractionStr, { fractionStr = it }, "Kelly Fraction %", Modifier.weight(1f))
                }
            }

            result?.let { r ->
                CTPCard(accentColor = CTPColors.Green) {
                    Text("KELLY ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    ResultRow("Full Kelly %",       "%.2f%%".format(r.fullKellyPct))
                    ResultRow("Fractional Kelly %", "%.2f%%".format(r.fractionalKellyPct))
                    ResultRow("Recommended Bet",    "%.4f".format(r.betAmount), CTPColors.Green)
                    ResultRow("Betting Edge",        "${if (r.edge >= 0) "+" else ""}%.2f%%".format(r.edge),
                        if (r.edge > 0) CTPColors.Green else CTPColors.Red)
                    ResultRow("Expected Value",      "${if (r.ev >= 0) "+" else ""}%.4f".format(r.ev),
                        if (r.ev > 0) CTPColors.Green else CTPColors.Red)
                    Spacer(Modifier.height(8.dp))
                    VerdictBadge(r.verdict, if (r.edge > 2) CTPColors.Green else if (r.edge > 0) CTPColors.Dice else CTPColors.Red)
                }
            }
        }
    }
}

// ── Parlay Analyzer ───────────────────────────────────────────────────────────

@Composable
fun ParlayScreen(onBack: () -> Unit) {
    // Up to 8 legs
    var legCount by remember { mutableIntStateOf(3) }
    val odds = remember { mutableStateListOf("2.10", "1.85", "1.95", "2.20", "1.75", "2.05", "1.90", "2.30") }
    val probs = remember { mutableStateListOf("55", "60", "52", "48", "58", "50", "54", "46") }
    var stakeStr by remember { mutableStateOf("100") }

    val result = remember(legCount, odds.toList(), probs.toList(), stakeStr) {
        val stake      = stakeStr.toDoubleOrNull() ?: return@remember null
        val legOdds    = odds.take(legCount).mapNotNull { it.toDoubleOrNull() }
        val legProbs   = probs.take(legCount).mapNotNull { it.toDoubleOrNull()?.div(100.0) }
        if (legOdds.size != legCount || legProbs.size != legCount) return@remember null
        if (legOdds.any { it <= 1.0 }) return@remember null

        val combinedOdds  = ProbabilityMath.parlayOdds(*legOdds.toDoubleArray())
        val winProb       = ProbabilityMath.parlayWinProb(*legProbs.toDoubleArray())
        val impliedProb   = ProbabilityMath.decimalToImplied(combinedOdds)
        val ev            = ProbabilityMath.sportsBetEV(stake, combinedOdds, winProb)
        val payout        = stake * combinedOdds
        val edge          = (winProb - impliedProb) * 100.0

        mapOf(
            "Legs"              to "$legCount",
            "Combined Odds"     to "%.4f×".format(combinedOdds),
            "True Win Prob"     to "%.2f%%".format(winProb * 100),
            "Implied Prob"      to "%.2f%%".format(impliedProb * 100),
            "Your Edge"         to "${if (edge >= 0) "+" else ""}%.2f%%".format(edge),
            "Stake"             to "%.2f".format(stake),
            "Potential Payout"  to "%.2f".format(payout),
            "Potential Profit"  to "+%.2f".format(payout - stake),
            "Expected Value"    to "${if (ev >= 0) "+" else ""}%.4f".format(ev),
        ) to edge
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Parlay Analyzer", "Multi-leg bet calculator", CTPColors.Sports, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Legs:", style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
                listOf(2, 3, 4, 5, 6, 8).forEach { n ->
                    FilterChip(
                        selected = legCount == n,
                        onClick = { legCount = n },
                        label = { Text("$n", style = CTPType.LabelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CTPColors.Sports,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            CTPInput(stakeStr, { stakeStr = it }, "Total Stake")

            (0 until legCount).forEach { i ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(odds[i], { odds[i] = it }, "Leg ${i + 1} Odds", Modifier.weight(1f))
                    CTPInput(probs[i], { probs[i] = it }, "Est Win %", Modifier.weight(1f))
                }
            }

            result?.let { (map, edge) ->
                CTPCard(accentColor = if (edge > 0) CTPColors.Green else CTPColors.Red) {
                    Text("PARLAY ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    map.forEach { (k, v) ->
                        val color = when {
                            k.contains("Edge") || k.contains("EV") -> if (edge > 0) CTPColors.Green else CTPColors.Red
                            k.contains("Payout") || k.contains("Profit") -> CTPColors.Green
                            k.contains("Odds") -> CTPColors.Dice
                            else -> CTPColors.TextPrimary
                        }
                        ResultRow(k, v, color)
                        CTPDivider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ── Arbitrage Calculator ──────────────────────────────────────────────────────

@Composable
fun ArbitrageScreen(onBack: () -> Unit) {
    var stakeStr  by remember { mutableStateOf("1000") }
    var oddsAStr  by remember { mutableStateOf("2.10") }
    var oddsBStr  by remember { mutableStateOf("2.05") }
    var oddsCStr  by remember { mutableStateOf("") }  // optional 3-way
    var threeWay  by remember { mutableStateOf(false) }

    data class ArbResult(
        val invSum: Double,
        val arbExists: Boolean,
        val stakes: List<Double>,
        val payouts: List<Double>,
        val guaranteedProfit: Double,
        val roi: Double
    )

    val result: ArbResult? = remember(stakeStr, oddsAStr, oddsBStr, oddsCStr, threeWay) {
        val stake = stakeStr.toDoubleOrNull() ?: return@remember null
        val oa = oddsAStr.toDoubleOrNull()    ?: return@remember null
        val ob = oddsBStr.toDoubleOrNull()    ?: return@remember null
        if (oa <= 1.0 || ob <= 1.0) return@remember null
        val oddsList = if (threeWay) {
            val oc = oddsCStr.toDoubleOrNull() ?: return@remember null
            if (oc <= 1.0) return@remember null
            listOf(oa, ob, oc)
        } else listOf(oa, ob)

        val invSum = ProbabilityMath.arbitrageInverseSum(*oddsList.toDoubleArray())
        val stakes = ProbabilityMath.arbitrageStakes(stake, *oddsList.toDoubleArray())
        val payouts = stakes.zip(oddsList).map { (s, o) -> s * o }
        val lockedPayout = payouts.min()
        val profit = lockedPayout - stake

        ArbResult(
            invSum = invSum,
            arbExists = invSum < 1.0,
            stakes = stakes,
            payouts = payouts,
            guaranteedProfit = profit,
            roi = (profit / stake) * 100.0
        )
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Arbitrage Calculator", "Guaranteed profit finder", CTPColors.Sports, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Sports) {
                Text("When the inverse sum of odds < 1.0, a guaranteed profit exists regardless of outcome.", style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = threeWay, onCheckedChange = { threeWay = it }, colors = SwitchDefaults.colors(checkedThumbColor = CTPColors.Sports))
                Spacer(Modifier.width(8.dp))
                Text("3-way market", style = CTPType.BodyMedium, color = CTPColors.TextSecondary)
            }

            CTPInput(stakeStr, { stakeStr = it }, "Total Stake")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CTPInput(oddsAStr, { oddsAStr = it }, "Outcome A Odds", Modifier.weight(1f))
                CTPInput(oddsBStr, { oddsBStr = it }, "Outcome B Odds", Modifier.weight(1f))
            }
            if (threeWay) {
                CTPInput(oddsCStr, { oddsCStr = it }, "Outcome C Odds")
            }

            result?.let { r ->
                CTPCard(accentColor = if (r.arbExists) CTPColors.Green else CTPColors.Red) {
                    Text("ARBITRAGE ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    ResultRow("Inverse Sum", "%.6f".format(r.invSum),
                        if (r.invSum < 1.0) CTPColors.Green else CTPColors.Red)
                    r.stakes.forEachIndexed { i, s ->
                        ResultRow("Stake ${listOf("A","B","C")[i]}", "%.4f".format(s))
                        ResultRow("Payout ${listOf("A","B","C")[i]}", "%.4f".format(r.payouts[i]))
                    }
                    CTPDivider(Modifier.padding(vertical = 4.dp))
                    ResultRow("Guaranteed Profit", "${if (r.guaranteedProfit >= 0) "+" else ""}%.4f".format(r.guaranteedProfit),
                        if (r.guaranteedProfit > 0) CTPColors.Green else CTPColors.Red)
                    ResultRow("Guaranteed ROI", "%.2f%%".format(r.roi),
                        if (r.roi > 0) CTPColors.Green else CTPColors.Red)
                    Spacer(Modifier.height(8.dp))
                    VerdictBadge(
                        if (r.arbExists) "ARBITRAGE FOUND" else "NO ARBITRAGE",
                        if (r.arbExists) CTPColors.Green else CTPColors.Red
                    )
                }
            }
        }
    }
}

// ── Value Bet Calculator ──────────────────────────────────────────────────────

@Composable
fun ValueBetScreen(onBack: () -> Unit) {
    var stakeStr  by remember { mutableStateOf("100") }
    var oddsStr   by remember { mutableStateOf("2.10") }
    var winPctStr by remember { mutableStateOf("55") }

    val result = remember(stakeStr, oddsStr, winPctStr) {
        val stake = stakeStr.toDoubleOrNull()            ?: return@remember null
        val odds  = oddsStr.toDoubleOrNull()             ?: return@remember null
        val prob  = (winPctStr.toDoubleOrNull() ?: return@remember null) / 100.0
        if (odds <= 1.0 || prob <= 0 || prob >= 1) return@remember null

        val implied  = ProbabilityMath.decimalToImplied(odds) * 100.0
        val edge     = ProbabilityMath.bettingEdge(prob, odds) * 100.0
        val ev       = ProbabilityMath.sportsBetEV(stake, odds, prob)
        val payout   = stake * odds
        val roi      = (ev / stake) * 100.0
        val fairOdds = 1.0 / prob

        Triple(
            listOf(
                "Decimal Odds"    to "%.2f".format(odds),
                "Estimated Win %" to "%.2f%%".format(prob * 100),
                "Implied Prob"    to "%.2f%%".format(implied),
                "Fair Odds"       to "%.4f".format(fairOdds),
                "Your Edge"       to "${if (edge >= 0) "+" else ""}%.2f%%".format(edge),
                "Payout if Win"   to "%.4f".format(payout),
                "Net Profit"      to "+%.4f".format(payout - stake),
                "Expected Value"  to "${if (ev >= 0) "+" else ""}%.4f".format(ev),
                "Expected ROI"    to "${if (roi >= 0) "+" else ""}%.2f%%".format(roi),
            ), edge, ev
        )
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Value Bet Calculator", "Edge and EV analysis", CTPColors.Sports, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CTPCard(accentColor = CTPColors.Sports) {
                Text("INPUTS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CTPInput(stakeStr,  { stakeStr = it },  "Stake",         Modifier.weight(1f))
                    CTPInput(oddsStr,   { oddsStr = it },   "Decimal Odds",  Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CTPInput(winPctStr, { winPctStr = it }, "Estimated Win Probability %")
            }

            result?.let { (rows, edge, ev) ->
                CTPCard(accentColor = if (edge > 0) CTPColors.Green else CTPColors.Red) {
                    Text("VALUE ANALYSIS", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    rows.forEach { (k, v) ->
                        val color = when {
                            k.contains("Edge") || k.contains("EV") || k.contains("ROI") ->
                                if (edge > 0) CTPColors.Green else CTPColors.Red
                            k.contains("Profit") || k.contains("Payout") -> CTPColors.Green
                            else -> CTPColors.TextPrimary
                        }
                        ResultRow(k, v, color)
                        CTPDivider(Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    val verdict = when {
                        edge > 5  -> "STRONG VALUE BET"
                        edge > 2  -> "GOOD VALUE BET"
                        edge > 0  -> "MARGINAL VALUE"
                        else      -> "NO VALUE — SKIP"
                    }
                    VerdictBadge(verdict, if (edge > 2) CTPColors.Green else if (edge > 0) CTPColors.Dice else CTPColors.Red)
                }
            }
        }
    }
}
