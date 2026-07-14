package com.shvertex.universalconv.data

import kotlin.math.*

// ── Unit definition ────────────────────────────────────────────────
data class UnitDef(val label: String, val factor: Double)

// ── Conversion result ──────────────────────────────────────────────
data class ConvResult(val label: String, val value: String)

// ── Generic linear converter ───────────────────────────────────────
fun linearConvert(value: Double, fromUnit: String, units: List<UnitDef>): List<ConvResult> {
    val fromFactor = units.first { it.label == fromUnit }.factor
    return units.map { u ->
        val result = value * fromFactor / u.factor
        ConvResult(u.label, formatNum(result))
    }
}

fun formatNum(n: Double): String {
    if (n.isInfinite() || n.isNaN()) return "—"
    if (n == 0.0) return "0"
    val abs = abs(n)
    return when {
        abs >= 1e15 || (abs < 1e-9 && abs > 0) -> String.format("%.4e", n)
        else -> {
            val s = "%.12f".format(n)
            s.trimEnd('0').trimEnd('.')
        }
    }
}

// ── Unit registries ────────────────────────────────────────────────

val LENGTH_UNITS = listOf(
    UnitDef("m",     1.0),      UnitDef("km",   1000.0),   UnitDef("cm",  0.01),
    UnitDef("mm",    0.001),    UnitDef("µm",   1e-6),     UnitDef("nm",  1e-9),
    UnitDef("mi",    1609.344), UnitDef("yd",   0.9144),   UnitDef("ft",  0.3048),
    UnitDef("in",    0.0254),   UnitDef("nmi",  1852.0),   UnitDef("ly",  9.4607e15),
    UnitDef("AU",    1.496e11),
)

val WEIGHT_UNITS = listOf(
    UnitDef("kg",   1.0),     UnitDef("g",    0.001),    UnitDef("mg",  1e-6),
    UnitDef("µg",   1e-9),    UnitDef("t",    1000.0),   UnitDef("lb",  0.45359237),
    UnitDef("oz",   0.028349523125), UnitDef("st", 6.35029318), UnitDef("ct", 0.0002),
    UnitDef("gr",   6.479891e-5),
)

val VOLUME_UNITS = listOf(
    UnitDef("L",      1.0),      UnitDef("mL",     0.001),     UnitDef("m³",   1000.0),
    UnitDef("cm³",    0.001),    UnitDef("ft³",    28.316847), UnitDef("in³",  0.016387064),
    UnitDef("gal US", 3.785412), UnitDef("gal UK", 4.54609),   UnitDef("qt",   0.946353),
    UnitDef("pt",     0.473176), UnitDef("cup",    0.236588),  UnitDef("fl oz",0.0295735),
    UnitDef("tbsp",   0.0147868),UnitDef("tsp",    0.00492892),UnitDef("bbl",  158.987),
)

val AREA_UNITS = listOf(
    UnitDef("m²",    1.0),       UnitDef("km²",  1e6),       UnitDef("cm²", 1e-4),
    UnitDef("mm²",   1e-6),      UnitDef("ha",   1e4),       UnitDef("ac",  4046.856),
    UnitDef("ft²",   0.092903),  UnitDef("in²",  6.4516e-4), UnitDef("yd²", 0.836127),
    UnitDef("mi²",   2589988.11),
)

val TIME_UNITS = listOf(
    UnitDef("s",     1.0),       UnitDef("ns",   1e-9),      UnitDef("µs",   1e-6),
    UnitDef("ms",    0.001),     UnitDef("min",  60.0),      UnitDef("h",    3600.0),
    UnitDef("day",   86400.0),   UnitDef("wk",   604800.0),  UnitDef("mo",   2629800.0),
    UnitDef("yr",    31557600.0),UnitDef("dec",  315576000.0),UnitDef("cent",3155760000.0),
)

val SPEED_UNITS = listOf(
    UnitDef("m/s",   1.0),    UnitDef("km/h", 1/3.6),  UnitDef("mph",   0.44704),
    UnitDef("ft/s",  0.3048), UnitDef("knot", 0.514444), UnitDef("mach", 343.0),
    UnitDef("c",     299792458.0),
)

