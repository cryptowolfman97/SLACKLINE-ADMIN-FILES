package com.example.omnicortex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.omnicortex.engine.HttpReconEngine
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.HttpReconViewModel

@Composable
fun HttpReconScreen(onBack: () -> Unit) {
    val vm: HttpReconViewModel = viewModel()
    val state   by vm.state.collectAsState()
    val input   by vm.input.collectAsState()
    val history by vm.history.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(BgCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = AegisAmber)
            }
            Column(Modifier.weight(1f)) {
                Text("HTTP Recon", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Security header & web app analysis", color = TextMuted, fontSize = 11.sp)
            }
            // Consent badge
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(AegisAmber.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) { Text("Authorised Use Only", color = AegisAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Input
            item {
                AegisCard(accentColor = AegisAmber) {
                    SectionHeader("Target URL", AegisAmber)
                    OutlinedTextField(
                        value = input, onValueChange = { vm.setInput(it) },
                        placeholder = { Text("https://example.com", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Language, null, tint = AegisAmber) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { keyboard?.hide(); vm.run() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisAmber, unfocusedBorderColor = BgCardBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AegisAmber
                        )
                    )
                    Button(
                        onClick = { keyboard?.hide(); vm.run() },
                        enabled = input.isNotBlank() && state !is HttpReconViewModel.State.Running,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AegisAmber),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = BgAmoled, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state is HttpReconViewModel.State.Running) "Analysing…" else "Analyse Target",
                            color = BgAmoled, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (val s = state) {
                is HttpReconViewModel.State.Running -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScanningIndicator("Running HTTP analysis…", AegisAmber)
                            Text("Testing headers, CORS, methods,\nredirects and fingerprinting tech",
                                color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                is HttpReconViewModel.State.Error -> item {
                    AegisCard(accentColor = AegisRed) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Error, null, tint = AegisRed, modifier = Modifier.size(20.dp))
                            Text(s.msg, color = AegisRed, fontSize = 13.sp)
                        }
                    }
                }
                is HttpReconViewModel.State.Done -> {
                    item { HttpResultCard(s.result) }
                    item { SecurityHeadersCard(s.result.securityHeaders) }
                    if (s.result.cookieIssues.isNotEmpty()) item { CookieIssuesCard(s.result.cookieIssues) }
                    if (s.result.redirectChain.isNotEmpty()) item { RedirectChainCard(s.result.redirectChain) }
                    item { AllHeadersCard(s.result.headers) }
                }
                else -> {}
            }

            if (history.isNotEmpty() && state is HttpReconViewModel.State.Idle) {
                item { SectionHeader("Recent Scans", AegisAmber) }
                items(history) { r ->
                    val gradeColor = gradeColor(r.grade)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(BgCard).clickable { vm.setInput(r.url); vm.run() }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Language, null, tint = AegisAmber, modifier = Modifier.size(16.dp))
                        Text(r.url, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.grade, color = gradeColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HttpResultCard(r: HttpReconEngine.HttpReconResult) {
    val gc = gradeColor(r.grade)
    AegisCard(accentColor = gc) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.url.take(45), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MicroBadge("HTTP ${r.statusCode}", if (r.statusCode < 400) AegisGreen else AegisRed)
                    MicroBadge("${r.responseTimeMs}ms", AegisCyan)
                }
                if (r.techStack.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(r.techStack) { t -> MicroBadge(t, AegisPurple) }
                    }
                }
            }
            GradeBadge(r.grade, gc)
        }
        if (r.serverBanner.isNotBlank()) {
            HorizontalDivider(color = BgCardBorder)
            Text("Server: ${r.serverBanner}", color = TextMuted, fontSize = 11.sp)
        }
        HorizontalDivider(color = BgCardBorder)
        SectionHeader("Findings", gc)
        r.findings.take(10).forEach { f ->
            FindingRow(title = f.title, detail = f.detail, severity = f.severity, passed = f.passed)
        }
    }
}

@Composable
private fun SecurityHeadersCard(headers: List<HttpReconEngine.SecurityHeaderCheck>) {
    AegisCard(accentColor = AegisAmber) {
        SectionHeader("Security Headers (${headers.count { it.present }}/${headers.size})", AegisAmber)
        headers.forEach { h ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (h.present) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (h.present) AegisGreen else severityColor(h.severity, false),
                    modifier = Modifier.size(15.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(h.header, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    if (!h.present) Text(h.recommendation, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp)
                    else if (h.value.isNotBlank()) Text(h.value.take(60), color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CookieIssuesCard(issues: List<HttpReconEngine.CookieIssue>) {
    AegisCard(accentColor = AegisOrange) {
        SectionHeader("Cookie Issues (${issues.size})", AegisOrange)
        issues.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.cookieName, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Missing: ${c.missingFlags.joinToString(", ")}", color = AegisOrange, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RedirectChainCard(chain: List<HttpReconEngine.RedirectHop>) {
    AegisCard(accentColor = AegisCyan) {
        SectionHeader("Redirect Chain (${chain.size} hops)", AegisCyan)
        chain.forEachIndexed { i, hop ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${i + 1}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(14.dp))
                Text(hop.url.take(40), color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                MicroBadge("${hop.statusCode}", if (hop.statusCode < 400) AegisGreen else AegisRed)
            }
        }
    }
}

@Composable
private fun AllHeadersCard(headers: List<HttpReconEngine.HeaderEntry>) {
    var expanded by remember { mutableStateOf(false) }
    AegisCard(accentColor = TextMuted) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("All Response Headers (${headers.size})", TextSecondary)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
        if (expanded) {
            headers.forEach { h ->
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(h.name, color = AegisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(h.value.take(120), color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GradeBadge(grade: String, color: Color) {
    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center) {
        Text(grade, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MicroBadge(text: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.15f))
        .padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun gradeColor(grade: String) = when (grade) {
    "A+", "A" -> AegisGreen; "B" -> AegisCyan; "C" -> AegisAmber; "D" -> AegisOrange; else -> AegisRed
}
