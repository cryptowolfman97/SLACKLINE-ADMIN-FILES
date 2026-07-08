package com.shvertex.supaadmin.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val MANAGEMENT_BASE = "https://api.supabase.com/v1"

// ── Data models ───────────────────────────────────────────────────────────────

data class SupabaseProject(
    val id: String        = "",
    val name: String      = "",
    val status: String    = "",
    val region: String    = "",
    val region_name: String = "",
    val plan: String?     = null,
    val subscription: String? = null,
    val tier: String?     = null
) {
    val ref: String get() = id
    val displayRegion: String get() = region.ifBlank { region_name }
    val planName: String get() = (plan ?: subscription ?: tier ?: "--").ifBlank { "--" }
}

data class SupabaseUser(
    val id: String          = "",
    val email: String?      = null,
    val phone: String?      = null,
    val provider: String?   = null,
    val app_metadata: Map<String, Any>? = null,
    val user_metadata: Map<String, Any>? = null,
    val created_at: String? = null,
    val last_sign_in_at: String? = null,
    val updated_at: String? = null,
    val banned_until: String? = null,
    val confirmed_at: String? = null
) {
    val displayName: String get() = email ?: phone ?: id
    val providerStr: String get() =
        provider ?: (app_metadata?.get("provider") as? String) ?: "--"
    val isBanned: Boolean get() = banned_until?.let {
        try { Instant.parse(it).isAfter(Instant.now()) } catch (_: Exception) { false }
    } ?: false
    val isConfirmed: Boolean get() = confirmed_at != null
}

data class SupabaseTable(
    val schema: String         = "public",
    val name: String           = "",
    val row_estimate: String   = "--",
    val primary_key_text: String = "--",
    val rls_enabled: Boolean?  = null,
    val replica_identity: String = "--",
    val source: String         = "pg_meta"
)

data class SupabaseColumn(
    val name: String       = "",
    val data_type: String  = "",
    val is_nullable: Boolean = true,
    val default_value: String? = null,
    val is_primary_key: Boolean = false
)

data class SupabaseBucket(
    val id: String            = "",
    val name: String          = "",
    val public: Boolean       = false,
    val file_size_limit: Long? = null,
    val allowed_mime_types: List<String>? = null,
    val created_at: String?   = null,
    val updated_at: String?   = null
) {
    val sizeLimitDisplay: String get() = file_size_limit?.let { formatBytes(it) } ?: "Unlimited"
}

data class StorageObject(
    val id: String?           = null,
    val name: String          = "",
    val bucket_id: String?    = null,
    val created_at: String?   = null,
    val updated_at: String?   = null,
    val metadata: Map<String, Any>? = null
) {
    val isFolder: Boolean get() = name.endsWith("/") || (metadata == null && !name.contains("."))
    val displaySize: String get() {
        val size = (metadata?.get("size") as? Double)?.toLong()
        return size?.let { formatBytes(it) } ?: "--"
    }
}

data class SupabaseFunction(
    val id: String          = "",
    val name: String        = "",
    val slug: String        = "",
    val status: String      = "--",
    val version: Int        = 0,
    val verify_jwt: Boolean = true,
    val updated_at: String? = null,
    val created_at: String? = null
) {
    val displayName: String get() = name.ifBlank { slug }.ifBlank { id }
    val isActive: Boolean get() = status.lowercase() in listOf("active", "online", "deployed")
}

data class SupabaseSecret(
    val name: String       = "",
    val digest: String?    = null,
    val updated_at: String? = null
)

data class SupabaseRlsPolicy(
    val id: String        = "",
    val name: String      = "",
    val table: String     = "",
    val schema: String    = "public",
    val command: String   = "",
    val roles: List<String> = emptyList(),
    val definition: String? = null,
    val check: String?    = null,
    val enabled: Boolean  = true
)

data class SupabaseMigration(
    val version: String    = "",
    val name: String       = "",
    val inserted_at: String? = null,
    val status: String     = "--"
)

// ── NEW: Cron job model ───────────────────────────────────────────────────────

data class CronJob(
    val jobid: Long        = 0,
    val jobname: String    = "",
    val schedule: String   = "",
    val command: String    = "",
    val nodename: String   = "",
    val active: Boolean    = true
)

// ── NEW: Webhook model ────────────────────────────────────────────────────────

data class DatabaseWebhook(
    val id: Long           = 0,
    val name: String       = "",
    val table_name: String = "",
    val schema_name: String = "public",
    val events: List<String> = emptyList(),
    val service_url: String = "",
    val active: Boolean    = true
)

// ── Overview / Usage / Logs payloads ──────────────────────────────────────────

data class OverviewPayload(
    val projects: List<SupabaseProject>     = emptyList(),
    val currentProject: SupabaseProject     = SupabaseProject(),
    val functions: List<SupabaseFunction>   = emptyList(),
    val secrets: List<SupabaseSecret>       = emptyList(),
    val users: List<SupabaseUser>           = emptyList(),
    val tables: List<SupabaseTable>         = emptyList(),
    val buckets: List<SupabaseBucket>       = emptyList(),
    val errors: List<String>               = emptyList()
)

data class UsageMetrics(
    val dbSizeBytes: Long?     = null,
    val storageSizeBytes: Long? = null,
    val monthlyActiveUsers: Int? = null,
    val thirdPartyMau: Int?    = null,
    val apiRequestsCount: Long? = null,
    val edgeInvocations: Long? = null,
    val planName: String       = "--",
    val region: String         = "--"
)

