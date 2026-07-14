package com.example.interstellarcalc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NasaImageRepository {

    private const val BASE_URL = "https://images-api.nasa.gov/search"

    suspend fun fetchImageUrl(objectName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = "$BASE_URL?q=${URLEncoder.encode(objectName, "UTF-8")}&media_type=image&page_size=1"
                val conn   = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout    = 10000
                conn.requestMethod  = "GET"
                conn.setRequestProperty("User-Agent", "InterstellarCalc/1.0")
                try {
                    if (conn.responseCode != 200) return@withContext null
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    extractFirstHref(body)
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) { null }
        }
    }

    suspend fun resolveManifestUrl(manifestUrl: String): String? {
        if (manifestUrl.endsWith(".jpg") || manifestUrl.endsWith(".png")) return manifestUrl
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(manifestUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout    = 10000
                conn.requestMethod  = "GET"
                conn.setRequestProperty("User-Agent", "InterstellarCalc/1.0")
                try {
                    if (conn.responseCode != 200) return@withContext null
                    val body  = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    val lines = body.trim('[', ']', '"').split("\",\"")
                    lines.firstOrNull { it.endsWith(".jpg") || it.endsWith(".png") }
                        ?.replace("\\", "")
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) { null }
        }
    }

    private fun extractFirstHref(json: String): String? {
        val idx   = json.indexOf("\"href\"")
        if (idx < 0) return null
        val start = json.indexOf('"', idx + 7)
        if (start < 0) return null
        val end   = json.indexOf('"', start + 1)
        if (end < 0) return null
        return json.substring(start + 1, end)
    }
}
