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
fun AdminContactScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var discord by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val info = StoreSupabaseApi.getContactInfo()
        info?.let {
            whatsapp = it["whatsapp"]?.jsonPrimitive?.content ?: ""
            email = it["email"]?.jsonPrimitive?.content ?: ""
            discord = it["discord"]?.jsonPrimitive?.content ?: ""
            telegram = it["telegram"]?.jsonPrimitive?.content ?: ""
            instagram = it["instagram"]?.jsonPrimitive?.content ?: ""
            website = it["website"]?.jsonPrimitive?.content ?: ""
        }
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = discord, onValueChange = { discord = it }, label = { Text("Discord") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = telegram, onValueChange = { telegram = it }, label = { Text("Telegram") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = instagram, onValueChange = { instagram = it }, label = { Text("Instagram") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            scope.launch {
                StoreSupabaseApi.upsertContactInfo(mapOf("whatsapp" to whatsapp, "email" to email, "discord" to discord, "telegram" to telegram, "instagram" to instagram, "website" to website))
                onDone()
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC))) { Text("Save") }
    }
}