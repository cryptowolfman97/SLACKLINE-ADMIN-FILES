package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.engine.ApkAnalyserEngine
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.ApkAnalyserViewModel

@Composable
fun ApkAnalyserScreen(onBack: () -> Unit) {
    val vm: ApkAnalyserViewModel = viewModel()
    val state    by vm.state.collectAsState()
    val appList  by vm.appList.collectAsState()
    val search   by vm.search.collectAsState()
    val selTab   by vm.selectedTab.collectAsState()

    Column(Modifier.fillMaxSize().background(BgAmoled)) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(BgCard).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = {
                if (state is ApkAnalyserViewModel.State.Done || state is ApkAnalyserViewModel.State.Error)
                    vm.reset()
                else onBack()
            }) { Icon(Icons.Default.ArrowBack, null, tint = AegisPurple) }
            Column(Modifier.weight(1f)) {
                Text("APK Analyser", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Deep Android app security analysis", color = TextMuted, fontSize = 11.sp)
            }
        }

        when (val s = state) {
            is ApkAnalyserViewModel.State.Idle -> {
                // App list picker
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = search, onValueChange = { vm.setSearch(it) },
                        placeholder = { Text("Search installed apps…", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AegisPurple) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        singleLine = true, shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPurple, unfocusedBorderColor = BgCardBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AegisPurple
                        )
                    )
                    val filtered = vm.filteredApps()
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(icon = Icons.Default.Apps, title = "No Apps Found",
                                subtitle = "No installed user apps match your search.")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filtered, key = { it.first }) { (pkg, label) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BgCard)
                                        .clickable { vm.analyse(pkg) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(AegisPurple.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Android, null, tint = AegisPurple, modifier = Modifier.size(18.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(pkg, color = TextMuted, fontSize = 10.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            is ApkAnalyserViewModel.State.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ScanningIndicator("Analysing APK…", AegisPurple)
                        Text("Scanning manifest, components,\npermissions and secrets",
                            color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            is ApkAnalyserViewModel.State.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = Icons.Default.Error, title = "Analysis Failed",
                        subtitle = s.msg, actionLabel = "Back", onAction = { vm.reset() })
                }
            }

            is ApkAnalyserViewModel.State.Done -> {
                ApkResultView(s.result, selTab) { vm.setTab(it) }
            }
        }
    }
}

