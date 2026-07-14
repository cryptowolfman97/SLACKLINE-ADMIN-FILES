package com.example.omnicortex.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.omnicortex.data.models.Severity

object ApkAnalyserEngine {

    data class ApkAnalysisResult(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val targetSdk: Int,
        val minSdk: Int,
        val isDebuggable: Boolean,
        val isBackupAllowed: Boolean,
        val usesCleartextTraffic: Boolean,
        val hasNetworkSecurityConfig: Boolean,
        val exportedComponents: List<ExportedComponent>,
        val dangerousPermissions: List<String>,
        val signatureInfo: SignatureInfo,
        val hardcodedSecrets: List<SecretFinding>,
        val findings: List<ApkFinding>
    )

    data class ExportedComponent(
        val name: String,
        val type: String,       // Activity | Service | Receiver | Provider
        val hasIntentFilter: Boolean,
        val isProtected: Boolean,
        val permission: String
    )

    data class SignatureInfo(
        val algorithm: String,
        val isDebugSigned: Boolean,
        val subjectDN: String,
        val validFrom: String,
        val validTo: String
    )

    data class SecretFinding(
        val type: String,
        val preview: String,
        val location: String
    )

    data class ApkFinding(
        val title: String,
        val detail: String,
        val severity: Severity,
        val passed: Boolean,
        val category: String
    )

    private val SECRET_PATTERNS = mapOf(
        "API Key" to Regex("""(?i)(api[_-]?key|apikey)\s*[=:]\s*["']?([A-Za-z0-9\-_]{20,})["']?"""),
        "AWS Access Key" to Regex("""AKIA[0-9A-Z]{16}"""),
        "Private Key Header" to Regex("""-----BEGIN (RSA |EC |DSA )?PRIVATE KEY-----"""),
        "Google API Key" to Regex("""AIza[0-9A-Za-z\-_]{35}"""),
        "Firebase URL" to Regex("""https://[a-z0-9-]+\.firebaseio\.com"""),
        "Basic Auth" to Regex("""(?i)Authorization:\s*Basic\s+[A-Za-z0-9+/]+=*"""),
        "Bearer Token" to Regex("""(?i)Bearer\s+[A-Za-z0-9\-._~+/]+=*"""),
        "Password in URL" to Regex("""(?i)://[^:]+:[^@]+@"""),
        "Hardcoded Password" to Regex("""(?i)(password|passwd|pwd)\s*[=:]\s*["'][^"']{4,}["']"""),
        "Secret Key" to Regex("""(?i)(secret[_-]?key|secret)\s*[=:]\s*["'][^"']{8,}["']""")
    )

