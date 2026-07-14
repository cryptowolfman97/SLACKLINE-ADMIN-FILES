package com.example.omnicortex.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.data.models.FirewallRule
import com.example.omnicortex.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShizukuViewModel(app: Application) : AndroidViewModel(app) {

    data class AppEntry(
        val packageName: String,
        val label: String,
        val uid: Int,
        val isSystemApp: Boolean,
        val requestedPermissions: List<String>,
        val blocked: Boolean
    )

    data class NetUsage(val uid: Int, val label: String, val rxBytes: Long, val txBytes: Long)

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(val apps: List<AppEntry>) : UiState()
        data class Error(val message: String) : UiState()
    }

    val availability: StateFlow<ShizukuManager.Availability> = ShizukuManager.availability

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _netUsage = MutableStateFlow<List<NetUsage>>(emptyList())
    val netUsage: StateFlow<List<NetUsage>> = _netUsage.asStateFlow()

    private var monitoring = false

    fun requestShizukuPermission() = ShizukuManager.requestPermission()

    fun refreshAvailability() = ShizukuManager.refreshAvailability()

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val entries = withContext(Dispatchers.IO) {
                    val pm = getApplication<Application>().packageManager
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { it.packageName != getApplication<Application>().packageName }
                        .map { appInfo ->
                            val perms = try {
                                pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                                    .requestedPermissions?.toList() ?: emptyList()
                            } catch (_: Exception) { emptyList() }
                            AppEntry(
                                packageName = appInfo.packageName,
                                label = pm.getApplicationLabel(appInfo).toString(),
                                uid = appInfo.uid,
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                requestedPermissions = perms,
                                blocked = AegisStore.isFirewallBlocked(appInfo.packageName)
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                }
                _uiState.value = UiState.Loaded(entries)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load installed apps")
            }
        }
    }

    fun toggleFirewall(entry: AppEntry) {
        viewModelScope.launch {
            val newBlocked = !entry.blocked
            val ok = ShizukuManager.setAppNetworkBlocked(entry.uid, newBlocked)
            if (ok) {
                AegisStore.upsertFirewallRule(
                    getApplication(),
                    FirewallRule(entry.packageName, entry.uid, newBlocked)
                )
                val current = _uiState.value
                if (current is UiState.Loaded) {
                    _uiState.value = UiState.Loaded(
                        current.apps.map { if (it.packageName == entry.packageName) it.copy(blocked = newBlocked) else it }
                    )
                }
            }
        }
    }

    fun setPermission(entry: AppEntry, permission: String, grant: Boolean) {
        viewModelScope.launch {
            if (grant) ShizukuManager.grantPermission(entry.packageName, permission)
            else ShizukuManager.revokePermission(entry.packageName, permission)
        }
    }

    /** Polls `dumpsys netstats` every few seconds while the monitor screen is open. */
    fun startNetworkMonitor() {
        if (monitoring) return
        monitoring = true
        viewModelScope.launch {
            val labelCache = mutableMapOf<Int, String>()
            val pm = getApplication<Application>().packageManager
            while (monitoring) {
                val dump = ShizukuManager.getNetstatsDump()
                val parsed = parseNetstats(dump)
                    .map { (uid, rx, tx) ->
                        val label = labelCache.getOrPut(uid) {
                            try {
                                val pkgs = pm.getPackagesForUid(uid)
                                pkgs?.firstOrNull()?.let {
                                    pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
                                } ?: "uid $uid"
                            } catch (_: Exception) { "uid $uid" }
                        }
                        NetUsage(uid, label, rx, tx)
                    }
                    .sortedByDescending { it.rxBytes + it.txBytes }
                _netUsage.value = parsed
                delay(4000)
            }
        }
    }

    fun stopNetworkMonitor() {
        monitoring = false
    }

    /**
     * `dumpsys netstats detail` output isn't a stable, documented format — it
     * varies across Android versions. This parser looks for the common
     * `uid=<n> ... rb=<rxBytes> ... tb=<txBytes>` pattern found in the
     * per-uid stats section and skips anything it can't confidently parse,
     * rather than guessing. Good enough for a relative "who's using data"
     * view; not meant to be byte-perfect.
     */
    private fun parseNetstats(dump: String): List<Triple<Int, Long, Long>> {
        if (dump.isBlank()) return emptyList()
        val uidRegex = Regex("""uid=(\d+)""")
        val rbRegex = Regex("""rb=(\d+)""")
        val tbRegex = Regex("""tb=(\d+)""")
        val results = mutableMapOf<Int, Pair<Long, Long>>()
        dump.lineSequence().forEach { line ->
            val uid = uidRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            val rb = rbRegex.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val tb = tbRegex.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            if (rb == 0L && tb == 0L) return@forEach
            val existing = results[uid] ?: (0L to 0L)
            results[uid] = (existing.first + rb) to (existing.second + tb)
        }
        return results.map { (uid, pair) -> Triple(uid, pair.first, pair.second) }
    }

    override fun onCleared() {
        stopNetworkMonitor()
        super.onCleared()
    }
}
