@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AdminDownloadLinksScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var downloads by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf<JsonObject?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        downloads = StoreSupabaseApi.getSiteDownloads()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Downloads", color = Color(0xFFF2F4F6)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF00E5CC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF00E5CC)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.padding(top = padding.calculateTopPadding())
        ) {
            items(downloads) { item ->
                val id = item["id"]?.jsonPrimitive?.content ?: ""
                val code = item["code"]?.jsonPrimitive?.content ?: ""
                val url = item["url"]?.jsonPrimitive?.content ?: ""
                val label = item["label"]?.jsonPrimitive?.content ?: ""
                val version = item["version"]?.jsonPrimitive?.content ?: ""

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = code,
                            color = Color(0xFFF2F4F6),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$label${if (version.isNotEmpty()) " · v$version" else ""}",
                            color = Color(0xFF8A929C),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = url,
                            color = Color(0xFF8A929C),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showEditDialog = item }) {
                                Text("Edit", color = Color(0xFF00E5CC))
                            }
                            TextButton(onClick = { showDeleteDialog = id }) {
                                Text("Delete", color = Color(0xFFFF4D6A))
                            }
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                item {
                    Text(
                        "No download links defined.",
                        color = Color(0xFF8A929C),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Edit dialog
    showEditDialog?.let { item ->
        var editCode by remember { mutableStateOf(item["code"]?.jsonPrimitive?.content ?: "") }
        var editUrl by remember { mutableStateOf(item["url"]?.jsonPrimitive?.content ?: "") }
        var editLabel by remember { mutableStateOf(item["label"]?.jsonPrimitive?.content ?: "") }
        var editVersion by remember { mutableStateOf(item["version"]?.jsonPrimitive?.content ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Download Link", color = Color(0xFFF2F4F6)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editCode, onValueChange = { editCode = it },
                        label = { Text("Code (e.g., store_apk)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editUrl, onValueChange = { editUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editLabel, onValueChange = { editLabel = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editVersion, onValueChange = { editVersion = it },
                        label = { Text("Version (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        StoreSupabaseApi.updateSiteDownload(
                            item["id"]?.jsonPrimitive?.content ?: "",
                            mapOf(
                                "code" to editCode,
                                "url" to editUrl,
                                "label" to editLabel,
                                "version" to editVersion
                            )
                        )
                        downloads = StoreSupabaseApi.getSiteDownloads()
                        showEditDialog = null
                    }
                }) {
                    Text("Save", color = Color(0xFF00E5CC))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel", color = Color(0xFF8A929C))
                }
            }
        )
    }

    // Delete confirmation
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Download Link") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        StoreSupabaseApi.deleteSiteDownload(id)
                        downloads = StoreSupabaseApi.getSiteDownloads()
                        showDeleteDialog = null
                    }
                }) {
                    Text("Delete", color = Color(0xFFFF4D6A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color(0xFF8A929C))
                }
            }
        )
    }

    // Add dialog
    if (showAddDialog) {
        var addCode by remember { mutableStateOf("") }
        var addUrl by remember { mutableStateOf("") }
        var addLabel by remember { mutableStateOf("") }
        var addVersion by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Download Link", color = Color(0xFFF2F4F6)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = addCode, onValueChange = { addCode = it },
                        label = { Text("Code (e.g., synapse_apk)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addUrl, onValueChange = { addUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addLabel, onValueChange = { addLabel = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addVersion, onValueChange = { addVersion = it },
                        label = { Text("Version (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        StoreSupabaseApi.insertSiteDownload(
                            mapOf(
                                "code" to addCode,
                                "url" to addUrl,
                                "label" to addLabel,
                                "version" to addVersion
                            )
                        )
                        downloads = StoreSupabaseApi.getSiteDownloads()
                        showAddDialog = false
                    }
                }) {
                    Text("Add", color = Color(0xFF00E5CC))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF8A929C))
                }
            }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5CC),
    unfocusedBorderColor = Color(0xFF8A929C),
    focusedTextColor = Color(0xFFF2F4F6),
    unfocusedTextColor = Color(0xFFF2F4F6),
    focusedLabelColor = Color(0xFF00E5CC),
    unfocusedLabelColor = Color(0xFF8A929C)
)