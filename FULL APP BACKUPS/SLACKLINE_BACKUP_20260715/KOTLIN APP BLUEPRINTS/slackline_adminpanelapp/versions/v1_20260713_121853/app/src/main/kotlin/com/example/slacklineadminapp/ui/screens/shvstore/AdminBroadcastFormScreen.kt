package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import kotlinx.coroutines.launch

private val BG        = Color(0xFF000000)
private val CARD      = Color(0xFF0E0E0E)
private val BORDER    = Color(0xFF1C1C1C)
private val ACCENT    = Color(0xFF00E5CC)
private val SUCCESS   = Color(0xFF00C96B)
private val ERROR     = Color(0xFFFF4D6A)
private val TXT       = Color(0xFFF2F4F6)
private val TXT_MUTED = Color(0xFF4A5260)
private val TXT_SUB   = Color(0xFF8A929C)

@Composable
fun AdminBroadcastFormScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title       by remember { mutableStateOf("") }
    var message     by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var statusMsg   by remember { mutableStateOf("") }
    var statusOk    by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ACCENT.copy(alpha = 0.1f))
                    .border(1.dp, ACCENT.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Campaign, null, tint = ACCENT, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("New Broadcast", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TXT)
                Text("Saves to inbox + sends push notification", fontSize = 11.sp, color = TXT_MUTED)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Title field
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = broadcastFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        // Message field
        OutlinedTextField(
            value = message, onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            maxLines = 8,
            shape = RoundedCornerShape(14.dp),
            colors = broadcastFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        // Info card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ACCENT.copy(alpha = 0.06f))
                .border(1.dp, ACCENT.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Send, null, tint = ACCENT, modifier = Modifier.size(14.dp))
            Text(
                "This will save to all users' inboxes and send a push notification to every signed-in device instantly.",
                fontSize = 12.sp, color = TXT_SUB, lineHeight = 18.sp
            )
        }

        if (statusMsg.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background((if (statusOk) SUCCESS else ERROR).copy(alpha = 0.1f))
                    .border(1.dp, (if (statusOk) SUCCESS else ERROR).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(statusMsg, color = if (statusOk) SUCCESS else ERROR, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Send button
        Button(
            onClick = {
                if (title.isBlank() || message.isBlank()) {
                    statusMsg = "Title and message are required."
                    statusOk  = false
                    return@Button
                }
                scope.launch {
                    isLoading = true; statusMsg = ""
                    // 1. Save broadcast to Supabase (appears in user inboxes)
                    val saved = StoreSupabaseApi.insertBroadcast(mapOf("title" to title, "message" to message))
                    // 2. Send FCM push notification via Edge Function
                    val pushed = StoreSupabaseApi.sendFcmBroadcast(title, message)

                    isLoading = false
                    if (saved) {
                        statusMsg = if (pushed) "✓ Sent to inbox + push notification delivered."
                                    else        "✓ Saved to inbox. Push notification failed (check Edge Function)."
                        statusOk = saved
                        // brief delay then close
                        kotlinx.coroutines.delay(1800)
                        onDone()
                    } else {
                        statusMsg = "Failed to save broadcast. Please try again."
                        statusOk  = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (!isLoading) Brush.horizontalGradient(listOf(ACCENT, Color(0xFF00BFA8)))
                        else            Brush.horizontalGradient(listOf(ACCENT.copy(alpha = 0.4f), Color(0xFF00BFA8).copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("Sending…", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Text("Send Broadcast", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun broadcastFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Color(0xFF00E5CC).copy(alpha = 0.7f),
    unfocusedBorderColor    = Color(0xFF1C1C1C),
    focusedTextColor        = Color(0xFFF2F4F6),
    unfocusedTextColor      = Color(0xFFF2F4F6),
    cursorColor             = Color(0xFF00E5CC),
    focusedLabelColor       = Color(0xFF00E5CC),
    unfocusedLabelColor     = Color(0xFF4A5260),
    focusedContainerColor   = Color(0xFF0A0A0A),
    unfocusedContainerColor = Color(0xFF0A0A0A)
)
