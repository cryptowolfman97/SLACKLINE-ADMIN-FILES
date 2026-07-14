package com.shvertex.universalconv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.universalconv.data.*
import com.shvertex.universalconv.navigation.*
import com.shvertex.universalconv.ui.components.*
import com.shvertex.universalconv.ui.theme.*
import kotlin.math.*

@Composable
fun ConverterScreen(moduleId: String, onBack: () -> Unit) {
    val module = ALL_MODULES.find { it.id == moduleId }
    val accent = module?.accent ?: Teal
    val title  = module?.title ?: moduleId

    when (moduleId) {
        "temperature"  -> TemperatureConverterScreen(accent, onBack)
        "fuel"         -> FuelConverterScreen(accent, onBack)
        "currency"     -> CurrencyConverterScreen(accent, onBack)
        "electric"     -> ElectricConverterScreen(accent, onBack)
        "color"        -> ColorCodeScreen(accent, onBack)
        "numeral"      -> NumeralScreen(accent, onBack)
        "sound"        -> SoundConverterScreen(accent, onBack)
        "papersize"    -> PaperSizeScreen(accent, onBack)
        "timezone"     -> TimeZoneScreen(accent, onBack)
        "dateage"      -> DateAgeScreen(accent, onBack)
        "tip"          -> TipCalculatorScreen(accent, onBack)
        "discount"     -> DiscountScreen(accent, onBack)
        "bmibmr"       -> BMIBMRScreen(accent, onBack)
        "shoesize"     -> ShoeSizeScreen(accent, onBack)
        "ringsize"     -> RingSizeScreen(accent, onBack)
        "bloodglucose" -> BloodGlucoseScreen(accent, onBack)
        "tiresize"     -> TireSizeScreen(accent, onBack)
        "humidity"     -> HumidityScreen(accent, onBack)
        "clothing"     -> ClothingScreen(accent, onBack)
        else           -> LinearConverterScreen(moduleId, title, accent, onBack)
    }
}

// ── Generic linear converter ───────────────────────────────────────
@Composable
fun LinearConverterScreen(moduleId: String, title: String, accent: Color, onBack: () -> Unit) {
    val units = when (moduleId) {
        "length"        -> LENGTH_UNITS
        "weight"        -> WEIGHT_UNITS
        "volume"        -> VOLUME_UNITS
        "area"          -> AREA_UNITS
        "time"          -> TIME_UNITS
        "speed"         -> SPEED_UNITS
        "pressure"      -> PRESSURE_UNITS
        "energy"        -> ENERGY_UNITS
        "power"         -> POWER_UNITS
        "torque"        -> TORQUE_UNITS
        "acceleration"  -> ACCELERATION_UNITS
        "force"         -> FORCE_UNITS
        "density"       -> DENSITY_UNITS
        "flowrate"      -> FLOW_UNITS
        "viscosity"     -> VISCOSITY_UNITS
        "angle"         -> ANGLE_UNITS
        "frequency"     -> FREQUENCY_UNITS
        "magnetic"      -> MAGNETIC_UNITS
        "datastorage"   -> DATA_STORAGE_UNITS
        "typography"    -> TYPOGRAPHY_UNITS
        "illuminance"   -> ILLUMINANCE_UNITS
        "luminance"     -> LUMINANCE_UNITS
        "radioactivity" -> RADIOACTIVITY_UNITS
        "raddose"       -> RAD_DOSE_UNITS
        "concentration" -> CONCENTRATION_UNITS
        "cooking"       -> COOKING_UNITS
        else -> LENGTH_UNITS
    }

    var inputVal by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf(units.first().label) }

    val results = remember(inputVal, fromUnit) {
        val v = inputVal.toDoubleOrNull()
        if (v != null) linearConvert(v, fromUnit, units)
        else units.map { ConvResult(it.label, "—") }
    }

    GenericConverterContent(
        title       = title,
        accent      = accent,
        units       = units.map { it.label },
        selectedUnit= fromUnit,
        onUnitSelect= { fromUnit = it },
        inputValue  = inputVal,
        onInputChange = { inputVal = it },
        results     = results.map { it.label to it.value },
        onBack      = onBack,
    )
}

// ── Temperature ────────────────────────────────────────────────────
@Composable
fun TemperatureConverterScreen(accent: Color, onBack: () -> Unit) {
    var inputVal by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("°C") }

    val results = remember(inputVal, fromUnit) {
        val v = inputVal.toDoubleOrNull()
        TEMP_UNITS.map { to ->
            to to (if (v != null) formatNum(convertTemperature(v, fromUnit, to)) else "—")
        }
    }

    GenericConverterContent(
        title = "Temperature", accent = accent,
        units = TEMP_UNITS, selectedUnit = fromUnit, onUnitSelect = { fromUnit = it },
        inputValue = inputVal, onInputChange = { inputVal = it },
        results = results, onBack = onBack,
    )
}

