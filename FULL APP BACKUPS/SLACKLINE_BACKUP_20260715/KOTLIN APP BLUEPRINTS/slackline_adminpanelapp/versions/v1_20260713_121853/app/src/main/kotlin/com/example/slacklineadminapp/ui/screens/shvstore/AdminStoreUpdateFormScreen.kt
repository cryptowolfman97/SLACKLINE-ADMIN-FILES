package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AdminStoreUpdateFormScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val config = StoreSupabaseApi.getStoreConfig()
        config?.let {
            version = it["latest_version"]?.jsonPrimitive?.content ?: ""
            apkUrl = it["apk_url"]?.jsonPrimitive?.content ?: ""
            message = it["update_message"]?.jsonPrimitive?.content ?: ""
        }
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Store Update Config", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF2F4F6))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Latest Version") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = apkUrl, onValueChange = { apkUrl = it }, label = { Text("APK URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Update Message") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            scope.launch { StoreSupabaseApi.updateStoreConfig("1", mapOf("latest_version" to version, "apk_url" to apkUrl, "update_message" to message)); onDone() }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC))) { Text("Save") }
    }
}