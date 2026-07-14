package com.example.interstellarcalc.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.interstellarcalc.data.DatabaseHelper
import com.example.interstellarcalc.data.DeepSkyObject
import com.example.interstellarcalc.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ── Category filter definitions ──────────────────────────────────────────────
private val CATEGORIES = listOf(
    "ALL" to "All",
    "STAR" to "Stars",
    "BLACK_HOLE" to "Black Holes",
    "GALAXY" to "Galaxies",
    "NEBULA" to "Nebulae",
    "GLOBULAR_CLUSTER" to "Globular Clusters",
    "OPEN_CLUSTER" to "Open Clusters",
    "PLANET" to "Planets",
    "MOON" to "Moons",
    "OTHER" to "Other"
)

// ── List screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSkyCatalogueScreen(navController: NavController) {
    val context     = LocalContext.current
    val db          = remember { DatabaseHelper.getInstance(context) }
    var query       by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf("ALL") }
    var results     by remember { mutableStateOf<List<DeepSkyObject>>(emptyList()) }
    var featured    by remember { mutableStateOf<List<DeepSkyObject>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    // Load featured objects on first open
    LaunchedEffect(Unit) {
        featured = withContext(Dispatchers.IO) { db.getFeatured(24) }
    }

    // Search with debounce
    LaunchedEffect(query, category) {
        if (query.isBlank()) {
            results     = emptyList()
            hasSearched = false
            isLoading   = false
            return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        results     = withContext(Dispatchers.IO) {
            db.search(query.trim(), if (category == "ALL") null else category)
        }
        hasSearched = true
        isLoading   = false
    }

    Scaffold(topBar = { CalcTopBar("Deep Sky Catalogue", navController) }) { padding ->
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
                placeholder   = { Text("Search stars, galaxies, nebulae, black holes…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = ""; results = emptyList(); hasSearched = false }) {
                            Icon(Icons.Default.Clear, null)
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

            Spacer(Modifier.height(10.dp))

            // Category chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORIES) { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick  = { category = key },
                        label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                            containerColor         = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor             = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                hasSearched && results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                hasSearched -> {
                    Text(
                        "${results.size} object${if (results.size != 1) "s" else ""} found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(results, key = { it.id }) { obj ->
                            DeepSkyCard(obj) { navController.navigate("deepsky_detail/${obj.id}") }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                else -> {
                    // Show featured objects when idle
                    Text(
                        "Featured Objects",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(featured, key = { it.id }) { obj ->
                            DeepSkyCard(obj) { navController.navigate("deepsky_detail/${obj.id}") }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeepSkyCard(obj: DeepSkyObject, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Illustration
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                DeepSkyIllustration(obj.category, obj.objectType, size = 56.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(obj.displayName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (obj.displayName != obj.name) {
                    Text(obj.name, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CatalogueChip(categoryLabel(obj.category), MaterialTheme.colorScheme.primary)
                    obj.constellation?.let { CatalogueChip(it, MaterialTheme.colorScheme.secondary) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                obj.magnitude?.let {
                    Text("%.1f".format(it), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("mag", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Detail screen ─────────────────────────────────────────────────────────────
@Composable
fun DeepSkyDetailScreen(navController: NavController, objectId: Int) {
    val context = LocalContext.current
    val db      = remember { DatabaseHelper.getInstance(context) }
    var obj     by remember { mutableStateOf<DeepSkyObject?>(null) }

    LaunchedEffect(objectId) {
        obj = withContext(Dispatchers.IO) { db.getById(objectId) }
    }

    val current = obj
    if (current == null) {
        Scaffold(topBar = { CalcTopBar("Loading…", navController) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    Scaffold(topBar = { CalcTopBar(current.displayName, navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero illustration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    DeepSkyIllustration(current.category, current.objectType, size = 180.dp)
                }
            }

            // Name + chips
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (current.displayName != current.name)
                    Text(current.name, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogueChip(categoryLabel(current.category), MaterialTheme.colorScheme.primary)
                    CatalogueChip(current.objectType, MaterialTheme.colorScheme.secondary)
                }
            }

            // Physical data
            SectionCard("Properties") {
                current.constellation?.let { StatRow("Constellation", it) }
                current.magnitude?.let    { StatRow("Magnitude", "%.2f".format(it)) }
                current.distanceLy?.let   { StatRow("Distance", formatDistanceLy(it)) }
                current.spectralType?.let { StatRow("Spectral Type", it) }
                current.tempKelvin?.let   { StatRow("Temperature", "%,d K".format(it)) }
                current.massSolar?.let    { StatRow("Mass", formatMass(it)) }
                current.radiusSolar?.let  { StatRow("Radius", "%.3f R☉".format(it)) }
                current.raDeg?.let        { StatRow("Right Ascension", "%.4f°".format(it)) }
                current.decDeg?.let       { StatRow("Declination", "%.4f°".format(it)) }
            }

            // Description
            current.description?.let {
                SectionCard("About") {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                }
            }

            // Fun fact
            current.funFact?.let {
                SectionCard("Did You Know?") {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
internal fun categoryLabel(cat: String) = when (cat) {
    "STAR"             -> "Star"
    "BLACK_HOLE"       -> "Black Hole"
    "GALAXY"           -> "Galaxy"
    "NEBULA"           -> "Nebula"
    "GLOBULAR_CLUSTER" -> "Globular Cluster"
    "OPEN_CLUSTER"     -> "Open Cluster"
    "PLANET"           -> "Planet"
    "MOON"             -> "Moon"
    else               -> "Other"
}

internal fun formatDistanceLy(ly: Double) = when {
    ly <= 0.0          -> "In our solar system"
    ly < 0.01          -> "%.5f ly".format(ly)
    ly < 10.0          -> "%.2f ly".format(ly)
    ly < 1_000.0       -> "%.1f ly".format(ly)
    ly < 1_000_000.0   -> "%,.0f ly".format(ly)
    ly < 1_000_000_000.0 -> "%.2f Mly".format(ly / 1_000_000.0)
    else               -> "%.2f Bly".format(ly / 1_000_000_000.0)
}

internal fun formatMass(m: Double) = when {
    m < 0.001          -> "%.5f M☉".format(m)
    m < 1_000.0        -> "%.3f M☉".format(m)
    m < 1_000_000.0    -> "%,.0f M☉".format(m)
    m < 1_000_000_000.0 -> "%.2f M M☉".format(m / 1_000_000.0)
    else               -> "%.2f B M☉".format(m / 1_000_000_000.0)
}
