package com.example.omnicortex.engine

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.example.omnicortex.data.models.PostureCategory
import com.example.omnicortex.data.models.PostureFinding
import com.example.omnicortex.data.models.Severity
import java.util.UUID

object PostureEngine {

    // ── Run all checks, return findings + score ───────────────────────────────
    fun runFullAudit(ctx: Context): PostureAuditResult {
        val findings = mutableListOf<PostureFinding>()
        findings += checkLockAndAccess(ctx)
        findings += checkDeveloperExposure(ctx)
        findings += checkAppEcosystem(ctx)
        findings += checkOsIntegrity(ctx)
        findings += checkNetworkHygiene(ctx)
        val score = computeScore(findings)
        return PostureAuditResult(score, findings)
    }

    // ── Category 1: Lock & Access ─────────────────────────────────────────────
    private fun checkLockAndAccess(ctx: Context): List<PostureFinding> {
        val km  = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val findings = mutableListOf<PostureFinding>()

        // Screen lock set
        val hasLock = km.isDeviceSecure
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.LOCK_ACCESS,
            title    = "Screen Lock",
            detail   = if (hasLock) "A screen lock is set on this device."
                       else "No screen lock is configured. Device is accessible to anyone.",
            severity = if (hasLock) Severity.INFO else Severity.CRITICAL,
            passed   = hasLock,
            fixAdvice = "Enable a PIN, pattern, or biometric lock in Settings → Security."
        )

