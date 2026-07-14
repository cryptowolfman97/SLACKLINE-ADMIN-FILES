package com.shvertex.simplibudgetrevamped.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.simplibudgetrevamped.data.*
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.theme.*
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(vm: AppViewModel, onNavigate: (String) -> Unit) {
    val data = vm.data
    val cur = data.settings.currency
    var selYm by remember { mutableStateOf(currentMonth()) }
    val months = remember(data) { allMonthsInData(data) }
    LaunchedEffect(Unit) { vm.processRecurring() }

    val monthIncome  = remember(data, selYm) { monthlyIncomeTotal(data, selYm) }
    val catSpent     = remember(data, selYm) { monthlyExpenseCategoryTotals(data, selYm) }
    val totalSpent   = catSpent.values.sum()
    val totalPlanned = data.budgetCategories.sumOf { it.planned }
    val remaining    = monthIncome - totalSpent
    val unassigned   = monthIncome - totalPlanned

    val breathingTransition = rememberInfiniteTransition(label = "global_breathing")
    val ambientAlpha by breathingTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(AmoledBg)) {
        BudgetBackground(modifier = Modifier.fillMaxWidth().height(3000.dp))
        // Background art now fills the ENTIRE screen to match the Budge style
        EnhancedHomeBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color.Transparent)
                .padding(start = 20.dp, end = 14.dp, top = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SIMPLI-BUDGET", style = androidx.compose.ui.text.TextStyle(
                        brush = GradientTeal, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold))
                    Text("By SHVERTEX TECH", color = AmoledSubtext, fontSize = 12.sp)
                }
                IconButton(onClick = { onNavigate("settings") }) {
                    Icon(Icons.Default.Settings, "Settings", tint = Accent2)
                }
            }

            MonthChipRow(months = months, selected = selYm, onSelect = { selYm = it },
                labelOf = { ym -> try { java.time.LocalDate.parse("$ym-01")
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")) } catch (e: Exception) { ym } })

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(4.dp))

                AnimatedHomeHeroCard(monthIncome, totalSpent, totalPlanned, remaining, cur, selYm)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("INCOME", monthIncome, cur, Accent, brush = GradientTeal, modifier = Modifier.weight(1f))
                    SummaryCard("SPENT", totalSpent, cur, Danger, brush = GradientRed, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("LEFT", remaining, cur,
                        if (remaining >= 0) GreenPos else Danger,
                        brush = if (remaining >= 0) GradientGreen else GradientRed,
                        modifier = Modifier.weight(1f))
                    SummaryCard("UNASSIGNED", unassigned, cur,
                        if (unassigned >= 0) Accent2 else Warning,
                        brush = if (unassigned >= 0) GradientBlue else GradientGold,
                        modifier = Modifier.weight(1f))
                }

                if (totalPlanned > 0) {
                    val pct = (totalSpent / totalPlanned).toFloat().coerceIn(0f, 1f)
                    val brush = if (pct >= 1f) GradientRed else if (pct >= 0.8f) GradientGold else GradientGreen
                    val budgetAccentColor = if (pct >= 0.8f) Danger else GreenPos
                    
                    AppCard(accentColor = budgetAccentColor.copy(alpha = ambientAlpha)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Budget Health", color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${(pct * 100).toInt()}% used", style = androidx.compose.ui.text.TextStyle(
                                    brush = brush, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                            }
                            GradientBar(pct, brush)
                            Text("${formatAmount(totalSpent, cur)} of ${formatAmount(totalPlanned, cur)} planned",
                                color = AmoledSubtext, fontSize = 11.sp)
                        }
                    }
                }

                if (catSpent.isNotEmpty()) HomeSpendingDonut(catSpent, data, cur)

                SectionLabel("RECENT TRANSACTIONS")
                val recent = remember(data, selYm) {
                    data.transactions.filter { inMonth(it.date, selYm) }.sortedByDescending { it.date }.take(5)
                }
                if (recent.isEmpty()) {
                    AppCard { EmptyState(Icons.Default.List, "No transactions yet", "Log your first expense to get started.") }
                } else {
                    recent.forEach { tx ->
                        TransactionRow(tx.name, tx.category, tx.date, tx.amount, tx.type, cur)
                    }
                    TextButton(onClick = { onNavigate("transactions") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("View all →", style = androidx.compose.ui.text.TextStyle(
                            brush = GradientBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                }

                val now = LocalDate.now()
                val upcoming = data.bills.filter { !billPaidInMonth(it, selYm) && it.dueDay >= now.dayOfMonth }
                    .sortedBy { it.dueDay }.take(3)
                if (upcoming.isNotEmpty()) {
                    SectionLabel("UPCOMING BILLS")
                    upcoming.forEach { bill ->
                        val days = bill.dueDay - now.dayOfMonth
                        val c = if (days <= 0) Danger else if (days <= 3) Warning else AmoledSubtext
                        AppCard(accentColor = c.copy(alpha = ambientAlpha), cornerRadius = 12.dp) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = Warning, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bill.name, color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (days <= 0) "Due today / overdue" else "Due in ${days}d (${bill.dueDay}th)",
                                        color = c, fontSize = 11.sp)
                                }
                                Text(formatAmount(bill.amount, cur), color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                val topGoals = data.savingsGoals.take(3)
                if (topGoals.isNotEmpty()) {
                    SectionLabel("SAVINGS GOALS")
                    topGoals.forEach { goal ->
                        val pct = if (goal.target > 0) (goal.saved / goal.target).toFloat().coerceIn(0f, 1f) else 0f
                        val gc = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { Accent }
                        AppCard(accentColor = gc.copy(alpha = ambientAlpha), cornerRadius = 12.dp) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(goal.name, color = AmoledText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${(pct * 100).toInt()}%", color = gc, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                GradientBar(pct, Brush.linearGradient(listOf(gc, gc.copy(alpha = 0.5f))))
                                Text("${formatAmount(goal.saved, cur)} of ${formatAmount(goal.target, cur)}", color = AmoledSubtext, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Full Screen "Budge-Style" Active Canvas Background ────────────────────────
@Composable
fun EnhancedHomeBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "ehbg")
    val phase1 by inf.animateFloat(0f, 2f * Math.PI.toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "p1")
    val phase2 by inf.animateFloat(0f, 2f * Math.PI.toFloat(), infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "p2")
    
    val driftFactor by inf.animateFloat(0f, 60f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "df")
    val rotation by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "rot")

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Budge-style scattered vectors over the FULL height of the screen
        
        // Top-left: Floating Coin
        rotate(degrees = rotation * 0.15f, pivot = Offset(w * 0.15f, h * 0.15f)) {
            drawCircle(color = Color(0xFF00E5A0).copy(alpha = 0.04f), radius = 35f, center = Offset(w * 0.15f, h * 0.15f), style = Stroke(width = 3f))
            drawCircle(color = Color(0xFF00E5A0).copy(alpha = 0.04f), radius = 22f, center = Offset(w * 0.15f, h * 0.15f), style = Stroke(width = 1f))
        }

        // Top-right: Abstract Card
        rotate(degrees = -15f + (driftFactor * 0.05f), pivot = Offset(w * 0.85f, h * 0.2f)) {
            drawRoundRect(
                color = Color(0xFF3D8EFF).copy(alpha = 0.04f),
                topLeft = Offset(w * 0.75f, h * 0.18f),
                size = Size(70f, 45f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 3f)
            )
        }
        
        // Middle-left: Abstract Diamond
        translate(top = -driftFactor * 0.5f) {
            rotate(degrees = rotation * 0.08f, pivot = Offset(w * 0.1f, h * 0.45f)) {
                val diamondPath = Path().apply {
                    moveTo(w * 0.1f, h * 0.43f)
                    lineTo(w * 0.13f, h * 0.46f)
                    lineTo(w * 0.1f, h * 0.51f)
                    lineTo(w * 0.07f, h * 0.46f)
                    close()
                }
                drawPath(diamondPath, color = Color(0xFFD46EFF).copy(alpha = 0.05f), style = Stroke(width = 2f))
            }
        }

        // Middle-right: Floating Coin 2
        rotate(degrees = -rotation * 0.12f, pivot = Offset(w * 0.9f, h * 0.55f)) {
            drawCircle(color = Color(0xFFFFB300).copy(alpha = 0.03f), radius = 45f, center = Offset(w * 0.9f, h * 0.55f), style = Stroke(width = 3f))
            drawCircle(color = Color(0xFFFFB300).copy(alpha = 0.03f), radius = 30f, center = Offset(w * 0.9f, h * 0.55f), style = Stroke(width = 1.5f))
        }

        // Bottom-left: Abstract Card 2
        rotate(degrees = 25f - (driftFactor * 0.04f), pivot = Offset(w * 0.2f, h * 0.75f)) {
            drawRoundRect(
                color = Color(0xFF2ED573).copy(alpha = 0.03f),
                topLeft = Offset(w * 0.1f, h * 0.72f),
                size = Size(80f, 50f),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2.5f)
            )
        }

        // Bottom-right: Abstract Diamond 2
        translate(top = driftFactor * 0.3f) {
            rotate(degrees = rotation * 0.1f, pivot = Offset(w * 0.8f, h * 0.85f)) {
                val diamondPath2 = Path().apply {
                    moveTo(w * 0.8f, h * 0.82f)
                    lineTo(w * 0.85f, h * 0.86f)
                    lineTo(w * 0.8f, h * 0.92f)
                    lineTo(w * 0.75f, h * 0.86f)
                    close()
                }
                drawPath(diamondPath2, color = Color(0xFF3D8EFF).copy(alpha = 0.04f), style = Stroke(width = 2f))
            }
        }

        // 2. Render Waves (Confined to top 30% of screen to not overwhelm the UI)
        val waveBaseHeight = h * 0.06f // Lock waves closer to the top header
        
        val path1 = Path().apply {
            moveTo(0f, waveBaseHeight)
            for (x in 0..w.toInt() step 20) {
                val xF = x.toFloat()
                val y = waveBaseHeight + 40f * sin(xF * 0.004f + phase1)
                lineTo(xF, y)
            }
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(path1, brush = Brush.verticalGradient(listOf(Color(0xFF00E5A0).copy(alpha = 0.08f), Color.Transparent)))

        val path2 = Path().apply {
            moveTo(0f, waveBaseHeight - 20f)
            for (x in 0..w.toInt() step 20) {
                val xF = x.toFloat()
                val y = (waveBaseHeight - 20f) + 32f * cos(xF * 0.005f + phase2)
                lineTo(xF, y)
            }
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(path2, brush = Brush.verticalGradient(listOf(Color(0xFF3D8EFF).copy(alpha = 0.06f), Color.Transparent)))
        
        // 3. Ambient header glow node
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFD46EFF).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.05f),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.8f, h * 0.05f)
        )

        // 4. Native, low-overhead micro-particle float network matrix across full screen
        val points = listOf(
            Offset(w * 0.15f, h * 0.15f), Offset(w * 0.35f, h * 0.10f),
            Offset(w * 0.55f, h * 0.25f), Offset(w * 0.72f, h * 0.18f),
            Offset(w * 0.90f, h * 0.30f), Offset(w * 0.25f, h * 0.35f),
            Offset(w * 0.10f, h * 0.60f), Offset(w * 0.85f, h * 0.70f),
            Offset(w * 0.45f, h * 0.85f), Offset(w * 0.65f, h * 0.95f)
        )
        
        points.forEachIndexed { idx, pt ->
            val dynamicOffset = if (idx % 2 == 0) {
                Offset(pt.x + driftFactor * 0.15f, pt.y - driftFactor * 0.1f)
            } else {
                Offset(pt.x - driftFactor * 0.1f, pt.y + driftFactor * 0.12f)
            }
            drawCircle(
                color = if (idx % 2 == 0) Color(0xFF00E5A0) else Color(0xFF3D8EFF),
                radius = 2.5f,
                center = dynamicOffset,
                alpha = 0.18f + (0.1f * sin(driftFactor * 0.05f + idx))
            )
        }
    }
}

// ── Premium Shifting Gradient Spectral Hero Card Component ──────────────────
@Composable
private fun AnimatedHomeHeroCard(income: Double, spent: Double, planned: Double, remaining: Double, cur: String, ym: String) {
    val usagePct = if (planned > 0) (spent / planned).toFloat() else 0f
    val (msg, baseBrush) = when {
        remaining < 0 -> "Overspent this month" to GradientRed
        usagePct >= 1f && planned > 0 -> "Budget fully used" to GradientGold
        planned == 0.0 -> "Set your budget" to GradientBlue
        else -> "You're on track  🎯" to GradientTeal
    }

    val cyclicTransition = rememberInfiniteTransition(label = "hero_spectral_shift")
    val shiftProgress by cyclicTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )

    val dynamicHeroBrush = remember(baseBrush, shiftProgress) {
        val colors = when (baseBrush) {
            GradientRed -> listOf(Color(0xFFFF4757).copy(alpha = 0.4f), Color(0xFFFF6B6B).copy(alpha = 0.4f), Color(0xFF9B000E).copy(alpha = 0.4f))
            GradientGold -> listOf(Color(0xFFFFB300).copy(alpha = 0.4f), Color(0xFFFFD54F).copy(alpha = 0.4f), Color(0xFFB37D00).copy(alpha = 0.4f))
            GradientBlue -> listOf(Color(0xFF3D8EFF).copy(alpha = 0.4f), Color(0xFF70A1FF).copy(alpha = 0.4f), Color(0xFF0044A3).copy(alpha = 0.4f))
            else -> listOf(Color(0xFF00E5A0).copy(alpha = 0.4f), Color(0xFF3D8EFF).copy(alpha = 0.4f), Color(0xFF009688).copy(alpha = 0.4f))
        }
        Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 200f * shiftProgress),
            end = Offset(1000f, 600f * (1f - shiftProgress))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A0F0D), Color(0xFF06090F))))
            .border(
                width = 1.dp,
                brush = dynamicHeroBrush,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = monthLabel(ym).uppercase(), 
                style = androidx.compose.ui.text.TextStyle(
                    brush = dynamicHeroBrush, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    letterSpacing = 1.5.sp
                )
            )
            Text(msg, color = AmoledText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            GradientAmountText(remaining, cur, dynamicHeroBrush, 32)
            Text("remaining this month", color = AmoledSubtext, fontSize = 12.sp)
            
            if (planned > 0) {
                Spacer(Modifier.height(2.dp))
                GradientBar(usagePct, dynamicHeroBrush)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(usagePct * 100).toInt()}% of budget used", color = AmoledSubtext, fontSize = 11.sp)
                    Text("${formatAmount(planned, cur)} planned", color = AmoledSubtext, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeSpendingDonut(catSpent: Map<String, Double>, data: AppData, cur: String) {
    val total = catSpent.values.sum()
    val colorMap = data.budgetCategories.associate { it.name to it.color }
    val top = catSpent.entries.sortedByDescending { it.value }.take(6)
    val segs = top.mapIndexed { i, (name, value) ->
        Triple(name, value, try { Color(android.graphics.Color.parseColor(colorMap[name] ?: catColor(i))) }
        catch (e: Exception) { Accent })
    }
    AppCard(accentColor = Purple) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SPENDING BREAKDOWN", style = androidx.compose.ui.text.TextStyle(
                brush = GradientPurple, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    DonutChart(segs, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradientAmountText(total, cur, GradientPurple, 12)
                        Text("total", color = AmoledSubtext, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    top.forEachIndexed { i, (name, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorDot(colorMap[name] ?: catColor(i), 8.dp)
                            Text(name, color = AmoledSubtext, fontSize = 11.sp, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${if (total > 0) (value / total * 100).toInt() else 0}%",
                                color = AmoledText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
