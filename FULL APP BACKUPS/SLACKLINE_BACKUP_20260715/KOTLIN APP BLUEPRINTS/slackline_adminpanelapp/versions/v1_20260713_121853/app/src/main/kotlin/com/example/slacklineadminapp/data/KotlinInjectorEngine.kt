package com.example.slacklineadminapp.engine

import android.os.Environment
import com.example.slacklineadminapp.data.KotlinProduct
import java.io.File
import com.example.slacklineadminapp.data.AppStorage

// ── GateConfig — all injection-time constants for the gate screen ─────
data class GateConfig(
    val appDisplayName   : String = "",
    val proPrice         : String = "Contact us for pricing",
    val gracePeriodHours : Int    = 6,
    val bankName         : String = "",
    val bankAccName      : String = "",
    val bankAccount      : String = "",
    val bankBranch       : String = "",
    val bankSwift        : String = "",
    val cryptoUsdtBsc    : String = "",
    val cryptoUsdtTrc    : String = "",
    val cryptoUsdtPlasma : String = "",
    val cryptoEth        : String = "",
    val cryptoLtc        : String = "",
    val disclaimer       : String = ""
)

object KotlinInjectorEngine {

    val outputDir = AppStorage.injectedOutputsDir()

    init { if (!outputDir.exists()) outputDir.mkdirs() }

    // ── MANIFEST ──────────────────────────────────────────────────────

    fun patchManifest(content: String): String {
        var result = content
        if (!result.contains("android.permission.INTERNET")) {
            result = result.replaceFirst(
                "<application",
                "<uses-permission android:name=\"android.permission.INTERNET\" />\n\n    <application"
            )
        }
        if (!result.contains("android:name=\".SHVApplication\"")) {
            result = result.replace(
                Regex("<application\\s"),
                "<application\n        android:name=\".SHVApplication\"\n        "
            )
        }
        return result
    }

    fun patchMainActivity(content: String, product: KotlinProduct, packageName: String): String {
        return content
    }

    // ── LIBS.VERSIONS.TOML MERGER ─────────────────────────────────────

    fun mergeLibsVersionsToml(existing: String): String {
        val requiredVersions = mapOf(
            "composeBom"      to "\"2024.06.00\"",
            "activityCompose" to "\"1.9.0\"",
            "coroutines"      to "\"1.8.1\"",
            "kotlin"          to "\"2.1.0\""
        )
        val requiredPlugins = mapOf(
            "kotlin-compose" to "{ id = \"org.jetbrains.kotlin.plugin.compose\", version.ref = \"kotlin\" }"
        )
        val requiredLibraries = mapOf(
            "compose-bom"                     to "{ group = \"androidx.compose\", name = \"compose-bom\", version.ref = \"composeBom\" }",
            "compose-ui"                      to "{ group = \"androidx.compose.ui\", name = \"ui\" }",
            "compose-ui-graphics"             to "{ group = \"androidx.compose.ui\", name = \"ui-graphics\" }",
            "compose-ui-tooling-preview"      to "{ group = \"androidx.compose.ui\", name = \"ui-tooling-preview\" }",
            "compose-ui-tooling"              to "{ group = \"androidx.compose.ui\", name = \"ui-tooling\" }",
            "compose-ui-test-manifest"        to "{ group = \"androidx.compose.ui\", name = \"ui-test-manifest\" }",
            "compose-material3"               to "{ group = \"androidx.compose.material3\", name = \"material3\" }",
            "compose-material-icons-extended" to "{ group = \"androidx.compose.material\", name = \"material-icons-extended\" }",
            "activity-compose"                to "{ group = \"androidx.activity\", name = \"activity-compose\", version.ref = \"activityCompose\" }",
            "kotlinx-coroutines-android"      to "{ group = \"org.jetbrains.kotlinx\", name = \"kotlinx-coroutines-android\", version.ref = \"coroutines\" }"
        )

        val lines = existing.lines().toMutableList()

        fun keyExistsInSection(sectionHeader: String, key: String): Boolean {
            val start = lines.indexOfFirst { it.trim() == sectionHeader }
            if (start == -1) return false
            val nextSection = (start + 1 until lines.size).firstOrNull { lines[it].trim().startsWith("[") }
            val end = nextSection ?: lines.size
            val bare = key.replace("-", "").replace("_", "").lowercase()
            return (start until end).any { i ->
                val lb = lines[i].replace("-", "").replace("_", "").replace("\"", "").lowercase()
                lb.trimStart().startsWith("$bare =") || lb.trimStart().startsWith("$bare=")
            }
        }

        fun sectionInsertIndex(header: String): Int {
            val start = lines.indexOfFirst { it.trim() == header }
            if (start == -1) return -1
            val nextSection = (start + 1 until lines.size).firstOrNull { lines[it].trim().startsWith("[") }
            val end = nextSection ?: lines.size
            var insert = end
            while (insert > start + 1 && lines[insert - 1].isBlank()) insert--
            return insert
        }

        fun inject(header: String, newLines: List<String>) {
            val idx = sectionInsertIndex(header); if (idx == -1) return
            lines.addAll(idx, newLines)
        }

        val mv = requiredVersions.filterKeys { !keyExistsInSection("[versions]", it) }
        if (mv.isNotEmpty()) inject("[versions]", mv.map { (k, v) -> "$k = $v" })
        val mp = requiredPlugins.filterKeys { !keyExistsInSection("[plugins]", it) }
        if (mp.isNotEmpty()) inject("[plugins]", mp.map { (k, v) -> "$k = $v" })
        val ml = requiredLibraries.filterKeys { !keyExistsInSection("[libraries]", it) }
        if (ml.isNotEmpty()) inject("[libraries]", ml.map { (k, v) -> "$k = $v" })

        return lines.joinToString("\n")
    }

    // ── BUILD.GRADLE.KTS MERGER ───────────────────────────────────────

