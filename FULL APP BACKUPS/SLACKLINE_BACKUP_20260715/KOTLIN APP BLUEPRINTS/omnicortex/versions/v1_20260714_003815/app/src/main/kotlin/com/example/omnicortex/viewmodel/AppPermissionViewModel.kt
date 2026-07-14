package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.models.AppRiskEntry
import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.engine.AppRiskEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPermissionViewModel(app: Application) : AndroidViewModel(app) {

    sealed class State {
        object Idle     : State()
        object Scanning : State()
        data class Done(val result: AppRiskEngine.ScanResult) : State()
        data class Error(val msg: String) : State()
    }

    private val _state  = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val _filter = MutableStateFlow<String>("All")
    val filter: StateFlow<String> = _filter

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    val filterOptions = listOf("All", "Critical", "High", "Medium")

    fun scan() {
        viewModelScope.launch {
            _state.value = State.Scanning
            try {
                val result = withContext(Dispatchers.Default) {
                    AppRiskEngine.scanInstalledApps(getApplication())
                }
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun setFilter(f: String) { _filter.value = f }
    fun setSearch(q: String) { _search.value = q }

    fun filteredApps(result: AppRiskEngine.ScanResult): List<AppRiskEntry> {
        val q   = _search.value.lowercase()
        val fil = _filter.value
        return result.apps.filter { app ->
            (q.isBlank() || app.appName.lowercase().contains(q) || app.packageName.contains(q)) &&
            when (fil) {
                "Critical" -> app.riskScore >= 80
                "High"     -> app.riskScore in 50..79
                "Medium"   -> app.riskScore in 20..49
                else       -> true
            }
        }
    }
}
