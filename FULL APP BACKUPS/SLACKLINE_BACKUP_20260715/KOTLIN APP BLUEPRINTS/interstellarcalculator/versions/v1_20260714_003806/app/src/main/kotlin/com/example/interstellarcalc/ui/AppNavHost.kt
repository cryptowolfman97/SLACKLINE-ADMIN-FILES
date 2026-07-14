package com.example.interstellarcalc.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.interstellarcalc.ui.screens.*
import com.example.interstellarcalc.ui.theme.AppTheme

sealed class Screen(val route: String) {
    object Home              : Screen("home")
    object StarCatalogue     : Screen("star_catalogue")
    object BlackHole         : Screen("bh_catalogue")
    object DeepSky           : Screen("deepsky")
    object Relativistic      : Screen("relativistic")
    object Orbital           : Screen("orbital")
    object Tsiolkovsky       : Screen("tsiolkovsky")
    object Hohmann           : Screen("hohmann")
    object Gravity           : Screen("gravity")
    object EscapeVelocity    : Screen("escape_velocity")
    object VelocityDilation  : Screen("velocity_dilation")
    object PlanetWeight      : Screen("planet_weight")
    object Schwarzschild     : Screen("schwarzschild")
    object LightTravel       : Screen("light_travel")
    object StellarLifetime   : Screen("stellar_lifetime")
    object Redshift          : Screen("redshift")
    object Settings          : Screen("settings")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    currentTheme : AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    NavHost(
        navController       = navController,
        startDestination    = Screen.Home.route,
        enterTransition     = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition      = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
        popEnterTransition  = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
        popExitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        composable(Screen.Home.route)             { HomeScreen(navController) }
        composable(Screen.StarCatalogue.route)    { StarCatalogueScreen(navController) }
        composable(Screen.BlackHole.route)        { BlackHoleCatalogueScreen(navController) }
        composable(Screen.DeepSky.route)          { DeepSkyCatalogueScreen(navController) }
        composable(Screen.Relativistic.route)     { RelativisticRocketScreen(navController) }
        composable(Screen.Orbital.route)          { OrbitalMechanicsScreen(navController) }
        composable(Screen.Tsiolkovsky.route)      { TsiolkovskyScreen(navController) }
        composable(Screen.Hohmann.route)          { HohmannScreen(navController) }
        composable(Screen.Gravity.route)          { GravityTimeDilationScreen(navController) }
        composable(Screen.EscapeVelocity.route)   { EscapeVelocityScreen(navController) }
        composable(Screen.VelocityDilation.route) { VelocityDilationScreen(navController) }
        composable(Screen.PlanetWeight.route)     { PlanetWeightScreen(navController) }
        composable(Screen.Schwarzschild.route)    { SchwarzschildScreen(navController) }
        composable(Screen.LightTravel.route)      { LightTravelScreen(navController) }
        composable(Screen.StellarLifetime.route)  { StellarLifetimeScreen(navController) }
        composable(Screen.Redshift.route)         { RedshiftScreen(navController) }
        composable(Screen.Settings.route)         { SettingsScreen(navController, currentTheme, onThemeChange) }

        composable("star_detail/{starId}") { back ->
            val id = back.arguments?.getString("starId") ?: return@composable
            StarDetailScreen(navController, id)
        }
        composable("bh_detail/{bhId}") { back ->
            val id = back.arguments?.getString("bhId") ?: return@composable
            BlackHoleDetailScreen(navController, id)
        }
        composable("deepsky_detail/{objectId}") { back ->
            val id = back.arguments?.getString("objectId")?.toIntOrNull() ?: return@composable
            DeepSkyDetailScreen(navController, id)
        }
    }
}
