package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.data.models.BreachRecord
import com.example.omnicortex.data.prefs.AegisPreferences
import com.example.omnicortex.engine.BreachEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

class BreachViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    sealed class CheckState {
        object Idle                          : CheckState()
        data class Checking(val item: String): CheckState()
        object Done                          : CheckState()
        data class Error(val msg: String)    : CheckState()
    }

    private val _checkState  = MutableStateFlow<CheckState>(CheckState.Idle)
    val checkState: StateFlow<CheckState> = _checkState

    private val _watchlist   = MutableStateFlow<List<String>>(emptyList())
    val watchlist: StateFlow<List<String>> = _watchlist

    val breachRecords = AegisStore.breachRecordsFlow

    private val _newInput = MutableStateFlow("")
    val newInput: StateFlow<String> = _newInput

    init {
        viewModelScope.launch {
            AegisPreferences.watchlistFlow(ctx).collect { raw ->
                _watchlist.value = try { json.decodeFromString(raw) } catch (e: Exception) { emptyList() }
            }
        }
    }

    fun setNewInput(v: String) { _newInput.value = v }

    fun addToWatchlist(item: String) {
        val clean = item.trim().lowercase()
        if (clean.isBlank() || _watchlist.value.contains(clean)) return
        viewModelScope.launch {
            val updated = _watchlist.value + clean
            AegisPreferences.setWatchlist(ctx, json.encodeToString(updated))
            _newInput.value = ""
        }
    }

    fun removeFromWatchlist(item: String) {
        viewModelScope.launch {
            val updated = _watchlist.value.filter { it != item }
            AegisPreferences.setWatchlist(ctx, json.encodeToString(updated))
            // Remove stored breach records for this item
            AegisStore.deleteBreachesForItem(ctx, item)
        }
    }

    fun checkAll() {
        val list = _watchlist.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            // API key is empty for now — show informative message
            val apiKey = "" // TODO: read from settings when user adds key
            list.forEach { item ->
                _checkState.value = CheckState.Checking(item)
                val result = if (item.contains("@")) {
                    BreachEngine.checkEmail(item, apiKey)
                } else {
                    BreachEngine.checkDomain(item, apiKey)
                }
                result.onSuccess { breaches ->
                    breaches.forEach { b ->
                        AegisStore.insertBreach(
                            ctx,
                            BreachRecord(
                                id          = UUID.randomUUID().toString(),
                                watchItem   = item,
                                breachName  = b.name,
                                breachDate  = b.breachDate,
                                dataClasses = b.dataClasses.joinToString(", "),
                                isNew       = true
                            )
                        )
                    }
                }
            }
            _checkState.value = CheckState.Done
        }
    }

    fun markAllRead() {
        viewModelScope.launch { AegisStore.markBreachesRead(ctx) }
    }
}
