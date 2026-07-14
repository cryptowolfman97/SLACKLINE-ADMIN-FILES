package com.snaploader.app.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snaploader.app.data.SettingsRepository
import com.snaploader.app.manager.AppDownloadManager
import com.snaploader.app.model.*
import com.snaploader.app.service.FloatingWindowService
import com.snaploader.app.ui.theme.AccentColour
import com.snaploader.app.ui.theme.AppTheme
import com.snaploader.app.util.DirectParser
import com.snaploader.app.util.UrlDetector
import com.snaploader.app.util.YtDlpHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Result types ──────────────────────────────────────────────────────────────
sealed class ShareFetchResult {
    object Loading : ShareFetchResult()
    data class Success(
        val title        : String,
        val thumbnail    : String,
        val qualities    : List<QualityOption>,
        val hasSubtitles : Boolean = false
    ) : ShareFetchResult()
    data class Error(val message: String) : ShareFetchResult()
}

class ShareViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG             = "ShareViewModel"
    private val settings        = SettingsRepository.getInstance(app)
    private val downloadManager = AppDownloadManager.getInstance(app)

    val theme: StateFlow<AppTheme> = settings.theme
    val accentColour: StateFlow<AccentColour> = settings.accentColour

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _platform = MutableStateFlow(Platform.GENERAL)
    val platform: StateFlow<Platform> = _platform

    private val _fetchResult = MutableStateFlow<ShareFetchResult?>(null)
    val fetchResult: StateFlow<ShareFetchResult?> = _fetchResult

    // true = download was queued and sheet can auto-dismiss / show PiP
    private val _downloadStarted = MutableStateFlow(false)
    val downloadStarted: StateFlow<Boolean> = _downloadStarted

    // Whether to show the subtitle toggle in the sheet
    private val _hasSubtitles = MutableStateFlow(false)
    val hasSubtitles: StateFlow<Boolean> = _hasSubtitles

    // Auto-quality settings exposed to the sheet
    val autoQualityEnabled  : StateFlow<Boolean> = settings.autoQualityEnabled
    val preferredResolution : StateFlow<String>  = settings.preferredResolution

    private var cachedTitle     = ""
    private var cachedThumbnail = ""
    private var cachedJson      : String? = null   // keep raw JSON for subtitle check

    // ── Entry point ───────────────────────────────────────────────────────────
    fun loadUrl(url: String) {
        _url.value      = url
        _platform.value = UrlDetector.detect(url)
        fetch(url)
    }

    fun retry() = fetch(_url.value)

    // ── Fetch qualities ───────────────────────────────────────────────────────
    private fun fetch(url: String) {
        _fetchResult.value  = ShareFetchResult.Loading
        _downloadStarted.value = false
        cachedJson          = null

        viewModelScope.launch {
            try {
                val platform = _platform.value

                val ytDlpReady = if (YtDlpHelper.isInitialized()) true
                                 else YtDlpHelper.initWithoutUpdate(getApplication())

                val (title, thumbnail, qualities, hasSubs) = when {
                    platform == Platform.YOUTUBE && ytDlpReady -> {
                        val json = YtDlpHelper.fetchInfo(url)
                        if (json != null) {
                            cachedJson = json
                            val (t, th, q) = YtDlpHelper.parseQualities(json)
                            val subs = YtDlpHelper.hasSubtitles(json)
                            Quad(t, th, q, subs)
                        } else Quad("YouTube Video", youTubeThumbnail(url), emptyList(), false)
                    }
                    platform == Platform.TIKTOK -> {
                        val (t, q) = DirectParser.fetchQualities(url, platform)
                        Quad(t, "", q, false)
                    }
                    platform == Platform.TWITTER -> {
                        val (t, q) = DirectParser.fetchQualities(url, platform)
                        Quad(t, "", q, false)
                    }
                    (platform == Platform.INSTAGRAM || platform == Platform.FACEBOOK) && ytDlpReady -> {
                        val json = YtDlpHelper.fetchInfo(url)
                        if (json != null) {
                            cachedJson = json
                            val (t, th, q) = YtDlpHelper.parseQualities(json)
                            Quad(t, th, q, false)
                        } else {
                            val (t, q) = DirectParser.fetchQualities(url, platform)
                            Quad(t, "", q, false)
                        }
                    }
                    else -> {
                        if (ytDlpReady) {
                            val json = YtDlpHelper.fetchInfo(url)
                            if (json != null) {
                                cachedJson = json
                                val (t, th, q) = YtDlpHelper.parseQualities(json)
                                Quad(t, th, q, YtDlpHelper.hasSubtitles(json))
                            } else {
                                val (t, q) = DirectParser.fetchQualities(url, platform)
                                Quad(t, "", q, false)
                            }
                        } else {
                            val (t, q) = DirectParser.fetchQualities(url, platform)
                            Quad(t, "", q, false)
                        }
                    }
                }

                cachedTitle     = title
                cachedThumbnail = thumbnail
                _hasSubtitles.value = hasSubs

                // Auto-quality: if enabled, immediately start download and skip picker
                if (settings.autoQualityEnabled.value && qualities.isNotEmpty()) {
                    val preferred = settings.preferredResolution.value
                    val pref = qualities.firstOrNull {
                        it.mediaType == MediaType.VIDEO &&
                        it.quality.contains(preferred, ignoreCase = true) &&
                        it.format == "mp4"
                    } ?: qualities.firstOrNull { it.mediaType == MediaType.VIDEO }
                    if (pref != null) {
                        Log.d(TAG, "Auto-quality: picked ${pref.label} for $title")
                        startDownloadInternal(pref, downloadSubtitles = false)
                        return@launch
                    }
                }

                if (qualities.isEmpty()) {
                    _fetchResult.value = ShareFetchResult.Error(
                        "No downloadable formats found for this URL."
                    )
                } else {
                    _fetchResult.value = ShareFetchResult.Success(
                        title        = title,
                        thumbnail    = thumbnail,
                        qualities    = qualities,
                        hasSubtitles = hasSubs
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch exception: ${e.message}", e)
                _fetchResult.value = ShareFetchResult.Error(
                    e.message ?: "Failed to fetch video info"
                )
            }
        }
    }

    // ── Start download ────────────────────────────────────────────────────────
    fun startDownload(quality: QualityOption, downloadSubtitles: Boolean = false) {
        startDownloadInternal(quality, downloadSubtitles)
    }

    private fun startDownloadInternal(quality: QualityOption, downloadSubtitles: Boolean) {
        val url   = _url.value
        val dest  = settings.downloadPath.value.ifEmpty { downloadManager.defaultDownloadDir() }
        val title = cachedTitle.ifEmpty { "Download" }

        downloadManager.enqueue(
            url          = url,
            quality      = quality,
            title        = title,
            platform     = _platform.value,
            destDir      = dest,
            thumbnailUrl = cachedThumbnail,
            subtitleUrl  = if (downloadSubtitles) "en" else ""
        )
        _downloadStarted.value = true

        // Broadcast to FloatingWindowService so it can show a PiP for this download
        val app = getApplication<Application>()
        app.sendBroadcast(
            Intent(FloatingWindowService.ACTION_SHARE_DOWNLOAD_STARTED).apply {
                putExtra(FloatingWindowService.EXTRA_SHARE_TITLE, title)
            }
        )
        Log.d(TAG, "Share download queued: $title | ${quality.label}")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun youTubeThumbnail(url: String): String {
        val videoId = Regex("(?:v=|youtu\\.be/)([A-Za-z0-9_-]{11})").find(url)
            ?.groupValues?.get(1) ?: return ""
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
}

// Simple 4-value holder to avoid nested Pairs
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
