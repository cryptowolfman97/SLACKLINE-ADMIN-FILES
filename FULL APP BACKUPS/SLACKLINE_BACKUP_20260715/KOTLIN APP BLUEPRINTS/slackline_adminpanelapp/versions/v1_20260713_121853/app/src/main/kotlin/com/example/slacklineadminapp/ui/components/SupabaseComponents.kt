package com.example.slacklineadminapp.ui.components

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.slacklineadminapp.data.RealtimeEvent
import com.example.slacklineadminapp.data.NavScreen
import com.example.slacklineadminapp.ui.theme.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

// ── Slackline color aliases for Supabase module ───────────────────────────────
// TealCol = primary accent (maps to SupaGreen role)
// RedCol  = error/danger
// BlueCol = info
// AmberCol = warning
// GreenCol = success

// ── Card variants ─────────────────────────────────────────────────────────────

@Composable
fun SCard(
    modifier: Modifier = Modifier,
    color: Color = CardBg,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = modifier.fillMaxWidth(), color = color, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun SCard2(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) =
    SCard(modifier, CardBg2, padding, content)

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, color: Color = TealCol, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(bottom = 4.dp)) {
        Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ── Status / click chips ──────────────────────────────────────────────────────

@Composable
fun StatusChip(text: String, color: Color, small: Boolean = false) {
    Surface(color = color.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text, color = color,
            fontSize = if (small) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp)
        )
    }
}

@Composable
fun ClickChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = color.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

// ── Stat grid ─────────────────────────────────────────────────────────────────

@Composable
fun StatGrid(vararg items: Triple<String, String, Color>) {
    val rows = items.toList().chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label, color) ->
                    Surface(modifier = Modifier.weight(1f), color = color.copy(0.10f), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(label, color = SubText, fontSize = 11.sp)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Stat card with icon ───────────────────────────────────────────────────────

@Composable
fun StatCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = CardBg, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(label, color = SubText, fontSize = 11.sp)
            }
        }
    }
}

// ── Bar chart (MPAndroidChart) ────────────────────────────────────────────────

@Composable
fun BarChartView(
    entries: List<Pair<String, Float>>,
    label: String = "",
    barColor: Color = TealCol,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
) {
    if (entries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = SubText, fontSize = 12.sp)
        }
        return
    }
    val barColorArgb   = barColor.toArgb()
    val textColorArgb  = SubText.toArgb()
    val gridColorArgb  = Color(0xFF1A1A1A).toArgb()

    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setDrawGridBackground(false); setDrawBarShadow(false); setDrawValueAboveBar(true)
                description.isEnabled = false; legend.isEnabled = false
                setTouchEnabled(false); setScaleEnabled(false)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false)
                    granularity = 1f; textColor = textColorArgb; textSize = 9f
                    setTypeface(Typeface.DEFAULT)
                }
                axisLeft.apply { setDrawGridLines(true); gridColor = gridColorArgb; textColor = textColorArgb; textSize = 9f; axisMinimum = 0f }
                axisRight.isEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                animateY(600)
            }
        },
        update = { chart ->
            val barEntries = entries.mapIndexed { i, (_, v) -> BarEntry(i.toFloat(), v) }
            val labels = entries.map { it.first }
            val dataSet = BarDataSet(barEntries, label).apply {
                color = barColorArgb; valueTextColor = textColorArgb; valueTextSize = 9f
                setDrawValues(entries.size <= 12)
            }
            chart.data = BarData(dataSet).apply { barWidth = 0.6f }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.labelCount = labels.size.coerceAtMost(8)
            chart.invalidate()
        },
        modifier = modifier
    )
}

// ── Data grid ─────────────────────────────────────────────────────────────────

