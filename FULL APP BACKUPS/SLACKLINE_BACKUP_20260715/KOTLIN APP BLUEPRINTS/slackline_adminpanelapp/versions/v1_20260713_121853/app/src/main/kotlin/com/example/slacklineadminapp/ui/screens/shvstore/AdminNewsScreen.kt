package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AdminNewsScreen(onEdit: (String) -> Unit, onAdd: () -> Unit) {
    var news by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/news") {
            header("apikey", StoreSupabaseApi.ANON_KEY)
            header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
            parameter("order", "created_at.desc")
        }
        if (resp.status == HttpStatusCode.OK) news = resp.body()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = Color(0xFF00E5CC)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(news) { item ->
                val id = item["id"]?.jsonPrimitive?.content ?: return@items
                val isPublished = item["is_published"]?.jsonPrimitive?.boolean ?: false
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item["title"]?.jsonPrimitive?.content ?: "",
                                color = Color(0xFFF2F4F6),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isPublished,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        StoreSupabaseApi.updateNews(id, mapOf("is_published" to checked))
                                        // refresh list
                                        val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/news") {
                                            header("apikey", StoreSupabaseApi.ANON_KEY)
                                            header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                                            parameter("order", "created_at.desc")
                                        }
                                        if (resp.status == HttpStatusCode.OK) news = resp.body()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            item["created_at"]?.jsonPrimitive?.content?.take(10) ?: "",
                            color = Color(0xFF8A929C),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onEdit(id) }) { Text("Edit", color = Color(0xFF00E5CC)) }
                            TextButton(onClick = { showDelete = id }) { Text("Delete", color = Color(0xFFFF4D6A)) }
                        }
                    }
                }
            }
        }
    }

    if (showDelete != null) {
        AlertDialog(
            onDismissRequest = { showDelete = null },
            title = { Text("Delete News") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        StoreSupabaseApi.deleteNews(showDelete!!)
                        val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/news") {
                            header("apikey", StoreSupabaseApi.ANON_KEY)
                            header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                            parameter("order", "created_at.desc")
                        }
                        if (resp.status == HttpStatusCode.OK) news = resp.body()
                        showDelete = null
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = null }) { Text("Cancel") } }
        )
    }
}