package com.snaploader.app.util

import android.content.Context
import android.util.Log
import com.snaploader.app.model.MediaType
import com.snaploader.app.model.QualityOption
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

object YtDlpHelper {

    private const val TAG              = "YtDlpHelper"
    private const val FETCH_TIMEOUT_MS = 60_000L   // 60s for info fetch
    private const val DL_TIMEOUT_MS    = 360_000L  // 6 min per download attempt

    @Volatile private var initialized = false

    suspend fun init(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            FFmpeg.getInstance().init(context)
            Log.d(TAG, "ffmpeg init done")
            YoutubeDL.getInstance().init(context)
            Log.d(TAG, "yt-dlp base init done, updating…")
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(
                    context, YoutubeDL.UpdateChannel.STABLE
                )
                Log.d(TAG, "yt-dlp update status: $status")
            } catch (e: Exception) {
                Log.w(TAG, "yt-dlp update failed (using bundled): ${e.message}")
            }
            initialized = true
            Log.d(TAG, "yt-dlp + ffmpeg initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init yt-dlp: ${e.message}", e)
            false
        }
    }

    fun isInitialized(): Boolean = initialized

    /** Safe init for share context — no network update, just loads binaries. */
    suspend fun initWithoutUpdate(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        return@withContext try {
            FFmpeg.getInstance().init(context)
            YoutubeDL.getInstance().init(context)
            initialized = true
            Log.d(TAG, "yt-dlp initialized (no update)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "initWithoutUpdate failed: ${e.message}", e)
            false
        }
    }

    // ── Fetch JSON info ───────────────────────────────────────────────────────
    suspend fun fetchInfo(url: String): String? = withContext(Dispatchers.IO) {
        if (!initialized) { Log.e(TAG, "fetchInfo called before init"); return@withContext null }
        withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            try {
                val request = YoutubeDLRequest(url).apply {
                    addOption("--no-playlist")
                    addOption("--no-check-certificate")
                    addOption("--socket-timeout", "20")
                    addOption("--extractor-args", "youtube:player_client=android_vr")
                    addOption("-J")
                }
                val response = YoutubeDL.getInstance().execute(request)
                val output   = response.out.trim()
                Log.d(TAG, "fetchInfo (first 500): ${output.take(500)}")
                if (output.startsWith("{")) output else null
            } catch (e: Exception) {
                Log.e(TAG, "fetchInfo exception: ${e.message}", e)
                null
            }
        }
    }

    // ── Download with retry + fallback ────────────────────────────────────────
    /**
     * Tries the requested formatId first.
     * On failure retries with a height-capped fallback: best[height<=N][ext=mp4]
     * On second failure retries with just "best" as last resort.
     *
     * @return Pair(success, actualFilePath)
     */
    suspend fun download(
        url              : String,
        formatId         : String,
        destPath         : String,
        subtitleLang     : String  = "",
        progressCallback : (Int, Long, Long) -> Unit   // progress%, speedBps, etaSeconds
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {

        if (!initialized) { Log.e(TAG, "download before init"); return@withContext Pair(false, "") }

        val heightFromFormat = Regex("(\\d{3,4})p").find(formatId)?.groupValues?.get(1)?.toIntOrNull()

        val formatsToTry = buildList {
            add(formatId)
            if (heightFromFormat != null) {
                add("bestvideo[height<=$heightFromFormat][ext=mp4]+bestaudio[ext=m4a]/best[height<=$heightFromFormat][ext=mp4]/best[height<=$heightFromFormat]")
            }
            add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
        }.distinct()

        var lastFilePath = ""

        for ((attemptIndex, fmt) in formatsToTry.withIndex()) {
            Log.d(TAG, "download attempt ${attemptIndex + 1}/${formatsToTry.size} fmt=$fmt")

            val result = withTimeoutOrNull(DL_TIMEOUT_MS) {
                tryDownload(url, fmt, destPath, subtitleLang, progressCallback)
            }

            if (result != null && result.first) {
                Log.d(TAG, "download succeeded on attempt ${attemptIndex + 1}, file=${result.second}")
                return@withContext result
            }
            if (result != null) lastFilePath = result.second
            Log.w(TAG, "download attempt ${attemptIndex + 1} failed, trying next fallback")
        }

        Log.e(TAG, "All download attempts exhausted for $url")
        Pair(false, lastFilePath)
    }

    private suspend fun tryDownload(
        url              : String,
        format           : String,
        destPath         : String,
        subtitleLang     : String,
        progressCallback : (Int, Long, Long) -> Unit
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var capturedFile = ""
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--no-check-certificate")
                addOption("--socket-timeout", "20")
                addOption("--extractor-args", "youtube:player_client=android_vr")
                addOption("-f", format)
                addOption("--merge-output-format", "mp4")
                addOption("-o", "$destPath/%(title)s.%(ext)s")
                // Print the actual output filename so we can capture it
                addOption("--print", "after_move:filepath")
                if (subtitleLang.isNotEmpty()) {
                    addOption("--write-sub")
                    addOption("--sub-langs", subtitleLang)
                    addOption("--convert-subs", "srt")
                }
            }
            YoutubeDL.getInstance().execute(request) { progress, etaSeconds, _ ->
                // yt-dlp doesn't report speed directly; we pass 0 for speed
                progressCallback(progress.toInt().coerceIn(0, 100), 0L, etaSeconds.toLong())
                // Capture the filepath from the last line of output
            }.also { response ->
                // The --print after_move:filepath writes the path to stdout
                val lines = response.out.trim().lines()
                val fileLine = lines.lastOrNull { it.contains("/") && !it.startsWith("[") }
                if (fileLine != null) capturedFile = fileLine.trim()
                Log.d(TAG, "tryDownload stdout last lines: ${lines.takeLast(3)}")
            }
            Log.d(TAG, "tryDownload finished: file=$capturedFile")
            Pair(true, capturedFile)
        } catch (e: Exception) {
            Log.e(TAG, "tryDownload exception (format=$format): ${e.message}", e)
            Pair(false, capturedFile)
        }
    }

    // ── Parse qualities from JSON ─────────────────────────────────────────────
    fun parseQualities(json: String): Triple<String, String, List<QualityOption>> {
        val root      = JSONObject(json)
        val title     = root.optString("title", "Unknown title")
        val videoId   = root.optString("id", "")
        val webpage   = root.optString("webpage_url", "")

        // Derive thumbnail URL from YouTube video ID if available
        val thumbnail = root.optString("thumbnail", "").ifEmpty {
            if (videoId.isNotEmpty()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
        }

        val formats   = root.optJSONArray("formats") ?: JSONArray()
        val seen      = mutableSetOf<String>()
        val rawVideo  = mutableListOf<QualityOption>()
        val rawAudio  = mutableListOf<QualityOption>()

        Log.d(TAG, "parseQualities: total formats = ${formats.length()}")

        for (i in 0 until formats.length()) {
            val f      = formats.getJSONObject(i)
            val vcodec = f.optString("vcodec", "none")
            val acodec = f.optString("acodec", "none")
            val ext    = f.optString("ext", "mp4")
            val height = f.optInt("height", 0)
            val fid    = f.optString("format_id", "")
            val fsize  = f.optLong("filesize", 0L)
            val note   = f.optString("format_note", "")

            if (ext == "mhtml" || note == "storyboard") continue

            if (vcodec != "none" && height > 0) {
                // Prefer MP4 — de-duplicate by resolution, keep MP4 over WEBM
                val resKey = "${height}p"
                val label  = "$resKey ${ext.uppercase()}"
                if (ext.equals("mp4", ignoreCase = true)) {
                    // Always add/replace with mp4 entry at this resolution
                    val mp4Key = "${height}p_mp4"
                    if (seen.add(mp4Key)) {
                        rawVideo.add(QualityOption(
                            label        = "$resKey MP4",
                            quality      = resKey,
                            format       = "mp4",
                            mediaType    = MediaType.VIDEO,
                            sizeEstimate = if (fsize > 0) "~${fsize / (1024 * 1024)} MB" else "",
                            formatId     = fid
                        ))
                        Log.d(TAG, "Adding MP4 video: $resKey fid=$fid")
                    }
                } else if (!seen.contains("${height}p_mp4") && seen.add(label)) {
                    // Only add WEBM if there's no MP4 at this resolution yet
                    rawVideo.add(QualityOption(
                        label        = label,
                        quality      = resKey,
                        format       = ext,
                        mediaType    = MediaType.VIDEO,
                        sizeEstimate = if (fsize > 0) "~${fsize / (1024 * 1024)} MB" else "",
                        formatId     = fid
                    ))
                    Log.d(TAG, "Adding non-MP4 video: $label fid=$fid")
                }
            }

            if (vcodec == "none" && acodec != "none") {
                val abr   = f.optInt("abr", 0)
                val label = if (abr > 0) "${ext.uppercase()} ${abr}kbps" else "${ext.uppercase()} Audio"
                if (seen.add(label)) {
                    rawAudio.add(QualityOption(
                        label        = label,
                        quality      = "audio",
                        format       = ext,
                        mediaType    = MediaType.AUDIO,
                        sizeEstimate = if (fsize > 0) "~${fsize / (1024 * 1024)} MB" else "",
                        formatId     = fid
                    ))
                }
            }
        }

        val sortedVideo = rawVideo.sortedByDescending {
            it.quality.replace("p", "").toIntOrNull() ?: 0
        }
        val sortedAudio = rawAudio.sortedByDescending {
            it.label.filter(Char::isDigit).toIntOrNull() ?: 0
        }

        val qualities = sortedVideo + sortedAudio
        Log.d(TAG, "parseQualities: ${qualities.size} total (${sortedVideo.size} video, ${sortedAudio.size} audio)")
        return Triple(title, thumbnail, qualities)
    }

    /** Check if subtitles are available in the JSON. */
    fun hasSubtitles(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            root.optJSONObject("subtitles")?.length()?.let { it > 0 } == true ||
            root.optJSONObject("automatic_captions")?.length()?.let { it > 0 } == true
        } catch (_: Exception) { false }
    }
}
