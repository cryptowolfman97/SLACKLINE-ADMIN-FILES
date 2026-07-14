package com.shvertex.supaadmin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// ── Config model ──────────────────────────────────────────────────────────────

data class SupabaseConfig(
    val project_url: String           = "",
    val project_ref: String           = "",
    val anon_key: String              = "",
    val personal_access_token: String = "",
    val project_admin_key: String     = "",
    val email: String                 = "",
    val password: String              = ""
) {
    val normalizedUrl: String get() = normalizeUrl(project_url)
    val currentRef: String get() = project_ref.ifBlank { guessRef(normalizedUrl) }

    val hasUrl:   Boolean get() = normalizedUrl.isNotBlank()
    val hasPat:   Boolean get() = personal_access_token.isNotBlank()
    val hasAdmin: Boolean get() = project_admin_key.isNotBlank()
    val hasAnon:  Boolean get() = anon_key.isNotBlank()

    fun managementHeaders() = mapOf(
        "Authorization" to "Bearer $personal_access_token",
        "Content-Type"  to "application/json"
    )

    fun projectHeaders(useAnon: Boolean = false): Map<String, String> {
        val key = if (useAnon && project_admin_key.isBlank()) anon_key else project_admin_key
        return mapOf(
            "apikey"        to key,
            "Authorization" to "Bearer $key",
            "Content-Type"  to "application/json"
        )
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            val s = raw.trim()
            if (s.isBlank()) return ""
            val prefixed = if (!s.startsWith("http://") && !s.startsWith("https://")) "https://$s" else s
            return prefixed.trimEnd('/')
        }

        fun guessRef(url: String): String {
            val norm = normalizeUrl(url)
            if (norm.isBlank()) return ""
            val host = norm.removePrefix("https://").removePrefix("http://").substringBefore("/").trim()
            return host.substringBefore(".").trim()
        }
    }
}

data class SupabaseSettings(
    val timeout_seconds: Int    = 40,
    val table_preview_rows: Int = 5,
    val app_pin: String         = "",
    val dark_mode: Boolean      = true,
    val compact_mode: Boolean   = false,
    val use_biometrics: Boolean = false
)

data class WebViewCredentials(
    val email: String       = "",
    val password: String    = "",
    val rememberMe: Boolean = false
)

data class SqlSnippet(
    val id: String      = "",
    val name: String    = "",
    val sql: String     = "",
    val savedAt: String = ""
)

data class SavedConnection(
    val id: String                    = "",
    val nickname: String              = "",
    val project_url: String           = "",
    val project_ref: String           = "",
    val personal_access_token: String = "",
    val project_admin_key: String     = "",
    val anon_key: String              = "",
    val email: String                 = "",
    val password: String              = "",
    val savedAt: String               = ""
)

// ── Keystore key names ────────────────────────────────────────────────────────

private const val KEY_PAT       = "cred_pat"
private const val KEY_ADMIN_KEY = "cred_admin_key"
private const val KEY_ANON_KEY  = "cred_anon_key"
private const val KEY_EMAIL     = "cred_email"
private const val KEY_PASSWORD  = "cred_password"
private const val KEY_WV_PASS   = "cred_wv_password"

// Connection sensitive fields — prefixed by connection ID
private fun connPat(id: String)      = "conn_${id}_pat"
private fun connAdminKey(id: String) = "conn_${id}_admin_key"
private fun connAnonKey(id: String)  = "conn_${id}_anon_key"
private fun connEmail(id: String)    = "conn_${id}_email"
private fun connPassword(id: String) = "conn_${id}_password"

// ── Storage object ────────────────────────────────────────────────────────────

object SupabaseStorage {
    private val gson = Gson()

