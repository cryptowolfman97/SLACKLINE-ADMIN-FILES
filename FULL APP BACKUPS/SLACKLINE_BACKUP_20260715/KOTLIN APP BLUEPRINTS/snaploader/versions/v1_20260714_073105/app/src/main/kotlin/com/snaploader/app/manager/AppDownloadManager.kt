package com.snaploader.app.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.snaploader.app.MainActivity
import com.snaploader.app.SnapLoaderApp
import com.snaploader.app.model.*
import com.snaploader.app.util.YtDlpHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AppDownloadManager(private val context: Context) {

    private val TAG          = "AppDownloadManager"
    private val scope        = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notifManager = context.getSystemService(NotificationManager::class.java)

    private val _downloads  = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads

    private val pauseSignals   = ConcurrentHashMap<String, Boolean>()
    private val activeJobs     = ConcurrentHashMap<String, Job>()
    private val tempFiles      = ConcurrentHashMap<String, File>()   // id → .part file

    var sequentialQueue  : Boolean = false
    var filenameTemplate : String  = "{title}"
    var wifiOnly         : Boolean = false
    var subtitleLang     : String  = ""   // "en" or "" to disable

    // ── Enqueue ───────────────────────────────────────────────────────────────
    fun enqueue(
        url          : String,
        quality      : QualityOption,
        title        : String,
        platform     : Platform,
        destDir      : String = defaultDownloadDir(),
        thumbnailUrl : String = "",
        subtitleUrl  : String = ""
    ): String {
        val id       = UUID.randomUUID().toString()
        val position = _downloads.value.size
        val item     = DownloadItem(
            id           = id,
            title        = title,
            platform     = platform,
            quality      = quality.quality,
            format       = quality.format,
            mediaType    = quality.mediaType,
            url          = url,
            status       = DownloadStatus.QUEUED,
            queuePosition = position,
            thumbnailUrl = thumbnailUrl,
            subtitleUrl  = subtitleUrl
        )
        _downloads.update { it + item }

        if (sequentialQueue) {
            val hasActive = _downloads.value.any {
                it.id != id && it.status == DownloadStatus.DOWNLOADING
            }
            if (!hasActive) scope.launch { executeDownload(item, quality, destDir) }
        } else {
            scope.launch { executeDownload(item, quality, destDir) }
        }
        return id
    }

    // ── Execute ───────────────────────────────────────────────────────────────
    private suspend fun executeDownload(
        item    : DownloadItem,
        quality : QualityOption,
        destDir : String
    ) {
        // Wi-Fi gate
        if (wifiOnly && !isOnWifi()) {
            Log.w(TAG, "Wi-Fi only mode, no Wi-Fi available — queuing ${item.title}")
            updateStatus(item.id, DownloadStatus.QUEUED)
            return
        }

        Log.d(TAG, "Starting: ${item.title} | quality=${quality.quality} | fmt=${quality.formatId}")
        updateStatus(item.id, DownloadStatus.DOWNLOADING)
        showNotification(item.id, item.title, 0)

        val job = scope.launch {
            val (success, filePath) = when {
                quality.directUrl.isNotEmpty() -> {
                    Log.d(TAG, "Direct download: ${item.title}")
                    val ok = downloadDirect(
                        id       = item.id,
                        url      = quality.directUrl,
                        destDir  = destDir,
                        title    = item.title,
                        ext      = quality.format,
                        platform = item.platform.label
                    )
                    Pair(ok, findSavedFile(destDir, item.title, quality.format)?.absolutePath ?: "")
                }
                quality.formatId.isNotEmpty() -> {
                    val fmt = if (item.mediaType == MediaType.AUDIO)
                        quality.formatId
                    else
                        "${quality.formatId}+bestaudio[ext=m4a]/bestaudio"

                    Log.d(TAG, "yt-dlp download: ${item.title} format=$fmt")
                    YtDlpHelper.download(
                        url              = item.url,
                        formatId         = fmt,
                        destPath         = destDir,
                        subtitleLang     = if (item.subtitleUrl.isEmpty()) subtitleLang else "",
                        progressCallback = { pct, spd, eta ->
                            updateProgress(item.id, pct)
                            updateSpeedEta(item.id, spd, eta)
                            showNotification(item.id, item.title, pct)
                        }
                    )
                }
                else -> {
                    Log.e(TAG, "No directUrl and no formatId for ${item.title}")
                    Pair(false, "")
                }
            }

            notifManager.cancel(item.id.hashCode())
            activeJobs.remove(item.id)
            tempFiles.remove(item.id)

            val wasPaused = pauseSignals[item.id] == true
            pauseSignals.remove(item.id)

            if (success) {
                Log.d(TAG, "Completed: ${item.title} → $filePath")
                _downloads.update { list ->
                    list.map {
                        if (it.id == item.id) it.copy(
                            status     = DownloadStatus.COMPLETED,
                            progress   = 100,
                            filePath   = filePath,
                            speedBps   = 0L,
                            etaSeconds = -1L,
                            retryCount = it.retryCount
                        ) else it
                    }
                }
                showCompleteNotification(item.id, item.title)
            } else if (!wasPaused) {
                val currentRetry = _downloads.value.find { it.id == item.id }?.retryCount ?: 0
                if (currentRetry < 2) {
                    // Auto-retry up to 2 times with 5s delay
                    Log.w(TAG, "Download failed, auto-retry ${currentRetry + 1}/2: ${item.title}")
                    _downloads.update { list ->
                        list.map {
                            if (it.id == item.id) it.copy(
                                status     = DownloadStatus.QUEUED,
                                retryCount = currentRetry + 1,
                                progress   = 0
                            ) else it
                        }
                    }
                    delay(5_000)
                    val retried = _downloads.value.find { it.id == item.id } ?: return@launch
                    executeDownload(retried, quality, destDir)
                } else {
                    Log.e(TAG, "Download failed after retries: ${item.title}")
                    updateStatus(item.id, DownloadStatus.FAILED)
                    showFailedNotification(item.id, item.title)
                }
            }

            if (sequentialQueue) kickNextQueued(destDir)
        }

        activeJobs[item.id] = job
        job.join()
    }

    private fun kickNextQueued(destDir: String) {
        val next = _downloads.value
            .filter { it.status == DownloadStatus.QUEUED }
            .minByOrNull { it.queuePosition } ?: return
        val q = QualityOption(
            label     = next.quality,
            quality   = next.quality,
            format    = next.format,
            mediaType = next.mediaType,
            directUrl = next.url,
            formatId  = ""
        )
        scope.launch { executeDownload(next, q, destDir) }
    }

    // ── Pause / Resume ────────────────────────────────────────────────────────
    fun pause(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        if (item.status != DownloadStatus.DOWNLOADING) return
        Log.d(TAG, "Pausing: ${item.title}")
        pauseSignals[id] = true
        updateStatus(id, DownloadStatus.PAUSED)
        activeJobs[id]?.cancel()
        notifManager.cancel(id.hashCode())
    }

    fun resume(id: String, quality: QualityOption, destDir: String = defaultDownloadDir()) {
        val item = _downloads.value.find { it.id == id } ?: return
        if (item.status != DownloadStatus.PAUSED) return
        Log.d(TAG, "Resuming: ${item.title}")
        pauseSignals.remove(id)
        updateStatus(id, DownloadStatus.QUEUED)
        scope.launch { executeDownload(item, quality, destDir) }
    }

    // ── Reorder queue ─────────────────────────────────────────────────────────
    fun moveUp(id: String) {
        _downloads.update { list ->
            val idx = list.indexOfFirst { it.id == id }.takeIf { it > 0 } ?: return@update list
            val m = list.toMutableList()
            val tmp = m[idx]; m[idx] = m[idx - 1]; m[idx - 1] = tmp
            m.mapIndexed { i, item -> item.copy(queuePosition = i) }
        }
    }

    fun moveDown(id: String) {
        _downloads.update { list ->
            val idx = list.indexOfFirst { it.id == id }
                .takeIf { it >= 0 && it < list.size - 1 } ?: return@update list
            val m = list.toMutableList()
            val tmp = m[idx]; m[idx] = m[idx + 1]; m[idx + 1] = tmp
            m.mapIndexed { i, item -> item.copy(queuePosition = i) }
        }
    }

    // ── Direct download with resume support ───────────────────────────────────
    private suspend fun downloadDirect(
        id      : String,
        url     : String,
        destDir : String,
        title   : String,
        ext     : String,
        platform: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "downloadDirect: url=${url.take(100)}")

            val isTikTok = url.contains("tiktok.com") || url.contains("tiktokcdn")
            val dir      = File(destDir).also { it.mkdirs() }
            val baseName = applyFilenameTemplate(title, platform)
            val destFile = run {
                var candidate = File(dir, "$baseName.$ext")
                var counter   = 1
                while (candidate.exists() && !tempFiles.containsKey(id)) {
                    candidate = File(dir, "$baseName ($counter).$ext")
                    counter++
                }
                candidate
            }
            val partFile = File(destDir, "${destFile.nameWithoutExtension}.part")
            tempFiles[id] = partFile

            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                if (isTikTok) {
                    setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    setRequestProperty("Referer",        "https://www.tiktok.com/")
                    setRequestProperty("Origin",         "https://www.tiktok.com")
                    setRequestProperty("Sec-Fetch-Dest", "video")
                    setRequestProperty("Sec-Fetch-Mode", "no-cors")
                    setRequestProperty("Sec-Fetch-Site", "cross-site")
                } else {
                    setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                }
                setRequestProperty("Accept", "*/*")
                // Resume header
                if (existingBytes > 0) setRequestProperty("Range", "bytes=$existingBytes-")
                connectTimeout          = 30_000
                readTimeout             = 60_000
                instanceFollowRedirects = true
                connect()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "downloadDirect: HTTP $responseCode, content-length=${conn.contentLengthLong}, resume=$existingBytes")

            val isResume  = responseCode == 206
            val isNormal  = responseCode in 200..299
            if (!isNormal) {
                Log.e(TAG, "downloadDirect: HTTP error $responseCode")
                return@withContext false
            }

            val contentLength = conn.contentLengthLong
            val total         = if (isResume) existingBytes + contentLength else contentLength
            var downloaded    = if (isResume) existingBytes else 0L
            var lastTime      = System.currentTimeMillis()
            var lastBytes     = downloaded

            Log.d(TAG, "downloadDirect: saving to ${partFile.absolutePath}, resume=$isResume, offset=$downloaded")

            conn.inputStream.use { input ->
                // Append mode for resume, overwrite for fresh start
                (if (isResume) RandomAccessFile(partFile, "rw").also { it.seek(existingBytes) }.let { raf ->
                    object : java.io.OutputStream() {
                        override fun write(b: Int) = raf.write(b)
                        override fun write(b: ByteArray, off: Int, len: Int) = raf.write(b, off, len)
                        override fun close() = raf.close()
                    }
                } else FileOutputStream(partFile)).use { output ->
                    val buffer = ByteArray(16_384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (pauseSignals[id] == true) {
                            Log.d(TAG, "downloadDirect: pause at $downloaded bytes (part file kept)")
                            return@withContext false
                        }
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now     = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speed = if (elapsed > 0) (downloaded - lastBytes) * 1000L / elapsed else 0L
                            val eta   = if (speed > 0 && total > 0) (total - downloaded) / speed else -1L
                            updateSpeedEta(id, speed, eta)
                            lastTime  = now
                            lastBytes = downloaded
                        }
                        if (total > 0) {
                            val pct = ((downloaded.toDouble() / total) * 100).toInt().coerceIn(0, 100)
                            updateProgress(id, pct)
                            showNotification(id, title, pct)
                        }
                    }
                }
            }

            // Rename .part → final file
            if (downloaded > 0) {
                partFile.renameTo(destFile)
                Log.d(TAG, "downloadDirect: complete, renamed to ${destFile.absolutePath}")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            if (pauseSignals[id] == true) return@withContext false
            Log.e(TAG, "downloadDirect exception: ${e.message}", e)
            false
        }
    }

    // ── Filename template ─────────────────────────────────────────────────────
    private fun applyFilenameTemplate(title: String, platform: String = ""): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return filenameTemplate
            .replace("{title}",    title)
            .replace("{date}",     date)
            .replace("{platform}", platform)
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .trim().take(80).ifEmpty { sanitize(title) }
    }

    private fun findSavedFile(destDir: String, title: String, ext: String): File? {
        val dir = File(destDir)
        if (!dir.exists()) return null
        val base = sanitize(title).take(20)
        return dir.listFiles()
            ?.filter {
                it.isFile &&
                it.extension.equals(ext, ignoreCase = true) &&
                it.nameWithoutExtension.startsWith(base)
            }
            ?.maxByOrNull { it.lastModified() }
    }

    // ── Network check ─────────────────────────────────────────────────────────
    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // ── Storage stats ─────────────────────────────────────────────────────────
    fun getStorageStats(): StorageStats {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "UDbySHV"
        )
        if (!dir.exists()) return StorageStats(0L, 0, 0, 0)
        val files      = dir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
        val totalBytes = files.sumOf { it.length() }
        val audioExts  = setOf("mp3", "m4a", "aac", "opus")
        val audioCount = files.count { it.extension.lowercase() in audioExts }
        return StorageStats(
            totalBytes = totalBytes,
            fileCount  = files.size,
            videoCount = files.size - audioCount,
            audioCount = audioCount
        )
    }

    fun clearAllDownloads(onlyCompleted: Boolean = false) {
        if (!onlyCompleted) {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
            pauseSignals.clear()
            _downloads.update { emptyList() }
        } else {
            _downloads.update { list -> list.filter { it.status != DownloadStatus.COMPLETED } }
        }
    }

    fun deleteDownloadFile(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        if (item.filePath.isNotEmpty()) File(item.filePath).takeIf { it.exists() }?.delete()
        // Also clean up .part file if any
        tempFiles[id]?.takeIf { it.exists() }?.delete()
        remove(id)
    }

    // ── State helpers ─────────────────────────────────────────────────────────
    private fun updateStatus(id: String, status: DownloadStatus, progress: Int = -1) {
        _downloads.update { list ->
            list.map {
                if (it.id == id) it.copy(
                    status   = status,
                    progress = if (progress >= 0) progress else it.progress
                ) else it
            }
        }
    }

    private fun updateProgress(id: String, progress: Int) {
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(progress = progress) else it }
        }
    }

    private fun updateSpeedEta(id: String, speedBps: Long, etaSeconds: Long) {
        _downloads.update { list ->
            list.map {
                if (it.id == id) it.copy(speedBps = speedBps, etaSeconds = etaSeconds)
                else it
            }
        }
    }

    fun remove(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        pauseSignals.remove(id)
        tempFiles.remove(id)
        _downloads.update { it.filter { item -> item.id != id } }
        notifManager.cancel(id.hashCode())
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun showNotification(id: String, title: String, progress: Int) {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        notifManager.notify(
            id.hashCode(),
            NotificationCompat.Builder(context, SnapLoaderApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText("Downloading… $progress%")
                .setProgress(100, progress, progress == 0)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setContentIntent(intent).build()
        )
    }

    private fun showCompleteNotification(id: String, title: String) {
        notifManager.notify(
            id.hashCode(),
            NotificationCompat.Builder(context, SnapLoaderApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete").setContentText(title)
                .setAutoCancel(true).setSilent(true).build()
        )
    }

    private fun showFailedNotification(id: String, title: String) {
        val retryIntent = PendingIntent.getActivity(
            context, id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        notifManager.notify(
            id.hashCode(),
            NotificationCompat.Builder(context, SnapLoaderApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download failed")
                .setContentText(title)
                .setContentIntent(retryIntent)
                .setAutoCancel(true).build()
        )
    }

    // ── Utils ─────────────────────────────────────────────────────────────────
    private fun sanitize(name: String) =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().take(80).ifEmpty { "download" }

    fun defaultDownloadDir(): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "UDbySHV"
        )
        dir.mkdirs()
        return dir.absolutePath
    }

    fun loadDownloadsFromDisk(): List<DownloadItem> {
        val dirsToScan = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "UDbySHV"),
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SnapLoader")
        )
        return dirsToScan.filter { it.exists() }.flatMap { dir ->
            dir.listFiles()?.filter { it.isFile && it.length() > 0 && it.extension != "part" }?.map { file ->
                val ext  = file.extension.lowercase()
                val type = if (ext in listOf("mp3", "m4a", "aac", "opus")) MediaType.AUDIO else MediaType.VIDEO
                DownloadItem(
                    id        = file.absolutePath,
                    title     = file.nameWithoutExtension,
                    platform  = Platform.UNKNOWN,
                    quality   = ext.uppercase(),
                    format    = ext,
                    mediaType = type,
                    url       = "",
                    status    = DownloadStatus.COMPLETED,
                    progress  = 100,
                    filePath  = file.absolutePath
                )
            } ?: emptyList()
        }.sortedByDescending { File(it.id).lastModified() }
    }

    fun mergeFromDisk(diskItems: List<DownloadItem>) {
        _downloads.update { current ->
            val existingIds = current.map { it.id }.toSet()
            current + diskItems.filter { it.id !in existingIds }
        }
    }

    companion object {
        @Volatile private var INSTANCE: AppDownloadManager? = null
        fun getInstance(context: Context): AppDownloadManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