// ── Fuel Economy ───────────────────────────────────────────────────
@Composable
fun FuelConverterScreen(accent: Color, onBack: () -> Unit) {
    var inputVal by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("km/L") }

    val results = remember(inputVal, fromUnit) {
        val v = inputVal.toDoubleOrNull()
        FUEL_UNITS.map { to ->
            to to (if (v != null) formatNum(convertFuel(v, fromUnit, to)) else "—")
        }
    }

    GenericConverterContent(
        title = "Fuel Economy", accent = accent,
        units = FUEL_UNITS, selectedUnit = fromUnit, onUnitSelect = { fromUnit = it },
        inputValue = inputVal, onInputChange = { inputVal = it },
        results = results, onBack = onBack,
    )
}

// ── Sound ──────────────────────────────────────────────────────────
@Composable
fun SoundConverterScreen(accent: Color, onBack: () -> Unit) {
    var inputVal by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("dB") }

    val results = remember(inputVal, fromUnit) {
        val v = inputVal.toDoubleOrNull()
        SOUND_UNITS.map { to ->
            to to (if (v != null) formatNum(convertSound(v, fromUnit, to)) else "—")
        }
    }

    GenericConverterContent(
        title = "Sound Level", accent = accent,
        units = SOUND_UNITS, selectedUnit = fromUnit, onUnitSelect = { fromUnit = it },
        inputValue = inputVal, onInputChange = { inputVal = it },
        results = results, onBack = onBack,
    )
}

