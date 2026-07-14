package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.engine.ApkAnalyserEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApkAnalyserViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Done(val result: ApkAnalyserEngine.ApkAnalysisResult) : State()
        data class Error(val msg: String) : State()
    }
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val _appList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val appList: StateFlow<List<Pair<String, String>>> = _appList
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    init { loadAppList() }

    fun setSearch(v: String) { _search.value = v }
    fun setTab(v: Int) { _selectedTab.value = v }

    private fun loadAppList() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                ApkAnalyserEngine.getInstalledUserApps(ctx)
            }
            _appList.value = apps
        }
    }

    fun analyse(packageName: String) {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    ApkAnalyserEngine.analyse(ctx, packageName)
                }
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Analysis failed")
            }
        }
    }

    fun filteredApps(): List<Pair<String, String>> {
        val q = _search.value.lowercase()
        return if (q.isBlank()) _appList.value
        else _appList.value.filter { it.second.lowercase().contains(q) || it.first.contains(q) }
    }

    fun reset() { _state.value = State.Idle }
}
