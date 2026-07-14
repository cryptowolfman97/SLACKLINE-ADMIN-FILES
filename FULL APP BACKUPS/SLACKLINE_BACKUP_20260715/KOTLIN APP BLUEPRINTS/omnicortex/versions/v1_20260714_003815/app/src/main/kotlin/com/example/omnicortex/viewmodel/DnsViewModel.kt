package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.engine.DnsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DnsViewModel(app: Application) : AndroidViewModel(app) {
    sealed class State {
        object Idle : State()
        object Running : State()
        data class Done(val result: DnsEngine.DnsResult) : State()
        data class Error(val msg: String) : State()
    }
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    fun setInput(v: String) { _input.value = v }

    fun lookup() {
        val domain = _input.value.trim()
        if (domain.isBlank()) return
        viewModelScope.launch {
            _state.value = State.Running
            try {
                val result = DnsEngine.lookup(domain)
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Lookup failed")
            }
        }
    }
    fun reset() { _state.value = State.Idle }
}