@Composable
fun DataGrid(
    columns: List<String>,
    rows: List<Map<String, Any>>,
    sortColumn: String = "",
    sortAsc: Boolean = true,
    onSortClick: (String) -> Unit = {},
    onDeleteRowClick: ((Map<String, Any>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colWidths = columns.map { col ->
        val maxLen = (rows.take(50).mapNotNull { it[col]?.toString()?.length }.maxOrNull() ?: 10).coerceIn(col.length, 32)
        (maxLen * 8 + 24).dp
    }
    val horizontalScrollState = rememberScrollState()
    val deleteColWidth = 40.dp
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, CardBg2, RoundedCornerShape(8.dp))) {
        Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            Row(modifier = Modifier.background(CardBg2)) {
                if (onDeleteRowClick != null) {
                    Box(modifier = Modifier.width(deleteColWidth).height(36.dp))
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(CardBg2))
                }
                columns.forEachIndexed { i, col ->
                    Row(
                        modifier = Modifier.width(colWidths[i]).clickable { onSortClick(col) }.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(col, color = if (sortColumn == col) TealCol else SubText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (sortColumn == col) Icon(if (sortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = TealCol, modifier = Modifier.size(10.dp))
                    }
                    if (i < columns.lastIndex) Box(modifier = Modifier.width(1.dp).height(36.dp).background(CardBg2))
                }
            }
            HorizontalDivider(color = CardBg2, thickness = 1.dp)
            rows.forEachIndexed { rowIdx, row ->
                val rowBg = if (rowIdx % 2 == 0) CardBg else CardBg2
                val clipboard = LocalClipboardManager.current
                Row(modifier = Modifier.background(rowBg)) {
                    if (onDeleteRowClick != null) {
                        Box(modifier = Modifier.width(deleteColWidth).height(30.dp), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { onDeleteRowClick(row) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(CardBg2))
                    }
                    columns.forEachIndexed { i, col ->
                        val cellVal = row[col]?.toString() ?: ""
                        Text(
                            cellVal, color = TextCol, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(colWidths[i])
                                .clickable { clipboard.setText(AnnotatedString(cellVal)) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                        if (i < columns.lastIndex) Box(modifier = Modifier.width(1.dp).height(30.dp).background(CardBg2))
                    }
                }
                if (rowIdx < rows.lastIndex) HorizontalDivider(color = CardBg2.copy(0.4f), thickness = 0.5.dp)
            }
        }
    }
}

// ── Structured log row ────────────────────────────────────────────────────────

@Composable
fun LogRow(record: Map<String, Any>) {
    val timestamp = (record["timestamp"] ?: record["inserted_at"] ?: record["event_message"] ?: "")
        .toString().take(19).ifBlank { "--" }
    val level = (record["level"] ?: record["severity"] ?: record["status"] ?: "")
        .toString().uppercase().ifBlank { "INFO" }
    val message = (record["event_message"] ?: record["msg"] ?: record["message"]
        ?: record.values.firstOrNull())?.toString() ?: "--"
    val levelColor = when {
        "ERROR" in level || "FATAL" in level -> RedCol
        "WARN" in level  -> AmberCol
        "DEBUG" in level -> PurpleCol
        else             -> BlueCol
    }
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CardBg)
            .clickable { clipboard.setText(AnnotatedString(message)) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(level, levelColor, small = true)
                Spacer(Modifier.width(6.dp))
                Text(timestamp, color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
            Text(message, color = TextCol, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ContentCopy, null, tint = SubText.copy(0.5f), modifier = Modifier.size(12.dp).padding(top = 2.dp))
    }
}

// ── Realtime event row ────────────────────────────────────────────────────────

@Composable
fun RealtimeEventRow(event: RealtimeEvent) {
    val color = when (event.eventType) {
        "INSERT" -> GreenCol
        "UPDATE" -> BlueCol
        "DELETE" -> RedCol
        else     -> AmberCol
    }
    val clipboard = LocalClipboardManager.current
    SCard(padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(event.eventType, color, small = true)
            Spacer(Modifier.width(8.dp))
            Text(event.table, color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(event.timestamp, color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0A0A))
                .clickable { clipboard.setText(AnnotatedString(event.payload)) }.padding(8.dp)
        ) {
            Text(event.payload.take(300), color = TealCol, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Web Dashboard button ──────────────────────────────────────────────────────

@Composable
fun WebDashboardButton(label: String = "Open in Web Dashboard", onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Default.OpenInBrowser, null, tint = BlueCol, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = BlueCol, fontSize = 12.sp)
    }
}

// ── Text field ────────────────────────────────────────────────────────────────

@Composable
fun STextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    hint: String = "",
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var showPassword by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(label, color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CardBg2)
                .border(1.dp, CardBg2, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = TextStyle(color = TextCol, fontSize = 13.sp,
                    fontFamily = if (isPassword) FontFamily.Monospace else FontFamily.Default),
                cursorBrush = SolidColor(TealCol), singleLine = singleLine,
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty() && hint.isNotBlank()) Text(hint, color = SubText.copy(0.5f), fontSize = 13.sp)
                    inner()
                }
            )
            if (isPassword) {
                IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(22.dp)) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = SubText, modifier = Modifier.size(16.dp))
                }
            }
            trailingIcon?.invoke()
        }
    }
}

