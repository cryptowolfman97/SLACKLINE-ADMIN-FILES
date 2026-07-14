package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.engine.PortScanEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PortScanViewModel(app: Application) : AndroidViewModel(app) {
    sealed class State {
        object Idle : State()
        data class Scanning(val progress: Int, val total: Int) : State()
        data class Done(val result: PortScanEngine.ScanResult) : State()
        data class Error(val msg: String) : State()
    }
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host
    private val _mode = MutableStateFlow("Top 35")
    val mode: StateFlow<String> = _mode
    private val _customPorts = MutableStateFlow("")
    val customPorts: StateFlow<String> = _customPorts

    val scanModes = listOf("Top 35", "Top 1000", "Custom")

    fun setHost(v: String) { _host.value = v }
    fun setMode(v: String) { _mode.value = v }
    fun setCustomPorts(v: String) { _customPorts.value = v }

    fun scan() {
        val h = _host.value.trim()
        if (h.isBlank()) return
        val ports = when (_mode.value) {
            "Top 35"   -> PortScanEngine.TOP_100_PORTS
            "Top 1000" -> PortScanEngine.TOP_1000_PORTS
            else -> _customPorts.value.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        if (ports.isEmpty()) return
        viewModelScope.launch {
            _state.value = State.Scanning(0, ports.size)
            try {
                val result = PortScanEngine.scan(h, ports) { done, total ->
                    _state.value = State.Scanning(done, total)
                }
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Scan failed")
            }
        }
    }
    fun reset() { _state.value = State.Idle }
}