        // Device encryption
        val encStatus = dpm.storageEncryptionStatus
        val isEncrypted = encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER ||
                encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.LOCK_ACCESS,
            title    = "Storage Encryption",
            detail   = if (isEncrypted) "Device storage is encrypted."
                       else "Storage encryption is not active. Data is readable without lock bypass.",
            severity = if (isEncrypted) Severity.INFO else Severity.CRITICAL,
            passed   = isEncrypted,
            fixAdvice = "Enable encryption in Settings → Security → Encryption."
        )

        // Keyguard disabled flag
        val kgDisabled = Settings.Global.getInt(
            ctx.contentResolver, "keyguard_disabled_features", 0
        ) != 0
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.LOCK_ACCESS,
            title    = "Keyguard Features",
            detail   = if (!kgDisabled) "Keyguard security features are fully active."
                       else "Some keyguard security features are disabled by a policy or app.",
            severity = if (!kgDisabled) Severity.INFO else Severity.MEDIUM,
            passed   = !kgDisabled,
            fixAdvice = "Review device admin apps that may be disabling lock-screen features."
        )

        return findings
    }

    // ── Category 2: Developer Exposure ────────────────────────────────────────
    private fun checkDeveloperExposure(ctx: Context): List<PostureFinding> {
        val findings = mutableListOf<PostureFinding>()
        val cr = ctx.contentResolver

        // USB debugging
        val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.DEVELOPER_EXPOSURE,
            title    = "USB Debugging (ADB)",
            detail   = if (!adbEnabled) "USB debugging is disabled."
                       else "ADB is enabled. A connected computer can extract data or install apps silently.",
            severity = if (!adbEnabled) Severity.INFO else Severity.HIGH,
            passed   = !adbEnabled,
            fixAdvice = "Disable USB debugging in Settings → Developer Options unless actively needed."
        )

        // Developer options enabled
        val devOpts = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.DEVELOPER_EXPOSURE,
            title    = "Developer Options",
            detail   = if (!devOpts) "Developer options are disabled."
                       else "Developer options are active, exposing advanced system controls.",
            severity = if (!devOpts) Severity.INFO else Severity.MEDIUM,
            passed   = !devOpts,
            fixAdvice = "Disable developer options in Settings → Developer Options → toggle off."
        )

        // Mock locations
        val mockLoc = Settings.Secure.getInt(cr, Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.DEVELOPER_EXPOSURE,
            title    = "Mock Locations",
            detail   = if (!mockLoc) "Mock location apps are not permitted."
                       else "Mock locations are enabled. Apps may receive fake GPS coordinates.",
            severity = if (!mockLoc) Severity.INFO else Severity.MEDIUM,
            passed   = !mockLoc,
            fixAdvice = "Disable mock locations in Settings → Developer Options → Allow mock locations."
        )

        // OEM unlock
        val oemUnlock = Settings.Global.getInt(cr, "oem_unlock_enabled", 0) == 1
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.DEVELOPER_EXPOSURE,
            title    = "OEM Unlock",
            detail   = if (!oemUnlock) "OEM unlock is disabled. Bootloader is protected."
                       else "OEM unlock is enabled. Bootloader can be unlocked, bypassing full-disk encryption.",
            severity = if (!oemUnlock) Severity.INFO else Severity.HIGH,
            passed   = !oemUnlock,
            fixAdvice = "Disable OEM unlock in Settings → Developer Options if not needed."
        )

        return findings
    }

    // ── Category 3: App Ecosystem ─────────────────────────────────────────────
    private fun checkAppEcosystem(ctx: Context): List<PostureFinding> {
        val findings = mutableListOf<PostureFinding>()
        val pm = ctx.packageManager

        // Unknown sources / sideloading
        val unknownSources = Settings.Secure.getInt(
            ctx.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0
        ) == 1
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.APP_ECOSYSTEM,
            title    = "Install Unknown Apps",
            detail   = if (!unknownSources) "Installation from unknown sources is restricted."
                       else "Apps from unknown sources are permitted. Malicious APKs can be silently installed.",
            severity = if (!unknownSources) Severity.INFO else Severity.HIGH,
            passed   = !unknownSources,
            fixAdvice = "Revoke sideload permission per-app in Settings → Apps → Special access → Install unknown apps."
        )

        // Device admin apps count
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminApps = dpm.activeAdmins ?: emptyList()
        val suspiciousAdmins = adminApps.count { admin ->
            try {
                val info = pm.getApplicationInfo(admin.packageName, 0)
                (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            } catch (e: Exception) { false }
        }
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.APP_ECOSYSTEM,
            title    = "Device Admin Apps",
            detail   = when {
                adminApps.isEmpty() -> "No device administrator apps are active."
                suspiciousAdmins == 0 -> "${adminApps.size} admin app(s) active — all appear to be system apps."
                else -> "$suspiciousAdmins non-system app(s) have device administrator privileges. This is unusual."
            },
            severity = when {
                suspiciousAdmins > 0 -> Severity.HIGH
                adminApps.size > 2   -> Severity.MEDIUM
                else                 -> Severity.INFO
            },
            passed   = suspiciousAdmins == 0,
            fixAdvice = "Review device admin apps in Settings → Security → Device admin apps."
        )

        // Accessibility services with potential risk
        val accessibilityEnabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val accessibilityServices = accessibilityEnabled
            .split(":").filter { it.isNotBlank() }
            .filter { svc ->
                try {
                    val pkg = svc.substringBefore("/")
                    val info = pm.getApplicationInfo(pkg, 0)
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } catch (e: Exception) { false }
            }
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.APP_ECOSYSTEM,
            title    = "Accessibility Services",
            detail   = if (accessibilityServices.isEmpty())
                "No non-system accessibility services are active."
            else
                "${accessibilityServices.size} non-system app(s) have accessibility access. These can read screen content and simulate input.",
            severity = if (accessibilityServices.isEmpty()) Severity.INFO else Severity.HIGH,
            passed   = accessibilityServices.isEmpty(),
            fixAdvice = "Review accessibility services in Settings → Accessibility and disable any you don't recognise."
        )

        return findings
    }

    // ── Category 4: OS Integrity ──────────────────────────────────────────────
    private fun checkOsIntegrity(ctx: Context): List<PostureFinding> {
        val findings = mutableListOf<PostureFinding>()

        // Android version / patch level
        val sdkVersion   = Build.VERSION.SDK_INT
        val patchLevel   = Build.VERSION.SECURITY_PATCH  // "2024-05-01"
        val isOldVersion = sdkVersion < 31  // Android 12 is minimum for full security
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.OS_INTEGRITY,
            title    = "Android Version",
            detail   = "Running Android ${Build.VERSION.RELEASE} (API $sdkVersion). Security patch: $patchLevel." +
                       if (isOldVersion) " This version no longer receives full security updates." else " Version is acceptable.",
            severity = if (isOldVersion) Severity.HIGH else Severity.INFO,
            passed   = !isOldVersion,
            fixAdvice = "Update to the latest available Android version in Settings → System → System Update."
        )

        // Check patch recency (warn if > 6 months old)
        val patchOld = try {
            val parts = patchLevel.split("-")
            val patchYear  = parts[0].toInt()
            val patchMonth = parts[1].toInt()
            val now = java.util.Calendar.getInstance()
            val diffMonths = (now.get(java.util.Calendar.YEAR) - patchYear) * 12 +
                    (now.get(java.util.Calendar.MONTH) + 1 - patchMonth)
            diffMonths > 6
        } catch (e: Exception) { false }

        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.OS_INTEGRITY,
            title    = "Security Patch Age",
            detail   = if (!patchOld) "Security patch ($patchLevel) is reasonably current."
                       else "Security patch ($patchLevel) is over 6 months old. Known CVEs may be unpatched.",
            severity = if (!patchOld) Severity.INFO else Severity.HIGH,
            passed   = !patchOld,
            fixAdvice = "Apply the latest security patch from Settings → System → System Update."
        )

        // Root detection (basic heuristics — no library needed)
        val suPaths = listOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
        )
        val rooted = suPaths.any { java.io.File(it).exists() } ||
                try { Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                    .inputStream.bufferedReader().readLine() != null
                } catch (e: Exception) { false }

        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.OS_INTEGRITY,
            title    = "Root Detection",
            detail   = if (!rooted) "No root indicators detected. OS integrity appears intact."
                       else "Root indicators detected. System partition may be modified, bypassing security guarantees.",
            severity = if (!rooted) Severity.INFO else Severity.CRITICAL,
            passed   = !rooted,
            fixAdvice = "Rooted devices cannot guarantee OS integrity. Consider factory reset and fresh install."
        )

        // Build integrity (test-keys indicate custom ROM)
        val buildTags = Build.TAGS ?: ""
        val testKeys  = buildTags.contains("test-keys")
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.OS_INTEGRITY,
            title    = "Build Integrity",
            detail   = if (!testKeys) "Build signed with release keys. OS appears unmodified."
                       else "Build signed with test-keys. This indicates a custom or unofficial ROM.",
            severity = if (!testKeys) Severity.INFO else Severity.HIGH,
            passed   = !testKeys,
            fixAdvice = "Install official firmware from your device manufacturer to restore build integrity."
        )

        return findings
    }

    // ── Category 5: Network Hygiene ───────────────────────────────────────────
    private fun checkNetworkHygiene(ctx: Context): List<PostureFinding> {
        val findings = mutableListOf<PostureFinding>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // VPN active
        val network = cm.activeNetwork
        val caps    = cm.getNetworkCapabilities(network)
        val vpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        findings += PostureFinding(
            id       = UUID.randomUUID().toString(),
            category = PostureCategory.NETWORK_HYGIENE,
            title    = "VPN Protection",
            detail   = if (vpnActive) "A VPN is currently active. Network traffic is tunnelled."
                       else "No VPN is active. Network traffic may be intercepted on untrusted networks.",
            severity = if (vpnActive) Severity.INFO else Severity.MEDIUM,
            passed   = vpnActive,
            fixAdvice = "Use a trusted VPN when connecting to public or untrusted WiFi networks."
        )

        // WiFi security check
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val connInfo = wm.connectionInfo
        val isWifi   = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (isWifi && connInfo != null) {
            val ssid = connInfo.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: ""
            val isOpen = connInfo.supplicantState.name.contains("COMPLETED") &&
                    wm.configuredNetworks?.find {
                        it.SSID == "\"$ssid\""
                    }?.allowedKeyManagement?.get(0) == true  // rough open-network heuristic

            findings += PostureFinding(
                id       = UUID.randomUUID().toString(),
                category = PostureCategory.NETWORK_HYGIENE,
                title    = "WiFi Network",
                detail   = "Connected to: ${ssid.ifBlank { "Unknown" }}. " +
                           "Use the Network Intelligence module for full threat analysis of this network.",
                severity = Severity.INFO,
                passed   = true,
                fixAdvice = "Run a full network scan in the Network Intelligence module."
            )
        }

        // HTTP proxy
        val proxyHost = System.getProperty("http.proxyHost")
        val hasProxy  = !proxyHost.isNullOrBlank()
        if (hasProxy) {
            findings += PostureFinding(
                id       = UUID.randomUUID().toString(),
                category = PostureCategory.NETWORK_HYGIENE,
                title    = "HTTP Proxy Detected",
                detail   = "A system-level HTTP proxy is configured ($proxyHost). All HTTP traffic passes through it.",
                severity = Severity.HIGH,
                passed   = false,
                fixAdvice = "If you did not set this proxy, remove it in WiFi settings → Proxy. This could indicate a MitM setup."
            )
        }

        return findings
    }

    // ── Score computation ─────────────────────────────────────────────────────
    // Weight: CRITICAL=-25, HIGH=-12, MEDIUM=-6, LOW=-2, INFO=0
    // Start at 100, deduct per failed check weighted by severity
    fun computeScore(findings: List<PostureFinding>): Int {
        var score = 100
        for (f in findings) {
            if (!f.passed) {
                score -= when (f.severity) {
                    Severity.CRITICAL -> 25
                    Severity.HIGH     -> 12
                    Severity.MEDIUM   -> 6
                    Severity.LOW      -> 2
                    Severity.INFO     -> 0
                }
            }
        }
        return score.coerceIn(0, 100)
    }

    fun scoreGrade(score: Int): String = when {
        score >= 90 -> "A"
        score >= 75 -> "B"
        score >= 60 -> "C"
        score >= 40 -> "D"
        else        -> "F"
    }

    fun scoreLabel(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Good"
        score >= 60 -> "Fair"
        score >= 40 -> "At Risk"
        else        -> "Critical"
    }

    fun scoreColor(score: Int) = when {
        score >= 75 -> "green"
        score >= 50 -> "amber"
        else        -> "red"
    }
}

data class PostureAuditResult(
    val score: Int,
    val findings: List<PostureFinding>
) {
    val criticalCount get() = findings.count { !it.passed && it.severity == Severity.CRITICAL }
    val highCount     get() = findings.count { !it.passed && it.severity == Severity.HIGH }
    val mediumCount   get() = findings.count { !it.passed && it.severity == Severity.MEDIUM }
    val passedCount   get() = findings.count { it.passed }
    val totalCount    get() = findings.size
    val grade         get() = PostureEngine.scoreGrade(score)
    val label         get() = PostureEngine.scoreLabel(score)
}
