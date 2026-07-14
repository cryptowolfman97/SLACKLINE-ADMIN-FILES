package com.example.omnicortex.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

// ── VPN State ─────────────────────────────────────────────────────────────────

sealed class VpnState {
    object DISCONNECTED : VpnState()
    object CONNECTING   : VpnState()
    object CONNECTED    : VpnState()
    data class ERROR(val message: String) : VpnState()
}

// ── Engine ────────────────────────────────────────────────────────────────────

/**
 * WarpVpnEngine
 *
 * Singleton that manages VPN state and provides the public API
 * for starting/stopping the Cloudflare WARP DNS VPN tunnel.
 *
 * Usage:
 *   WarpVpnEngine.requestPermissionIfNeeded(activity, REQUEST_CODE)
 *   WarpVpnEngine.connect(context)
 *   WarpVpnEngine.disconnect(context)
 */
object WarpVpnEngine {

    const val VPN_PERMISSION_REQUEST_CODE = 7001

    private val _state = MutableStateFlow<VpnState>(VpnState.DISCONNECTED)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    // Traffic counters (updated periodically by the service)
    private val _bytesIn  = AtomicLong(0)
    private val _bytesOut = AtomicLong(0)
    val bytesIn:  Long get() = _bytesIn.get()
    val bytesOut: Long get() = _bytesOut.get()

    // DNS query stats
    private val _queriesTotal   = AtomicLong(0)
    private val _queriesBlocked = AtomicLong(0)
    val queriesTotal:   Long get() = _queriesTotal.get()
    val queriesBlocked: Long get() = _queriesBlocked.get()

    /** Call from Activity.onActivityResult with REQUEST_CODE to proceed after permission grant */
    fun onPermissionResult(resultCode: Int, context: Context) {
        if (resultCode == Activity.RESULT_OK) connect(context)
        else _state.value = VpnState.ERROR("VPN permission denied")
    }

    /**
     * Checks if VPN permission is already granted.
     * If not, launches the system permission dialog.
     * Returns true if permission was already granted (connect immediately).
     */
    fun requestPermissionIfNeeded(activity: Activity): Boolean {
        val intent = VpnService.prepare(activity)
        return if (intent == null) {
            true  // Already granted
        } else {
            activity.startActivityForResult(intent, VPN_PERMISSION_REQUEST_CODE)
            false
        }
    }

    fun connect(context: Context) {
        if (_state.value is VpnState.CONNECTED || _state.value is VpnState.CONNECTING) return
        _state.value = VpnState.CONNECTING
        resetCounters()
        val intent = Intent(context, WarpVpnService::class.java).apply {
            action = WarpVpnService.ACTION_START
        }
        context.startService(intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, WarpVpnService::class.java).apply {
            action = WarpVpnService.ACTION_STOP
        }
        context.startService(intent)
        _state.value = VpnState.DISCONNECTED
    }

    /** Called by WarpVpnService to update state */
    fun onStateChange(state: VpnState) {
        _state.value = state
    }

    fun recordQuery(blocked: Boolean) {
        _queriesTotal.incrementAndGet()
        if (blocked) _queriesBlocked.incrementAndGet()
    }

    fun addTraffic(inBytes: Long, outBytes: Long) {
        _bytesIn.addAndGet(inBytes)
        _bytesOut.addAndGet(outBytes)
    }

    private fun resetCounters() {
        _bytesIn.set(0); _bytesOut.set(0)
        _queriesTotal.set(0); _queriesBlocked.set(0)
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024       -> "${bytes} B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else               -> "${"%.2f".format(bytes / (1024.0 * 1024))} MB"
    }
}
