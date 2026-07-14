package com.example.omnicortex.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.omnicortex.data.models.AppRiskEntry
import com.example.omnicortex.data.models.RiskFlag
import com.example.omnicortex.data.models.Severity

object AppRiskEngine {

    // High-risk permission groups mapped to human labels
    private val EXFIL_PERMS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO"
    )

    private val SURVEILLANCE_PERMS = setOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.CAPTURE_AUDIO_OUTPUT",
        "android.permission.CAMERA"
    )

    private val LOCATION_PERMS = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    private val CREDENTIAL_PERMS = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.READ_FRAME_BUFFER",
        "android.permission.CAPTURE_SECURE_VIDEO_OUTPUT"
    )

    private val NETWORK_PERMS = setOf(
        "android.permission.INTERNET",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE"
    )

    private val BACKGROUND_PERMS = setOf(
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.BIND_DEVICE_ADMIN"
    )

    data class ScanResult(
        val apps: List<AppRiskEntry>,
        val criticalCount: Int,
        val highCount: Int,
        val totalScanned: Int
    )

    fun scanInstalledApps(ctx: Context): ScanResult {
        val pm    = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            PackageManager.GET_PERMISSIONS else 0

        val packages = try {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) { emptyList() }

        val results = packages.mapNotNull { app ->
            // Skip system apps for cleaner results — user-installed only
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@mapNotNull null

            val pkgInfo = try {
                pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            } catch (e: Exception) { return@mapNotNull null }

            val grantedPerms = pkgInfo.requestedPermissions
                ?.filterIndexed { idx, _ ->
                    (pkgInfo.requestedPermissionsFlags?.getOrNull(idx)
                        ?: 0) and PackageManager.GET_PERMISSIONS != 0
                }?.toSet() ?: emptySet()

            val allPerms = pkgInfo.requestedPermissions?.toSet() ?: emptySet()

            val flags = analysePermissions(allPerms, grantedPerms)
            if (flags.isEmpty()) return@mapNotNull null

            val appName = try { pm.getApplicationLabel(app).toString() }
                          catch (e: Exception) { app.packageName }

            val score = computeRiskScore(flags)
            AppRiskEntry(
                packageName = app.packageName,
                appName     = appName,
                riskScore   = score,
                riskFlags   = flags
            )
        }.sortedByDescending { it.riskScore }

        return ScanResult(
            apps          = results,
            criticalCount = results.count { it.riskScore >= 80 },
            highCount     = results.count { it.riskScore in 50..79 },
            totalScanned  = packages.count { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        )
    }

    private fun analysePermissions(
        allPerms: Set<String>,
        grantedPerms: Set<String>
    ): List<RiskFlag> {
        val flags = mutableListOf<RiskFlag>()
        val hasInternet = allPerms.any { it in NETWORK_PERMS }
        val hasBackground = allPerms.any { it in BACKGROUND_PERMS }

        // Data exfiltration
        val exfilPerms = allPerms.intersect(EXFIL_PERMS)
        if (exfilPerms.isNotEmpty() && hasInternet) {
            flags += RiskFlag(
                label    = "Data Exfiltration Risk",
                detail   = "Requests access to sensitive data (${exfilPerms.size} data permission(s)) plus internet access. Could silently upload personal data.",
                severity = if (exfilPerms.size >= 3) Severity.CRITICAL else Severity.HIGH
            )
        }

        // Surveillance
        val survPerms = allPerms.intersect(SURVEILLANCE_PERMS)
        if (survPerms.isNotEmpty() && hasInternet) {
            val hasMic    = "android.permission.RECORD_AUDIO" in survPerms
            val hasCam    = "android.permission.CAMERA" in survPerms
            val label     = buildString {
                if (hasMic && hasCam) append("microphone and camera")
                else if (hasMic) append("microphone")
                else append("camera")
            }
            flags += RiskFlag(
                label    = "Surveillance Risk",
                detail   = "Requests $label access combined with internet. Could stream audio/video without obvious user interaction.",
                severity = if (hasMic && hasCam && hasBackground) Severity.CRITICAL else Severity.HIGH
            )
        }

        // Location stalking
        val locPerms = allPerms.intersect(LOCATION_PERMS)
        val hasBgLoc = "android.permission.ACCESS_BACKGROUND_LOCATION" in allPerms
        if (locPerms.isNotEmpty() && hasInternet) {
            flags += RiskFlag(
                label    = if (hasBgLoc) "Background Location Tracking" else "Location Tracking Risk",
                detail   = if (hasBgLoc)
                    "Requests background location — tracks position even when app is closed."
                else
                    "Requests precise location combined with internet access.",
                severity = if (hasBgLoc) Severity.HIGH else Severity.MEDIUM
            )
        }

        // Credential / accessibility risk
        val credPerms = allPerms.intersect(CREDENTIAL_PERMS)
        if (credPerms.isNotEmpty()) {
            flags += RiskFlag(
                label    = "Credential Access Risk",
                detail   = "Requests accessibility or screen-capture permissions. Can read on-screen content including passwords and PINs.",
                severity = Severity.CRITICAL
            )
        }

        // Silent install
        if ("android.permission.REQUEST_INSTALL_PACKAGES" in allPerms) {
            flags += RiskFlag(
                label    = "Silent Install Capability",
                detail   = "Can install other applications without user interaction if granted.",
                severity = Severity.HIGH
            )
        }

        // Boot persistence
        if ("android.permission.RECEIVE_BOOT_COMPLETED" in allPerms && hasBackground) {
            flags += RiskFlag(
                label    = "Persistent Background Run",
                detail   = "Starts automatically on device boot and runs in the background indefinitely.",
                severity = Severity.MEDIUM
            )
        }

        return flags
    }

    private fun computeRiskScore(flags: List<RiskFlag>): Int {
        var score = 0
        for (f in flags) {
            score += when (f.severity) {
                Severity.CRITICAL -> 40
                Severity.HIGH     -> 25
                Severity.MEDIUM   -> 12
                Severity.LOW      -> 5
                Severity.INFO     -> 0
            }
        }
        return score.coerceIn(0, 100)
    }
}
