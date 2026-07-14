package com.example.slacklineadminapp.data

import com.google.gson.annotations.SerializedName
import java.io.File

data class CloudPreset(
    val name: String = "",
    val type: String = "github_path",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val path: String = "",
    val token: String = "",
    val alias: String = "",
    val username: String = "",
    @SerializedName("project_url")           val projectUrl: String          = "",
    @SerializedName("project_ref")           val projectRef: String          = "",
    @SerializedName("anon_key")              val anonKey: String             = "",
    @SerializedName("personal_access_token") val personalAccessToken: String = "",
    @SerializedName("project_admin_key")     val projectAdminKey: String     = "",
    val email: String = "",
    val password: String = ""
)

object CloudPresetsStore {
    private data class PresetsFile(val presets: List<CloudPreset> = emptyList())
    private fun file() = AppStorage.cloudSettingsFile()

    fun loadAll(): List<CloudPreset> = AppStorage.loadJson(file(), PresetsFile()).presets
    fun save(list: List<CloudPreset>) = AppStorage.saveJson(file(), PresetsFile(list))
    fun add(p: CloudPreset) = save(loadAll().filter { it.name != p.name } + p)
    fun delete(name: String) = save(loadAll().filter { it.name != name })
}
