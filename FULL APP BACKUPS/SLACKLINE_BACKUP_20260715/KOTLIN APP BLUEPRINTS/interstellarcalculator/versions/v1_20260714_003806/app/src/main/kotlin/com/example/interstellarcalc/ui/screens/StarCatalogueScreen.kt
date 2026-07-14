package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.data.*
import com.example.interstellarcalc.ui.components.*

// ─────────────────────────────────────────────────────────────────────────────
// List screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarCatalogueScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) STAR_CATALOGUE
        else STAR_CATALOGUE.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.constellation.contains(query, ignoreCase = true) ||
            it.spectralType.contains(query, ignoreCase = true) ||
            starTypeLabel(it.starType).contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = { CalcTopBar("Star Catalogue", navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Search by name, constellation, type…") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon  = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f)
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text  = "${filtered.size} star${if (filtered.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { star ->
                        StarListCard(star) {
                            navController.navigate("star_detail/${star.id}")
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StarListCard(star: StarEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini illustration
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                StarIllustration(star.starType, size = 56.dp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = star.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = star.constellation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CatalogueChip(starTypeLabel(star.starType), MaterialTheme.colorScheme.primary)
                    CatalogueChip(star.spectralType, MaterialTheme.colorScheme.secondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = formatDistance(star.distanceLy),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text  = "from Earth",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detail screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StarDetailScreen(navController: NavController, starId: String) {
    val star = remember(starId) { STAR_CATALOGUE.find { it.id == starId } }

    if (star == null) {
        Scaffold(topBar = { CalcTopBar("Star Detail", navController) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("Star not found.")
            }
        }
        return
    }

    Scaffold(topBar = { CalcTopBar(star.name, navController) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Illustration hero
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StarIllustration(star.starType, size = 180.dp)
                }
            }

            // Name + type chips
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(star.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(star.constellation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogueChip(starTypeLabel(star.starType), MaterialTheme.colorScheme.primary)
                    CatalogueChip("Class ${star.spectralType}", MaterialTheme.colorScheme.secondary)
                }
            }

            // Stats grid
            SectionCard("Physical Properties") {
                StatRow("Distance",          formatDistance(star.distanceLy))
                StatRow("Mass",              "%.3f M☉".format(star.massSolar))
                StatRow("Radius",            "%.3f R☉".format(star.radiusSolar))
                StatRow("Luminosity",        formatLuminosity(star.luminositySolar))
                StatRow("Surface Temp",      "${"%,d".format(star.tempKelvin)} K")
                StatRow("Apparent Mag",      "%.2f".format(star.apparentMag))
                StatRow("Absolute Mag",      "%.2f".format(star.absoluteMag))
            }

            // Fun fact
            SectionCard("Notable Fact") {
                Text(
                    text  = star.funFact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

internal fun starTypeLabel(t: com.example.interstellarcalc.data.StarType) = when (t) {
    com.example.interstellarcalc.data.StarType.BLUE_SUPERGIANT -> "Blue Supergiant"
    com.example.interstellarcalc.data.StarType.YELLOW_DWARF    -> "Yellow Dwarf"
    com.example.interstellarcalc.data.StarType.RED_GIANT       -> "Red Giant"
    com.example.interstellarcalc.data.StarType.RED_DWARF       -> "Red Dwarf"
    com.example.interstellarcalc.data.StarType.WHITE_DWARF     -> "White Dwarf"
    com.example.interstellarcalc.data.StarType.NEUTRON_STAR    -> "Neutron Star"
}

internal fun formatDistance(ly: Double) = when {
    ly == 0.0       -> "0 ly (our Sun)"
    ly < 10.0       -> "%.2f ly".format(ly)
    ly < 1000.0     -> "%.1f ly".format(ly)
    ly < 1_000_000.0-> "%,.0f ly".format(ly)
    else            -> "%.2f Mly".format(ly / 1_000_000.0)
}

internal fun formatLuminosity(l: Double) = when {
    l < 0.001  -> "%.5f L☉".format(l)
    l < 1.0    -> "%.4f L☉".format(l)
    l < 1000.0 -> "%.2f L☉".format(l)
    else       -> "%,.0f L☉".format(l)
}

@Composable
internal fun CatalogueChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
}