    // Context injected once on app start via init()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        migrateFromPlaintextIfNeeded()
    }

    // ── Downloads directory (exports, settings, snippets — non-sensitive) ─────

    private fun dir(): File = File(
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
        "SupaAdmin by SHV"
    ).also { it.mkdirs() }

    fun dataDir(): File = dir()

    private fun configFile()      = File(dir(), "supabase_config.json")
    private fun settingsFile()    = File(dir(), "supabase_settings.json")
    private fun connectionsFile() = File(dir(), "saved_connections.json")
    private fun webviewCredFile() = File(dir(), "webview_credentials.json")
    private fun sqlSnippetsFile() = File(dir(), "sql_snippets.json")

    // ── Android Keystore via EncryptedSharedPreferences ───────────────────────

    private fun encryptedPrefs(): SharedPreferences {
        val ctx = appContext ?: throw IllegalStateException("SupabaseStorage.init() not called")
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            "supaadmin_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun secureGet(key: String): String =
        try { encryptedPrefs().getString(key, "") ?: "" } catch (_: Exception) { "" }

    private fun securePut(vararg pairs: Pair<String, String>) {
        try {
            encryptedPrefs().edit().apply {
                pairs.forEach { (k, v) -> putString(k, v) }
                apply()
            }
        } catch (_: Exception) { }
    }

    private fun secureRemove(vararg keys: String) {
        try {
            encryptedPrefs().edit().apply {
                keys.forEach { remove(it) }
                apply()
            }
        } catch (_: Exception) { }
    }

    // ── One-time migration from plaintext config.json ─────────────────────────
    // On first launch after upgrade: reads old plaintext keys, saves them to
    // Keystore, then scrubs them from the JSON file on disk.

    private fun migrateFromPlaintextIfNeeded() {
        val file = configFile()
        if (!file.exists()) return
        try {
            val text = file.readText()
            val map = gson.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)
                as? Map<String, String> ?: return

            val pat      = (map["personal_access_token"] ?: "").trim()
            val adminKey = (map["project_admin_key"] ?: "").trim()
            val anonKey  = (map["anon_key"] ?: "").trim()
            val email    = (map["email"] ?: "").trim()
            val password = (map["password"] ?: "")

            // Only migrate if we found sensitive data in the file
            val hasSensitive = listOf(pat, adminKey, anonKey, email, password).any { it.isNotBlank() }
            if (!hasSensitive) return

            // Write to Keystore
            securePut(
                KEY_PAT       to pat,
                KEY_ADMIN_KEY to adminKey,
                KEY_ANON_KEY  to anonKey,
                KEY_EMAIL     to email,
                KEY_PASSWORD  to password
            )

            // Scrub sensitive fields from the JSON file — keep only url and ref
            val scrubbed = mapOf(
                "project_url" to (SupabaseConfig.normalizeUrl(map["project_url"] ?: "")),
                "project_ref" to (map["project_ref"] ?: "").trim()
            )
            file.writeText(gson.toJson(scrubbed))
        } catch (_: Exception) { }
    }

    // ── Config load/save ──────────────────────────────────────────────────────
    // Non-sensitive (url, ref) → JSON file in Downloads
    // Sensitive (keys, password) → Android Keystore

    fun loadConfig(): SupabaseConfig {
        // Load non-sensitive from file
        var url = ""; var ref = ""
        try {
            if (configFile().exists()) {
                val map = gson.fromJson(
                    configFile().readText(),
                    object : TypeToken<Map<String, String>>() {}.type
                ) as? Map<String, String> ?: emptyMap()
                url = SupabaseConfig.normalizeUrl(map["project_url"] ?: "")
                ref = (map["project_ref"] ?: "").trim().ifBlank { SupabaseConfig.guessRef(url) }
            }
        } catch (_: Exception) { }

        // Load sensitive from Keystore
        return SupabaseConfig(
            project_url           = url,
            project_ref           = ref,
            personal_access_token = secureGet(KEY_PAT),
            project_admin_key     = secureGet(KEY_ADMIN_KEY),
            anon_key              = secureGet(KEY_ANON_KEY),
            email                 = secureGet(KEY_EMAIL),
            password              = secureGet(KEY_PASSWORD)
        )
    }

    fun saveConfig(cfg: SupabaseConfig) {
        val url = SupabaseConfig.normalizeUrl(cfg.project_url)
        val ref = cfg.project_ref.trim().ifBlank { SupabaseConfig.guessRef(url) }

        // Non-sensitive to Downloads JSON
        try {
            configFile().writeText(gson.toJson(mapOf(
                "project_url" to url,
                "project_ref" to ref
            )))
        } catch (_: Exception) { }

        // Sensitive to Keystore
        securePut(
            KEY_PAT       to cfg.personal_access_token.trim(),
            KEY_ADMIN_KEY to cfg.project_admin_key.trim(),
            KEY_ANON_KEY  to cfg.anon_key.trim(),
            KEY_EMAIL     to cfg.email.trim(),
            KEY_PASSWORD  to cfg.password
        )
    }

    fun deleteConfig() {
        configFile().delete()
        secureRemove(KEY_PAT, KEY_ADMIN_KEY, KEY_ANON_KEY, KEY_EMAIL, KEY_PASSWORD)
    }

    // ── Settings (non-sensitive — stays in Downloads) ─────────────────────────

    fun loadSettings(): SupabaseSettings = try {
        val map = gson.fromJson(
            settingsFile().readText(),
            object : TypeToken<Map<String, Any>>() {}.type
        ) as? Map<String, Any> ?: emptyMap()
        SupabaseSettings(
            timeout_seconds    = ((map["timeout_seconds"] as? Double)?.toInt() ?: 40).coerceIn(10, 120),
            table_preview_rows = ((map["table_preview_rows"] as? Double)?.toInt() ?: 5).coerceIn(1, 20),
            app_pin            = (map["app_pin"] as? String ?: "").trim(),
            dark_mode          = (map["dark_mode"] as? Boolean) ?: true,
            compact_mode       = (map["compact_mode"] as? Boolean) ?: false,
            use_biometrics     = (map["use_biometrics"] as? Boolean) ?: false
        )
    } catch (_: Exception) { SupabaseSettings() }

    fun saveSettings(s: SupabaseSettings) {
        try {
            settingsFile().writeText(gson.toJson(mapOf(
                "timeout_seconds"    to s.timeout_seconds.coerceIn(10, 120),
                "table_preview_rows" to s.table_preview_rows.coerceIn(1, 20),
                "app_pin"            to s.app_pin.trim(),
                "dark_mode"          to s.dark_mode,
                "compact_mode"       to s.compact_mode,
                "use_biometrics"     to s.use_biometrics
            )))
        } catch (_: Exception) { }
    }

    // ── Connections ───────────────────────────────────────────────────────────
    // Nicknames/URLs saved in Downloads JSON
    // Sensitive fields per-connection saved in Keystore keyed by connection ID

    fun loadConnections(): List<SavedConnection> {
        val shells: List<Map<String, String>> = try {
            gson.fromJson(
                connectionsFile().readText(),
                object : TypeToken<List<Map<String, String>>>() {}.type
            ) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        return shells.mapNotNull { map ->
            val id = (map["id"] ?: "").ifBlank { return@mapNotNull null }
            SavedConnection(
                id          = id,
                nickname    = map["nickname"] ?: "",
                project_url = map["project_url"] ?: "",
                project_ref = map["project_ref"] ?: "",
                savedAt     = map["savedAt"] ?: "",
                // Sensitive — from Keystore
                personal_access_token = secureGet(connPat(id)),
                project_admin_key     = secureGet(connAdminKey(id)),
                anon_key              = secureGet(connAnonKey(id)),
                email                 = secureGet(connEmail(id)),
                password              = secureGet(connPassword(id))
            )
        }
    }

    fun saveConnections(list: List<SavedConnection>) {
        // Non-sensitive shells to Downloads
        val shells = list.map { mapOf(
            "id"          to it.id,
            "nickname"    to it.nickname,
            "project_url" to it.project_url,
            "project_ref" to it.project_ref,
            "savedAt"     to it.savedAt
        )}
        try { connectionsFile().writeText(gson.toJson(shells)) } catch (_: Exception) { }

        // Sensitive fields to Keystore per connection
        list.forEach { conn ->
            securePut(
                connPat(conn.id)      to conn.personal_access_token,
                connAdminKey(conn.id) to conn.project_admin_key,
                connAnonKey(conn.id)  to conn.anon_key,
                connEmail(conn.id)    to conn.email,
                connPassword(conn.id) to conn.password
            )
        }
    }

    fun deleteConnection(id: String) {
        secureRemove(connPat(id), connAdminKey(id), connAnonKey(id), connEmail(id), connPassword(id))
    }

    // ── WebView credentials ───────────────────────────────────────────────────
    // Email + rememberMe → webview_credentials.json (not sensitive)
    // Password → Keystore

    fun loadWebViewCredentials(): WebViewCredentials {
        var email = ""; var rememberMe = false
        try {
            if (webviewCredFile().exists()) {
                val map = gson.fromJson(
                    webviewCredFile().readText(),
                    object : TypeToken<Map<String, Any>>() {}.type
                ) as? Map<String, Any> ?: emptyMap()
                email      = (map["email"] as? String ?: "").trim()
                rememberMe = (map["rememberMe"] as? Boolean) ?: false
            }
        } catch (_: Exception) { }
        return WebViewCredentials(
            email      = email,
            password   = secureGet(KEY_WV_PASS),
            rememberMe = rememberMe
        )
    }

    fun saveWebViewCredentials(creds: WebViewCredentials) {
        try {
            webviewCredFile().writeText(gson.toJson(mapOf(
                "email"      to creds.email.trim(),
                "rememberMe" to creds.rememberMe
            )))
        } catch (_: Exception) { }
        securePut(KEY_WV_PASS to creds.password)
    }

    fun clearWebViewCredentials() {
        saveWebViewCredentials(WebViewCredentials())
        secureRemove(KEY_WV_PASS)
    }

    // ── SQL Snippets (non-sensitive — stays in Downloads) ─────────────────────

    fun loadSqlSnippets(): List<SqlSnippet> = try {
        gson.fromJson(
            sqlSnippetsFile().readText(),
            object : TypeToken<List<SqlSnippet>>() {}.type
        ) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun saveSqlSnippets(list: List<SqlSnippet>) {
        try { sqlSnippetsFile().writeText(gson.toJson(list)) } catch (_: Exception) { }
    }
}
