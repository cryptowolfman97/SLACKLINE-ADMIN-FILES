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
fun TransactionsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var selYm by remember { mutableStateOf(currentMonth()) }
    val months = remember(data) { allMonthsInData(data) }
    var search by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("All") }
    var filterCat by remember { mutableStateOf("All") }
    var showAdd by remember { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<Transaction?>(null) }
    val catNames = remember(data) { listOf("All") + data.budgetCategories.map { it.name } }
    val txList = remember(data, selYm, search, filterType, filterCat) {
        data.transactions
            .filter { inMonth(it.date, selYm) }
            .filter { if (filterType != "All") it.type == filterType.lowercase() else true }
            .filter { if (filterCat != "All") it.category == filterCat else true }
            .filter { if (search.isNotBlank()) it.name.contains(search, true) || it.category.contains(search, true) else true }
            .sortedByDescending { it.date }
    }
    val totalExp = txList.filter { it.type == "expense" }.sumOf { it.amount }
    val totalInc = txList.filter { it.type == "income" }.sumOf { it.amount }

    DisposableEffect(Unit) {
        onDispose {
            showAdd = false
            editTx = null
        }
    }

    // Wrapped in Box to allow the background image layer
    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Transactions", subtitle = "${txList.size} entries", onBack = onBack) { AddChip { showAdd = true } }

            // Summary strip
            Row(modifier = Modifier.fillMaxWidth().background(AmoledSurface)
                .padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("OUT", color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text(formatAmount(totalExp, cur), style = androidx.compose.ui.text.TextStyle(
                        brush = GradientRed, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("IN", color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text(formatAmount(totalInc, cur), style = androidx.compose.ui.text.TextStyle(
                        brush = GradientGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                }
            }

            MonthChipRow(months = months, selected = selYm, onSelect = { selYm = it },
                labelOf = { ym -> try { java.time.LocalDate.parse("$ym-01")
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")) } catch (e: Exception) { ym } })

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AppTextField(search, { search = it }, "Search transactions", modifier = Modifier.weight(1f),
                    trailingIcon = { Icon(Icons.Default.Search, null, tint = AmoledSubtext) })
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All","Expense","Income").forEach { t ->
                    TxChip(t, t == filterType) { filterType = t }
                }
                catNames.take(6).forEach { c -> TxChip(c, c == filterCat) { filterCat = c } }
            }

            if (txList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Default.List, "No transactions", "Add your first entry above.")
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.height(4.dp))
                    txList.forEach { tx ->
                        TransactionRow(tx.name, tx.category, tx.date, tx.amount, tx.type, cur,
                            onEdit = { editTx = tx }, onDelete = { vm.deleteTransaction(tx.id) })
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (showAdd) TxEditorDialog(vm, null) { showAdd = false }
        editTx?.let { TxEditorDialog(vm, it) { editTx = null } }
    }
}

@Composable
private fun TxChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .then(if (selected) Modifier.background(GradientTeal) else Modifier.background(AmoledNavBtn))
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(label, color = if (selected) AmoledBg else AmoledSubtext,
            fontSize = 12.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
    }
}

@Composable
fun TxEditorDialog(vm: AppViewModel, tx: Transaction?, onDismiss: () -> Unit) {
    val data = vm.data
    val cats = data.budgetCategories.map { it.name }.ifEmpty { listOf("Other") }
    val accOptions = listOf("None") + data.accounts.filter { it.isActive }.map { it.name }
    var name by remember { mutableStateOf(tx?.name ?: "") }
    var amount by remember { mutableStateOf(tx?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(tx?.type ?: "expense") }
    var category by remember { mutableStateOf(tx?.category ?: cats.first()) }
    var date by remember { mutableStateOf(tx?.date ?: today()) }
    var note by remember { mutableStateOf(tx?.note ?: "") }
    var selAcc by remember { mutableStateOf(data.accounts.find { it.id == tx?.accountId }?.name ?: "None") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (tx == null) "New Transaction" else "Edit Transaction", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Name / Payee")
            AppTextField(amount, { amount = it }, "Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppDropdown(type.replaceFirstChar { it.uppercase() }, listOf("Expense","Income"), { type = it.lowercase() }, "Type")
            AppDropdown(category, cats, { category = it }, "Category")
            AppDropdown(selAcc, accOptions, { selAcc = it }, "Account")
            AppTextField(date, { date = it }, "Date (YYYY-MM-DD)")
            AppTextField(note, { note = it }, "Note (optional)")
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
            if (name.isBlank() || amt <= 0) return@PrimaryButton
            vm.upsertTransaction(Transaction(id = tx?.id ?: uid(), name = name.trim(), amount = amt,
                type = type, category = category, date = date.trim(), note = note.trim(),
                accountId = data.accounts.find { it.name == selAcc }?.id ?: "",
                recurringId = tx?.recurringId ?: ""))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}