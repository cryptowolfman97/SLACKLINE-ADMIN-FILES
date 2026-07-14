package com.example.omnicortex.license

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * SH Vertex account session management (sign in / create account / sign out)
 * plus combined access-status resolution (login + tier + revocation).
 *
 * No demo/trial state — Omni-Cortex uses a straight Free / Pro / Pro+ tier
 * model gated entirely on SHVertex account login + license tier.
 */
object SHVAccount {

    private const val SESSION_FILE = "shv_cloud_session_${LicenseGateConfig.APP_CODE}.json"

    data class Session(
        val accessToken: String, val refreshToken: String,
        val expiresAt: Long, val userId: String, val email: String
    )

    /** "free" | "pro" | "pro_plus" */
    data class AccessStatus(
        val loggedIn: Boolean,
        val tier: String,
        val revoked: Boolean,
        val message: String,
        val email: String = "",
        val licenseId: String = ""
    )

    private fun sessionFile(ctx: Context) = File(ctx.filesDir, SESSION_FILE)

    fun loadSession(ctx: Context): Session? = try {
        val j = JSONObject(sessionFile(ctx).readText())
        val t = j.optString("access_token")
        if (t.isBlank()) null
        else Session(
            t, j.optString("refresh_token"), j.optLong("expires_at"),
            j.optJSONObject("user")?.optString("id") ?: "",
            j.optJSONObject("user")?.optString("email") ?: ""
        )
    } catch (e: Exception) { null }

    fun clearSession(ctx: Context) { sessionFile(ctx).delete() }

    private fun request(method: String, url: String, token: String?, body: JSONObject?): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("apikey", LicenseGateConfig.PUBLISHABLE_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${token ?: LicenseGateConfig.PUBLISHABLE_KEY}")
        conn.connectTimeout = 14_000; conn.readTimeout = 14_000
        if (body != null) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        }
        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp   = stream?.bufferedReader()?.readText() ?: "{}"
        if (code !in 200..299) throw RuntimeException(
            try { JSONObject(resp).optString("message", resp) } catch (e: Exception) { resp })
        return try { JSONObject(resp) } catch (e: Exception) { JSONObject().put("_array", JSONArray(resp)) }
    }

    suspend fun signIn(ctx: Context, email: String, password: String): Session =
        withContext(Dispatchers.IO) {
            val result = request(
                "POST",
                "${LicenseGateConfig.SUPABASE_URL}/auth/v1/token?grant_type=password",
                null,
                JSONObject().put("email", email.trim()).put("password", password)
            )
            sessionFile(ctx).writeText(result.toString())
            Session(
                result.optString("access_token"), result.optString("refresh_token"),
                result.optLong("expires_at"),
                result.optJSONObject("user")?.optString("id") ?: "",
                result.optJSONObject("user")?.optString("email") ?: ""
            )
        }

    suspend fun signUp(ctx: Context, email: String, password: String): Session =
        withContext(Dispatchers.IO) {
            val result = request(
                "POST",
                "${LicenseGateConfig.SUPABASE_URL}/auth/v1/signup",
                null,
                JSONObject().put("email", email.trim()).put("password", password)
            )
            val hasToken = result.optString("access_token").isNotBlank()
            if (hasToken) {
                sessionFile(ctx).writeText(result.toString())
                Session(
                    result.optString("access_token"), result.optString("refresh_token"),
                    result.optLong("expires_at"),
                    result.optJSONObject("user")?.optString("id") ?: "",
                    result.optJSONObject("user")?.optString("email") ?: ""
                )
            } else {
                // Email confirmation required by Supabase project settings.
                throw RuntimeException("Account created. Check your email to confirm, then sign in.")
            }
        }

    fun signOut(ctx: Context) { clearSession(ctx) }

    /**
     * Resolves login + tier + revocation in one pass. Called on startup,
     * every LicenseGateConfig.PERIODIC_CHECK_MINUTES, and whenever the
     * License Details button is pressed.
     */
    suspend fun getAccessStatus(ctx: Context): AccessStatus = withContext(Dispatchers.IO) {
        val session = loadSession(ctx)
            ?: return@withContext AccessStatus(false, "free", false, "Not signed in to SH Vertex account.")

        if (!SHVLicense.isProductActive()) {
            return@withContext AccessStatus(true, "free", false, "Product temporarily unavailable.", session.email)
        }

        // Locally-saved activation code (from Activate License) takes priority if present.
        val localLic = SHVLicense.loadLicense(ctx)
        if (localLic != null) {
            val r = SHVLicense.checkLicense(localLic.optString("activation_code"), ctx, checkRevocation = true)
            if (r.valid) {
                return@withContext AccessStatus(true, normalizeTier(r.tier), false, r.message, session.email, r.licenseId)
            }
            if (r.revoked) SHVLicense.deleteLicense(ctx)
        }

        // Otherwise, resolve tier from the account's license row on Supabase.
        try {
            val deviceCode = SHVLicense.getDeviceCode(ctx)
            val url = "${LicenseGateConfig.SUPABASE_URL}/rest/v1/kl_licenses" +
                "?product_id=eq.${LicenseGateConfig.PRODUCT_ID}&user_email=eq.${session.email}" +
                "&select=license_id,tier,status,device_code&order=created_at.desc&limit=1"
            val res = request("GET", url, session.accessToken, null)
            val arr = res.optJSONArray("_array") ?: JSONArray()
            if (arr.length() == 0) {
                return@withContext AccessStatus(true, "free", false, "No active license. Free tier only.", session.email)
            }
            val row = arr.getJSONObject(0)
            val status = row.optString("status", "active")
            if (status == "revoked") {
                return@withContext AccessStatus(true, "free", true, "License revoked.", session.email, row.optString("license_id"))
            }
            AccessStatus(true, normalizeTier(row.optString("tier", "free")), false, "License active.", session.email, row.optString("license_id"))
        } catch (e: Exception) {
            AccessStatus(true, "free", false, "Cannot verify license: ${e.message}", session.email)
        }
    }

    private fun normalizeTier(raw: String): String = when (raw.lowercase().trim()) {
        "pro+", "pro_plus", "proplus" -> "pro_plus"
        "pro" -> "pro"
        else -> "free"
    }
}
