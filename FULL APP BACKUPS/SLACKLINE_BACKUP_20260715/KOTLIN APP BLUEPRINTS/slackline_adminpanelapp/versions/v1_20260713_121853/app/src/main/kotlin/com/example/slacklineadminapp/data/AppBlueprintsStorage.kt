package com.example.slacklineadminapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class AppBlueprint(
    val name: String                       = "",
    val version: String                    = "1.0.0",
    val description: String                = "",
    val created_at: String                 = "",
    val updated_at: String                 = "",
    val files: MutableMap<String, String>  = mutableMapOf()
)

object AppBlueprintsStore {
    private val gson = Gson()
    private fun file() = AppStorage.appBlueprintsFile()
    fun exportDir(): File = AppStorage.appBlueprintsExportDir()

    fun loadAll(): MutableMap<String, AppBlueprint> = try {
        val type = object : TypeToken<MutableMap<String, AppBlueprint>>() {}.type
        gson.fromJson<MutableMap<String, AppBlueprint>>(file().readText(), type) ?: mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }

    fun saveAll(data: Map<String, AppBlueprint>) { file().writeText(gson.toJson(data)) }

    fun utcNow(): String = WebsitesStore.utcNow()
    fun slugify(name: String): String = WebsitesStore.slugify(name)

    fun humanBytes(bytes: Long): String = when {
        bytes < 1024          -> "${bytes} B"
        bytes < 1024 * 1024   -> "${"%.1f".format(bytes / 1024.0)} KB"
        else                  -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    }

    /** List items visible at a given path, dirs first */
    fun listPath(files: Map<String, String>, path: String): List<Pair<String, String>> {
        val prefix = if (path.isNotBlank()) "$path/" else ""
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Pair<String, String>>()
        for (fullPath in files.keys) {
            if (!fullPath.startsWith(prefix) || fullPath == path) continue
            val relative = fullPath.removePrefix(prefix)
            val parts = relative.split("/", limit = 2)
            val name = parts[0]
            if (name in seen) continue
            seen.add(name)
            val type = if (parts.size == 2) "dir" else "file"
            result.add(type to name)
        }
        return result.sortedWith(compareBy({ it.first != "dir" }, { it.second }))
    }
}
