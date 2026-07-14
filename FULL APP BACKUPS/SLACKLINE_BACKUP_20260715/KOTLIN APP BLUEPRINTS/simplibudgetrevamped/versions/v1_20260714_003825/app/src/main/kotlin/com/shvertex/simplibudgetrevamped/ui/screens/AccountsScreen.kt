package com.shvertex.simplibudgetrevamped.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.data.*
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.theme.*

@Composable
fun AccountsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    val balances = remember(data) { accountBalances(data) }
    val netWorth = remember(balances, data) {
        data.accounts.filter { it.includeInNetworth && it.isActive }.sumOf { balances[it.id] ?: 0.0 }
    }
    var showAdd by remember { mutableStateOf(false) }
    var editAcc by remember { mutableStateOf<Account?>(null) }
    var showTransfer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        AccountsBackground(modifier = Modifier.fillMaxWidth().height(180.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Accounts", onBack = onBack) {
                Row {
                    IconButton(onClick = { showTransfer = true }) { Icon(Icons.Default.Refresh, "Transfer", tint = Accent2) }
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add", tint = Accent) }
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))

                // Net worth hero
                HeroCard(brush = if (netWorth >= 0) GradientTeal else GradientRed) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("NET WORTH", style = androidx.compose.ui.text.TextStyle(
                            brush = if (netWorth >= 0) GradientTeal else GradientRed,
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp))
                        GradientAmountText(netWorth, cur, if (netWorth >= 0) GradientTeal else GradientRed, 32)
                        Text("${data.accounts.count { it.isActive }} active accounts", color = AmoledSubtext, fontSize = 12.sp)
                    }
                }

                SectionLabel("YOUR ACCOUNTS")
                if (data.accounts.isEmpty()) EmptyState(Icons.Default.Star, "No accounts yet", "Add your bank accounts, wallets and more.")

                data.accounts.forEach { acc ->
                    val bal = balances[acc.id] ?: 0.0
                    val accBrush = when (acc.type) {
                        "Bank" -> GradientTeal; "Savings" -> GradientGreen
                        "Credit Card" -> GradientRed; "E-wallet" -> GradientBlue
                        "Loan" -> GradientRed; else -> GradientPurple
                    }
                    AppCard(accentColor = if (bal >= 0) Accent else Danger, cornerRadius = 14.dp) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { editAcc = acc }) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accBrush),
                                    contentAlignment = Alignment.Center) {
                                    Icon(accountIcon(acc.type), null, tint = AmoledBg, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.name, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(acc.type, color = AmoledSubtext, fontSize = 12.sp)
                                    if (!acc.isActive) Text("Inactive", color = Warning, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    GradientAmountText(bal, cur, if (bal >= 0) accBrush else GradientRed, 16)
                                    if (acc.includeInNetworth) Text("In net worth", color = AmoledSubtext, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                val recent = data.accountTransfers.sortedByDescending { it.date }.take(5)
                if (recent.isNotEmpty()) {
                    SectionLabel("RECENT TRANSFERS")
                    recent.forEach { tr ->
                        val from = data.accounts.find { it.id == tr.fromAccountId }?.name ?: "?"
                        val to = data.accounts.find { it.id == tr.toAccountId }?.name ?: "?"
                        AppCard(accentColor = Accent2, cornerRadius = 12.dp) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GradientBlue),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Refresh, null, tint = AmoledBg, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("$from → $to", color = AmoledText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(tr.date, color = AmoledSubtext, fontSize = 11.sp)
                                }
                                Text(formatAmount(tr.amount, cur), color = Accent2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAdd) AccountEditorDialog(vm, null) { showAdd = false }
    editAcc?.let { AccountEditorDialog(vm, it) { editAcc = null } }
    if (showTransfer) TransferDialog(vm) { showTransfer = false }
}

fun accountIcon(type: String) = when (type) {
    "Bank" -> Icons.Default.Star; "Savings" -> Icons.Default.Favorite
    "Cash" -> Icons.Default.ShoppingCart; "Credit Card" -> Icons.Default.Lock
    "Loan" -> Icons.Default.Info; else -> Icons.Default.AccountBox
}

@Composable
fun AccountEditorDialog(vm: AppViewModel, acc: Account?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(acc?.name ?: "") }
    var type by remember { mutableStateOf(acc?.type ?: "Bank") }
    var opening by remember { mutableStateOf(acc?.openingBalance?.toString() ?: "0") }
    var inNW by remember { mutableStateOf(acc?.includeInNetworth ?: true) }
    var active by remember { mutableStateOf(acc?.isActive ?: true) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (acc == null) "New Account" else "Edit Account", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Account Name")
            AppDropdown(type, ACCOUNT_TYPES, { type = it }, "Type")
            AppTextField(opening, { opening = it }, "Opening Balance", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("Include in Net Worth", color = AmoledText, fontSize = 13.sp)
                Switch(checked = inNW, onCheckedChange = { inNW = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledBg, checkedTrackColor = Accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("Active", color = AmoledText, fontSize = 13.sp)
                Switch(checked = active, onCheckedChange = { active = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledBg, checkedTrackColor = Accent))
            }
            if (acc != null) TextButton(onClick = { vm.deleteAccount(acc.id); onDismiss() }) {
                Text("Delete Account", color = Danger, fontSize = 13.sp)
            }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            if (name.isBlank()) return@PrimaryButton
            vm.upsertAccount(Account(id = acc?.id ?: uid(), name = name.trim(), type = type,
                openingBalance = opening.toDoubleOrNull() ?: 0.0, includeInNetworth = inNW, isActive = active))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}

@Composable
fun TransferDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val accounts = vm.data.accounts.filter { it.isActive }
    if (accounts.size < 2) {
        AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
            title = { Text("Transfer", color = AmoledText, fontWeight = FontWeight.Bold) },
            text = { Text("Need at least 2 active accounts.", color = AmoledSubtext) },
            confirmButton = { PrimaryButton("OK", onDismiss) }); return
    }
    val names = accounts.map { it.name }
    var from by remember { mutableStateOf(names[0]) }; var to by remember { mutableStateOf(names[1]) }
    var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Transfer", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppDropdown(from, names, { from = it }, "From Account")
            AppDropdown(to, names, { to = it }, "To Account")
            AppTextField(amount, { amount = it }, "Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(note, { note = it }, "Note (optional)")
        }},
        confirmButton = { PrimaryButton("Transfer", onClick = {
            val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
            if (amt <= 0 || from == to) return@PrimaryButton
            val fromId = accounts.find { it.name == from }?.id ?: return@PrimaryButton
            val toId = accounts.find { it.name == to }?.id ?: return@PrimaryButton
            vm.addTransfer(AccountTransfer(fromAccountId = fromId, toAccountId = toId, amount = amt, date = today(), note = note.trim()))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}
