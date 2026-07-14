package com.shvertex.universalconv.license

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

    const val APP_CODE   = "uni_calculator"
    const val PRODUCT_ID = "51bd9327-559b-42a1-a841-24f1ee50eb3e"
    const val DEMO_HOURS = 24
    private const val SESSION_FILE = "shv_cloud_session_${APP_CODE}.json"

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
        conn.setRequestProperty("Authorization", "Bearer ${token ?: PUBLISHABLE_KEY}")
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
            val conn = URL("$SUPABASE_URL/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
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
            val url = "$SUPABASE_URL/rest/v1/kl_demo_sessions" +
                "?product_id=eq.${PRODUCT_ID}&device_code=eq.${deviceCode}" +
                "&select=id,is_active,demo_started_at,demo_expires_at" +
                "&order=demo_started_at.desc&limit=1"
            val arr = get(url)
            if (arr.length() == 0) return@withContext DemoState(false, true, "none", true,
                "No trial started yet. Start a trial to evaluate the app.", "0m", 0, false)
            parseDemoSession(arr.getJSONObject(0), signedIn = true)
        } catch (e: Exception) {
            DemoState(false, true, "error", false, "Cannot verify demo: ${e.message}", "0m", 0, true)
        }
    }

    suspend fun startDemo(ctx: Context): DemoState = withContext(Dispatchers.IO) {
        val session    = loadSession(ctx) ?: throw RuntimeException("Sign in first.")
        val deviceCode = SHVLicense.getDeviceCode(ctx)
        val checkUrl   = "$SUPABASE_URL/rest/v1/kl_demo_sessions" +
            "?product_id=eq.${PRODUCT_ID}&device_code=eq.${deviceCode}&select=id&limit=1"
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
        val conn = URL("$SUPABASE_URL/rest/v1/kl_demo_sessions").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        conn.setRequestProperty("Prefer", "return=representation")
        conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp   = stream?.bufferedReader()?.readText() ?: "[]"
        if (code !in 200..299) throw RuntimeException("Failed to start demo: $resp")
        val arr = JSONArray(resp)
        if (arr.length() > 0) parseDemoSession(arr.getJSONObject(0), signedIn = true)
        else {
            val remSec = DEMO_HOURS.toLong() * 3600L
            DemoState(true, true, "active", false,
                "Trial started. ${formatRemaining(remSec)} remaining.",
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
            if (stillValid) "Trial active: ${formatRemaining(remSec)} remaining."
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
                s.trim().replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it }
            )
        } catch (e: Exception) { null }
    }

    fun formatRemaining(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val d = seconds / 86400; val h = (seconds % 86400) / 3600; val m = (seconds % 3600) / 60
        return when { d > 0 -> "$d d $h h"; h > 0 -> "$h h $m m"; else -> "$m m" }
    }
}