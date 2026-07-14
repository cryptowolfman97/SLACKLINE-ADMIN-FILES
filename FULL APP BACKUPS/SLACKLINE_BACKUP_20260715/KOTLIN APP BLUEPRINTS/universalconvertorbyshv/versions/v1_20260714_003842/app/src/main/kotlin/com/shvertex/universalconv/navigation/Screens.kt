package com.shvertex.universalconv.navigation

import androidx.compose.ui.graphics.Color
import com.shvertex.universalconv.ui.theme.*

// ── Screen routes ──────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Settings    : Screen("settings")
    object About       : Screen("about")
    object Converter   : Screen("converter/{moduleId}") {
        fun create(id: String) = "converter/$id"
    }
}

// ── Converter categories ───────────────────────────────────────────
enum class ConverterCategory(val label: String, val accent: Color) {
    EVERYDAY          ("Everyday",               Teal),
    SCIENCE           ("Science & Engineering",  Gold),
    ELECTRONICS       ("Electronics & Digital",  Blue),
    LIGHT             ("Light & Optics",          Color(0xFFFFE144)),
    PRINT             ("Printing & Design",       Purple),
    TOOLS             ("Everyday Tools",          Green),
}

// ── Module descriptor ──────────────────────────────────────────────
data class ConverterModule(
    val id       : String,
    val title    : String,
    val subtitle : String,
    val icon     : String,
    val accent   : Color,
    val category : ConverterCategory,
    val keywords : String = "",
)

