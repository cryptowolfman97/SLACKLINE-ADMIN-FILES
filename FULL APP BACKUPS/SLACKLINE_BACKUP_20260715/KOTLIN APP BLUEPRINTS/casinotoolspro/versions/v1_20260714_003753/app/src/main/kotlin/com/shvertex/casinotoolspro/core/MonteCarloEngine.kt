package com.shvertex.casinotoolspro.core

import kotlin.math.*
import kotlin.random.Random

/** Result of a Monte Carlo simulation run */
data class MCResult(
    val sessions: Int,
    val avgProfit: Double,
    val medianProfit: Double,
    val bestSession: Double,
    val worstSession: Double,
    val winRate: Double,       // % of sessions with profit > 0
    val bustRate: Double,      // % of sessions that went broke
    val avgBets: Double,
    val longestLossStreak: Int,
    val stdDev: Double,
    val sharpeRatio: Double,   // risk-adjusted return (improvement over original)
    val profitFactor: Double,  // gross wins / gross losses
    val var95: Double,         // 95% Value at Risk (5th percentile loss)
    val profitDistribution: List<Double> = emptyList() // for histogram
)

/** Per-session configuration */
data class MCConfig(
    val bankroll: Double,
    val baseBet: Double,
    val multiplier: Double,
    val winChancePct: Double,
    val increaseOnLossPct: Double = 100.0,   // % to increase bet on loss (martingale = 100)
    val increaseOnWinPct: Double  = 0.0,     // % to increase bet on win  (anti-martingale)
    val stopProfitPct: Double     = 0.0,     // stop when profit >= X% of bankroll (0 = disabled)
    val stopLossPct: Double       = 0.0,     // stop when loss >= X% of bankroll (0 = disabled)
    val maxBets: Int              = 100,
    val sessions: Int             = 5_000,
    val maxBetCapPct: Double      = 0.0      // max bet as % of bankroll (0 = uncapped)
)

object MonteCarloEngine {

    /**
     * Run a full Monte Carlo simulation.
     * Designed to be called from a coroutine (not main thread).
     *
     * @param config simulation parameters
     * @param onProgress callback with (completed, total) for progress UI
     * @return aggregated MCResult
     */
    fun simulate(
        config: MCConfig,
        onProgress: ((Int, Int) -> Unit)? = null
    ): MCResult {
        val rng = Random(System.nanoTime())

        val profits = mutableListOf<Double>()
        var busts = 0
        var totalBets = 0L
        var globalLongestLoss = 0
        var totalWagered = 0.0
        var totalGrossWin = 0.0
        var totalGrossLoss = 0.0

        val winProb = config.winChancePct / 100.0
        val maxBetCap = if (config.maxBetCapPct > 0)
            config.bankroll * config.maxBetCapPct / 100.0 else Double.MAX_VALUE

        val stopProfit = if (config.stopProfitPct > 0)
            config.bankroll * config.stopProfitPct / 100.0 else Double.MAX_VALUE
        val stopLoss = if (config.stopLossPct > 0)
            config.bankroll * config.stopLossPct / 100.0 else Double.MAX_VALUE

        val batchSize = maxOf(100, config.sessions / 50)

        for (s in 0 until config.sessions) {
            var balance = config.bankroll
            var currentBet = config.baseBet
            var sessionBets = 0
            var lossStreak = 0
            var sessionLossStreak = 0

            while (sessionBets < config.maxBets && currentBet > 0 && currentBet <= balance) {
                // Stop conditions
                val sessionProfit = balance - config.bankroll
                if (config.stopProfitPct > 0 && sessionProfit >= stopProfit) break
                if (config.stopLossPct > 0 && -sessionProfit >= stopLoss) break

                val bet = minOf(currentBet, balance, maxBetCap)
                balance -= bet
                totalWagered += bet
                sessionBets++

                val win = rng.nextDouble() < winProb
                if (win) {
                    val payout = bet * config.multiplier
                    balance += payout
                    totalGrossWin += payout - bet
                    lossStreak = 0
                    currentBet = if (config.increaseOnWinPct > 0)
                        config.baseBet * (1.0 + config.increaseOnWinPct / 100.0).pow(0.0) // reset-like
                    else
                        config.baseBet
                } else {
                    totalGrossLoss += bet
                    lossStreak++
                    sessionLossStreak = maxOf(sessionLossStreak, lossStreak)
                    currentBet = bet * (1.0 + config.increaseOnLossPct / 100.0)
                }
            }

            globalLongestLoss = maxOf(globalLongestLoss, sessionLossStreak)
            totalBets += sessionBets

            val profit = balance - config.bankroll
            profits.add(profit)
            if (profit < -config.bankroll * 0.99) busts++

            if (onProgress != null && (s + 1) % batchSize == 0) {
                onProgress(s + 1, config.sessions)
            }
        }

        profits.sort()

        val n = profits.size
        val avgProfit = profits.average()
        val medianProfit = if (n % 2 == 0)
            (profits[n / 2 - 1] + profits[n / 2]) / 2.0
        else profits[n / 2]

        val stdDev = sqrt(profits.map { (it - avgProfit).pow(2.0) }.average())
        val sharpe = if (stdDev > 0) avgProfit / stdDev else 0.0
        val profitFactor = if (totalGrossLoss > 0) totalGrossWin / totalGrossLoss else
            if (totalGrossWin > 0) Double.MAX_VALUE else 0.0
        val var95 = profits[(n * 0.05).toInt().coerceIn(0, n - 1)]

        // Downsample distribution for charting (50 points max)
        val dist = if (n > 50) {
            val step = n / 50
            profits.filterIndexed { i, _ -> i % step == 0 }
        } else profits.toList()

        return MCResult(
            sessions       = config.sessions,
            avgProfit      = avgProfit,
            medianProfit   = medianProfit,
            bestSession    = profits.last(),
            worstSession   = profits.first(),
            winRate        = profits.count { it > 0 }.toDouble() / n * 100.0,
            bustRate       = busts.toDouble() / n * 100.0,
            avgBets        = totalBets.toDouble() / n,
            longestLossStreak = globalLongestLoss,
            stdDev         = stdDev,
            sharpeRatio    = sharpe,
            profitFactor   = profitFactor,
            var95          = var95,
            profitDistribution = dist
        )
    }

