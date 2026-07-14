package com.example.omnicortex.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.vpn.DoHEngine
import com.example.omnicortex.vpn.PermissionHelper
import com.example.omnicortex.vpn.VpnOverlayService
import com.example.omnicortex.vpn.VpnState
import com.example.omnicortex.vpn.WarpVpnEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PrivacyShieldUiState(
    val vpnState: VpnState              = VpnState.DISCONNECTED,
    val dohEnabled: Boolean             = false,
    val queriesTotal: Long              = 0L,
    val queriesBlocked: Long            = 0L,
    val bytesIn: Long                   = 0L,
    val bytesOut: Long                  = 0L,
    val blocklistSize: Int              = 0,
    val batteryExempt: Boolean          = false,
    val overlayGranted: Boolean         = false,
    val pipActive: Boolean              = false,
)

class PrivacyShieldViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(
        PrivacyShieldUiState(blocklistSize = DoHEngine.blocklistSize())
    )
    val uiState: StateFlow<PrivacyShieldUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WarpVpnEngine.state.collect { vpnState ->
                _uiState.value = _uiState.value.copy(vpnState = vpnState)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                if (WarpVpnEngine.state.value is VpnState.CONNECTED) {
                    _uiState.value = _uiState.value.copy(
                        queriesTotal   = WarpVpnEngine.queriesTotal,
                        queriesBlocked = WarpVpnEngine.queriesBlocked,
                        bytesIn        = WarpVpnEngine.bytesIn,
                        bytesOut       = WarpVpnEngine.bytesOut,
                    )
                }
                delay(1000)
            }
        }
    }

    /** Call whenever the screen is (re)entered to refresh permission states */
    fun refreshPermissions(context: Context) {
        val status = PermissionHelper.getStatus(context)
        _uiState.value = _uiState.value.copy(
            batteryExempt  = status.batteryExempt,
            overlayGranted = status.overlayGranted
        )
    }

    fun toggleVpn(activity: Activity) {
        val context = getApplication<Application>()
        when (_uiState.value.vpnState) {
            is VpnState.CONNECTED, is VpnState.CONNECTING -> WarpVpnEngine.disconnect(context)
            else -> {
                val alreadyGranted = WarpVpnEngine.requestPermissionIfNeeded(activity)
                if (alreadyGranted) WarpVpnEngine.connect(context)
            }
        }
    }

    fun onVpnPermissionResult(resultCode: Int) {
        WarpVpnEngine.onPermissionResult(resultCode, getApplication())
    }

    fun requestBatteryExemption(activity: Activity) {
        PermissionHelper.requestBatteryOptimizationExemption(activity)
    }

    fun requestOverlayPermission(activity: Activity) {
        PermissionHelper.requestOverlayPermission(activity)
    }

    /** Show / hide the floating PiP bubble */
    fun togglePip(context: Context) {
        val isActive = _uiState.value.pipActive
        if (isActive) {
            VpnOverlayService.hide(context)
            _uiState.value = _uiState.value.copy(pipActive = false)
        } else {
            if (!PermissionHelper.canDrawOverlays(context)) return  // guard
            VpnOverlayService.show(context)
            _uiState.value = _uiState.value.copy(pipActive = true)
        }
    }

    fun toggleDoH(context: Context) {
        _uiState.value = _uiState.value.copy(dohEnabled = !_uiState.value.dohEnabled)
    }

    fun formatBytes(bytes: Long) = WarpVpnEngine.formatBytes(bytes)
}
