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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.data.*
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.theme.*

@Composable
fun ReportsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data; val cur = data.settings.currency
    var selYm by remember { mutableStateOf(currentMonth()) }
    val months = remember(data) { allMonthsInData(data) }
    val last6 = remember { lastNMonths(6) }
    val catSpent = remember(data, selYm) { monthlyExpenseCategoryTotals(data, selYm) }
    val monthIncome = remember(data, selYm) { monthlyIncomeTotal(data, selYm) }
    val totalSpent = catSpent.values.sum()
    val savingsRate = if (monthIncome > 0) ((monthIncome - totalSpent) / monthIncome * 100) else 0.0

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        ReportsBackground(modifier = Modifier.fillMaxWidth().height(160.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Reports", onBack = onBack)
            MonthChipRow(months = months, selected = selYm, onSelect = { selYm = it },
                labelOf = { ym -> try { java.time.LocalDate.parse("$ym-01")
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")) } catch (e: Exception) { ym } })
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("INCOME", monthIncome, cur, Accent, brush = GradientTeal, modifier = Modifier.weight(1f))
                    SummaryCard("EXPENSES", totalSpent, cur, Danger, brush = GradientRed, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("NET", monthIncome - totalSpent, cur,
                        if (monthIncome >= totalSpent) GreenPos else Danger,
                        brush = if (monthIncome >= totalSpent) GradientGreen else GradientRed,
                        modifier = Modifier.weight(1f))
                    AppCard(accentColor = if (savingsRate >= 20) GreenPos else Warning, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SAVINGS RATE", color = AmoledSubtext, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Text("${"%.1f".format(savingsRate)}%", style = androidx.compose.ui.text.TextStyle(
                                brush = if (savingsRate >= 20) GradientGreen else if (savingsRate >= 0) GradientGold else GradientRed,
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                }

                // 6-month chart
                SectionLabel("6-MONTH OVERVIEW")
                AppCard(accentColor = Purple) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val maxVal = last6.maxOfOrNull { ym ->
                            maxOf(monthlyIncomeTotal(data, ym), monthlyExpenseCategoryTotals(data, ym).values.sum())
                        }?.coerceAtLeast(1.0) ?: 1.0
                        Row(modifier = Modifier.fillMaxWidth().height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            last6.forEach { ym ->
                                val inc = monthlyIncomeTotal(data, ym)
                                val exp = monthlyExpenseCategoryTotals(data, ym).values.sum()
                                val label = try { java.time.LocalDate.parse("$ym-01")
                                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM")) } catch (e: Exception) { ym.takeLast(2) }
                                val isSel = ym == selYm
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.width(10.dp)
                                            .height(((inc / maxVal) * 100).dp.coerceAtLeast(2.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isSel) GradientTeal else Brush.linearGradient(listOf(Accent.copy(alpha = 0.4f), Accent.copy(alpha = 0.2f)))))
                                        Box(modifier = Modifier.width(10.dp)
                                            .height(((exp / maxVal) * 100).dp.coerceAtLeast(2.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isSel) GradientRed else Brush.linearGradient(listOf(Danger.copy(alpha = 0.4f), Danger.copy(alpha = 0.2f)))))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, color = if (isSel) Accent else AmoledSubtext, fontSize = 10.sp,
                                        textAlign = TextAlign.Center, fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
                                Text("Income", color = AmoledSubtext, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Danger))
                                Text("Expenses", color = AmoledSubtext, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Category breakdown
                if (catSpent.isNotEmpty()) {
                    SectionLabel("BY CATEGORY")
                    AppCard(accentColor = Cyan) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val total = catSpent.values.sum()
                            val colorMap = data.budgetCategories.associate { it.name to it.color }
                            catSpent.entries.sortedByDescending { it.value }.forEach { (cat, spent) ->
                                val pct = if (total > 0) (spent / total).toFloat() else 0f
                                val planned = data.budgetCategories.find { it.name == cat }?.planned ?: 0.0
                                val c = try { Color(android.graphics.Color.parseColor(colorMap[cat] ?: "#00E5A0")) } catch (e: Exception) { Accent }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ColorDot(colorMap[cat] ?: "#00E5A0", 8.dp)
                                            Text(cat, color = AmoledText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(formatAmount(spent, cur), color = AmoledText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            if (planned > 0) Text("of ${formatAmount(planned, cur)}", color = AmoledSubtext, fontSize = 10.sp)
                                        }
                                    }
                                    GradientBar(pct, Brush.linearGradient(listOf(c, c.copy(alpha = 0.5f))), height = 6.dp)
                                }
                            }
                        }
                    }
                }

                // Top spending days
                SectionLabel("TOP SPENDING DAYS")
                val topDays = remember(data, selYm) {
                    data.transactions.filter { inMonth(it.date, selYm) && it.type == "expense" }
                        .groupBy { it.date }.mapValues { e -> e.value.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(5)
                }
                AppCard(accentColor = Warning) {
                    if (topDays.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions this month.", color = AmoledSubtext, fontSize = 13.sp)
                        }
                    } else {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topDays.forEachIndexed { i, (date, amt) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(GradientGold),
                                            contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", color = AmoledBg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text(date, color = AmoledText, fontSize = 13.sp)
                                    }
                                    Text(formatAmount(amt, cur), style = androidx.compose.ui.text.TextStyle(
                                        brush = GradientRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
