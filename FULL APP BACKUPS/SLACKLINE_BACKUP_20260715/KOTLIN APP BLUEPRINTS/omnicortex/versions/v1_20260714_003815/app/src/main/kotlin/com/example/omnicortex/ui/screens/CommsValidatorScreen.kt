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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.data.models.TlsResult
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.CommsViewModel

@Composable
fun CommsValidatorScreen(onBack: () -> Unit) {
    val vm: CommsViewModel = viewModel()
    val state   by vm.state.collectAsState()
    val input   by vm.input.collectAsState()
    val history by vm.history.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

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
                Icon(Icons.Default.ArrowBack, null, tint = AegisBlue)
            }
            Column(Modifier.weight(1f)) {
                Text("Comms Validator", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("TLS & certificate inspection", color = TextMuted, fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Input card ────────────────────────────────────────────────────
            item {
                AegisCard(accentColor = AegisBlue) {
                    SectionHeader("Domain or URL", AegisBlue)
                    OutlinedTextField(
                        value         = input,
                        onValueChange = { vm.setInput(it) },
                        placeholder   = { Text("e.g. google.com or https://example.com", color = TextMuted, fontSize = 12.sp) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { keyboard?.hide(); vm.check() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AegisBlue,
                            unfocusedBorderColor = BgCardBorder,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = AegisBlue
                        )
                    )
                    Button(
                        onClick  = { keyboard?.hide(); vm.check() },
                        enabled  = input.isNotBlank() && state !is CommsViewModel.State.Checking,
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AegisBlue),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state is CommsViewModel.State.Checking) "Inspecting…" else "Inspect Domain",
                            color = BgAmoled, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── State ─────────────────────────────────────────────────────────
            when (val s = state) {
                is CommsViewModel.State.Checking -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ScanningIndicator("Inspecting TLS configuration…", AegisBlue)
                    }
                }

                is CommsViewModel.State.Error -> item {
                    AegisCard(accentColor = AegisRed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = AegisRed, modifier = Modifier.size(22.dp))
                            Text(s.msg, color = AegisRed, fontSize = 13.sp)
                        }
                    }
                }

                is CommsViewModel.State.Done -> {
                    item { TlsResultCard(s.result) }
                }

                else -> {}
            }

            // ── History ───────────────────────────────────────────────────────
            if (history.isNotEmpty()) {
                item { SectionHeader("Recent Checks", AegisBlue) }
                items(history) { r ->
                    HistoryRow(r) {
                        vm.setInput(r.domain)
                        vm.check()
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TlsResultCard(r: TlsResult) {
    val gradeColor = when (r.grade) {
        "A+" -> AegisGreen
        "A"  -> AegisGreen
        "B"  -> AegisCyan
        "C"  -> AegisAmber
        else -> AegisRed
    }
    AegisCard(accentColor = gradeColor) {
        // Domain + grade header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(r.domain, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (r.isReachable) "Reachable" else "Unreachable",
                    color = if (r.isReachable) AegisGreen else AegisRed,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(r.grade, color = gradeColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        HorizontalDivider(color = BgCardBorder)

        // Cert details
        if (r.isReachable) {
            InfoRow("TLS Version",   r.tlsVersion,   if (r.tlsVersion == "TLSv1.3") AegisGreen else if (r.tlsVersion == "TLSv1.2") AegisCyan else AegisRed)
            InfoRow("Subject",       r.certSubject,  TextSecondary)
            InfoRow("Issuer",        r.certIssuer,   TextSecondary)
            InfoRow("Valid From",    r.certValidFrom, TextSecondary)
            InfoRow("Expires",       "${r.certValidTo} (${r.daysUntilExpiry}d)",
                when {
                    r.isExpired       -> AegisRed
                    r.daysUntilExpiry < 30 -> AegisAmber
                    else              -> AegisGreen
                })
            InfoRow("HSTS",  if (r.hasHsts) "Enforced" else "Not set", if (r.hasHsts) AegisGreen else AegisAmber)
            InfoRow("Self-signed", if (r.isSelfSigned) "Yes — untrusted" else "No — CA signed", if (r.isSelfSigned) AegisRed else AegisGreen)

            HorizontalDivider(color = BgCardBorder)
            SectionHeader("Findings", gradeColor)
        }

        r.findings.forEach { f ->
            FindingRow(
                title    = f.label,
                detail   = f.detail,
                severity = f.severity,
                passed   = f.passed
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun HistoryRow(r: TlsResult, onClick: () -> Unit) {
    val gradeColor = when (r.grade) {
        "A+", "A" -> AegisGreen; "B" -> AegisCyan; "C" -> AegisAmber; else -> AegisRed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Language, null, tint = AegisBlue, modifier = Modifier.size(18.dp))
        Text(r.domain, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(r.grade, color = gradeColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}
