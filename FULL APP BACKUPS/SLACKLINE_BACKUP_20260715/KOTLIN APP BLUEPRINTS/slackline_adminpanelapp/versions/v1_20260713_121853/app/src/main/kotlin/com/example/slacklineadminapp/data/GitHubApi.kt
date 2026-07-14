package com.example.slacklineadminapp.data

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Stateless GitHub REST API v3 client.
 * Every call is synchronous — callers must dispatch to a background thread.
 */
class GitHubApi(
    private val username: String,
    private val token: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ── Headers ───────────────────────────────────────────────────────────

    private fun headers() = mapOf(
        "Authorization"        to "token $token",
        "Accept"               to "application/vnd.github.v3+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "User-Agent"           to "SHV-Admin"
    )

    // ── Low-level HTTP ────────────────────────────────────────────────────

    private fun get(url: String): String {
        val req = Request.Builder().url(url)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw ApiException(resp.code, errorMessage(body))
        return body
    }

    private fun put(url: String, json: String): String {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .put(body).build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw ApiException(resp.code, errorMessage(respBody))
        return respBody
    }

    // ─── ADDED: LOW LEVEL PATCH UTILITY METHOD FOR INTEGRATED REVISIONS ────────
    private fun patch(url: String, json: String): String {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .patch(body).build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw ApiException(resp.code, errorMessage(respBody))
        return respBody
    }

    private fun delete(url: String, json: String): String {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .delete(body).build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (resp.code == 204) return ""
        if (!resp.isSuccessful) throw ApiException(resp.code, errorMessage(respBody))
        return respBody
    }

    private fun post(url: String, json: String): String {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .post(body).build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw ApiException(resp.code, errorMessage(respBody))
        return respBody
    }

    private fun errorMessage(body: String) = try {
        JsonParser.parseString(body).asJsonObject
            .get("message")?.asString ?: body.take(200)
    } catch (e: Exception) { body.take(200) }

    // ── Repository API ────────────────────────────────────────────────────

    fun listRepos(perPage: Int = 50): List<GitHubRepo> {
        val body = get("https://api.github.com/user/repos?sort=updated&per_page=$perPage")
        return gson.fromJson(body, Array<GitHubRepo>::class.java).toList()
    }

    fun createRepo(name: String, description: String, private: Boolean): GitHubRepo {
        val payload = gson.toJson(mapOf(
            "name"        to name,
            "description" to description,
            "private"     to private,
            "auto_init"   to true
        ))
        val body = post("https://api.github.com/user/repos", payload)
        return gson.fromJson(body, GitHubRepo::class.java)
    }

    fun deleteRepo(owner: String, repo: String) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo")
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .delete().build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful && resp.code != 204)
            throw ApiException(resp.code, "Delete repo failed: ${resp.code}")
    }

    // ── Contents API ──────────────────────────────────────────────────────

    fun listContents(owner: String, repo: String, path: String, branch: String): List<GitHubItem> {
        val encodedPath = path.trimStart('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$encodedPath?ref=$branch"
        val body = get(url)
        return if (body.trimStart().startsWith("[")) {
            gson.fromJson(body, Array<GitHubItem>::class.java).toList()
        } else {
            listOf(gson.fromJson(body, GitHubItem::class.java))
        }
    }

    fun getFileContent(owner: String, repo: String, path: String, branch: String): Pair<String, String> {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
        val body = get(url)
        val obj = JsonParser.parseString(body).asJsonObject
        val sha = obj.get("sha").asString
        val encoded = obj.get("content").asString.replace("\n", "")
        val decoded = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        return sha to decoded
    }

    /** Binary-safe variant of [getFileContent] — used by backup/restore so images, keys, zips etc. round-trip exactly. */
    fun getFileBytes(owner: String, repo: String, path: String, branch: String): Pair<String, ByteArray> {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
        val body = get(url)
        val obj = JsonParser.parseString(body).asJsonObject
        val sha = obj.get("sha").asString
        val encoded = obj.get("content").asString.replace("\n", "")
        return sha to Base64.decode(encoded, Base64.DEFAULT)
    }

    fun putFile(
        owner: String, repo: String, path: String,
        content: String, message: String, branch: String, sha: String?
    ): String {
        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val payload = buildMap<String, Any> {
            put("message", message)
            put("content", encoded)
            put("branch", branch)
            if (sha != null) put("sha", sha)
        }
        return put(
            "https://api.github.com/repos/$owner/$repo/contents/$path",
            gson.toJson(payload)
        )
    }

    fun putBinaryFile(
        owner: String, repo: String, path: String,
        bytes: ByteArray, message: String, branch: String, sha: String?
    ): String {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val payload = buildMap<String, Any> {
            put("message", message)
            put("content", encoded)
            put("branch", branch)
            if (sha != null) put("sha", sha)
        }
        return put(
            "https://api.github.com/repos/$owner/$repo/contents/$path",
            gson.toJson(payload)
        )
    }

    fun deleteFile(owner: String, repo: String, path: String, sha: String, message: String, branch: String) {
        val payload = gson.toJson(mapOf("message" to message, "sha" to sha, "branch" to branch))
        delete("https://api.github.com/repos/$owner/$repo/contents/$path", payload)
    }

    fun getRepo(owner: String, repo: String): GitHubRepo? = try {
        val body = get("https://api.github.com/repos/$owner/$repo")
        gson.fromJson(body, GitHubRepo::class.java)
    } catch (e: ApiException) {
        if (e.code == 404) null else throw e
    }

    fun validateToken(): String {
        val body = get("https://api.github.com/user")
        return JsonParser.parseString(body).asJsonObject.get("login").asString
    }

    fun listBranches(owner: String, repo: String): List<String> {
        val body = get("https://api.github.com/repos/$owner/$repo/branches")
        return gson.fromJson(body, Array<JsonObject>::class.java)
            .map { it.get("name").asString }
    }

    fun searchRepos(query: String): List<GitHubRepo> {
        val url = "https://api.github.com/search/repositories?q=${
            java.net.URLEncoder.encode("$query user:$username", "UTF-8")
        }&sort=updated"
        val body = get(url)
        val obj = JsonParser.parseString(body).asJsonObject
        return gson.fromJson(obj.getAsJsonArray("items"), Array<GitHubRepo>::class.java).toList()
    }

    // ── Releases Platform API Integration Layer ───────────────────────────

    fun listReleases(owner: String, repo: String): List<GitHubRelease> {
        val body = get("https://api.github.com/repos/$owner/$repo/releases")
        return gson.fromJson(body, Array<GitHubRelease>::class.java).toList()
    }

    // ─── ADDED: FETCH SINGLE RECOVERY TAG NODES ──────────────────────────────
    fun getSingleRelease(owner: String, repo: String, releaseId: Long): GitHubRelease {
        val body = get("https://api.github.com/repos/$owner/$repo/releases/$releaseId")
        return gson.fromJson(body, GitHubRelease::class.java)
    }

    fun createRelease(
        owner: String,
        repo: String,
        tagName: String,
        title: String,
        body: String,
        isPrerelease: Boolean,
        isLatest: Boolean
    ): GitHubRelease {
        val payload = gson.toJson(mapOf(
            "tag_name"         to tagName,
            "name"             to title,
            "body"             to body,
            "prerelease"       to isPrerelease,
            "make_latest"      to if (isLatest) "true" else "false"
        ))
        val responseJson = post("https://api.github.com/repos/$owner/$repo/releases", payload)
        return gson.fromJson(responseJson, GitHubRelease::class.java)
    }

    // ─── ADDED: PATCH MODIFY AN EXISTING RECORD REMOTELY ──────────────────────
    fun patchRelease(
        owner: String,
        repo: String,
        releaseId: Long,
        tagName: String,
        title: String,
        body: String,
        isPrerelease: Boolean,
        isLatest: Boolean
    ): GitHubRelease {
        val payload = gson.toJson(mapOf(
            "tag_name"    to tagName,
            "name"        to title,
            "body"        to body,
            "prerelease"  to isPrerelease,
            "make_latest" to if (isLatest) "true" else "false"
        ))
        val responseJson = patch("https://api.github.com/repos/$owner/$repo/releases/$releaseId", payload)
        return gson.fromJson(responseJson, GitHubRelease::class.java)
    }

    fun uploadReleaseAsset(owner: String, repo: String, releaseId: Long, file: File) {
        val fileBytes = file.readBytes()
        val body = fileBytes.toRequestBody("application/vnd.android.package-archive".toMediaType())
        val uploadUrl = "https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets?name=${file.name}"
        
        val req = Request.Builder().url(uploadUrl)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .post(body).build()
            
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw ApiException(resp.code, "Asset upload failed: " + errorMessage(respBody))
    }

    // ─── ADDED: PURGE SINGLE DETACHED ARTIFACTS IN-PLACE ─────────────────────
    fun deleteReleaseAsset(owner: String, repo: String, assetId: Long) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/assets/$assetId")
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .delete().build()
            
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful && resp.code != 204)
            throw ApiException(resp.code, "Failed to drop old package file.")
    }

    fun downloadReleaseAssetUrl(downloadUrl: String, destFile: File) {
        val req = Request.Builder().url(downloadUrl)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
            
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, "Failed to stream asset bytes.")
        
        resp.body?.byteStream()?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun deleteRelease(owner: String, repo: String, releaseId: Long) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/$releaseId")
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .delete().build()
            
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful && resp.code != 204)
            throw ApiException(resp.code, "Failed to remove release context.")
    }

    class ApiException(val code: Int, message: String) : Exception("HTTP $code: $message")
}

