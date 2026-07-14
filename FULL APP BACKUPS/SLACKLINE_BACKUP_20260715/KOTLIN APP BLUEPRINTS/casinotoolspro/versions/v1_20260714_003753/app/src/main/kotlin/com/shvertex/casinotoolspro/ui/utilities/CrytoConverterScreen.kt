package com.shvertex.casinotoolspro.ui.utilities

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shvertex.casinotoolspro.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ── Supported coins ───────────────────────────────────────────────────────────

data class CoinInfo(
    val id: String,          // CoinGecko id
    val symbol: String,      // display symbol
    val name: String
)

val SUPPORTED_COINS = listOf(
    CoinInfo("bitcoin",       "BTC",  "Bitcoin"),
    CoinInfo("ethereum",      "ETH",  "Ethereum"),
    CoinInfo("litecoin",      "LTC",  "Litecoin"),
    CoinInfo("solana",        "SOL",  "Solana"),
    CoinInfo("dogecoin",      "DOGE", "Dogecoin"),
    CoinInfo("tether",        "USDT", "Tether"),
    CoinInfo("binancecoin",   "BNB",  "BNB"),
    CoinInfo("ripple",        "XRP",  "XRP"),
    CoinInfo("cardano",       "ADA",  "Cardano"),
    CoinInfo("tron",          "TRX",  "TRON"),
)

val FIAT_CURRENCIES = listOf("USD", "EUR", "GBP", "INR", "AUD", "CAD", "JPY")

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class RateState(
    val rates: Map<String, Double> = emptyMap(),   // coinId -> USD price
    val fiatRates: Map<String, Double> = emptyMap(), // fiat -> USD rate
    val loading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String = ""
)

class CryptoViewModel : ViewModel() {
    private val _state = MutableStateFlow(RateState())
    val state          = _state.asStateFlow()

    init { fetchRates() }

    fun fetchRates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                // Fetch crypto prices in USD from CoinGecko (free, no API key needed)
                val coinIds   = SUPPORTED_COINS.joinToString(",") { it.id }
                val cryptoUrl = "https://api.coingecko.com/api/v3/simple/price" +
                        "?ids=$coinIds&vs_currencies=usd"

                val cryptoJson = withContext(Dispatchers.IO) { fetchUrl(cryptoUrl) }
                val cryptoObj  = JSONObject(cryptoJson)

                val rates = mutableMapOf<String, Double>()
                SUPPORTED_COINS.forEach { coin ->
                    if (cryptoObj.has(coin.id)) {
                        rates[coin.id] = cryptoObj.getJSONObject(coin.id).getDouble("usd")
                    }
                }

                // Fetch fiat rates (exchangerate-api free tier, no key needed)
                val fiatUrl  = "https://open.er-api.com/v6/latest/USD"
                val fiatJson = withContext(Dispatchers.IO) { fetchUrl(fiatUrl) }
                val fiatObj  = JSONObject(fiatJson).getJSONObject("rates")

                val fiatRates = mutableMapOf<String, Double>()
                FIAT_CURRENCIES.forEach { fiat ->
                    if (fiatObj.has(fiat)) {
                        fiatRates[fiat] = fiatObj.getDouble(fiat)
                    }
                }
                // USD to USD is always 1
                fiatRates["USD"] = 1.0

                val time = java.text.SimpleDateFormat(
                    "HH:mm:ss", java.util.Locale.US
                ).format(java.util.Date())

