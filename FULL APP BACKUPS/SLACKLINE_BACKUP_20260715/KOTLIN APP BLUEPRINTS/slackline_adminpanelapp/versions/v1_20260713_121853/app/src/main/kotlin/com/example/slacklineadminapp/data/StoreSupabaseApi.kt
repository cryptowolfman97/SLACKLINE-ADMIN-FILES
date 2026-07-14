package com.example.slacklineadminapp.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

object StoreSupabaseApi {
    const val BASE_URL = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    const val ANON_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    var accessToken: String? = null
    var userId:      String? = null
    var userEmail:   String? = null

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun HttpRequestBuilder.addAuthHeaders() {
        header("apikey", ANON_KEY)
        header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
        header("Content-Type", "application/json")
        header("Prefer", "return=representation")
    }

    // ─── Auth ────────────────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String): Boolean {
        val response = client.post("$BASE_URL/auth/v1/token?grant_type=password") {
            addAuthHeaders()
            setBody(mapOf("email" to email, "password" to password))
        }
        if (response.status == HttpStatusCode.OK) {
            val body = response.body<JsonObject>()
            accessToken = body["access_token"]?.jsonPrimitive?.content
            userId      = body["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content
            userEmail   = body["user"]?.jsonObject?.get("email")?.jsonPrimitive?.content
            return true
        }
        return false
    }

    suspend fun signUp(email: String, password: String, displayName: String): Boolean {
        val response = client.post("$BASE_URL/auth/v1/signup") {
            addAuthHeaders()
            setBody(mapOf("email" to email, "password" to password,
                          "data" to mapOf("display_name" to displayName)))
        }
        return response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
    }

    suspend fun signOut() {
        try { client.post("$BASE_URL/auth/v1/logout") { addAuthHeaders() } }
        finally { accessToken = null; userId = null; userEmail = null }
    }

    suspend fun isAdmin(): Boolean {
        if (userId == null) return false
        val response = client.get("$BASE_URL/rest/v1/profiles") {
            addAuthHeaders()
            parameter("id", "eq.$userId")
            parameter("select", "is_admin")
        }
        if (response.status == HttpStatusCode.OK) {
            val list = response.body<JsonArray>()
            return list.firstOrNull()?.jsonObject?.get("is_admin")?.jsonPrimitive?.boolean ?: false
        }
        return false
    }

    // ─── Customer Data ───────────────────────────────────────────────────────

    suspend fun getNews(): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/news") {
            addAuthHeaders()
            parameter("is_published", "eq.true")
            parameter("order", "created_at.desc")
            parameter("limit", "20")
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    suspend fun getBroadcasts(): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/broadcasts") {
            addAuthHeaders()
            parameter("order", "created_at.desc")
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    suspend fun getStoreConfig(): JsonObject? {
        val response = client.get("$BASE_URL/rest/v1/store_config") {
            addAuthHeaders()
            parameter("limit", "1")
        }
        if (response.status == HttpStatusCode.OK) {
            val list = response.body<List<JsonObject>>()
            return list.firstOrNull()
        }
        return null
    }

    suspend fun getContactInfo(): JsonObject? {
        val response = client.get("$BASE_URL/rest/v1/contact_info") {
            addAuthHeaders()
            parameter("select", "*")
            parameter("limit", "1")
        }
        if (response.status == HttpStatusCode.OK) {
            val list = response.body<List<JsonObject>>()
            return list.firstOrNull()
        }
        return null
    }

    // ─── Apps CRUD ───────────────────────────────────────────────────────────