    fun analyse(ctx: Context, packageName: String): ApkAnalysisResult {
        val pm  = ctx.packageManager
        val pkg = pm.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS or
            PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val appInfo = pkg.applicationInfo ?: throw IllegalStateException("ApplicationInfo is null for $packageName")
        val findings = mutableListOf<ApkFinding>()

        // ── Basic flags ───────────────────────────────────────────────────────
        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val isBackup     = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        @Suppress("DEPRECATION")
        val cleartext    = (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        val hasNsc       = appInfo.metaData?.containsKey("android.security.net.config") == true

        if (isDebuggable) findings += ApkFinding(
            "Debuggable Flag Enabled",
            "android:debuggable=true allows ADB debugging, bypassing sandbox and exposing internal state.",
            Severity.CRITICAL, false, "Manifest"
        )

        if (isBackup) findings += ApkFinding(
            "Backup Allowed",
            "android:allowBackup=true allows ADB backup of app data without root. Sensitive data could be extracted.",
            Severity.HIGH, false, "Manifest"
        ) else findings += ApkFinding(
            "Backup Disabled", "android:allowBackup=false.", Severity.INFO, true, "Manifest"
        )

        if (cleartext) findings += ApkFinding(
            "Cleartext Traffic Permitted",
            "usesCleartextTraffic=true allows HTTP connections. Credentials and data transmitted in plaintext.",
            Severity.HIGH, false, "Manifest"
        ) else findings += ApkFinding(
            "Cleartext Traffic Blocked", "App enforces HTTPS.", Severity.INFO, true, "Manifest"
        )

        if (!hasNsc) findings += ApkFinding(
            "No Network Security Config",
            "No network_security_config.xml defined. App uses default network security settings.",
            Severity.LOW, false, "Manifest"
        )

        // ── Target SDK ────────────────────────────────────────────────────────
        val targetSdk = appInfo.targetSdkVersion  // safe: appInfo is non-null here
        if (targetSdk < 29) findings += ApkFinding(
            "Low Target SDK ($targetSdk)",
            "Targeting API $targetSdk misses security improvements in newer Android versions.",
            Severity.MEDIUM, false, "Manifest"
        )

        // ── Exported components ───────────────────────────────────────────────
        val exported = mutableListOf<ExportedComponent>()

        pkg.activities?.forEach { act ->
            if (act.exported) {
                // intentFilters is not populated via PackageManager flags; derive from export state
                val hasIntentFilter = false  // conservative: treat as unfiltered for security analysis
                val perm = act.permission ?: ""
                exported += ExportedComponent(
                    act.name.substringAfterLast("."), "Activity", hasIntentFilter, perm.isNotBlank(), perm
                )
                if (!hasIntentFilter && perm.isBlank()) {
                    findings += ApkFinding(
                        "Exported Activity: ${act.name.substringAfterLast(".")}",
                        "Activity is exported without intent filter or permission — accessible to any app.",
                        Severity.HIGH, false, "Components"
                    )
                }
            }
        }

        pkg.services?.forEach { svc ->
            if (svc.exported) {
                val perm = svc.permission ?: ""
                exported += ExportedComponent(
                    svc.name.substringAfterLast("."), "Service", false, perm.isNotBlank(), perm
                )
                if (perm.isBlank()) findings += ApkFinding(
                    "Exported Service: ${svc.name.substringAfterLast(".")}",
                    "Service exported without permission — any app can bind or start it.",
                    Severity.HIGH, false, "Components"
                )
            }
        }

        pkg.receivers?.forEach { rec ->
            if (rec.exported) {
                val perm = rec.permission ?: ""
                exported += ExportedComponent(
                    rec.name.substringAfterLast("."), "Receiver", false, perm.isNotBlank(), perm
                )
                if (perm.isBlank()) findings += ApkFinding(
                    "Exported Receiver: ${rec.name.substringAfterLast(".")}",
                    "Broadcast receiver exported without permission.",
                    Severity.MEDIUM, false, "Components"
                )
            }
        }

        pkg.providers?.forEach { prov ->
            if (prov.exported) {
                val hasPerms = prov.readPermission != null || prov.writePermission != null
                exported += ExportedComponent(
                    prov.name.substringAfterLast("."), "Provider", false, hasPerms, prov.readPermission ?: ""
                )
                if (!hasPerms) findings += ApkFinding(
                    "Exported Content Provider: ${prov.name.substringAfterLast(".")}",
                    "Content provider is exported without read/write permissions — data readable by any app.",
                    Severity.CRITICAL, false, "Components"
                )
            }
        }

        // ── Dangerous permissions ─────────────────────────────────────────────
        val dangerous = pkg.requestedPermissions?.filter { perm ->
            try {
                val info = pm.getPermissionInfo(perm, 0)
                info.protectionLevel and 0xff == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
            } catch (e: Exception) { false }
        } ?: emptyList()

        if (dangerous.size > 8) findings += ApkFinding(
            "Excessive Dangerous Permissions (${dangerous.size})",
            "App requests ${dangerous.size} dangerous permissions — significantly above average.",
            Severity.HIGH, false, "Permissions"
        )

        // ── Signature ─────────────────────────────────────────────────────────
        val sigInfo = analyseSignature(pkg)
        if (sigInfo.isDebugSigned) findings += ApkFinding(
            "Debug Certificate",
            "App is signed with a debug certificate. Never ship to production with debug signing.",
            Severity.CRITICAL, false, "Signing"
        )

        // ── Scan APK for hardcoded secrets ────────────────────────────────────
        val secrets = scanForSecrets(ctx, packageName, appInfo)
        secrets.forEach { s ->
            findings += ApkFinding(
                "Hardcoded ${s.type} Detected",
                "Found ${s.type}: \"${s.preview}\" in ${s.location}",
                Severity.CRITICAL, false, "Secrets"
            )
        }

        val appName = try { pm.getApplicationLabel(appInfo).toString() } catch (e: Exception) { packageName }

        return ApkAnalysisResult(
            packageName         = packageName,
            appName             = appName,
            versionName         = pkg.versionName ?: "",
            versionCode         = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong(),
            targetSdk           = targetSdk,
            minSdk              = appInfo.minSdkVersion,
            isDebuggable        = isDebuggable,
            isBackupAllowed     = isBackup,
            usesCleartextTraffic = cleartext,
            hasNetworkSecurityConfig = hasNsc,
            exportedComponents  = exported,
            dangerousPermissions = dangerous,
            signatureInfo       = sigInfo,
            hardcodedSecrets    = secrets,
            findings            = findings.sortedBy { it.passed }
        )
    }

    private fun analyseSignature(pkg: PackageInfo): SignatureInfo {
        return try {
            val cert = if (Build.VERSION.SDK_INT >= 28) {
                pkg.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                pkg.signatures?.firstOrNull()
            }
            if (cert != null) {
                val x509 = java.security.cert.CertificateFactory
                    .getInstance("X509")
                    .generateCertificate(java.io.ByteArrayInputStream(cert.toByteArray()))
                    as java.security.cert.X509Certificate
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                SignatureInfo(
                    algorithm    = x509.sigAlgName,
                    isDebugSigned = x509.subjectDN.name.contains("Android Debug", ignoreCase = true),
                    subjectDN    = x509.subjectDN.name.take(80),
                    validFrom    = fmt.format(x509.notBefore),
                    validTo      = fmt.format(x509.notAfter)
                )
            } else SignatureInfo("Unknown", false, "Unknown", "", "")
        } catch (e: Exception) {
            SignatureInfo("Unknown", false, "Error reading signature", "", "")
        }
    }

    private fun scanForSecrets(ctx: Context, packageName: String, appInfo: ApplicationInfo): List<SecretFinding> {
        val secrets = mutableListOf<SecretFinding>()
        // Scan the APK source directory for shared prefs / databases path hints
        try {
            val apkPath = appInfo.sourceDir
            val apkFile = java.io.File(apkPath)
            if (apkFile.exists()) {
                val zip = java.util.zip.ZipFile(apkFile)
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".xml") || it.name.endsWith(".json") || it.name.endsWith("assets/") }
                    .take(30)
                    .forEach { entry ->
                        try {
                            val content = zip.getInputStream(entry).bufferedReader().readText().take(8192)
                            SECRET_PATTERNS.forEach { (type, pattern) ->
                                pattern.find(content)?.let { match ->
                                    val preview = match.value.take(60)
                                    secrets += SecretFinding(type, preview, entry.name)
                                }
                            }
                        } catch (e: Exception) { /* skip unreadable entries */ }
                    }
                zip.close()
            }
        } catch (e: Exception) { /* APK scan failed */ }
        return secrets.distinctBy { it.type + it.location }
    }

    fun getInstalledUserApps(ctx: Context): List<Pair<String, String>> {
        val pm = ctx.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { app ->
                val label = try { pm.getApplicationLabel(app).toString() } catch (e: Exception) { app.packageName }
                Pair(app.packageName, label)
            }
            .sortedBy { it.second }
    }
}