// ── Electric ───────────────────────────────────────────────────────
@Composable
fun ElectricConverterScreen(accent: Color, onBack: () -> Unit) {
    var group by remember { mutableStateOf(ELECTRIC_GROUPS[0]) }
    var inputVal by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf(ELECTRIC_GROUPS[0].units.first().label) }

    LaunchedEffect(group) { fromUnit = group.units.first().label }

    val results = remember(inputVal, fromUnit, group) {
        val v = inputVal.toDoubleOrNull()
        group.units.map { u ->
            u.label to (if (v != null) {
                val fromF = group.units.first { it.label == fromUnit }.factor
                formatNum(v * fromF / u.factor)
            } else "—")
        }
    }

    Scaffold(
        topBar = { SHVTopBar("Electric Units", onBack = onBack) },
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
            // Group selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ELECTRIC_GROUPS.forEach { g ->
                    UnitChip(g.label, g.label == group.label, accent) {
                        group = g; inputVal = ""
                    }
                }
            }
            ValueInput(inputVal, { inputVal = it }, accent = accent)
            Text("TO — ALL UNITS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                group.units.forEach { u -> UnitChip(u.label, u.label == fromUnit, accent) { fromUnit = u.label } }
            }
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { (lbl, v) -> ResultRow(lbl, v, accent) }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Color Codes ────────────────────────────────────────────────────
@Composable
fun ColorCodeScreen(accent: Color, onBack: () -> Unit) {
    var hexInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    data class ColorResult(val space: String, val value: String, val col: Color)

    val colorResults = remember(hexInput) {
        val raw = hexInput.trim().trimStart('#')
        val expanded = when (raw.length) {
            3 -> "${raw[0]}${raw[0]}${raw[1]}${raw[1]}${raw[2]}${raw[2]}"
            6 -> raw
            else -> null
        }
        if (expanded == null) return@remember emptyList()
        try {
            val r = expanded.substring(0,2).toInt(16)
            val g = expanded.substring(2,4).toInt(16)
            val b = expanded.substring(4,6).toInt(16)
            val rf = r/255f; val gf = g/255f; val bf = b/255f
            val cmax = maxOf(rf,gf,bf); val cmin = minOf(rf,gf,bf); val delta = cmax - cmin
            val l = (cmax+cmin)/2f
            val s = if (delta == 0f) 0f else delta/(1f - abs(2*l-1))
            val h = when {
                delta == 0f -> 0f
                cmax == rf  -> 60f * (((gf-bf)/delta) % 6)
                cmax == gf  -> 60f * (((bf-rf)/delta) + 2)
                else        -> 60f * (((rf-gf)/delta) + 4)
            }
            val v = cmax
            val sv = if (cmax == 0f) 0f else delta/cmax
            val k = 1f - cmax
            val cm = if (k == 1f) 0f else (1f-rf-k)/(1f-k)
            val mg = if (k == 1f) 0f else (1f-gf-k)/(1f-k)
            val y  = if (k == 1f) 0f else (1f-bf-k)/(1f-k)
            listOf(
                ColorResult("RGB",  "rgb($r, $g, $b)",  Teal),
                ColorResult("HSL",  "hsl(%.1f, %.1f%%, %.1f%%)".format(h, s*100, l*100), Blue),
                ColorResult("HSV",  "hsv(%.1f, %.1f%%, %.1f%%)".format(h, sv*100, v*100), Purple),
                ColorResult("CMYK", "cmyk(%.1f%%, %.1f%%, %.1f%%, %.1f%%)".format(cm*100, mg*100, y*100, k*100), Gold),
            )
        } catch (e: Exception) { emptyList() }
    }

    val previewColor = remember(hexInput) {
        try {
            val raw = hexInput.trim().trimStart('#')
            val ex = when(raw.length) { 3 -> "${raw[0]}${raw[0]}${raw[1]}${raw[1]}${raw[2]}${raw[2]}"; 6 -> raw; else -> null }
            if (ex != null) Color(("FF$ex").toLong(16).or(0xFF000000)) else Surface2
        } catch (e: Exception) { Surface2 }
    }

    Scaffold(topBar = { SHVTopBar("Color Codes", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("HEX INPUT", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            ValueInput(hexInput, { hexInput = it }, hint = "#RRGGBB or RRGGBB", accent = accent)
            // Color swatch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewColor)
                    .border(1.dp, Border, RoundedCornerShape(12.dp)),
            )
            // Results
            colorResults.forEach { cr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cr.col.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.background(cr.col.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text(cr.space, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cr.col)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(cr.value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(cr.value)) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = cr.col.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Numeral Systems ────────────────────────────────────────────────
@Composable
fun NumeralScreen(accent: Color, onBack: () -> Unit) {
    var fromBase by remember { mutableStateOf("DEC") }
    var inputVal by remember { mutableStateOf("") }
    val bases = listOf("DEC","BIN","HEX","OCT","TXT","ROM")
    val clipboard = LocalClipboardManager.current

    data class NumeralResult(val base: String, val value: String, val col: Color)

    val results: List<NumeralResult> = remember(inputVal, fromBase) {
        val colors = listOf(Teal, Gold, Blue, Rose, Pink, Color(0xFFFFCC33))
        if (inputVal.isBlank()) return@remember bases.mapIndexed { i, b -> NumeralResult(b, "—", colors[i]) }
        try {
            val n: Long = when (fromBase) {
                "DEC" -> inputVal.trim().toLong()
                "BIN" -> inputVal.trim().removePrefix("0b").toLong(2)
                "HEX" -> inputVal.trim().removePrefix("0x").toLong(16)
                "OCT" -> inputVal.trim().removePrefix("0o").toLong(8)
                "TXT" -> inputVal.first().code.toLong()
                "ROM" -> romanToInt(inputVal.trim()).toLong()
                else  -> inputVal.trim().toLong()
            }
            bases.mapIndexed { i, b ->
                val v = when (b) {
                    "DEC" -> n.toString()
                    "BIN" -> n.toString(2)
                    "HEX" -> n.toString(16).uppercase()
                    "OCT" -> n.toString(8)
                    "TXT" -> inputVal.map { c -> "${c.code}" }.joinToString(" ")
                    "ROM" -> intToRoman(n.toInt())
                    else  -> "—"
                }
                NumeralResult(b, v, colors[i])
            }
        } catch (e: Exception) {
            bases.mapIndexed { i, b -> NumeralResult(b, "error", listOf(Teal, Gold, Blue, Rose, Pink, Color(0xFFFFCC33))[i]) }
        }
    }

    Scaffold(topBar = { SHVTopBar("Numeral Systems", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("INPUT BASE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                bases.forEach { b -> UnitChip(b, b == fromBase, accent) { fromBase = b; inputVal = "" } }
            }
            ValueInput(inputVal, { inputVal = it }, hint = "Enter value…", accent = accent)
            Text("ALL REPRESENTATIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(r.col.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.background(r.col.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp).defaultMinSize(minWidth = 38.dp)) {
                                Text(r.base, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = r.col, textAlign = TextAlign.Center)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(r.value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = if (r.value == "—" || r.value == "error") TextMuted else TextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (r.value != "—" && r.value != "error") clipboard.setText(AnnotatedString(r.value)) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = r.col.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Paper Sizes ────────────────────────────────────────────────────
@Composable
fun PaperSizeScreen(accent: Color, onBack: () -> Unit) {
    var unit by remember { mutableStateOf("mm") }
    val units = listOf("mm","cm","in","px")
    val clipboard = LocalClipboardManager.current

    fun mmTo(v: Int, u: String) = when (u) {
        "cm" -> "%.1f".format(v/10.0)
        "in" -> "%.2f".format(v/25.4)
        "px" -> "${(v/25.4*96).toInt()}"
        else -> v.toString()
    }

    Scaffold(topBar = { SHVTopBar("Paper Sizes", onBack = onBack) }, containerColor = Black) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(14.dp)) {
                units.forEach { u -> UnitChip(u, u == unit, accent) { unit = u } }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                PAPER_SIZES.forEachIndexed { i, p ->
                    val dim = "${mmTo(p.wMm, unit)} × ${mmTo(p.hMm, unit)} $unit"
                    val col = listOf(Teal,Blue,Purple,Gold,Rose,Green,Cyan,Pink,Orange)[i % 9]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(col.copy(alpha = 0.07f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = col, modifier = Modifier.width(100.dp))
                        Text(dim, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(dim)) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = col.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Time Zones ─────────────────────────────────────────────────────
@Composable
fun TimeZoneScreen(accent: Color, onBack: () -> Unit) {
    var inputTime by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val cols = listOf(Teal,Blue,Purple,Gold,Rose,Green,Cyan,Pink,Orange)

    fun convertTimes(raw: String): Map<String,String> {
        val parts = raw.trim().replace(".",":").split(":")
        return try {
            val h = parts[0].toInt() % 24
            val m = if (parts.size > 1) parts[1].toInt() else 0
            val utcMin = h*60 + m
            TIMEZONES.associate { tz ->
                val total = ((utcMin + (tz.offset * 60).toInt()) % 1440 + 1440) % 1440
                val th = total / 60; val tm = total % 60
                tz.code to "%02d:%02d".format(th, tm)
            }
        } catch (e: Exception) { TIMEZONES.associate { it.code to "—" } }
    }

    val times = remember(inputTime) { if (inputTime.isBlank()) emptyMap() else convertTimes(inputTime) }

    Scaffold(topBar = { SHVTopBar("Time Zones", onBack = onBack) }, containerColor = Black) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ValueInput(inputTime, { inputTime = it }, hint = "UTC time e.g. 14:30", accent = accent, modifier = Modifier.weight(1f))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                TIMEZONES.forEachIndexed { i, tz ->
                    val col = cols[i % cols.size]
                    val off = if (tz.offset >= 0) "UTC+${tz.offset.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }}" else "UTC${tz.offset.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(col.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tz.code, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = col, modifier = Modifier.width(52.dp))
                        Text(tz.city, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text(times[tz.code] ?: "—", style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(off, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Tip Calculator ─────────────────────────────────────────────────
@Composable
fun TipCalculatorScreen(accent: Color, onBack: () -> Unit) {
    var bill   by remember { mutableStateOf("") }
    var tipPct by remember { mutableStateOf("15") }
    var split  by remember { mutableStateOf("1") }

    val result = remember(bill, tipPct, split) {
        try {
            val b = bill.toDouble(); val t = tipPct.toDouble(); val s = maxOf(1, split.toIntOrNull() ?: 1)
            val tipAmt = b * t / 100; val total = b + tipAmt; val perHead = total / s
            Triple(tipAmt, total, perHead)
        } catch (e: Exception) { null }
    }

    Scaffold(topBar = { SHVTopBar("Tip Calculator", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ValueInput(bill, { bill = it }, hint = "Bill amount", accent = accent)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10,12,15,18,20,25).forEach { pct ->
                            UnitChip("$pct%", tipPct == pct.toString(), accent) { tipPct = pct.toString() }
                        }
                    }
                    ValueInput(tipPct, { tipPct = it }, hint = "Tip %", accent = accent)
                    ValueInput(split, { split = it }, hint = "Split between (people)", accent = accent)
                }
            }
            if (result != null) {
                SHVCard(accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tip" to "%.2f".format(result.first), "Total" to "%.2f".format(result.second), "Per person" to "%.2f".format(result.third)).forEach { (l,v) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(l, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                                Text(v, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Discount / % ───────────────────────────────────────────────────
@Composable
fun DiscountScreen(accent: Color, onBack: () -> Unit) {
    var orig by remember { mutableStateOf("") }; var disc by remember { mutableStateOf("") }
    var xOfX by remember { mutableStateOf("") }; var xOfY by remember { mutableStateOf("") }
    var chgOld by remember { mutableStateOf("") }; var chgNew by remember { mutableStateOf("") }

    Scaffold(topBar = { SHVTopBar("Discount & %", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Block 1
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("X% OFF A PRICE", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    ValueInput(orig, { orig = it }, hint = "Original price", accent = accent)
                    ValueInput(disc, { disc = it }, hint = "Discount %", accent = accent)
                    val r = try { val o = orig.toDouble(); val d = disc.toDouble(); val s = o*d/100; "Save %.2f  →  Pay %.2f".format(s, o-s) } catch (e: Exception) { "" }
                    if (r.isNotEmpty()) Text(r, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            // Block 2
            SHVCard(accent = Blue) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WHAT % IS X OF Y?", style = MaterialTheme.typography.labelSmall, color = Blue, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    ValueInput(xOfX, { xOfX = it }, hint = "X (part)", accent = Blue)
                    ValueInput(xOfY, { xOfY = it }, hint = "Y (whole)", accent = Blue)
                    val r = try { "%.4g%%".format(xOfX.toDouble()/xOfY.toDouble()*100) } catch (e: Exception) { "" }
                    if (r.isNotEmpty()) Text(r, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Blue, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            // Block 3
            SHVCard(accent = Purple) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("% CHANGE (OLD → NEW)", style = MaterialTheme.typography.labelSmall, color = Purple, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    ValueInput(chgOld, { chgOld = it }, hint = "Old value", accent = Purple)
                    ValueInput(chgNew, { chgNew = it }, hint = "New value", accent = Purple)
                    val r = try { val c = (chgNew.toDouble()-chgOld.toDouble())/chgOld.toDouble()*100; "%s%.4g%%".format(if (c >= 0) "+" else "", c) } catch (e: Exception) { "" }
                    if (r.isNotEmpty()) Text(r, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (r.startsWith("+")) Green else Rose, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── BMI / BMR ──────────────────────────────────────────────────────
@Composable
fun BMIBMRScreen(accent: Color, onBack: () -> Unit) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age    by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var actIdx by remember { mutableStateOf(1) }
    val actFactors = listOf(1.2, 1.375, 1.55, 1.725, 1.9)
    val actLabels  = listOf("Sedentary","Light","Moderate","Active","Very Active")

    val result = remember(height, weight, age, isMale, actIdx) {
        try {
            val h = height.toDouble(); val w = weight.toDouble(); val a = age.toDouble()
            val hm = h/100; val bmi = w/(hm*hm)
            val cat = when { bmi < 18.5 -> "Underweight"; bmi < 25.0 -> "Normal"; bmi < 30.0 -> "Overweight"; else -> "Obese" }
            val bmr = if (isMale) 10*w+6.25*h-5*a+5 else 10*w+6.25*h-5*a-161
            val tdee = bmr * actFactors[actIdx]
            Triple(bmi to cat, bmr, tdee)
        } catch (e: Exception) { null }
    }

    Scaffold(topBar = { SHVTopBar("BMI & BMR", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ValueInput(height, { height = it }, hint = "Height (cm)", accent = accent)
                    ValueInput(weight, { weight = it }, hint = "Weight (kg)", accent = accent)
                    ValueInput(age, { age = it }, hint = "Age (years)", accent = accent)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitChip("Male", isMale, accent) { isMale = true }
                        UnitChip("Female", !isMale, accent) { isMale = false }
                    }
                    Text("ACTIVITY LEVEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        actLabels.forEachIndexed { i, l -> UnitChip(l, i == actIdx, accent) { actIdx = i } }
                    }
                }
            }
            if (result != null) {
                val (bmiPair, bmr, tdee) = result
                val (bmi, cat) = bmiPair
                val bmiColor = when (cat) { "Underweight" -> Blue; "Normal" -> Green; "Overweight" -> Orange; else -> Rose }
                SHVCard(accent = bmiColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BMI", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("%.1f".format(bmi), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = bmiColor)
                                Text(cat, style = MaterialTheme.typography.bodySmall, color = bmiColor)
                            }
                        }
                        HorizontalDivider(color = Border)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BMR", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            Text("%.0f kcal/day".format(bmr), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TDEE", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            Text("%.0f kcal/day".format(tdee), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Date & Age ─────────────────────────────────────────────────────
@Composable
fun DateAgeScreen(accent: Color, onBack: () -> Unit) {
    var dob  by remember { mutableStateOf("") }
    var d1   by remember { mutableStateOf("") }
    var d2   by remember { mutableStateOf("") }
    var ageResult  by remember { mutableStateOf("Enter date of birth") }
    var diffResult by remember { mutableStateOf("Enter two dates") }

    fun parseDate(s: String): java.util.Calendar? {
        val formats = listOf("yyyy-MM-dd","dd/MM/yyyy","dd-MM-yyyy","MM/dd/yyyy")
        for (fmt in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                sdf.isLenient = false
                val d = sdf.parse(s.trim()) ?: continue
                val c = java.util.Calendar.getInstance(); c.time = d; return c
            } catch (e: Exception) {}
        }
        return null
    }

    Scaffold(topBar = { SHVTopBar("Date & Age", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Age calculator
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AGE CALCULATOR", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    ValueInput(dob, { dob = it }, hint = "Date of birth (YYYY-MM-DD)", accent = accent)
                    Button(
                        onClick = {
                            val cal = parseDate(dob)
                            if (cal == null) { ageResult = "Invalid date format"; return@Button }
                            val now = java.util.Calendar.getInstance()
                            var years = now.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR)
                            val monthsBorn = cal.get(java.util.Calendar.MONTH)
                            val daysBorn = cal.get(java.util.Calendar.DAY_OF_MONTH)
                            if (now.get(java.util.Calendar.MONTH) < monthsBorn ||
                                (now.get(java.util.Calendar.MONTH) == monthsBorn && now.get(java.util.Calendar.DAY_OF_MONTH) < daysBorn))
                                years--
                            val diff = (now.timeInMillis - cal.timeInMillis) / 86400000L
                            ageResult = "$years years  ·  ${diff.toLocaleString()} days total"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) { Text("CALCULATE AGE", color = Black) }
                    Text(ageResult, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            // Date difference
            SHVCard(accent = Blue) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATE DIFFERENCE", style = MaterialTheme.typography.labelSmall, color = Blue, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    ValueInput(d1, { d1 = it }, hint = "Start date (YYYY-MM-DD)", accent = Blue)
                    ValueInput(d2, { d2 = it }, hint = "End date (YYYY-MM-DD)", accent = Blue)
                    Button(
                        onClick = {
                            val c1 = parseDate(d1); val c2 = parseDate(d2)
                            if (c1 == null || c2 == null) { diffResult = "Invalid date format"; return@Button }
                            val delta = Math.abs((c2.timeInMillis - c1.timeInMillis) / 86400000L)
                            val weeks = delta / 7; val days = delta % 7
                            diffResult = "${delta.toLocaleString()} days  ·  $weeks wk $days d  ·  ≈${delta/30} mo  ·  ≈${delta/365} yr"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    ) { Text("CALCULATE DIFFERENCE", color = Black) }
                    Text(diffResult, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

private fun Long.toLocaleString() = String.format("%,d", this)

// ── Blood Glucose ──────────────────────────────────────────────────
@Composable
fun BloodGlucoseScreen(accent: Color, onBack: () -> Unit) {
    var mgdl by remember { mutableStateOf("") }
    var mmol by remember { mutableStateOf("") }
    var converting by remember { mutableStateOf(false) }

    Scaffold(topBar = { SHVTopBar("Blood Glucose", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SHVCard(accent = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mgdl,
                        onValueChange = {
                            if (!converting) { mgdl = it; converting = true
                                mmol = try { "%.2f".format(it.toDouble()/18.018) } catch (e: Exception) { "" }
                                converting = false
                            }
                        },
                        label = { Text("mg/dL  (US/Canada)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = accent, unfocusedBorderColor = Border, cursorColor = accent, focusedContainerColor = Surface2, unfocusedContainerColor = Surface1),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = mmol,
                        onValueChange = {
                            if (!converting) { mmol = it; converting = true
                                mgdl = try { "%.1f".format(it.toDouble()*18.018) } catch (e: Exception) { "" }
                                converting = false
                            }
                        },
                        label = { Text("mmol/L  (UK/Europe)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Blue, unfocusedBorderColor = Border, cursorColor = Blue, focusedContainerColor = Surface2, unfocusedContainerColor = Surface1),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Reference: Fasting 70–99 mg/dL  (3.9–5.5 mmol/L)\nPost-meal: < 140 mg/dL  (< 7.8 mmol/L)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Shoe Sizes ─────────────────────────────────────────────────────
@Composable
fun ShoeSizeScreen(accent: Color, onBack: () -> Unit) {
    Scaffold(topBar = { SHVTopBar("Shoe Sizes", onBack = onBack) }, containerColor = Black) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("US M","US W","UK","EU","JP").forEach { h ->
                    Text(h, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val cols = listOf(Teal,Blue,Purple,Gold,Rose,Green,Cyan,Pink,Orange)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                SHOE_DATA.forEachIndexed { i, r ->
                    val col = cols[i % cols.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(col.copy(alpha = 0.07f))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf(r.usM, r.usW, r.uk, r.eu, r.jp).forEach { v ->
                            Text(v, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Ring Sizes ─────────────────────────────────────────────────────
@Composable
fun RingSizeScreen(accent: Color, onBack: () -> Unit) {
    Scaffold(topBar = { SHVTopBar("Ring Sizes", onBack = onBack) }, containerColor = Black) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("US","UK","EU","Diam mm").forEach { h ->
                    Text(h, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val cols = listOf(Gold,Teal,Blue,Purple,Rose,Green,Cyan,Pink,Orange)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                RING_DATA.forEachIndexed { i, r ->
                    val col = cols[i % cols.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(col.copy(alpha = 0.07f))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf(r.us, r.uk, r.eu, r.diam).forEach { v ->
                            Text(v, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Tire Size Decoder ──────────────────────────────────────────────
@Composable
fun TireSizeScreen(accent: Color, onBack: () -> Unit) {
    var tireInput by remember { mutableStateOf("") }

    data class TireResult(val key: String, val value: String)

    val results: List<TireResult> = remember(tireInput) {
        val raw = tireInput.trim().uppercase().replace(" ","")
        val m = Regex("(\\d{3})/(\\d{2})R(\\d{2}(?:\\.\\d)?)").find(raw)
        if (m == null) return@remember emptyList()
        try {
            val width = m.groupValues[1].toInt()
            val aspect = m.groupValues[2].toInt()
            val rim = m.groupValues[3].toDouble()
            val sidewall = width * aspect / 100.0
            val rimMm = rim * 25.4
            val diam = rimMm + 2 * sidewall
            val circ = Math.PI * diam
            listOf(
                TireResult("Width",        "$width mm  /  %.2f in".format(width/25.4)),
                TireResult("Aspect Ratio", "$aspect%"),
                TireResult("Rim",          "$rim\"  /  %.1f mm".format(rimMm)),
                TireResult("Sidewall",     "%.1f mm".format(sidewall)),
                TireResult("Total Ø",      "%.1f mm  /  %.2f in".format(diam, diam/25.4)),
                TireResult("Circumf",      "%.0f mm  /  %.1f in".format(circ, circ/25.4)),
                TireResult("Rev/km",       "%.0f".format(1000000/circ)),
            )
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(topBar = { SHVTopBar("Tire Size Decoder", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueInput(tireInput, { tireInput = it }, hint = "e.g. 205/55R16", accent = accent)
            if (results.isNotEmpty()) {
                SHVCard(accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.forEach { r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.key, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(r.value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else if (tireInput.isNotEmpty()) {
                Text("Format: 205/55R16", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Humidity / Dew Point ───────────────────────────────────────────
@Composable
fun HumidityScreen(accent: Color, onBack: () -> Unit) {
    var temp by remember { mutableStateOf("") }
    var rh   by remember { mutableStateOf("") }

    data class HumResult(val key: String, val value: String)

    val results: List<HumResult> = remember(temp, rh) {
        try {
            val T = temp.toDouble(); val RH = rh.toDouble()
            val a = 17.625; val b = 243.04
            val alpha = ln(RH/100) + a*T/(b+T)
            val td = b*alpha/(a-alpha)
            val psat = 6.1078 * 10.0.pow(7.5*T/(237.3+T))
            val ah = 216.7 * (RH/100 * psat / (273.15+T))
            listOf(
                HumResult("Dew Point", "%.1f °C  /  %.1f °F".format(td, td*9/5+32)),
                HumResult("Frost Point", "%.1f °C".format(td - 0.5*(T-td))),
                HumResult("Abs. Humidity", "%.2f g/m³".format(ah)),
                HumResult("Rel. Humidity", "%.1f%%".format(RH)),
            )
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(topBar = { SHVTopBar("Humidity", onBack = onBack) }, containerColor = Black) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueInput(temp, { temp = it }, hint = "Air temperature (°C)", accent = accent)
            ValueInput(rh, { rh = it }, hint = "Relative humidity (%)", accent = accent)
            if (results.isNotEmpty()) {
                SHVCard(accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.forEach { r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.key, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(r.value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// ── Clothing Sizes ─────────────────────────────────────────────────
private val CLOTHING_TABLES = mapOf(
    "Women's Tops" to mapOf(
        "Size" to listOf("XS","S","M","L","XL","XXL"),
        "US"   to listOf("0–2","4–6","8–10","12–14","16–18","20"),
        "EU"   to listOf("32–34","36–38","40–42","44–46","48–50","52"),
        "UK"   to listOf("4–6","8–10","12–14","16–18","20–22","24"),
    ),
    "Men's Tops" to mapOf(
        "Size" to listOf("XS","S","M","L","XL","XXL"),
        "US"   to listOf("34–36","38–40","42–44","46–48","50–52","54–56"),
        "EU"   to listOf("44–46","48–50","52–54","56–58","60–62","64–66"),
        "UK"   to listOf("34–36","38–40","42–44","46–48","50–52","54–56"),
    ),
    "Women's Shoes" to mapOf(
        "Size" to listOf("5","5.5","6","6.5","7","7.5","8","8.5","9","10"),
        "US"   to listOf("5","5.5","6","6.5","7","7.5","8","8.5","9","10"),
        "EU"   to listOf("35","35.5","36","37","37.5","38","38.5","39","40","41"),
        "UK"   to listOf("2.5","3","3.5","4","4.5","5","5.5","6","6.5","7.5"),
        "JP"   to listOf("22","22","23","23","23.5","24","24","24.5","25","25.5"),
    ),
    "Men's Shoes" to mapOf(
        "Size" to listOf("6","7","7.5","8","8.5","9","9.5","10","10.5","11","12"),
        "US"   to listOf("6","7","7.5","8","8.5","9","9.5","10","10.5","11","12"),
        "EU"   to listOf("39","40","40.5","41","42","42.5","43","44","44.5","45","46"),
        "UK"   to listOf("5.5","6","6.5","7","7.5","8","8.5","9","9.5","10","11"),
        "JP"   to listOf("24","25","25","26","26.5","27","27","28","28","28.5","29"),
    ),
)

@Composable
fun ClothingScreen(accent: Color, onBack: () -> Unit) {
    var category by remember { mutableStateOf(CLOTHING_TABLES.keys.first()) }
    val colMap = mapOf("Size" to Teal, "US" to Blue, "EU" to Purple, "UK" to Gold, "JP" to Rose, "IT" to Green)

    Scaffold(topBar = { SHVTopBar("Clothing & Shoe Sizes", onBack = onBack) }, containerColor = Black) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp)) {
                CLOTHING_TABLES.keys.forEach { cat ->
                    UnitChip(cat, cat == category, accent) { category = cat }
                }
            }
            val table = CLOTHING_TABLES[category] ?: return@Column
            val headers = table.keys.toList()
            val rows = table.values.first().size

            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                // Header
                Row(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 8.dp)) {
                    headers.forEach { h ->
                        Text(h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colMap[h] ?: Teal, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                repeat(rows) { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i % 2 == 0) Surface1 else Surface2)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        headers.forEach { h ->
                            Text(table[h]?.getOrNull(i) ?: "—", style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Currency ───────────────────────────────────────────────────────
// Live-rate state: null = loading in progress, non-null = settled (live or fallback)
private enum class RateStatus { LOADING, LIVE, OFFLINE }

@Composable
fun CurrencyConverterScreen(accent: Color, onBack: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    // null rates = initial loading; we don't show stale fallback numbers during that phase
    var rates by remember { mutableStateOf<Map<String, Double>?>(null) }
    var rateStatus by remember { mutableStateOf(RateStatus.LOADING) }
    var lastUpdated by remember { mutableStateOf("") }
    val favourites = remember { mutableStateListOf("USD","EUR","GBP","JPY","AUD","CAD") }
    var refreshKey by remember { mutableStateOf(0) }

    // Fetch live rates – re-runs whenever refreshKey changes (manual refresh)
    LaunchedEffect(refreshKey) {
        rateStatus = RateStatus.LOADING
        rates = null
        try {
            // Network MUST be off the main thread; withContext(IO) suspends here
            // and resumes on the main thread once done — no NetworkOnMainThreadException
            val newRates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val url = java.net.URL("https://open.er-api.com/v6/latest/USD")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().readText()
                val jsonRates = org.json.JSONObject(text).getJSONObject("rates")
                val result = mutableMapOf<String, Double>()
                CURRENCIES.forEach { c ->
                    if (jsonRates.has(c.code)) result[c.code] = jsonRates.getDouble(c.code)
                }
                result
            }
            if (newRates.isNotEmpty()) {
                rates = newRates
                rateStatus = RateStatus.LIVE
                lastUpdated = java.text.SimpleDateFormat("HH:mm · dd MMM", java.util.Locale.getDefault()).format(java.util.Date())
            } else {
                rates = CURRENCY_FALLBACK
                rateStatus = RateStatus.OFFLINE
            }
        } catch (e: Exception) {
            rates = CURRENCY_FALLBACK
            rateStatus = RateStatus.OFFLINE
        }
    }

    val allCodes = CURRENCIES.map { it.code }
    val ordered = (favourites + allCodes.filter { it !in favourites })
    val settledRates = rates
    val fromRate = settledRates?.get(fromCurrency) ?: 1.0
    val amountVal = amount.toDoubleOrNull()

    Scaffold(
        topBar = { SHVTopBar("Currency", onBack = onBack) },
        containerColor = Black,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Status bar ──────────────────────────────────────────
            Column(Modifier.padding(horizontal = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when (rateStatus) {
                            RateStatus.LOADING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Teal,
                                    strokeWidth = 1.5.dp,
                                )
                                Text("Fetching live rates…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            RateStatus.LIVE -> {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(Green, RoundedCornerShape(4.dp))
                                )
                                Text("Live rates · $lastUpdated", style = MaterialTheme.typography.bodySmall, color = Green)
                            }
                            RateStatus.OFFLINE -> {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(Orange, RoundedCornerShape(4.dp))
                                )
                                Text("Offline · Fallback rates", style = MaterialTheme.typography.bodySmall, color = Orange)
                            }
                        }
                    }
                    // Manual refresh button
                    IconButton(
                        onClick = { refreshKey++ },
                        modifier = Modifier.size(30.dp),
                        enabled = rateStatus != RateStatus.LOADING,
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh rates",
                            tint = if (rateStatus != RateStatus.LOADING) Teal else TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("FROM", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ordered.take(10).forEach { code ->
                        UnitChip(code, code == fromCurrency, accent) { fromCurrency = code }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ValueInput(amount, { amount = it }, hint = "Enter amount…", accent = accent)
            }

            // ── Results list ────────────────────────────────────────
            if (settledRates == null) {
                // Full-screen loading skeleton while first fetch is in flight
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = Teal, modifier = Modifier.size(36.dp))
                        Text("Loading live exchange rates…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("★ FAVOURITES", style = MaterialTheme.typography.labelSmall, color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(6.dp))
                    val cols = listOf(Teal,Blue,Purple,Gold,Rose,Green,Cyan,Pink,Orange)
                    ordered.forEachIndexed { i, code ->
                        if (i == favourites.size) {
                            Spacer(Modifier.height(10.dp))
                            Text("ALL CURRENCIES", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                        val info = CURRENCIES.find { it.code == code }
                        val toRate = settledRates[code] ?: 1.0
                        val converted = if (amountVal != null) amountVal / fromRate * toRate else null
                        val col = cols[i % cols.size]
                        val isFav = code in favourites
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(col.copy(alpha = 0.07f))
                                .clickable { fromCurrency = code }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.background(col.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 3.dp)) {
                                Text(info?.country ?: code.take(2), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = col)
                            }
                            Text(code, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = col, modifier = Modifier.width(38.dp))
                            Text(
                                if (converted != null) "%.4f".format(converted) else "—",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                if (isFav) favourites.remove(code) else favourites.add(0, code)
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(if (isFav) Icons.Rounded.Star else Icons.Rounded.StarOutline, contentDescription = null, tint = if (isFav) Gold else TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Text("ⓘ Mid-market rates · Not bank rates · For reference only", style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
