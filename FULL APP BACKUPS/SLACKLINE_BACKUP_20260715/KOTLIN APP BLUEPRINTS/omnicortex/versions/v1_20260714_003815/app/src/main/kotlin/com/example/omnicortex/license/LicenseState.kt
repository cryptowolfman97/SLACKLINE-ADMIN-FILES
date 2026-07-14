package com.example.omnicortex.license

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tier ordering used for "does the user have at least X" checks. */
enum class Tier(val rank: Int, val label: String) {
    FREE(0, "Free"), PRO(1, "Pro"), PRO_PLUS(2, "Pro+");

    companion object {
        fun from(raw: String): Tier = when (raw.lowercase()) {
            "pro_plus", "pro+", "proplus" -> PRO_PLUS
            "pro" -> PRO
            else -> FREE
        }
    }
}

data class LicenseUiState(
    val loading: Boolean = true,
    val loggedIn: Boolean = false,
    val tier: Tier = Tier.FREE,
    val revoked: Boolean = false,
    val email: String = "",
    val message: String = "Checking license…"
)

/**
 * Single source of truth for license/account state across the app.
 * Started once from MainActivity; does not alter any existing ViewModel
 * or screen logic.
 */
object LicenseState {
    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    private var started = false

    fun start(appContext: Context, scope: CoroutineScope) {
        if (started) return
        started = true
        refresh(appContext, scope)
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(LicenseGateConfig.PERIODIC_CHECK_MINUTES * 60_000L)
                refresh(appContext, scope)
            }
        }
    }

    /** Call this on-demand, e.g. when the License Details button is tapped. */
    fun refresh(appContext: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(loading = true)
            val status = SHVAccount.getAccessStatus(appContext)
            _state.value = LicenseUiState(
                loading  = false,
                loggedIn = status.loggedIn,
                tier     = Tier.from(status.tier),
                revoked  = status.revoked,
                email    = status.email,
                message  = status.message
            )
        }
    }

    fun hasAtLeast(required: Tier): Boolean {
        val s = _state.value
        return s.loggedIn && !s.revoked && s.tier.rank >= required.rank
    }
}
