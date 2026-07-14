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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.data.*
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.theme.*

@Composable
fun BudgetScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var selYm by remember { mutableStateOf(currentMonth()) }
    val months = remember(data) { allMonthsInData(data) }
    val monthIncome = remember(data, selYm) { monthlyIncomeTotal(data, selYm) }
    val catSpent = remember(data, selYm) { monthlyExpenseCategoryTotals(data, selYm) }
    val totalPlanned = data.budgetCategories.sumOf { it.planned }
    var showAddCat by remember { mutableStateOf(false) }
    var showAddIncome by remember { mutableStateOf(false) }
    var editCat by remember { mutableStateOf<BudgetCategory?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp).alpha(1.0f))
        
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Budget",
                subtitle = "Unassigned: ${formatAmount(monthIncome - totalPlanned, cur)}",
                onBack = onBack)
            MonthChipRow(months = months, selected = selYm, onSelect = { selYm = it },
                labelOf = { ym -> try { java.time.LocalDate.parse("$ym-01")
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")) } catch (e: Exception) { ym } })
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                // Income card
                AppCard(accentColor = Accent) {
                    // FIX: Added .fillMaxWidth() here to push the Add button to the right edge
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("MONTHLY INCOME", color = AmoledSubtext, fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            GradientAmountText(monthIncome, cur, GradientTeal, 24)
                        }
                        AddChip { showAddIncome = true }
                    }
                }
                SectionLabel("BUDGET CATEGORIES")
                data.budgetCategories.forEach { cat ->
                    val spent = catSpent[cat.name] ?: 0.0
                    val rem = cat.planned - spent
                    val pct = if (cat.planned > 0) (spent / cat.planned).toFloat() else 0f
                    val catColor = try { Color(android.graphics.Color.parseColor(cat.color)) } catch (e: Exception) { Accent }
                    val brush = when { pct >= 1f -> GradientRed; pct >= 0.8f -> GradientGold
                        else -> Brush.linearGradient(listOf(catColor, catColor.copy(alpha = 0.6f))) }
                    AppCard(accentColor = catColor, cornerRadius = 14.dp) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { editCat = cat }) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ColorDot(cat.color, 10.dp)
                                        Text(cat.name, color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        if (pct >= 1f) Text("⚠", fontSize = 13.sp)
                                    }
                                    Text(formatAmount(rem, cur),
                                        color = if (rem >= 0) GreenPos else Danger,
                                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                GradientBar(pct, brush)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Planned ${formatAmount(cat.planned, cur)}", color = AmoledSubtext, fontSize = 11.sp)
                                    Text("Spent ${formatAmount(spent, cur)}", color = AmoledSubtext, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { showAddCat = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.Add, null, tint = Accent2, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Category", style = androidx.compose.ui.text.TextStyle(
                        brush = GradientBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAddIncome) AddIncomeDialog(vm) { showAddIncome = false }
    if (showAddCat) AddCatDialog(onDismiss = { showAddCat = false }, onSave = { vm.addCategory(it); showAddCat = false })
    editCat?.let { EditCatDialog(it, onDismiss = { editCat = null },
        onSave = { vm.updateCategory(it); editCat = null },
        onDelete = { vm.deleteCategory(it.name); editCat = null }) }
}

@Composable
fun AddIncomeDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val data = vm.data
    var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selAcc by remember { mutableStateOf("None") }
    val accOptions = listOf("None") + data.accounts.filter { it.isActive }.map { it.name }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Add Income", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Source (e.g. Salary)")
            AppTextField(amount, { amount = it }, "Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(note, { note = it }, "Note (optional)")
            AppDropdown(selAcc, accOptions, { selAcc = it }, "Account")
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            val amt = amount.toDoubleOrNull() ?: return@PrimaryButton
            if (name.isBlank() || amt <= 0) return@PrimaryButton
            vm.upsertIncome(IncomeEntry(name = name.trim(), amount = amt, date = today(), note = note.trim(),
                accountId = data.accounts.find { it.name == selAcc }?.id ?: ""))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}

@Composable
private fun AddCatDialog(onDismiss: () -> Unit, onSave: (BudgetCategory) -> Unit) {
    var name by remember { mutableStateOf("") }; var planned by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text("New Category", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Category Name")
            AppTextField(planned, { planned = it }, "Planned Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        }},
        confirmButton = { PrimaryButton("Add", onClick = {
            if (name.isBlank()) return@PrimaryButton
            onSave(BudgetCategory(name = name.trim(), planned = planned.toDoubleOrNull() ?: 0.0, color = catColor(0)))
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}

@Composable
private fun EditCatDialog(cat: BudgetCategory, onDismiss: () -> Unit, onSave: (BudgetCategory) -> Unit, onDelete: (BudgetCategory) -> Unit) {
    var planned by remember { mutableStateOf(cat.planned.toString()) }
    var alertPct by remember { mutableStateOf(cat.alertPct.toString()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(cat.name, color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(planned, { planned = it }, "Planned Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(alertPct, { alertPct = it }, "Alert at %", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            TextButton(onClick = { onDelete(cat) }) { Text("Delete Category", color = Danger, fontSize = 13.sp) }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            onSave(cat.copy(planned = planned.toDoubleOrNull() ?: 0.0, alertPct = alertPct.toIntOrNull() ?: 80))
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}
