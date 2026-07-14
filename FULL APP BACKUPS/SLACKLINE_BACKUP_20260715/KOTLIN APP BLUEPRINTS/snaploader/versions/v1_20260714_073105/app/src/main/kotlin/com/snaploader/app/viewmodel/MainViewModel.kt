package com.snaploader.app.viewmodel

import android.app.Application
import android.content.*
import android.os.Build
import android.provider.Settings
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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG             = "MainViewModel"
    private val settings        = SettingsRepository.getInstance(app)
    private val downloadManager = AppDownloadManager.getInstance(app)

    // ── Settings ──────────────────────────────────────────────────────────────
    val theme               : StateFlow<AppTheme>     = settings.theme
    val accentColour        : StateFlow<AccentColour> = settings.accentColour
    val confirmExit         : StateFlow<Boolean>      = settings.confirmExit
    val wifiOnly            : StateFlow<Boolean>      = settings.wifiOnly
    val downloadPath        : StateFlow<String>       = settings.downloadPath
    val sequentialQueue     : StateFlow<Boolean>      = settings.sequentialQueue
    val filenameTemplate    : StateFlow<String>       = settings.filenameTemplate
    val downloadSubtitles   : StateFlow<Boolean>      = settings.downloadSubtitles
    val autoQualityEnabled  : StateFlow<Boolean>      = settings.autoQualityEnabled
    val preferredResolution : StateFlow<String>       = settings.preferredResolution
    val preferredFormat     : StateFlow<String>       = settings.preferredFormat
    val maxConcurrent       : StateFlow<Int>          = settings.maxConcurrent

    // ── Downloads ─────────────────────────────────────────────────────────────
    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads

    // ── URL input ─────────────────────────────────────────────────────────────
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput

    private val _detectedPlatform = MutableStateFlow(Platform.GENERAL)
    val detectedPlatform: StateFlow<Platform> = _detectedPlatform

    // ── Fetch result ──────────────────────────────────────────────────────────
    private val _fetchResult = MutableStateFlow<FetchResult?>(null)
    val fetchResult: StateFlow<FetchResult?> = _fetchResult

    // ── Batch download ────────────────────────────────────────────────────────
    private val _batchEntries = MutableStateFlow<List<BatchEntry>>(emptyList())
    val batchEntries: StateFlow<List<BatchEntry>> = _batchEntries

    private val _showBatchDialog = MutableStateFlow(false)
    val showBatchDialog: StateFlow<Boolean> = _showBatchDialog

    // ── Storage stats ─────────────────────────────────────────────────────────
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats

    // ── Floating window ───────────────────────────────────────────────────────
    private val _isFloating = MutableStateFlow(false)
    val isFloating: StateFlow<Boolean> = _isFloating

    private val _needsOverlayPermission = MutableStateFlow(false)
    val needsOverlayPermission: StateFlow<Boolean> = _needsOverlayPermission

    // ── Snackbar ──────────────────────────────────────────────────────────────
    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar

    // ── yt-dlp ────────────────────────────────────────────────────────────────
    private val _ytDlpAvailable = MutableStateFlow(false)
    val ytDlpAvailable: StateFlow<Boolean> = _ytDlpAvailable

    // ── Clipboard pending URL ─────────────────────────────────────────────────
    private val _pendingClipboardUrl = MutableStateFlow<String?>(null)
    val pendingClipboardUrl: StateFlow<String?> = _pendingClipboardUrl

    // ── License / SHVertex account (additive) ─────────────────────────────────
    private val _accountChecking = MutableStateFlow(true)
    val accountChecking: StateFlow<Boolean> = _accountChecking
    private val _accountLoggedIn = MutableStateFlow(false)
    val accountLoggedIn: StateFlow<Boolean> = _accountLoggedIn
    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail
    private val _licenseChecking = MutableStateFlow(false)
    val licenseChecking: StateFlow<Boolean> = _licenseChecking
    private val _licenseTier = MutableStateFlow(com.snaploader.app.license.SHVLicense.Tier.FREE)
    val licenseTier: StateFlow<com.snaploader.app.license.SHVLicense.Tier> = _licenseTier
    private val _deviceCode = MutableStateFlow("")
    val deviceCode: StateFlow<String> = _deviceCode

    private val _loginEmailDraft = MutableStateFlow("")
    val loginEmailDraft: StateFlow<String> = _loginEmailDraft
    private val _loginPasswordDraft = MutableStateFlow("")
    val loginPasswordDraft: StateFlow<String> = _loginPasswordDraft
    private val _loginRememberMe = MutableStateFlow(true)
    val loginRememberMe: StateFlow<Boolean> = _loginRememberMe
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError
    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy

    private val _showLicenseDialog = MutableStateFlow(false)
    val showLicenseDialogFlow: StateFlow<Boolean> = _showLicenseDialog
    private val _lockedFeatureTier = MutableStateFlow<com.snaploader.app.license.SHVLicense.Tier?>(null)
    val lockedFeatureTier: StateFlow<com.snaploader.app.license.SHVLicense.Tier?> = _lockedFeatureTier

    private val _activationCodeDraft = MutableStateFlow("")
    val activationCodeDraft: StateFlow<String> = _activationCodeDraft
    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage
    private val _activationBusy = MutableStateFlow(false)
    val activationBusy: StateFlow<Boolean> = _activationBusy

    private val _requestTierSelected = MutableStateFlow(com.snaploader.app.license.SHVLicense.Tier.PRO)
    val requestTierSelected: StateFlow<com.snaploader.app.license.SHVLicense.Tier> = _requestTierSelected

    init {
        viewModelScope.launch { sequentialQueue.collect { downloadManager.sequentialQueue = it } }
        viewModelScope.launch { filenameTemplate.collect { downloadManager.filenameTemplate = it } }
        viewModelScope.launch { wifiOnly.collect { downloadManager.wifiOnly = it } }
        viewModelScope.launch {
            downloadSubtitles.collect { downloadManager.subtitleLang = if (it) "en" else "" }
        }
        viewModelScope.launch {
            val fromDisk = downloadManager.loadDownloadsFromDisk()
            if (fromDisk.isNotEmpty()) downloadManager.mergeFromDisk(fromDisk)
        }
        viewModelScope.launch {
            _ytDlpAvailable.value = YtDlpHelper.init(getApplication())
        }
        // Broadcast updated download list + accent colour to FloatingWindowService on every change
        viewModelScope.launch {
            combine(downloads, accentColour) { list, accent -> Pair(list, accent) }.collect { (list, accent) ->
                if (_isFloating.value) broadcastDownloadList(list, accent)
            }
        }
        bootAccountAndLicense()
    }

    // ── URL input ─────────────────────────────────────────────────────────────
    fun onUrlChanged(url: String) {
        _urlInput.value         = url
        _detectedPlatform.value = if (UrlDetector.isValidUrl(url)) UrlDetector.detect(url)
                                  else Platform.GENERAL
        if (_fetchResult.value != null && _fetchResult.value !is FetchResult.Loading)
            _fetchResult.value = null
    }

    fun pasteFromClipboard(text: String) {
        val trimmed = text.trim()
        val urls    = extractUrls(trimmed)
        if (urls.size > 1) {
            _batchEntries.value    = urls.map { BatchEntry(it, UrlDetector.detect(it)) }
            _showBatchDialog.value = true
        } else {
            onUrlChanged(trimmed)
        }
    }

    fun clearUrl() {
        _urlInput.value         = ""
        _fetchResult.value      = null
        _detectedPlatform.value = Platform.GENERAL
    }

    fun checkClipboardOnResume(context: Context) {
        try {
            val cm      = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text    = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
            val trimmed = text.trim()
            if (trimmed.startsWith("http") &&
                UrlDetector.isValidUrl(trimmed) &&
                trimmed != _urlInput.value &&
                _urlInput.value.isEmpty()) {
                _pendingClipboardUrl.value = trimmed
                viewModelScope.launch { _snackbar.emit("📋 Link detected — tap Paste to use it") }
            }
        } catch (_: Exception) {}
    }

    fun acceptClipboardUrl() {
        val url = _pendingClipboardUrl.value ?: return
        _pendingClipboardUrl.value = null
        pasteFromClipboard(url)
    }

    fun dismissClipboardUrl() { _pendingClipboardUrl.value = null }

    // ── Fetch qualities ───────────────────────────────────────────────────────
    fun fetchQualities() {
        val url = _urlInput.value.trim()
        if (!UrlDetector.isValidUrl(url)) {
            viewModelScope.launch { _snackbar.emit("Please enter a valid URL") }
            return
        }
        _fetchResult.value = FetchResult.Loading
        viewModelScope.launch {
            val platform = _detectedPlatform.value
            try {
                val result: FetchResult = when (platform) {
                    Platform.YOUTUBE -> {
                        if (!_ytDlpAvailable.value) {
                            FetchResult.Error("YouTube requires yt-dlp. Binary may be missing.")
                        } else {
                            val json = YtDlpHelper.fetchInfo(url)
                            if (json != null) {
                                val (title, thumbnail, qualities) = YtDlpHelper.parseQualities(json)
                                val hasSubs = YtDlpHelper.hasSubtitles(json)
                                FetchResult.Success(title, thumbnail, platform, qualities, hasSubs)
                            } else {
                                val (title, qualities) = DirectParser.fetchQualities(url, platform)
                                FetchResult.Success("$title (format list unavailable)", "", platform, qualities)
                            }
                        }
                    }
                    Platform.TIKTOK -> {
                        val (title, qualities) = DirectParser.fetchQualities(url, platform)
                        if (qualities.isEmpty()) FetchResult.Error("Could not fetch TikTok video.")
                        else FetchResult.Success(title, "", platform, qualities)
                    }
                    Platform.TWITTER -> {
                        val (title, qualities) = DirectParser.fetchQualities(url, platform)
                        if (qualities.isEmpty()) FetchResult.Error("Could not fetch Twitter/X video.")
                        else FetchResult.Success(title, "", platform, qualities)
                    }
                    Platform.INSTAGRAM, Platform.FACEBOOK -> {
                        if (_ytDlpAvailable.value) {
                            val json = YtDlpHelper.fetchInfo(url)
                            if (json != null) {
                                val (title, thumbnail, qualities) = YtDlpHelper.parseQualities(json)
                                FetchResult.Success(title, thumbnail, platform, qualities)
                            } else {
                                val (title, qualities) = DirectParser.fetchQualities(url, platform)
                                FetchResult.Success(title, "", platform, qualities)
                            }
                        } else FetchResult.Error("${platform.label} downloads require yt-dlp.")
                    }
                    else -> {
                        val ytResult = if (_ytDlpAvailable.value) {
                            val json = YtDlpHelper.fetchInfo(url)
                            if (json != null) {
                                val (title, thumbnail, qualities) = YtDlpHelper.parseQualities(json)
                                FetchResult.Success(title, thumbnail, platform, qualities)
                            } else null
                        } else null
                        ytResult ?: run {
                            val (title, qualities) = DirectParser.fetchQualities(url, platform)
                            FetchResult.Success(title, "", platform, qualities)
                        }
                    }
                }
                _fetchResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "fetchQualities: ${e.message}", e)
                _fetchResult.value = FetchResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Start download ────────────────────────────────────────────────────────
    fun startDownload(quality: QualityOption) {
        val url    = _urlInput.value.trim()
        val result = _fetchResult.value as? FetchResult.Success ?: return
        val dest   = downloadPath.value.ifEmpty { downloadManager.defaultDownloadDir() }
        val subLang = if (downloadSubtitles.value &&
            quality.mediaType == MediaType.VIDEO &&
            result.hasSubtitles) "en" else ""
        downloadManager.enqueue(
            url          = url,
            quality      = quality,
            title        = result.title,
            platform     = result.platform,
            destDir      = dest,
            thumbnailUrl = result.thumbnail,
            subtitleUrl  = subLang
        )
        viewModelScope.launch { _snackbar.emit("Download started: ${result.title} (${quality.label})") }
    }

    // ── Batch download ────────────────────────────────────────────────────────
    fun dismissBatchDialog() { _showBatchDialog.value = false }

    fun toggleBatchEntry(url: String, selected: Boolean) {
        _batchEntries.update { list ->
            list.map { if (it.url == url) it.copy(selected = selected) else it }
        }
    }

    /** Called from HomeScreen when multiple URLs are detected in the input. */
    fun fetchQualitiesForUrls(urls: List<String>) {
        if (urls.isEmpty()) return
        if (urls.size == 1) {
            onUrlChanged(urls[0])
            fetchQualities()
            return
        }
        viewModelScope.launch {
            _batchEntries.value    = urls.map { BatchEntry(it, UrlDetector.detect(it), true) }
            _showBatchDialog.value = true
        }
    }

    fun startBatchDownload(qualityPref: String = "1080p") {
        val selected = _batchEntries.value.filter { it.selected }
        val dest     = downloadPath.value.ifEmpty { downloadManager.defaultDownloadDir() }
        _showBatchDialog.value = false
        viewModelScope.launch {
            selected.forEach { entry ->
                try {
                    val (title, qualities) = DirectParser.fetchQualities(entry.url, entry.platform)
                    val quality = qualities.firstOrNull {
                        it.quality.contains(qualityPref, ignoreCase = true)
                    } ?: qualities.firstOrNull() ?: return@forEach
                    downloadManager.enqueue(entry.url, quality, title, entry.platform, dest)
                } catch (e: Exception) {
                    Log.e(TAG, "Batch fetch failed for ${entry.url}: ${e.message}")
                }
            }
            _snackbar.emit("Batch started: ${selected.size} downloads queued")
        }
    }

    // ── Queue management ──────────────────────────────────────────────────────
    fun removeDownload(id: String)     = downloadManager.remove(id)
    fun deleteDownloadFile(id: String) = downloadManager.deleteDownloadFile(id)
    fun pauseDownload(id: String)      = downloadManager.pause(id)

    fun resumeDownload(id: String) {
        val item = downloads.value.find { it.id == id } ?: return
        val dest = downloadPath.value.ifEmpty { downloadManager.defaultDownloadDir() }
        val q    = QualityOption(
            label     = item.quality,
            quality   = item.quality,
            format    = item.format,
            mediaType = item.mediaType,
            directUrl = item.url,
            formatId  = ""
        )
        downloadManager.resume(id, q, dest)
    }

    fun retryDownload(id: String) {
        val item = downloads.value.find { it.id == id } ?: return
        val dest = downloadPath.value.ifEmpty { downloadManager.defaultDownloadDir() }
        val q    = QualityOption(
            label     = item.quality,
            quality   = item.quality,
            format    = item.format,
            mediaType = item.mediaType,
            directUrl = item.url,
            formatId  = ""
        )
        // Reset status then resume so AppDownloadManager treats it as a fresh attempt
        downloadManager.resume(id, q, dest)
    }

    fun moveDownloadUp(id: String)   = downloadManager.moveUp(id)
    fun moveDownloadDown(id: String) = downloadManager.moveDown(id)
    fun clearCompleted()             = downloadManager.clearAllDownloads(onlyCompleted = true)
    fun clearAll()                   = downloadManager.clearAllDownloads(onlyCompleted = false)

    // ── Storage stats ─────────────────────────────────────────────────────────
    fun refreshStorageStats() {
        viewModelScope.launch { _storageStats.value = downloadManager.getStorageStats() }
    }

    // ── Floating window ───────────────────────────────────────────────────────
    fun toggleFloatingWindow(context: Context) {
        if (_isFloating.value) stopFloatingWindow(context) else startFloatingWindow(context)
    }

    fun startFloatingWindow(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            _needsOverlayPermission.value = true
            return
        }
        val activeItems = downloads.value.filter { it.status == DownloadStatus.DOWNLOADING }
        val accent      = accentColour.value
        val intent      = FloatingWindowService.buildIntent(context, activeItems.size).apply {
            putExtra(FloatingWindowService.EXTRA_DOWNLOAD_TITLES,
                activeItems.map { it.title }.toTypedArray())
            putExtra(FloatingWindowService.EXTRA_DOWNLOAD_PROGRESSES,
                activeItems.map { it.progress }.toIntArray())
            putExtra(FloatingWindowService.EXTRA_ACCENT_DARK,   accent.darkPrimary.value.toLong())
            putExtra(FloatingWindowService.EXTRA_ACCENT_CONTAINER, accent.darkContainer.value.toLong())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else
            context.startService(intent)
        _isFloating.value = true
    }

    fun stopFloatingWindow(context: Context) {
        context.stopService(Intent(context, FloatingWindowService::class.java))
        _isFloating.value = false
    }

    fun onOverlayPermissionResult(context: Context) {
        _needsOverlayPermission.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context))
            startFloatingWindow(context)
    }

    private fun broadcastDownloadList(list: List<DownloadItem>, accent: AccentColour) {
        val activeItems = list.filter { it.status == DownloadStatus.DOWNLOADING }
        getApplication<Application>().sendBroadcast(
            Intent(FloatingWindowService.ACTION_UPDATE_DOWNLOADS).apply {
                putExtra(FloatingWindowService.EXTRA_COUNT, activeItems.size)
                putExtra(FloatingWindowService.EXTRA_DOWNLOAD_TITLES,
                    activeItems.map { it.title }.toTypedArray())
                putExtra(FloatingWindowService.EXTRA_DOWNLOAD_PROGRESSES,
                    activeItems.map { it.progress }.toIntArray())
                putExtra(FloatingWindowService.EXTRA_ACCENT_DARK,   accent.darkPrimary.value.toLong())
                putExtra(FloatingWindowService.EXTRA_ACCENT_CONTAINER, accent.darkContainer.value.toLong())
            }
        )
    }

    // ── Settings setters ──────────────────────────────────────────────────────
    fun setTheme(theme: AppTheme)              = settings.setTheme(theme)
    fun setAccentColour(accent: AccentColour)  = settings.setAccentColour(accent)
    fun setConfirmExit(value: Boolean)         = settings.setConfirmExit(value)
    fun setWifiOnly(value: Boolean)            = settings.setWifiOnly(value)
    fun setDownloadPath(path: String)          = settings.setDownloadPath(path)
    fun setSequentialQueue(value: Boolean)     = settings.setSequentialQueue(value)
    fun setFilenameTemplate(template: String)  = settings.setFilenameTemplate(template)
    fun setDownloadSubtitles(value: Boolean)   = settings.setDownloadSubtitles(value)
    fun setAutoQualityEnabled(value: Boolean)  = settings.setAutoQualityEnabled(value)
    fun setPreferredResolution(res: String)    = settings.setPreferredResolution(res)
    fun setPreferredFormat(fmt: String)        = settings.setPreferredFormat(fmt)

    fun setMaxConcurrent(value: Int) {
        val cap = com.snaploader.app.license.FeatureAccess.maxConcurrentFor(_licenseTier.value)
        if (value > cap) {
            _lockedFeatureTier.value = if (cap < 3) com.snaploader.app.license.SHVLicense.Tier.PRO
                                       else com.snaploader.app.license.SHVLicense.Tier.PRO_PLUS
            return
        }
        settings.setMaxConcurrent(value)
    }

    // ── License / SHVertex account (additive) ───────────────────────────────

    private fun bootAccountAndLicense() {
        val ctx = getApplication<Application>()
        val code = com.snaploader.app.license.SHVLicense.getDeviceCode(ctx)
        val remembered = com.snaploader.app.license.SHVAccount.rememberedEmail(ctx)
        _accountChecking.value = true
        _deviceCode.value = code
        _loginEmailDraft.value = remembered ?: ""
        _loginRememberMe.value = remembered != null
        viewModelScope.launch {
            val status = com.snaploader.app.license.SHVAccount.getAccessStatus(ctx)
            _accountChecking.value = false
            _accountLoggedIn.value = status.loggedIn
            _accountEmail.value = status.email
            _licenseTier.value = status.tier
        }
    }

    /**
     * Full account + license + revocation recheck. Call this whenever the
     * user returns to Home (e.g. from onResume / bottom-nav Home tap). Runs
     * silently — only surfaces UI if [forceUiSpinner] is set (manual
     * "Refresh" tap) or something is actually wrong.
     */
    fun refreshAccessSilently(forceUiSpinner: Boolean = false) {
        val ctx = getApplication<Application>()
        if (forceUiSpinner) _licenseChecking.value = true
        viewModelScope.launch {
            val status = com.snaploader.app.license.SHVAccount.getAccessStatus(ctx)
            _licenseChecking.value = false
            _accountLoggedIn.value = status.loggedIn
            _accountEmail.value = status.email
            _licenseTier.value = status.tier
        }
    }

    fun onLoginEmailChange(v: String) { _loginEmailDraft.value = v; _loginError.value = null }
    fun onLoginPasswordChange(v: String) { _loginPasswordDraft.value = v; _loginError.value = null }
    fun onToggleRememberMe(v: Boolean) { _loginRememberMe.value = v }

    fun signIn() {
        val email = _loginEmailDraft.value.trim()
        val password = _loginPasswordDraft.value
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Enter your email and password."
            return
        }
        _loginBusy.value = true; _loginError.value = null
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val session = com.snaploader.app.license.SHVAccount.signIn(ctx, email, password, _loginRememberMe.value)
                _loginBusy.value = false
                _loginPasswordDraft.value = ""
                _accountLoggedIn.value = true
                _accountEmail.value = session.email
                refreshAccessSilently()
            } catch (e: Exception) {
                _loginBusy.value = false
                _loginError.value = e.message ?: "Sign in failed."
            }
        }
    }

    fun signOut() {
        val ctx = getApplication<Application>()
        com.snaploader.app.license.SHVAccount.signOut(ctx)
        _accountLoggedIn.value = false
        _accountEmail.value = null
        _licenseTier.value = com.snaploader.app.license.SHVLicense.Tier.FREE
        _showLicenseDialog.value = false
    }

    fun showLicenseDialog() { _showLicenseDialog.value = true }
    fun dismissLicenseDialog() { _showLicenseDialog.value = false }
    fun dismissLockedDialog() { _lockedFeatureTier.value = null }
    fun onSelectRequestTier(t: com.snaploader.app.license.SHVLicense.Tier) { _requestTierSelected.value = t }
    fun onActivationCodeChange(v: String) { _activationCodeDraft.value = v; _activationMessage.value = null }

    fun activateLicense() {
        val code = _activationCodeDraft.value.trim()
        if (code.isBlank()) { _activationMessage.value = "Paste an activation code first."; return }
        _activationBusy.value = true; _activationMessage.value = null
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val result = com.snaploader.app.license.SHVLicense.checkLicense(code, ctx, checkRevocation = true)
            if (result.valid) {
                val (payload, _) = com.snaploader.app.license.SHVLicense.decodeTokenPublic(code)
                com.snaploader.app.license.SHVLicense.saveLicense(ctx, code, payload)
                _activationBusy.value = false
                _activationMessage.value = "License verified."
                _licenseTier.value = result.tier
            } else {
                _activationBusy.value = false
                _activationMessage.value = result.message
            }
        }
    }

    // ── Gating helpers — the single chokepoint every gated action goes through ──

    private fun gate(required: com.snaploader.app.license.SHVLicense.Tier, allowed: Boolean, action: () -> Unit) {
        if (allowed) action() else _lockedFeatureTier.value = required
    }

    fun attemptBatchDownload(urls: List<String>) {
        gate(com.snaploader.app.license.SHVLicense.Tier.PRO, com.snaploader.app.license.FeatureAccess.canBatchDownload(_licenseTier.value)) {
            fetchQualitiesForUrls(urls)
        }
    }

    fun attemptPasteFromClipboard(text: String) {
        val urls = Regex("""https?://[^\s,\n]+""").findAll(text).map { it.value.trimEnd(',', '.', ')') }.toList()
        if (urls.size > 1) attemptBatchDownload(urls) else pasteFromClipboard(text)
    }

    fun attemptToggleFloatingWindow(context: Context) {
        gate(com.snaploader.app.license.SHVLicense.Tier.PRO_PLUS, com.snaploader.app.license.FeatureAccess.canUseFloatingWindow(_licenseTier.value)) {
            toggleFloatingWindow(context)
        }
    }

    fun attemptSetAccentColour(accent: AccentColour) {
        gate(com.snaploader.app.license.SHVLicense.Tier.PRO, com.snaploader.app.license.FeatureAccess.canCustomizeAccent(_licenseTier.value)) {
            setAccentColour(accent)
        }
    }

    fun attemptAdvancedSetting(action: () -> Unit) {
        gate(com.snaploader.app.license.SHVLicense.Tier.PRO, com.snaploader.app.license.FeatureAccess.canUseAdvancedSettings(_licenseTier.value), action)
    }

    fun isAdBlockAllowed(): Boolean = com.snaploader.app.license.FeatureAccess.canUseAdBlock(_licenseTier.value)

    fun attemptAddCustomSite(action: () -> Unit) {
        gate(com.snaploader.app.license.SHVLicense.Tier.PRO_PLUS, com.snaploader.app.license.FeatureAccess.canAddCustomSite(_licenseTier.value), action)
    }

    private fun extractUrls(text: String): List<String> =
        Regex("""https?://[^\s,\n]+""")
            .findAll(text)
            .map { it.value.trimEnd(',', '.', ')') }
            .toList()
}
