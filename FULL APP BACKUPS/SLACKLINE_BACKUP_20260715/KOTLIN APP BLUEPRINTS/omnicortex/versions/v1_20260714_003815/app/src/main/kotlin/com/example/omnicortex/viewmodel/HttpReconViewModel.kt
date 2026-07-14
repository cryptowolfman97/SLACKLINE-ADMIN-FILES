package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.engine.HttpReconEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HttpReconViewModel(app: Application) : AndroidViewModel(app) {
    sealed class State {
        object Idle : State()
        object Running : State()
        data class Done(val result: HttpReconEngine.HttpReconResult) : State()
        data class Error(val msg: String) : State()
    }
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input
    private val _history = MutableStateFlow<List<HttpReconEngine.HttpReconResult>>(emptyList())
    val history: StateFlow<List<HttpReconEngine.HttpReconResult>> = _history

    fun setInput(v: String) { _input.value = v }

    fun run() {
        val url = _input.value.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _state.value = State.Running
            try {
                val result = HttpReconEngine.analyse(url)
                _history.value = listOf(result) + _history.value.take(9)
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Analysis failed")
            }
        }
    }
    fun reset() { _state.value = State.Idle }
}
