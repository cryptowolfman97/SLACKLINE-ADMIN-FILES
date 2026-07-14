package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.data.models.NetworkProfile
import com.example.omnicortex.data.models.ScanResult
import com.example.omnicortex.data.prefs.AegisPreferences
import com.example.omnicortex.engine.NetworkScanEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NetworkViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    sealed class State {
        object Idle     : State()
        object Scanning : State()
        data class Done(val result: NetworkScanEngine.ScanResult) : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    val networkProfiles = AegisStore.networkProfilesFlow

    fun scan() {
        viewModelScope.launch {
            _state.value = State.Scanning
            try {
                val result = withContext(Dispatchers.IO) {
                    NetworkScanEngine.scan(ctx)
                }
                if (result == null) {
                    _state.value = State.Error("Not connected to WiFi or location permission denied.")
                    return@launch
                }
                // Persist current network profile
                withContext(Dispatchers.IO) {
                    val existing = AegisStore.findNetworkByBssid(result.currentBssid)
                    AegisStore.upsertNetwork(
                        ctx,
                        NetworkProfile(
                            bssid     = result.currentBssid,
                            ssid      = result.currentSsid,
                            security  = result.currentSecurity,
                            frequency = result.currentFrequency,
                            trusted   = existing?.trusted ?: false,
                            firstSeen = existing?.firstSeen ?: System.currentTimeMillis(),
                            lastSeen  = System.currentTimeMillis()
                        )
                    )
                    // Also persist nearby networks
                    result.nearbyNetworks.forEach { n ->
                        val ex = AegisStore.findNetworkByBssid(n.bssid)
                        AegisStore.upsertNetwork(
                            ctx,
                            NetworkProfile(
                                bssid     = n.bssid,
                                ssid      = n.ssid,
                                security  = n.security,
                                frequency = n.frequency,
                                trusted   = ex?.trusted ?: false,
                                firstSeen = ex?.firstSeen ?: System.currentTimeMillis(),
                                lastSeen  = System.currentTimeMillis()
                            )
                        )
                    }
                    // Save scan result
                    val threatSummary = if (result.threats.isEmpty()) "No threats detected"
                                       else "${result.threats.size} threat(s) found"
                    AegisStore.insertScan(
                        ctx,
                        ScanResult(
                            id      = UUID.randomUUID().toString(),
                            type    = "network",
                            score   = if (result.threats.isEmpty()) 100 else (100 - result.threats.size * 20).coerceAtLeast(0),
                            summary = threatSummary
                        )
                    )
                }
                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun toggleTrust(bssid: String) {
        viewModelScope.launch {
            val profile = AegisStore.findNetworkByBssid(bssid) ?: return@launch
            AegisStore.upsertNetwork(ctx, profile.copy(trusted = !profile.trusted))
        }
    }
}
