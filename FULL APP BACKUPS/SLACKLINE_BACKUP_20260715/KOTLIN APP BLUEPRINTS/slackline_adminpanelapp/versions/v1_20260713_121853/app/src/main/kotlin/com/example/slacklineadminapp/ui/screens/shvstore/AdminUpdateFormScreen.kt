@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AdminUpdateFormScreen(updateId: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var appName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetVersion by remember { mutableStateOf("") }
    var expectedDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("planned") }
    val isEditing = updateId != "new"

    LaunchedEffect(updateId) {
        if (isEditing) {
            val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/upcoming_updates") {
                header("apikey", StoreSupabaseApi.ANON_KEY)
                header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                parameter("id", "eq.$updateId")
            }
            if (resp.status == HttpStatusCode.OK) {
                val list = resp.body<List<JsonObject>>()
                val item = list.firstOrNull()
                item?.let {
                    appName = it["app_name"]?.jsonPrimitive?.content ?: ""
                    title = it["title"]?.jsonPrimitive?.content ?: ""
                    description = it["description"]?.jsonPrimitive?.content ?: ""
                    targetVersion = it["target_version"]?.jsonPrimitive?.content ?: ""
                    expectedDate = it["expected_date"]?.jsonPrimitive?.content ?: ""
                    status = it["status"]?.jsonPrimitive?.content ?: "planned"
                }
            }
        }
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Feature Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = targetVersion, onValueChange = { targetVersion = it }, label = { Text("Target Version") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = expectedDate, onValueChange = { expectedDate = it }, label = { Text("Expected Date") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status: ", color = Color(0xFFF2F4F6))
            listOf("planned", "in_progress", "released").forEach { s ->
                FilterChip(
                    selected = status == s,
                    onClick = { status = s },
                    label = { Text(s.replace("_"," ")) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (s) {
                            "planned" -> Color(0xFF8A929C)
                            "in_progress" -> Color(0xFFFFB300)
                            else -> Color(0xFF00E5CC)
                        }
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val data = mapOf("app_name" to appName, "title" to title, "description" to description, "target_version" to targetVersion, "expected_date" to expectedDate, "status" to status)
                    if (isEditing) StoreSupabaseApi.updateUpdate(updateId, data) else StoreSupabaseApi.insertUpdate(data)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC))
        ) { Text(if (isEditing) "Update" else "Create") }
    }
}