val PRESSURE_UNITS = listOf(
    UnitDef("Pa",   1.0),     UnitDef("kPa",  1e3),    UnitDef("MPa",  1e6),
    UnitDef("bar",  1e5),     UnitDef("mbar", 100.0),  UnitDef("psi",  6894.757),
    UnitDef("atm",  101325.0),UnitDef("mmHg", 133.322),UnitDef("inHg", 3386.39),
    UnitDef("torr", 133.322),
)

val ENERGY_UNITS = listOf(
    UnitDef("J",    1.0),       UnitDef("kJ",   1e3),    UnitDef("MJ",    1e6),
    UnitDef("cal",  4.184),     UnitDef("kcal", 4184.0), UnitDef("Wh",    3600.0),
    UnitDef("kWh",  3600000.0), UnitDef("MWh",  3.6e9),  UnitDef("BTU",   1055.056),
    UnitDef("eV",   1.602177e-19),UnitDef("erg", 1e-7),  UnitDef("ft·lb", 1.35582),
)

val POWER_UNITS = listOf(
    UnitDef("W",      1.0),      UnitDef("kW",    1000.0),  UnitDef("MW",    1e6),
    UnitDef("GW",     1e9),      UnitDef("hp",    745.69987),UnitDef("hp(m)", 735.49875),
    UnitDef("BTU/h",  0.29307107),UnitDef("cal/s", 4.184),  UnitDef("kcal/h",1.163),
)

val TORQUE_UNITS = listOf(
    UnitDef("Nm",   1.0),   UnitDef("lb·ft", 1.35582), UnitDef("kg·m", 9.80665),
    UnitDef("oz·in",0.00706155),
)

val ACCELERATION_UNITS = listOf(
    UnitDef("m/s²",  1.0),   UnitDef("ft/s²", 0.3048), UnitDef("g",    9.80665),
    UnitDef("Gal",   0.01),  UnitDef("in/s²", 0.0254),
)

val FORCE_UNITS = listOf(
    UnitDef("N",    1.0),         UnitDef("kN",   1e3),       UnitDef("MN",   1e6),
    UnitDef("lbf",  4.4482216),   UnitDef("kgf",  9.80665),   UnitDef("dyne", 1e-5),
    UnitDef("kip",  4448.2216),   UnitDef("ozf",  0.27801385),
)

val DENSITY_UNITS = listOf(
    UnitDef("kg/m³",   1.0),      UnitDef("g/cm³",  1000.0),  UnitDef("g/mL",   1000.0),
    UnitDef("kg/L",    1000.0),   UnitDef("lb/ft³", 16.018463),UnitDef("lb/in³", 27679.905),
    UnitDef("t/m³",    1000.0),
)

val FLOW_UNITS = listOf(
    UnitDef("L/s",    1.0),       UnitDef("L/min",  1/60.0),    UnitDef("L/h",   1/3600.0),
    UnitDef("m³/s",   1000.0),    UnitDef("m³/h",   1000/3600.0),UnitDef("mL/s", 0.001),
    UnitDef("GPM",    0.0630902), UnitDef("CFM",    0.471947),   UnitDef("CFS",   28.3168),
)

val VISCOSITY_UNITS = listOf(
    UnitDef("Pa·s",  1.0),    UnitDef("mPa·s", 1e-3),  UnitDef("cP",  1e-3),
    UnitDef("P",     0.1),    UnitDef("lb/(ft·s)", 1.48816),
)

val ANGLE_UNITS = listOf(
    UnitDef("°",      1.0),          UnitDef("rad",   180.0/PI), UnitDef("grad",   0.9),
    UnitDef("arcmin", 1/60.0),       UnitDef("arcsec",1/3600.0), UnitDef("rev",    360.0),
    UnitDef("mrad",   180.0/(PI*1000)),
)

val FREQUENCY_UNITS = listOf(
    UnitDef("Hz",    1.0),   UnitDef("kHz",  1e3),   UnitDef("MHz",  1e6),
    UnitDef("GHz",   1e9),   UnitDef("THz",  1e12),  UnitDef("rpm",  1/60.0),
    UnitDef("mHz",   0.001),
)

val MAGNETIC_UNITS = listOf(
    UnitDef("T",   1.0),    UnitDef("mT",  1e-3),  UnitDef("µT",  1e-6),
    UnitDef("nT",  1e-9),   UnitDef("G",   1e-4),  UnitDef("mG",  1e-7),
    UnitDef("Oe",  79.5775),
)

