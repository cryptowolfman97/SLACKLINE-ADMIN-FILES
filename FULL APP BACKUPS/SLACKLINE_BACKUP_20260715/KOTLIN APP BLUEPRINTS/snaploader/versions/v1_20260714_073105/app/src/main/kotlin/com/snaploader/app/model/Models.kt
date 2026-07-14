package com.snaploader.app.model

// ── Platform enum ─────────────────────────────────────────────────────────────
enum class Platform(val label: String, val emoji: String, val color: Long) {
    YOUTUBE   ("YouTube",   "▶",  0xFFFF0000),
    TIKTOK    ("TikTok",    "♪",  0xFF010101),
    INSTAGRAM ("Instagram", "📸", 0xFFE1306C),
    FACEBOOK  ("Facebook",  "f",  0xFF1877F2),
    TWITTER   ("Twitter/X", "𝕏",  0xFF000000),
    GENERAL   ("General",   "🔗", 0xFF607D8B),
    UNKNOWN   ("Unknown",   "📁", 0xFF9E9E9E),
}

// ── Media type ────────────────────────────────────────────────────────────────
enum class MediaType { VIDEO, AUDIO, IMAGE }

// ── A single quality option returned by the parser ───────────────────────────
data class QualityOption(
    val label        : String,
    val quality      : String,
    val format       : String,
    val mediaType    : MediaType,
    val sizeEstimate : String  = "",
    val directUrl    : String  = "",
    val formatId     : String  = "",
    val subtitleUrl  : String  = "",      // optional SRT/VTT URL
    val isPreferred  : Boolean = false    // marked as auto-quality preference
)

// ── Download job tracked in the DownloadManager ──────────────────────────────
data class DownloadItem(
    val id             : String,
    val title          : String,
    val platform       : Platform,
    val quality        : String,
    val format         : String,
    val mediaType      : MediaType,
    val url            : String,
    var progress       : Int            = 0,
    var status         : DownloadStatus = DownloadStatus.QUEUED,
    var filePath       : String         = "",
    val timestamp      : Long           = System.currentTimeMillis(),
    var queuePosition  : Int            = 0,
    var errorMessage   : String         = "",
    var speedBps       : Long           = 0L,
    var etaSeconds     : Long           = -1L,
    val thumbnailUrl   : String         = "",
    val subtitleUrl    : String         = "",
    var retryCount     : Int            = 0,
    var resumeOffset   : Long           = 0L    // bytes already downloaded (for resume)
)

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

// ── Batch download entry ──────────────────────────────────────────────────────
data class BatchEntry(
    val url      : String,
    val platform : Platform,
    var selected : Boolean = true
)

// ── Result wrapper ────────────────────────────────────────────────────────────
sealed class FetchResult {
    data class Success(
        val title        : String,
        val thumbnail    : String,
        val platform     : Platform,
        val qualities    : List<QualityOption>,
        val hasSubtitles : Boolean = false
    ) : FetchResult()
    data class Error(val message: String) : FetchResult()
    object Loading : FetchResult()
}

// ── Storage stats ─────────────────────────────────────────────────────────────
data class StorageStats(
    val totalBytes : Long,
    val fileCount  : Int,
    val videoCount : Int,
    val audioCount : Int
) {
    val totalMb      : Float  get() = totalBytes / (1024f * 1024f)
    val totalGb      : Float  get() = totalBytes / (1024f * 1024f * 1024f)
    val displaySize  : String get() = when {
        totalBytes < 1024 * 1024        -> "${totalBytes / 1024} KB"
        totalBytes < 1024 * 1024 * 1024 -> "%.1f MB".format(totalMb)
        else                            -> "%.2f GB".format(totalGb)
    }
}

// ── Auto-quality preference ───────────────────────────────────────────────────
data class AutoQualityPref(
    val platform   : Platform,
    val resolution : String,   // e.g. "1080p"
    val format     : String    // e.g. "mp4"
)
