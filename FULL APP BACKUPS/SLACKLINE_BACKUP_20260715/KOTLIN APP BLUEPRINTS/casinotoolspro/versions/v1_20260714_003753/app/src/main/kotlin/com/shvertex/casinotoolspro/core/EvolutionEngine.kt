package com.shvertex.casinotoolspro.core

import kotlin.math.abs
import kotlin.random.Random

/** A single candidate strategy genome */
data class StrategyGenome(
    val multiplier: Double,
    val winChancePct: Double,
    val increaseOnLossPct: Double,
    val baseBetPct: Double,    // base bet as % of bankroll
    val maxBets: Int,
    // Score assigned after evaluation
    val score: Double = 0.0,
    val result: MCResult? = null
)

data class KenoGenome(
    val picks: Int,
    val minHits: Int,
    val baseBet: Double,
    val increaseOnLossPct: Double,
    val score: Double = 0.0,
    val result: KenoMCResult? = null
)

data class MinesGenome(
    val mines: Int,
    val picks: Int,
    val baseBet: Double,
    val increaseOnLossPct: Double,
    val score: Double = 0.0,
    val avgProfit: Double = 0.0,
    val bustRate: Double = 0.0,
    val winRate: Double = 0.0
)

enum class EvolutionGoal {
    MAX_PROFIT,     // maximize average profit
    MIN_RISK,       // minimize bust rate
    BEST_SHARPE,    // balance return and risk
    MAX_WIN_RATE    // maximize sessions with profit
}

object EvolutionEngine {

    private val rng = Random(System.nanoTime())

    // ── Dice / Limbo Evolution ────────────────────────────────────────────────

