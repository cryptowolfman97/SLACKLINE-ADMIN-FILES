package com.example.omnicortex.data.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * AegisScoreStore
 *
 * Persists the Aegis security score and its breakdown to
 * /sdcard/Download/OmniCortex/aegis_score.json
 *
 * This survives app reinstalls and clears, and the user can
 * inspect or back it up manually.
 */
object AegisScoreStore {

    private const val DIR  = "OmniCortex"
    private const val FILE = "aegis_score.json"

    private fun file(): File {
        val dir = File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ),
            DIR
        )
        dir.mkdirs()
        return File(dir, FILE)
    }

    data class ScoreData(
        val score: Int,
        val label: String,
        val lastScanMs: Long,
        /** Map of check-name → passed (true/false) */
        val breakdown: Map<String, Boolean> = emptyMap()
    )

    suspend fun save(data: ScoreData) = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("score",      data.score)
            put("label",      data.label)
            put("lastScanMs", data.lastScanMs)
            val bd = JSONObject()
            data.breakdown.forEach { (k, v) -> bd.put(k, v) }
            put("breakdown", bd)
        }
        file().writeText(json.toString(2))
    }

    suspend fun load(): ScoreData? = withContext(Dispatchers.IO) {
        val f = file()
        if (!f.exists()) return@withContext null
        try {
            val json = JSONObject(f.readText())
            val bdJson = json.optJSONObject("breakdown")
            val breakdown = mutableMapOf<String, Boolean>()
            bdJson?.keys()?.forEach { k -> breakdown[k] = bdJson.getBoolean(k) }
            ScoreData(
                score      = json.getInt("score"),
                label      = json.getString("label"),
                lastScanMs = json.getLong("lastScanMs"),
                breakdown  = breakdown
            )
        } catch (_: Exception) { null }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file().delete()
    }

    fun lastScanFormatted(lastScanMs: Long): String {
        if (lastScanMs == 0L) return "Never"
        val diff = System.currentTimeMillis() - lastScanMs
        val mins  = diff / 60_000
        val hours = diff / 3_600_000
        val days  = diff / 86_400_000
        return when {
            mins  < 1   -> "Just now"
            mins  < 60  -> "${mins}m ago"
            hours < 24  -> "${hours}h ago"
            else        -> "${days}d ago"
        }
    }
}
