package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.LocalAppColors

private sealed class AuthState {
    object Loading : AuthState()
    object Ready   : AuthState()
    object NoCreds : AuthState()
    object Failed  : AuthState()
}

private data class StoreModule(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

private val STORE_MODULES = listOf(
    StoreModule("Apps\nManager",       Icons.Default.Apps,           "store_apps",           Color(0xFF00E5CC)),
    StoreModule("News\nManager",       Icons.Default.Article,        "store_news",           Color(0xFF4FC3F7)),
    StoreModule("Broadcasts",          Icons.Default.Campaign,       "store_broadcasts",     Color(0xFFFFB300)),
    StoreModule("App\nUpdates",        Icons.Default.SystemUpdate,   "store_updates",        Color(0xFF81C784)),
    StoreModule("Users",               Icons.Default.Group,          "store_users",          Color(0xFFCE93D8)),
    StoreModule("Contact\nInfo",       Icons.Default.ContactPhone,   "store_contact",        Color(0xFFFF8A65)),
    StoreModule("Download\nLinks",     Icons.Default.Link,           "store_download_links", Color(0xFF4DB6AC)),
    StoreModule("Store\nUpdate",       Icons.Default.Upgrade,        "store_update_form",    Color(0xFFA5D6A7)),
)

@Composable
fun StoreAdminHubScreen(
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }

    LaunchedEffect(Unit) {
        val cfg = SecurityConfig.get(context)
        if (cfg.storeEmail.isBlank() || cfg.storePassword.isBlank()) {
            authState = AuthState.NoCreds
            return@LaunchedEffect
        }
        try {
            val ok = StoreSupabaseApi.signIn(cfg.storeEmail, cfg.storePassword)
            authState = if (ok) AuthState.Ready else AuthState.Failed
        } catch (e: Exception) {
            authState = AuthState.Failed
        }
    }

    if (authState != AuthState.Ready) {
        Box(
            modifier = Modifier.fillMaxSize().background(appColors.bg),
            contentAlignment = Alignment.Center
        ) {
            when (authState) {
                AuthState.Loading -> CircularProgressIndicator(
                    color = Color(0xFF00E5CC),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
                AuthState.NoCreds -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFFFB300), modifier = Modifier.size(36.dp))
                    Text("Store credentials not set.", color = appColors.text, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text("Go to Settings → Store Config to set them.", color = appColors.subtext, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onNavigateBack) { Text("Back") }
                }
                AuthState.Failed -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF4D6A), modifier = Modifier.size(36.dp))
                    Text("Store sign-in failed.", color = appColors.text, fontSize = 14.sp)
                    Text("Check credentials in Settings or network.", color = appColors.subtext, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onNavigateBack) { Text("Back") }
                }
                else -> {}
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.card)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF00E5CC))
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SHV STORE ADMIN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5CC),
                    letterSpacing = 1.5.sp
                )
                Text(
                    "Store Management Console",
                    fontSize = 11.sp,
                    color = appColors.subtext,
                    letterSpacing = 0.3.sp
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 1.dp)

        // ── Module grid ───────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(STORE_MODULES) { module ->
                StoreModuleTile(module = module, onClick = { onNavigate(module.route) })
            }
        }
    }
}

@Composable
private fun StoreModuleTile(module: StoreModule, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(appColors.card)
            .border(1.dp, module.color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(module.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(module.icon, null, tint = module.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                module.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.text,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}