                _state.value = RateState(
                    rates       = rates,
                    fiatRates   = fiatRates,
                    loading     = false,
                    error       = null,
                    lastUpdated = time
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error   = "Failed to fetch rates: ${e.message}"
                )
            }
        }
    }

    private fun fetchUrl(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.setRequestProperty("Accept", "application/json")
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    fun convert(
        amount: Double,
        fromSymbol: String,
        toSymbol: String,
        rates: Map<String, Double>,
        fiatRates: Map<String, Double>
    ): Double? {
        // Get USD value of `from`
        val fromUsd: Double = when {
            fromSymbol == "USD" -> amount
            FIAT_CURRENCIES.contains(fromSymbol) -> {
                val rate = fiatRates[fromSymbol] ?: return null
                amount / rate  // convert fiat -> USD
            }
            else -> {
                val coinId = SUPPORTED_COINS.find { it.symbol == fromSymbol }?.id ?: return null
                val price  = rates[coinId] ?: return null
                amount * price  // crypto -> USD
            }
        }

        // Convert USD to `to`
        return when {
            toSymbol == "USD" -> fromUsd
            FIAT_CURRENCIES.contains(toSymbol) -> {
                val rate = fiatRates[toSymbol] ?: return null
                fromUsd * rate
            }
            else -> {
                val coinId = SUPPORTED_COINS.find { it.symbol == toSymbol }?.id ?: return null
                val price  = rates[coinId] ?: return null
                if (price == 0.0) null else fromUsd / price
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun CryptoConverterScreen(onBack: () -> Unit) {
    val vm: CryptoViewModel = viewModel()
    val state by vm.state.collectAsState()

    var amountStr by remember { mutableStateOf("1") }
    var fromSymbol by remember { mutableStateOf("BTC") }
    var toSymbol   by remember { mutableStateOf("USD") }

    // All selectable symbols = cryptos + fiats
    val allSymbols = SUPPORTED_COINS.map { it.symbol } + FIAT_CURRENCIES

    val converted: Double? = remember(amountStr, fromSymbol, toSymbol, state.rates, state.fiatRates) {
        val amount = amountStr.toDoubleOrNull() ?: return@remember null
        if (state.rates.isEmpty()) return@remember null
        vm.convert(amount, fromSymbol, toSymbol, state.rates, state.fiatRates)
    }

    // Refresh spin animation
    val refreshRotation by rememberInfiniteTransition(label = "refresh").animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label         = "spin"
    )

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Crypto Converter", "Live rates via CoinGecko & ER-API", CTPColors.Gold, onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Status bar ────────────────────────────────────────────────────
            CTPCard(accentColor = CTPColors.Gold, showAccent = false) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        when {
                            state.loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier  = Modifier.size(14.dp),
                                        color     = CTPColors.Gold,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Fetching live rates...",
                                        style = CTPType.BodyMedium, color = CTPColors.TextMuted)
                                }
                            }
                            state.error != null -> {
                                Text("⚠ ${state.error}",
                                    style = CTPType.BodyMedium, color = CTPColors.Red)
                                Text("Showing cached / last known rates",
                                    style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                            }
                            else -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier.size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CTPColors.Green)
                                    )
                                    Text("Live  ·  Updated ${state.lastUpdated}",
                                        style = CTPType.BodyMedium, color = CTPColors.Green)
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick  = { vm.fetchRates() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CTPColors.CardElevated)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint     = CTPColors.Gold,
                            modifier = Modifier.then(
                                if (state.loading) Modifier.rotate(refreshRotation) else Modifier
                            )
                        )
                    }
                }
            }

            // ── Amount input ──────────────────────────────────────────────────
            CTPInput(amountStr, { amountStr = it }, "Amount to Convert")

            // ── FROM selector ─────────────────────────────────────────────────
            CTPCard(accentColor = CTPColors.Gold) {
                Text("FROM", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                CurrencyGrid(
                    symbols    = allSymbols,
                    selected   = fromSymbol,
                    onSelect   = { fromSymbol = it },
                    rates      = state.rates,
                    fiatRates  = state.fiatRates,
                    accentColor = CTPColors.Gold
                )
            }

            // ── Swap button ───────────────────────────────────────────────────
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick  = { val tmp = fromSymbol; fromSymbol = toSymbol; toSymbol = tmp },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CTPColors.CardElevated)
                        .border(1.dp, CTPColors.Border, RoundedCornerShape(22.dp))
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap",
                        tint = CTPColors.Gold)
                }
            }

            // ── TO selector ───────────────────────────────────────────────────
            CTPCard(accentColor = CTPColors.Green) {
                Text("TO", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                CurrencyGrid(
                    symbols    = allSymbols,
                    selected   = toSymbol,
                    onSelect   = { toSymbol = it },
                    rates      = state.rates,
                    fiatRates  = state.fiatRates,
                    accentColor = CTPColors.Green
                )
            }

            // ── Result ────────────────────────────────────────────────────────
            CTPCard(accentColor = CTPColors.Green, showAccent = false) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$amountStr $fromSymbol  =",
                        style = CTPType.HeadlineMedium,
                        color = CTPColors.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    when {
                        state.loading -> CircularProgressIndicator(color = CTPColors.Green)
                        converted != null -> {
                            Text(
                                text  = formatConverted(converted, toSymbol),
                                style = CTPType.DisplayLarge.copy(fontSize = 32.sp),
                                color = CTPColors.Green,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                toSymbol,
                                style = CTPType.HeadlineMedium,
                                color = CTPColors.TextMuted
                            )
                        }
                        else -> Text(
                            "Enter an amount and select currencies",
                            style = CTPType.BodyMedium,
                            color = CTPColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Live price table ──────────────────────────────────────────────
            if (state.rates.isNotEmpty()) {
                LivePriceTable(rates = state.rates, fiatRates = state.fiatRates)
            }
        }
    }
}

