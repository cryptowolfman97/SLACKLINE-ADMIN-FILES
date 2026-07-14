package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.engine.PortScanEngine
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.PortScanViewModel

@Composable
fun PortScanScreen(onBack: () -> Unit) {
    val vm: PortScanViewModel = viewModel()
    val state       by vm.state.collectAsState()
    val host        by vm.host.collectAsState()
    val mode        by vm.mode.collectAsState()
    val customPorts by vm.customPorts.collectAsState()
    val keyboard    = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().background(BgAmoled)) {
        Row(Modifier.fillMaxWidth().background(BgCard).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = AegisOrange) }
            Column(Modifier.weight(1f)) {
                Text("Port Scanner", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("TCP port scan & service detection", color = TextMuted, fontSize = 11.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AegisOrange.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Authorised Use Only", color = AegisOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                AegisCard(accentColor = AegisOrange) {
                    SectionHeader("Target Host", AegisOrange)
                    OutlinedTextField(
                        value = host, onValueChange = { vm.setHost(it) },
                        placeholder = { Text("192.168.1.1 or example.com", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Router, null, tint = AegisOrange) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisOrange, unfocusedBorderColor = BgCardBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AegisOrange
                        )
                    )
                    // Mode selector
                    SectionHeader("Scan Mode", AegisOrange)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.scanModes.forEach { m ->
                            val selected = mode == m
                            Button(
                                onClick = { vm.setMode(m) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) AegisOrange else BgElevated
                                )
                            ) {
                                Text(m, color = if (selected) BgAmoled else TextSecondary,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (mode == "Custom") {
                        OutlinedTextField(
                            value = customPorts, onValueChange = { vm.setCustomPorts(it) },
                            placeholder = { Text("80,443,8080,3306", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(10.dp), label = { Text("Custom ports (comma separated)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisOrange, unfocusedBorderColor = BgCardBorder,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AegisOrange,
                                focusedLabelColor = AegisOrange, unfocusedLabelColor = TextMuted
                            )
                        )
                    }
                    Button(
                        onClick = { keyboard?.hide(); vm.scan() },
                        enabled = host.isNotBlank() && state !is PortScanViewModel.State.Scanning,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AegisOrange,
                            disabledContainerColor = AegisOrange.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Radar, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state is PortScanViewModel.State.Scanning) "Scanning…" else "Start Scan",
                            color = BgAmoled, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (val s = state) {
                is PortScanViewModel.State.Scanning -> item {
                    AegisCard(accentColor = AegisOrange) {
                        val pct = if (s.total > 0) (s.progress * 100f / s.total).toInt() else 0
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ScanningIndicator("Scanning ports…", AegisOrange)
                                Text("$pct%", color = AegisOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { s.progress.toFloat() / s.total.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AegisOrange, trackColor = BgCardBorder
                            )
                            Text("${s.progress} / ${s.total} ports probed",
                                color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
                is PortScanViewModel.State.Error -> item {
                    AegisCard(accentColor = AegisRed) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Error, null, tint = AegisRed, modifier = Modifier.size(20.dp))
                            Text(s.msg, color = AegisRed, fontSize = 13.sp)
                        }
                    }
                }
                is PortScanViewModel.State.Done -> {
                    item { PortScanResultCard(s.result) }
                    if (s.result.openPorts.isNotEmpty()) {
                        item { SectionHeader("Open Ports (${s.result.openPorts.size})", AegisOrange) }
                        items(s.result.openPorts) { port -> PortEntryRow(port) }
                    } else {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                EmptyState(icon = Icons.Default.Lock,
                                    title = "No Open Ports Found",
                                    subtitle = "All scanned ports appear closed or filtered.")
                            }
                        }
                    }
                }
                else -> {}
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PortScanResultCard(r: PortScanEngine.ScanResult) {
    AegisCard(accentColor = AegisOrange) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.host, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("IP: ${r.resolvedIp}", color = TextMuted, fontSize = 11.sp)
                if (r.osGuess.isNotBlank()) Text("OS: ${r.osGuess}", color = AegisOrange, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${r.openPorts.size}", color = AegisOrange, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("open", color = TextMuted, fontSize = 10.sp)
            }
        }
        StatStrip(listOf(
            Triple("${r.openPorts.size}", "Open", AegisGreen),
            Triple("${r.closedCount}", "Closed", TextMuted),
            Triple("${r.filteredCount}", "Filtered", AegisAmber),
            Triple("${r.scanDurationMs / 1000}s", "Duration", AegisCyan)
        ))
    }
}

@Composable
private fun PortEntryRow(port: PortScanEngine.PortEntry) {
    val isRisky = port.port in listOf(21, 23, 4444, 5900, 6379, 27017, 9200)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (isRisky) AegisRed.copy(alpha = 0.07f) else BgCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
            .background(if (isRisky) AegisRed.copy(alpha = 0.15f) else AegisOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Text("${port.port}", color = if (isRisky) AegisRed else AegisOrange,
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(port.service, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (port.banner.isNotBlank())
                Text(port.banner.take(60), color = TextMuted, fontSize = 10.sp, maxLines = 1)
        }
        if (isRisky) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(AegisRed.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("RISKY", color = AegisRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
