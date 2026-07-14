package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*

@Composable
fun AdminUpdatesScreen(onEdit: (String) -> Unit, onAdd: () -> Unit) {
    var updates by remember { mutableStateOf<List<JsonObject>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/upcoming_updates") {
                header("apikey", StoreSupabaseApi.ANON_KEY)
                header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                parameter("order", "created_at.desc")
            }
            if (resp.status == HttpStatusCode.OK) updates = resp.body()
        } catch (_: Exception) {}
    }

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd, containerColor = Color(0xFF00E5CC)) { Icon(Icons.Default.Add, contentDescription = "Add") } },
        containerColor = Color(0xFF000000)
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(updates) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E))) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(item["title"]?.jsonPrimitive?.content ?: "", color = Color(0xFFF2F4F6), style = MaterialTheme.typography.titleSmall)
                            Text(item["status"]?.jsonPrimitive?.content?.replace("_"," ") ?: "", color = Color(0xFF8A929C), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onEdit(item["id"]?.jsonPrimitive?.content ?: "") }) { Text("Edit", color = Color(0xFF00E5CC)) }
                    }
                }
            }
        }
    }
}