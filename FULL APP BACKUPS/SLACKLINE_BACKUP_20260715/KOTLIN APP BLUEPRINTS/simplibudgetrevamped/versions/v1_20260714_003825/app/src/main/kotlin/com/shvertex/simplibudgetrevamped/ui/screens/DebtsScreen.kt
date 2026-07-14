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
fun DebtsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var showAdd by remember { mutableStateOf(false) }
    var editDebt by remember { mutableStateOf<Debt?>(null) }
    var payDebt by remember { mutableStateOf<Debt?>(null) }
    val totalDebt = data.debts.sumOf { it.balance }

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        DebtsBackground(modifier = Modifier.fillMaxWidth().height(180.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Debt Tracker", onBack = onBack) { AddChip { showAdd = true } }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                HeroCard(brush = GradientRed) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TOTAL DEBT", style = androidx.compose.ui.text.TextStyle(
                            brush = GradientRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp))
                        GradientAmountText(totalDebt, cur, GradientRed, 32)
                        Text("${data.debts.count { it.balance <= 0.0 }} of ${data.debts.size} paid off",
                            color = AmoledSubtext, fontSize = 12.sp)
                    }
                }
                SectionLabel("YOUR DEBTS")
                if (data.debts.isEmpty()) EmptyState(Icons.Default.Info, "No debts tracked", "Add loans, credit cards or money owed.")
                data.debts.sortedByDescending { it.balance }.forEach { debt ->
                    val paid = debt.balance <= 0.0
                    val pct = if (debt.originalBalance > 0)
                        ((debt.originalBalance - debt.balance) / debt.originalBalance).toFloat().coerceIn(0f, 1f) else 0f
                    AppCard(accentColor = if (paid) GreenPos else Danger, cornerRadius = 14.dp) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { editDebt = debt }) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                            .background(if (paid) GradientGreen else GradientRed),
                                            contentAlignment = Alignment.Center) {
                                            Text(if (paid) "✓" else "💳", fontSize = 16.sp)
                                        }
                                        Column {
                                            Text(debt.name, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            if (debt.interestRate > 0)
                                                Text("${debt.interestRate}% APR  •  Min: ${formatAmount(debt.minPayment, cur)}",
                                                    color = AmoledSubtext, fontSize = 11.sp)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (paid) "PAID OFF" else formatAmount(debt.balance, cur),
                                            style = androidx.compose.ui.text.TextStyle(
                                                brush = if (paid) GradientGreen else GradientRed,
                                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
                                        if (!paid) TextButton(onClick = { payDebt = debt },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                            Text("Pay", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (debt.originalBalance > 0) {
                                    GradientBar(pct, if (paid) GradientGreen else GradientBlue, height = 8.dp)
                                    Text("${(pct * 100).toInt()}% paid — ${formatAmount(debt.originalBalance - debt.balance, cur)} cleared",
                                        color = AmoledSubtext, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAdd) DebtDialog(vm, null) { showAdd = false }
    editDebt?.let { DebtDialog(vm, it) { editDebt = null } }
    payDebt?.let { debt ->
        PayDialog(debt, cur, onPay = { vm.recordDebtPayment(debt.id, it); payDebt = null }) { payDebt = null }
    }
}

@Composable
private fun DebtDialog(vm: AppViewModel, debt: Debt?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(debt?.name ?: "") }
    var balance by remember { mutableStateOf(debt?.balance?.toString() ?: "") }
    var original by remember { mutableStateOf(debt?.originalBalance?.toString() ?: "") }
    var minPay by remember { mutableStateOf(debt?.minPayment?.toString() ?: "0") }
    var rate by remember { mutableStateOf(debt?.interestRate?.toString() ?: "0") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (debt == null) "New Debt" else "Edit Debt", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Debt Name")
            AppTextField(balance, { balance = it }, "Current Balance", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(original, { original = it }, "Original Balance", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(minPay, { minPay = it }, "Minimum Payment", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(rate, { rate = it }, "Interest Rate %", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            if (debt != null) TextButton(onClick = { vm.deleteDebt(debt.id); onDismiss() }) { Text("Delete", color = Danger) }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            val bal = balance.toDoubleOrNull() ?: return@PrimaryButton
            if (name.isBlank()) return@PrimaryButton
            vm.upsertDebt(Debt(id = debt?.id ?: uid(), name = name.trim(), balance = bal,
                originalBalance = original.toDoubleOrNull() ?: bal,
                minPayment = minPay.toDoubleOrNull() ?: 0.0, interestRate = rate.toDoubleOrNull() ?: 0.0))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}

@Composable
private fun PayDialog(debt: Debt, cur: String, onPay: (Double) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(debt.minPayment.toString()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Record Payment", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(debt.name, color = AmoledSubtext, fontSize = 13.sp)
            Text("Remaining: ${formatAmount(debt.balance, cur)}", style = androidx.compose.ui.text.TextStyle(
                brush = GradientRed, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            AppTextField(amount, { amount = it }, "Payment Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        }},
        confirmButton = { PrimaryButton("Record", onClick = {
            val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
            if (amt > 0) onPay(amt)
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}