val DATA_STORAGE_UNITS = listOf(
    UnitDef("B",    1.0),     UnitDef("KB",   1e3),     UnitDef("MB",   1e6),
    UnitDef("GB",   1e9),     UnitDef("TB",   1e12),    UnitDef("PB",   1e15),
    UnitDef("KiB",  1024.0),  UnitDef("MiB",  1048576.0),UnitDef("GiB", 1073741824.0),
    UnitDef("TiB",  1099511627776.0),UnitDef("bit",0.125),UnitDef("Kbit",125.0),
    UnitDef("Mbit", 125000.0),UnitDef("Gbit", 125000000.0),
)

val TYPOGRAPHY_UNITS = listOf(
    UnitDef("px",   1.0),       UnitDef("pt",   96/72.0), UnitDef("pc",  16.0),
    UnitDef("in",   96.0),      UnitDef("cm",   96/2.54), UnitDef("mm",  96/25.4),
    UnitDef("em",   16.0),      UnitDef("rem",  16.0),    UnitDef("dp",  1.0),
    UnitDef("sp",   1.0),
)

val ILLUMINANCE_UNITS = listOf(
    UnitDef("lux",    1.0),    UnitDef("fc",     10.7639), UnitDef("phot",  10000.0),
    UnitDef("nox",    0.001),  UnitDef("lm/m²",  1.0),    UnitDef("lm/ft²",10.7639),
)

val LUMINANCE_UNITS = listOf(
    UnitDef("cd/m²",  1.0),    UnitDef("nit",    1.0),    UnitDef("fL",    3.42626),
    UnitDef("L",      3183.099),UnitDef("sb",    10000.0), UnitDef("mcd/m²",1e-3),
    UnitDef("kcd/m²", 1e3),
)

val RADIOACTIVITY_UNITS = listOf(
    UnitDef("Bq",   1.0),    UnitDef("kBq",  1e3),   UnitDef("MBq",  1e6),
    UnitDef("GBq",  1e9),    UnitDef("Ci",   3.7e10), UnitDef("mCi",  3.7e7),
    UnitDef("µCi",  3.7e4),  UnitDef("Gy",   1.0),   UnitDef("mGy",  1e-3),
    UnitDef("rad",  0.01),   UnitDef("Sv",   1.0),   UnitDef("mSv",  1e-3),
    UnitDef("rem",  0.01),
)

val RAD_DOSE_UNITS = listOf(
    UnitDef("Sv",   1.0),    UnitDef("mSv",  1e-3),  UnitDef("µSv",  1e-6),
    UnitDef("rem",  0.01),   UnitDef("mrem", 1e-4),  UnitDef("Gy",   1.0),
    UnitDef("mGy",  1e-3),   UnitDef("rad",  0.01),  UnitDef("J/kg", 1.0),
)

val CONCENTRATION_UNITS = listOf(
    UnitDef("mol/L",    1.0),    UnitDef("mmol/L", 1e-3),  UnitDef("µmol/L", 1e-6),
    UnitDef("nmol/L",   1e-9),   UnitDef("mol/m³", 1e-3),  UnitDef("g/L",    1.0),
    UnitDef("mg/L",     1e-3),   UnitDef("µg/L",   1e-6),  UnitDef("ppm",    1e-3),
    UnitDef("ppb",      1e-6),   UnitDef("ppt",    1e-9),  UnitDef("% v/v",  10.0),
)

val COOKING_UNITS = listOf(
    UnitDef("mL",      1.0),     UnitDef("L",       1000.0),  UnitDef("tsp",    4.92892),
    UnitDef("tbsp",    14.7868), UnitDef("fl oz",    29.5735), UnitDef("cup",    236.588),
    UnitDef("pt",      473.176), UnitDef("qt",       946.353), UnitDef("gal",    3785.41),
    UnitDef("g",       1.0),     UnitDef("kg",       1000.0),  UnitDef("oz",     28.3495),
    UnitDef("lb",      453.592),
)

// ── Temperature (special – no linear factor) ──────────────────────
val TEMP_UNITS = listOf("°C","°F","K","°R","°Ré","°Rø","°N","°De")