// ── Currency Grid ─────────────────────────────────────────────────────────────

@Composable
fun CurrencyGrid(
    symbols: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    rates: Map<String, Double>,
    fiatRates: Map<String, Double>,
    accentColor: Color
) {
    val rows = symbols.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { symbol ->
                    val isSelected = symbol == selected
                    val isCrypto   = SUPPORTED_COINS.any { it.symbol == symbol }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else CTPColors.CardElevated
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentColor else CTPColors.Border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(symbol) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                symbol,
                                style = CTPType.LabelLarge,
                                color = if (isSelected) accentColor else CTPColors.TextPrimary
                            )
                            // Show price under crypto symbols when loaded
                            if (isCrypto && rates.isNotEmpty()) {
                                val coinId = SUPPORTED_COINS.find { it.symbol == symbol }?.id
                                val price  = coinId?.let { rates[it] }
                                if (price != null) {
                                    Text(
                                        formatPrice(price),
                                        style = CTPType.LabelMedium.copy(fontSize = 8.sp),
                                        color = CTPColors.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
                // Pad last row
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Live Price Table ──────────────────────────────────────────────────────────

@Composable
fun LivePriceTable(rates: Map<String, Double>, fiatRates: Map<String, Double>) {
    CTPCard(accentColor = CTPColors.Gold) {
        Text("LIVE PRICES (USD)", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            Text("Coin",  style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Price (USD)", style = CTPType.LabelMedium, color = CTPColors.TextMuted, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
        }
        CTPDivider(Modifier.padding(vertical = 4.dp))

        SUPPORTED_COINS.forEach { coin ->
            val price = rates[coin.id]
            if (price != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(coin.symbol, style = CTPType.Mono, color = CTPColors.TextPrimary)
                        Text(coin.name,   style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                    }
                    Text(
                        "$${"%.2f".format(price)}".let {
                            if (price >= 1000) "$${"%.0f".format(price)}"
                            else if (price >= 1) "$${"%.4f".format(price)}"
                            else "$${"%.8f".format(price)}"
                        },
                        style     = CTPType.Mono,
                        color     = CTPColors.Gold,
                        textAlign = TextAlign.End,
                        modifier  = Modifier.weight(1.5f)
                    )
                }
                CTPDivider(Modifier.padding(vertical = 1.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Fiat Rates (per 1 USD)", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        FIAT_CURRENCIES.filter { it != "USD" }.forEach { fiat ->
            val rate = fiatRates[fiat]
            if (rate != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 USD → $fiat", style = CTPType.Mono, color = CTPColors.TextSecondary)
                    Text("%.4f".format(rate), style = CTPType.Mono, color = CTPColors.TextPrimary)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatConverted(value: Double, symbol: String): String {
    return when {
        symbol in FIAT_CURRENCIES -> when {
            value >= 1_000_000 -> "${"%.2f".format(value / 1_000_000)}M"
            value >= 1_000     -> "${"%.2f".format(value)}"
            else               -> "%.4f".format(value)
        }
        value >= 1     -> "%.8f".format(value)
        value >= 0.001 -> "%.8f".format(value)
        else           -> "%.10f".format(value)
    }
}

fun formatPrice(price: Double): String = when {
    price >= 1000 -> "$${"%.0f".format(price)}"
    price >= 1    -> "$${"%.2f".format(price)}"
    else          -> "$${"%.4f".format(price)}"
}