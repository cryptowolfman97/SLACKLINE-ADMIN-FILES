package com.example.omnicortex.engine

import android.content.Context
import android.net.wifi.WifiManager
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.data.models.Severity

object NetworkScanEngine {

    data class ScanResult(
        val currentSsid: String,
        val currentBssid: String,
        val currentSecurity: String,
        val currentFrequency: Int,
        val signalLevel: Int,
        val nearbyNetworks: List<NearbyNetwork>,
        val threats: List<NetworkThreat>
    )

    data class NearbyNetwork(
        val ssid: String,
        val bssid: String,
        val security: String,
        val signalLevel: Int,
        val frequency: Int,
        val isTrusted: Boolean = false
    )

    data class NetworkThreat(
        val type: String,
        val targetSsid: String,
        val detail: String,
        val severity: Severity
    )

    fun parseSecurity(capabilities: String): String = when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA")  -> "WPA"
        capabilities.contains("WEP")  -> "WEP"
        capabilities.isBlank()        -> "OPEN"
        else                          -> "OPEN"
    }

    fun securityColor(security: String) = when (security) {
        "WPA3" -> "green"
        "WPA2" -> "cyan"
        "WPA"  -> "amber"
        else   -> "red"
    }

    fun detectThreats(
        connectedSsid: String,
        connectedBssid: String,
        nearby: List<NearbyNetwork>
    ): List<NetworkThreat> {
        val threats = mutableListOf<NetworkThreat>()

        val sameNameNetworks = nearby.filter {
            it.ssid.equals(connectedSsid, ignoreCase = true) && it.bssid != connectedBssid
        }
        if (sameNameNetworks.isNotEmpty()) {
            threats += NetworkThreat(
                type       = "evil_twin",
                targetSsid = connectedSsid,
                detail     = "Network \"$connectedSsid\" is broadcasting from ${sameNameNetworks.size + 1} " +
                             "different access points. One may be a rogue evil twin attempting to intercept traffic.",
                severity   = Severity.CRITICAL
            )
        }

        val currentNet = nearby.firstOrNull { it.bssid == connectedBssid }
        if (currentNet != null && currentNet.security == "OPEN") {
            threats += NetworkThreat(
                type       = "open_network",
                targetSsid = connectedSsid,
                detail     = "You are connected to an open (unencrypted) network. All traffic can be intercepted in plaintext.",
                severity   = Severity.HIGH
            )
        }

        if (currentNet?.security == "WEP") {
            threats += NetworkThreat(
                type       = "weak_encryption",
                targetSsid = connectedSsid,
                detail     = "Network uses WEP encryption which is cryptographically broken and can be cracked in minutes.",
                severity   = Severity.HIGH
            )
        }

        val honeypotNames = listOf(
            "free wifi", "freewifi", "free_wifi", "public wifi", "guest wifi",
            "starbucks", "airport wifi", "hotel wifi", "xfinity", "attwifi"
        )
        if (honeypotNames.any { connectedSsid.lowercase().contains(it) }) {
            threats += NetworkThreat(
                type       = "ssid_spoof",
                targetSsid = connectedSsid,
                detail     = "\"$connectedSsid\" matches a commonly spoofed honeypot network name used in MITM attacks.",
                severity   = Severity.MEDIUM
            )
        }

        val unknownCount = nearby.count { !it.isTrusted && it.ssid != connectedSsid }
        if (unknownCount > 15) {
            threats += NetworkThreat(
                type       = "dense_environment",
                targetSsid = connectedSsid,
                detail     = "$unknownCount unknown networks detected nearby. Dense public WiFi environments increase interception risk.",
                severity   = Severity.LOW
            )
        }

        return threats
    }

    @Suppress("DEPRECATION")
    fun scan(ctx: Context): ScanResult? {
        val wm = ctx.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val connInfo = wm.connectionInfo ?: return null
        val rawSsid  = connInfo.ssid ?: return null
        val currentSsid  = rawSsid.removePrefix("\"").removeSuffix("\"")
        val currentBssid = connInfo.bssid ?: ""

        if (currentSsid == "<unknown ssid>" || currentSsid.isBlank()) return null

        val scanResults = wm.scanResults ?: emptyList()

        val nearby = scanResults.map { r ->
            NearbyNetwork(
                ssid        = r.SSID ?: "",
                bssid       = r.BSSID ?: "",
                security    = parseSecurity(r.capabilities ?: ""),
                signalLevel = WifiManager.calculateSignalLevel(r.level, 100),
                frequency   = r.frequency,
                isTrusted   = AegisStore.findNetworkByBssid(r.BSSID ?: "")?.trusted == true
            )
        }.sortedByDescending { it.signalLevel }

        val currentSecurity = parseSecurity(
            scanResults.firstOrNull { it.BSSID == currentBssid }?.capabilities ?: ""
        )

        val threats = detectThreats(currentSsid, currentBssid, nearby)

        return ScanResult(
            currentSsid      = currentSsid,
            currentBssid     = currentBssid,
            currentSecurity  = currentSecurity,
            currentFrequency = connInfo.frequency,
            signalLevel      = WifiManager.calculateSignalLevel(connInfo.rssi, 100),
            nearbyNetworks   = nearby,
            threats          = threats
        )
    }
}