    fun mergeBuildGradleKts(existing: String): String {
        val sb = StringBuilder(existing)
        if (!existing.contains("kotlin.plugin.compose") && !existing.contains("kotlin-compose")) {
            val idx = findBlockEnd(existing, "plugins")
            if (idx != -1) sb.insert(idx, "\n    alias(libs.plugins.kotlin.compose)")
        }
        val s1 = sb.toString()
        if (!s1.contains("compose = true") && !s1.contains("compose=true")) {
            val bfIdx = findBlockEnd(s1, "buildFeatures")
            if (bfIdx != -1) sb.insert(bfIdx, "\n        compose = true")
            else {
                val dcIdx = findBlockEnd(sb.toString(), "defaultConfig")
                if (dcIdx != -1) sb.insert(dcIdx + 1, "\n\n    buildFeatures {\n        compose = true\n    }")
            }
        }
        val requiredDeps = linkedMapOf(
            "compose.bom"                     to "implementation(platform(libs.compose.bom))",
            "compose.ui"                      to "implementation(libs.compose.ui)",
            "compose.ui.graphics"             to "implementation(libs.compose.ui.graphics)",
            "compose.ui.tooling.preview"      to "implementation(libs.compose.ui.tooling.preview)",
            "compose.material3"               to "implementation(libs.compose.material3)",
            "compose.material.icons.extended" to "implementation(libs.compose.material.icons.extended)",
            "activity.compose"                to "implementation(libs.activity.compose)",
            "kotlinx.coroutines.android"      to "implementation(libs.kotlinx.coroutines.android)",
            "compose.ui.tooling"              to "debugImplementation(libs.compose.ui.tooling)",
            "compose.ui.test.manifest"        to "debugImplementation(libs.compose.ui.test.manifest)"
        )
        fun depExists(content: String, key: String): Boolean {
            val bare = key.replace(".", "").replace("-", "").replace("_", "").lowercase()
            return content.replace(".", "").replace("-", "").replace("_", "").lowercase().contains(bare)
        }
        val missing = requiredDeps.filterKeys { !depExists(sb.toString(), it) }
        if (missing.isNotEmpty()) {
            val depsIdx = findBlockEnd(sb.toString(), "dependencies")
            if (depsIdx != -1) {
                val inject = buildString {
                    append("\n\n    // ── SHV Compose Gate dependencies ──")
                    missing.values.forEach { append("\n    $it") }
                }
                sb.insert(depsIdx, inject)
            }
        }
        return sb.toString()
    }

