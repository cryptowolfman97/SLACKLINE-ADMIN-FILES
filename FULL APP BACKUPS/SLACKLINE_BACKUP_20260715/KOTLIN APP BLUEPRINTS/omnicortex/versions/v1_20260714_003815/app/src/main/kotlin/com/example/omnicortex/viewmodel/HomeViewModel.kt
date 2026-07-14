package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.store.AegisScoreStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val score: Int      = -1,   // -1 = not yet loaded / never scanned
    val label: String   = "",
    val lastScan: String = ""
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadScore() }

    fun loadScore() {
        viewModelScope.launch {
            val data = AegisScoreStore.load()
            if (data != null) {
                _uiState.value = HomeUiState(
                    score    = data.score,
                    label    = data.label,
                    lastScan = AegisScoreStore.lastScanFormatted(data.lastScanMs)
                )
            }
        }
    }
}