fun convertTemperature(value: Double, from: String, to: String): Double {
    val c = when (from) {
        "°C"  -> value
        "°F"  -> (value - 32) * 5/9
        "K"   -> value - 273.15
        "°R"  -> (value - 491.67) * 5/9
        "°Ré" -> value * 5/4
        "°Rø" -> (value - 7.5) * 40/21
        "°N"  -> value * 100/33
        "°De" -> 100 - value * 2/3
        else  -> value
    }
    return when (to) {
        "°C"  -> c
        "°F"  -> c * 9/5 + 32
        "K"   -> c + 273.15
        "°R"  -> (c + 273.15) * 9/5
        "°Ré" -> c * 4/5
        "°Rø" -> c * 21/40 + 7.5
        "°N"  -> c * 33/100
        "°De" -> (100 - c) * 3/2
        else  -> c
    }
}

// ── Fuel economy (special) ─────────────────────────────────────────
val FUEL_UNITS = listOf("km/L","L/100km","mpg US","mpg UK","mi/L","km/gal US")

fun convertFuel(value: Double, from: String, to: String): Double {
    val kml = when (from) {
        "km/L"      -> value
        "L/100km"   -> if (value == 0.0) 0.0 else 100.0 / value
        "mpg US"    -> value * 0.425144
        "mpg UK"    -> value * 0.354006
        "mi/L"      -> value * 1.60934
        "km/gal US" -> value / 3.78541
        else -> value
    }
    return when (to) {
        "km/L"      -> kml
        "L/100km"   -> if (kml == 0.0) 0.0 else 100.0 / kml
        "mpg US"    -> kml / 0.425144
        "mpg UK"    -> kml / 0.354006
        "mi/L"      -> kml / 1.60934
        "km/gal US" -> kml * 3.78541
        else -> kml
    }
}

// ── Sound (logarithmic) ────────────────────────────────────────────
val SOUND_UNITS = listOf("dB","dBm","dBW","Np")

fun convertSound(value: Double, from: String, to: String): Double {
    val dbw = when (from) {
        "dBm" -> value - 30.0
        "dBW" -> value
        "Np"  -> value * 20.0 / ln(10.0)
        else  -> value
    }
    return when (to) {
        "dBm" -> dbw + 30.0
        "dBW" -> dbw
        "Np"  -> dbw * ln(10.0) / 20.0
        else  -> dbw
    }
}

// ── Electric units (multi-group) ───────────────────────────────────
data class ElectricGroup(val label: String, val units: List<UnitDef>)

val ELECTRIC_GROUPS = listOf(
    ElectricGroup("Voltage",    listOf(UnitDef("V",1.0),UnitDef("mV",1e-3),UnitDef("µV",1e-6),UnitDef("kV",1e3),UnitDef("MV",1e6))),
    ElectricGroup("Current",    listOf(UnitDef("A",1.0),UnitDef("mA",1e-3),UnitDef("µA",1e-6),UnitDef("kA",1e3),UnitDef("nA",1e-9))),
    ElectricGroup("Resistance", listOf(UnitDef("Ω",1.0),UnitDef("mΩ",1e-3),UnitDef("kΩ",1e3),UnitDef("MΩ",1e6),UnitDef("GΩ",1e9))),
)

// ── Currency (live rates + fallback) ──────────────────────────────
data class CurrencyInfo(val code: String, val country: String)

