package com.shvertex.universalconv.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.universalconv.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import kotlin.math.pow


// ── App top bar ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SHVTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Teal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = if (onBack != null) {{
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Teal)
            }
        }} else ({}),
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Black,
            titleContentColor = Teal,
        ),
    )
}

// ── Section header ─────────────────────────────────────────────────
@Composable
fun SectionHeader(text: String, accent: Color = Teal) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.07f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(Modifier.size(3.dp, 14.dp).background(accent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.8.sp,
        )
    }
}

// ── Card container ─────────────────────────────────────────────────
@Composable
fun SHVCard(
    modifier: Modifier = Modifier,
    accent: Color = Teal,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val mod = if (onClick != null)
        modifier.clip(shape).clickable(onClick = onClick)
    else modifier
    Column(
        modifier = mod
            .background(CardBg, shape)
            .border(1.dp, Border, shape)
            .padding(16.dp),
        content = content,
    )
}

// ── Unit chip (scrollable selector) ────────────────────────────────
@Composable
fun UnitChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg  = if (selected) accent.copy(alpha = 0.18f) else Surface2
    val fg  = if (selected) accent else TextSecondary
    val border = if (selected) accent.copy(alpha = 0.5f) else Border
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = fg)
    }
}

// ── Value input ────────────────────────────────────────────────────
@Composable
fun ValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "Enter value…",
    accent: Color = Teal,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, color = TextMuted, style = MaterialTheme.typography.bodyMedium) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = {
                    val text = clipboard.getText()?.text ?: ""
                    if (text.isNotEmpty()) onValueChange(text.trim())
                }) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            focusedBorderColor   = accent,
            unfocusedBorderColor = Border,
            cursorColor          = accent,
            focusedContainerColor   = Surface2,
            unfocusedContainerColor = Surface1,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    )
}

// ── Result row ─────────────────────────────────────────────────────
@Composable
fun ResultRow(label: String, value: String, accent: Color) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(1200); copied = false } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Unit badge
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 52.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (value == "—" || value == "error") TextMuted else TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = {
                if (value != "—" && value != "error") {
                    clipboard.setText(AnnotatedString(value))
                    copied = true
                }
            },
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = if (copied) Green else accent.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Module tile (3-column compact) ─────────────────────────────────
@Composable
fun ModuleTile(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 14.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
    }
}

