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
fun GoalsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var tab by remember { mutableStateOf(0) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddFund by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var editFund by remember { mutableStateOf<SinkingFund?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        GoalsBackground(modifier = Modifier.fillMaxWidth().height(180.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Goals & Funds", onBack = onBack) {
                AddChip { if (tab == 0) showAddGoal = true else showAddFund = true }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp)).background(AmoledCard)) {
                listOf("Savings Goals", "Sinking Funds").forEachIndexed { i, label ->
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .then(if (tab == i) Modifier.background(GradientTeal) else Modifier.background(Color.Transparent))
                        .clickable { tab = i }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (tab == i) AmoledBg else AmoledSubtext,
                            fontSize = 13.sp, fontWeight = if (tab == i) FontWeight.ExtraBold else FontWeight.Normal)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tab == 0) {
                    if (data.savingsGoals.isEmpty()) EmptyState(Icons.Default.Star, "No savings goals", "Tap + to add your first goal.")
                    data.savingsGoals.forEach { goal ->
                        val pct = if (goal.target > 0) (goal.saved / goal.target).toFloat().coerceIn(0f, 1f) else 0f
                        val gc = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { Accent }
                        val brush = Brush.linearGradient(listOf(gc, gc.copy(alpha = 0.5f)))
                        AppCard(accentColor = gc) {
                            Box(modifier = Modifier.fillMaxWidth().clickable { editGoal = goal }) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(brush),
                                                contentAlignment = Alignment.Center) { Text("🎯", fontSize = 16.sp) }
                                            Column {
                                                Text(goal.name, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                if (goal.deadline != "No deadline") Text("By ${goal.deadline}", color = AmoledSubtext, fontSize = 11.sp)
                                            }
                                        }
                                        Text("${(pct * 100).toInt()}%", style = androidx.compose.ui.text.TextStyle(
                                            brush = brush, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
                                    }
                                    GradientBar(pct, brush, height = 10.dp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Saved: ${formatAmount(goal.saved, cur)}", color = AmoledSubtext, fontSize = 12.sp)
                                        Text("Target: ${formatAmount(goal.target, cur)}", color = AmoledSubtext, fontSize = 12.sp)
                                    }
                                    if (pct >= 1f) Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(GradientGreen).padding(6.dp), contentAlignment = Alignment.Center) {
                                        Text("🎉 Goal Reached!", color = AmoledBg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (data.sinkingFunds.isEmpty()) EmptyState(Icons.Default.Favorite, "No sinking funds", "Tap + to add a fund.")
                    data.sinkingFunds.forEach { fund ->
                        val pct = if (fund.target > 0) (fund.saved / fund.target).toFloat().coerceIn(0f, 1f) else 0f
                        AppCard(accentColor = Cyan) {
                            Box(modifier = Modifier.fillMaxWidth().clickable { editFund = fund }) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GradientCyan),
                                                contentAlignment = Alignment.Center) { Text("💰", fontSize = 16.sp) }
                                            Column {
                                                Text(fund.name, color = AmoledText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                if (fund.monthlyNeeded > 0) Text("${formatAmount(fund.monthlyNeeded, cur)}/mo", color = AmoledSubtext, fontSize = 11.sp)
                                            }
                                        }
                                        Text("${(pct * 100).toInt()}%", style = androidx.compose.ui.text.TextStyle(
                                            brush = GradientCyan, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
                                    }
                                    GradientBar(pct, GradientCyan, height = 10.dp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Saved: ${formatAmount(fund.saved, cur)}", color = AmoledSubtext, fontSize = 12.sp)
                                        Text("Target: ${formatAmount(fund.target, cur)}", color = AmoledSubtext, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAddGoal) GoalDialog(vm, null) { showAddGoal = false }
    editGoal?.let { GoalDialog(vm, it) { editGoal = null } }
    if (showAddFund) FundDialog(vm, null) { showAddFund = false }
    editFund?.let { FundDialog(vm, it) { editFund = null } }
}

@Composable
private fun GoalDialog(vm: AppViewModel, goal: SavingsGoal?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(goal?.target?.toString() ?: "") }
    var saved by remember { mutableStateOf(goal?.saved?.toString() ?: "0") }
    var deadline by remember { mutableStateOf(goal?.deadline ?: "No deadline") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (goal == null) "New Goal" else "Edit Goal", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Goal Name")
            AppTextField(target, { target = it }, "Target Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(saved, { saved = it }, "Amount Saved", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(deadline, { deadline = it }, "Deadline (YYYY-MM-DD)")
            if (goal != null) TextButton(onClick = { vm.deleteGoal(goal.id); onDismiss() }) { Text("Delete", color = Danger) }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            if (name.isBlank()) return@PrimaryButton
            vm.upsertGoal(SavingsGoal(id = goal?.id ?: uid(), name = name.trim(),
                target = target.toDoubleOrNull() ?: 0.0, saved = saved.toDoubleOrNull() ?: 0.0,
                deadline = deadline.ifBlank { "No deadline" }, color = goal?.color ?: catColor((0..11).random())))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}

@Composable
private fun FundDialog(vm: AppViewModel, fund: SinkingFund?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(fund?.name ?: "") }
    var target by remember { mutableStateOf(fund?.target?.toString() ?: "") }
    var saved by remember { mutableStateOf(fund?.saved?.toString() ?: "0") }
    var monthly by remember { mutableStateOf(fund?.monthlyNeeded?.toString() ?: "0") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = AmoledCard, shape = RoundedCornerShape(20.dp),
        title = { Text(if (fund == null) "New Fund" else "Edit Fund", color = AmoledText, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(name, { name = it }, "Fund Name")
            AppTextField(target, { target = it }, "Target Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(saved, { saved = it }, "Amount Saved", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            AppTextField(monthly, { monthly = it }, "Monthly Contribution", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            if (fund != null) TextButton(onClick = { vm.deleteFund(fund.id); onDismiss() }) { Text("Delete", color = Danger) }
        }},
        confirmButton = { PrimaryButton("Save", onClick = {
            if (name.isBlank()) return@PrimaryButton
            vm.upsertFund(SinkingFund(id = fund?.id ?: uid(), name = name.trim(),
                target = target.toDoubleOrNull() ?: 0.0, saved = saved.toDoubleOrNull() ?: 0.0,
                monthlyNeeded = monthly.toDoubleOrNull() ?: 0.0))
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { GhostButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth()) })
}