val CURRENCIES = listOf(
    CurrencyInfo("USD","US"), CurrencyInfo("EUR","EU"), CurrencyInfo("GBP","GB"),
    CurrencyInfo("JPY","JP"), CurrencyInfo("CNY","CN"), CurrencyInfo("INR","IN"),
    CurrencyInfo("CAD","CA"), CurrencyInfo("AUD","AU"), CurrencyInfo("CHF","CH"),
    CurrencyInfo("BRL","BR"), CurrencyInfo("MXN","MX"), CurrencyInfo("SGD","SG"),
    CurrencyInfo("HKD","HK"), CurrencyInfo("NOK","NO"), CurrencyInfo("SEK","SE"),
    CurrencyInfo("DKK","DK"), CurrencyInfo("NZD","NZ"), CurrencyInfo("ZAR","ZA"),
    CurrencyInfo("RUB","RU"), CurrencyInfo("TRY","TR"), CurrencyInfo("KRW","KR"),
    CurrencyInfo("IDR","ID"), CurrencyInfo("SAR","SA"), CurrencyInfo("AED","AE"),
    CurrencyInfo("PLN","PL"), CurrencyInfo("THB","TH"), CurrencyInfo("MYR","MY"),
    CurrencyInfo("PHP","PH"), CurrencyInfo("EGP","EG"), CurrencyInfo("PKR","PK"),
    CurrencyInfo("LKR","LK"), CurrencyInfo("CZK","CZ"), CurrencyInfo("HUF","HU"),
    CurrencyInfo("RON","RO"), CurrencyInfo("BGN","BG"), CurrencyInfo("VND","VN"),
    CurrencyInfo("BDT","BD"), CurrencyInfo("NGN","NG"), CurrencyInfo("KES","KE"),
    CurrencyInfo("UAH","UA"), CurrencyInfo("CLP","CL"), CurrencyInfo("COP","CO"),
    CurrencyInfo("ILS","IL"), CurrencyInfo("TWD","TW"), CurrencyInfo("QAR","QA"),
    CurrencyInfo("KWD","KW"), CurrencyInfo("BHD","BH"), CurrencyInfo("OMR","OM"),
)

val CURRENCY_FALLBACK = mapOf(
    "USD" to 1.0,   "EUR" to 0.92,  "GBP" to 0.79,  "JPY" to 149.5, "CNY" to 7.24,
    "INR" to 83.1,  "CAD" to 1.36,  "AUD" to 1.53,  "CHF" to 0.9,   "BRL" to 4.97,
    "MXN" to 17.15, "SGD" to 1.34,  "HKD" to 7.82,  "NOK" to 10.6,  "SEK" to 10.4,
    "DKK" to 6.89,  "NZD" to 1.63,  "ZAR" to 18.9,  "RUB" to 91.5,  "TRY" to 32.3,
    "KRW" to 1330.0, "IDR" to 15700.0, "SAR" to 3.75, "AED" to 3.67,  "PLN" to 4.03,
    "THB" to 35.1,   "MYR" to 4.72,   "PHP" to 56.3, "EGP" to 30.9,  "PKR" to 278.0,
    "LKR" to 365.0,  "CZK" to 22.8,   "HUF" to 357.0,"RON" to 4.58,  "BGN" to 1.80,
    "VND" to 24500.0,"BDT" to 110.0,  "NGN" to 1480.0,"KES" to 130.0,"UAH" to 37.5,
    "CLP" to 930.0, "COP" to 3900.0,"ILS" to 3.65,  "TWD" to 31.5,  "QAR" to 3.64,
    "KWD" to 0.307, "BHD" to 0.377, "OMR" to 0.385,
)

// ── Numeral helpers ────────────────────────────────────────────────
fun intToRoman(n: Int): String {
    if (n <= 0 || n > 3999) return "Out of range (1–3999)"
    val vals = intArrayOf(1000,900,500,400,100,90,50,40,10,9,5,4,1)
    val syms  = arrayOf("M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I")
    var num = n; val sb = StringBuilder()
    for (i in vals.indices) while (num >= vals[i]) { sb.append(syms[i]); num -= vals[i] }
    return sb.toString()
}

fun romanToInt(s: String): Int {
    val map = mapOf('I' to 1,'V' to 5,'X' to 10,'L' to 50,'C' to 100,'D' to 500,'M' to 1000)
    var result = 0; var prev = 0
    for (ch in s.uppercase().reversed()) {
        val v = map[ch] ?: throw IllegalArgumentException("Invalid: $ch")
        if (v < prev) result -= v else result += v
        prev = v
    }
    return result
}

// ── Paper sizes ─────────────────────────────────────────────────────
data class PaperSize(val name: String, val wMm: Int, val hMm: Int)

