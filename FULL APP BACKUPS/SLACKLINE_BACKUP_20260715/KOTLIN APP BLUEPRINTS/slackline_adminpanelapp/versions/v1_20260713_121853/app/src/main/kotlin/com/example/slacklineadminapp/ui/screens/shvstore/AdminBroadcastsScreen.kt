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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AdminBroadcastsScreen() {
    var broadcasts by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var showForm   by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    LaunchedEffect(Unit) { broadcasts = StoreSupabaseApi.getBroadcasts() }

    if (showForm) {
        AdminBroadcastFormScreen(onDone = {
            showForm = false
            scope.launch { broadcasts = StoreSupabaseApi.getBroadcasts() }
        })
        return
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showForm = true },
                containerColor = Color(0xFF00E5CC),
                contentColor   = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Broadcast")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (broadcasts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No broadcasts yet.\nTap + to create one.",
                            color = Color(0xFF4A5260),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
            items(broadcasts) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E))) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item["title"]?.jsonPrimitive?.content ?: "",
                                color = Color(0xFFF2F4F6),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                (item["message"]?.jsonPrimitive?.content?.take(80) ?: "") + "...",
                                color = Color(0xFF8A929C),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = {
                            scope.launch {
                                StoreSupabaseApi.deleteBroadcast(
                                    item["id"]?.jsonPrimitive?.content ?: ""
                                )
                                broadcasts = StoreSupabaseApi.getBroadcasts()
                            }
                        }) { Text("Delete", color = Color(0xFFFF4D6A)) }
                    }
                }
            }
        }
    }
}
