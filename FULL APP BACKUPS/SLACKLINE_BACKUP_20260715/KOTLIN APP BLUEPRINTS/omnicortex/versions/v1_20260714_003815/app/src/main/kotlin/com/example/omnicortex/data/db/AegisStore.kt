package com.example.omnicortex.data.db

import android.content.Context
import com.example.omnicortex.data.models.BreachRecord
import com.example.omnicortex.data.models.FirewallRule
import com.example.omnicortex.data.models.NetworkProfile
import com.example.omnicortex.data.models.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ── Replaces Room — pure JSON file persistence, AndroidIDE compatible ─────────
object AegisStore {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // ── Internal state flows (in-memory, refreshed from disk on load) ─────────
    private val _scanResults    = MutableStateFlow<List<ScanResult>>(emptyList())
    private val _breachRecords  = MutableStateFlow<List<BreachRecord>>(emptyList())
    private val _networkProfiles = MutableStateFlow<List<NetworkProfile>>(emptyList())
    private val _firewallRules  = MutableStateFlow<List<FirewallRule>>(emptyList())

    val scanResultsFlow:     Flow<List<ScanResult>>     = _scanResults
    val breachRecordsFlow:   Flow<List<BreachRecord>>   = _breachRecords
    val networkProfilesFlow: Flow<List<NetworkProfile>> = _networkProfiles
    val firewallRulesFlow:   Flow<List<FirewallRule>>   = _firewallRules

    // ── Init — call once from Application class ───────────────────────────────
    fun init(ctx: Context) {
        _scanResults.value     = load(scanFile(ctx))
        _breachRecords.value   = load(breachFile(ctx))
        _networkProfiles.value = load(networkFile(ctx))
        _firewallRules.value   = load(firewallFile(ctx))
    }

    // ── Scan results ──────────────────────────────────────────────────────────
    suspend fun insertScan(ctx: Context, r: ScanResult) = withContext(Dispatchers.IO) {
        val list = (_scanResults.value + r)
            .sortedByDescending { it.timestamp }
            .take(50)
        save(scanFile(ctx), list)
        _scanResults.value = list
    }

    fun latestScanOfType(type: String): ScanResult? =
        _scanResults.value.firstOrNull { it.type == type }

    // ── Breach records ────────────────────────────────────────────────────────
    suspend fun insertBreach(ctx: Context, r: BreachRecord) = withContext(Dispatchers.IO) {
        val existing = _breachRecords.value
        if (existing.any { it.id == r.id }) return@withContext
        val list = listOf(r) + existing
        save(breachFile(ctx), list)
        _breachRecords.value = list
    }

    suspend fun markBreachesRead(ctx: Context) = withContext(Dispatchers.IO) {
        val list = _breachRecords.value.map { it.copy(isNew = false) }
        save(breachFile(ctx), list)
        _breachRecords.value = list
    }

    suspend fun deleteBreachesForItem(ctx: Context, watchItem: String) = withContext(Dispatchers.IO) {
        val list = _breachRecords.value.filter { it.watchItem != watchItem }
        save(breachFile(ctx), list)
        _breachRecords.value = list
    }

    fun newBreachCount(): Int = _breachRecords.value.count { it.isNew }

    // ── Network profiles ──────────────────────────────────────────────────────
    suspend fun upsertNetwork(ctx: Context, p: NetworkProfile) = withContext(Dispatchers.IO) {
        val list = _networkProfiles.value.toMutableList()
        val idx  = list.indexOfFirst { it.bssid == p.bssid }
        if (idx >= 0) list[idx] = p else list.add(0, p)
        save(networkFile(ctx), list)
        _networkProfiles.value = list
    }

    fun findNetworkByBssid(bssid: String): NetworkProfile? =
        _networkProfiles.value.firstOrNull { it.bssid == bssid }

    fun findNetworksBySsid(ssid: String): List<NetworkProfile> =
        _networkProfiles.value.filter { it.ssid == ssid }

    // ── Firewall rules (Shizuku Mode) ───────────────────────────────────────────
    suspend fun upsertFirewallRule(ctx: Context, rule: FirewallRule) = withContext(Dispatchers.IO) {
        val list = _firewallRules.value.toMutableList()
        val idx  = list.indexOfFirst { it.packageName == rule.packageName }
        if (idx >= 0) list[idx] = rule else list.add(rule)
        save(firewallFile(ctx), list)
        _firewallRules.value = list
    }

    fun isFirewallBlocked(packageName: String): Boolean =
        _firewallRules.value.firstOrNull { it.packageName == packageName }?.blocked ?: false

    suspend fun clearFirewallRules(ctx: Context) = withContext(Dispatchers.IO) {
        save(firewallFile(ctx), emptyList<FirewallRule>())
        _firewallRules.value = emptyList()
    }

    // ── File paths ────────────────────────────────────────────────────────────
    private fun dataDir(ctx: Context) =
        File(ctx.filesDir, "aegis_data").also { it.mkdirs() }

    private fun scanFile(ctx: Context)    = File(dataDir(ctx), "scan_results.json")
    private fun breachFile(ctx: Context)  = File(dataDir(ctx), "breach_records.json")
    private fun networkFile(ctx: Context) = File(dataDir(ctx), "network_profiles.json")
    private fun firewallFile(ctx: Context) = File(dataDir(ctx), "firewall_rules.json")

    // ── JSON helpers ──────────────────────────────────────────────────────────
    private inline fun <reified T> load(file: File): List<T> = try {
        if (file.exists()) json.decodeFromString<List<T>>(file.readText()) else emptyList()
    } catch (e: Exception) { emptyList() }

    private inline fun <reified T> save(file: File, list: List<T>) {
        file.writeText(json.encodeToString(list))
    }
}
