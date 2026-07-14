package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.models.TlsResult
import com.example.omnicortex.engine.TlsInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommsViewModel(app: Application) : AndroidViewModel(app) {

    sealed class State {
        object Idle                          : State()
        object Checking                      : State()
        data class Done(val result: TlsResult) : State()
        data class Error(val msg: String)    : State()
    }

    private val _state   = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val _input   = MutableStateFlow("")
    val input: StateFlow<String> = _input

    private val _history = MutableStateFlow<List<TlsResult>>(emptyList())
    val history: StateFlow<List<TlsResult>> = _history

    fun setInput(v: String) { _input.value = v }

    fun check() {
        val domain = _input.value.trim()
        if (domain.isBlank()) return
        viewModelScope.launch {
            _state.value = State.Checking
            try {
                val result = withContext(Dispatchers.IO) { TlsInspector.inspect(domain) }
                _history.value = listOf(result) + _history.value.take(9)
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Check failed")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}
