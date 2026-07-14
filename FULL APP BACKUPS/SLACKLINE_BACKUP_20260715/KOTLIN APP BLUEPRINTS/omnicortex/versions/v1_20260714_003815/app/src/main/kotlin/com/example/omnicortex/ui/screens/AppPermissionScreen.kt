package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.data.models.AppRiskEntry
import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.AppPermissionViewModel

@Composable
fun AppPermissionScreen(onBack: () -> Unit) {
    val vm: AppPermissionViewModel = viewModel()
    val state  by vm.state.collectAsState()
    val filter by vm.filter.collectAsState()
    val search by vm.search.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = AegisPurple)
            }
            Column(Modifier.weight(1f)) {
                Text("App Intelligence", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Permission risk analysis", color = TextMuted, fontSize = 11.sp)
            }
            Button(
                onClick  = { vm.scan() },
                enabled  = state !is AppPermissionViewModel.State.Scanning,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = AegisPurple,
                    disabledContainerColor = AegisPurple.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    if (state is AppPermissionViewModel.State.Scanning) "Scanning…" else "Scan Apps",
                    color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
        }

        when (val s = state) {
            is AppPermissionViewModel.State.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon        = Icons.Default.Apps,
                        title       = "App Permission Intelligence",
                        subtitle    = "Scans all installed apps and maps their\npermissions to real security risk categories.",
                        actionLabel = "Scan Now",
                        onAction    = { vm.scan() }
                    )
                }
            }

            is AppPermissionViewModel.State.Scanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ScanningIndicator("Analysing installed apps…", AegisPurple)
                }
            }

            is AppPermissionViewModel.State.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon        = Icons.Default.Error,
                        title       = "Scan Failed",
                        subtitle    = s.msg,
                        actionLabel = "Retry",
                        onAction    = { vm.scan() }
                    )
                }
            }

            is AppPermissionViewModel.State.Done -> {
                val filtered = vm.filteredApps(s.result)

                // ── Stats ─────────────────────────────────────────────────────
                StatStrip(
                    listOf(
                        Triple("${s.result.totalScanned}", "Scanned", TextSecondary),
                        Triple("${s.result.apps.size}", "Flagged", AegisAmber),
                        Triple("${s.result.criticalCount}", "Critical", AegisRed),
                        Triple("${s.result.highCount}", "High", AegisOrange)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // ── Search bar ────────────────────────────────────────────────
                OutlinedTextField(
                    value         = search,
                    onValueChange = { vm.setSearch(it) },
                    placeholder   = { Text("Search apps…", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AegisPurple,
                        unfocusedBorderColor = BgCardBorder,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = AegisPurple
                    )
                )

                Spacer(Modifier.height(8.dp))

                // ── Filter chips ──────────────────────────────────────────────
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.filterOptions) { opt ->
                        val selected = filter == opt
                        val color    = when (opt) {
                            "Critical" -> AegisRed
                            "High"     -> AegisOrange
                            "Medium"   -> AegisAmber
                            else       -> AegisPurple
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) color.copy(alpha = 0.2f) else BgCard)
                                .clickable { vm.setFilter(opt) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                opt,
                                color = if (selected) color else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── App list ──────────────────────────────────────────────────
                if (filtered.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No apps matching this filter.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppRiskCard(app)
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRiskCard(app: AppRiskEntry) {
    var expanded by remember { mutableStateOf(false) }
    val riskColor = when {
        app.riskScore >= 80 -> AegisRed
        app.riskScore >= 50 -> AegisOrange
        app.riskScore >= 20 -> AegisAmber
        else                -> TextSecondary
    }
    val riskLabel = when {
        app.riskScore >= 80 -> "CRITICAL"
        app.riskScore >= 50 -> "HIGH"
        app.riskScore >= 20 -> "MEDIUM"
        else                -> "LOW"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Risk score circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${app.riskScore}",
                    color = riskColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(Modifier.weight(1f)) {
                Text(app.appName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(app.packageName, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(riskLabel, color = riskColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("${app.riskFlags.size} flag(s)", color = TextMuted, fontSize = 10.sp)
            }
        }

        if (expanded) {
            HorizontalDivider(color = BgCardBorder, modifier = Modifier.padding(horizontal = 12.dp))
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                app.riskFlags.forEach { flag ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            severityIcon(flag.severity),
                            null,
                            tint = severityColor(flag.severity, false),
                            modifier = Modifier.size(15.dp).padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                flag.label,
                                color = severityColor(flag.severity, false),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(flag.detail, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
