package com.example.slacklineadminapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

private const val SUPABASE_URL = "https://ovdxetyadfsxehwnbyuz.supabase.co"
private const val SUPABASE_ANON_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

private val supabaseClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

@Serializable
data class ClientRequest(
    val id: String,
    val name: String,
    val email: String?,
    val whatsapp: String? = null,
    val category: String,
    val description: String,
    @SerialName("created_at") val createdAt: String,
    val status: String
)

enum class RequestStatus(val value: String, val label: String, val color: Color) {
    PENDING("pending", "Pending", AmberCol),
    IN_PROGRESS("in_progress", "In Progress", BlueCol),
    DONE("done", "Done", GreenCol);

    companion object {
        fun fromString(statusStr: String): RequestStatus {
            return entries.find { it.value == statusStr.lowercase() } ?: PENDING
        }
    }
}

// ── Advanced PIN confirmation dialog ──────────────────────────────────────────

@Composable
private fun AdvancedPinDialog(
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(RedCol.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedCol, modifier = Modifier.size(24.dp))
                }

                Text(
                    "Confirm Delete",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    "Enter your Advanced PIN to permanently delete this request.",
                    color = SubText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // PIN dots display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { i ->
                        Text(
                            text = if (i < pinInput.length) "●" else "○",
                            fontSize = 24.sp,
                            color = if (i < pinInput.length) TealCol else SubText
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = RedCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Number pad
                val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    digits.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .background(
                                            if (key.isEmpty()) Color.Transparent
                                            else Color.White.copy(alpha = 0.07f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = key.isNotEmpty()) {
                                            if (key == "⌫") {
                                                if (pinInput.isNotEmpty()) {
                                                    pinInput = pinInput.dropLast(1)
                                                    errorMsg = ""
                                                }
                                            } else if (pinInput.length < 6) {
                                                pinInput += key
                                                errorMsg = ""
                                                if (pinInput.length == 6) {
                                                    val stored = SecurityConfig.get(context).advPin
                                                    if (pinInput == stored) {
                                                        onConfirmed()
                                                    } else {
                                                        errorMsg = "Incorrect PIN"
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key.isNotEmpty()) {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = if (key == "⌫") 20.sp else 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SubText, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientServiceRequestsScreen(onNavigateBack: () -> Unit) {
    val appColors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isRefreshing by remember { mutableStateOf(true) }
    var requests by remember { mutableStateOf<List<ClientRequest>>(emptyList()) }

    val fetchRequests: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val response = supabaseClient.get("$SUPABASE_URL/rest/v1/client_requests?select=*&order=created_at.desc") {
                    headers {
                        append("apikey", SUPABASE_ANON_KEY)
                        append("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    }
                }
                requests = response.body()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRequests()
    }

    val updateStatus: (String, RequestStatus) -> Unit = { requestId, newStatus ->
        coroutineScope.launch {
            try {
                supabaseClient.patch("$SUPABASE_URL/rest/v1/client_requests?id=eq.$requestId") {
                    headers {
                        append("apikey", SUPABASE_ANON_KEY)
                        append("Authorization", "Bearer $SUPABASE_ANON_KEY")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                    setBody("""{"status":"${newStatus.value}"}""")
                }
                requests = requests.map {
                    if (it.id == requestId) it.copy(status = newStatus.value) else it
                }
                Toast.makeText(context, "Status updated to ${newStatus.label}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val deleteRequest: (String, String) -> Unit = { requestId, clientName ->
        coroutineScope.launch {
            try {
                supabaseClient.delete("$SUPABASE_URL/rest/v1/client_requests?id=eq.$requestId") {
                    headers {
                        append("apikey", SUPABASE_ANON_KEY)
                        append("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    }
                }
                requests = requests.filter { it.id != requestId }
                Toast.makeText(context, "Request from $clientName deleted.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to delete request.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = appColors.bg,
        topBar = {
            TopAppBar(
                title = { Text("Client Service Requests", color = CyanCol, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TealCol)
                    }
                },
                actions = {
                    IconButton(onClick = fetchRequests) {
                        if (isRefreshing) {
                            CircularProgressIndicator(color = CyanCol, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TealCol)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.bg.copy(alpha = 0.9f))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard("Pending", requests.count { RequestStatus.fromString(it.status) == RequestStatus.PENDING }, AmberCol, Modifier.weight(1f))
                StatusCard("In Progress", requests.count { RequestStatus.fromString(it.status) == RequestStatus.IN_PROGRESS }, BlueCol, Modifier.weight(1f))
                StatusCard("Done", requests.count { RequestStatus.fromString(it.status) == RequestStatus.DONE }, GreenCol, Modifier.weight(1f))
            }

            HorizontalDivider(color = TealCol.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    RequestItemCard(
                        request = request,
                        onStatusChange = { newStatus -> updateStatus(request.id, newStatus) },
                        onDelete = { deleteRequest(request.id, request.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), color = color, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RequestItemCard(
    request: ClientRequest,
    onStatusChange: (RequestStatus) -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    val currentStatus = RequestStatus.fromString(request.status)

    val formattedDate = remember(request.createdAt) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            formatter.format(parser.parse(request.createdAt) ?: Date())
        } catch (e: Exception) {
            request.createdAt.take(10)
        }
    }

    // Advanced PIN dialog
    if (showPinDialog) {
        AdvancedPinDialog(
            onConfirmed = {
                showPinDialog = false
                onDelete()
            },
            onDismiss = { showPinDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .border(1.dp, if (expanded) TealCol.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = request.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(text = request.category, color = TealCol, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .background(currentStatus.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = currentStatus.label, color = currentStatus.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = request.description,
                color = SubText,
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Received: $formattedDate",
                color = SubText.copy(alpha = 0.4f),
                fontSize = 10.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = TealCol.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    // ── Status Flow Selection Array ──
                    Text("Change Status:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RequestStatus.entries.forEach { statusOption ->
                            val isSelected = currentStatus == statusOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) statusOption.color.copy(alpha = 0.2f) else appColors.bg,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) statusOption.color else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { if (!isSelected) onStatusChange(statusOption) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusOption.label,
                                    color = if (isSelected) statusOption.color else SubText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = TealCol.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    // ── Contact Action Routing ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Routing Button
                        val hasWhatsApp = !request.whatsapp.isNullOrBlank()
                        Button(
                            onClick = {
                                if (hasWhatsApp) {
                                    val safePhone = request.whatsapp!!.replace(Regex("[^0-9+]"), "")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$safePhone"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Client has not filled that section.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasWhatsApp) Color(0xFF25D366) else Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (hasWhatsApp) Color.White else SubText.copy(alpha = 0.4f))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (hasWhatsApp) "WhatsApp" else "No WhatsApp",
                                color = if (hasWhatsApp) Color.White else SubText.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Email Routing Button
                        val hasEmail = !request.email.isNullOrBlank()
                        Button(
                            onClick = {
                                if (hasEmail) {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${request.email}"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Client has not filled that section.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasEmail) CyanCol else Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (hasEmail) appColors.bg else SubText.copy(alpha = 0.4f))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (hasEmail) "Email Client" else "No Email",
                                color = if (hasEmail) appColors.bg else SubText.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(color = RedCol.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    // ── Delete Entry ──
                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedCol.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(RedCol.copy(alpha = 0.4f))
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedCol)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Request", color = RedCol, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
