package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Colour tokens (extend / override your theme values as needed)
// ---------------------------------------------------------------------------
private val BgPage       = Color(0xFF000000)
private val BgCard       = Color(0xFF0E0E0E)
private val BgCardBorder = Color(0xFF1C1C1C)
private val TealAccent   = Color(0xFF00E5CC)
private val RoseAccent   = Color(0xFFE8445A)
private val AmberAccent  = Color(0xFFFBBC04)
private val TextPrimary  = Color(0xFFF2F4F6)
private val TextMuted    = Color(0xFF4A5260)
private val GreenDim     = Color(0xFF1DB954)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    onNavigateTo: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var userEmail by remember { mutableStateOf(StoreSupabaseApi.userEmail ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            stats = StoreSupabaseApi.getStats()
            userEmail = StoreSupabaseApi.userEmail ?: ""
        } catch (e: Exception) {
            errorMessage = "Failed to load stats: ${e.message}"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgPage) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Header ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                DashboardHeader(userEmail = userEmail, onSignOut = {
                    scope.launch { StoreSupabaseApi.signOut(); onSignOut() }
                })
                Spacer(Modifier.height(16.dp))
            }

            // ── Error banner ─────────────────────────────────────────────
            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onRetry = {
                            scope.launch {
                                errorMessage = null
                                try { stats = StoreSupabaseApi.getStats() }
                                catch (e: Exception) { errorMessage = e.message }
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Stat grid ────────────────────────────────────────────────
            item {
                SectionLabel("Overview")
                Spacer(Modifier.height(8.dp))

                if (stats.isNotEmpty()) {
                    val statItems = listOf(
                        StatData("Total Apps",    stats["total_apps"]?.toString()      ?: "0", TealAccent,  Icons.Default.Apps),
                        StatData("Published",     stats["published_apps"]?.toString()  ?: "0", GreenDim,    Icons.Default.CheckCircle),
                        StatData("News Posts",    stats["total_news"]?.toString()      ?: "0", AmberAccent, Icons.Default.Article),
                        StatData("Roadmap Items", stats["total_updates"]?.toString()   ?: "0", GreenDim,    Icons.Default.Timeline),
                        StatData("Total Users",   stats["total_users"]?.toString()     ?: "0", TealAccent,  Icons.Default.People),
                        StatData("Downloads",     stats["total_downloads"]?.toString() ?: "0", AmberAccent, Icons.Default.Download),
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp) // 3 rows × 76dp + 2 gaps × 10dp
                    ) {
                        items(statItems.size) { i -> StatCard(statItems[i]) }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgCard)
                            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TealAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Loading overview…", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
            }

            // ── Content & Management ─────────────────────────────────────
            item {
                SectionLabel("Content & Management")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionCard(
                        label = "New App",
                        icon = Icons.Default.Add,
                        accentColor = TealAccent,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_app_form/new") }

                    ActionCard(
                        label = "Roadmap",
                        icon = Icons.Default.Timeline,
                        accentColor = GreenDim,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_update_form/new") }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionCard(
                        label = "New News",
                        icon = Icons.Default.Article,
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_news_form/new") }

                    ActionCard(
                        label = "Contact",
                        icon = Icons.Default.ContactMail,
                        accentColor = TextMuted,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_contact") }
                }

                Spacer(Modifier.height(18.dp))
            }

            // ── Audience Engagement ──────────────────────────────────────
            item {
                SectionLabel("Audience Engagement")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionCard(
                        label = "Broadcast",
                        icon = Icons.Default.Campaign,
                        accentColor = RoseAccent,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_broadcast_form") }

                    ActionCard(
                        label = "Sent",
                        icon = Icons.AutoMirrored.Filled.List,
                        accentColor = TextMuted,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateTo("admin_broadcasts_list") }
                }

                Spacer(Modifier.height(10.dp))

                ActionCardWide(
                    label = "App Users",
                    sublabel = "View registered users & device info",
                    icon = Icons.Default.People,
                    accentColor = TealAccent
                ) { onNavigateTo("admin_users") }

                Spacer(Modifier.height(18.dp))
            }

            // ── Store update config (full-width) ─────────────────────────
            item {
                SectionLabel("System")
                Spacer(Modifier.height(8.dp))

                ActionCardWide(
                    label = "Store Update Config",
                    sublabel = "Manage forced-update rules & version flags",
                    icon = Icons.Default.SystemUpdate,
                    accentColor = TealAccent
                ) { onNavigateTo("admin_store_update_form") }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------
@Composable
private fun DashboardHeader(userEmail: String, onSignOut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                userEmail,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A0810))
                .border(1.dp, RoseAccent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onSignOut, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign out",
                    tint = RoseAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section label
// ---------------------------------------------------------------------------
@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TealAccent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TealAccent,
            letterSpacing = 1.2.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Stat card — reduced height 84dp → 68dp, tighter padding
// ---------------------------------------------------------------------------
private data class StatData(
    val label: String,
    val value: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun StatCard(data: StatData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)                              // was 84dp
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)                             // was 14dp
    ) {
        // Icon top-right
        Icon(
            data.icon,
            contentDescription = null,
            tint = data.color.copy(alpha = 0.18f),
            modifier = Modifier
                .size(26.dp)                            // was 32dp
                .align(Alignment.TopEnd)
        )
        // Value + label bottom-left
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                data.value,
                fontSize = 20.sp,                       // was 24sp
                fontWeight = FontWeight.Bold,
                color = data.color,
                letterSpacing = (-0.5).sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                data.label,
                fontSize = 10.sp,
                color = TextMuted,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Action card — reduced height 72dp → 54dp
// ---------------------------------------------------------------------------
@Composable
private fun ActionCard(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(46.dp),              // was 72dp
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BgCardBorder, BgCardBorder))
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),           // was 14dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)                        // was 34dp
                    .clip(RoundedCornerShape(7.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
            }
            Text(
                label,
                color = TextPrimary,
                fontSize = 12.sp,                       // was 13sp
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Wide action card — reduced height 72dp → 54dp
// ---------------------------------------------------------------------------
@Composable
private fun ActionCardWide(
    label: String,
    sublabel: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().height(49.dp), // was 72dp
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BgCardBorder, BgCardBorder))
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),           // was 16dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)                        // was 38dp
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(sublabel, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Error banner
// ---------------------------------------------------------------------------
@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C0E0E))
            .border(1.dp, RoseAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(16.dp))
        Text(message, color = RoseAccent, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2)
        TextButton(
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColors(contentColor = TealAccent),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}