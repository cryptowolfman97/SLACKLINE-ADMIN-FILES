package com.shvertex.supaadmin.license

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * SupaAccount
 * ───────────
 * SHVertex account (Supabase Auth) session handling for Supa Studio.
 *
 * Unlike Slackline's SHVAccount, there is NO demo/trial mode here — every
 * tier (including Free) requires a signed-in SHVertex account. This is
 * intentional per spec: license tier is verified locally via SupaLicense
 * (RSA + device-bound), but *access to the app at all* is gated behind a
 * live account, which is what stops a modified APK from just skipping the
 * check — there's a real account boundary in front of it, not just a
 * local flag.
 *
 * Session is stored in the app's private files dir (not external storage)
 * so another app cannot read it off-device. "Remember me" persists only
 * the email (never the password) to prefill the sign-in form next time.
 */
object SupaAccount {

    private const val SUPABASE_URL    = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    const val APP_CODE   = LicenseGateConfig.APP_CODE
    const val PRODUCT_ID = LicenseGateConfig.PRODUCT_ID

    private const val SESSION_FILE  = "shv_cloud_session_${APP_CODE}.json"
    private const val PREFS_NAME    = "shv_account_prefs_$APP_CODE"
    private const val PREF_EMAIL    = "remembered_email"

    // The ViewModel has no Context of its own (it's a plain ViewModel, not
    // an AndroidViewModel), so we hold the application context here —
    // initialised once from MainActivity, same pattern as SupabaseStorage.init().
    private var appCtx: Context? = null
    fun init(context: Context) { appCtx = context.applicationContext }
    fun appContext(): Context = appCtx
        ?: throw IllegalStateException("SupaAccount.init() not called — call it from MainActivity.onCreate()")

    data class Session(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val userId: String,
        val email: String
    )

    /** Combined view of "can this user use the app, and at what tier". */
    data class AccessStatus(
        val loggedIn: Boolean,
        val tier: SupaLicense.Tier,
        val message: String,
        val email: String?
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

    // ── Remember-me (email only — never the password) ──────────────────

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun rememberedEmail(ctx: Context): String? = prefs(ctx).getString(PREF_EMAIL, null)

    fun setRememberedEmail(ctx: Context, email: String?) {
        prefs(ctx).edit().apply {
            if (email.isNullOrBlank()) remove(PREF_EMAIL) else putString(PREF_EMAIL, email.trim())
        }.apply()
    }

    // ── Sign in ──────────────────────────────────────────────────────────

    suspend fun signIn(ctx: Context, email: String, password: String, rememberMe: Boolean): Session =
        withContext(Dispatchers.IO) {
            val conn = URL("$SUPABASE_URL/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
            conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000
            conn.outputStream.use {
                it.write(JSONObject().put("email", email.trim()).put("password", password).toString().toByteArray())
            }
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp   = stream?.bufferedReader()?.readText() ?: "{}"
            if (code !in 200..299) throw RuntimeException(
                try { JSONObject(resp).optString("error_description", resp) } catch (e: Exception) { resp })
            val result = JSONObject(resp)
            sessionFile(ctx).writeText(result.toString())
            setRememberedEmail(ctx, if (rememberMe) email.trim() else null)
            Session(
                result.optString("access_token"), result.optString("refresh_token"),
                result.optLong("expires_at"),
                result.optJSONObject("user")?.optString("id") ?: "",
                result.optJSONObject("user")?.optString("email") ?: email.trim()
            )
        }

    fun signOut(ctx: Context) {
        clearSession(ctx)
        SupaLicense.deleteLicense(ctx)
    }

    // ── Combined access status (account + license), used on every Home tap ──

    suspend fun getAccessStatus(ctx: Context): AccessStatus = withContext(Dispatchers.IO) {
        val session = loadSession(ctx)
            ?: return@withContext AccessStatus(false, SupaLicense.Tier.FREE, "Sign in to your SH Vertex account to continue.", null)

        val licResult = SupaLicense.checkFull(ctx)
        val tier = if (licResult.valid) licResult.tier else SupaLicense.Tier.FREE
        val message = if (licResult.valid) "License verified." else "Signed in — Free tier."
        AccessStatus(true, tier, message, session.email)
    }
}
