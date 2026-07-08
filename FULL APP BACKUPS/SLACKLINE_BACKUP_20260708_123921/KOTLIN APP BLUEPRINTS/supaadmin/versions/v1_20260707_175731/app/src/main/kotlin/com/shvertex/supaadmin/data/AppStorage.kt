package com.shvertex.supaadmin.data

import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppStorage {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun dataDir(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SupaAdmin"
        ).also { it.mkdirs() }

    inline fun <reified T> loadJson(file: File, default: T): T = try {
        if (!file.exists()) default
        else gson.fromJson(file.readText(Charsets.UTF_8), object : TypeToken<T>() {}.type) ?: default
    } catch (e: Exception) { default }

    fun saveJson(file: File, data: Any) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(data), Charsets.UTF_8)
    }

    fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
}