    private fun findBlockEnd(content: String, blockName: String): Int {
        val match = Regex("""$blockName\s*\{""").find(content) ?: return -1
        var depth = 0; var i = match.range.first
        while (i < content.length) {
            when (content[i]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) return i } }
            i++
        }
        return -1
    }

    // ── SHVApplication GENERATOR ──────────────────────────────────────

    fun generateSHVApplication(packageName: String, product: KotlinProduct): String {
        return """
package $packageName

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import $packageName.shvgate.LicenseGateScreen

class SHVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(SHVGateCallbacks())
    }

    inner class SHVGateCallbacks : ActivityLifecycleCallbacks {

        private val gatedActivities = mutableSetOf<String>()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            val activityName = activity.javaClass.simpleName
            if (activityName !in gatedActivities) {
                gatedActivities.add(activityName)
                injectGate(activity)
            }
        }

        private fun injectGate(activity: Activity) {
            val accessGranted = mutableStateOf(false)
            val gateView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    if (!accessGranted.value) {
                        LicenseGateScreen(
                            context = activity,
                            onAccessGranted = {
                                accessGranted.value = true
                                (parent as? android.view.ViewGroup)?.removeView(this@apply)
                            }
                        )
                    }
                }
            }
            // Set solid black immediately so no app content flashes before gate draws
            activity.window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
            (activity.window.decorView as android.view.ViewGroup).addView(
                gateView,
                android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            gatedActivities.remove(activity.javaClass.simpleName)
        }
    }
}
        """.trimIndent()
    }

    // ── SHVLicense GENERATOR ──────────────────────────────────────────

    fun generateSHVLicense(packageName: String, product: KotlinProduct): String {
        val pemBlock = product.publicKeyPem.trim()
        return """
package $packageName.license

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.Inflater

object SHVLicense {

    private const val BUNDLE_APP   = "${product.appCode}"
    private const val ACT_PREFIX   = "${product.prefix}"
    private const val LICENSE_FILE = "shv_license_${product.appCode}.json"
    private const val SUPABASE_URL = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    private const val ANON_KEY     = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    private val PUBLIC_KEY_PEM = ""${'"'}
$pemBlock
    ""${'"'}.trimIndent()

    fun getDeviceCode(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "fallback"
        return MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(8).uppercase()
    }

    fun decodeTokenPublic(code: String): Pair<JSONObject, String> {
        val prefix  = "${'$'}{ACT_PREFIX}-"
        var cleaned = code.trim().replace("\n", "").replace(" ", "")
        if (cleaned.startsWith(prefix)) cleaned = cleaned.removePrefix(prefix)
        cleaned = cleaned.replace(".", "")
        val padded     = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
        val compressed = Base64.getUrlDecoder().decode(padded)
        val inflater   = Inflater(); inflater.setInput(compressed)
        val output = ByteArray(65536); val len = inflater.inflate(output); inflater.end()
        val json = JSONObject(String(output, 0, len, Charsets.UTF_8))
        return Pair(json.getJSONObject("p"), json.getString("s"))
    }

    private fun verify(payload: JSONObject, sigB64: String): Boolean {
        val canonical   = buildCanonicalJson(payload)
        val pemStripped = PUBLIC_KEY_PEM
            .replace(Regex("-----.*?-----"), "")
            .replace(Regex("\\s+"), "")
        val keyBytes = try { Base64.getDecoder().decode(pemStripped) }
                       catch (e: Exception) { throw Exception("Key Base64 decode failed") }
        val pubKey = try { KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes)) }
                     catch (e: Exception) {
                         try { KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(wrapPkcs1InX509(keyBytes))) }
                         catch (e2: Exception) { throw Exception("KeyFactory parsing failed") }
                     }
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(pubKey)
        sig.update(canonical.toByteArray(Charsets.UTF_8))
        val sigBytes = try { Base64.getUrlDecoder().decode(sigB64) }
                       catch (e: Exception) {
                           try { Base64.getDecoder().decode(sigB64) }
                           catch (e2: Exception) { throw Exception("Signature Base64 decode failed") }
                       }
        return sig.verify(sigBytes)
    }

    private fun wrapPkcs1InX509(pkcs1: ByteArray): ByteArray {
        val oid = byteArrayOf(0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,
            0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00)
        return derEncode(0x30, oid + derEncode(0x03, byteArrayOf(0x00) + pkcs1))
    }

    private fun derEncode(tag: Int, content: ByteArray): ByteArray {
        val len = content.size
        val lb  = when {
            len < 128 -> byteArrayOf(len.toByte())
            len < 256 -> byteArrayOf(0x81.toByte(), len.toByte())
            else      -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xff).toByte())
        }
        return byteArrayOf(tag.toByte()) + lb + content
    }

    private fun buildCanonicalJson(obj: JSONObject): String {
        val parts = obj.keys().asSequence().sorted()
            .map { k -> "\"${'$'}k\":${'$'}{canonicalValue(obj.get(k))}" }.toList()
        return "{${'$'}{parts.joinToString(",")}}"
    }

    private fun canonicalValue(value: Any?): String = when (value) {
        is JSONObject -> buildCanonicalJson(value)
        is String     -> "\"${'$'}value\""
        is Boolean    -> if (value) "true" else "false"
        null, JSONObject.NULL -> "null"
        else          -> value.toString()
    }

    fun isProductActive(productId: String): Boolean {
        return try {
            val url = "${'$'}SUPABASE_URL/rest/v1/kl_products" +
                "?product_id=eq.${'$'}{productId}&select=product_id&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${'$'}ANON_KEY")
            conn.connectTimeout = 8_000; conn.readTimeout = 8_000
            val code = conn.responseCode
            val resp = if (code in 200..299) conn.inputStream.bufferedReader().readText() else "[]"
            conn.disconnect()
            org.json.JSONArray(resp).length() > 0
        } catch (e: Exception) { true }
    }

    fun isRevokedOnServer(licenseId: String): Boolean {
        return try {
            val licUrl = "${'$'}SUPABASE_URL/rest/v1/kl_licenses" +
                "?license_id=eq.${'$'}{licenseId}&select=status&limit=1"
            val licConn = URL(licUrl).openConnection() as HttpURLConnection
            licConn.setRequestProperty("apikey", ANON_KEY)
            licConn.setRequestProperty("Authorization", "Bearer ${'$'}ANON_KEY")
            licConn.connectTimeout = 8_000; licConn.readTimeout = 8_000
            val licResp = licConn.inputStream.bufferedReader().readText()
            val licArr  = JSONArray(licResp)
            if (licArr.length() > 0) {
                val status = licArr.getJSONObject(0).optString("status", "active")
                if (status == "revoked") return true
            }
            val revUrl = "${'$'}SUPABASE_URL/rest/v1/kl_revocations" +
                "?payload->>license_id=eq.${'$'}{licenseId}&select=id&limit=1"
            val revConn = URL(revUrl).openConnection() as HttpURLConnection
            revConn.setRequestProperty("apikey", ANON_KEY)
            revConn.setRequestProperty("Authorization", "Bearer ${'$'}ANON_KEY")
            revConn.connectTimeout = 8_000; revConn.readTimeout = 8_000
            val revResp = revConn.inputStream.bufferedReader().readText()
            JSONArray(revResp).length() > 0
        } catch (e: Exception) { false }
    }

    data class LicenseResult(
        val valid: Boolean,
        val tier: String = "",
        val message: String = "",
        val licenseId: String = "",
        val revoked: Boolean = false
    )

    fun checkLicense(code: String, context: Context, checkRevocation: Boolean = false): LicenseResult {
        if (code.isBlank()) return LicenseResult(false, message = "No activation code.")
        val deviceCode = getDeviceCode(context)
        return try {
            val (payload, sigB64) = decodeTokenPublic(code)
            val isVerified = try { verify(payload, sigB64) }
                             catch (e: Exception) { return LicenseResult(false, message = "Crash: ${'$'}{e.message}") }
            if (!isVerified) return LicenseResult(false, message = "Signature invalid.")
            if (payload.optString("app").lowercase() != BUNDLE_APP.lowercase())
                return LicenseResult(false, message = "Wrong product.")
            val bound = payload.optString("device_code").trim().uppercase()
            if (bound.isNotEmpty() && bound != deviceCode.uppercase())
                return LicenseResult(false, message = "Device mismatch. Yours: ${'$'}deviceCode")
            val expiry = payload.optString("expires_at").ifBlank { payload.optString("expiry") }
            if (expiry.isNotBlank()) {
                try {
                    val exp = java.time.Instant.parse(
                        expiry.replace(" ", "T").let { if (!it.endsWith("Z")) "${'$'}{it}Z" else it }
                    )
                    if (java.time.Instant.now().isAfter(exp))
                        return LicenseResult(false, message = "License expired.")
                } catch (e: Exception) { }
            }
            val licId = payload.optString("license_id")
            if (checkRevocation && licId.isNotBlank()) {
                if (isRevokedOnServer(licId))
                    return LicenseResult(false, message = "License has been revoked.", revoked = true)
            }
            LicenseResult(true, payload.optString("tier", "pro").lowercase(), "License verified.", licId)
        } catch (e: Exception) {
            LicenseResult(false, message = "Decode error: ${'$'}{e.message}")
        }
    }

    fun checkOnStartup(context: Context): LicenseResult {
        val saved = loadLicense(context) ?: return LicenseResult(false, message = "No license.")
        if (!isProductActive(saved.optString("product_id", BUNDLE_APP))) {
            deleteLicense(context)
            return LicenseResult(false, message = "Product no longer available.")
        }
        val result = checkLicense(saved.optString("activation_code"), context, checkRevocation = true)
        if (result.revoked) deleteLicense(context)
        return result
    }

    fun saveLicense(context: Context, code: String, payload: JSONObject) {
        File(context.filesDir, LICENSE_FILE).writeText(JSONObject().apply {
            put("activation_code", code)
            put("license_id", payload.optString("license_id"))
            put("product_id", BUNDLE_APP)
            put("tier", payload.optString("tier", "pro"))
            put("payload", payload)
            put("saved_at", java.time.Instant.now().toString())
        }.toString())
    }

    fun loadLicense(context: Context): JSONObject? = try {
        val f = File(context.filesDir, LICENSE_FILE)
        if (f.exists()) JSONObject(f.readText()) else null
    } catch (e: Exception) { null }

    fun deleteLicense(context: Context) { File(context.filesDir, LICENSE_FILE).delete() }
}
        """.trimIndent()
    }

    // ── SHVAccount GENERATOR ──────────────────────────────────────────

    fun generateSHVAccount(packageName: String, product: KotlinProduct, demoHours: Int): String {
        return """
package $packageName.license

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

object SHVAccount {
    private const val SUPABASE_URL    = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    const val APP_CODE   = "${product.appCode}"
    const val PRODUCT_ID = "${product.id}"
    const val DEMO_HOURS = $demoHours
    private const val SESSION_FILE = "shv_cloud_session_${'$'}{APP_CODE}.json"

    data class Session(val accessToken: String, val refreshToken: String,
        val expiresAt: Long, val userId: String, val email: String, val plan: String)

    data class DemoState(val valid: Boolean, val signedIn: Boolean, val status: String,
        val startAllowed: Boolean, val message: String, val remainingText: String,
        val remainingSeconds: Long, val offline: Boolean)

    data class AccessStatus(val valid: Boolean, val mode: String, val message: String,
        val tier: String?, val licenseId: String?, val trialState: DemoState?)

    private fun get(url: String, token: String? = null): JSONArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${'$'}{token ?: PUBLISHABLE_KEY}")
        conn.connectTimeout = 14_000; conn.readTimeout = 14_000
        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp   = stream?.bufferedReader()?.readText() ?: "[]"
        if (code !in 200..299) throw RuntimeException(
            try { JSONObject(resp).optString("message", resp) } catch (e: Exception) { resp })
        return JSONArray(resp)
    }

    private fun sessionFile(ctx: Context) = File(ctx.filesDir, SESSION_FILE)

    fun loadSession(ctx: Context): Session? = try {
        val j = JSONObject(sessionFile(ctx).readText())
        val t = j.optString("access_token")
        if (t.isBlank()) null
        else Session(t, j.optString("refresh_token"), j.optLong("expires_at"),
            j.optJSONObject("user")?.optString("id") ?: "",
            j.optJSONObject("user")?.optString("email") ?: "",
            j.optJSONObject("user")?.optJSONObject("user_metadata")?.optString("plan") ?: "Standard")
    } catch (e: Exception) { null }

    fun clearSession(ctx: Context) { sessionFile(ctx).delete() }

    suspend fun signIn(ctx: Context, email: String, password: String): Session =
        withContext(Dispatchers.IO) {
            val conn = URL("${'$'}SUPABASE_URL/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
            conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
            conn.outputStream.use { it.write(JSONObject().put("email", email.trim()).put("password", password).toString().toByteArray()) }
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp   = stream?.bufferedReader()?.readText() ?: "{}"
            if (code !in 200..299) throw RuntimeException(
                try { JSONObject(resp).optString("message", resp) } catch (e: Exception) { resp })
            val result = JSONObject(resp)
            sessionFile(ctx).writeText(result.toString())
            Session(result.optString("access_token"), result.optString("refresh_token"),
                result.optLong("expires_at"),
                result.optJSONObject("user")?.optString("id") ?: "",
                result.optJSONObject("user")?.optString("email") ?: "",
                result.optJSONObject("user")?.optJSONObject("user_metadata")?.optString("plan") ?: "Standard")
        }

    suspend fun getDemoStatus(ctx: Context): DemoState = withContext(Dispatchers.IO) {
        loadSession(ctx) ?: return@withContext DemoState(false, false, "none", false,
            "Sign in to your SH Vertex account.", "0m", 0, false)
        try {
            val deviceCode = SHVLicense.getDeviceCode(ctx)
            val url = "${'$'}SUPABASE_URL/rest/v1/kl_demo_sessions" +
                "?product_id=eq.${'$'}{PRODUCT_ID}&device_code=eq.${'$'}{deviceCode}" +
                "&select=id,is_active,demo_started_at,demo_expires_at" +
                "&order=demo_started_at.desc&limit=1"
            val arr = get(url)
            if (arr.length() == 0) return@withContext DemoState(false, true, "none", true,
                "No trial started yet. Start a trial to evaluate the app.", "0m", 0, false)
            parseDemoSession(arr.getJSONObject(0), signedIn = true)
        } catch (e: Exception) {
            DemoState(false, true, "error", false, "Cannot verify demo: ${'$'}{e.message}", "0m", 0, true)
        }
    }

    suspend fun startDemo(ctx: Context): DemoState = withContext(Dispatchers.IO) {
        val session    = loadSession(ctx) ?: throw RuntimeException("Sign in first.")
        val deviceCode = SHVLicense.getDeviceCode(ctx)
        val checkUrl   = "${'$'}SUPABASE_URL/rest/v1/kl_demo_sessions" +
            "?product_id=eq.${'$'}{PRODUCT_ID}&device_code=eq.${'$'}{deviceCode}&select=id&limit=1"
        val existing = get(checkUrl)
        if (existing.length() > 0) return@withContext getDemoStatus(ctx)

        val now       = java.time.Instant.now()
        val expiresAt = now.plusSeconds(DEMO_HOURS.toLong() * 3600L)
        val payload   = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("product_id", PRODUCT_ID); put("device_code", deviceCode)
            put("demo_started_at", now.toString()); put("demo_expires_at", expiresAt.toString())
            put("is_active", true)
        }
        val conn = URL("${'$'}SUPABASE_URL/rest/v1/kl_demo_sessions").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${'$'}{session.accessToken}")
        conn.setRequestProperty("Prefer", "return=representation")
        conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp   = stream?.bufferedReader()?.readText() ?: "[]"
        if (code !in 200..299) throw RuntimeException("Failed to start demo: ${'$'}resp")
        val arr = JSONArray(resp)
        if (arr.length() > 0) parseDemoSession(arr.getJSONObject(0), signedIn = true)
        else {
            val remSec = DEMO_HOURS.toLong() * 3600L
            DemoState(true, true, "active", false,
                "Trial started. ${'$'}{formatRemaining(remSec)} remaining.",
                formatRemaining(remSec), remSec, false)
        }
    }

    private fun parseDemoSession(row: JSONObject, signedIn: Boolean): DemoState {
        val isActive  = row.optBoolean("is_active", false)
        val expiresAt = parseIso(row.optString("demo_expires_at"))
        val now       = java.time.Instant.now()
        val stillValid = isActive && expiresAt != null && expiresAt.isAfter(now)
        val remSec    = if (stillValid && expiresAt != null) maxOf(0L, expiresAt.epochSecond - now.epochSecond) else 0L
        val status    = if (stillValid) "active" else if (expiresAt != null) "expired" else "none"
        return DemoState(stillValid, signedIn, status, !isActive && expiresAt == null,
            if (stillValid) "Trial active: ${'$'}{formatRemaining(remSec)} remaining."
            else if (status == "expired") "Your trial has expired. Upgrade to Pro to continue."
            else "No trial found.",
            formatRemaining(remSec), remSec, false)
    }

    suspend fun getAccessStatus(ctx: Context): AccessStatus = withContext(Dispatchers.IO) {
        if (!SHVLicense.isProductActive(PRODUCT_ID)) {
            SHVLicense.deleteLicense(ctx); clearSession(ctx)
            return@withContext AccessStatus(false, "none", "This product is no longer available.", null, null, null)
        }
        val lic = SHVLicense.loadLicense(ctx)
        if (lic != null) {
            val r = SHVLicense.checkLicense(lic.optString("activation_code"), ctx, checkRevocation = true)
            if (r.valid) return@withContext AccessStatus(true, "licensed", r.message, r.tier, r.licenseId, null)
            SHVLicense.deleteLicense(ctx)
        }
        val demo = getDemoStatus(ctx)
        AccessStatus(demo.valid, if (demo.valid) "trial" else "none", demo.message, null, null, demo)
    }

    private fun parseIso(s: String?): java.time.Instant? {
        if (s.isNullOrBlank()) return null
        return try {
            java.time.Instant.parse(
                s.trim().replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${'$'}{it}Z" else it }
            )
        } catch (e: Exception) { null }
    }

    fun formatRemaining(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val d = seconds / 86400; val h = (seconds % 86400) / 3600; val m = (seconds % 3600) / 60
        return when { d > 0 -> "${'$'}d d ${'$'}h h"; h > 0 -> "${'$'}h h ${'$'}m m"; else -> "${'$'}m m" }
    }
}
        """.trimIndent()
    }

    // ── LicenseGateScreen GENERATOR ───────────────────────────────────
    // Production-level AMOLED black UI. All payment methods, bank details,
    // crypto addresses with copy buttons, disclaimer, grace period notice.
    // Everything on one scrollable screen, cleanly sectioned.

    fun generateLicenseGateScreen(
        packageName: String,
        product: KotlinProduct,
        config: GateConfig
    ): String {
        val appName       = config.appDisplayName.ifBlank { product.name }
        val proPrice      = config.proPrice
        val gracePeriod   = config.gracePeriodHours
        val disclaimer    = config.disclaimer
        val bankName      = config.bankName
        val bankAccName   = config.bankAccName
        val bankAccount   = config.bankAccount
        val bankBranch    = config.bankBranch
        val bankSwift     = config.bankSwift
        val usdtBsc       = config.cryptoUsdtBsc
        val usdtTrc       = config.cryptoUsdtTrc
        val usdtPlasma    = config.cryptoUsdtPlasma
        val eth           = config.cryptoEth
        val ltc           = config.cryptoLtc
        val hasBankDetails = listOf(bankName, bankAccount, bankAccName, bankBranch, bankSwift).any { it.isNotBlank() }
        val hasCrypto      = listOf(usdtBsc, usdtTrc, usdtPlasma, eth, ltc).any { it.isNotBlank() }

        return """
package $packageName.shvgate

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import $packageName.license.SHVAccount
import $packageName.license.SHVLicense

// ── AMOLED palette ────────────────────────────────────────────────────
private val Bg        = Color(0xFF000000)
private val Surface   = Color(0xFF0D0D0D)
private val Card      = Color(0xFF141414)
private val CardAlt   = Color(0xFF1A1A1A)
private val Border    = Color(0xFF262626)
private val Green     = Color(0xFF00E676)
private val Cyan      = Color(0xFF00BCD4)
private val Amber     = Color(0xFFFFB300)
private val Red       = Color(0xFFFF5252)
private val Purple    = Color(0xFFCE93D8)
private val Wa        = Color(0xFF25D366)
private val Blue      = Color(0xFF1565C0)
private val OnPrimary = Color(0xFF000000)
private val TextPri   = Color(0xFFFFFFFF)
private val TextSec   = Color(0xFF9E9E9E)
private val TextMute  = Color(0xFF616161)

// ── Injection-time baked constants ────────────────────────────────────
private const val APP_NAME      = "$appName"
private const val PRO_PRICE     = "$proPrice"
private const val GRACE_HOURS   = $gracePeriod
private const val DISCLAIMER    = "$disclaimer"
private const val BANK_NAME     = "$bankName"
private const val BANK_ACCNAME  = "$bankAccName"
private const val BANK_ACCOUNT  = "$bankAccount"
private const val BANK_BRANCH   = "$bankBranch"
private const val BANK_SWIFT    = "$bankSwift"
private const val USDT_BSC      = "$usdtBsc"
private const val USDT_TRC      = "$usdtTrc"
private const val USDT_PLASMA   = "$usdtPlasma"
private const val ETH_ADDR      = "$eth"
private const val LTC_ADDR      = "$ltc"
private const val CONTACT_EMAIL = "ceo.shvertex@gmail.com"
private const val CONTACT_WA    = "+94771363462"
private const val ACCOUNT_URL   = "https://shvertex.online/account.html"

@Composable
fun LicenseGateScreen(context: Context, onAccessGranted: () -> Unit) {
    val scope      = rememberCoroutineScope()
    val clipboard  = LocalClipboardManager.current
    val deviceCode = remember { SHVLicense.getDeviceCode(context) }

    var isLoading      by remember { mutableStateOf(true) }
    var canContinue    by remember { mutableStateOf(false) }
    var statusMsg      by remember { mutableStateOf("Verifying access\u2026") }
    var accessMode     by remember { mutableStateOf("none") }
    var licenseId      by remember { mutableStateOf("") }
    var licenseTier    by remember { mutableStateOf("") }
    var trialRemaining by remember { mutableStateOf("") }
    var accountEmail   by remember { mutableStateOf("") }
    var accountPlan    by remember { mutableStateOf("") }
    var isSignedIn     by remember { mutableStateOf(false) }

    var showActivate   by remember { mutableStateOf(false) }
    var showSignIn     by remember { mutableStateOf(false) }
    var showRequest    by remember { mutableStateOf(false) }
    var showPayBank    by remember { mutableStateOf(false) }
    var showPayCrypto  by remember { mutableStateOf(false) }
    var activationCode by remember { mutableStateOf("") }
    var emailField     by remember { mutableStateOf("") }
    var passwordField  by remember { mutableStateOf("") }
    var fieldError       by remember { mutableStateOf("") }
    var copiedKey        by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var rememberMe       by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun copyToClipboard(text: String, key: String) {
        clipboard.setText(AnnotatedString(text)); copiedKey = key
        scope.launch { delay(2000); copiedKey = "" }
    }

    fun refreshSession() {
        val s = SHVAccount.loadSession(context)
        isSignedIn = s != null; accountEmail = s?.email ?: ""; accountPlan = s?.plan ?: ""
    }

    fun checkAccess() {
        scope.launch {
            isLoading = true; fieldError = ""; refreshSession()
            val access = withContext(Dispatchers.IO) { SHVAccount.getAccessStatus(context) }
            isLoading = false; accessMode = access.mode; canContinue = access.valid
            when (access.mode) {
                "licensed" -> { licenseId = access.licenseId ?: ""; licenseTier = access.tier?.uppercase() ?: "PRO"; statusMsg = "License active." }
                "trial"    -> { trialRemaining = access.trialState?.remainingText ?: ""; statusMsg = access.trialState?.message ?: "Trial active." }
                else       -> statusMsg = access.message.ifBlank { "No active license or trial." }
            }
        }
    }

    fun requestViaEmail(mode: String = "license") {
        val subject = Uri.encode(
            if (mode == "payment") "Pro License Payment — ${'$'}APP_NAME"
            else "Pro License Request — ${'$'}APP_NAME")
        val body = Uri.encode(
            if (mode == "payment")
                "Hello,\n\nI have completed payment for a Pro License for ${'$'}APP_NAME.\n" +
                "Device Code: ${'$'}deviceCode\n\nPlease process my license key.\n\nThank you."
            else
                "Hello,\n\nI want a Pro License for ${'$'}APP_NAME.\n" +
                "Device Code: ${'$'}deviceCode\n\nPlease send payment instructions.\n\nThank you.")
        openUrl("mailto:${'$'}CONTACT_EMAIL?subject=${'$'}subject&body=${'$'}body")
    }

    fun requestViaWhatsApp(mode: String = "license") {
        val msg = Uri.encode(
            if (mode == "payment")
                "Hello! I have completed payment for a Pro License for *${'$'}APP_NAME*.\n" +
                "\uD83D\uDCF1 Device Code: *${'$'}deviceCode*\n\nPlease process my license key. Thank you!"
            else
                "Hello! I want a Pro License for *${'$'}APP_NAME*.\n" +
                "\uD83D\uDCF1 Device Code: *${'$'}deviceCode*\n\nPlease send payment instructions. Thank you!")
        val number = CONTACT_WA.replace("+", "").replace(" ", "")
        try { openUrl("whatsapp://send?phone=${'$'}number&text=${'$'}msg") }
        catch (e: Exception) { openUrl("https://wa.me/${'$'}number?text=${'$'}msg") }
    }

    // Load remembered email on first composition
    LaunchedEffect(Unit) {
        // Load remembered email from private prefs — stored in app's own filesDir, inaccessible to users
        val prefs = context.getSharedPreferences(
            "shv_gate_prefs_${'$'}{SHVAccount.APP_CODE}", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("remembered_email", "")
        if (!saved.isNullOrBlank()) { emailField = saved; rememberMe = true }
        checkAccess()
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── App Header ────────────────────────────────────────────
            Box(modifier = Modifier.size(68.dp).clip(CircleShape)
                .background(Card).border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(if (canContinue) Icons.Default.LockOpen else Icons.Default.Lock,
                    null, tint = if (canContinue) Green else Cyan, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(APP_NAME, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = TextPri, textAlign = TextAlign.Center)
            Text("by SH Vertex", fontSize = 12.sp, color = Cyan,
                letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))

            // ── Device Code Card ──────────────────────────────────────
            GateCard {
                Label("DEVICE CODE")
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(deviceCode, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = TextPri, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                    CopyButton(
                        copied = copiedKey == "device",
                        onClick = { copyToClipboard(deviceCode, "device") }
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Border, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp),
                            color = Cyan, strokeWidth = 2.dp)
                        Text("Checking access\u2026", fontSize = 12.sp, color = TextSec)
                    }
                } else {
                    val dot = when (accessMode) { "licensed" -> Green; "trial" -> Amber; else -> Red }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dot))
                        Text(statusMsg, fontSize = 12.sp, color = dot)
                    }
                }
            }

            // ── Pro Price Card ────────────────────────────────────────
            GateCard(borderColor = Green.copy(alpha = 0.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Amber.copy(alpha = 0.1f))
                        .border(1.dp, Amber.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, null, tint = Amber, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Label("PRO LICENSE")
                        Spacer(Modifier.height(2.dp))
                        Text(PRO_PRICE, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                        Text("Lifetime \u00b7 Device-bound \u00b7 No subscription",
                            fontSize = 11.sp, color = TextSec)
                    }
                }
            }

            // ── Status cards ──────────────────────────────────────────
            AnimatedVisibility(visible = isSignedIn, enter = fadeIn(), exit = fadeOut()) {
                GateCard(borderColor = Cyan.copy(alpha = 0.25f)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Surface).border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Cyan, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Label("SH VERTEX ACCOUNT")
                            Spacer(Modifier.height(2.dp))
                            Text(accountEmail, fontSize = 13.sp, color = TextPri, fontWeight = FontWeight.Medium)
                            if (accountPlan.isNotBlank())
                                Text("Plan: ${'$'}accountPlan", fontSize = 11.sp, color = TextSec)
                        }
                        StatusBadge("ACTIVE", Green)
                    }
                }
            }

            AnimatedVisibility(visible = accessMode == "licensed" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Green.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(22.dp))
                        Column {
                            Label("PRO LICENSE ACTIVE")
                            Spacer(Modifier.height(2.dp))
                            Text("Tier: ${'$'}licenseTier  \u00b7  Lifetime",
                                fontSize = 14.sp, color = Green, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (licenseId.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ID: ${'$'}licenseId", fontSize = 10.sp, color = TextMute,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            CopyButton(copied = copiedKey == "licid",
                                onClick = { copyToClipboard(licenseId, "licid") }, small = true)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = accessMode == "trial" && !isLoading,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Amber.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Timer, null, tint = Amber, modifier = Modifier.size(22.dp))
                        Column {
                            Label("TRIAL SESSION")
                            Spacer(Modifier.height(2.dp))
                            Text(if (trialRemaining.isNotBlank()) "${'$'}trialRemaining remaining"
                                 else "Trial active",
                                fontSize = 14.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Upgrade to Pro for permanent access.", fontSize = 11.sp, color = TextSec)
                }
            }

            // ── Account action row ────────────────────────────────────
            AnimatedVisibility(visible = isSignedIn && !isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlatOutlineButton("Sign Out", Red, modifier = Modifier.weight(1f)) {
                        SHVAccount.clearSession(context)
                        isSignedIn = false; accountEmail = ""; accountPlan = ""
                        accessMode = "none"; canContinue = false; statusMsg = "Signed out."
                    }
                    FlatOutlineButton("Refresh", Purple, modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh) { checkAccess() }
                }
            }

            // ── Continue button ───────────────────────────────────────
            AnimatedVisibility(visible = canContinue && !isLoading) {
                Button(onClick = { onAccessGranted() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue to App", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnPrimary)
                }
            }

            // ── Section divider ───────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Border, thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            // ── Activate License ──────────────────────────────────────
            GateSectionButton("Activate License", showActivate, Green,
                icon = Icons.Default.VpnKey) {
                showActivate = !showActivate
                showSignIn = false; showRequest = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showActivate,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Green.copy(alpha = 0.25f)) {
                    Label("PASTE ACTIVATION CODE")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = activationCode,
                        onValueChange = { activationCode = it; fieldError = "" },
                        placeholder = { Text("Paste your activation code here", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        colors = gateTextFieldColors(Green))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatOutlineButton("Paste from Clipboard", Cyan, modifier = Modifier.weight(1f)) {
                            clipboard.getText()?.text?.let { if (it.isNotBlank()) activationCode = it }
                        }
                        FlatOutlineButton("Clear", Red, modifier = Modifier.weight(1f)) { activationCode = "" }
                    }
                    ErrorRow(fieldError)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        val code = activationCode.trim()
                        if (code.isBlank()) { fieldError = "Paste your activation code first."; return@Button }
                        isLoading = true; statusMsg = "Verifying\u2026"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { SHVLicense.checkLicense(code, context) }
                            isLoading = false
                            if (result.valid) {
                                withContext(Dispatchers.IO) {
                                    val (payload, _) = SHVLicense.decodeTokenPublic(code)
                                    SHVLicense.saveLicense(context, code, payload)
                                }
                                showActivate = false; checkAccess()
                            } else { fieldError = result.message; statusMsg = result.message }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(10.dp)) {
                        Text("Activate Pro License", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OnPrimary)
                    }
                }
            }

            // ── Request Pro License ───────────────────────────────────
            GateSectionButton("Request Pro License", showRequest, Amber,
                icon = Icons.Default.Send) {
                showRequest = !showRequest
                showActivate = false; showSignIn = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showRequest,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Amber.copy(alpha = 0.3f)) {
                    Label("REQUEST PRO LICENSE")
                    Spacer(Modifier.height(4.dp))
                    Text("Your device code is automatically included in the message.",
                        fontSize = 12.sp, color = TextSec)
                    Spacer(Modifier.height(10.dp))
                    // Device code preview
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(Surface, RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("YOUR DEVICE CODE", fontSize = 9.sp, color = TextMute,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(deviceCode, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = Cyan, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        }
                        CopyButton(copied = copiedKey == "reqdev", onClick = { copyToClipboard(deviceCode, "reqdev") })
                    }
                    Spacer(Modifier.height(12.dp))
                    Label("CHOOSE CONTACT METHOD")
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Request via Email", Blue, icon = Icons.Default.Email,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("license") }
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Request via WhatsApp", Wa, icon = Icons.Default.Chat,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("license") }
                }
            }

            // ── Sign In / Start Trial ─────────────────────────────────
            GateSectionButton("Sign In / Start Trial", showSignIn, Cyan,
                icon = Icons.Default.Person) {
                showSignIn = !showSignIn
                showActivate = false; showRequest = false; showPayBank = false; showPayCrypto = false; fieldError = ""
            }
            AnimatedVisibility(visible = showSignIn,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GateCard(borderColor = Cyan.copy(alpha = 0.25f)) {
                    Label("SH VERTEX ACCOUNT")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = emailField,
                        onValueChange = { emailField = it; fieldError = "" },
                        label = { Text("Email", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp), colors = gateTextFieldColors(Cyan))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = passwordField,
                        onValueChange = { passwordField = it; fieldError = "" },
                        label = { Text("Password", color = TextMute, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        colors = gateTextFieldColors(Cyan),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMute, modifier = Modifier.size(18.dp)
                                )
                            }
                        })
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Cyan, uncheckedColor = TextMute,
                                checkmarkColor = Color.Black))
                        Text("Remember email", fontSize = 12.sp, color = TextSec)
                    }
                    ErrorRow(fieldError)
                    Spacer(Modifier.height(8.dp))
                    FlatButton("Sign In", Cyan, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        if (emailField.isBlank() || passwordField.isBlank()) {
                            fieldError = "Email and password are required."; return@FlatButton }
                        isLoading = true; statusMsg = "Signing in\u2026"
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    SHVAccount.signIn(context, emailField, passwordField)
                                    val prefs = context.getSharedPreferences(
                                        "shv_gate_prefs_${'$'}{SHVAccount.APP_CODE}",
                                        android.content.Context.MODE_PRIVATE)
                                    if (rememberMe) prefs.edit().putString("remembered_email", emailField.trim()).apply()
                                    else prefs.edit().remove("remembered_email").apply()
                                }
                                showSignIn = false; checkAccess()
                            } catch (e: Exception) {
                                isLoading = false; fieldError = e.message ?: "Sign-in failed."; statusMsg = fieldError
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("OR START A FREE TRIAL", fontSize = 10.sp, color = TextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    FlatOutlineButton(
                        if (isSignedIn) "Start Trial Session" else "Sign in above to start trial",
                        if (isSignedIn) Amber else TextMute,
                        icon = Icons.Default.Timer,
                        modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        if (!isSignedIn) { fieldError = "Sign in first to start a trial."; return@FlatOutlineButton }
                        isLoading = true; statusMsg = "Starting trial\u2026"
                        scope.launch {
                            try {
                                val demo = withContext(Dispatchers.IO) { SHVAccount.startDemo(context) }
                                isLoading = false
                                if (demo.valid) { showSignIn = false; checkAccess() }
                                else { fieldError = demo.message; statusMsg = demo.message }
                            } catch (e: Exception) {
                                isLoading = false; fieldError = e.message ?: "Could not start trial."; statusMsg = fieldError
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("DON'T HAVE AN ACCOUNT?", fontSize = 10.sp, color = TextMute,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    FlatOutlineButton("Create SH Vertex Account", Purple,
                        icon = Icons.Default.PersonAdd, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        openUrl(ACCOUNT_URL)
                    }
                }
            }

            // ── Pay via Bank Transfer ─────────────────────────────────
            if (${hasBankDetails}) {
                GateSectionButton("Pay via Bank Transfer", showPayBank, Green.copy(alpha = 0.8f),
                    icon = Icons.Default.AccountBalance) {
                    showPayBank = !showPayBank
                    showActivate = false; showSignIn = false; showRequest = false; showPayCrypto = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayBank,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    GateCard(borderColor = Green.copy(alpha = 0.2f)) {
                        Label("BANK TRANSFER DETAILS")
                        Spacer(Modifier.height(4.dp))
                        Text("Transfer the Pro License amount to the account below, then notify us.",
                            fontSize = 12.sp, color = TextSec)
                        Spacer(Modifier.height(12.dp))
                        if (BANK_NAME.isNotBlank())     BankDetailRow("Bank",       BANK_NAME,    "bankname",  copiedKey, clipboard)
                        if (BANK_ACCNAME.isNotBlank())  BankDetailRow("Account Name", BANK_ACCNAME, "bankaccname", copiedKey, clipboard)
                        if (BANK_ACCOUNT.isNotBlank())  BankDetailRow("Account No.", BANK_ACCOUNT, "bankaccount", copiedKey, clipboard)
                        if (BANK_BRANCH.isNotBlank())   BankDetailRow("Branch",     BANK_BRANCH,  "bankbranch", copiedKey, clipboard)
                        if (BANK_SWIFT.isNotBlank())    BankDetailRow("SWIFT / BIC", BANK_SWIFT,  "bankswift",  copiedKey, clipboard)
                        Spacer(Modifier.height(4.dp))
                        DisclaimerBox()
                        Spacer(Modifier.height(12.dp))
                        Label("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via Email", Blue,
                            icon = Icons.Default.Email, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("payment") }
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via WhatsApp", Wa,
                            icon = Icons.Default.Chat, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("payment") }
                    }
                }
            }

            // ── Pay via Crypto ────────────────────────────────────────
            if (${hasCrypto}) {
                GateSectionButton("Pay via Crypto", showPayCrypto, Amber.copy(alpha = 0.8f),
                    icon = Icons.Default.CurrencyBitcoin) {
                    showPayCrypto = !showPayCrypto
                    showActivate = false; showSignIn = false; showRequest = false; showPayBank = false; fieldError = ""
                }
                AnimatedVisibility(visible = showPayCrypto,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    GateCard(borderColor = Amber.copy(alpha = 0.2f)) {
                        Label("CRYPTO WALLET ADDRESSES")
                        Spacer(Modifier.height(4.dp))
                        Text("Send the Pro License amount to any address below, then notify us with your transaction hash.",
                            fontSize = 12.sp, color = TextSec)
                        Spacer(Modifier.height(12.dp))
                        if (USDT_BSC.isNotBlank())    CryptoAddressRow("\uD83D\uDFE1 USDT — BSC (BEP20)", USDT_BSC,    "usdt_bsc",    copiedKey) { copyToClipboard(USDT_BSC, "usdt_bsc") }
                        if (USDT_TRC.isNotBlank())    CryptoAddressRow("\uD83D\uDFE1 USDT — TRC20",       USDT_TRC,    "usdt_trc",    copiedKey) { copyToClipboard(USDT_TRC, "usdt_trc") }
                        if (USDT_PLASMA.isNotBlank()) CryptoAddressRow("\uD83D\uDFE1 USDT — Plasma",      USDT_PLASMA, "usdt_plasma", copiedKey) { copyToClipboard(USDT_PLASMA, "usdt_plasma") }
                        if (ETH_ADDR.isNotBlank())    CryptoAddressRow("\uD83D\uDD35 ETH",                ETH_ADDR,    "eth",         copiedKey) { copyToClipboard(ETH_ADDR, "eth") }
                        if (LTC_ADDR.isNotBlank())    CryptoAddressRow("\u26AB LTC",                      LTC_ADDR,    "ltc",         copiedKey) { copyToClipboard(LTC_ADDR, "ltc") }
                        Spacer(Modifier.height(4.dp))
                        DisclaimerBox()
                        Spacer(Modifier.height(12.dp))
                        Label("NOTIFY US AFTER PAYMENT")
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via Email", Blue,
                            icon = Icons.Default.Email, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaEmail("payment") }
                        Spacer(Modifier.height(8.dp))
                        FlatButton("I've Paid — Notify via WhatsApp", Wa,
                            icon = Icons.Default.Chat, modifier = Modifier.fillMaxWidth().height(50.dp)) { requestViaWhatsApp("payment") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────

@Composable
private fun GateCard(
    borderColor: Color = Color(0xFF262626),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun GateSectionButton(
    label: String,
    active: Boolean,
    accent: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF141414) else Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (active) accent else Color(0xFF262626)))
    ) {
        if (icon != null) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = if (active) accent else Color(0xFF9E9E9E))
    }
}

@Composable
private fun FlatButton(
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp)) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
private fun FlatOutlineButton(
    label: String,
    accent: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(accent.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(10.dp)) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CopyButton(copied: Boolean, onClick: () -> Unit, small: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (copied) Color(0xFF00E676) else Color(0xFF00BCD4)),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                (if (copied) Color(0xFF00E676) else Color(0xFF00BCD4)).copy(alpha = 0.5f))),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = if (small) 10.dp else 14.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (copied) "COPIED!" else "COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(modifier = Modifier
        .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 10.sp, color = Color(0xFF616161),
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun ErrorRow(error: String) {
    if (error.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
            Text(error, fontSize = 12.sp, color = Color(0xFFFF5252))
        }
    }
}

@Composable
private fun BankDetailRow(
    label: String, value: String, key: String, copiedKey: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth()
        .padding(vertical = 4.dp)
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), fontSize = 9.sp, color = Color(0xFF616161),
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = Color(0xFFFFFFFF), fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(value)) },
            modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, null,
                tint = if (copiedKey == key) Color(0xFF00E676) else Color(0xFF00BCD4),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CryptoAddressRow(
    label: String, address: String, key: String, copiedKey: String, onCopy: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()
        .padding(vertical = 4.dp)
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
            CopyButton(copied = copiedKey == key, onClick = onCopy, small = true)
        }
        Spacer(Modifier.height(4.dp))
        Text(address, fontSize = 11.sp, color = Color(0xFF9E9E9E),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DisclaimerBox() {
    if (DISCLAIMER.isBlank()) return
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.fillMaxWidth()
        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = Color(0xFFFFB300),
                modifier = Modifier.size(14.dp).padding(top = 1.dp))
            Text(DISCLAIMER, fontSize = 11.sp, color = Color(0xFF9E9E9E), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun gateTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF262626),
    focusedTextColor = Color(0xFFFFFFFF), unfocusedTextColor = Color(0xFFFFFFFF),
    cursorColor = accent, focusedContainerColor = Color(0xFF0D0D0D),
    unfocusedContainerColor = Color(0xFF0D0D0D), focusedLabelColor = accent,
    unfocusedLabelColor = Color(0xFF616161))
        """.trimIndent()
    }
}
