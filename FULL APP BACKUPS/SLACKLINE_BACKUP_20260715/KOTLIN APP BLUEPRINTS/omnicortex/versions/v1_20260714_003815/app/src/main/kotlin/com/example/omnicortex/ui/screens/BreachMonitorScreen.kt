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
import com.example.omnicortex.data.models.BreachRecord
import com.example.omnicortex.ui.components.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.BreachViewModel

@Composable
fun BreachMonitorScreen(onBack: () -> Unit) {
    val vm: BreachViewModel = viewModel()
    val checkState  by vm.checkState.collectAsState()
    val watchlist   by vm.watchlist.collectAsState()
    val breachRecs  by vm.breachRecords.collectAsState(initial = emptyList())
    val newInput    by vm.newInput.collectAsState()
    val snack       = remember { SnackbarHostState() }
    val keyboard    = LocalSoftwareKeyboardController.current

    Scaffold(
        snackbarHost  = { SnackbarHost(snack) },
        containerColor = BgAmoled
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BgAmoled)
                
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
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
                    Text("Breach Monitor", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Email & domain watchlist", color = TextMuted, fontSize = 11.sp)
                }
                if (watchlist.isNotEmpty()) {
                    Button(
                        onClick  = { vm.checkAll(); vm.markAllRead() },
                        enabled  = checkState !is BreachViewModel.CheckState.Checking,
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AegisPurple,
                            disabledContainerColor = AegisPurple.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Check All", color = BgAmoled, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── API key notice ─────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisAmber.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, tint = AegisAmber, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                        Text(
                            "HIBP API key not yet configured. Add your watchlist — live breach checks will activate once the key is added in a future update.",
                            color = AegisAmber, fontSize = 11.sp, lineHeight = 15.sp
                        )
                    }
                }

                // ── Add item ───────────────────────────────────────────────────
                item {
                    AegisCard(accentColor = AegisPurple) {
                        SectionHeader("Add to Watchlist", AegisPurple)
                        OutlinedTextField(
                            value         = newInput,
                            onValueChange = { vm.setNewInput(it) },
                            placeholder   = { Text("email@example.com or domain.com", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon   = { Icon(Icons.Default.Add, null, tint = AegisPurple) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            shape         = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboard?.hide(); vm.addToWatchlist(newInput) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AegisPurple,
                                unfocusedBorderColor = BgCardBorder,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary,
                                cursorColor          = AegisPurple
                            )
                        )
                        Button(
                            onClick  = { keyboard?.hide(); vm.addToWatchlist(newInput) },
                            enabled  = newInput.isNotBlank(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = AegisPurple),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Add to Watchlist", color = BgAmoled, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Watchlist ──────────────────────────────────────────────────
                if (watchlist.isNotEmpty()) {
                    item { SectionHeader("Watching", AegisPurple) }
                    items(watchlist) { item ->
                        val itemBreaches = breachRecs.filter { it.watchItem == item }
                        val hasBreaches  = itemBreaches.isNotEmpty()
                        val isChecking   = checkState is BreachViewModel.CheckState.Checking &&
                                (checkState as BreachViewModel.CheckState.Checking).item == item

                        WatchlistRow(
                            item       = item,
                            breachCount = itemBreaches.size,
                            isChecking = isChecking,
                            onRemove   = { vm.removeFromWatchlist(item) }
                        )
                    }
                } else {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon     = Icons.Default.Security,
                                title    = "Nothing Monitored Yet",
                                subtitle = "Add email addresses or domains\nto monitor for data breaches."
                            )
                        }
                    }
                }

                // ── Breach records ─────────────────────────────────────────────
                if (breachRecs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("Breach History", AegisPurple)
                            Text(
                                "${breachRecs.size} record(s)",
                                color = TextMuted, fontSize = 11.sp
                            )
                        }
                    }
                    items(breachRecs, key = { it.id }) { rec ->
                        BreachRecordCard(rec)
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WatchlistRow(
    item: String,
    breachCount: Int,
    isChecking: Boolean,
    onRemove: () -> Unit
) {
    val isEmail = item.contains("@")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AegisPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = AegisPurple,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    if (isEmail) Icons.Default.Email else Icons.Default.Language,
                    null, tint = AegisPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(item, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (isEmail) "Email address" else "Domain",
                color = TextMuted, fontSize = 10.sp
            )
        }
        if (breachCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AegisRed.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("$breachCount breach${if (breachCount > 1) "es" else ""}",
                    color = AegisRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AegisGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Clean", color = AegisGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BreachRecordCard(rec: BreachRecord) {
    var expanded by remember { mutableStateOf(false) }
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AegisRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = AegisRed, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(rec.breachName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(rec.watchItem, color = TextMuted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rec.breachDate, color = TextMuted, fontSize = 10.sp)
                if (rec.isNew) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AegisRed.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("NEW", color = AegisRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
        if (expanded && rec.dataClasses.isNotBlank()) {
            HorizontalDivider(color = BgCardBorder, modifier = Modifier.padding(horizontal = 12.dp))
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Exposed Data", color = AegisRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(rec.dataClasses, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}