    /**
     * Fetch apps from Supabase.
     *
     * - onlyPublished = false (admin side): orders by created_at.desc so newest
     *   appears first, matching existing admin behaviour.
     * - onlyPublished = true  (store side):  orders by sort_order.asc so customers
     *   always see the custom order set by the admin. Apps with sort_order = 0
     *   fall to the bottom via the secondary created_at.desc fallback.
     */
    suspend fun getApps(onlyPublished: Boolean = false): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/apps") {
            addAuthHeaders()
            if (onlyPublished) {
                parameter("is_published", "eq.true")
                // Primary: custom order. Secondary: newest first for apps not yet ordered.
                parameter("order", "sort_order.asc,created_at.desc")
            } else {
                parameter("order", "created_at.desc")
            }
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    /** Returns only apps with is_featured = true, ordered by sort_order then name. */
    suspend fun getFeaturedApps(): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/apps") {
            addAuthHeaders()
            parameter("is_published", "eq.true")
            parameter("is_featured",  "eq.true")
            parameter("order", "sort_order.asc,name.asc")
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    suspend fun insertApp(data: Map<String, Any>): String? {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is JsonElement -> put(k, v)
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                is Int     -> put(k, v)
                is Number  -> put(k, v.toString())
                else       -> put(k, v.toString())
            }}
        }
        val response = client.post("$BASE_URL/rest/v1/apps") {
            addAuthHeaders(); setBody(json.toString())
        }
        if (response.status !in listOf(HttpStatusCode.Created, HttpStatusCode.OK)) return null
        return try {
            response.body<List<JsonObject>>().firstOrNull()?.get("id")?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }

    suspend fun updateApp(id: String, data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is JsonElement -> put(k, v)
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                is Int     -> put(k, v)
                is Number  -> put(k, v.toString())
                else       -> put(k, v.toString())
            }}
        }
        val response = client.patch("$BASE_URL/rest/v1/apps") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteApp(id: String): Boolean {
        val response = client.delete("$BASE_URL/rest/v1/apps") {
            addAuthHeaders(); parameter("id", "eq.$id")
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Ratings ─────────────────────────────────────────────────────────────

    /** Fetch all ratings for a given app. */
    suspend fun getRatings(appId: String): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/app_ratings") {
            addAuthHeaders()
            parameter("app_id", "eq.$appId")
            parameter("order",  "created_at.desc")
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    /** Return the current user's existing rating for this app, or null. */
    suspend fun getMyRating(appId: String): JsonObject? {
        val uid = userId ?: return null
        val response = client.get("$BASE_URL/rest/v1/app_ratings") {
            addAuthHeaders()
            parameter("app_id", "eq.$appId")
            parameter("user_id", "eq.$uid")
            parameter("limit", "1")
        }
        if (response.status == HttpStatusCode.OK) {
            val list = response.body<List<JsonObject>>()
            return list.firstOrNull()
        }
        return null
    }

    /** Insert a new rating row. */
    suspend fun insertRating(appId: String, stars: Int, review: String): Boolean {
        val uid = userId ?: return false
        val json = buildJsonObject {
            put("app_id",  appId)
            put("user_id", uid)
            put("stars",   stars)
            put("review",  review)
        }
        val response = client.post("$BASE_URL/rest/v1/app_ratings") {
            addAuthHeaders(); setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    /** Update an existing rating row by its id. */
    suspend fun updateRating(ratingId: String, stars: Int, review: String): Boolean {
        val json = buildJsonObject {
            put("stars",  stars)
            put("review", review)
        }
        val response = client.patch("$BASE_URL/rest/v1/app_ratings") {
            addAuthHeaders(); parameter("id", "eq.$ratingId"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── News CRUD ───────────────────────────────────────────────────────────

    suspend fun insertNews(data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                is Int     -> put(k, v)
                else       -> put(k, v.toString())
            }}
        }
        val response = client.post("$BASE_URL/rest/v1/news") {
            addAuthHeaders(); setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    suspend fun updateNews(id: String, data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                is Int     -> put(k, v)
                else       -> put(k, v.toString())
            }}
        }
        val response = client.patch("$BASE_URL/rest/v1/news") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteNews(id: String): Boolean {
        val response = client.delete("$BASE_URL/rest/v1/news") {
            addAuthHeaders(); parameter("id", "eq.$id")
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Updates CRUD ────────────────────────────────────────────────────────

    suspend fun insertUpdate(data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                else       -> put(k, v.toString())
            }}
        }
        val response = client.post("$BASE_URL/rest/v1/upcoming_updates") {
            addAuthHeaders(); setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    suspend fun updateUpdate(id: String, data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            data.forEach { (k, v) -> when (v) {
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                else       -> put(k, v.toString())
            }}
        }
        val response = client.patch("$BASE_URL/rest/v1/upcoming_updates") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteUpdate(id: String): Boolean {
        val response = client.delete("$BASE_URL/rest/v1/upcoming_updates") {
            addAuthHeaders(); parameter("id", "eq.$id")
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Contact ─────────────────────────────────────────────────────────────

    suspend fun upsertContactInfo(data: Map<String, Any>): Boolean {
        val existing = getContactInfo()
        return if (existing != null) {
            val id = existing["id"]?.jsonPrimitive?.content ?: return false
            val json = buildJsonObject { data.forEach { (k, v) -> put(k, v.toString()) } }
            val response = client.patch("$BASE_URL/rest/v1/contact_info") {
                addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
            }
            response.status == HttpStatusCode.OK
        } else {
            val json = buildJsonObject { data.forEach { (k, v) -> put(k, v.toString()) } }
            val response = client.post("$BASE_URL/rest/v1/contact_info") {
                addAuthHeaders(); setBody(json.toString())
            }
            response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
        }
    }

    // ─── Broadcasts ──────────────────────────────────────────────────────────

    suspend fun insertBroadcast(data: Map<String, String>): Boolean {
        val response = client.post("$BASE_URL/rest/v1/broadcasts") {
            addAuthHeaders(); setBody(data)
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    suspend fun deleteBroadcast(id: String): Boolean {
        val response = client.delete("$BASE_URL/rest/v1/broadcasts") {
            addAuthHeaders(); parameter("id", "eq.$id")
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Store Config ─────────────────────────────────────────────────────────

    suspend fun updateStoreConfig(id: String = "1", data: Map<String, String>): Boolean {
        val response = client.patch("$BASE_URL/rest/v1/store_config") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(data)
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Site Downloads ───────────────────────────────────────────────────────

    suspend fun getSiteDownloads(): List<JsonObject> {
        val response = client.get("$BASE_URL/rest/v1/site_downloads") {
            addAuthHeaders(); parameter("order", "code")
        }
        return if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    }

    suspend fun updateSiteDownload(id: String, data: Map<String, String>): Boolean {
        val json = buildJsonObject { data.forEach { (k, v) -> put(k, v) } }
        val response = client.patch("$BASE_URL/rest/v1/site_downloads") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteSiteDownload(id: String): Boolean {
        val response = client.delete("$BASE_URL/rest/v1/site_downloads") {
            addAuthHeaders(); parameter("id", "eq.$id")
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun insertSiteDownload(data: Map<String, String>): Boolean {
        val json = buildJsonObject { data.forEach { (k, v) -> put(k, v) } }
        val response = client.post("$BASE_URL/rest/v1/site_downloads") {
            addAuthHeaders(); setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    // ─── FCM Token ────────────────────────────────────────────────────────────

    /**
     * Upsert device FCM token for push notifications.
     * Table: device_tokens (user_id TEXT PK, token TEXT, updated_at TIMESTAMPTZ)
     */
    suspend fun upsertFcmToken(userId: String, token: String, deviceModel: String = ""): Boolean {
        val json = buildJsonObject {
            put("user_id",      userId)
            put("token",        token)
            put("device_model", deviceModel)
            put("updated_at",   "now()")
        }
        val response = client.post("$BASE_URL/rest/v1/device_tokens") {
            header("apikey", ANON_KEY)
            header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
            header("Content-Type", "application/json")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK)
    }

    /**
     * Send a push notification via FCM HTTP v1 API through a Supabase Edge Function.
     * The edge function handles the OAuth2 token exchange so the FCM server key never
     * lives in the app.
     *
     * Expected edge function name: "send-broadcast-notification"
     * Payload: { title: string, message: string }
     */
    suspend fun sendFcmBroadcast(title: String, message: String): Boolean {
        return try {
            val json = buildJsonObject {
                put("title",   title)
                put("message", message)
            }
            val response = client.post("$BASE_URL/functions/v1/send-broadcast-notification") {
                header("apikey", ANON_KEY)
                header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                header("Content-Type", "application/json")
                setBody(json.toString())
            }
            response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Created,
                                       HttpStatusCode.NoContent)
        } catch (_: Exception) { false }
    }

    // ─── Website Products Sync ─────────────────────────────────────────────────

    /**
     * Upserts a row into the website's `products` table using the SAME id as the
     * source `apps` row, so there's a clean 1:1 link with no separate mapping table.
     * Relies on the `on_conflict=id` upsert so repeated saves just update in place.
     */
    suspend fun upsertWebsiteProduct(id: String, data: Map<String, Any>): Boolean {
        val json = buildJsonObject {
            put("id", id)
            data.forEach { (k, v) -> when (v) {
                is JsonElement -> put(k, v)
                is String  -> put(k, v)
                is Boolean -> put(k, v)
                is Int     -> put(k, v)
                is Number  -> put(k, v.toString())
                else       -> put(k, v.toString())
            }}
        }
        val response = client.post("$BASE_URL/rest/v1/products") {
            header("apikey", ANON_KEY)
            header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
            header("Content-Type", "application/json")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            parameter("on_conflict", "id")
            setBody(json.toString())
        }
        return response.status in listOf(HttpStatusCode.Created, HttpStatusCode.OK, HttpStatusCode.NoContent)
    }

    /** Hides a product from the website without deleting it, if a row exists for this id. */
    suspend fun archiveWebsiteProductIfExists(id: String): Boolean {
        val json = buildJsonObject { put("status", "archived") }
        val response = client.patch("$BASE_URL/rest/v1/products") {
            addAuthHeaders(); parameter("id", "eq.$id"); setBody(json.toString())
        }
        return response.status == HttpStatusCode.OK
    }

    // ─── Admin Stats ──────────────────────────────────────────────────────────

    suspend fun getStats(): Map<String, Int> {
        val appsResp     = client.get("$BASE_URL/rest/v1/apps") {
            addAuthHeaders(); parameter("select", "id,is_published,download_count")
        }
        val newsResp     = client.get("$BASE_URL/rest/v1/news") {
            addAuthHeaders(); parameter("select", "id,is_published")
        }
        val updatesResp  = client.get("$BASE_URL/rest/v1/upcoming_updates") {
            addAuthHeaders(); parameter("select", "id,status")
        }
        val profilesResp = client.get("$BASE_URL/rest/v1/profiles") {
            addAuthHeaders(); parameter("select", "id")
        }
        val apps     = if (appsResp.status     == HttpStatusCode.OK) appsResp.body<List<JsonObject>>()     else emptyList()
        val news     = if (newsResp.status     == HttpStatusCode.OK) newsResp.body<List<JsonObject>>()     else emptyList()
        val updates  = if (updatesResp.status  == HttpStatusCode.OK) updatesResp.body<List<JsonObject>>()  else emptyList()
        val profiles = if (profilesResp.status == HttpStatusCode.OK) profilesResp.body<List<JsonObject>>() else emptyList()
        return mapOf(
            "total_apps"       to apps.size,
            "published_apps"   to apps.count  { it["is_published"]?.jsonPrimitive?.boolean == true },
            "total_news"       to news.size,
            "published_news"   to news.count  { it["is_published"]?.jsonPrimitive?.boolean == true },
            "total_updates"    to updates.size,
            "in_progress"      to updates.count { it["status"]?.jsonPrimitive?.content == "in_progress" },
            "total_users"      to profiles.size,
            "total_downloads"  to apps.sumOf { it["download_count"]?.jsonPrimitive?.int ?: 0 }
        )
    }
}
