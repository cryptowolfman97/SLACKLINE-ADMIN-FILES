package com.snaploader.app.util

import android.util.Log
import com.snaploader.app.model.MediaType
import com.snaploader.app.model.Platform
import com.snaploader.app.model.QualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fallback parser that fetches real download URLs via public APIs.
 *
 * TikTok  → (1) SSSTik API — server-side proxied, returns original high-quality CDN URL
 *            (2) tikwm.com API — re-encoded but reliable SD/audio safety net
 * Twitter → fxtwitter API (public, no auth needed)
 * YouTube → stubs only (yt-dlp is the only reliable method)
 * Instagram/Facebook → stubs (login-gated; yt-dlp handles these better)
 */
object DirectParser {

    private const val TAG        = "DirectParser"
    private const val UA         = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Safari/537.36"
    private const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 15_000

    suspend fun fetchQualities(url: String, platform: Platform): Pair<String, List<QualityOption>> =
        withContext(Dispatchers.IO) {
            try {
                when (platform) {
                    Platform.YOUTUBE   -> youtubeStubs(url)
                    Platform.TIKTOK    -> tiktokReal(url)
                    Platform.INSTAGRAM -> instagramStubs(url)
                    Platform.FACEBOOK  -> facebookStubs(url)
                    Platform.TWITTER   -> twitterReal(url)
                    Platform.GENERAL   -> genericStubs(url)
                    Platform.UNKNOWN   -> genericStubs(url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchQualities failed for $platform: ${e.message}", e)
                Pair("Download failed — ${e.message}", emptyList())
            }
        }

    // ── TikTok ────────────────────────────────────────────────────────────────
    private suspend fun tiktokReal(url: String): Pair<String, List<QualityOption>> =
        withContext(Dispatchers.IO) {
            val qualities = mutableListOf<QualityOption>()
            var title     = "TikTok Video"

            // ── Step 0: Resolve short links ──────────────────────────────────
            val resolvedUrl = resolveShortLink(url)
            Log.d(TAG, "TikTok resolved URL: $resolvedUrl")

            // ── Source 1: SSSTik API ─────────────────────────────────────────
            try {
                val ssstikResult = fetchViaSsstik(resolvedUrl)
                if (ssstikResult != null) {
                    val (ssstikTitle, noWmUrl, wmUrl, audioUrl) = ssstikResult
                    if (title == "TikTok Video" && ssstikTitle.isNotEmpty()) {
                        title = ssstikTitle
                    }
                    if (noWmUrl.isNotEmpty()) {
                        qualities.add(QualityOption(
                            label        = "Original Quality ★ (Highest)",
                            quality      = "original",
                            format       = "mp4",
                            mediaType    = MediaType.VIDEO,
                            sizeEstimate = "",
                            directUrl    = noWmUrl
                        ))
                        Log.d(TAG, "SSSTik: got no-watermark original URL")
                    }
                    if (wmUrl.isNotEmpty() && wmUrl != noWmUrl) {
                        qualities.add(QualityOption(
                            label        = "Original — With Watermark",
                            quality      = "original_wm",
                            format       = "mp4",
                            mediaType    = MediaType.VIDEO,
                            sizeEstimate = "",
                            directUrl    = wmUrl
                        ))
                    }
                    if (audioUrl.isNotEmpty()) {
                        qualities.add(QualityOption(
                            label        = "Original Audio ★",
                            quality      = "original_audio",
                            format       = "mp3",
                            mediaType    = MediaType.AUDIO,
                            sizeEstimate = "",
                            directUrl    = audioUrl
                        ))
                    }
                } else {
                    Log.w(TAG, "SSSTik: returned null result")
                }
            } catch (e: Exception) {
                Log.w(TAG, "SSSTik failed: ${e.message}")
            }

            // ── Source 2: tikwm — reliable SD/audio safety net ───────────────
            try {
                val encodedUrl = URLEncoder.encode(resolvedUrl, "UTF-8")
                val apiUrl     = "https://www.tikwm.com/api/?url=$encodedUrl&hd=1"
                Log.d(TAG, "TikTok tikwm request: $apiUrl")

                val response = httpGet(apiUrl)
                val root     = JSONObject(response)
                val code     = root.optInt("code", -1)

                if (code == 0) {
                    val data = root.getJSONObject("data")
                    if (title == "TikTok Video") {
                        title = data.optString("title", "TikTok Video").ifEmpty { "TikTok Video" }
                    }
                    val hdPlay = data.optString("hdplay", "")
                    val play   = data.optString("play", "")
                    val wmPlay = data.optString("wmplay", "")
                    val music  = data.optString("music", "")
                    val sizeHd = data.optLong("hd_size", 0L)
                    val sizeSd = data.optLong("size", 0L)

                    if (hdPlay.isNotEmpty()) qualities.add(QualityOption(
                        label = "HD — No Watermark (re-encoded)", quality = "hd", format = "mp4",
                        mediaType = MediaType.VIDEO,
                        sizeEstimate = if (sizeHd > 0) "~${sizeHd / (1024 * 1024)} MB" else "",
                        directUrl = hdPlay))
                    if (play.isNotEmpty()) qualities.add(QualityOption(
                        label = "SD — No Watermark (re-encoded)", quality = "sd", format = "mp4",
                        mediaType = MediaType.VIDEO,
                        sizeEstimate = if (sizeSd > 0) "~${sizeSd / (1024 * 1024)} MB" else "",
                        directUrl = play))
                    if (wmPlay.isNotEmpty()) qualities.add(QualityOption(
                        label = "SD — With Watermark (re-encoded)", quality = "sd_wm", format = "mp4",
                        mediaType = MediaType.VIDEO, sizeEstimate = "", directUrl = wmPlay))
                    if (music.isNotEmpty() && qualities.none { it.mediaType == MediaType.AUDIO }) {
                        qualities.add(QualityOption(
                            label = "Audio Only (MP3)", quality = "audio", format = "mp3",
                            mediaType = MediaType.AUDIO, sizeEstimate = "", directUrl = music))
                    }
                    Log.d(TAG, "tikwm returned ${qualities.size} total options after merge")
                } else {
                    Log.w(TAG, "tikwm code=$code")
                }
            } catch (e: Exception) {
                Log.w(TAG, "tikwm failed: ${e.message}")
            }

            if (qualities.isEmpty()) {
                Log.e(TAG, "All TikTok sources failed for: $url")
                return@withContext Pair("TikTok fetch failed — all sources exhausted", emptyList())
            }

            Log.d(TAG, "TikTok final: ${qualities.size} quality options for: $title")
            Pair(title, qualities)
        }

    // ── SSSTik API implementation ─────────────────────────────────────────────
    private data class SsstikResult(
        val title   : String,
        val noWmUrl : String,
        val wmUrl   : String,
        val audioUrl: String
    )

    private fun fetchViaSsstik(tiktokUrl: String): SsstikResult? {
        // ── Step A: GET homepage to extract the `tt` token ───────────────────
        val homepageHtml = httpGetWithHeaders("https://ssstik.io/en", mapOf(
            "User-Agent"      to UA_DESKTOP,
            "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9"
        ))

        // DEBUG: log a larger chunk of the homepage so we can see the form structure
        // if token extraction fails. Look for "SSSTik homepage chunk" in logcat.
        val formChunkIdx = homepageHtml.indexOf("<form")
        if (formChunkIdx >= 0) {
            Log.d(TAG, "SSSTik homepage form chunk: ${homepageHtml.substring(formChunkIdx, minOf(formChunkIdx + 600, homepageHtml.length))}")
        } else {
            Log.d(TAG, "SSSTik homepage (first 600): ${homepageHtml.take(600)}")
        }

        // Try multiple patterns to find the `tt` token — SSSTik occasionally
        // reorders or rewrites their form attributes between deploys.
        val ttToken =
            // Pattern 1: id before name  →  id="tt" name="tt" value="XXX"
            Regex("""id="tt"[^>]*name="tt"[^>]*value="([^"]+)"""")
                .find(homepageHtml)?.groupValues?.get(1)
            // Pattern 2: name before id  →  name="tt" id="tt" value="XXX"
            ?: Regex("""name="tt"[^>]*id="tt"[^>]*value="([^"]+)"""")
                .find(homepageHtml)?.groupValues?.get(1)
            // Pattern 3: name before value, no id required
            ?: Regex("""name="tt"[^>]*value="([^"]+)"""")
                .find(homepageHtml)?.groupValues?.get(1)
            // Pattern 4: value before name
            ?: Regex("""value="([^"]+)"[^>]*name="tt"""")
                .find(homepageHtml)?.groupValues?.get(1)
            // Pattern 5: bare token= anywhere in the page (some SPAs inline it as JS)
            ?: Regex(""""tt"\s*:\s*"([a-zA-Z0-9_\-]+)"""")
                .find(homepageHtml)?.groupValues?.get(1)

        if (ttToken.isNullOrEmpty()) {
            Log.w(TAG, "SSSTik: could not extract tt token — homepage may have changed structure")
            Log.w(TAG, "SSSTik: homepage length=${homepageHtml.length}, contains 'tt'=${homepageHtml.contains("\"tt\"")}")
            return null
        }
        Log.d(TAG, "SSSTik: extracted tt token: ${ttToken.take(12)}...")

        // ── Step B: POST to get download links ───────────────────────────────
        val postBody = "id=${URLEncoder.encode(tiktokUrl, "UTF-8")}" +
                       "&locale=en" +
                       "&tt=${URLEncoder.encode(ttToken, "UTF-8")}"

        val responseHtml = httpPost(
            url     = "https://ssstik.io/abc?url=dl",
            body    = postBody,
            headers = mapOf(
                "User-Agent"       to UA_DESKTOP,
                "Referer"          to "https://ssstik.io/en",
                "Origin"           to "https://ssstik.io",
                "Content-Type"     to "application/x-www-form-urlencoded",
                "Accept"           to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language"  to "en-US,en;q=0.9",
                "X-Requested-With" to "XMLHttpRequest"
            )
        )
        // DEBUG: log full POST response so we can see what SSSTik returned
        Log.d(TAG, "SSSTik POST response (first 800): ${responseHtml.take(800)}")

        // ── Step C: Parse download URLs from response HTML ───────────────────
        val title = Regex("""<p\s+class="maintext"[^>]*>\s*(.*?)\s*</p>""", RegexOption.DOT_MATCHES_ALL)
            .find(responseHtml)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim()?.take(100)?.ifEmpty { "TikTok Video" }
            ?: "TikTok Video"

        // Collect all https anchor hrefs from the response
        val allLinks = Regex("""href="(https://[^"]+)"""")
            .findAll(responseHtml)
            .map { it.groupValues[1] }
            .filter { link ->
                link.contains("tiktok") || link.contains("tiktokcdn") ||
                link.contains("muscdn") || link.contains("cdn") ||
                // SSSTik sometimes uses their own CDN proxy domain
                link.contains("ssstik") || link.contains("tikcdn")
            }
            .distinct()
            .toList()

        Log.d(TAG, "SSSTik: found ${allLinks.size} CDN links: ${allLinks.take(3).map { it.take(60) }}")

        if (allLinks.isEmpty()) {
            // Log all hrefs found regardless so we can diagnose
            val allHrefs = Regex("""href="(https://[^"]+)"""")
                .findAll(responseHtml)
                .map { it.groupValues[1].take(80) }
                .toList()
            Log.w(TAG, "SSSTik: no CDN links — all hrefs in response: $allHrefs")
            return null
        }

        val videoLinks = allLinks.filter {
            !it.contains("music") && !it.contains(".mp3") && !it.contains(".m4a")
        }
        val audioLinks = allLinks.filter {
            it.contains("music") || it.contains(".mp3") || it.contains(".m4a")
        }

        val noWmUrl  = videoLinks.getOrElse(0) { "" }
        val wmUrl    = videoLinks.getOrElse(1) { "" }
        val audioUrl = audioLinks.getOrElse(0) { "" }

        Log.d(TAG, "SSSTik parsed — noWm: ${noWmUrl.take(80)}")
        return SsstikResult(title, noWmUrl, wmUrl, audioUrl)
    }

    // ── TikTok: Resolve short links ───────────────────────────────────────────
    private fun resolveShortLink(url: String): String {
        val isShort = url.contains("vt.tiktok.com") ||
                      url.contains("vm.tiktok.com") ||
                      url.contains("m.tiktok.com")
        if (!isShort) return url

        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod           = "GET"
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", UA)
                connectTimeout          = TIMEOUT_MS
                readTimeout             = TIMEOUT_MS
                connect()
            }
            val responseCode = conn.responseCode
            val location     = conn.getHeaderField("Location") ?: url
            conn.disconnect()
            Log.d(TAG, "Short link $url -> HTTP $responseCode -> $location")
            if ((responseCode in 301..303) && location.isNotEmpty() && location != url)
                resolveShortLink(location) else location
        } catch (e: Exception) {
            Log.w(TAG, "Short link resolution failed: ${e.message}")
            url
        }
    }

    // ── Twitter/X — real API via fxtwitter ───────────────────────────────────
    private suspend fun twitterReal(url: String): Pair<String, List<QualityOption>> =
        withContext(Dispatchers.IO) {
            val tweetId = Regex("""/status/(\d+)""").find(url)?.groupValues?.get(1)
            if (tweetId.isNullOrEmpty()) return@withContext Pair("Twitter/X Video", twitterStubs())

            val apiUrl   = "https://api.fxtwitter.com/status/$tweetId"
            val response = httpGet(apiUrl)
            val root     = JSONObject(response)
            val code     = root.optInt("code", -1)
            if (code != 200) return@withContext Pair("Twitter/X Video", twitterStubs())

            val tweet  = root.optJSONObject("tweet") ?: return@withContext Pair("Twitter/X Video", twitterStubs())
            val title  = tweet.optString("text", "Twitter/X Video").take(80)
            val media  = tweet.optJSONObject("media") ?: return@withContext Pair(title, twitterStubs())
            val videos = media.optJSONArray("videos") ?: return@withContext Pair(title, twitterStubs())
            if (videos.length() == 0) return@withContext Pair(title, twitterStubs())

            val qualities = mutableListOf<QualityOption>()
            val variants  = videos.getJSONObject(0).optJSONArray("variants")
                ?: return@withContext Pair(title, twitterStubs())

            for (i in 0 until variants.length()) {
                val v = variants.getJSONObject(i)
                val variantUrl  = v.optString("url", "")
                val bitrate     = v.optInt("bitrate", 0)
                val contentType = v.optString("content_type", "video/mp4")
                if (variantUrl.isEmpty() || contentType == "application/x-mpegURL") continue
                val resMatch = Regex("""(\d+)x(\d+)""").find(variantUrl)
                val height   = resMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                val label    = if (resMatch != null) "${height}p MP4" else "${bitrate / 1000}kbps MP4"
                qualities.add(QualityOption(
                    label = label, quality = if (height > 0) "${height}p" else "${bitrate / 1000}kbps",
                    format = "mp4", mediaType = MediaType.VIDEO, directUrl = variantUrl))
            }
            Pair(title, qualities.sortedByDescending { it.quality.replace("p","").toIntOrNull() ?: 0 })
        }

    // ── YouTube stubs ─────────────────────────────────────────────────────────
    private fun youtubeStubs(url: String): Pair<String, List<QualityOption>> {
        val id    = UrlDetector.extractVideoId(url, Platform.YOUTUBE)
        val title = if (id.isNotEmpty()) "YouTube Video ($id)" else "YouTube Video"
        return Pair(title, listOf(
            QualityOption("1080p MP4", "1080p", "mp4", MediaType.VIDEO, formatId = "137+140"),
            QualityOption("720p MP4",  "720p",  "mp4", MediaType.VIDEO, formatId = "22"),
            QualityOption("480p MP4",  "480p",  "mp4", MediaType.VIDEO, formatId = "135+140"),
            QualityOption("360p MP4",  "360p",  "mp4", MediaType.VIDEO, formatId = "18"),
            QualityOption("Audio Only (M4A 128kbps)", "audio", "m4a", MediaType.AUDIO, formatId = "140"),
        ))
    }

    // ── Instagram stubs ───────────────────────────────────────────────────────
    private fun instagramStubs(url: String): Pair<String, List<QualityOption>> {
        val isReel = url.contains("/reel/")
        return Pair(if (isReel) "Instagram Reel" else "Instagram Post", listOf(
            QualityOption("Best Quality",          "best",  "mp4", MediaType.VIDEO, formatId = "best"),
            QualityOption("Worst Quality (small)", "worst", "mp4", MediaType.VIDEO, formatId = "worst"),
        ))
    }

    // ── Facebook stubs ────────────────────────────────────────────────────────
    private fun facebookStubs(url: String): Pair<String, List<QualityOption>> =
        Pair("Facebook Video", listOf(
            QualityOption("HD", "hd", "mp4", MediaType.VIDEO, formatId = "hd"),
            QualityOption("SD", "sd", "mp4", MediaType.VIDEO, formatId = "sd"),
        ))

    // ── Twitter fallback stubs ────────────────────────────────────────────────
    private fun twitterStubs(): List<QualityOption> = listOf(
        QualityOption("Best Quality",  "best",  "mp4", MediaType.VIDEO, formatId = "best"),
        QualityOption("Worst Quality", "worst", "mp4", MediaType.VIDEO, formatId = "worst"),
    )

    // ── Generic direct link ───────────────────────────────────────────────────
    private fun genericStubs(url: String): Pair<String, List<QualityOption>> {
        val ext     = url.substringAfterLast('.').lowercase().take(4).replace(Regex("[^a-z0-9]"), "")
        val isVideo = ext in listOf("mp4", "mkv", "webm", "avi", "mov")
        val isImage = ext in listOf("jpg", "jpeg", "png", "gif", "webp")
        val safeExt = ext.ifEmpty { "mp4" }
        return Pair("Media File", listOf(
            when {
                isImage -> QualityOption("Original Image", "original", safeExt, MediaType.IMAGE, directUrl = url)
                isVideo -> QualityOption("Original Video", "original", safeExt, MediaType.VIDEO, directUrl = url)
                else    -> QualityOption("Download File",  "original", safeExt, MediaType.VIDEO, directUrl = url)
            }
        ))
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────
    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json")
            connectTimeout = TIMEOUT_MS
            readTimeout    = TIMEOUT_MS
            connect()
        }
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode} from $url")
        return conn.inputStream.bufferedReader().readText()
    }

    private fun httpGetWithHeaders(url: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout          = TIMEOUT_MS
            readTimeout             = TIMEOUT_MS
            instanceFollowRedirects = true
            connect()
        }
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode} from $url")
        return conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    }

    private fun httpPost(url: String, body: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            doOutput       = true
            doInput        = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = TIMEOUT_MS
            readTimeout    = TIMEOUT_MS
            connect()
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode} from $url")
        return conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    }
}