val PAPER_SIZES = listOf(
    PaperSize("A0",841,1189), PaperSize("A1",594,841),  PaperSize("A2",420,594),
    PaperSize("A3",297,420),  PaperSize("A4",210,297),  PaperSize("A5",148,210),
    PaperSize("A6",105,148),  PaperSize("A7",74,105),
    PaperSize("B0",1000,1414),PaperSize("B1",707,1000), PaperSize("B2",500,707),
    PaperSize("B3",353,500),  PaperSize("B4",250,353),  PaperSize("B5",176,250),
    PaperSize("Letter",216,279),PaperSize("Legal",216,356),PaperSize("Tabloid",279,432),
    PaperSize("Executive",184,267),PaperSize("Half Letter",140,216),
    PaperSize("C4 Env",229,324),PaperSize("C5 Env",162,229),PaperSize("DL Env",110,220),
)

// ── Time zones ─────────────────────────────────────────────────────
data class TimeZone(val code: String, val offset: Double, val city: String)

val TIMEZONES = listOf(
    TimeZone("UTC",0.0,"Universal"),   TimeZone("GMT",0.0,"London GMT"),
    TimeZone("BST",1.0,"London BST"),  TimeZone("CET",1.0,"Paris/Berlin"),
    TimeZone("EET",2.0,"Athens"),      TimeZone("MSK",3.0,"Moscow"),
    TimeZone("GST",4.0,"Dubai"),       TimeZone("PKT",5.0,"Karachi"),
    TimeZone("IST",5.5,"India"),       TimeZone("BST+6",6.0,"Dhaka"),
    TimeZone("WIB",7.0,"Jakarta"),     TimeZone("CST+8",8.0,"Beijing"),
    TimeZone("JST",9.0,"Tokyo"),       TimeZone("AEST",10.0,"Sydney"),
    TimeZone("NZST",12.0,"Auckland"),  TimeZone("EST",-5.0,"New York"),
    TimeZone("EDT",-4.0,"NY Summer"),  TimeZone("CST",-6.0,"Chicago"),
    TimeZone("MST",-7.0,"Denver"),     TimeZone("PST",-8.0,"Los Angeles"),
    TimeZone("PDT",-7.0,"LA Summer"),  TimeZone("AKST",-9.0,"Anchorage"),
    TimeZone("HST",-10.0,"Honolulu"),  TimeZone("ART",-3.0,"Buenos Aires"),
    TimeZone("BRT",-3.0,"Brasilia"),   TimeZone("CAT",2.0,"Nairobi"),
    TimeZone("WAT",1.0,"Lagos"),       TimeZone("HKT",8.0,"Hong Kong"),
)

// ── Shoe sizes ─────────────────────────────────────────────────────
data class ShoeRow(val usM: String, val usW: String, val uk: String, val eu: String, val jp: String)
val SHOE_DATA = listOf(
    ShoeRow("6","7.5","5",  "38","24"), ShoeRow("6.5","8","5.5","39","24.5"),
    ShoeRow("7","8.5","6",  "40","25"), ShoeRow("7.5","9","6.5","40","25.5"),
    ShoeRow("8","9.5","7",  "41","26"), ShoeRow("8.5","10","7.5","42","26.5"),
    ShoeRow("9","10.5","8", "42","27"), ShoeRow("9.5","11","8.5","43","27.5"),
    ShoeRow("10","11.5","9","44","28"), ShoeRow("10.5","12","9.5","44","28.5"),
    ShoeRow("11","12.5","10","45","29"),ShoeRow("12","13.5","11","46","30"),
    ShoeRow("13","14.5","12","47","31"),
)

// ── Ring sizes ─────────────────────────────────────────────────────
data class RingRow(val us: String, val uk: String, val eu: String, val diam: String)
val RING_DATA = listOf(
    RingRow("4","H","46.8","14.9"),  RingRow("4.5","I","47.8","15.2"),
    RingRow("5","J","49.3","15.7"),  RingRow("5.5","K","50.6","16.1"),
    RingRow("6","L","51.9","16.5"),  RingRow("6.5","M","53.1","16.9"),
    RingRow("7","N","54.4","17.3"),  RingRow("7.5","O","55.7","17.7"),
    RingRow("8","P","57.0","18.1"),  RingRow("8.5","Q","58.3","18.6"),
    RingRow("9","R","59.5","18.9"),  RingRow("9.5","S","60.8","19.4"),
    RingRow("10","T","62.1","19.8"), RingRow("10.5","U","63.4","20.2"),
    RingRow("11","V","64.6","20.6"), RingRow("12","X","67.2","21.4"),
)
