package com.example.slacklineadminapp.ui.screens.shvstore
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun AdminNewsFormScreen(newsId: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(false) }
    val isEditing = newsId != "new"

    LaunchedEffect(newsId) {
        if (isEditing) {
            val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/news") {
                header("apikey", StoreSupabaseApi.ANON_KEY)
                header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                parameter("id", "eq.$newsId")
            }
            if (resp.status == HttpStatusCode.OK) {
                val list = resp.body<List<JsonObject>>()
                val item = list.firstOrNull()
                if (item != null) {
                    title = item["title"]?.jsonPrimitive?.content ?: ""
                    body = item["body"]?.jsonPrimitive?.content ?: ""
                    coverImage = item["cover_image_url"]?.jsonPrimitive?.content ?: ""
                    isPublished = item["is_published"]?.jsonPrimitive?.boolean ?: false
                }
            }
        }
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = coverImage, onValueChange = { coverImage = it }, label = { Text("Cover Image URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Published")
            Switch(checked = isPublished, onCheckedChange = { isPublished = it })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val data = mapOf("title" to title, "body" to body, "cover_image_url" to coverImage, "is_published" to isPublished)
                    if (isEditing) StoreSupabaseApi.updateNews(newsId, data) else StoreSupabaseApi.insertNews(data)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC))
        ) { Text(if (isEditing) "Update" else "Create") }
    }
}