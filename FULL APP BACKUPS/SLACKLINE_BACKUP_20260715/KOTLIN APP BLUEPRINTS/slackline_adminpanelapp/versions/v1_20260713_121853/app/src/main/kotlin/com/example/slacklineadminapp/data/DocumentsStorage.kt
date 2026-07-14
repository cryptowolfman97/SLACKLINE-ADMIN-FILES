package com.example.slacklineadminapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class DocumentFile(
    val title: String   = "",
    val file: String    = "",
    val content: String = "",
    val updated: String = ""
)

data class DocumentFolder(
    val name: String                           = "",
    val updated: String                        = "",
    val files: MutableMap<String, DocumentFile> = mutableMapOf()
)

object DocumentsStore {
    private val gson = Gson()
    private fun file() = AppStorage.documentsFile()
    fun exportDir(): File = AppStorage.documentsExportDir()

    fun loadAll(): MutableMap<String, DocumentFolder> = try {
        val type = object : TypeToken<MutableMap<String, DocumentFolder>>() {}.type
        gson.fromJson<MutableMap<String, DocumentFolder>>(file().readText(), type) ?: mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }

    fun saveAll(data: Map<String, DocumentFolder>) { file().writeText(gson.toJson(data)) }

    fun utcNow(): String = WebsitesStore.utcNow()
    fun slugify(name: String): String = WebsitesStore.slugify(name)
}