data class UsagePayload(
    val metrics: UsageMetrics            = UsageMetrics(),
    val sources: Map<String, String>     = emptyMap(),
    val notes: List<String>              = emptyList(),
    val currentProject: SupabaseProject  = SupabaseProject()
)

data class LogServicePayload(
    val serviceName: String         = "",
    val selectedRange: String       = "",
    val endpoint: String            = "",
    val count: Int?                 = null,
    val records: List<Map<String, Any>> = emptyList(),
    val error: String?              = null,
    val notes: List<String>         = emptyList()
)

data class TablePreviewDialog(val key: String, val rows: List<Any>)
data class UpdateRowContext(val tableName: String, val pkGuess: String)
data class InsertRowContext(val tableName: String, val columns: List<SupabaseColumn>)

data class PendingAction(
    val title: String,
    val endpoint: String,
    val method: String,
    val useManagement: Boolean,
    val requireAdmin: Boolean,
    val payloadTemplate: Any?,
    val successCallback: (() -> Unit)? = null
)

// ── Realtime event model ──────────────────────────────────────────────────────

data class RealtimeEvent(
    val timestamp: String  = "",
    val eventType: String  = "",  // INSERT, UPDATE, DELETE
    val schema: String     = "public",
    val table: String      = "",
    val payload: String    = ""
)

// ── Search result model ───────────────────────────────────────────────────────

data class GlobalSearchResult(
    val type: String,   // "table", "user", "function", "secret", "bucket"
    val label: String,
    val subtitle: String,
    val navSection: NavSection,
    val navScreen: NavScreen
)

// ── Navigation model ──────────────────────────────────────────────────────────

enum class NavSection {
    HOME, DATABASE, AUTH, DEVTOOLS, MORE
}

enum class NavScreen {
    // HOME
    DASHBOARD, OVERVIEW, PROJECTS, USAGE,
    // DATABASE
    TABLES, SQL, POLICIES, MIGRATIONS, CRON, WEBHOOKS,
    // AUTH
    USERS, SECRETS,
    // DEVTOOLS
    STORAGE, FUNCTIONS, LOGS, REALTIME,
    // MORE
    CONNECTIONS, CREDENTIALS, WEB_DASHBOARD, SETTINGS
}

// ── API client ────────────────────────────────────────────────────────────────

