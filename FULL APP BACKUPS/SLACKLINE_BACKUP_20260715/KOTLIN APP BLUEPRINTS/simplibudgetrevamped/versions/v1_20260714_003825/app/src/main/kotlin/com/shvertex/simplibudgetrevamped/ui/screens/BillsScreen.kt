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
fun BillsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var selYm by remember { mutableStateOf(currentMonth()) }
    val months = remember(data) { allMonthsInData(data) }
    var showAdd by remember { mutableStateOf(false) }
    var editBill by remember { mutableStateOf<Bill?>(null) }
    val paidCount = data.bills.count { billPaidInMonth(it, selYm) }
    val totalPaid = data.bills.filter { billPaidInMonth(it, selYm) }.sumOf { it.amount }
    val totalDue = data.bills.filter { !billPaidInMonth(it, selYm) }.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        BillsBackground(modifier = Modifier.fillMaxWidth().height(180.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Bills", onBack = onBack) { AddChip { showAdd = true } }

            // Summary strip
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppCard(accentColor = GreenPos, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("PAID", color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        GradientAmountText(totalPaid, cur, GradientGreen, 18)
                    }
                }
                AppCard(accentColor = Warning, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("REMAINING", color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        GradientAmountText(totalDue, cur, GradientGold, 18)
                    }
                }
            }

            MonthChipRow(months = months, selected = selYm, onSelect = { selYm = it },
                labelOf = { ym -> try { java.time.LocalDate.parse("$ym-01")
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")) } catch (e: Exception) { ym } })

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                if (data.bills.isEmpty()) EmptyState(Icons.Default.Notifications, "No bills yet", "Add recurring bills to track and mark them paid.")
                data.bills.sortedBy { it.dueDay }.forEach { bill ->
                    val paid = billPaidInMonth(bill, selYm)
                    AppCard(accentColor = if (paid) GreenPos else Danger, cornerRadius = 14.dp) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .then(if (paid) Modifier.background(GradientGreen) else Modifier.background(Color(0xFF1A0A0A)))
                                .border(1.dp, if (paid) Color.Transparent else Danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { vm.toggleBillPaid(bill.id, selYm) },
                                contentAlignment = Alignment.Center) {
                                Icon(if (paid) Icons.Default.Check else Icons.Default.Close, null,
                                    tint = if (paid) AmoledBg else Danger, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bill.name, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Due ${bill.dueDay}th  •  ${bill.category}", color = AmoledSubtext, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatAmount(bill.amount, cur), style = androidx.compose.ui.text.TextStyle(
                                    brush = if (paid) GradientGreen else GradientRed,
                                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                                Text(if (paid) "PAID ✓" else "UNPAID",
                                    color = if (paid) GreenPos else Warning, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            IconButton(onClick = { editBill = bill }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, "Edit", tint = AmoledSubtext, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAdd) BillDialog(vm, null) { showAdd = false }
    editBill?.let { BillDialog(vm, it) { editBill = null } }
}

@Composable
fun BillDialog(vm: AppViewModel, bill: Bill?, onDismiss: () -> Unit) {
    val data = vm.data
    val cats = data.budgetCategories.map { it.name }.ifEmpty { listOf("Subscriptions") }
    val accOptions = listOf("None") + data.accounts.filter { it.isActive }.map { it.name }
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amount by remember { mutableStateOf(bill?.amount?.toString() ?: "") }
    var dueDay by remember { mutableStateOf(bill?.dueDay?.toString() ?: "1") }
    var category by remember { mutableStateOf(bill?.category ?: "Subscriptions") }
    var selAcc by remember { mutableStateOf(data.accounts.find { it.id == bill?.accountId }?.name ?: "None") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (bill == null) "New Bill" else "Edit Bill", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Bill Name")
            AppTextField(amount, { amount = it }, "Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(dueDay, { dueDay = it }, "Due Day (1-31)", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            AppDropdown(category, cats, { category = it }, "Category")
            AppDropdown(selAcc, accOptions, { selAcc = it }, "Account")
            if (bill != null) TextButton(onClick = { vm.deleteBill(bill.id); onDismiss() }) { Text("Delete Bill", color = Danger) }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
            if (name.isBlank() || amt <= 0) return@PrimaryButton
            vm.upsertBill(Bill(id = bill?.id ?: uid(), name = name.trim(), amount = amt,
                dueDay = dueDay.toIntOrNull()?.coerceIn(1, 31) ?: 1, category = category,
                accountId = data.accounts.find { it.name == selAcc }?.id ?: "",
                paidMonths = bill?.paidMonths ?: emptyList()))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}