@Composable
private fun ApkResultView(
    r: ApkAnalyserEngine.ApkAnalysisResult,
    selectedTab: Int,
    onTab: (Int) -> Unit
) {
    val tabs = listOf("Overview", "Components", "Permissions", "Secrets")
    val criticals = r.findings.count { !it.passed && it.severity == com.example.omnicortex.data.models.Severity.CRITICAL }
    val highs     = r.findings.count { !it.passed && it.severity == com.example.omnicortex.data.models.Severity.HIGH }

    Column(Modifier.fillMaxSize()) {
        // App header
        Row(Modifier.fillMaxWidth().background(BgCard).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AegisPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Android, null, tint = AegisPurple, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(r.appName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(r.packageName, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("v${r.versionName} • API ${r.minSdk}–${r.targetSdk}", color = TextSecondary, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (criticals > 0) Box(Modifier.clip(RoundedCornerShape(5.dp))
                    .background(AegisRed.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("$criticals CRITICAL", color = AegisRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (highs > 0) Box(Modifier.clip(RoundedCornerShape(5.dp))
                    .background(AegisOrange.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("$highs HIGH", color = AegisOrange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = BgCard,
            contentColor = AegisPurple, indicator = {}, divider = {}) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { onTab(i) },
                    text = { Text(t, fontSize = 12.sp,
                        fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == i) AegisPurple else TextMuted) }
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selectedTab) {
                0 -> { // Overview
                    item {
                        StatStrip(listOf(
                            Triple("${r.findings.size}", "Checks", TextSecondary),
                            Triple("$criticals", "Critical", AegisRed),
                            Triple("$highs", "High", AegisOrange),
                            Triple("${r.exportedComponents.size}", "Exported", AegisAmber)
                        ))
                    }
                    item {
                        AegisCard(accentColor = AegisPurple) {
                            SectionHeader("Manifest Flags", AegisPurple)
                            FlagRow("Debuggable", r.isDebuggable, critical = true)
                            FlagRow("Backup Allowed", r.isBackupAllowed, critical = false)
                            FlagRow("Cleartext Traffic", r.usesCleartextTraffic, critical = true)
                            FlagRow("Network Security Config", !r.hasNetworkSecurityConfig, critical = false, invert = true)
                        }
                    }
                    item {
                        AegisCard(accentColor = AegisPurple) {
                            SectionHeader("Signing Certificate", AegisPurple)
                            SignRow("Algorithm", r.signatureInfo.algorithm,
                                if (r.signatureInfo.algorithm.contains("SHA256")) AegisGreen else AegisAmber)
                            SignRow("Debug Signed", if (r.signatureInfo.isDebugSigned) "YES" else "No",
                                if (r.signatureInfo.isDebugSigned) AegisRed else AegisGreen)
                            if (r.signatureInfo.subjectDN.isNotBlank())
                                SignRow("Subject", r.signatureInfo.subjectDN.take(50), TextSecondary)
                            if (r.signatureInfo.validTo.isNotBlank())
                                SignRow("Valid Until", r.signatureInfo.validTo, TextSecondary)
                        }
                    }
                    item {
                        AegisCard(accentColor = AegisPurple) {
                            SectionHeader("All Findings (${r.findings.size})", AegisPurple)
                            r.findings.forEach { f ->
                                FindingRow(title = f.title, detail = f.detail,
                                    severity = f.severity, passed = f.passed)
                            }
                        }
                    }
                }

                1 -> { // Components
                    if (r.exportedComponents.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                EmptyState(icon = Icons.Default.CheckCircle,
                                    title = "No Exported Components",
                                    subtitle = "No unprotected exported components found.")
                            }
                        }
                    } else {
                        items(r.exportedComponents) { comp ->
                            val color = when (comp.type) {
                                "Activity" -> AegisGreen; "Service" -> AegisCyan
                                "Receiver" -> AegisAmber; else -> AegisRed
                            }
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(BgCard).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(comp.type, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(comp.name, color = TextPrimary, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    if (!comp.isProtected) Box(Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(AegisRed.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("UNPROTECTED", color = AegisRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                if (comp.permission.isNotBlank())
                                    Text("Permission: ${comp.permission}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                2 -> { // Permissions
                    item {
                        AegisCard(accentColor = AegisAmber) {
                            SectionHeader("Dangerous Permissions (${r.dangerousPermissions.size})", AegisAmber)
                            if (r.dangerousPermissions.isEmpty()) {
                                Text("No dangerous permissions requested.", color = AegisGreen, fontSize = 12.sp)
                            } else {
                                r.dangerousPermissions.forEach { perm ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Warning, null, tint = AegisAmber, modifier = Modifier.size(13.dp))
                                        Text(perm.removePrefix("android.permission."),
                                            color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> { // Secrets
                    if (r.hardcodedSecrets.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                EmptyState(icon = Icons.Default.CheckCircle,
                                    title = "No Secrets Detected",
                                    subtitle = "No hardcoded credentials or API keys found in accessible APK resources.")
                            }
                        }
                    } else {
                        items(r.hardcodedSecrets) { secret ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(AegisRed.copy(alpha = 0.07f)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, null, tint = AegisRed, modifier = Modifier.size(14.dp))
                                    Text(secret.type, color = AegisRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(secret.preview, color = TextSecondary, fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text("Location: ${secret.location}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FlagRow(label: String, isBad: Boolean, critical: Boolean, invert: Boolean = false) {
    val actuallyBad = if (invert) !isBad else isBad
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (!actuallyBad) Icons.Default.CheckCircle else Icons.Default.Cancel,
                null,
                tint = if (!actuallyBad) AegisGreen else if (critical) AegisRed else AegisAmber,
                modifier = Modifier.size(14.dp)
            )
            Text(
                if (!actuallyBad) "Safe" else if (critical) "Risk" else "Warning",
                color = if (!actuallyBad) AegisGreen else if (critical) AegisRed else AegisAmber,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SignRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
