@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*

private val BG        = Color(0xFF000000)
private val CARD      = Color(0xFF0E0E0E)
private val BORDER    = Color(0xFF1C1C1C)
private val ACCENT    = Color(0xFF00E5CC)
private val GOLD      = Color(0xFFFFB300)
private val TXT       = Color(0xFFF2F4F6)
private val TXT_MUTED = Color(0xFF4A5260)
private val TXT_SUB   = Color(0xFF8A929C)

data class AppUser(
    val userId:      String,
    val email:       String,
    val deviceModel: String,
    val lastSeen:    String,
    val hasPush:     Boolean
)

@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    var users     by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // 1. Fetch all auth users via profiles table
            val profilesResp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/profiles") {
                header("apikey", StoreSupabaseApi.ANON_KEY)
                header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                parameter("select", "id,email")
            }

            // 2. Fetch all device tokens
            val tokensResp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/device_tokens") {
                header("apikey", StoreSupabaseApi.ANON_KEY)
                header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                parameter("select", "user_id,device_model,updated_at")
            }

            val profiles = if (profilesResp.status == HttpStatusCode.OK)
                profilesResp.body<List<JsonObject>>() else emptyList()

            val tokens = if (tokensResp.status == HttpStatusCode.OK)
                tokensResp.body<List<JsonObject>>() else emptyList()

            // Map user_id → token info
            val tokenMap = tokens.associateBy { it["user_id"]?.jsonPrimitive?.content ?: "" }

            users = profiles.map { profile ->
                val uid   = profile["id"]?.jsonPrimitive?.content ?: ""
                val email = profile["email"]?.jsonPrimitive?.content ?: "Unknown"
                val token = tokenMap[uid]
                AppUser(
                    userId      = uid,
                    email       = email,
                    deviceModel = token?.get("device_model")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "Unknown device",
                    lastSeen    = token?.get("updated_at")?.jsonPrimitive?.content?.take(10) ?: "Never",
                    hasPush     = token != null
                )
            }.sortedByDescending { it.hasPush }

        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("App Users", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TXT)
                        if (users.isNotEmpty())
                            Text("${users.size} registered · ${users.count { it.hasPush }} with push", fontSize = 10.sp, color = ACCENT)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(ACCENT.copy(alpha = 0.1f))
                                .border(1.dp, ACCENT.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ACCENT, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        containerColor = BG
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = ACCENT, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Text("Loading users…", color = TXT_MUTED, fontSize = 13.sp)
                }
            }
            return@Scaffold
        }

        if (users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                            .background(CARD).border(1.dp, BORDER, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.People, null, tint = TXT_MUTED, modifier = Modifier.size(32.dp)) }
                    Text("No users yet", color = TXT_MUTED, fontSize = 14.sp)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = padding.calculateTopPadding())
        ) {
            // Summary bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryChip(
                        label = "Total",
                        value = "${users.size}",
                        color = ACCENT,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "Push enabled",
                        value = "${users.count { it.hasPush }}",
                        color = Color(0xFF00C96B),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "No push",
                        value = "${users.count { !it.hasPush }}",
                        color = TXT_MUTED,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            items(users) { user ->
                UserCard(user = user)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.7f), letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun UserCard(user: AppUser) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CARD)
            .border(
                1.dp,
                if (user.hasPush) ACCENT.copy(alpha = 0.2f) else BORDER,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (user.hasPush) ACCENT.copy(alpha = 0.12f) else Color(0xFF141414)
                    )
                    .border(1.dp, if (user.hasPush) ACCENT.copy(alpha = 0.3f) else BORDER, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.email.first().uppercaseChar().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (user.hasPush) ACCENT else TXT_MUTED
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.email,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TXT,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = TXT_MUTED, modifier = Modifier.size(11.dp))
                    Text(user.deviceModel, fontSize = 11.sp, color = TXT_MUTED, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, tint = TXT_MUTED, modifier = Modifier.size(11.dp))
                    Text(
                        if (user.lastSeen == "Never") "Never signed in" else "Last seen ${user.lastSeen}",
                        fontSize = 11.sp, color = TXT_MUTED
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Push badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (user.hasPush) Color(0xFF00C96B).copy(alpha = 0.1f)
                        else TXT_MUTED.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        if (user.hasPush) Color(0xFF00C96B).copy(alpha = 0.3f) else BORDER,
                        RoundedCornerShape(7.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(
                        if (user.hasPush) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        null,
                        tint = if (user.hasPush) Color(0xFF00C96B) else TXT_MUTED,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        if (user.hasPush) "Push" else "Off",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.hasPush) Color(0xFF00C96B) else TXT_MUTED
                    )
                }
            }
        }
    }
}
