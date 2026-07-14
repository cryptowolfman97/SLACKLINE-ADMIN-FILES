package com.shvertex.simplibudgetrevamped.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.data.*
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data
    val prefs = vm.prefs
    val context = LocalContext.current

    var showCurrency  by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showPaycheck  by remember { mutableStateOf(false) }
    var showBackup    by remember { mutableStateOf(false) }
    var showRestore   by remember { mutableStateOf(false) }
    var showExport    by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        ScreenHeader(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("PREFERENCES")
            AppCard(accentColor = Accent) {
                SettRow(
                    Icons.Default.ShoppingCart, 
                    "Currency Symbol", 
                    "Current: ${data.settings.currency}",
                    onClick = { showCurrency = true }
                )
                SDivider()
                SettRow(
                    Icons.Default.Notifications, "Budget Alerts", "Alert at category threshold",
                    trailing = {
                        Switch(
                            prefs.alerts,
                            { vm.updatePrefs(prefs.copy(alerts = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AmoledBg, checkedTrackColor = Accent)
                        )
                    }
                )
                SDivider()
                SettRow(
                    Icons.Default.Refresh, "Rollover Budget", "Carry unspent to next month",
                    trailing = {
                        Switch(
                            prefs.rollover,
                            { vm.updatePrefs(prefs.copy(rollover = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AmoledBg, checkedTrackColor = Accent)
                        )
                    }
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("AUTOMATION")
            AppCard(accentColor = Accent2) {
                SettRow(
                    Icons.Default.Refresh, 
                    "Recurring Transactions", 
                    "Auto-recurring entries",
                    onClick = { showRecurring = true }
                )
                SDivider()
                SettRow(
                    Icons.Default.DateRange, 
                    "Paycheck Schedule", 
                    "Set payday dates",
                    onClick = { showPaycheck = true }
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("DATA")
            AppCard(accentColor = Purple) {
                SettRow(
                    Icons.Default.Star, 
                    "Backup Data", 
                    "Save to Downloads/SimpliBudget SHV/Backups",
                    onClick = { showBackup = true }
                )
                SDivider()
                SettRow(
                    Icons.Default.Favorite, 
                    "Restore Backup", 
                    "Load from saved backup",
                    onClick = { showRestore = true }
                )
                SDivider()
                SettRow(
                    Icons.Default.Menu, 
                    "Export CSV", 
                    "Export transactions to CSV",
                    onClick = { showExport = true }
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("STORAGE PATH")
            AppCard(accentColor = Cyan) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Downloads/SimpliBudget SHV/",
                        style = androidx.compose.ui.text.TextStyle(
                            brush = GradientCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                    listOf("simplibudget_data.json", "simplibudget_prefs.json", "Backups/", "Exports/").forEach {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Accent))
                            Text(it, color = AmoledSubtext, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("ABOUT")
            AppCard(accentColor = Pink) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "SimpliBudget SHV",
                        style = androidx.compose.ui.text.TextStyle(
                            brush = GradientPurple, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text("Version 1.0  •  AMOLED Zero-Based Budget", color = AmoledSubtext, fontSize = 12.sp)
                    Text("Kotlin + Jetpack Compose", color = AmoledSubtext, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCurrency) {
        CurrencyDialog(
            current = data.settings.currency,
            onSave = { sym -> vm.updateCurrency(sym); showCurrency = false },
            onDismiss = { showCurrency = false }
        )
    }
    if (showRecurring) {
        RecurringDialog(vm = vm, onDismiss = { showRecurring = false })
    }
    if (showPaycheck) {
        PaycheckDialog(vm = vm, onDismiss = { showPaycheck = false })
    }
    if (showBackup) {
        ConfirmDialog(
            title = "Backup Data",
            message = "Save a backup to Downloads/SimpliBudget SHV/Backups?",
            confirmLabel = "Backup",
            onConfirm = {
                try {
                    val json = fullBackupJson(data, prefs)
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    File(backupsDir(), "backup_$ts.json").writeText(json)
                    Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                showBackup = false
            },
            onDismiss = { showBackup = false }
        )
    }
    if (showRestore) {
        RestoreDialog(
            onRestore = { json ->
                try {
                    vm.restoreFromJson(json)
                    Toast.makeText(context, "Restored successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                showRestore = false
            },
            onDismiss = { showRestore = false }
        )
    }
    if (showExport) {
        ConfirmDialog(
            title = "Export CSV",
            message = "Export this month's transactions to Downloads/SimpliBudget SHV/Exports?",
            confirmLabel = "Export",
            onConfirm = {
                try {
                    exportTransactionsCsv(data, currentMonth())
                    Toast.makeText(context, "CSV exported!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                showExport = false
            },
            onDismiss = { showExport = false }
        )
    }
}

@Composable
private fun SDivider() = HorizontalDivider(
    color = AmoledBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp)
)

@Composable
private fun SettRow(
    icon: ImageVector, title: String, subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(GradientBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AmoledBg, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AmoledSubtext, fontSize = 11.sp)
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Default.ArrowForward, null, tint = AmoledSubtext, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ConfirmDialog(
    title: String, message: String, confirmLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = AmoledSubtext) },
        confirmButton = { PrimaryButton(confirmLabel, onConfirm, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) }
    )
}

@Composable
private fun CurrencyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val symbols = listOf("$", "€", "£", "¥", "₹", "₣", "₩", "₴", "₺", "₫", "฿", "R")
    var sel by remember { mutableStateOf(current) }
    var custom by remember { mutableStateOf(if (current !in symbols) current else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Currency Symbol", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                symbols.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { sym ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(if (sel == sym) Modifier.background(GradientTeal) else Modifier.background(AmoledNavBtn))
                                    .clickable { sel = sym; custom = "" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    sym,
                                    color = if (sel == sym) AmoledBg else AmoledText,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                AppTextField(custom, { custom = it; sel = it }, "Custom symbol")
            }
        },
        confirmButton = {
            PrimaryButton("Save", onClick = {
                val sym = if (custom.isNotBlank()) custom.trim() else sel
                if (sym.isNotBlank()) onSave(sym)
            }, modifier = Modifier.fillMaxWidth())
        },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) }
    )
}

@Composable
private fun RestoreDialog(onRestore: (String) -> Unit, onDismiss: () -> Unit) {
    val files = remember {
        backupsDir().listFiles()?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Restore Backup", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (files.isEmpty()) Text("No backups found.", color = AmoledSubtext)
                else {
                    Text("Select a backup:", color = AmoledSubtext, fontSize = 13.sp)
                    files.take(10).forEach { file ->
                        TextButton(onClick = {
                            try { onRestore(file.readText()) } catch (e: Exception) { onDismiss() }
                        }) { Text(file.name, color = Accent2, fontSize = 13.sp) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) }
    )
}

@Composable
private fun RecurringDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val data = vm.data
    var subScreen by remember { mutableStateOf<RecurringTransaction?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    when {
        addingNew -> RecurringEditorDialog(vm, null) { addingNew = false }
        subScreen != null -> RecurringEditorDialog(vm, subScreen) { subScreen = null }
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring", color = AmoledText, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { addingNew = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.recurring.isEmpty())
                        Text("No recurring transactions. Tap + to add.", color = AmoledSubtext, fontSize = 13.sp)
                    data.recurring.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { subScreen = rec }
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmoledNavBtn)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, color = AmoledText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Day ${rec.day}  •  ${rec.category}  •  ${rec.type}",
                                    color = AmoledSubtext, fontSize = 11.sp
                                )
                            }
                            Text(
                                formatAmount(rec.amount, data.settings.currency),
                                color = if (rec.type == "expense") Danger else GreenPos,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = { PrimaryButton("Done", onDismiss, modifier = Modifier.fillMaxWidth()) }
        )
    }
}

@Composable
private fun RecurringEditorDialog(vm: AppViewModel, rec: RecurringTransaction?, onDismiss: () -> Unit) {
    val cats = vm.data.budgetCategories.map { it.name }.ifEmpty { listOf("Other") }
    var name     by remember { mutableStateOf(rec?.name ?: "") }
    var amount   by remember { mutableStateOf(rec?.amount?.toString() ?: "") }
    var day      by remember { mutableStateOf(rec?.day?.toString() ?: "1") }
    var category by remember { mutableStateOf(rec?.category ?: cats.first()) }
    var type     by remember { mutableStateOf(rec?.type ?: "expense") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (rec == null) "New Recurring" else "Edit Recurring",
                color = AmoledText, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(name, { name = it }, "Name")
                AppTextField(amount, { amount = it }, "Amount",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                AppTextField(day, { day = it }, "Day of Month",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                AppDropdown(type.replaceFirstChar { it.uppercase() }, listOf("Expense", "Income"),
                    { type = it.lowercase() }, "Type")
                AppDropdown(category, cats, { category = it }, "Category")
                if (rec != null) TextButton(onClick = { vm.deleteRecurring(rec.id); onDismiss() }) {
                    Text("Delete", color = Danger)
                }
            }
        },
        confirmButton = {
            PrimaryButton("Save", onClick = {
                val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
                if (name.isBlank()) return@PrimaryButton
                vm.upsertRecurring(
                    RecurringTransaction(
                        id = rec?.id ?: uid(), name = name.trim(),
                        amount = amt, day = day.toIntOrNull()?.coerceIn(1, 28) ?: 1,
                        category = category, type = type
                    )
                )
                onDismiss()
            }, modifier = Modifier.fillMaxWidth())
        },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) }
    )
}

@Composable
private fun PaycheckDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val data = vm.data
    var addingNew by remember { mutableStateOf(false) }
    var addDay    by remember { mutableStateOf("") }
    var addAmount by remember { mutableStateOf("") }

    when {
        addingNew -> AlertDialog(
            onDismissRequest = { addingNew = false; addDay = ""; addAmount = "" },
            containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
            title = { Text("Add Paycheck", color = AmoledText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(addDay, { addDay = it }, "Day of Month (1-28)",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    AppTextField(addAmount, { addAmount = it }, "Amount",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
            },
            confirmButton = {
                PrimaryButton("Add", onClick = {
                    val amt = addAmount.toDoubleOrNull() ?: return@PrimaryButton
                    val d = addDay.toIntOrNull()?.coerceIn(1, 28) ?: return@PrimaryButton
                    vm.upsertPaycheck(Paycheck(day = d, amount = amt))
                    addDay = ""; addAmount = ""; addingNew = false
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GhostButton("Cancel", { addingNew = false; addDay = ""; addAmount = "" },
                    modifier = Modifier.fillMaxWidth())
            }
        )
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paychecks", color = AmoledText, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { addingNew = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (data.paychecks.isEmpty())
                        Text("No paycheck schedules. Tap + to add.", color = AmoledSubtext, fontSize = 13.sp)
                    data.paychecks.forEach { pc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmoledNavBtn)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Day ${pc.day} of month", color = AmoledText, fontSize = 13.sp)
                            }
                            Text(
                                formatAmount(pc.amount, data.settings.currency),
                                color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { vm.deletePaycheck(pc.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, null, tint = Danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { PrimaryButton("Done", onDismiss, modifier = Modifier.fillMaxWidth()) }
        )
    }
}
