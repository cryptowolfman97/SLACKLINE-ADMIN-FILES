package com.snaploader.app.data

import android.content.Context
import android.content.SharedPreferences
import com.snaploader.app.ui.theme.AccentColour
import com.snaploader.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository private constructor(context: Context) {

    val prefs: SharedPreferences =
        context.getSharedPreferences("snaploader_settings", Context.MODE_PRIVATE)

    // ── Theme ─────────────────────────────────────────────────────────────────
    private val _theme = MutableStateFlow(
        if (prefs.getString("theme", "AMOLED") == "LIGHT") AppTheme.LIGHT else AppTheme.AMOLED
    )
    val theme: StateFlow<AppTheme> = _theme

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _theme.value = theme
    }

    // ── Accent colour ─────────────────────────────────────────────────────────
    private val _accentColour = MutableStateFlow(
        try {
            AccentColour.valueOf(
                prefs.getString("accent_colour", AccentColour.EMERALD.name) ?: AccentColour.EMERALD.name
            )
        } catch (_: Exception) { AccentColour.EMERALD }
    )
    val accentColour: StateFlow<AccentColour> = _accentColour

    fun setAccentColour(accent: AccentColour) {
        prefs.edit().putString("accent_colour", accent.name).apply()
        _accentColour.value = accent
    }

    // ── Confirm exit ──────────────────────────────────────────────────────────
    private val _confirmExit = MutableStateFlow(prefs.getBoolean("confirm_exit", true))
    val confirmExit: StateFlow<Boolean> = _confirmExit

    fun setConfirmExit(value: Boolean) {
        prefs.edit().putBoolean("confirm_exit", value).apply()
        _confirmExit.value = value
    }

    // ── Wi-Fi only ────────────────────────────────────────────────────────────
    private val _wifiOnly = MutableStateFlow(prefs.getBoolean("wifi_only", false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly

    fun setWifiOnly(value: Boolean) {
        prefs.edit().putBoolean("wifi_only", value).apply()
        _wifiOnly.value = value
    }

    // ── Download path ─────────────────────────────────────────────────────────
    private val _downloadPath = MutableStateFlow(prefs.getString("download_path", "") ?: "")
    val downloadPath: StateFlow<String> = _downloadPath

    fun setDownloadPath(path: String) {
        prefs.edit().putString("download_path", path).apply()
        _downloadPath.value = path
    }

    // ── Sequential queue ──────────────────────────────────────────────────────
    private val _sequentialQueue = MutableStateFlow(prefs.getBoolean("sequential_queue", false))
    val sequentialQueue: StateFlow<Boolean> = _sequentialQueue

    fun setSequentialQueue(value: Boolean) {
        prefs.edit().putBoolean("sequential_queue", value).apply()
        _sequentialQueue.value = value
    }

    // ── Filename template ─────────────────────────────────────────────────────
    private val _filenameTemplate = MutableStateFlow(
        prefs.getString("filename_template", "{title}") ?: "{title}"
    )
    val filenameTemplate: StateFlow<String> = _filenameTemplate

    fun setFilenameTemplate(template: String) {
        prefs.edit().putString("filename_template", template).apply()
        _filenameTemplate.value = template
    }

    // ── Download subtitles ────────────────────────────────────────────────────
    private val _downloadSubtitles = MutableStateFlow(prefs.getBoolean("download_subtitles", false))
    val downloadSubtitles: StateFlow<Boolean> = _downloadSubtitles

    fun setDownloadSubtitles(value: Boolean) {
        prefs.edit().putBoolean("download_subtitles", value).apply()
        _downloadSubtitles.value = value
    }

    // ── Auto-quality ──────────────────────────────────────────────────────────
    private val _autoQualityEnabled = MutableStateFlow(prefs.getBoolean("auto_quality_enabled", false))
    val autoQualityEnabled: StateFlow<Boolean> = _autoQualityEnabled

    fun setAutoQualityEnabled(value: Boolean) {
        prefs.edit().putBoolean("auto_quality_enabled", value).apply()
        _autoQualityEnabled.value = value
    }

    private val _preferredResolution = MutableStateFlow(
        prefs.getString("preferred_resolution", "1080p") ?: "1080p"
    )
    val preferredResolution: StateFlow<String> = _preferredResolution

    fun setPreferredResolution(res: String) {
        prefs.edit().putString("preferred_resolution", res).apply()
        _preferredResolution.value = res
    }

    private val _preferredFormat = MutableStateFlow(
        prefs.getString("preferred_format", "mp4") ?: "mp4"
    )
    val preferredFormat: StateFlow<String> = _preferredFormat

    fun setPreferredFormat(fmt: String) {
        prefs.edit().putString("preferred_format", fmt).apply()
        _preferredFormat.value = fmt
    }

    // ── Max concurrent downloads ──────────────────────────────────────────────
    private val _maxConcurrent = MutableStateFlow(prefs.getInt("max_concurrent", 3))
    val maxConcurrent: StateFlow<Int> = _maxConcurrent

    fun setMaxConcurrent(value: Int) {
        prefs.edit().putInt("max_concurrent", value.coerceIn(1, 5)).apply()
        _maxConcurrent.value = value.coerceIn(1, 5)
    }

    companion object {
        @Volatile private var INSTANCE: SettingsRepository? = null
        fun getInstance(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