class SupabaseApi(private val cfg: SupabaseConfig, private val timeoutSec: Int = 40) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
        .readTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
        .writeTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ── Low-level HTTP ─────────────────────────────────────────────────────

    private fun buildBody(json: Any?): RequestBody? =
        if (json == null) null
        else gson.toJson(json).toRequestBody("application/json".toMediaType())

    private fun execute(method: String, url: String, headers: Map<String, String>,
                        params: Map<String, String>? = null, body: RequestBody? = null): JsonElement {
        val urlBuilder = url.toHttpUrl().newBuilder()
        params?.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val req = Request.Builder().url(urlBuilder.build())
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .method(method, body ?: if (method in listOf("POST", "PUT", "PATCH", "DELETE")) "".toRequestBody("application/json".toMediaType()) else null)
            .build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (resp.code == 204) return JsonParser.parseString("{\"status\":\"Success\"}")
        if (!resp.isSuccessful) {
            val msg = try {
                val obj = JsonParser.parseString(respBody).asJsonObject
                val pieces = listOf("message", "msg", "error_description", "error", "code")
                    .mapNotNull { k -> obj.get(k)?.takeIf { !it.isJsonNull }?.asString?.let { "$k: $it" } }
                "HTTP ${resp.code} - ${pieces.joinToString(" | ").ifBlank { respBody.take(200) }}"
            } catch (_: Exception) { "HTTP ${resp.code} - ${respBody.take(200)}" }
            throw SupabaseApiException(resp.code, msg)
        }
        return if (respBody.isBlank()) JsonParser.parseString("{}")
        else JsonParser.parseString(respBody)
    }

    private fun mgmtGet(path: String, params: Map<String, String>? = null) =
        execute("GET", MANAGEMENT_BASE + path, cfg.managementHeaders(), params)

    private fun mgmtPost(path: String, body: Any?) =
        execute("POST", MANAGEMENT_BASE + path, cfg.managementHeaders(), null, buildBody(body))

    private fun mgmtPatch(path: String, body: Any?) =
        execute("PATCH", MANAGEMENT_BASE + path, cfg.managementHeaders(), null, buildBody(body))

    private fun mgmtDelete(path: String, body: Any?) =
        execute("DELETE", MANAGEMENT_BASE + path, cfg.managementHeaders(), null, buildBody(body))

    private fun projGet(path: String, params: Map<String, String>? = null, useAnon: Boolean = false): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        if (!cfg.hasAdmin && !useAnon) throw SupabaseApiException(0, "Save the project admin/service key first.")
        return execute("GET", cfg.normalizedUrl + path, cfg.projectHeaders(useAnon), params)
    }

    private fun projPost(path: String, body: Any?, params: Map<String, String>? = null, useAnon: Boolean = false): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        return execute("POST", cfg.normalizedUrl + path, cfg.projectHeaders(useAnon), params, buildBody(body))
    }

    private fun projPut(path: String, body: Any?): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        return execute("PUT", cfg.normalizedUrl + path, cfg.projectHeaders(), null, buildBody(body))
    }

    private fun projPatch(path: String, body: Any?, matchCol: String? = null, matchVal: String? = null): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        val params = if (matchCol != null && matchVal != null) mapOf("$matchCol" to "eq.$matchVal") else null
        return execute("PATCH", cfg.normalizedUrl + path, cfg.projectHeaders(), params, buildBody(body))
    }

    private fun projDelete(path: String, params: Map<String, String>? = null): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        return execute("DELETE", cfg.normalizedUrl + path, cfg.projectHeaders(), params)
    }

    // ── Credential validation ──────────────────────────────────────────────

    fun testPat(): Int {
        if (!cfg.hasPat) throw SupabaseApiException(0, "Save the personal access token first.")
        val data = mgmtGet("/projects")
        return listifyJson(data).size
    }

    fun testProjectKey(): Int {
        val data = projGet("/auth/v1/admin/users", mapOf("page" to "1", "per_page" to "1"))
        return listifyJson(data).size
    }

    fun testCloudAuth(): String {
        if (!cfg.hasUrl)  throw SupabaseApiException(0, "Save the project URL first.")
        if (!cfg.hasAnon) throw SupabaseApiException(0, "Save the anon key first.")
        if (cfg.email.isBlank() || cfg.password.isBlank())
            throw SupabaseApiException(0, "Save both email and password first.")
        val headers = mapOf(
            "apikey"        to cfg.anon_key,
            "Authorization" to "Bearer ${cfg.anon_key}",
            "Content-Type"  to "application/json"
        )
        val resp = execute("POST", cfg.normalizedUrl + "/auth/v1/token",
            headers, mapOf("grant_type" to "password"),
            buildBody(mapOf("email" to cfg.email, "password" to cfg.password)))
        val user = resp.asJsonObject.getAsJsonObject("user")
        return user?.get("email")?.asString ?: cfg.email
    }

    // ── Projects ───────────────────────────────────────────────────────────

    fun listProjects(): List<SupabaseProject> {
        if (!cfg.hasPat) throw SupabaseApiException(0, "Save the personal access token first.")
        return listifyJson(mgmtGet("/projects")).map { gson.fromJson(it, SupabaseProject::class.java) }
    }

    fun getProject(): SupabaseProject {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Save a project URL or project ref first.") }
        return gson.fromJson(mgmtGet("/projects/$ref"), SupabaseProject::class.java)
    }

    // ── Functions ──────────────────────────────────────────────────────────

    fun listFunctions(): List<SupabaseFunction> {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Save a project URL or project ref first.") }
        return listifyJson(mgmtGet("/projects/$ref/functions")).map { gson.fromJson(it, SupabaseFunction::class.java) }
    }

    fun invokeFunction(slug: String, body: Map<String, Any> = emptyMap()): String {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        val key = if (cfg.hasAdmin) cfg.project_admin_key else cfg.anon_key
        val headers = mapOf(
            "Authorization" to "Bearer $key",
            "Content-Type"  to "application/json"
        )
        val resp = execute("POST", "${cfg.normalizedUrl}/functions/v1/$slug", headers, null, buildBody(body))
        return prettyJsonEl(resp)
    }

    // ── Secrets ────────────────────────────────────────────────────────────

    fun listSecrets(): List<SupabaseSecret> {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Save a project URL or project ref first.") }
        return listifyJson(mgmtGet("/projects/$ref/secrets")).map { gson.fromJson(it, SupabaseSecret::class.java) }
    }

    fun createSecret(name: String, value: String) {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Save a project URL or project ref first.") }
        mgmtPost("/projects/$ref/secrets", listOf(mapOf("name" to name, "value" to value)))
    }

    fun deleteSecret(name: String) {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Save a project URL or project ref first.") }
        mgmtDelete("/projects/$ref/secrets", listOf(name))
    }

    // ── Users ──────────────────────────────────────────────────────────────

    fun listUsers(limit: Int = 500): List<SupabaseUser> {
        val users = mutableListOf<SupabaseUser>()
        var page = 1
        val perPage = 100
        while (true) {
            val data = projGet("/auth/v1/admin/users", mapOf("page" to "$page", "per_page" to "$perPage"))
            val chunk = listifyJson(data).map { gson.fromJson(it, SupabaseUser::class.java) }
            if (chunk.isEmpty()) break
            users.addAll(chunk)
            if (chunk.size < perPage || users.size >= limit) break
            page++
        }
        return users.take(limit)
    }

    fun createUser(email: String, password: String, metadata: Map<String, Any> = emptyMap()) =
        projPost("/auth/v1/admin/users", mapOf("email" to email, "password" to password, "user_metadata" to metadata))

    fun updateUser(userId: String, payload: Map<String, Any>) =
        projPut("/auth/v1/admin/users/$userId", payload)

    fun deleteUser(userId: String) =
        projDelete("/auth/v1/admin/users/$userId")

    fun banUser(userId: String): JsonElement {
        val futureDate = "2099-12-31T23:59:59Z"
        return projPut("/auth/v1/admin/users/$userId", mapOf("banned_until" to futureDate))
    }

    fun unbanUser(userId: String): JsonElement =
        projPut("/auth/v1/admin/users/$userId", mapOf("banned_until" to "none"))

    fun sendPasswordReset(email: String): JsonElement {
        if (!cfg.hasUrl || !cfg.hasAnon) throw SupabaseApiException(0, "Project URL and anon key required.")
        val headers = mapOf(
            "apikey"        to cfg.anon_key,
            "Authorization" to "Bearer ${cfg.anon_key}",
            "Content-Type"  to "application/json"
        )
        return execute("POST", cfg.normalizedUrl + "/auth/v1/recover", headers, null,
            buildBody(mapOf("email" to email)))
    }

    // ── Tables ─────────────────────────────────────────────────────────────

    fun listTables(): List<SupabaseTable> {
        val paramSets = listOf(
            mapOf("limit" to "250", "excluded_schemas" to "pg_catalog,information_schema,extensions"),
            mapOf("limit" to "250", "included_schemas" to "public"),
            mapOf("limit" to "250")
        )
        var lastError: Exception? = null
        if (cfg.hasUrl && cfg.hasAdmin) {
            for (params in paramSets) {
                try {
                    val data = projGet("/pg/meta/tables", params)
                    val rows = listifyJson(data)
                    if (rows.isNotEmpty()) return rows.mapNotNull { compactTable(it) }.sortedWith(compareBy({ it.schema }, { it.name }))
                } catch (e: Exception) { lastError = e }
            }
        }
        if (cfg.hasPat && cfg.currentRef.isNotBlank()) {
            try { return listTablesViaOpenApi() } catch (e: Exception) { lastError = e }
        }
        lastError?.let { throw it }
        return emptyList()
    }

    fun listTableColumns(tableName: String, schemaName: String = "public"): List<SupabaseColumn> {
        return try {
            val data = projGet("/pg/meta/columns", mapOf(
                "table_name" to tableName,
                "schema_name" to schemaName
            ))
            listifyJson(data).map { el ->
                val o = el.asJsonObject
                SupabaseColumn(
                    name = o.get("name")?.asString ?: "",
                    data_type = o.get("data_type")?.asString ?: o.get("format")?.asString ?: "text",
                    is_nullable = o.get("is_nullable")?.asBoolean ?: true,
                    default_value = o.get("default_value")?.takeIf { !it.isJsonNull }?.asString,
                    is_primary_key = o.get("is_primary_key")?.asBoolean ?: false
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun listRlsPolicies(tableName: String? = null): List<SupabaseRlsPolicy> {
        return try {
            val params = if (tableName != null) mapOf("table" to tableName) else emptyMap()
            val data = projGet("/pg/meta/policies", params)
            listifyJson(data).map { el ->
                val o = el.asJsonObject
                SupabaseRlsPolicy(
                    id = o.get("id")?.asString ?: "",
                    name = o.get("name")?.asString ?: "",
                    table = o.get("table")?.asString ?: "",
                    schema = o.get("schema")?.asString ?: "public",
                    command = o.get("command")?.asString ?: "",
                    roles = (o.getAsJsonArray("roles")?.map { it.asString }) ?: emptyList(),
                    definition = o.get("definition")?.takeIf { !it.isJsonNull }?.asString,
                    check = o.get("check")?.takeIf { !it.isJsonNull }?.asString,
                    enabled = o.get("enabled")?.asBoolean ?: true
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun listTablesViaOpenApi(): List<SupabaseTable> {
        val ref = cfg.currentRef
        val data = mgmtGet("/projects/$ref/database/openapi")
        val paths = data.asJsonObject.getAsJsonObject("paths") ?: return emptyList()
        val seen = mutableSetOf<Pair<String, String>>()
        val rows = mutableListOf<SupabaseTable>()
        paths.entrySet().forEach { (rawPath, methods) ->
            val parts = rawPath.trimStart('/').split("/").filter { it.isNotBlank() }
            if (parts.isEmpty() || parts[0] == "rpc") return@forEach
            val name = parts[0]
            if (name.contains("{") || name.contains("}")) return@forEach
            var schema = "public"
            val getObj = (methods as? com.google.gson.JsonObject)?.getAsJsonObject("get")
            val tags = getObj?.getAsJsonArray("tags")
            tags?.forEach { tag ->
                val t = tag.asString.trim()
                if (t.isNotBlank() && t.lowercase() !in setOf("default", "public")) schema = t
            }
            if (seen.add(schema to name))
                rows.add(SupabaseTable(schema = schema, name = name, source = "management_openapi"))
        }
        return rows.sortedWith(compareBy({ it.schema }, { it.name }))
    }

    private fun compactTable(el: JsonElement): SupabaseTable? {
        if (!el.isJsonObject) return null
        val o = el.asJsonObject
        val schema = o.get("schema")?.asString ?: "public"
        val name = o.get("name")?.asString ?: o.get("table")?.asString ?: return null
        val rowEst = (o.get("rows") ?: o.get("row_count") ?: o.get("live_rows_estimate"))?.let {
            if (it.isJsonNull) "--" else it.asString
        } ?: "--"
        val pkText = compactPrimaryKey(o)
        val rls = o.get("rls_enabled")?.let { if (it.isJsonNull) null else it.asBoolean }
        return SupabaseTable(
            schema = schema, name = name, row_estimate = rowEst,
            primary_key_text = pkText, rls_enabled = rls,
            replica_identity = o.get("replica_identity")?.asString ?: "--"
        )
    }

    private fun compactPrimaryKey(o: com.google.gson.JsonObject): String {
        val pk = o.get("primary_keys") ?: o.get("primary_key") ?: o.get("pk") ?: return "--"
        if (pk.isJsonPrimitive) return pk.asString.ifBlank { "--" }
        if (pk.isJsonArray) {
            val cols = pk.asJsonArray.mapNotNull { item ->
                if (item.isJsonPrimitive) item.asString
                else if (item.isJsonObject) {
                    val obj = item.asJsonObject
                    (obj.getAsJsonArray("columns") ?: obj.getAsJsonArray("column_names"))
                        ?.joinToString(", ") { it.asString }
                        ?: obj.get("name")?.asString
                } else null
            }.filter { it.isNotBlank() }
            return cols.take(6).joinToString(", ").ifBlank { "--" }
        }
        return "--"
    }

    fun previewTable(tableName: String, schemaName: String = "public", limit: Int = 5): List<Any> {
        val data = projGet("/rest/v1/$tableName", mapOf("select" to "*", "limit" to "${limit.coerceIn(1, 20)}"))
        return listifyJson(data).map { if (it.isJsonObject) gson.fromJson(it, Map::class.java) else it }
    }

    // ── Full table data (paginated) ────────────────────────────────────────

    fun fetchTableRows(tableName: String, schemaName: String = "public", limit: Int = 50, offset: Int = 0): List<Map<String, Any>> {
        val data = projGet("/rest/v1/$tableName", mapOf(
            "select" to "*",
            "limit"  to "$limit",
            "offset" to "$offset"
        ))
        return listifyJson(data).mapNotNull { if (it.isJsonObject) gson.fromJson(it, Map::class.java) as? Map<String, Any> else null }
    }

    // ── Row CRUD ───────────────────────────────────────────────────────────

    fun updateRow(tableName: String, matchCol: String, matchVal: String, payload: Map<String, Any>) =
        projPatch("/rest/v1/$tableName", payload, matchCol, matchVal)

    fun insertRow(tableName: String, payload: Map<String, Any>) =
        projPost("/rest/v1/$tableName", payload)

    fun deleteRow(tableName: String, matchCol: String, matchVal: String) =
        projDelete("/rest/v1/$tableName", mapOf("$matchCol" to "eq.$matchVal"))

    // ── CSV Export ─────────────────────────────────────────────────────────

    fun exportTableCsv(tableName: String, limit: Int = 50000): String {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        if (!cfg.hasAdmin) throw SupabaseApiException(0, "Service key required for CSV export.")
        val key = cfg.project_admin_key
        val urlBuilder = "${cfg.normalizedUrl}/rest/v1/$tableName".toHttpUrl().newBuilder()
        urlBuilder.addQueryParameter("select", "*")
        urlBuilder.addQueryParameter("limit", "$limit")
        val req = Request.Builder()
            .url(urlBuilder.build())
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Accept", "text/csv")
            .get()
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw SupabaseApiException(resp.code, "CSV export failed: HTTP ${resp.code} — $body")
        return body
    }

    fun exportSqlResultCsv(query: String): String {
        val ref = cfg.currentRef.ifBlank { throw SupabaseApiException(0, "Project ref required.") }
        if (!cfg.hasPat) throw SupabaseApiException(0, "PAT required for SQL CSV export.")
        val result = mgmtPost("/projects/$ref/database/query", mapOf("query" to query))
        val rows = if (result.isJsonArray) result.asJsonArray.toList()
                   else listifyJson(result)
        if (rows.isEmpty()) return "-- No rows returned"
        val sb = StringBuilder()
        val firstObj = rows.firstOrNull()?.let { if (it.isJsonObject) it.asJsonObject else null }
        val headers = firstObj?.keySet()?.toList() ?: return "-- Could not determine columns"
        sb.appendLine(headers.joinToString(",") { escapeCsvCell(it) })
        rows.forEach { row ->
            if (row.isJsonObject) {
                val obj = row.asJsonObject
                sb.appendLine(headers.joinToString(",") { col ->
                    val v = obj.get(col)
                    if (v == null || v.isJsonNull) "" else escapeCsvCell(v.asString)
                })
            }
        }
        return sb.toString()
    }

    private fun escapeCsvCell(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            "\"${value.replace("\"", "\"\"")}\""
        else value
    }

    // ── Storage ────────────────────────────────────────────────────────────

    fun listBuckets(): List<SupabaseBucket> {
        val data = projGet("/storage/v1/bucket")
        return listifyJson(data).map { gson.fromJson(it, SupabaseBucket::class.java) }
    }

    fun createBucket(name: String, public: Boolean = false, fileSizeLimit: Long? = null): JsonElement {
        val body = mutableMapOf<String, Any>("id" to name, "name" to name, "public" to public)
        fileSizeLimit?.let { body["file_size_limit"] = it }
        return projPost("/storage/v1/bucket", body)
    }

    fun deleteBucket(bucketId: String): JsonElement =
        projDelete("/storage/v1/bucket/$bucketId")

    fun emptyBucket(bucketId: String): JsonElement =
        projPost("/storage/v1/bucket/$bucketId/empty", emptyMap<String, Any>())

    fun listBucketObjects(bucketId: String, prefix: String = "", limit: Int = 100): List<StorageObject> {
        return try {
            val data = projPost("/storage/v1/object/list/$bucketId",
                mapOf("prefix" to prefix, "limit" to limit, "offset" to 0))
            listifyJson(data).map { gson.fromJson(it, StorageObject::class.java) }
        } catch (_: Exception) { emptyList() }
    }

    fun deleteStorageObject(bucketId: String, path: String): JsonElement =
        projDelete("/storage/v1/object/$bucketId/$path")

    // NEW: Upload storage object via multipart
    fun uploadStorageObject(bucketId: String, path: String, fileBytes: ByteArray, mimeType: String): JsonElement {
        if (!cfg.hasUrl) throw SupabaseApiException(0, "Save the project URL first.")
        val key = if (cfg.hasAdmin) cfg.project_admin_key else cfg.anon_key
        val mediaType = mimeType.toMediaType()
        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("", path, RequestBody.create(mediaType, fileBytes))
            .build()
        val req = Request.Builder()
            .url("${cfg.normalizedUrl}/storage/v1/object/$bucketId/$path")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .post(requestBody)
            .build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw SupabaseApiException(resp.code, "Upload failed: HTTP ${resp.code} — $respBody")
        return JsonParser.parseString(respBody.ifBlank { "{\"status\":\"uploaded\"}" })
    }

    // ── SQL ────────────────────────────────────────────────────────────────

    fun executeSql(query: String): String {
        val ref = cfg.currentRef
        if (cfg.hasPat && ref.isNotBlank()) {
            try {
                val result = mgmtPost("/projects/$ref/database/query", mapOf("query" to query))
                return prettyJsonEl(result)
            } catch (_: Exception) { }
        }
        if (cfg.hasAdmin && cfg.hasUrl) {
            try {
                val result = projPost("/pg/meta/query", mapOf("query" to query))
                return prettyJsonEl(result)
            } catch (_: Exception) { }
        }
        if (cfg.hasAdmin && cfg.hasUrl) {
            val result = projPost("/rest/v1/rpc/exec_sql", mapOf("sql_string" to query))
            return prettyJsonEl(result)
        }
        throw SupabaseApiException(0, "No viable SQL execution method. Ensure PAT + project ref, or service key + project URL.")
    }

    fun executeSqlRaw(query: String): JsonElement {
        val ref = cfg.currentRef
        if (cfg.hasPat && ref.isNotBlank()) {
            try { return mgmtPost("/projects/$ref/database/query", mapOf("query" to query)) } catch (_: Exception) { }
        }
        if (cfg.hasAdmin && cfg.hasUrl) {
            try { return projPost("/pg/meta/query", mapOf("query" to query)) } catch (_: Exception) { }
        }
        return projPost("/rest/v1/rpc/exec_sql", mapOf("sql_string" to query))
    }

    // ── NEW: Cron Jobs (via SQL) ────────────────────────────────────────────

    fun listCronJobs(): List<CronJob> {
        val raw = executeSqlRaw("SELECT jobid, jobname, schedule, command, nodename, active FROM cron.job ORDER BY jobname;")
        return listifyJson(raw).mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            CronJob(
                jobid    = o.get("jobid")?.asLong ?: 0,
                jobname  = o.get("jobname")?.asString ?: "",
                schedule = o.get("schedule")?.asString ?: "",
                command  = o.get("command")?.asString ?: "",
                nodename = o.get("nodename")?.asString ?: "",
                active   = o.get("active")?.asBoolean ?: true
            )
        }
    }

    fun createCronJob(name: String, schedule: String, command: String) {
        val sql = "SELECT cron.schedule('${name.replace("'", "''")}', '${schedule.replace("'", "''")}', '${command.replace("'", "''")}');"
        executeSql(sql)
    }

    fun deleteCronJob(jobName: String) {
        val sql = "SELECT cron.unschedule('${jobName.replace("'", "''")}');"
        executeSql(sql)
    }

    fun toggleCronJob(jobId: Long, active: Boolean) {
        val sql = "UPDATE cron.job SET active = $active WHERE jobid = $jobId;"
        executeSql(sql)
    }

    // ── NEW: Database Webhooks (via SQL) ────────────────────────────────────

    fun listWebhooks(): List<DatabaseWebhook> {
        return try {
            val raw = executeSqlRaw("""
                SELECT id, name, table_name, schema_name, events, service_url, active
                FROM supabase_functions.hooks
                ORDER BY name;
            """.trimIndent())
            listifyJson(raw).mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val o = el.asJsonObject
                val eventsRaw = o.get("events")?.asString ?: ""
                val events = eventsRaw.trim('{', '}').split(",").map { it.trim() }.filter { it.isNotBlank() }
                DatabaseWebhook(
                    id          = o.get("id")?.asLong ?: 0,
                    name        = o.get("name")?.asString ?: "",
                    table_name  = o.get("table_name")?.asString ?: "",
                    schema_name = o.get("schema_name")?.asString ?: "public",
                    events      = events,
                    service_url = o.get("service_url")?.asString ?: "",
                    active      = o.get("active")?.asBoolean ?: true
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun deleteWebhook(id: Long) {
        executeSql("DELETE FROM supabase_functions.hooks WHERE id = $id;")
    }

    // ── Migrations ─────────────────────────────────────────────────────────

    fun listMigrations(): List<SupabaseMigration> {
        return try {
            val ref = cfg.currentRef.ifBlank { return emptyList() }
            val data = mgmtGet("/projects/$ref/database/migrations")
            listifyJson(data).map { el ->
                val o = el.asJsonObject
                SupabaseMigration(
                    version = o.get("version")?.asString ?: "",
                    name = o.get("name")?.asString ?: "",
                    inserted_at = o.get("inserted_at")?.takeIf { !it.isJsonNull }?.asString,
                    status = o.get("status")?.asString ?: "--"
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ── Overview payload ───────────────────────────────────────────────────

    fun overviewPayload(): OverviewPayload {
        val errors = mutableListOf<String>()
        val projects = safeFetch("Projects", errors) { listProjects() }
        val currentProject = safeFetch("Current project", errors) { getProject() }
        val functions = safeFetch("Functions", errors) { listFunctions() }
        val secrets = safeFetch("Secrets", errors) { listSecrets() }
        val (users, tables, buckets) = if (cfg.hasAdmin) {
            Triple(
                safeFetch("Users", errors) { listUsers(1000) },
                safeFetch("Tables", errors) { listTables() },
                safeFetch("Buckets", errors) { listBuckets() }
            )
        } else {
            errors.add("Users/Tables/Storage counts need the project admin/service key.")
            Triple(emptyList(), emptyList(), emptyList())
        }
        return OverviewPayload(
            projects        = projects        ?: emptyList(),
            currentProject  = currentProject  ?: SupabaseProject(),
            functions       = functions       ?: emptyList(),
            secrets         = secrets         ?: emptyList(),
            users           = users           ?: emptyList(),
            tables          = tables          ?: emptyList(),
            buckets         = buckets         ?: emptyList(),
            errors          = errors
        )
    }

    // ── Usage payload ──────────────────────────────────────────────────────

    fun usagePayload(): UsagePayload {
        val notes = mutableListOf<String>()
        val sources = mutableMapOf<String, String>()
        val currentProject = safeFetch("Current project", notes) { getProject() } ?: SupabaseProject()

        val users: List<SupabaseUser> = if (cfg.hasAdmin) {
            safeFetch("Users", notes) { listUsers(5000) } ?: emptyList()
        } else {
            notes.add("Monthly active user counts need the project admin/service key.")
            emptyList()
        }

        val threshold = Instant.now().minusSeconds(30L * 24 * 3600)
        var mau = 0; var thirdParty = 0
        users.forEach { u ->
            val stamp = u.last_sign_in_at ?: u.created_at ?: u.updated_at ?: return@forEach
            val instant = parseIso(stamp) ?: return@forEach
            if (instant.isAfter(threshold)) {
                mau++
                val p = u.providerStr.lowercase()
                if (p !in listOf("email", "phone", "--", "")) thirdParty++
            }
        }

        var dbBytes: Long? = null
        var storageBytes: Long? = null
        var apiRequests: Long? = null
        var edgeCalls: Long? = null

        fun probe(candidates: List<String>): JsonElement? {
            for (slug in candidates) {
                for (params in listOf(mapOf("limit" to "30"), emptyMap<String, String>())) {
                    try {
                        val ref = cfg.currentRef.ifBlank { return@probe null }
                        return mgmtGet("/projects/$ref/analytics/endpoints/$slug", params)
                    } catch (_: Exception) { }
                }
            }
            return null
        }

        probe(listOf("usage.database-size", "usage.database_size", "database-size"))
            ?.let { dbBytes = findNumber(it, listOf("size", "bytes")); if (dbBytes != null) sources["database_size"] = "analytics" }

        probe(listOf("usage.storage-size", "usage.storage_size", "storage-size"))
            ?.let { storageBytes = findNumber(it, listOf("size", "bytes", "storage")); if (storageBytes != null) sources["storage_size"] = "analytics" }

        probe(listOf("usage.api-requests-count", "usage.api_requests_count", "api-requests-count"))
            ?.let { apiRequests = findNumber(it, listOf("count", "requests")); if (apiRequests != null) sources["api_requests_count"] = "analytics" }

        probe(listOf("functions.combined-stats", "functions.combined_stats"))
            ?.let { edgeCalls = findNumber(it, listOf("count", "invocation", "requests", "total")); if (edgeCalls != null) sources["edge_invocations"] = "analytics" }

        val metrics = UsageMetrics(
            dbSizeBytes = dbBytes, storageSizeBytes = storageBytes,
            monthlyActiveUsers = if (cfg.hasAdmin) mau else null,
            thirdPartyMau = if (cfg.hasAdmin) thirdParty else null,
            apiRequestsCount = apiRequests, edgeInvocations = edgeCalls,
            planName = currentProject.planName, region = currentProject.displayRegion
        )
        return UsagePayload(metrics, sources, notes, currentProject)
    }

    // ── Logs payload ───────────────────────────────────────────────────────

    fun logPayload(serviceName: String, rangeLabel: String, limit: Int = 10): LogServicePayload {
        val hours = mapOf("1 Hour" to 1, "24 Hours" to 24, "7 Days" to 168, "30 Days" to 720)[rangeLabel] ?: 24
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val start = now.minusHours(hours.toLong())
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val notes = mutableListOf<String>()

        val candidates = mapOf(
            "API Gateway"    to listOf("logs.api-gateway", "logs.api_gateway", "api-gateway", "api_gateway"),
            "Postgres"       to listOf("logs.postgres", "postgres"),
            "PostgREST"      to listOf("logs.postgrest", "postgrest"),
            "Pooler"         to listOf("logs.pooler", "pooler", "logs.supavisor", "supavisor"),
            "Auth"           to listOf("logs.auth", "auth"),
            "Storage"        to listOf("logs.storage", "storage"),
            "Realtime"       to listOf("logs.realtime", "realtime"),
            "Edge Functions" to listOf("logs.edge-functions", "logs.edge_functions", "edge-functions", "edge_functions"),
            "Cron"           to listOf("logs.cron", "cron", "logs.pg-cron", "pg-cron")
        )[serviceName] ?: emptyList()

        val ref = cfg.currentRef.ifBlank {
            return LogServicePayload(serviceName = serviceName, selectedRange = rangeLabel, error = "Save a project URL or project ref first.", notes = notes)
        }

        val paramVariants = listOf(
            mapOf("limit" to "$limit", "hours" to "$hours"),
            mapOf("limit" to "$limit", "iso_timestamp_start" to start.format(fmt), "iso_timestamp_end" to now.format(fmt)),
            mapOf("limit" to "$limit"),
            emptyMap()
        )

        for (params in paramVariants) {
            for (slug in candidates) {
                try {
                    val result = mgmtGet("/projects/$ref/analytics/endpoints/$slug", params)
                    val records = extractRecords(result, limit.coerceIn(4, 20))
                    val count = findNumber(result, listOf("count", "total", "entries", "events", "results"))?.toInt() ?: records.size
                    return LogServicePayload(
                        serviceName = serviceName, selectedRange = rangeLabel,
                        endpoint = slug, count = count, records = records, notes = notes
                    )
                } catch (_: Exception) { }
            }
        }

        val errMsg = "Logs unavailable. Check your PAT and plan."
        notes.add(errMsg)
        return LogServicePayload(serviceName = serviceName, selectedRange = rangeLabel, error = errMsg, notes = notes)
    }

    // ── Generic action ─────────────────────────────────────────────────────

    fun executeAction(endpoint: String, method: String, payload: Any?, useManagement: Boolean) {
        if (useManagement) {
            when (method) {
                "POST"   -> mgmtPost(endpoint, payload)
                "PATCH"  -> mgmtPatch(endpoint, payload)
                "DELETE" -> mgmtDelete(endpoint, payload)
                else     -> throw IllegalArgumentException("Unsupported management method: $method")
            }
        } else {
            when (method) {
                "POST"   -> projPost(endpoint, payload)
                "PUT"    -> projPut(endpoint, payload)
                "PATCH"  -> projPatch(endpoint, payload)
                "DELETE" -> projDelete(endpoint)
                else     -> throw IllegalArgumentException("Unsupported method: $method")
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun listifyJson(el: JsonElement): List<JsonElement> {
        if (el.isJsonArray) return el.asJsonArray.toList()
        if (el.isJsonObject) {
            val obj = el.asJsonObject
            for (key in listOf("users", "projects", "data", "functions", "secrets", "tables", "buckets", "items", "objects", "migrations")) {
                obj.getAsJsonArray(key)?.let { return it.toList() }
            }
            obj.entrySet().forEach { (_, v) -> if (v.isJsonArray) return v.asJsonArray.toList() }
        }
        return emptyList()
    }

    private fun <T> safeFetch(label: String, errors: MutableList<String>, block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        errors.add("$label: ${e.message}"); null
    }

    private fun findNumber(el: JsonElement?, keys: List<String>): Long? {
        if (el == null || el.isJsonNull) return null
        if (el.isJsonObject) {
            el.asJsonObject.entrySet().forEach { (k, v) ->
                if (keys.any { it in k.lowercase() } && v.isJsonPrimitive && !v.isJsonNull) {
                    return try { v.asLong } catch (_: Exception) { null }
                }
            }
            el.asJsonObject.entrySet().forEach { (_, v) ->
                findNumber(v, keys)?.let { return it }
            }
        }
        if (el.isJsonArray) {
            el.asJsonArray.forEach { findNumber(it, keys)?.let { n -> return n } }
        }
        return null
    }

    private fun extractRecords(el: JsonElement?, maxItems: Int): List<Map<String, Any>> {
        if (el == null || el.isJsonNull) return emptyList()
        val list = listifyJson(el).take(maxItems)
        return list.mapNotNull { item ->
            if (item.isJsonObject) gson.fromJson(item, Map::class.java) as? Map<String, Any>
            else null
        }
    }

    private fun parseIso(value: String): Instant? = try {
        Instant.parse(value.replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
    } catch (_: Exception) { null }

    private fun prettyJsonEl(el: JsonElement): String =
        Gson().newBuilder().setPrettyPrinting().create().toJson(el)
}

// ── Top-level helpers ─────────────────────────────────────────────────────────

fun prettyJson(obj: Any?): String = Gson().newBuilder().setPrettyPrinting().create().toJson(obj)

fun shortTime(value: String?): String {
    val raw = value?.trim() ?: return "--"
    if (raw.isBlank()) return "--"
    return try {
        val instant = Instant.parse(raw.replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
        val zdt = instant.atZone(ZoneOffset.UTC)
        "%04d-%02d-%02d %02d:%02d".format(zdt.year, zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute)
    } catch (_: Exception) { raw.take(16) }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

class SupabaseApiException(val code: Int, message: String) : Exception(message)