// ── Data models ───────────────────────────────────────────────────────────────

data class GitHubRepo(
    val id: Long                   = 0,
    val name: String               = "",
    val full_name: String          = "",
    val description: String?       = null,
    val private: Boolean           = true,
    val html_url: String           = "",
    val default_branch: String     = "main",
    val language: String?          = null,
    val stargazers_count: Int      = 0,
    val size: Int                  = 0,
    val updated_at: String         = "",
    val owner: GitHubOwner         = GitHubOwner()
)

data class GitHubOwner(val login: String = "")

data class GitHubItem(
    val name: String  = "",
    val path: String  = "",
    val sha: String   = "",
    val size: Long    = 0,
    val type: String  = "file",
    val url: String   = "",
    val html_url: String = "",
    val download_url: String? = null
) {
    val isDirectory: Boolean get() = type == "dir"
    val isFile: Boolean      get() = type == "file"
    val sizeFormatted: String get() = when {
        size < 1024              -> "${size} B"
        size < 1024 * 1024       -> "${"%.1f".format(size / 1024.0)} KB"
        else                     -> "${"%.1f".format(size / (1024.0 * 1024))} MB"
    }
}

data class GitHubReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val browser_download_url: String
) {
    val sizeFormatted: String get() = when {
        size < 1024              -> "${size} B"
        size < 1024 * 1024       -> "${"%.1f".format(size / 1024.0)} KB"
        else                     -> "${"%.1f".format(size / (1024.0 * 1024))} MB"
    }
}

data class GitHubRelease(
    val id: Long,
    @com.google.gson.annotations.SerializedName("tag_name") val tagName: String,
    val name: String,
    val body: String?,
    val prerelease: Boolean,
    val latest: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList()
)
