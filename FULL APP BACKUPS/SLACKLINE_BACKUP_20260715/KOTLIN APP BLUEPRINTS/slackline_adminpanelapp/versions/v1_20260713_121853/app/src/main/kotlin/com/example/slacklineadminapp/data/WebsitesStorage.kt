package com.example.slacklineadminapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ── Models ────────────────────────────────────────────────────────────────────

data class WebsitePage(
    val title: String   = "",
    val file: String    = "",
    val html: String    = "",
    val updated: String = ""
)

data class WebsiteEntry(
    val name: String                      = "",
    val domain: String                    = "",
    val repo: String                      = "",
    val updated: String                   = "",
    val pages: MutableMap<String, WebsitePage> = mutableMapOf()
)

// ── Storage ───────────────────────────────────────────────────────────────────

object WebsitesStore {
    private val gson = Gson()
    private fun file() = AppStorage.websitesFile()
    fun exportDir(): File = AppStorage.websitesExportDir()

    fun loadAll(): MutableMap<String, WebsiteEntry> = try {
        val type = object : TypeToken<MutableMap<String, WebsiteEntry>>() {}.type
        gson.fromJson<MutableMap<String, WebsiteEntry>>(file().readText(), type) ?: mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }

    fun saveAll(data: Map<String, WebsiteEntry>) { file().writeText(gson.toJson(data)) }

    fun utcNow(): String = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

    fun slugify(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}