// ── Generic unit converter screen layout ───────────────────────────
@Composable
fun GenericConverterContent(
    title: String,
    accent: Color,
    units: List<String>,
    selectedUnit: String,
    onUnitSelect: (String) -> Unit,
    inputValue: String,
    onInputChange: (String) -> Unit,
    results: List<Pair<String, String>>,
    onBack: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Scaffold(
        topBar = { SHVTopBar(title, onBack = onBack) },
        containerColor = Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Unit chips
            Text("FROM", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                units.forEach { u ->
                    UnitChip(u, u == selectedUnit, accent) { onUnitSelect(u) }
                }
            }
            // Input
            ValueInput(inputValue, onInputChange, accent = accent)
            // Extra content (e.g. additional fields)
            extraContent()
            // Results
            Text("TO — ALL UNITS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { (label, value) ->
                        ResultRow(label, value, accent)
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}


// ── Calculator engine (shared by popup dialog + PIP mini calculator) ──
class CalcEngine {
    var expr          by mutableStateOf("")
    var result        by mutableStateOf("")
    var justGotResult by mutableStateOf(false)
    var memory        by mutableStateOf(0.0)
    var isDeg         by mutableStateOf(true)
    val history       = mutableStateListOf<String>()

    fun safeEval(raw: String): Double {
        var s = raw.replace("÷", "/").replace("×", "*").replace("−", "-")
        val opens = s.count { it == '(' } - s.count { it == ')' }
        s += ")".repeat(maxOf(opens, 0))
        val sinF  : (Double) -> Double = if (isDeg) { x -> kotlin.math.sin(Math.toRadians(x)) }  else { x -> kotlin.math.sin(x) }
        val cosF  : (Double) -> Double = if (isDeg) { x -> kotlin.math.cos(Math.toRadians(x)) }  else { x -> kotlin.math.cos(x) }
        val tanF  : (Double) -> Double = if (isDeg) { x -> kotlin.math.tan(Math.toRadians(x)) }  else { x -> kotlin.math.tan(x) }
        val asinF : (Double) -> Double = if (isDeg) { x -> Math.toDegrees(kotlin.math.asin(x)) } else { x -> kotlin.math.asin(x) }
        val acosF : (Double) -> Double = if (isDeg) { x -> Math.toDegrees(kotlin.math.acos(x)) } else { x -> kotlin.math.acos(x) }
        val atanF : (Double) -> Double = if (isDeg) { x -> Math.toDegrees(kotlin.math.atan(x)) } else { x -> kotlin.math.atan(x) }
        return ExprParser(s, sinF, cosF, tanF, asinF, acosF, atanF).parse()
    }

    fun formatResult(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return if (v == kotlin.math.floor(v) && kotlin.math.abs(v) < 1e15)
            v.toLong().toString()
        else "%.10g".format(v)
    }

    fun handleBtn(label: String) {
        when (label) {
            "C"  -> { expr = ""; result = ""; justGotResult = false }
            "⌫"  -> {
                if (justGotResult) { expr = ""; justGotResult = false }
                else if (expr.isNotEmpty()) expr = expr.dropLast(1)
                result = ""
            }
            "="  -> {
                try {
                    val v = safeEval(expr)
                    val r = formatResult(v)
                    history.add("$expr = $r")
                    if (history.size > 5) history.removeAt(0)
                    result = r; justGotResult = true
                } catch (e: Exception) { result = "Error" }
            }
            "MC" -> memory = 0.0
            "MR" -> { expr += formatResult(memory); justGotResult = false }
            "M+" -> { try { memory += safeEval(expr) } catch (e: Exception) {} }
            "M−" -> { try { memory -= safeEval(expr) } catch (e: Exception) {} }
            "MS" -> { try { memory  = safeEval(expr) } catch (e: Exception) {} }
            "π"  -> { if (justGotResult) expr = ""; expr += kotlin.math.PI.toString(); justGotResult = false }
            "e"  -> { if (justGotResult) expr = ""; expr += kotlin.math.E.toString();  justGotResult = false }
            "±"  -> { expr = if (expr.startsWith("-")) expr.drop(1) else "-$expr" }
            else -> {
                val fnMap = mapOf(
                    "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
                    "sin⁻¹" to "asin(", "cos⁻¹" to "acos(", "tan⁻¹" to "atan(",
                    "sinh" to "sinh(", "cosh" to "cosh(", "tanh" to "tanh(",
                    "log" to "log10(", "ln" to "ln(", "log₂" to "log2(",
                    "eˣ" to "exp(", "10ˣ" to "10^(", "√" to "sqrt(",
                    "∛" to "cbrt(", "x²" to "^2", "x³" to "^3", "xʸ" to "^",
                )
                val opMap = mapOf("÷" to "÷", "×" to "×", "−" to "−")
                val token = fnMap[label] ?: opMap[label] ?: label
                if (justGotResult && result.isNotEmpty() && label !in listOf("+","−","×","÷","%","^")) {
                    expr = ""
                }
                justGotResult = false
                expr += token
            }
        }
    }
}

@Composable
fun rememberCalcEngine(): CalcEngine = remember { CalcEngine() }

// ── Calculator display ──────────────────────────────────────────────
@Composable
fun CalcDisplay(
    engine: CalcEngine,
    accent: Color = Teal,
    showHistory: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (showHistory && engine.history.isNotEmpty()) {
            Text(
                engine.history.last(),
                style    = MaterialTheme.typography.labelSmall,
                color    = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            engine.expr.ifEmpty { "0" },
            style     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color     = TextSecondary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                engine.result,
                style      = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
                textAlign  = TextAlign.End,
            )
            if (engine.result.isNotEmpty() && engine.result != "Error") {
                IconButton(
                    onClick  = { clipboard.setText(AnnotatedString(engine.result)) },
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = accent, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

// ── Basic 4×5 keypad — responsive, fills the height it is given ──────
private val BASIC_CALC_ROWS = listOf(
    listOf("C","±","%","÷"),
    listOf("7","8","9","×"),
    listOf("4","5","6","−"),
    listOf("1","2","3","+"),
    listOf("⌫","0",".","="),
)

private fun basicBtnColors(lbl: String): Pair<Color, Color> = when {
    lbl == "="  -> TealDim to Black
    lbl == "C"  -> Color(0xFF3D0A0A) to Rose
    lbl == "⌫"  -> Color(0xFF2A1500) to Orange
    lbl in listOf("÷","×","−","+","%") -> Color(0xFF0A2218) to Green
    lbl == "±"  -> Surface3 to TextSecondary
    else -> Surface2 to TextPrimary
}

@Composable
fun BasicCalcGrid(
    engine: CalcEngine,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    spacing: Dp = 6.dp,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        BASIC_CALC_ROWS.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                row.forEach { lbl ->
                    val (bg, fg) = basicBtnColors(lbl)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable { engine.handleBtn(lbl) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            lbl,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = fg,
                            fontSize   = fontSize,
                        )
                    }
                }
            }
        }
    }
}

// ── Scientific functions panel (shown above the basic grid) ──────────
private val SCI_PANEL_ROWS = listOf(
    listOf("MC","MR","M+","M−","MS"),
    listOf("sin","cos","tan","sin⁻¹","cos⁻¹"),
    listOf("tan⁻¹","sinh","cosh","tanh","log"),
    listOf("ln","log₂","eˣ","10ˣ","√"),
    listOf("∛","x²","x³","xʸ","π"),
    listOf("(",")","e","",""),
)

private fun sciBtnColors(lbl: String): Pair<Color, Color> = when {
    lbl in listOf("MC","MR","M+","M−","MS") -> Color(0xFF1A0A2B) to Purple
    lbl in listOf("(",")") -> Surface3 to TextSecondary
    lbl.isEmpty() -> Color.Transparent to Color.Transparent
    else -> Color(0xFF0D1A2B) to Blue
}

@Composable
fun ScientificCalcPanel(engine: CalcEngine, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SCI_PANEL_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEach { lbl ->
                    if (lbl.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val (bg, fg) = sciBtnColors(lbl)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { engine.handleBtn(lbl) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                lbl,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color      = fg,
                                fontSize   = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Segmented toggle (Basic/Scientific, DEG/RAD, etc.) ────────────────
@Composable
fun SegmentedToggle(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface3),
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Teal else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    opt,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = if (active) Black else TextSecondary,
                    fontSize   = 11.sp,
                )
            }
        }
    }
}

// ── Calculator FAB + popup dialog ─────────────────────────────────────
@Composable
fun SciCalcFAB(
    showCalc: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick        = onToggle,
        modifier       = modifier,
        containerColor = TealDim,
        contentColor   = Black,
        shape          = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Calculate, contentDescription = "Calculator", modifier = Modifier.size(20.dp))
            Text("Calculator", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }

    if (showCalc) {
        SciCalcDialog(onDismiss = onDismiss)
    }
}