// ── Search field ──────────────────────────────────────────────────────────────

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String = "Search...", modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBg2)
            .border(1.dp, CardBg2, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = SubText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange,
            textStyle = TextStyle(color = TextCol, fontSize = 13.sp),
            cursorBrush = SolidColor(TealCol), singleLine = true, modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = SubText.copy(0.5f), fontSize = 13.sp)
                inner()
            }
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Clear, null, tint = SubText, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Code block ────────────────────────────────────────────────────────────────

@Composable
fun CodeBlock(text: String, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Surface(modifier = modifier.fillMaxWidth(), color = Color(0xFF0A0A0A), shape = RoundedCornerShape(8.dp)) {
        Box {
            Text(text, color = TealCol, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(12.dp))
            IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
                Icon(Icons.Default.ContentCopy, null, tint = SubText, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Copy row ──────────────────────────────────────────────────────────────────

@Composable
fun CopyRow(label: String, value: String, masked: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CardBg2)
            .clickable { clipboard.setText(AnnotatedString(value)) }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SubText, fontSize = 10.sp)
            Text(
                if (masked && value.length > 8) "${value.take(4)}…${value.takeLast(4)}" else value,
                color = TextCol, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1
            )
        }
        Icon(Icons.Default.ContentCopy, null, tint = SubText, modifier = Modifier.size(14.dp))
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────

@Composable
fun SDivider() { HorizontalDivider(color = SubText.copy(alpha = 0.15f), thickness = 1.dp) }

// ── Icon label row ────────────────────────────────────────────────────────────

@Composable
fun IconLabelRow(icon: ImageVector, label: String, value: String, color: Color = TextCol) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(icon, null, tint = SubText, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", color = SubText, fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Default.SearchOff) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = SubText.copy(0.5f), modifier = Modifier.size(40.dp))
        Text(message, color = SubText, fontSize = 13.sp)
    }
}

// ── Error banner with optional web fallback ───────────────────────────────────

@Composable
fun ErrorBanner(message: String, onDismiss: (() -> Unit)? = null, onWebFallback: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(RedCol.copy(0.12f)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = RedCol, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = RedCol, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = RedCol, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (onWebFallback != null) {
            Spacer(Modifier.height(4.dp))
            WebDashboardButton(onClick = onWebFallback)
        }
    }
}

// ── Info banner ───────────────────────────────────────────────────────────────

@Composable
fun InfoBanner(message: String, color: Color = BlueCol, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(color.copy(0.10f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = color, fontSize = 12.sp)
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = TealCol, textColor: Color = Color.White, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(8.dp), enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(0.4f))
    ) { Text(text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = SubText) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.4f))
    ) { Text(text, color = color, fontSize = 13.sp) }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    PrimaryButton(text, onClick, modifier, RedCol, Color.White)

// ── Loading overlay ───────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(message: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.70f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = TealCol, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Text(message, color = TextCol, fontSize = 13.sp)
            }
        }
    }
}

// ── Live dot ──────────────────────────────────────────────────────────────────

@Composable
fun LiveDot(color: Color = GreenCol) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
}

// ── Confirm dialog ────────────────────────────────────────────────────────────

@Composable
fun SConfirmDialog(title: String, message: String, confirmText: String = "Confirm", confirmColor: Color = RedCol, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBg2,
        title = { Text(title, color = TextCol, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = SubText, fontSize = 13.sp) },
        confirmButton = { PrimaryButton(confirmText, onConfirm, color = confirmColor, textColor = Color.White) },
        dismissButton = { SecondaryButton("Cancel", onDismiss) }
    )
}

// ── Require credentials ───────────────────────────────────────────────────────

@Composable
fun RequireCredentials(vararg messages: String, onWebFallback: (() -> Unit)? = null) {
    SCard {
        Icon(Icons.Default.VpnKey, null, tint = AmberCol, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        messages.forEach { Text(it, color = AmberCol, fontSize = 13.sp) }
        Spacer(Modifier.height(4.dp))
        Text("Go to the Connection tab to save credentials.", color = SubText, fontSize = 12.sp)
        if (onWebFallback != null) {
            Spacer(Modifier.height(8.dp))
            WebDashboardButton(onClick = onWebFallback)
        }
    }
}

// ── Sub-nav chip row ──────────────────────────────────────────────────────────

@Composable
fun SubNavRow(screens: List<Pair<String, NavScreen>>, active: NavScreen, onSelect: (NavScreen) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(screens.size) { i ->
            val (label, screen) = screens[i]
            val selected = active == screen
            Surface(
                onClick = { onSelect(screen) },
                color = if (selected) TealCol.copy(0.15f) else CardBg2,
                shape = RoundedCornerShape(20.dp),
                border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, TealCol.copy(0.4f)) else null
            ) {
                Text(label, color = if (selected) TealCol else SubText, fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
    }
}