    fun evolveDice(
        bankroll: Double,
        generations: Int = 20,
        populationSize: Int = 40,
        sessionsPerEval: Int = 500,
        goal: EvolutionGoal = EvolutionGoal.BEST_SHARPE,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<StrategyGenome> {

        var population = (0 until populationSize).map { randomDiceGenome() }

        repeat(generations) { gen ->
            // Evaluate
            population = population.map { genome ->
                val cfg = MCConfig(
                    bankroll            = bankroll,
                    baseBet             = bankroll * genome.baseBetPct / 100.0,
                    multiplier          = genome.multiplier,
                    winChancePct        = genome.winChancePct,
                    increaseOnLossPct   = genome.increaseOnLossPct,
                    maxBets             = genome.maxBets,
                    sessions            = sessionsPerEval
                )
                val result = MonteCarloEngine.simulate(cfg)
                genome.copy(score = scoreForGoal(result, goal), result = result)
            }

            // Sort by score descending
            population = population.sortedByDescending { it.score }

            onProgress?.invoke(gen + 1, generations)

            if (gen < generations - 1) {
                // Keep top 25%, breed and mutate rest
                val elite = population.take(populationSize / 4)
                val bred = (0 until populationSize * 3 / 4).map {
                    val parent = elite.random(rng)
                    mutateDiceGenome(parent)
                }
                population = elite + bred
            }
        }

        return population.sortedByDescending { it.score }
    }

    private fun scoreForGoal(result: MCResult, goal: EvolutionGoal): Double = when (goal) {
        EvolutionGoal.MAX_PROFIT  -> result.avgProfit
        EvolutionGoal.MIN_RISK    -> -result.bustRate + result.winRate * 0.5
        EvolutionGoal.BEST_SHARPE -> result.sharpeRatio
        EvolutionGoal.MAX_WIN_RATE -> result.winRate - result.bustRate * 0.3
    }

    private fun randomDiceGenome(): StrategyGenome {
        val mult = rng.nextDouble(1.05, 50.0)
        return StrategyGenome(
            multiplier          = mult,
            winChancePct        = ProbabilityMath.diceWinChance(mult),
            increaseOnLossPct   = rng.nextDouble(10.0, 200.0),
            baseBetPct          = rng.nextDouble(0.1, 2.0),
            maxBets             = rng.nextInt(10, 200)
        )
    }

    private fun mutateDiceGenome(parent: StrategyGenome): StrategyGenome {
        val multDelta = rng.nextDouble(-5.0, 5.0)
        val newMult   = (parent.multiplier + multDelta).coerceIn(1.05, 200.0)
        return StrategyGenome(
            multiplier          = newMult,
            winChancePct        = ProbabilityMath.diceWinChance(newMult),
            increaseOnLossPct   = (parent.increaseOnLossPct * rng.nextDouble(0.85, 1.15)).coerceIn(5.0, 300.0),
            baseBetPct          = (parent.baseBetPct * rng.nextDouble(0.85, 1.15)).coerceIn(0.05, 5.0),
            maxBets             = (parent.maxBets + rng.nextInt(-20, 20)).coerceIn(5, 500)
        )
    }

    // ── Keno Evolution ────────────────────────────────────────────────────────

    fun evolveKeno(
        bankroll: Double,
        generations: Int = 15,
        populationSize: Int = 30,
        sessionsPerEval: Int = 300,
        goal: EvolutionGoal = EvolutionGoal.BEST_SHARPE,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<KenoGenome> {
        var population = (0 until populationSize).map { randomKenoGenome(bankroll) }

        repeat(generations) { gen ->
            population = population.map { genome ->
                val cfg = KenoMCConfig(
                    bankroll           = bankroll,
                    picks              = genome.picks,
                    minHits            = genome.minHits,
                    baseBet            = genome.baseBet,
                    increaseOnLossPct  = genome.increaseOnLossPct,
                    sessions           = sessionsPerEval
                )
                val result = KenoMonteCarloEngine.simulate(cfg)
                val score = when (goal) {
                    EvolutionGoal.MAX_PROFIT   -> result.avgProfit
                    EvolutionGoal.MIN_RISK     -> -result.bustRate
                    EvolutionGoal.MAX_WIN_RATE -> result.winRate
                    EvolutionGoal.BEST_SHARPE  -> result.avgProfit * (1.0 - result.bustRate / 100.0)
                }
                genome.copy(score = score, result = result)
            }

            population = population.sortedByDescending { it.score }
            onProgress?.invoke(gen + 1, generations)

            if (gen < generations - 1) {
                val elite = population.take(populationSize / 4)
                val bred = (0 until populationSize * 3 / 4).map {
                    mutateKenoGenome(elite.random(rng), bankroll)
                }
                population = elite + bred
            }
        }

        return population.sortedByDescending { it.score }
    }

    private fun randomKenoGenome(bankroll: Double): KenoGenome {
        val picks = rng.nextInt(1, 11)
        val hits  = rng.nextInt(1, picks + 1)
        return KenoGenome(
            picks              = picks,
            minHits            = hits,
            baseBet            = (bankroll * rng.nextDouble(0.005, 0.03)).coerceAtLeast(0.01),
            increaseOnLossPct  = rng.nextDouble(10.0, 100.0)
        )
    }

    private fun mutateKenoGenome(parent: KenoGenome, bankroll: Double): KenoGenome {
        val newPicks = (parent.picks + rng.nextInt(-1, 2)).coerceIn(1, 10)
        val newHits  = (parent.minHits + rng.nextInt(-1, 2)).coerceIn(1, newPicks)
        return KenoGenome(
            picks              = newPicks,
            minHits            = newHits,
            baseBet            = (parent.baseBet * rng.nextDouble(0.85, 1.15)).coerceIn(0.01, bankroll * 0.05),
            increaseOnLossPct  = (parent.increaseOnLossPct * rng.nextDouble(0.85, 1.15)).coerceIn(5.0, 150.0)
        )
    }

    // ── Mines Evolution ───────────────────────────────────────────────────────

    fun evolveMines(
        bankroll: Double,
        generations: Int = 15,
        populationSize: Int = 30,
        sessionsPerEval: Int = 500,
        goal: EvolutionGoal = EvolutionGoal.BEST_SHARPE,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<MinesGenome> {
        var population = (0 until populationSize).map { randomMinesGenome(bankroll) }

        repeat(generations) { gen ->
            population = population.map { genome ->
                val result = simulateMines(genome, bankroll, sessionsPerEval)
                val score = when (goal) {
                    EvolutionGoal.MAX_PROFIT   -> result.first
                    EvolutionGoal.MIN_RISK     -> -result.third
                    EvolutionGoal.MAX_WIN_RATE -> result.second
                    EvolutionGoal.BEST_SHARPE  -> result.first * (1.0 - result.third / 100.0)
                }
                genome.copy(score = score, avgProfit = result.first, winRate = result.second, bustRate = result.third)
            }
            population = population.sortedByDescending { it.score }
            onProgress?.invoke(gen + 1, generations)

            if (gen < generations - 1) {
                val elite = population.take(populationSize / 4)
                val bred = (0 until populationSize * 3 / 4).map {
                    mutateMinesGenome(elite.random(rng), bankroll)
                }
                population = elite + bred
            }
        }
        return population.sortedByDescending { it.score }
    }

    private fun simulateMines(genome: MinesGenome, bankroll: Double, sessions: Int): Triple<Double, Double, Double> {
        val rng2 = Random(System.nanoTime())
        val survivalProb = ProbabilityMath.minesSurvivalProbability(genome.mines, genome.picks)
        val multiplier = ProbabilityMath.minesMultiplier(genome.mines, genome.picks)
        val profits = mutableListOf<Double>()
        var busts = 0

        repeat(sessions) {
            var balance = bankroll
            var bet = genome.baseBet
            for (round in 0 until 50) {
                if (bet > balance || bet <= 0) { busts++; break }
                balance -= bet
                if (rng2.nextDouble() < survivalProb) {
                    balance += bet * multiplier
                    bet = genome.baseBet
                } else {
                    bet *= (1.0 + genome.increaseOnLossPct / 100.0)
                }
            }
            profits.add(balance - bankroll)
        }
        val n = profits.size
        return Triple(
            profits.average(),
            profits.count { it > 0 }.toDouble() / n * 100.0,
            busts.toDouble() / n * 100.0
        )
    }

    private fun randomMinesGenome(bankroll: Double): MinesGenome {
        val mines = rng.nextInt(1, 20)
        val picks = rng.nextInt(1, 25 - mines)
        return MinesGenome(
            mines = mines, picks = picks,
            baseBet = bankroll * rng.nextDouble(0.005, 0.03),
            increaseOnLossPct = rng.nextDouble(20.0, 150.0)
        )
    }

    private fun mutateMinesGenome(parent: MinesGenome, bankroll: Double): MinesGenome {
        val newMines = (parent.mines + rng.nextInt(-2, 3)).coerceIn(1, 22)
        val newPicks = (parent.picks + rng.nextInt(-1, 2)).coerceIn(1, 24 - newMines)
        return MinesGenome(
            mines = newMines, picks = newPicks,
            baseBet = (parent.baseBet * rng.nextDouble(0.8, 1.2)).coerceIn(0.01, bankroll * 0.05),
            increaseOnLossPct = (parent.increaseOnLossPct * rng.nextDouble(0.85, 1.15)).coerceIn(5.0, 200.0)
        )
    }
}
