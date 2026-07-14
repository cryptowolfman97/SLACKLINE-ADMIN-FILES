package com.shvertex.casinotoolspro.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "ctp_session")

data class SessionEntry(
    val timestamp: String,
    val amount: Double
)

data class Strategy(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Custom",
    val game: String = "general",
    val bankroll: Double = 1000.0,
    val baseBet: Double = 1.0,
    val multiplier: Double = 2.0,
    val winChancePct: Double = 49.5,
    val increaseOnLossPct: Double = 100.0,
    val maxBets: Int = 100,
    val notes: String = "",
    val createdAt: String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm", Locale.US
    ).format(Date())
)

class BankrollManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var INSTANCE: BankrollManager? = null

        fun getInstance(context: Context): BankrollManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BankrollManager(context.applicationContext)
                    .also { INSTANCE = it }
            }

        private val KEY_PROFIT         = doublePreferencesKey("session_profit")
        private val KEY_START_TIME     = longPreferencesKey("start_time")
        private val KEY_TOTAL_SESSIONS = intPreferencesKey("total_sessions")
    }

    private val gson = Gson()

    // ── Session Profit ────────────────────────────────────────────────────────

    val sessionProfit: Flow<Double> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_PROFIT] ?: 0.0 }

    val startTime: Flow<Long> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_START_TIME] ?: System.currentTimeMillis() }

    suspend fun updateProfit(absoluteAmount: Double) {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_PROFIT]         = absoluteAmount
                prefs[KEY_TOTAL_SESSIONS] = (prefs[KEY_TOTAL_SESSIONS] ?: 0) + 1
                // Initialise start time if not set
                if (prefs[KEY_START_TIME] == null) {
                    prefs[KEY_START_TIME] = System.currentTimeMillis()
                }
            }
            // File I/O on IO dispatcher, fully isolated — won't crash main flow
            logEntrySafe(absoluteAmount)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun resetSession() {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_PROFIT]     = 0.0
                prefs[KEY_START_TIME] = System.currentTimeMillis()
            }
            clearHistorySafe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Session History ───────────────────────────────────────────────────────

    private fun historyFile() = File(context.filesDir, "session_history.json")

    fun loadHistory(): List<SessionEntry> {
        return try {
            val file = historyFile()
            if (!file.exists()) return emptyList()
            val json = file.readText()
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<List<SessionEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun logEntrySafe(amount: Double) {
        try {
            val entries = loadHistory().toMutableList()
            entries.add(
                SessionEntry(
                    timestamp = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm", Locale.US
                    ).format(Date()),
                    amount = amount
                )
            )
            val trimmed = if (entries.size > 500) entries.takeLast(500) else entries
            historyFile().writeText(gson.toJson(trimmed))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearHistorySafe() {
        try {
            historyFile().writeText("[]")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Strategy Library ──────────────────────────────────────────────────────

    private fun strategiesFile() = File(context.filesDir, "strategies.json")

    fun loadStrategies(): List<Strategy> {
        return try {
            val file = strategiesFile()
            if (!file.exists()) return defaultStrategies()
            val json = file.readText()
            if (json.isBlank()) return defaultStrategies()
            val type = object : TypeToken<List<Strategy>>() {}.type
            gson.fromJson<List<Strategy>>(json, type) ?: defaultStrategies()
        } catch (e: Exception) {
            e.printStackTrace()
            defaultStrategies()
        }
    }

    fun saveStrategies(strategies: List<Strategy>) {
        try {
            strategiesFile().writeText(gson.toJson(strategies))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addStrategy(strategy: Strategy) {
        val list = loadStrategies().toMutableList()
        list.add(strategy)
        saveStrategies(list)
    }

    fun deleteStrategy(id: String) {
        saveStrategies(loadStrategies().filter { it.id != id })
    }

    private fun defaultStrategies(): List<Strategy> = listOf(
        Strategy(
            name = "Classic Martingale", category = "Martingale", game = "dice",
            bankroll = 1000.0, baseBet = 1.0, multiplier = 2.0,
            winChancePct = 49.5, increaseOnLossPct = 100.0, maxBets = 10,
            notes = "Double bet on every loss. Reset on win."
        ),
        Strategy(
            name = "Conservative Grind", category = "Flat Bet", game = "dice",
            bankroll = 1000.0, baseBet = 0.5, multiplier = 2.0,
            winChancePct = 49.5, increaseOnLossPct = 0.0, maxBets = 200,
            notes = "Flat bet, low multiplier. Maximizes session length."
        ),
        Strategy(
            name = "High Roller Limbo", category = "Anti-Martingale", game = "limbo",
            bankroll = 1000.0, baseBet = 5.0, multiplier = 10.0,
            winChancePct = 9.9, increaseOnLossPct = 50.0, maxBets = 50,
            notes = "High multiplier with moderate loss progression."
        ),
        Strategy(
            name = "D'Alembert", category = "D'Alembert", game = "dice",
            bankroll = 1000.0, baseBet = 1.0, multiplier = 2.0,
            winChancePct = 49.5, increaseOnLossPct = 10.0, maxBets = 100,
            notes = "Increase by 1 unit on loss, decrease by 1 on win."
        ),
        Strategy(
            name = "Paroli System", category = "Paroli", game = "dice",
            bankroll = 1000.0, baseBet = 1.0, multiplier = 2.0,
            winChancePct = 49.5, increaseOnLossPct = 0.0, maxBets = 150,
            notes = "Anti-Martingale: double on win, reset on loss."
        ),
    )
}