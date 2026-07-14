package com.shvertex.universalconv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.universalconv.navigation.*
import com.shvertex.universalconv.ui.components.*
import com.shvertex.universalconv.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recents: List<String>,
    onNavigate: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back press — only fires when this screen is on top
    BackHandler { showExitDialog = true }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor    = Surface2,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            title = {
                Text("Exit App", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to exit Universal Calculator?")
            },
            confirmButton = {
                TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                    Text("EXIT", color = Rose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CANCEL", color = Teal)
                }
            },
        )
    }

    val searchResults = remember(query) {
        if (query.isBlank()) emptyList()
        else ALL_MODULES.filter { m ->
            query.lowercase().let { q ->
                m.title.lowercase().contains(q) ||
                m.keywords.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Universal Calculator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Teal)
                        Text("by SH Vertex  •  45 modules", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black),
            )
        },
        containerColor = Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Search bar ──
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search 45 modules…", color = TextMuted, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    focusedBorderColor      = Teal,
                    unfocusedBorderColor    = Border,
                    cursorColor             = Teal,
                    focusedContainerColor   = Surface2,
                    unfocusedContainerColor = Surface1,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            if (query.isNotEmpty()) {
                // ── Search results ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (searchResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No results for \"$query\"", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        searchResults.forEach { m ->
                            SearchResultRow(m, onClick = { onNavigate(m.id) })
                        }
                    }
                    Spacer(Modifier.height(60.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp),
                ) {
                    // ── Recents ──
                    if (recents.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader("Recently Used")
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            recents.takeLast(6).reversed().forEach { id ->
                                val mod = ALL_MODULES.find { it.id == id }
                                if (mod != null) {
                                    RecentChip(mod, onClick = { onNavigate(mod.id) })
                                }
                            }
                        }
                    }

                    // ── Categories ──
                    ConverterCategory.values().forEach { cat ->
                        val modules = ALL_MODULES.filter { it.category == cat }
                        Spacer(Modifier.height(12.dp))
                        SectionHeader(cat.label, accent = cat.accent)
                        Spacer(Modifier.height(6.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 2000.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp),
                            userScrollEnabled = false,
                        ) {
                            items(modules) { m ->
                                ModuleTile(
                                    emoji    = m.icon,
                                    title    = m.title,
                                    subtitle = m.subtitle,
                                    accent   = m.accent,
                                    onClick  = { onNavigate(m.id) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentChip(mod: ConverterModule, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(mod.accent.copy(alpha = 0.12f))
            .border(1.dp, mod.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(mod.icon, fontSize = 13.sp)
        Text(mod.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = mod.accent)
    }
}

@Composable
private fun SearchResultRow(mod: ConverterModule, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(mod.accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(mod.icon, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(mod.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(mod.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = mod.accent, modifier = Modifier.size(20.dp))
    }
}