package com.example.interstellarcalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.data.*
import com.example.interstellarcalc.ui.components.*

// ─────────────────────────────────────────────────────────────────────────────
// List screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackHoleCatalogueScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) BLACKHOLE_CATALOGUE
        else BLACKHOLE_CATALOGUE.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true) ||
            bhTypeLabel(it.blackHoleType).contains(query, ignoreCase = true) ||
            it.discoveryMethod.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = { CalcTopBar("Black Hole Catalogue", navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Search by name, location, type…") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon  = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(14.dp),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f)
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text  = "${filtered.size} black hole${if (filtered.size != 1) "s" else ""} found",
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
                    items(filtered, key = { it.id }) { bh ->
                        BlackHoleListCard(bh) {
                            navController.navigate("bh_detail/${bh.id}")
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BlackHoleListCard(bh: BlackHoleEntry, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                BlackHoleIllustration(bh.blackHoleType, size = 56.dp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = bh.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text  = bh.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CatalogueChip(bhTypeLabel(bh.blackHoleType), MaterialTheme.colorScheme.primary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = formatMassSolar(bh.massSolar),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    text  = "M☉",
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
fun BlackHoleDetailScreen(navController: NavController, bhId: String) {
    val bh = remember(bhId) { BLACKHOLE_CATALOGUE.find { it.id == bhId } }

    if (bh == null) {
        Scaffold(topBar = { CalcTopBar("Black Hole Detail", navController) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("Black hole not found.")
            }
        }
        return
    }

    Scaffold(topBar = { CalcTopBar(bh.name, navController) }) { padding ->
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
                    BlackHoleIllustration(bh.blackHoleType, size = 180.dp)
                }
            }

            // Name + chips
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(bh.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(bh.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogueChip(bhTypeLabel(bh.blackHoleType), MaterialTheme.colorScheme.primary)
                    CatalogueChip(bh.spinParameter + " Spin", MaterialTheme.colorScheme.secondary)
                }
            }

            // Physical properties
            SectionCard("Physical Properties") {
                StatRow("Mass",                    formatMassSolar(bh.massSolar) + " M☉")
                StatRow("Schwarzschild Radius",    formatSchwarzschildKm(bh.schwarzschildRadiusKm))
                StatRow("Distance from Earth",     formatDistance(bh.distanceLy))
                StatRow("Type",                    bhTypeLabel(bh.blackHoleType))
                StatRow("Spin",                    bh.spinParameter)
            }

            // Discovery
            SectionCard("Discovery") {
                StatRow("Year",   bh.discoveryYear.toString())
                StatRow("Method", bh.discoveryMethod)
            }

            // Fun fact
            SectionCard("Notable Fact") {
                Text(
                    text  = bh.funFact,
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

internal fun bhTypeLabel(t: BlackHoleType) = when (t) {
    BlackHoleType.STELLAR        -> "Stellar"
    BlackHoleType.INTERMEDIATE   -> "Intermediate"
    BlackHoleType.SUPERMASSIVE   -> "Supermassive"
}

internal fun formatMassSolar(m: Double) = when {
    m < 1_000.0          -> "%.2f".format(m)
    m < 1_000_000.0      -> "%,.0f".format(m)
    m < 1_000_000_000.0  -> "%.2fM".format(m / 1_000_000.0)
    else                 -> "%.2fB".format(m / 1_000_000_000.0)
}

internal fun formatSchwarzschildKm(km: Double) = when {
    km < 1_000.0         -> "%.1f km".format(km)
    km < 1_000_000.0     -> "%,.0f km".format(km)
    km < 1.496e8         -> "%.4f AU".format(km / 1.496e8)
    else                 -> "%.3f AU".format(km / 1.496e8)
}
