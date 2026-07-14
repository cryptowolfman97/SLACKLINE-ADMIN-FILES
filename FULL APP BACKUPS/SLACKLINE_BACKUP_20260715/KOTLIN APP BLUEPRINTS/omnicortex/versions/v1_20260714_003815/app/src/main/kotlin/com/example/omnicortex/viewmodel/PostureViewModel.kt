package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.data.models.PostureCategory
import com.example.omnicortex.data.models.PostureFinding
import com.example.omnicortex.data.models.ScanResult
import com.example.omnicortex.data.prefs.AegisPreferences
import com.example.omnicortex.data.store.AegisScoreStore
import com.example.omnicortex.engine.PostureAuditResult
import com.example.omnicortex.engine.PostureEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PostureViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    sealed class State {
        object Idle     : State()
        object Scanning : State()
        data class Done(val result: PostureAuditResult) : State()
        data class Error(val msg: String) : State()
    }

    private val _state            = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State>   = _state

    private val _selectedCategory = MutableStateFlow<PostureCategory?>(null)
    val selectedCategory: StateFlow<PostureCategory?> = _selectedCategory

    val lastScanTime = AegisPreferences.lastPostureScanFlow(ctx)

    fun runScan() {
        viewModelScope.launch {
            _state.value = State.Scanning
            try {
                val result = withContext(Dispatchers.Default) {
                    PostureEngine.runFullAudit(ctx)
                }
                withContext(Dispatchers.IO) {
                    // Existing DB insert — unchanged
                    AegisStore.insertScan(
                        ctx,
                        ScanResult(
                            id      = UUID.randomUUID().toString(),
                            type    = "posture",
                            score   = result.score,
                            summary = "${result.criticalCount} critical, ${result.highCount} high"
                        )
                    )
                    AegisPreferences.setLastPostureScan(ctx, System.currentTimeMillis())

                    // ── Persist score to Downloads/OmniCortex so HomeScreen survives restarts ──
                    AegisScoreStore.save(
                        AegisScoreStore.ScoreData(
                            score      = result.score,
                            label      = result.grade,
                            lastScanMs = System.currentTimeMillis(),
                            breakdown  = result.findings.associate { it.title to it.passed }
                        )
                    )
                }
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun selectCategory(cat: PostureCategory?) { _selectedCategory.value = cat }

    fun filteredFindings(result: PostureAuditResult): List<PostureFinding> {
        val cat = _selectedCategory.value ?: return result.findings
        return result.findings.filter { it.category == cat }
    }
}
