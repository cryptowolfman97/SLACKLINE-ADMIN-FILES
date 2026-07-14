package com.shvertex.casinotoolspro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shvertex.casinotoolspro.ui.dice.*
import com.shvertex.casinotoolspro.ui.evolution.*
import com.shvertex.casinotoolspro.ui.home.HomeScreen
import com.shvertex.casinotoolspro.ui.keno.KenoMCScreen
import com.shvertex.casinotoolspro.ui.mines.MinesAnalyticsScreen
import com.shvertex.casinotoolspro.ui.montecarlo.MonteCarloScreen
import com.shvertex.casinotoolspro.ui.sports.*
import com.shvertex.casinotoolspro.ui.strategies.StrategiesScreen
import com.shvertex.casinotoolspro.ui.utilities.*

@Composable
fun AppNavigation(navController: NavHostController) {

    fun back()              = navController.popBackStack()
    fun go(route: String)   = navController.navigate(route)

    NavHost(navController = navController, startDestination = Routes.HOME) {

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { go(it) })
        }

        // ── Core Tools ────────────────────────────────────────────────────────
        composable(Routes.STRATEGIES) {
            StrategiesScreen(onBack = { back() }, onNavigate = { go(it) })
        }
        composable(Routes.DICE_SIM) {
            DiceSimScreen(onBack = { back() })
        }
        composable(Routes.DICE_CALC) {
            DiceCalcScreen(onBack = { back() })
        }
        composable(Routes.MONTE_CARLO) {
            MonteCarloScreen(onBack = { back() })
        }
        composable(Routes.DICE_OPTIMIZER) {
            DiceOptimizerScreen(onBack = { back() })
        }
        composable(Routes.DICE_GENERATOR) {
            DiceGeneratorScreen(onBack = { back() })
        }

        // ── Evolution Lab ─────────────────────────────────────────────────────
        composable(Routes.STRATEGY_FORGE) {
            StrategyForgeScreen(onBack = { back() })
        }
        composable(Routes.DICE_EVOLUTION) {
            DiceEvolutionScreen(onBack = { back() }, isLimbo = false)
        }
        composable(Routes.LIMBO_EVOLUTION) {
            DiceEvolutionScreen(onBack = { back() }, isLimbo = true)
        }
        composable(Routes.KENO_EVOLUTION) {
            KenoEvolutionScreen(onBack = { back() })
        }
        composable(Routes.MINES_EVOLUTION) {
            MinesEvolutionScreen(onBack = { back() })
        }

        // ── Research Lab ──────────────────────────────────────────────────────
        composable(Routes.STRESS_TEST) {
            StressTestScreen(onBack = { back() })
        }
        composable(Routes.BANKROLL_LAB) {
            BankrollLabScreen(onBack = { back() })
        }

        // ── Game Analytics ────────────────────────────────────────────────────
        composable(Routes.KENO_MC) {
            KenoMCScreen(onBack = { back() })
        }
        composable(Routes.MINES_ANALYTICS) {
            MinesAnalyticsScreen(onBack = { back() })
        }
        composable(Routes.BLACKJACK) {
            // Now lives in utilities package
            BlackjackScreen(onBack = { back() })
        }

        // ── Sports ────────────────────────────────────────────────────────────
        composable(Routes.SPORTS_LAB) {
            SportsLabScreen(onBack = { back() }, onNavigate = { go(it) })
        }
        composable(Routes.KELLY_CALC) {
            KellyScreen(onBack = { back() })
        }
        composable(Routes.PARLAY) {
            ParlayScreen(onBack = { back() })
        }
        composable(Routes.VALUE_BET) {
            ValueBetScreen(onBack = { back() })
        }
        composable(Routes.ARBITRAGE) {
            ArbitrageScreen(onBack = { back() })
        }

        // ── Utilities ─────────────────────────────────────────────────────────
        composable(Routes.COMPOUND) {
            CompoundGrowthScreen(onBack = { back() })
        }
        composable(Routes.PATTERN) {
            PatternMasterScreen(onBack = { back() })
        }
        composable(Routes.CONVERTER) {
            CryptoConverterScreen(onBack = { back() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { back() })
        }
    }
}
