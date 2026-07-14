package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.AppStorage
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ActivityLogScreen(onNavigateBack: () -> Unit) {
    var logs by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val file = AppStorage.activityLogFile()
            logs = AppStorage.loadJson(file, emptyList<Map<String, String>>())
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bg)
    ) {
        // Header
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, null, tint = OrangeCol, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Global Activity Log", color = OrangeCol, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeCol)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        AppCard {
                            BodyText("No activity recorded yet. Generate a license to see logs here.", SubText)
                        }
                    }
                } else {
                    item {
                        BodyText("Showing ${logs.take(50).size} of ${logs.size} entries.", SubText)
                    }
                    items(logs.take(50)) { log ->
                        AppCard(color = LocalAppColors.current.card2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    log["action"] ?: "Unknown Action",
                                    color = OrangeCol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    (log["time"] ?: "").take(16).replace("T", " "),
                                    color = SubText,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                "[${log["product"] ?: ""}]  •  ${log["details"] ?: "No details."}",
                                color = TextCol,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        BottomNavBar(listOf("← BACK" to onNavigateBack))
    }
}
