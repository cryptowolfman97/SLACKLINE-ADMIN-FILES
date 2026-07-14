package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

// Accent teal used across this screen
private val TEAL = Color(0xFF00E5CC)

@Composable
fun AdminAppsScreen(onEdit: (String) -> Unit, onAdd: () -> Unit) {
    var apps       by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var sortMode   by remember { mutableStateOf("newest") } // "newest" | "az" | "za" | "custom"
    val scope      = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf<String?>(null) }

    // Custom-order working copy — a mutable list the user can rearrange before saving
    var customOrder by remember { mutableStateOf<List<JsonObject>>(emptyList()) }

    // Snackbar feedback for Save Order result
    val snackbarHostState = remember { SnackbarHostState() }

    // Saving progress flag
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { apps = StoreSupabaseApi.getApps() }

    // Keep customOrder in sync whenever the raw apps list refreshes,
    // but only if we haven't entered Custom mode yet (avoids overwriting work in progress).
    LaunchedEffect(apps) {
        if (sortMode != "custom") {
            customOrder = apps.sortedBy { it["sort_order"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE }
        }
    }

    // Derive displayed list based on active sort mode
    val displayedApps = remember(apps, sortMode, customOrder) {
        when (sortMode) {
            "az"     -> apps.sortedBy { it["name"]?.jsonPrimitive?.content?.lowercase() ?: "" }
            "za"     -> apps.sortedByDescending { it["name"]?.jsonPrimitive?.content?.lowercase() ?: "" }
            "custom" -> customOrder
            else     -> apps // newest — default API order (created_at.desc)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = TEAL) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Sort chips row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(selected = sortMode == "newest", onClick = { sortMode = "newest" }, label = "Newest")
                SortChip(selected = sortMode == "az",     onClick = { sortMode = "az"     }, label = "A‑Z")
                SortChip(selected = sortMode == "za",     onClick = { sortMode = "za"     }, label = "Z‑A")
                SortChip(
                    selected = sortMode == "custom",
                    onClick  = {
                        // Entering Custom mode: seed list from current sort_order values
                        if (sortMode != "custom") {
                            customOrder = apps.sortedBy {
                                it["sort_order"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
                            }
                        }
                        sortMode = "custom"
                    },
                    label    = "Custom"
                )
            }

            // ── Custom mode: Save Order button + hint ─────────────────────
            AnimatedVisibility(visible = sortMode == "custom", enter = fadeIn(), exit = fadeOut()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp)) {
                    Text(
                        "Use ▲ ▼ to reorder, then tap Save Order.",
                        fontSize = 11.sp,
                        color    = Color(0xFF8A929C)
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                // Write position index (1-based) into sort_order for every app
                                val results = customOrder.mapIndexed { index, app ->
                                    val id = app["id"]?.jsonPrimitive?.content ?: return@mapIndexed false
                                    StoreSupabaseApi.updateApp(id, mapOf("sort_order" to (index + 1)))
                                }
                                isSaving = false
                                if (results.all { it }) {
                                    snackbarHostState.showSnackbar("✓ Order saved successfully")
                                    // Refresh so the in-memory list matches the DB
                                    apps = StoreSupabaseApi.getApps()
                                } else {
                                    snackbarHostState.showSnackbar("⚠ Some updates failed — try again")
                                }
                            }
                        },
                        enabled  = !isSaving,
                        colors   = ButtonDefaults.buttonColors(containerColor = TEAL),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color    = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isSaving) "Saving…" else "Save Order",
                            color      = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── App list ──────────────────────────────────────────────────
            LazyColumn(
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedApps, key = { it["id"]?.jsonPrimitive?.content ?: "" }) { app ->
                    val id          = app["id"]?.jsonPrimitive?.content ?: return@items
                    val isPublished = app["is_published"]?.jsonPrimitive?.boolean ?: false
                    val currentIdx  = customOrder.indexOf(app)

                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {

                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment   = Alignment.CenterVertically
                            ) {
                                // ── Position badge (Custom mode only) ─────
                                if (sortMode == "custom" && currentIdx >= 0) {
                                    Text(
                                        "#${currentIdx + 1}",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = TEAL,
                                        modifier   = Modifier.padding(end = 8.dp)
                                    )
                                }

                                Text(
                                    app["name"]?.jsonPrimitive?.content ?: "",
                                    color      = Color(0xFFF2F4F6),
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.weight(1f)
                                )

                                // ── Up / Down arrows (Custom mode only) ───
                                if (sortMode == "custom") {
                                    Row {
                                        IconButton(
                                            onClick  = {
                                                if (currentIdx > 0) {
                                                    val mutable = customOrder.toMutableList()
                                                    mutable.add(currentIdx - 1, mutable.removeAt(currentIdx))
                                                    customOrder = mutable
                                                }
                                            },
                                            enabled  = currentIdx > 0,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move up",
                                                tint = if (currentIdx > 0) TEAL else Color(0xFF2A2A2A)
                                            )
                                        }
                                        IconButton(
                                            onClick  = {
                                                if (currentIdx < customOrder.lastIndex) {
                                                    val mutable = customOrder.toMutableList()
                                                    mutable.add(currentIdx + 1, mutable.removeAt(currentIdx))
                                                    customOrder = mutable
                                                }
                                            },
                                            enabled  = currentIdx < customOrder.lastIndex,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move down",
                                                tint = if (currentIdx < customOrder.lastIndex) TEAL else Color(0xFF2A2A2A)
                                            )
                                        }
                                    }
                                } else {
                                    // Normal mode: publish toggle
                                    Switch(
                                        checked         = isPublished,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                StoreSupabaseApi.updateApp(id, mapOf("is_published" to checked))
                                                apps = StoreSupabaseApi.getApps()
                                            }
                                        }
                                    )
                                }
                            }

                            // Show publish toggle below in Custom mode so it's still accessible
                            if (sortMode == "custom") {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        if (isPublished) "Published" else "Unpublished",
                                        fontSize = 11.sp,
                                        color    = if (isPublished) Color(0xFF4CAF50) else Color(0xFF8A929C)
                                    )
                                    Switch(
                                        checked         = isPublished,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                StoreSupabaseApi.updateApp(id, mapOf("is_published" to checked))
                                                apps = StoreSupabaseApi.getApps()
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "v${app["version"]?.jsonPrimitive?.content ?: ""}",
                                color = Color(0xFF8A929C),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onEdit(id) }) { Text("Edit",   color = TEAL) }
                                TextButton(onClick = { showDelete = id }) { Text("Delete", color = Color(0xFFFF4D6A)) }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDelete != null) {
        AlertDialog(
            onDismissRequest = { showDelete = null },
            title            = { Text("Delete App") },
            text             = { Text("Are you sure?") },
            confirmButton    = {
                TextButton(onClick = {
                    scope.launch {
                        StoreSupabaseApi.deleteApp(showDelete!!)
                        apps      = StoreSupabaseApi.getApps()
                        // Rebuild customOrder after deletion
                        customOrder = apps.sortedBy {
                            it["sort_order"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
                        }
                        showDelete = null
                    }
                }) { Text("Delete") }
            },
            dismissButton    = { TextButton(onClick = { showDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SortChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TEAL,
            selectedLabelColor     = Color(0xFF000000),
            containerColor         = Color(0xFF111111),
            labelColor             = Color(0xFF8A929C)
        )
    )
}
