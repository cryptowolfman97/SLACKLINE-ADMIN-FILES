package com.shvertex.casinotoolspro.core

import kotlin.math.*

/**
 * Central math library for all probability calculations.
 * Pure functions — no side effects, fully testable.
 */
object ProbabilityMath {

    // ── Dice / Limbo ──────────────────────────────────────────────────────────

    /** Win probability from multiplier (house edge = 1%) */
    fun diceWinChance(multiplier: Double): Double =
        (99.0 / multiplier).coerceIn(0.01, 99.99)

    /** Multiplier from win chance */
    fun diceMultiplier(winChance: Double): Double =
        (99.0 / winChance.coerceIn(0.01, 99.99))

    /** House edge as a percentage */
    fun houseEdge(multiplier: Double): Double {
        val prob = diceWinChance(multiplier) / 100.0
        return (1.0 - prob * multiplier) * 100.0
    }

    /**
     * Expected value of a single bet
     * positive = player edge, negative = house edge
     */
    fun expectedValue(bet: Double, multiplier: Double): Double {
        val prob = diceWinChance(multiplier) / 100.0
        return bet * (prob * (multiplier - 1.0) - (1.0 - prob))
    }

    /**
     * Martingale: number of bets before bankroll busts
     * given consecutive losses starting from baseBet
     */
    fun martingaleBustBets(bankroll: Double, baseBet: Double, multiplierOnLoss: Double = 2.0): Int {
        var balance = bankroll
        var bet = baseBet
        var count = 0
        while (bet <= balance && count < 100_000) {
            balance -= bet
            bet *= multiplierOnLoss
            count++
        }
        return count
    }

    /**
     * Break-even multiplier for a given win chance
     * (no house edge)
     */
    fun breakEvenMultiplier(winChancePct: Double): Double =
        100.0 / winChancePct.coerceAtLeast(0.001)

    /**
     * Probability of at least one win in N bets
     */
    fun probAtLeastOneWin(winChancePct: Double, bets: Int): Double {
        val pLose = 1.0 - winChancePct / 100.0
        return 1.0 - pLose.pow(bets.toDouble())
    }

    /**
     * Kelly Criterion optimal bet fraction
     * b = decimal odds - 1, p = win prob, q = loss prob
     */
    fun kellyCriterion(decimalOdds: Double, winProbability: Double): Double {
        val b = decimalOdds - 1.0
        val p = winProbability
        val q = 1.0 - p
        return ((b * p - q) / b).coerceAtLeast(0.0)
    }

    fun kellyBetAmount(bankroll: Double, decimalOdds: Double, winProbability: Double, fraction: Double = 1.0): Double {
        val kelly = kellyCriterion(decimalOdds, winProbability)
        return bankroll * kelly * fraction
    }

    // ── Keno ─────────────────────────────────────────────────────────────────

    /** Combination C(n, k) */
    fun combination(n: Int, k: Int): Long {
        if (k > n || k < 0) return 0L
        if (k == 0 || k == n) return 1L
        val kk = minOf(k, n - k)
        var result = 1L
        for (i in 0 until kk) {
            result = result * (n - i) / (i + 1)
        }
        return result
    }

    /**
     * Hypergeometric probability: 40-ball keno board, 10 drawn
     * Probability of exactly `hits` out of `picks` chosen by player
     */
    fun kenoHypergeometric(picks: Int, hits: Int, boardSize: Int = 40, drawn: Int = 10): Double {
        val total = combination(boardSize, drawn)
        if (total == 0L) return 0.0
        val ways = combination(picks, hits).toLong() * combination(boardSize - picks, drawn - hits).toLong()
        return ways.toDouble() / total.toDouble()
    }

    /** Probability of at least `minHits` on a keno ticket */
    fun kenoWinProbability(picks: Int, minHits: Int, boardSize: Int = 40, drawn: Int = 10): Double {
        var prob = 0.0
        for (h in minHits..minOf(picks, drawn)) {
            prob += kenoHypergeometric(picks, h, boardSize, drawn)
        }
        return prob
    }

    /** Fair payout multiplier for keno (94% RTP) */
    fun kenoFairMultiplier(picks: Int, minHits: Int, rtp: Double = 0.94): Double {
        val prob = kenoWinProbability(picks, minHits).coerceAtLeast(1e-9)
        return (1.0 / prob) * rtp
    }

    // ── Mines ─────────────────────────────────────────────────────────────────