val ALL_MODULES = listOf(
    // EVERYDAY
    ConverterModule("length",       "Length",         "km · mi · ft · m",       "📏", Color(0xFF6699FF), ConverterCategory.EVERYDAY,    "length distance km mi ft meter inch"),
    ConverterModule("weight",       "Weight",         "kg · lb · oz · g",       "⚖️", Color(0xFFFF9955), ConverterCategory.EVERYDAY,    "weight mass kg lb oz gram"),
    ConverterModule("temperature",  "Temperature",    "°C · °F · K",            "🌡️", Color(0xFFFF5566), ConverterCategory.EVERYDAY,    "temperature celsius fahrenheit kelvin"),
    ConverterModule("volume",       "Volume",         "L · mL · gal",           "🧪", Color(0xFF44CCFF), ConverterCategory.EVERYDAY,    "volume liter gallon ml cup"),
    ConverterModule("area",         "Area",           "m² · ft² · ha",          "⬜", Color(0xFFFFBB33), ConverterCategory.EVERYDAY,    "area square meter acre hectare"),
    ConverterModule("time",         "Time",           "s · min · h · yr",       "⏱️", Color(0xFFAA88FF), ConverterCategory.EVERYDAY,    "time second minute hour day year"),
    ConverterModule("speed",        "Speed",          "km/h · mph · m/s",       "💨", Color(0xFF44EE77), ConverterCategory.EVERYDAY,    "speed velocity kmh mph knot"),
    ConverterModule("currency",     "Currency",       "USD · EUR · GBP",        "💱", Color(0xFF44CC88), ConverterCategory.EVERYDAY,    "currency money usd eur gbp forex"),
    ConverterModule("fuel",         "Fuel Economy",   "km/L · L/100km",         "⛽", Color(0xFFFF6666), ConverterCategory.EVERYDAY,    "fuel mpg economy"),
    ConverterModule("cooking",      "Cooking",        "cup · tbsp · mL",        "🍳", Color(0xFFFF8844), ConverterCategory.EVERYDAY,    "cooking recipe cup tablespoon"),
    ConverterModule("clothing",     "Clothing",       "US · EU · UK · JP",      "👗", Color(0xFFDD66EE), ConverterCategory.EVERYDAY,    "clothing apparel size"),
    // SCIENCE
    ConverterModule("pressure",     "Pressure",       "Pa · bar · psi",         "🔵", Color(0xFFFF66BB), ConverterCategory.SCIENCE,     "pressure pascal bar psi atm"),
    ConverterModule("energy",       "Energy",         "J · cal · kWh",          "⚡", Color(0xFFFFCC00), ConverterCategory.SCIENCE,     "energy joule calorie kwh"),
    ConverterModule("power",        "Power",          "W · kW · hp",            "🔌", Color(0xFF00EE99), ConverterCategory.SCIENCE,     "power watt kilowatt horsepower"),
    ConverterModule("torque",       "Torque",         "Nm · lb·ft",             "🔧", Color(0xFFFF8800), ConverterCategory.SCIENCE,     "torque newton meter"),
    ConverterModule("acceleration", "Acceleration",   "m/s² · g",               "🚀", Color(0xFF9966FF), ConverterCategory.SCIENCE,     "acceleration gravity g-force"),
    ConverterModule("force",        "Force",          "N · kN · lbf",           "💪", Color(0xFFFF4444), ConverterCategory.SCIENCE,     "force newton pound"),
    ConverterModule("density",      "Density",        "kg/m³ · g/cm³",          "🔴", Color(0xFF66DD88), ConverterCategory.SCIENCE,     "density kg/m3"),
    ConverterModule("flowrate",     "Flow Rate",      "L/s · GPM",              "🌊", Color(0xFF44AAFF), ConverterCategory.SCIENCE,     "flow rate liter per second gpm"),
    ConverterModule("viscosity",    "Viscosity",      "Pa·s · cP",              "💧", Color(0xFF7799FF), ConverterCategory.SCIENCE,     "viscosity poise centipoise"),
    ConverterModule("angle",        "Angle",          "° · rad · grad",         "📐", Color(0xFFFF9944), ConverterCategory.SCIENCE,     "angle degree radian gradian"),
    ConverterModule("frequency",    "Frequency",      "Hz · kHz · MHz",         "〰️", Color(0xFF66DD66), ConverterCategory.SCIENCE,     "frequency hertz khz mhz"),
    ConverterModule("radioactivity","Radioactivity",  "Bq · Ci · Sv",           "☢️", Color(0xFF99FF44), ConverterCategory.SCIENCE,     "radioactivity becquerel curie"),
    ConverterModule("sound",        "Sound",          "dB · dBm · dBW",         "🔊", Color(0xFFFFAA33), ConverterCategory.SCIENCE,     "sound decibel db"),
    ConverterModule("concentration","Concentration",  "mol/L · ppm · ppb",      "🧬", Color(0xFF66FFAA), ConverterCategory.SCIENCE,     "concentration mol ppm ppb"),
    ConverterModule("raddose",      "Radiation Dose", "Sv · rem · Gy",          "☣️", Color(0xFF99FF55), ConverterCategory.SCIENCE,     "radiation dose sievert rem"),
    ConverterModule("humidity",     "Humidity",       "RH · dew · abs",         "💦", Color(0xFF44CCFF), ConverterCategory.SCIENCE,     "humidity dew point"),
    // ELECTRONICS
    ConverterModule("electric",     "Electric",       "V · A · Ω",              "⚡", Color(0xFF7788FF), ConverterCategory.ELECTRONICS,  "electric voltage current resistance"),
    ConverterModule("magnetic",     "Magnetic",       "T · mT · G · Oe",        "🧲", Color(0xFFFF55AA), ConverterCategory.ELECTRONICS,  "magnetic tesla gauss"),
    ConverterModule("datastorage",  "Data Storage",   "B · KB · MB · GB",       "💾", Color(0xFFBB66FF), ConverterCategory.ELECTRONICS,  "data storage byte kb mb gb tb"),
    ConverterModule("typography",   "Typography",     "px · pt · em · dp",      "🔡", Color(0xFF44FFDD), ConverterCategory.ELECTRONICS,  "typography pixel point em"),
    ConverterModule("color",        "Color Codes",    "HEX · RGB · HSL",        "🎨", Color(0xFFFF77CC), ConverterCategory.ELECTRONICS,  "color hex rgb hsl"),
    ConverterModule("numeral",      "Numerals",       "DEC · BIN · HEX",        "#️⃣", Color(0xFF00DDBB), ConverterCategory.ELECTRONICS,  "numeral binary hex decimal octal roman"),
    // LIGHT
    ConverterModule("illuminance",  "Illuminance",    "lux · fc · phot",        "💡", Color(0xFFFFEE44), ConverterCategory.LIGHT,        "illuminance lux foot-candle"),
    ConverterModule("luminance",    "Luminance",      "cd/m² · fL · nit",       "☀️", Color(0xFFFFCC33), ConverterCategory.LIGHT,        "luminance candela nit"),
    // PRINT
    ConverterModule("papersize",    "Paper Sizes",    "A4 · Letter · Legal",    "📄", Color(0xFFCC77FF), ConverterCategory.PRINT,        "paper size a4 letter legal"),
    // TOOLS
    ConverterModule("timezone",     "Time Zones",     "UTC · GMT · World",      "🌍", Color(0xFF44AAFF), ConverterCategory.TOOLS,        "timezone utc gmt world clock"),
    ConverterModule("dateage",      "Date & Age",     "Days · Age · Diff",      "📅", Color(0xFFBB77FF), ConverterCategory.TOOLS,        "date age difference birthday"),
    ConverterModule("tip",          "Tip Calc",       "Bill · Tip · Split",     "🧾", Color(0xFF44DD88), ConverterCategory.TOOLS,        "tip gratuity restaurant bill split"),
    ConverterModule("discount",     "Discount %",     "Sale · Off · Savings",   "🏷️", Color(0xFFFF8844), ConverterCategory.TOOLS,        "discount percentage off sale"),
    ConverterModule("bmibmr",       "BMI / BMR",      "Body · Health · Calc",   "❤️", Color(0xFFFF4466), ConverterCategory.TOOLS,        "bmi bmr body mass index health"),
    ConverterModule("shoesize",     "Shoe Sizes",     "US · UK · EU · JP",      "👟", Color(0xFF88AAFF), ConverterCategory.TOOLS,        "shoe size us uk eu jp"),
    ConverterModule("ringsize",     "Ring Sizes",     "US · UK · EU · mm",      "💍", Color(0xFFFFCC33), ConverterCategory.TOOLS,        "ring size us uk eu mm"),
    ConverterModule("bloodglucose", "Blood Glucose",  "mg/dL · mmol/L",         "🩸", Color(0xFFFF4444), ConverterCategory.TOOLS,        "blood glucose sugar mg/dl mmol"),
    ConverterModule("tiresize",     "Tire Sizes",     "205/55R16 decoder",      "🛞", Color(0xFF999999), ConverterCategory.TOOLS,        "tire tyre size aspect ratio"),
)