@Composable
fun SciCalcDialog(onDismiss: () -> Unit) {
    val engine = rememberCalcEngine()
    var mode by remember { mutableStateOf("Basic") }

    Dialog(
        onDismissRequest = onDismiss,
        // Platform default width keeps the dialog naturally sized
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(20.dp),
            color          = Surface1,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Header ──
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Calculator",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Teal,
                        modifier   = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // ── Mode + angle-unit toggles ──
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    SegmentedToggle(
                        options  = listOf("Basic","Scientific"),
                        selected = mode,
                        onSelect = { mode = it },
                    )
                    if (mode == "Scientific") {
                        SegmentedToggle(
                            options  = listOf("DEG","RAD"),
                            selected = if (engine.isDeg) "DEG" else "RAD",
                            onSelect = { engine.isDeg = (it == "DEG") },
                        )
                    }
                }

                // ── Display ──
                CalcDisplay(engine)

                // ── Scientific functions (only in Scientific mode) ──
                if (mode == "Scientific") {
                    ScientificCalcPanel(engine, modifier = Modifier.fillMaxWidth())
                }

                // ── Keypad ──
                BasicCalcGrid(
                    engine   = engine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (mode == "Scientific") 230.dp else 260.dp),
                )
            }
        }
    }
}

// ── PIP launch FAB ──────────────────────────────────────────────────
@Composable
fun PipCalcFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick        = onClick,
        modifier       = modifier,
        containerColor = Surface2,
        contentColor   = Teal,
        shape          = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "PIP Calculator", modifier = Modifier.size(20.dp))
            Text("PIP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Expression parser ───────────────────────────────────────────────
private class ExprParser(
    private val input: String,
    private val sinF : (Double) -> Double,
    private val cosF : (Double) -> Double,
    private val tanF : (Double) -> Double,
    private val asinF: (Double) -> Double,
    private val acosF: (Double) -> Double,
    private val atanF: (Double) -> Double,
) {
    private var pos = 0

    fun parse(): Double {
        val v = parseExpr()
        if (pos < input.length) throw IllegalArgumentException("Unexpected: ${input[pos]}")
        return v
    }

    private fun parseExpr(): Double {
        var v = parseTerm()
        while (pos < input.length) {
            when {
                input[pos] == '+' -> { pos++; v += parseTerm() }
                input[pos] == '-' -> { pos++; v -= parseTerm() }
                else -> break
            }
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parsePower()
        while (pos < input.length) {
            when {
                input[pos] == '*' -> { pos++; v *= parsePower() }
                input[pos] == '/' -> { pos++; v /= parsePower() }
                input[pos] == '%' -> { pos++; v %= parsePower() }
                else -> break
            }
        }
        return v
    }

    private fun parsePower(): Double {
        var base = parseUnary()
        if (pos < input.length && input[pos] == '^') {
            pos++; base = base.pow(parseUnary())
        }
        return base
    }

    private fun parseUnary(): Double {
        if (pos < input.length && input[pos] == '-') { pos++; return -parseUnary() }
        if (pos < input.length && input[pos] == '+') { pos++; return  parseUnary() }
        return parseAtom()
    }

    private fun parseAtom(): Double {
        if (pos >= input.length) return 0.0
        if (input[pos] == '(') {
            pos++
            val v = parseExpr()
            if (pos < input.length && input[pos] == ')') pos++
            return v
        }
        if (input[pos].isDigit() || input[pos] == '.') {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.' || input[pos] == 'E' || (input[pos] == '-' && pos > 0 && input[pos-1] == 'E'))) pos++
            return input.substring(start, pos).toDouble()
        }
        val rest = input.substring(pos)
        val fnMap = mapOf(
            "asin"  to { x: Double -> asinF(x) },
            "acos"  to { x: Double -> acosF(x) },
            "atan"  to { x: Double -> atanF(x) },
            "sinh"  to { x: Double -> kotlin.math.sinh(x) },
            "cosh"  to { x: Double -> kotlin.math.cosh(x) },
            "tanh"  to { x: Double -> kotlin.math.tanh(x) },
            "sin"   to { x: Double -> sinF(x) },
            "cos"   to { x: Double -> cosF(x) },
            "tan"   to { x: Double -> tanF(x) },
            "log10" to { x: Double -> kotlin.math.log10(x) },
            "log2"  to { x: Double -> kotlin.math.log2(x) },
            "ln"    to { x: Double -> kotlin.math.ln(x) },
            "exp"   to { x: Double -> kotlin.math.exp(x) },
            "sqrt"  to { x: Double -> kotlin.math.sqrt(x) },
            "cbrt"  to { x: Double -> kotlin.math.sign(x) * kotlin.math.abs(x).pow(1.0/3.0) },
            "abs"   to { x: Double -> kotlin.math.abs(x) },
        )
        for ((name, fn) in fnMap) {
            if (rest.startsWith(name)) {
                pos += name.length
                val arg = parseAtom()
                return fn(arg)
            }
        }
        if (rest.startsWith("PI") || rest.startsWith("pi")) { pos += 2; return kotlin.math.PI }
        if (rest.startsWith("E") || rest.startsWith("e"))   { pos += 1; return kotlin.math.E  }
        throw IllegalArgumentException("Unknown token at: $rest")
    }
}
