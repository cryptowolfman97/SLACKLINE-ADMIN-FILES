package com.example.slacklineadminapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// ── Config model ──────────────────────────────────────────────────────────────

data class SupabaseConfig(
    val project_url: String            = "",
    val project_ref: String            = "",
    val anon_key: String               = "",
    val personal_access_token: String  = "",
    val project_admin_key: String      = "",
    val email: String                  = "",
    val password: String               = ""
) {
    val normalizedUrl: String get() = normalizeUrl(project_url)
    val currentRef: String get() =
        project_ref.ifBlank { guessRef(normalizedUrl) }

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
    val timeout_seconds: Int     = 40,
    val table_preview_rows: Int  = 5,
    val app_pin: String          = "",
    val dark_mode: Boolean       = true,
    val compact_mode: Boolean    = false
)

// ── WebView credentials (separate from API credentials) ───────────────────────

data class WebViewCredentials(
    val email: String       = "",
    val password: String    = "",
    val rememberMe: Boolean = false
)

// ── SQL Snippet ────────────────────────────────────────────────────────────────

data class SqlSnippet(
    val id: String      = "",
    val name: String    = "",
    val sql: String     = "",
    val savedAt: String = ""
)

// ── Persistence ───────────────────────────────────────────────────────────────

object SupabaseStorage {
    private val gson = Gson()

    // ── Storage path routed through AppStorage (single source of truth) ────────
    private fun dir(): File = AppStorage.supabaseDataDir()

    fun dataDir(): File = dir()

    private fun configFile()       = File(dir(), "supabase_config.json")
    private fun settingsFile()     = File(dir(), "supabase_settings.json")
    private fun connectionsFile()  = File(dir(), "saved_connections.json")
    private fun webviewCredFile()  = File(dir(), "webview_credentials.json")
    private fun sqlSnippetsFile()  = File(dir(), "sql_snippets.json")

    // ── Config ────────────────────────────────────────────────────────────────

    fun loadConfig(): SupabaseConfig = try {
        val text = configFile().readText()
        val map = gson.fromJson(text, object : TypeToken<Map<String, String>>() {}.type) as? Map<String, String> ?: emptyMap()
        val url = SupabaseConfig.normalizeUrl(map["project_url"] ?: "")
        SupabaseConfig(
            project_url           = url,
            project_ref           = (map["project_ref"] ?: "").trim().ifBlank { SupabaseConfig.guessRef(url) },
            anon_key              = (map["anon_key"] ?: "").trim(),
            personal_access_token = (map["personal_access_token"] ?: "").trim(),
            project_admin_key     = (map["project_admin_key"] ?: "").trim(),
            email                 = (map["email"] ?: "").trim(),
            password              = (map["password"] ?: "")
        )
    } catch (_: Exception) { SupabaseConfig() }

    fun saveConfig(cfg: SupabaseConfig) {
        val url = SupabaseConfig.normalizeUrl(cfg.project_url)
        val map = mapOf(
            "project_url"           to url,
            "project_ref"           to cfg.project_ref.trim().ifBlank { SupabaseConfig.guessRef(url) },
            "anon_key"              to cfg.anon_key.trim(),
            "personal_access_token" to cfg.personal_access_token.trim(),
            "project_admin_key"     to cfg.project_admin_key.trim(),
            "email"                 to cfg.email.trim(),
            "password"              to cfg.password
        )
        try { configFile().writeText(gson.toJson(map)) } catch (_: Exception) { }
    }

    fun deleteConfig() { configFile().delete() }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun loadSettings(): SupabaseSettings = try {
        val text = settingsFile().readText()
        val map = gson.fromJson(text, object : TypeToken<Map<String, Any>>() {}.type) as? Map<String, Any> ?: emptyMap()
        SupabaseSettings(
            timeout_seconds    = ((map["timeout_seconds"] as? Double)?.toInt() ?: 40).coerceIn(10, 120),
            table_preview_rows = ((map["table_preview_rows"] as? Double)?.toInt() ?: 5).coerceIn(1, 20),
            app_pin            = (map["app_pin"] as? String ?: "").trim(),
            dark_mode          = (map["dark_mode"] as? Boolean) ?: true,
            compact_mode       = (map["compact_mode"] as? Boolean) ?: false
        )
    } catch (_: Exception) { SupabaseSettings() }

    fun saveSettings(s: SupabaseSettings) {
        val map = mapOf(
            "timeout_seconds"    to s.timeout_seconds.coerceIn(10, 120),
            "table_preview_rows" to s.table_preview_rows.coerceIn(1, 20),
            "app_pin"            to s.app_pin.trim(),
            "dark_mode"          to s.dark_mode,
            "compact_mode"       to s.compact_mode
        )
        try { settingsFile().writeText(gson.toJson(map)) } catch (_: Exception) { }
    }

    // ── Connections ───────────────────────────────────────────────────────────

    fun loadConnections(): List<SavedConnection> = try {
        gson.fromJson(connectionsFile().readText(),
            object : TypeToken<List<SavedConnection>>() {}.type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun saveConnections(list: List<SavedConnection>) {
        try { connectionsFile().writeText(gson.toJson(list)) } catch (_: Exception) { }
    }

    // ── WebView credentials ───────────────────────────────────────────────────

    fun loadWebViewCredentials(): WebViewCredentials = try {
        val text = webviewCredFile().readText()
        val map = gson.fromJson(text, object : TypeToken<Map<String, Any>>() {}.type) as? Map<String, Any> ?: emptyMap()
        WebViewCredentials(
            email      = (map["email"] as? String ?: "").trim(),
            password   = (map["password"] as? String ?: ""),
            rememberMe = (map["rememberMe"] as? Boolean) ?: false
        )
    } catch (_: Exception) { WebViewCredentials() }

    fun saveWebViewCredentials(creds: WebViewCredentials) {
        val map = mapOf("email" to creds.email.trim(), "password" to creds.password, "rememberMe" to creds.rememberMe)
        try { webviewCredFile().writeText(gson.toJson(map)) } catch (_: Exception) { }
    }

    fun clearWebViewCredentials() { saveWebViewCredentials(WebViewCredentials()) }

    // ── SQL Snippets ──────────────────────────────────────────────────────────

    fun loadSqlSnippets(): List<SqlSnippet> = try {
        gson.fromJson(sqlSnippetsFile().readText(),
            object : TypeToken<List<SqlSnippet>>() {}.type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun saveSqlSnippets(list: List<SqlSnippet>) {
        try { sqlSnippetsFile().writeText(gson.toJson(list)) } catch (_: Exception) { }
    }
}

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