    /**
     * Score a strategy for evolution ranking.
     * Higher is better. Balances return vs risk.
     */
    fun scoreResult(result: MCResult): Double {
        val returnScore  = result.avgProfit.coerceAtLeast(-1000.0)
        val bustPenalty  = result.bustRate / 100.0 * 3.0
        val winBonus     = result.winRate  / 100.0
        val sharpBonus   = result.sharpeRatio.coerceIn(-5.0, 5.0) * 0.5
        return returnScore * (1.0 - bustPenalty) + winBonus + sharpBonus
    }
}

// ── Keno Engine ───────────────────────────────────────────────────────────────

data class KenoMCConfig(
    val bankroll: Double,
    val picks: Int,
    val minHits: Int,
    val baseBet: Double,
    val increaseOnLossPct: Double = 50.0,
    val maxBets: Int = 50,
    val sessions: Int = 3_000
)

data class KenoMCResult(
    val sessions: Int,
    val picks: Int,
    val minHits: Int,
    val winProbability: Double,
    val payoutMultiplier: Double,
    val avgProfit: Double,
    val medianProfit: Double,
    val winRate: Double,
    val bustRate: Double,
    val longestLossStreak: Int
)

object KenoMonteCarloEngine {

    fun simulate(config: KenoMCConfig): KenoMCResult {
        val rng = Random(System.nanoTime())
        val winProb = ProbabilityMath.kenoWinProbability(config.picks, config.minHits)
        val payout  = ProbabilityMath.kenoFairMultiplier(config.picks, config.minHits)

        val profits = mutableListOf<Double>()
        var busts = 0
        var globalLongest = 0

        repeat(config.sessions) {
            var balance = config.bankroll
            var bet = config.baseBet
            var lossStreak = 0

            for (round in 0 until config.maxBets) {
                if (bet > balance || bet <= 0) { busts++; break }
                balance -= bet
                val win = rng.nextDouble() < winProb
                if (win) {
                    balance += bet * payout
                    lossStreak = 0
                    bet = config.baseBet
                } else {
                    lossStreak++
                    globalLongest = maxOf(globalLongest, lossStreak)
                    bet *= (1.0 + config.increaseOnLossPct / 100.0)
                }
            }
            profits.add(balance - config.bankroll)
        }

        profits.sort()
        val n = profits.size
        return KenoMCResult(
            sessions          = config.sessions,
            picks             = config.picks,
            minHits           = config.minHits,
            winProbability    = winProb * 100.0,
            payoutMultiplier  = payout,
            avgProfit         = profits.average(),
            medianProfit      = profits[n / 2],
            winRate           = profits.count { it > 0 }.toDouble() / n * 100.0,
            bustRate          = busts.toDouble() / n * 100.0,
            longestLossStreak = globalLongest
        )
    }
}