    /**
     * Probability of surviving `picks` safe clicks given `mines` on a 25-tile grid
     */
    fun minesSurvivalProbability(mines: Int, picks: Int, gridSize: Int = 25): Double {
        if (picks <= 0) return 1.0
        if (mines >= gridSize) return 0.0
        var prob = 1.0
        val safe = gridSize - mines
        for (i in 0 until picks) {
            val remaining = gridSize - i
            val safeRemaining = safe - i
            if (safeRemaining <= 0 || remaining <= 0) return 0.0
            prob *= safeRemaining.toDouble() / remaining.toDouble()
        }
        return prob
    }

    /**
     * Fair payout multiplier for mines at given picks (96% RTP)
     */
    fun minesMultiplier(mines: Int, picks: Int, rtp: Double = 0.96): Double {
        val prob = minesSurvivalProbability(mines, picks).coerceAtLeast(1e-12)
        return (1.0 / prob) * rtp
    }

    /** Expected value per cash-out at given pick level */
    fun minesExpectedValue(bet: Double, mines: Int, picks: Int): Double {
        val prob = minesSurvivalProbability(mines, picks)
        val mult = minesMultiplier(mines, picks)
        return bet * (prob * mult - 1.0)
    }

    // ── Blackjack ─────────────────────────────────────────────────────────────

    /** Simplified basic strategy house edge by rule set */
    fun blackjackHouseEdge(
        decks: Int = 6,
        dealerHitsSoft17: Boolean = true,
        doubleAfterSplit: Boolean = true,
        surrenderAllowed: Boolean = false
    ): Double {
        var edge = 0.006  // 6-deck base: ~0.6%
        edge -= (6 - decks) * 0.00058   // fewer decks = lower edge
        if (!dealerHitsSoft17) edge -= 0.0022
        if (!doubleAfterSplit) edge += 0.0014
        if (surrenderAllowed) edge -= 0.0008
        return (edge * 100.0).coerceIn(0.1, 5.0)
    }

    // ── Sports / Value Betting ────────────────────────────────────────────────

    /** Convert American odds to decimal */
    fun americanToDecimal(american: Int): Double = when {
        american > 0  -> (american / 100.0) + 1.0
        american < 0  -> (100.0 / -american) + 1.0
        else          -> 1.0
    }

    /** Convert decimal to implied probability */
    fun decimalToImplied(decimal: Double): Double =
        if (decimal <= 0) 0.0 else 1.0 / decimal

    /** Betting edge = estimated win prob - implied prob */
    fun bettingEdge(estimatedWinProb: Double, decimalOdds: Double): Double =
        estimatedWinProb - decimalToImplied(decimalOdds)

    /** Expected value of a bet */
    fun sportsBetEV(stake: Double, decimalOdds: Double, estimatedWinProb: Double): Double {
        val net = stake * (decimalOdds - 1.0)
        val loss = stake
        return net * estimatedWinProb - loss * (1.0 - estimatedWinProb)
    }

    /** Arbitrage: inverse sum of odds. < 1.0 = arb exists */
    fun arbitrageInverseSum(vararg odds: Double): Double =
        odds.sumOf { 1.0 / it.coerceAtLeast(1.001) }

    /** Stakes for each leg of an arbitrage to guarantee equal payout */
    fun arbitrageStakes(totalStake: Double, vararg odds: Double): List<Double> {
        val invSum = arbitrageInverseSum(*odds)
        return odds.map { totalStake * (1.0 / it) / invSum }
    }

    /** Parlay combined odds */
    fun parlayOdds(vararg legs: Double): Double =
        legs.fold(1.0) { acc, odds -> acc * odds }

    /** Parlay win probability (assuming independent events) */
    fun parlayWinProb(vararg legProbs: Double): Double =
        legProbs.fold(1.0) { acc, p -> acc * p }

    // ── Compound Growth ───────────────────────────────────────────────────────

    /** Compound growth: A = P(1 + r)^n */
    fun compoundGrowth(principal: Double, ratePercentage: Double, periods: Int): Double =
        principal * (1.0 + ratePercentage / 100.0).pow(periods.toDouble())

    /** Periods to reach target given rate */
    fun periodsToTarget(principal: Double, target: Double, ratePercentage: Double): Int {
        if (ratePercentage <= 0 || principal <= 0) return -1
        val r = 1.0 + ratePercentage / 100.0
        return ceil(ln(target / principal) / ln(r)).toInt()
    }

    // ── Ruin / Survival ───────────────────────────────────────────────────────

    /**
     * Gambler's ruin probability: probability of reaching `target`
     * before busting from `start` with win probability `p`
     */
    fun gamblerRuinSurvival(start: Int, target: Int, p: Double): Double {
        val q = 1.0 - p
        return when {
            abs(p - 0.5) < 1e-10 -> start.toDouble() / target.toDouble()
            else -> {
                val r = q / p
                (1.0 - r.pow(start.toDouble())) / (1.0 - r.pow(target.toDouble()))
            }
        }
    }
}
