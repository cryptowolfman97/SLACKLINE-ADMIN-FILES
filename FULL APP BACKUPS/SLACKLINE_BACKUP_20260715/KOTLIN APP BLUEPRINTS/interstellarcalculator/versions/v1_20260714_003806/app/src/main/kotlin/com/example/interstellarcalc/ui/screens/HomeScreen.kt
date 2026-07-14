package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.interstellarcalc.ui.Screen
import com.example.interstellarcalc.ui.components.AnimatedSpaceBackground

private data class CalcTile(
    val title    : String,
    val subtitle : String,
    val icon     : ImageVector,
    val route    : String,
    val isHeader : Boolean = false
)

private val TILES = listOf(
    // ── Catalogues ────────────────────────────────────────────────────────────
    CalcTile("Star Catalogue",        "Named stars",           Icons.Default.Star,                Screen.StarCatalogue.route),
    CalcTile("Black Hole Catalogue",  "Black holes",           Icons.Default.BlurCircular,        Screen.BlackHole.route),
    CalcTile("Deep Sky Catalogue",    "331 objects offline",   Icons.Default.TravelExplore,       Screen.DeepSky.route),
    // ── Original calculators ──────────────────────────────────────────────────
    CalcTile("Relativistic Rocket",   "Time dilation & fuel",  Icons.Default.RocketLaunch,        Screen.Relativistic.route),
    CalcTile("Orbital Mechanics",     "Velocity & period",     Icons.Default.Public,              Screen.Orbital.route),
    CalcTile("Tsiolkovsky Δv",        "Rocket equation",       Icons.Default.LocalFireDepartment, Screen.Tsiolkovsky.route),
    CalcTile("Hohmann Transfer",      "Orbital maneuver",      Icons.Default.SwapVert,            Screen.Hohmann.route),
    CalcTile("Gravity Dilation",      "Gravitational redshift",Icons.Default.Compress,            Screen.Gravity.route),
    // ── New calculators ───────────────────────────────────────────────────────
    CalcTile("Escape Velocity",       "Any body, any altitude",Icons.Default.FlightTakeoff,       Screen.EscapeVelocity.route),
    CalcTile("Velocity Dilation",     "Special relativity",    Icons.Default.Speed,               Screen.VelocityDilation.route),
    CalcTile("Planet Weight",         "Your weight anywhere",  Icons.Default.FitnessCenter,       Screen.PlanetWeight.route),
    CalcTile("Schwarzschild Radius",  "Event horizon size",    Icons.Default.Circle,              Screen.Schwarzschild.route),
    CalcTile("Light Travel Time",     "Cosmic distances",      Icons.Default.WbSunny,             Screen.LightTravel.route),
    CalcTile("Stellar Lifetime",      "How long stars live",   Icons.Default.AutoAwesome,         Screen.StellarLifetime.route),
    CalcTile("Redshift",              "Cosmological motion",   Icons.Default.Waves,               Screen.Redshift.route),
    // ── Settings ──────────────────────────────────────────────────────────────
    CalcTile("Settings",              "Themes & preferences",  Icons.Default.Settings,            Screen.Settings.route),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Renamed for clarity since it now handles both initial load and the hide/reveal feature
    var showModules by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showModules = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background always rendered — never clipped or obscured
        AnimatedSpaceBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Interstellar Calc+",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "by SHV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // The press-and-hold reveal button
                        Icon(
                            imageVector = if (showModules) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Hold to reveal background",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(12.dp) // Centers the 24dp icon inside a 48dp touch target
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            showModules = false // Hide modules on press down
                                            tryAwaitRelease()   // Wait until the user lifts their finger
                                            showModules = true  // Bring modules back
                                        }
                                    )
                                }
                        )

                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            AnimatedVisibility(
                visible = showModules,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 },
                // Added a matching exit animation for when the user holds the button
                exit    = fadeOut(tween(400)) + slideOutVertically(tween(400)) { it / 4 }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start  = 14.dp,
                        end    = 14.dp,
                        top    = padding.calculateTopPadding() + 4.dp,
                        bottom = 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    items(TILES) { tile ->
                        Card(
                            onClick   = { navController.navigate(tile.route) },
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier  = Modifier.fillMaxWidth().height(92.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    tile.icon,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = tile.title,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = Color.White,
                                        lineHeight = 16.sp,
                                        maxLines   = 2
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text      = tile.subtitle,
                                        fontSize  = 10.sp,
                                        color     = Color.White.copy(alpha = 0.60f),
                                        maxLines  = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
