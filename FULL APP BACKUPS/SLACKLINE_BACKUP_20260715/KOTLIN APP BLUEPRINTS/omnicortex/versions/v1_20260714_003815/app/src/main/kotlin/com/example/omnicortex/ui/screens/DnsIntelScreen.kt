package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.engine.DnsEngine
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.DnsViewModel

@Composable
fun DnsIntelScreen(onBack: () -> Unit) {
    val vm: DnsViewModel = viewModel()
    val state   by vm.state.collectAsState()
    val input   by vm.input.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().background(BgAmoled)) {
        Row(Modifier.fillMaxWidth().background(BgCard).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = AegisCyan) }
            Column(Modifier.weight(1f)) {
                Text("DNS Intelligence", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Records, SPF/DMARC, subdomains, blacklists", color = TextMuted, fontSize = 11.sp)
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                AegisCard(accentColor = AegisCyan) {
                    SectionHeader("Target Domain", AegisCyan)
                    OutlinedTextField(
                        value = input, onValueChange = { vm.setInput(it) },
                        placeholder = { Text("example.com", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Dns, null, tint = AegisCyan) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { keyboard?.hide(); vm.lookup() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan, unfocusedBorderColor = BgCardBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AegisCyan
                        )
                    )
                    Button(
                        onClick = { keyboard?.hide(); vm.lookup() },
                        enabled = input.isNotBlank() && state !is DnsViewModel.State.Running,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan,
                            disabledContainerColor = AegisCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state is DnsViewModel.State.Running) "Querying…" else "Run DNS Lookup",
                            color = BgAmoled, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (val s = state) {
                is DnsViewModel.State.Running -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScanningIndicator("Running DNS reconnaissance…", AegisCyan)
                            Text("Querying records, enumerating subdomains,\nchecking SPF/DMARC and blacklists",
                                color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                is DnsViewModel.State.Error -> item {
                    AegisCard(accentColor = AegisRed) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Error, null, tint = AegisRed, modifier = Modifier.size(20.dp))
                            Text(s.msg, color = AegisRed, fontSize = 13.sp)
                        }
                    }
                }
                is DnsViewModel.State.Done -> {
                    val r = s.result
                    // Summary stats
                    item {
                        StatStrip(listOf(
                            Triple("${r.aRecords.size}", "A Records", AegisCyan),
                            Triple("${r.mxRecords.size}", "MX", AegisGreen),
                            Triple("${r.subdomains.size}", "Subdomains", AegisPurple),
                            Triple(if (r.blacklistHits.isEmpty()) "✓" else "✗", "Blacklist", if (r.blacklistHits.isEmpty()) AegisGreen else AegisRed)
                        ))
                    }

                    // Findings
                    item {
                        AegisCard(accentColor = AegisCyan) {
                            SectionHeader("Security Findings", AegisCyan)
                            r.findings.forEach { f ->
                                FindingRow(title = f.title, detail = f.detail, severity = f.severity, passed = f.passed)
                            }
                        }
                    }

                    // DNS Records
                    item {
                        AegisCard(accentColor = AegisCyan) {
                            SectionHeader("DNS Records", AegisCyan)
                            if (r.aRecords.isNotEmpty()) DnsRecordSection("A Records", r.aRecords, AegisCyan)
                            if (r.mxRecords.isNotEmpty()) DnsRecordSection("MX Records", r.mxRecords, AegisGreen)
                            if (r.nsRecords.isNotEmpty()) DnsRecordSection("NS Records", r.nsRecords, AegisPurple)
                            if (r.txtRecords.isNotEmpty()) DnsRecordSection("TXT Records", r.txtRecords, AegisAmber)
                        }
                    }

                    // Email security
                    if (r.spfAnalysis != null || r.dmarcAnalysis != null) {
                        item {
                            AegisCard(accentColor = AegisAmber) {
                                SectionHeader("Email Security", AegisAmber)
                                r.spfAnalysis?.let { spf ->
                                    Text("SPF", color = AegisAmber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(spf.record.take(100), color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                                    Text("Mechanism: ${spf.allMechanism.ifBlank { "none" }}",
                                        color = if (spf.allMechanism.startsWith("-")) AegisGreen else AegisAmber, fontSize = 11.sp)
                                }
                                r.dmarcAnalysis?.let { dm ->
                                    HorizontalDivider(color = BgCardBorder)
                                    Text("DMARC", color = AegisAmber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(dm.record.take(100), color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                                    Text("Policy: ${dm.policy.uppercase()} — ${dm.percentage}% enforcement",
                                        color = if (dm.policy == "reject") AegisGreen else AegisAmber, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Subdomains
                    if (r.subdomains.isNotEmpty()) {
                        item { SectionHeader("Discovered Subdomains (${r.subdomains.size})", AegisPurple) }
                        items(r.subdomains) { sub ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(BgCard).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Language, null, tint = AegisPurple, modifier = Modifier.size(14.dp))
                                Text(sub.subdomain, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(sub.ip, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // Blacklist hits
                    if (r.blacklistHits.isNotEmpty()) {
                        item {
                            AegisCard(accentColor = AegisRed) {
                                SectionHeader("Blacklist Hits (${r.blacklistHits.size})", AegisRed)
                                r.blacklistHits.forEach { bl ->
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Block, null, tint = AegisRed, modifier = Modifier.size(14.dp))
                                        Text(bl, color = AegisRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Zone transfer result
                    item {
                        AegisCard(accentColor = if (r.zoneTransferAttempt.successful) AegisRed else AegisGreen) {
                            SectionHeader("Zone Transfer (AXFR)", if (r.zoneTransferAttempt.successful) AegisRed else AegisGreen)
                            Text(r.zoneTransferAttempt.detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
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
private fun DnsRecordSection(label: String, records: List<String>, color: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        records.forEach { rec ->
            Text(rec.take(80), color = TextSecondary, fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp))
        }
    }
}
