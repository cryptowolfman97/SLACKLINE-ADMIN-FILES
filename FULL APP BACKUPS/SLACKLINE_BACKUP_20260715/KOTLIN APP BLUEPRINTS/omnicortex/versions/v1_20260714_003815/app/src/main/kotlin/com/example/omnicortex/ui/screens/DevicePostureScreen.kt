package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.data.models.PostureCategory
import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.engine.PostureAuditResult
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.PostureViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DevicePostureScreen(onBack: () -> Unit) {
    val vm: PostureViewModel = viewModel()
    val state           by vm.state.collectAsState()
    val selectedCat     by vm.selectedCategory.collectAsState()
    val lastScan        by vm.lastScanTime.collectAsState(initial = 0L)

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
                Icon(Icons.Default.ArrowBack, null, tint = AegisGreen)
            }
            Column(Modifier.weight(1f)) {
                Text("Device Posture", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (lastScan == 0L) "Never scanned"
                    else "Last scan: ${formatTime(lastScan)}",
                    color = TextMuted, fontSize = 11.sp
                )
            }
            // Scan button
            Button(
                onClick  = { vm.runScan() },
                enabled  = state !is PostureViewModel.State.Scanning,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = AegisGreen,
                    disabledContainerColor = AegisGreen.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    if (state is PostureViewModel.State.Scanning) "Scanning…" else "Scan Now",
                    color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
        }

        when (val s = state) {
            is PostureViewModel.State.Idle -> {
                // First launch prompt
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon       = Icons.Default.Shield,
                        title      = "Run Your First Audit",
                        subtitle   = "SHV Omni-Cortex will analyse 20+ security indicators\nacross 5 categories of your device.",
                        actionLabel = "Start Scan",
                        onAction   = { vm.runScan() }
                    )
                }
            }

            is PostureViewModel.State.Scanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ScanningIndicator("Analysing device security…", AegisGreen)
                        Text(
                            "Checking OS integrity, apps,\nnetwork hygiene and more",
                            color = TextMuted, fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            is PostureViewModel.State.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = AegisRed, modifier = Modifier.size(40.dp))
                        Text("Scan failed", color = AegisRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(s.msg, color = TextSecondary, fontSize = 12.sp)
                        Button(onClick = { vm.runScan() }, colors = ButtonDefaults.buttonColors(containerColor = AegisGreen)) {
                            Text("Retry", color = BgAmoled, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is PostureViewModel.State.Done -> {
                PostureResultView(
                    result      = s.result,
                    selectedCat = selectedCat,
                    onCatSelect = { vm.selectCategory(it) },
                    findings    = vm.filteredFindings(s.result)
                )
            }
        }
    }
}

@Composable
private fun PostureResultView(
    result: PostureAuditResult,
    selectedCat: PostureCategory?,
    onCatSelect: (PostureCategory?) -> Unit,
    findings: List<com.example.omnicortex.data.models.PostureFinding>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Score card ────────────────────────────────────────────────────────
        item {
            AegisCard(accentColor = scoreAccentColor(result.score)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AegisScoreRing(score = result.score, ringSize = 160.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradeBox(result.grade, scoreAccentColor(result.score))
                        Spacer(Modifier.height(4.dp))
                        MiniStat("${result.criticalCount}", "Critical", AegisRed)
                        MiniStat("${result.highCount}",     "High",     AegisOrange)
                        MiniStat("${result.mediumCount}",   "Medium",   AegisAmber)
                        MiniStat("${result.passedCount}",   "Passed",   AegisGreen)
                    }
                }
            }
        }

        // ── Stat strip ────────────────────────────────────────────────────────
        item {
            StatStrip(
                listOf(
                    Triple("${result.totalCount}", "Checks", TextSecondary),
                    Triple("${result.passedCount}", "Passed", AegisGreen),
                    Triple("${result.criticalCount + result.highCount}", "Failed", AegisRed)
                )
            )
        }

        // ── Category filter chips ─────────────────────────────────────────────
        item {
            SectionHeader("Filter by Category")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip("All", selectedCat == null, AegisGreen) { onCatSelect(null) }
                }
                items(PostureCategory.values().toList()) { cat ->
                    CategoryChip(
                        label    = catLabel(cat),
                        selected = selectedCat == cat,
                        color    = catColor(cat),
                        onClick  = { onCatSelect(if (selectedCat == cat) null else cat) }
                    )
                }
            }
        }

        // ── Findings list ─────────────────────────────────────────────────────
        item { SectionHeader("Security Findings") }

        items(findings.sortedWith(
            compareBy<com.example.omnicortex.data.models.PostureFinding> { it.passed }
                .thenBy { it.severity.ordinal }
        )) { finding ->
            FindingRow(
                title     = finding.title,
                detail    = finding.detail,
                severity  = finding.severity,
                passed    = finding.passed,
                fixAdvice = finding.fixAdvice
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────
@Composable
private fun GradeBox(grade: String, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(grade, color = color, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else BgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) color else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun catLabel(cat: PostureCategory) = when (cat) {
    PostureCategory.LOCK_ACCESS         -> "Lock & Access"
    PostureCategory.DEVELOPER_EXPOSURE  -> "Dev Exposure"
    PostureCategory.APP_ECOSYSTEM       -> "App Ecosystem"
    PostureCategory.OS_INTEGRITY        -> "OS Integrity"
    PostureCategory.NETWORK_HYGIENE     -> "Network"
}

private fun catColor(cat: PostureCategory) = when (cat) {
    PostureCategory.LOCK_ACCESS         -> AegisGreen
    PostureCategory.DEVELOPER_EXPOSURE  -> AegisAmber
    PostureCategory.APP_ECOSYSTEM       -> AegisPurple
    PostureCategory.OS_INTEGRITY        -> AegisCyan
    PostureCategory.NETWORK_HYGIENE     -> AegisBlue
}

private fun scoreAccentColor(score: Int) = when {
    score >= 75 -> AegisGreen
    score >= 50 -> AegisAmber
    else        -> AegisRed
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(ts))
