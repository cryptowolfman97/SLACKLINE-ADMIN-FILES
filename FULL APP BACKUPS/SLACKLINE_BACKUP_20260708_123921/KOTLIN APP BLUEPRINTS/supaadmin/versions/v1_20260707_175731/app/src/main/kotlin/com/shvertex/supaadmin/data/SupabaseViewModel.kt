package com.shvertex.supaadmin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// ── Cache ─────────────────────────────────────────────────────────────────────

data class CacheEntry<T>(
    val payload: T?,
    val error: String?,
    val loadedAt: String
)

// ── UI State ──────────────────────────────────────────────────────────────────

data class SupabaseUiState(
    // ── Navigation
    val activeSection: NavSection              = NavSection.HOME,
    val activeScreen: NavScreen                = NavScreen.DASHBOARD,

    // ── Core
    val cfg: SupabaseConfig                    = SupabaseConfig(),
    val settings: SupabaseSettings             = SupabaseSettings(),
    val isLoading: Boolean                     = false,
    val loadingMessage: String                 = "Loading...",
    val snackMessage: String?                  = null,
    val snackIsError: Boolean                  = false,
    val tabError: String?                      = null,
    val pinNeedsSetup: Boolean                 = false,
    val pinUnlocked: Boolean                   = false,

    // ── Credential form
    val credDraft: SupabaseConfig              = SupabaseConfig(),

    // ── Saved connections
    val savedConnections: List<SavedConnection> = emptyList(),
    val showSaveConnectionDialog: Boolean      = false,
    val connectionNicknameDraft: String        = "",

    // ── Module caches
    val overviewCache: CacheEntry<OverviewPayload>?              = null,
    val projectsCache: CacheEntry<List<SupabaseProject>>?        = null,
    val usersCache: CacheEntry<List<SupabaseUser>>?              = null,
    val tablesCache: CacheEntry<List<SupabaseTable>>?            = null,
    val storageCache: CacheEntry<List<SupabaseBucket>>?          = null,
    val functionsCache: CacheEntry<List<SupabaseFunction>>?      = null,
    val secretsCache: CacheEntry<List<SupabaseSecret>>?          = null,
    val policiesCache: CacheEntry<List<SupabaseRlsPolicy>>?      = null,
    val migrationsCache: CacheEntry<List<SupabaseMigration>>?    = null,
    val usageCache: CacheEntry<UsagePayload>?                    = null,
    val logsCache: CacheEntry<LogServicePayload>?                = null,
    val cronCache: CacheEntry<List<CronJob>>?                    = null,
    val webhooksCache: CacheEntry<List<DatabaseWebhook>>?        = null,

    // ── SQL
    val sqlQuery: String                       = "SELECT * FROM auth.users LIMIT 10;",
    val sqlResult: String                      = "",
    val sqlRunning: Boolean                    = false,
    val sqlHistory: List<String>               = emptyList(),
    val sqlResultRows: List<Map<String, Any>>  = emptyList(),
    val sqlResultColumns: List<String>         = emptyList(),

    // ── SQL Snippets
    val sqlSnippets: List<SqlSnippet>          = emptyList(),
    val showSnippetsPanel: Boolean             = false,
    val showSaveSnippetDialog: Boolean         = false,
    val snippetNameDraft: String               = "",

    // ── Schema Dump
    val schemaDump: String                     = "",
    val schemaDumpRunning: Boolean             = false,
    val schemaDumpSection: String              = "All",

    // ── CSV Export
    val csvExportRunning: Boolean              = false,
    val showCsvExportDialog: Boolean           = false,
    val csvExportTableName: String             = "",
    val csvExportLimit: Int                    = 10000,
    val showSqlCsvExportDialog: Boolean        = false,

    // ── Logs controls
    val logService: String                     = "API Gateway",
    val logRange: String                       = "24 Hours",
    val logLimit: Int                          = 10,

    // ── Filters
    val userSearch: String                     = "",
    val tableSearch: String                    = "",
    val tableSchemaFilter: String              = "All",
    val bucketSearch: String                   = "",
    val functionSearch: String                 = "",
    val secretSearch: String                   = "",
    val policySearch: String                   = "",
    val cronSearch: String                     = "",
    val webhookSearch: String                  = "",

    // ── Table detail + data grid
    val activeTableForDetail: SupabaseTable?   = null,
    val tableColumns: List<SupabaseColumn>     = emptyList(),
    val tableColumnsLoading: Boolean           = false,
    val tableDataRows: List<Map<String, Any>>  = emptyList(),
    val tableDataLoading: Boolean              = false,
    val tableDataPage: Int                     = 0,
    val tableDataHasMore: Boolean              = false,
    val tableSortColumn: String                = "",
    val tableSortAsc: Boolean                  = true,

    // ── Table preview
    val tablePreviewCache: Map<String, List<Any>> = emptyMap(),
    val activeTablePreview: TablePreviewDialog?   = null,

    // ── Storage browser
    val activeBucket: SupabaseBucket?          = null,
    val bucketObjects: List<StorageObject>     = emptyList(),
    val bucketPath: String                     = "",
    val bucketObjectsLoading: Boolean          = false,
    val uploadProgress: Float                  = 0f,
    val isUploading: Boolean                   = false,

    // ── Realtime
    val realtimeActive: Boolean                = false,
    val realtimeSchema: String                 = "public",
    val realtimeTable: String                  = "",
    val realtimeEvents: List<RealtimeEvent>    = emptyList(),
    val realtimeError: String?                 = null,

    // ── Global search
    val showGlobalSearch: Boolean              = false,
    val globalSearchQuery: String              = "",
    val globalSearchResults: List<GlobalSearchResult> = emptyList(),

    // ── Dialogs
    val pendingAction: PendingAction?          = null,
    val updateRowContext: UpdateRowContext?     = null,
    val insertRowContext: InsertRowContext?     = null,
    val showCreateSecret: Boolean              = false,
    val showCreateUser: Boolean                = false,
    val showAddSecretPayload: Boolean          = false,
    val showFunctionInvoker: Boolean           = false,
    val activeFunctionSlug: String             = "",
    val showCreateBucket: Boolean              = false,
    val showCreateCronJob: Boolean             = false,
    val showDeleteRowConfirm: Boolean          = false,
    val deleteRowTarget: Pair<String, String>? = null,

    // ── Settings
    val settingsTimeoutDraft: String           = "40",
    val settingsPreviewRowsDraft: String       = "5",
    val showSetPinDialog: Boolean              = false,

    // ── WebView
    val webViewUrl: String                     = "",
    val webViewCredentials: WebViewCredentials = WebViewCredentials(),
    val webViewCredDraft: WebViewCredentials   = WebViewCredentials(),
    val showWebViewCredDialog: Boolean         = false
)

class SupabaseViewModel : ViewModel() {

    private val _state = MutableStateFlow(SupabaseUiState())
    val state: StateFlow<SupabaseUiState> = _state.asStateFlow()
    private var realtimeJob: Job? = null

    // ── Boot ──────────────────────────────────────────────────────────────

    fun boot() {
        val cfg      = SupabaseStorage.loadConfig()
        val settings = SupabaseStorage.loadSettings()
        val conns    = SupabaseStorage.loadConnections()
        val snippets = SupabaseStorage.loadSqlSnippets()
        val wvCreds  = SupabaseStorage.loadWebViewCredentials()
        val hasPin   = settings.app_pin.isNotBlank()
        _state.update {
            it.copy(
                cfg = cfg, settings = settings, credDraft = cfg,
                savedConnections = conns,
                sqlSnippets = snippets,
                webViewCredentials = wvCreds,
                webViewCredDraft = wvCreds,
                settingsTimeoutDraft     = settings.timeout_seconds.toString(),
                settingsPreviewRowsDraft = settings.table_preview_rows.toString(),
                pinNeedsSetup = !hasPin,
                pinUnlocked   = false
            )
        }
    }

    fun unlockPin(pin: String): Boolean {
        val correct = _state.value.settings.app_pin
        return if (pin == correct) {
            _state.update { it.copy(pinUnlocked = true) }
            true
        } else false
    }

    fun setupPin(pin: String) {
        val s = SupabaseStorage.loadSettings().copy(app_pin = pin)
        SupabaseStorage.saveSettings(s)
        _state.update { it.copy(settings = s, pinNeedsSetup = false, pinUnlocked = true) }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    fun navigateTo(screen: NavScreen) {
        val section = screenToSection(screen)
        _state.update { it.copy(activeSection = section, activeScreen = screen, tabError = null) }
        onScreenEntered(screen)
    }

    fun navigateToSection(section: NavSection) {
        val screen = sectionDefaultScreen(section)
        _state.update { it.copy(activeSection = section, activeScreen = screen, tabError = null) }
        onScreenEntered(screen)
    }

    fun navigateToWebDashboard(targetPath: String = "") {
        val ref = _state.value.cfg.currentRef
        val base = "https://supabase.com/dashboard/project/${ref.ifBlank { "" }}"
        val url = if (targetPath.isNotBlank()) "$base$targetPath" else base
        _state.update { it.copy(
            activeSection = NavSection.MORE,
            activeScreen  = NavScreen.WEB_DASHBOARD,
            webViewUrl    = url
        )}
    }

    private fun onScreenEntered(screen: NavScreen) {
        when (screen) {
            NavScreen.OVERVIEW    -> if (_state.value.overviewCache  == null) loadOverview()
            NavScreen.PROJECTS    -> if (_state.value.projectsCache  == null) loadProjects()
            NavScreen.USERS       -> if (_state.value.usersCache     == null) loadUsers()
            NavScreen.TABLES      -> if (_state.value.tablesCache    == null) loadTables()
            NavScreen.STORAGE     -> if (_state.value.storageCache   == null) loadStorage()
            NavScreen.FUNCTIONS   -> if (_state.value.functionsCache == null) loadFunctions()
            NavScreen.SECRETS     -> if (_state.value.secretsCache   == null) loadSecrets()
            NavScreen.POLICIES    -> if (_state.value.policiesCache  == null) loadPolicies()
            NavScreen.MIGRATIONS  -> if (_state.value.migrationsCache == null) loadMigrations()
            NavScreen.USAGE       -> if (_state.value.usageCache     == null) loadUsage()
            NavScreen.CRON        -> if (_state.value.cronCache      == null) loadCronJobs()
            NavScreen.WEBHOOKS    -> if (_state.value.webhooksCache  == null) loadWebhooks()
            else -> Unit
        }
    }

    fun forceRefresh() {
        when (_state.value.activeScreen) {
            NavScreen.OVERVIEW    -> loadOverview()
            NavScreen.PROJECTS    -> loadProjects()
            NavScreen.USERS       -> loadUsers()
            NavScreen.TABLES      -> loadTables()
            NavScreen.STORAGE     -> loadStorage()
            NavScreen.FUNCTIONS   -> loadFunctions()
            NavScreen.SECRETS     -> loadSecrets()
            NavScreen.POLICIES    -> loadPolicies()
            NavScreen.MIGRATIONS  -> loadMigrations()
            NavScreen.USAGE       -> loadUsage()
            NavScreen.LOGS        -> loadLogs()
            NavScreen.CRON        -> loadCronJobs()
            NavScreen.WEBHOOKS    -> loadWebhooks()
            else -> Unit
        }
    }

    private fun screenToSection(screen: NavScreen): NavSection = when (screen) {
        NavScreen.DASHBOARD, NavScreen.OVERVIEW, NavScreen.PROJECTS, NavScreen.USAGE -> NavSection.HOME
        NavScreen.TABLES, NavScreen.SQL, NavScreen.POLICIES, NavScreen.MIGRATIONS, NavScreen.CRON, NavScreen.WEBHOOKS -> NavSection.DATABASE
        NavScreen.USERS, NavScreen.SECRETS -> NavSection.AUTH
        NavScreen.STORAGE, NavScreen.FUNCTIONS, NavScreen.LOGS, NavScreen.REALTIME -> NavSection.DEVTOOLS
        NavScreen.CONNECTIONS, NavScreen.CREDENTIALS, NavScreen.WEB_DASHBOARD, NavScreen.SETTINGS -> NavSection.MORE
    }

    private fun sectionDefaultScreen(section: NavSection): NavScreen = when (section) {
        NavSection.HOME     -> NavScreen.DASHBOARD
        NavSection.DATABASE -> NavScreen.TABLES
        NavSection.AUTH     -> NavScreen.USERS
        NavSection.DEVTOOLS -> NavScreen.STORAGE
        NavSection.MORE     -> NavScreen.CONNECTIONS
    }

    // Backward-compat helper for any legacy code using old SupabaseTab references
    fun switchTab(screen: NavScreen) = navigateTo(screen)

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun api() = SupabaseApi(SupabaseStorage.loadConfig(), _state.value.settings.timeout_seconds)

    private fun now(): String {
        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        return "%04d-%02d-%02d %02d:%02d".format(now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute)
    }

    fun snack(msg: String, isError: Boolean = false) =
        _state.update { it.copy(snackMessage = msg, snackIsError = isError) }

    fun clearSnack() = _state.update { it.copy(snackMessage = null) }

    private fun <T> async(loadingMsg: String, block: suspend () -> T, onSuccess: (T) -> Unit, onError: (String) -> Unit = { snack(it, true) }) {
        _state.update { it.copy(isLoading = true, loadingMessage = loadingMsg, tabError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = block()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isLoading = false) }
                    onSuccess(result)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isLoading = false) }
                    onError(msg)
                }
            }
        }
    }

    // ── Credentials ───────────────────────────────────────────────────────

    fun updateCredDraft(field: String, value: String) {
        val d = _state.value.credDraft
        val updated = when (field) {
            "project_url"           -> d.copy(project_url = value)
            "project_ref"           -> d.copy(project_ref = value)
            "anon_key"              -> d.copy(anon_key = value)
            "personal_access_token" -> d.copy(personal_access_token = value)
            "project_admin_key"     -> d.copy(project_admin_key = value)
            "email"                 -> d.copy(email = value)
            "password"              -> d.copy(password = value)
            else                    -> d
        }
        _state.update { it.copy(credDraft = updated) }
    }

    fun inferRefFromUrl() {
        val url = _state.value.credDraft.project_url
        val ref = SupabaseConfig.guessRef(url)
        _state.update { it.copy(credDraft = _state.value.credDraft.copy(project_ref = ref)) }
    }

    fun saveCredentials() {
        val draft = _state.value.credDraft
        val url = SupabaseConfig.normalizeUrl(draft.project_url)
        val ref = draft.project_ref.trim().ifBlank { SupabaseConfig.guessRef(url) }
        val saved = draft.copy(project_url = url, project_ref = ref)
        SupabaseStorage.saveConfig(saved)
        _state.update { it.copy(cfg = saved, credDraft = saved) }
        snack("Credentials saved.")
    }

    fun clearCredentialDraft() {
        _state.update { it.copy(credDraft = SupabaseConfig()) }
    }

    fun reloadCredentials() {
        val cfg = SupabaseStorage.loadConfig()
        _state.update { it.copy(cfg = cfg, credDraft = cfg) }
        snack("Credentials reloaded from disk.")
    }

    fun deleteCredentials() {
        SupabaseStorage.deleteConfig()
        val fresh = SupabaseConfig()
        _state.update { it.copy(cfg = fresh, credDraft = fresh) }
        clearCache()
        snack("Credentials deleted.")
        navigateTo(NavScreen.CREDENTIALS)
    }

    // ── Saved Connections ─────────────────────────────────────────────────

    fun showSaveConnection() = _state.update { it.copy(showSaveConnectionDialog = true, connectionNicknameDraft = "") }
    fun dismissSaveConnection() = _state.update { it.copy(showSaveConnectionDialog = false) }
    fun updateConnectionNickname(v: String) = _state.update { it.copy(connectionNicknameDraft = v) }

    fun saveCurrentAsConnection() {
        val cfg = _state.value.cfg
        val nickname = _state.value.connectionNicknameDraft.trim().ifBlank {
            cfg.currentRef.ifBlank { "Unnamed" }
        }
        val conn = SavedConnection(
            id = System.currentTimeMillis().toString(),
            nickname = nickname,
            project_url = cfg.project_url,
            project_ref = cfg.project_ref,
            personal_access_token = cfg.personal_access_token,
            project_admin_key = cfg.project_admin_key,
            anon_key = cfg.anon_key,
            email = cfg.email,
            password = cfg.password,
            savedAt = now()
        )
        val list = _state.value.savedConnections.toMutableList()
        list.add(0, conn)
        SupabaseStorage.saveConnections(list)
        _state.update { it.copy(savedConnections = list, showSaveConnectionDialog = false) }
        snack("Connection \"$nickname\" saved.")
    }

    fun loadSavedConnection(conn: SavedConnection) {
        val cfg = SupabaseConfig(
            project_url = conn.project_url,
            project_ref = conn.project_ref,
            personal_access_token = conn.personal_access_token,
            project_admin_key = conn.project_admin_key,
            anon_key = conn.anon_key,
            email = conn.email,
            password = conn.password
        )
        SupabaseStorage.saveConfig(cfg)
        clearCache()
        _state.update { it.copy(cfg = cfg, credDraft = cfg) }
        snack("Loaded: ${conn.nickname}")
        navigateTo(NavScreen.DASHBOARD)
    }

    fun deleteSavedConnection(id: String) {
        val list = _state.value.savedConnections.filter { it.id != id }
        SupabaseStorage.saveConnections(list)
        SupabaseStorage.deleteConnection(id)
        _state.update { it.copy(savedConnections = list) }
        snack("Connection removed.")
    }

    // ── Connection tests ──────────────────────────────────────────────────

    fun testPat() {
        val draft = _state.value.credDraft
        async("Testing PAT...",
            { SupabaseApi(draft, _state.value.settings.timeout_seconds).testPat() },
            { count -> snack("✓ PAT works. $count project(s) returned.") }
        )
    }

    fun testProjectKey() {
        val draft = _state.value.credDraft
        async("Testing service key...",
            { SupabaseApi(draft, _state.value.settings.timeout_seconds).testProjectKey() },
            { count -> snack("✓ Service key works. Preview users returned: $count") }
        )
    }

    fun testCloudAuth() {
        val draft = _state.value.credDraft
        async("Testing cloud auth...",
            { SupabaseApi(draft, _state.value.settings.timeout_seconds).testCloudAuth() },
            { email -> snack("✓ Signed in as $email") }
        )
    }

    // ── Module loaders ────────────────────────────────────────────────────

    fun loadOverview() = async("Loading overview...", { api().overviewPayload() }, { result ->
        _state.update { it.copy(overviewCache = CacheEntry(result, null, now())) }
    }, { err -> _state.update { it.copy(overviewCache = CacheEntry(null, err, now()), tabError = err) } })

    fun loadProjects() = async("Loading projects...", { api().listProjects() }, { rows ->
        _state.update { it.copy(projectsCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(projectsCache = CacheEntry(null, err, now()), tabError = err) } })

    fun useProject(proj: SupabaseProject) {
        val ref = proj.ref.ifBlank { return }
        val cfg = _state.value.cfg
        val updated = cfg.copy(
            project_ref = ref,
            project_url = cfg.project_url.ifBlank { "https://$ref.supabase.co" }
        )
        SupabaseStorage.saveConfig(updated)
        _state.update { it.copy(cfg = updated, credDraft = updated) }
        snack("Project set to $ref")
    }

    fun loadUsers() = async("Loading users...", { api().listUsers(500) }, { rows ->
        _state.update { it.copy(usersCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(usersCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setUserSearch(query: String) = _state.update { it.copy(userSearch = query) }

    fun loadTables() = async("Loading tables...", { api().listTables() }, { rows ->
        _state.update { it.copy(tablesCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(tablesCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setTableSearch(query: String) = _state.update { it.copy(tableSearch = query) }
    fun setTableSchemaFilter(schema: String) = _state.update { it.copy(tableSchemaFilter = schema) }

    fun openTableDetail(table: SupabaseTable) {
        _state.update { it.copy(
            activeTableForDetail = table,
            tableColumns = emptyList(),
            tableColumnsLoading = true,
            tableDataRows = emptyList(),
            tableDataPage = 0,
            tableDataHasMore = false,
            tableSortColumn = "",
            tableSortAsc = true
        )}
        viewModelScope.launch(Dispatchers.IO) {
            val cols = try { api().listTableColumns(table.name, table.schema) } catch (_: Exception) { emptyList() }
            val rows = try { api().fetchTableRows(table.name, table.schema, 50, 0) } catch (_: Exception) { emptyList<Map<String, Any>>() }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(
                    tableColumns = cols,
                    tableColumnsLoading = false,
                    tableDataRows = rows,
                    tableDataLoading = false,
                    tableDataHasMore = rows.size >= 50
                )}
            }
        }
    }

    fun loadMoreTableData() {
        val table = _state.value.activeTableForDetail ?: return
        val currentRows = _state.value.tableDataRows
        val offset = currentRows.size
        _state.update { it.copy(tableDataLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val rows = try { api().fetchTableRows(table.name, table.schema, 50, offset) } catch (_: Exception) { emptyList<Map<String, Any>>() }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(
                    tableDataRows = currentRows + rows,
                    tableDataLoading = false,
                    tableDataHasMore = rows.size >= 50
                )}
            }
        }
    }

    fun setTableSort(column: String) {
        val asc = if (_state.value.tableSortColumn == column) !_state.value.tableSortAsc else true
        _state.update { it.copy(tableSortColumn = column, tableSortAsc = asc) }
    }

    fun closeTableDetail() = _state.update { it.copy(activeTableForDetail = null, tableColumns = emptyList(), tableDataRows = emptyList()) }

    fun loadStorage() = async("Loading buckets...", { api().listBuckets() }, { rows ->
        _state.update { it.copy(storageCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(storageCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setBucketSearch(q: String) = _state.update { it.copy(bucketSearch = q) }

    fun openBucket(bucket: SupabaseBucket) {
        _state.update { it.copy(activeBucket = bucket, bucketPath = "", bucketObjects = emptyList(), bucketObjectsLoading = true) }
        loadBucketObjects(bucket.id, "")
    }

    fun closeBucket() = _state.update { it.copy(activeBucket = null, bucketObjects = emptyList(), bucketPath = "") }

    fun loadBucketObjects(bucketId: String, prefix: String) {
        _state.update { it.copy(bucketObjectsLoading = true, bucketPath = prefix) }
        viewModelScope.launch(Dispatchers.IO) {
            val objects = try { api().listBucketObjects(bucketId, prefix) } catch (_: Exception) { emptyList() }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(bucketObjects = objects, bucketObjectsLoading = false) }
            }
        }
    }

    fun showCreateBucket() = _state.update { it.copy(showCreateBucket = true) }
    fun dismissCreateBucket() = _state.update { it.copy(showCreateBucket = false) }

    fun createBucket(name: String, public: Boolean) {
        if (name.isBlank()) { snack("Bucket name required.", true); return }
        async("Creating bucket $name...", { api().createBucket(name, public) }, {
            snack("Bucket \"$name\" created.")
            loadStorage()
        })
    }

    fun deleteBucket(bucketId: String) = async("Deleting bucket...", { api().deleteBucket(bucketId) }, {
        snack("Bucket deleted.")
        loadStorage()
    })

    fun emptyBucket(bucketId: String) = async("Emptying bucket...", { api().emptyBucket(bucketId) }, {
        snack("Bucket emptied.")
    })

    fun deleteStorageObject(bucketId: String, path: String) {
        async("Deleting object...", { api().deleteStorageObject(bucketId, path) }, {
            snack("Object deleted.")
            loadBucketObjects(bucketId, _state.value.bucketPath)
        })
    }

    fun uploadStorageObject(bucketId: String, path: String, fileBytes: ByteArray, mimeType: String) {
        _state.update { it.copy(isUploading = true, uploadProgress = 0f) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                api().uploadStorageObject(bucketId, path, fileBytes, mimeType)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isUploading = false, uploadProgress = 1f) }
                    snack("Uploaded: $path")
                    loadBucketObjects(bucketId, _state.value.bucketPath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isUploading = false, uploadProgress = 0f) }
                    snack("Upload failed: ${e.message}", true)
                }
            }
        }
    }

    fun loadFunctions() = async("Loading functions...", { api().listFunctions() }, { rows ->
        _state.update { it.copy(functionsCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(functionsCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setFunctionSearch(q: String) = _state.update { it.copy(functionSearch = q) }

    fun showInvokeFunction(slug: String) = _state.update { it.copy(showFunctionInvoker = true, activeFunctionSlug = slug) }
    fun dismissFunctionInvoker() = _state.update { it.copy(showFunctionInvoker = false, activeFunctionSlug = "") }

    fun invokeFunction(slug: String, bodyJson: String) {
        _state.update { it.copy(showFunctionInvoker = false) }
        val parsedBody = try {
            com.google.gson.Gson().fromJson(bodyJson, Map::class.java) as? Map<String, Any> ?: emptyMap()
        } catch (_: Exception) { emptyMap<String, Any>() }
        async("Invoking $slug...", { api().invokeFunction(slug, parsedBody) }, { result ->
            _state.update { it.copy(sqlResult = result) }
            navigateTo(NavScreen.SQL)
            snack("Function $slug invoked. Result in SQL panel.")
        })
    }

    fun loadSecrets() = async("Loading secrets...", { api().listSecrets() }, { rows ->
        _state.update { it.copy(secretsCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(secretsCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setSecretSearch(q: String) = _state.update { it.copy(secretSearch = q) }

    fun createSecret(name: String, value: String) {
        if (name.isBlank() || value.isBlank()) { snack("Name and value required.", true); return }
        async("Creating secret...", { api().createSecret(name, value) }, {
            snack("Secret \"$name\" created.")
            loadSecrets()
        })
    }

    fun deleteSecret(name: String) = async("Deleting secret...", { api().deleteSecret(name) }, {
        snack("Secret \"$name\" deleted.")
        loadSecrets()
    })

    fun loadPolicies() = async("Loading RLS policies...", { api().listRlsPolicies() }, { rows ->
        _state.update { it.copy(policiesCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(policiesCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setPolicySearch(q: String) = _state.update { it.copy(policySearch = q) }

    fun loadMigrations() = async("Loading migrations...", { api().listMigrations() }, { rows ->
        _state.update { it.copy(migrationsCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(migrationsCache = CacheEntry(null, err, now()), tabError = err) } })

    fun loadUsage() = async("Loading usage...", { api().usagePayload() }, { result ->
        _state.update { it.copy(usageCache = CacheEntry(result, null, now())) }
    }, { err -> _state.update { it.copy(usageCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setLogService(service: String) = _state.update { it.copy(logService = service) }
    fun setLogRange(range: String)     = _state.update { it.copy(logRange = range) }
    fun setLogLimit(limit: Int)        = _state.update { it.copy(logLimit = limit) }

    fun loadLogs() = async(
        "Loading logs – ${_state.value.logService}...",
        { api().logPayload(_state.value.logService, _state.value.logRange, _state.value.logLimit) },
        { result -> _state.update { it.copy(logsCache = CacheEntry(result, result.error, now())) } },
        { err -> _state.update { it.copy(logsCache = CacheEntry(null, err, now()), tabError = err) } }
    )

    // ── Cron Jobs ─────────────────────────────────────────────────────────

    fun loadCronJobs() = async("Loading cron jobs...", { api().listCronJobs() }, { rows ->
        _state.update { it.copy(cronCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(cronCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setCronSearch(q: String) = _state.update { it.copy(cronSearch = q) }
    fun showCreateCronJob() = _state.update { it.copy(showCreateCronJob = true) }
    fun dismissCreateCronJob() = _state.update { it.copy(showCreateCronJob = false) }

    fun createCronJob(name: String, schedule: String, command: String) {
        if (name.isBlank() || schedule.isBlank() || command.isBlank()) {
            snack("All fields required.", true); return
        }
        async("Creating cron job...", { api().createCronJob(name, schedule, command) }, {
            snack("Cron job \"$name\" created.")
            loadCronJobs()
        })
    }

    fun deleteCronJob(jobName: String) = async("Deleting cron job...", { api().deleteCronJob(jobName) }, {
        snack("Cron job \"$jobName\" deleted.")
        loadCronJobs()
    })

    fun toggleCronJob(jobId: Long, active: Boolean) = async(
        if (active) "Enabling cron job..." else "Disabling cron job...",
        { api().toggleCronJob(jobId, active) },
        {
            snack(if (active) "Cron job enabled." else "Cron job disabled.")
            loadCronJobs()
        }
    )

    // ── Webhooks ──────────────────────────────────────────────────────────

    fun loadWebhooks() = async("Loading webhooks...", { api().listWebhooks() }, { rows ->
        _state.update { it.copy(webhooksCache = CacheEntry(rows, null, now())) }
    }, { err -> _state.update { it.copy(webhooksCache = CacheEntry(null, err, now()), tabError = err) } })

    fun setWebhookSearch(q: String) = _state.update { it.copy(webhookSearch = q) }

    fun deleteWebhook(id: Long, name: String) = async("Deleting webhook...", { api().deleteWebhook(id) }, {
        snack("Webhook \"$name\" deleted.")
        loadWebhooks()
    })

    // ── Realtime ──────────────────────────────────────────────────────────

    fun setRealtimeSchema(s: String) = _state.update { it.copy(realtimeSchema = s) }
    fun setRealtimeTable(t: String) = _state.update { it.copy(realtimeTable = t) }

    fun startRealtime() {
        val cfg = _state.value.cfg
        val table = _state.value.realtimeTable.trim()
        if (table.isBlank()) { snack("Enter a table name first.", true); return }
        if (!cfg.hasUrl) { snack("Project URL required.", true); return }

        _state.update { it.copy(realtimeActive = true, realtimeError = null, realtimeEvents = emptyList()) }
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val ref = cfg.currentRef
                val key = if (cfg.hasAdmin) cfg.project_admin_key else cfg.anon_key
                val wsUrl = "wss://$ref.supabase.co/realtime/v1/websocket?apikey=$key&vsn=1.0.0"

                val request = Request.Builder().url(wsUrl).build()
                val wsClient = okhttp3.OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.SECONDS)
                    .build()

                val heartbeatRef = java.util.concurrent.atomic.AtomicLong(1)

                val listener = object : okhttp3.WebSocketListener() {
                    override fun onOpen(ws: okhttp3.WebSocket, response: okhttp3.Response) {
                        // Join channel for the table
                        val schema = _state.value.realtimeSchema.ifBlank { "public" }
                        val joinMsg = """{"topic":"realtime:$schema:$table","event":"phx_join","payload":{"config":{"broadcast":{"self":false},"presence":{"key":""},"postgres_changes":[{"event":"*","schema":"$schema","table":"$table"}]}},"ref":"${heartbeatRef.getAndIncrement()}"}"""
                        ws.send(joinMsg)
                        // Heartbeat every 30s
                        viewModelScope.launch {
                            while (_state.value.realtimeActive) {
                                delay(30_000)
                                val hb = """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${heartbeatRef.getAndIncrement()}"}"""
                                ws.send(hb)
                            }
                        }
                    }

                    override fun onMessage(ws: okhttp3.WebSocket, text: String) {
                        try {
                            val el = com.google.gson.JsonParser.parseString(text)
                            if (!el.isJsonObject) return
                            val obj = el.asJsonObject
                            val event = obj.get("event")?.asString ?: return
                            if (event !in listOf("INSERT", "UPDATE", "DELETE", "postgres_changes")) return

                            val payload = obj.getAsJsonObject("payload")
                            val data = payload?.getAsJsonObject("data") ?: payload

                            val eventType = data?.get("type")?.asString
                                ?: obj.get("event")?.asString
                                ?: "CHANGE"

                            val record = data?.getAsJsonObject("record") ?: data
                            val ts = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))

                            val evt = RealtimeEvent(
                                timestamp = ts,
                                eventType = eventType.uppercase(),
                                schema    = _state.value.realtimeSchema,
                                table     = _state.value.realtimeTable,
                                payload   = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(record)
                            )
                            viewModelScope.launch(Dispatchers.Main) {
                                val events = (_state.value.realtimeEvents + evt).takeLast(100)
                                _state.update { it.copy(realtimeEvents = events) }
                            }
                        } catch (_: Exception) { }
                    }

                    override fun onFailure(ws: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _state.update { it.copy(realtimeActive = false, realtimeError = t.message) }
                        }
                    }

                    override fun onClosed(ws: okhttp3.WebSocket, code: Int, reason: String) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _state.update { it.copy(realtimeActive = false) }
                        }
                    }
                }

                wsClient.newWebSocket(request, listener)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(realtimeActive = false, realtimeError = e.message) }
                }
            }
        }
    }

    fun stopRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
        _state.update { it.copy(realtimeActive = false) }
    }

    fun clearRealtimeEvents() = _state.update { it.copy(realtimeEvents = emptyList()) }

    // ── SQL ───────────────────────────────────────────────────────────────

    fun setSqlQuery(q: String) = _state.update { it.copy(sqlQuery = q) }

    fun runSql() {
        val query = _state.value.sqlQuery.trim()
        if (query.isBlank()) { snack("Empty query.", true); return }
        _state.update { it.copy(sqlRunning = true, sqlResult = "Executing...", sqlResultRows = emptyList(), sqlResultColumns = emptyList()) }
        val history = (_state.value.sqlHistory + query).takeLast(20).distinct()
        _state.update { it.copy(sqlHistory = history) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawEl = api().executeSqlRaw(query)
                val prettyResult = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rawEl)
                // Try to parse into rows for the data grid
                val rows: List<Map<String, Any>> = try {
                    val arr = if (rawEl.isJsonArray) rawEl.asJsonArray
                              else rawEl.asJsonObject.entrySet().firstOrNull { it.value.isJsonArray }?.value?.asJsonArray
                    arr?.mapNotNull { el ->
                        if (el.isJsonObject) com.google.gson.Gson().fromJson(el, Map::class.java) as? Map<String, Any> else null
                    } ?: emptyList()
                } catch (_: Exception) { emptyList() }
                val cols = rows.firstOrNull()?.keys?.toList() ?: emptyList()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(
                        sqlRunning = false,
                        sqlResult = prettyResult,
                        sqlResultRows = rows,
                        sqlResultColumns = cols
                    )}
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(sqlRunning = false, sqlResult = "Error: ${e.message}", sqlResultRows = emptyList(), sqlResultColumns = emptyList()) }
                }
            }
        }
    }

    fun clearSqlQuery() = _state.update { it.copy(sqlQuery = "") }

    fun openInSql(query: String) {
        _state.update { it.copy(sqlQuery = query, sqlResult = "Run the query above to see results.") }
        navigateTo(NavScreen.SQL)
    }

    fun loadSqlHistoryItem(query: String) = _state.update { it.copy(sqlQuery = query) }

    // ── SQL Snippets ──────────────────────────────────────────────────────

    fun showSnippetsPanel() = _state.update { it.copy(showSnippetsPanel = true) }
    fun dismissSnippetsPanel() = _state.update { it.copy(showSnippetsPanel = false) }
    fun showSaveSnippetDialog() = _state.update { it.copy(showSaveSnippetDialog = true, snippetNameDraft = "") }
    fun dismissSaveSnippetDialog() = _state.update { it.copy(showSaveSnippetDialog = false) }
    fun updateSnippetNameDraft(v: String) = _state.update { it.copy(snippetNameDraft = v) }

    fun saveCurrentQueryAsSnippet() {
        val query = _state.value.sqlQuery.trim()
        val name = _state.value.snippetNameDraft.trim()
        if (query.isBlank()) { snack("Write a query first.", true); return }
        if (name.isBlank()) { snack("Enter a snippet name.", true); return }
        val snippet = SqlSnippet(
            id = System.currentTimeMillis().toString(),
            name = name, sql = query, savedAt = now()
        )
        val list = _state.value.sqlSnippets.toMutableList()
        list.add(0, snippet)
        SupabaseStorage.saveSqlSnippets(list)
        _state.update { it.copy(sqlSnippets = list, showSaveSnippetDialog = false) }
        snack("Snippet \"$name\" saved.")
    }

    fun loadSnippet(snippet: SqlSnippet) {
        _state.update { it.copy(sqlQuery = snippet.sql, showSnippetsPanel = false) }
    }

    fun deleteSnippet(id: String) {
        val list = _state.value.sqlSnippets.filter { it.id != id }
        SupabaseStorage.saveSqlSnippets(list)
        _state.update { it.copy(sqlSnippets = list) }
        snack("Snippet deleted.")
    }

    // ── Schema Dump ───────────────────────────────────────────────────────

    fun setSchemaDumpSection(section: String) = _state.update { it.copy(schemaDumpSection = section) }

    fun runSchemaDump() {
        val section = _state.value.schemaDumpSection
        _state.update { it.copy(schemaDumpRunning = true, schemaDump = "Running schema dump…") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val cfg = SupabaseStorage.loadConfig()
                val ref = cfg.currentRef
                if (!cfg.hasPat || ref.isBlank()) {
                    "ERROR: PAT and project ref are required for Schema Dump.\nGo to Connection tab and save your Personal Access Token."
                } else {
                    val queries = buildSchemaDumpQueries(section)
                    val sb = StringBuilder()
                    sb.appendLine("-- SupaAdmin by SHV — Schema Dump")
                    sb.appendLine("-- Project: $ref")
                    sb.appendLine("-- Section: $section")
                    sb.appendLine("-- Generated: ${java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)}")
                    sb.appendLine()
                    queries.forEach { (label, sql) ->
                        sb.appendLine("-- ══════════════════════════════════════")
                        sb.appendLine("-- $label")
                        sb.appendLine("-- ══════════════════════════════════════")
                        try {
                            val client = okhttp3.OkHttpClient.Builder()
                                .connectTimeout(40L, java.util.concurrent.TimeUnit.SECONDS)
                                .readTimeout(40L, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            val body = com.google.gson.Gson().toJson(mapOf("query" to sql))
                                .toByteArray()
                                .let { okhttp3.RequestBody.create("application/json".toMediaType(), it) }
                            val req = okhttp3.Request.Builder()
                                .url("https://api.supabase.com/v1/projects/$ref/database/query")
                                .addHeader("Authorization", "Bearer ${cfg.personal_access_token}")
                                .addHeader("Content-Type", "application/json")
                                .post(body)
                                .build()
                            val resp = client.newCall(req).execute()
                            val respBody = resp.body?.string() ?: ""
                            if (resp.isSuccessful) {
                                try {
                                    val el = com.google.gson.JsonParser.parseString(respBody)
                                    sb.appendLine(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(el))
                                } catch (_: Exception) {
                                    sb.appendLine(respBody)
                                }
                            } else {
                                sb.appendLine("-- ERROR HTTP ${resp.code}: $respBody")
                            }
                        } catch (e: Exception) {
                            sb.appendLine("-- ERROR: ${e.message}")
                        }
                        sb.appendLine()
                    }
                    sb.toString()
                }
            } catch (e: Exception) {
                "Error running schema dump: ${e.message}"
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(schemaDumpRunning = false, schemaDump = result) }
            }
        }
    }

    fun exportSchemaDump() {
        val dump = _state.value.schemaDump
        if (dump.isBlank()) { snack("No dump to export yet.", true); return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val subDir = java.io.File(SupabaseStorage.dataDir(), "Schema Dump Files").also { it.mkdirs() }
                val ts = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val ref = _state.value.cfg.currentRef.ifBlank { "project" }
                val file = java.io.File(subDir, "schema_dump_${ref}_$ts.sql")
                file.writeText(dump)
                withContext(Dispatchers.Main) {
                    snack("Exported to Schema Dump Files/${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { snack("Export failed: ${e.message}", true) }
            }
        }
    }

    fun exportSchemaDumpCsv() {
        val dump = _state.value.schemaDump
        if (dump.isBlank()) { snack("No dump to export yet.", true); return }
        _state.update { it.copy(csvExportRunning = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ts = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val ref = _state.value.cfg.currentRef.ifBlank { "project" }
                val subDir = java.io.File(SupabaseStorage.dataDir(), "Schema Dump Files").also { it.mkdirs() }
                val csvDir = java.io.File(subDir, "schema_csv_${ref}_$ts").also { it.mkdirs() }

                val sectionPattern = Regex("""-- ══+\n-- (.+?)\n-- ══+\n(.*?)(?=\n-- ══|$)""", RegexOption.DOT_MATCHES_ALL)
                val matches = sectionPattern.findAll(dump)
                var fileCount = 0

                matches.forEach { match ->
                    val sectionName = match.groupValues[1].trim()
                        .replace("/", "_").replace(" ", "_").replace("&", "and")
                        .replace("(", "").replace(")", "").lowercase()
                    val content = match.groupValues[2].trim()
                    try {
                        val jsonStart = content.indexOf('[')
                        val jsonEnd   = content.lastIndexOf(']')
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            val arr = com.google.gson.JsonParser.parseString(
                                content.substring(jsonStart, jsonEnd + 1)
                            ).asJsonArray
                            if (arr.size() > 0) {
                                val headers = arr[0].asJsonObject.keySet().toList()
                                val sb = StringBuilder()
                                sb.appendLine(headers.joinToString(",") { escapeCsvValue(it) })
                                arr.forEach { el ->
                                    if (el.isJsonObject) {
                                        val obj = el.asJsonObject
                                        sb.appendLine(headers.joinToString(",") { col ->
                                            val v = obj.get(col)
                                            if (v == null || v.isJsonNull) "" else escapeCsvValue(v.asString)
                                        })
                                    }
                                }
                                java.io.File(csvDir, "$sectionName.csv").writeText(sb.toString())
                                fileCount++
                            }
                        }
                    } catch (_: Exception) { }
                }

                withContext(Dispatchers.Main) {
                    _state.update { it.copy(csvExportRunning = false) }
                    if (fileCount > 0)
                        snack("Exported $fileCount CSV files to Schema Dump Files/schema_csv_${ref}_$ts/")
                    else
                        snack("No parseable sections found. Run the dump first.", true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(csvExportRunning = false) }
                    snack("CSV export failed: ${e.message}", true)
                }
            }
        }
    }

    private fun escapeCsvValue(value: String): String =
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
            "\"${value.replace("\"", "\"\"")}\""
        else value

    fun clearSchemaDump() = _state.update { it.copy(schemaDump = "") }

    // ── CSV Export ────────────────────────────────────────────────────────

    fun showCsvExport(tableName: String) =
        _state.update { it.copy(showCsvExportDialog = true, csvExportTableName = tableName, csvExportLimit = 10000) }

    fun dismissCsvExport() = _state.update { it.copy(showCsvExportDialog = false) }
    fun setCsvExportLimit(limit: Int) = _state.update { it.copy(csvExportLimit = limit) }
    fun showSqlCsvExport() = _state.update { it.copy(showSqlCsvExportDialog = true) }
    fun dismissSqlCsvExport() = _state.update { it.copy(showSqlCsvExportDialog = false) }

    fun exportTableCsv(tableName: String, limit: Int) {
        _state.update { it.copy(csvExportRunning = true, showCsvExportDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csv = api().exportTableCsv(tableName, limit)
                saveCsvFile(tableName, csv)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snack("CSV export failed: ${e.message}", true)
                    _state.update { it.copy(csvExportRunning = false) }
                }
            }
        }
    }

    fun exportSqlResultCsv(query: String) {
        if (query.isBlank()) { snack("No query to export.", true); return }
        _state.update { it.copy(csvExportRunning = true, showSqlCsvExportDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csv = api().exportSqlResultCsv(query)
                saveCsvFile("sql_result", csv)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snack("CSV export failed: ${e.message}", true)
                    _state.update { it.copy(csvExportRunning = false) }
                }
            }
        }
    }

    private suspend fun saveCsvFile(name: String, csv: String) {
        val ts = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val ref = _state.value.cfg.currentRef.ifBlank { "project" }
        val file = java.io.File(SupabaseStorage.dataDir(), "${name}_${ref}_$ts.csv")
        file.writeText(csv)
        val lines = csv.lines().size
        withContext(Dispatchers.Main) {
            _state.update { it.copy(csvExportRunning = false) }
            snack("CSV saved: ${file.name} ($lines lines)")
        }
    }

    // ── Table preview ─────────────────────────────────────────────────────

    fun previewTable(tableName: String, schemaName: String) {
        val key = "$schemaName.$tableName"
        _state.value.tablePreviewCache[key]?.let {
            _state.update { s -> s.copy(activeTablePreview = TablePreviewDialog(key, it)) }
            return
        }
        async("Loading preview for $key...", {
            api().previewTable(tableName, schemaName, _state.value.settings.table_preview_rows)
        }, { rows ->
            val updated = _state.value.tablePreviewCache.toMutableMap().also { it[key] = rows }
            _state.update { s -> s.copy(tablePreviewCache = updated, activeTablePreview = TablePreviewDialog(key, rows)) }
        })
    }

    fun dismissTablePreview() = _state.update { it.copy(activeTablePreview = null) }

    // ── Row operations ────────────────────────────────────────────────────

    fun showUpdateRow(tableName: String, pkGuess: String) =
        _state.update { it.copy(updateRowContext = UpdateRowContext(tableName, pkGuess)) }

    fun dismissUpdateRow() = _state.update { it.copy(updateRowContext = null) }

    fun executeUpdateRow(tableName: String, matchCol: String, matchVal: String, payload: Map<String, Any>) {
        _state.update { it.copy(updateRowContext = null) }
        async("Updating $tableName...", {
            api().updateRow(tableName, matchCol, matchVal, payload)
        }, { snack("Row updated.") },
        { err -> snack("Update failed: $err", true) })
    }

    fun showInsertRow(table: SupabaseTable, columns: List<SupabaseColumn>) =
        _state.update { it.copy(insertRowContext = InsertRowContext(table.name, columns)) }

    fun dismissInsertRow() = _state.update { it.copy(insertRowContext = null) }

    fun executeInsertRow(tableName: String, payload: Map<String, Any>) {
        _state.update { it.copy(insertRowContext = null) }
        async("Inserting row into $tableName...", {
            api().insertRow(tableName, payload)
        }, {
            snack("Row inserted.")
            // Refresh active table data if open
            _state.value.activeTableForDetail?.let { t ->
                if (t.name == tableName) {
                    openTableDetail(t)
                }
            }
        }, { err -> snack("Insert failed: $err", true) })
    }

    // ── User CRUD ─────────────────────────────────────────────────────────

    fun createUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { snack("Email and password required.", true); return }
        async("Creating user...", { api().createUser(email, password) }, {
            snack("User $email created.")
            loadUsers()
        })
    }

    fun deleteUser(userId: String, email: String) = async("Deleting user...", { api().deleteUser(userId) }, {
        snack("User $email deleted.")
        loadUsers()
    })

    fun banUser(userId: String, email: String) = async("Banning user...", { api().banUser(userId) }, {
        snack("User $email banned.")
        loadUsers()
    })

    fun unbanUser(userId: String, email: String) = async("Unbanning user...", { api().unbanUser(userId) }, {
        snack("User $email unbanned.")
        loadUsers()
    })

    fun sendPasswordReset(email: String) = async("Sending reset email...", { api().sendPasswordReset(email) }, {
        snack("Password reset email sent to $email.")
    })

    // ── Generic CRUD action ───────────────────────────────────────────────

    fun showActionDialog(action: PendingAction) = _state.update { it.copy(pendingAction = action) }
    fun dismissActionDialog()                   = _state.update { it.copy(pendingAction = null) }

    fun executeAction(action: PendingAction, payload: Any?) {
        _state.update { it.copy(pendingAction = null) }
        async("Executing ${action.title}...", {
            api().executeAction(action.endpoint, action.method, payload, action.useManagement)
        }, {
            snack("${action.title} completed.")
            action.successCallback?.invoke()
        }, { err -> snack("${action.title} failed: $err", true) })
    }

    // ── Global search ─────────────────────────────────────────────────────

    fun showGlobalSearch() = _state.update { it.copy(showGlobalSearch = true, globalSearchQuery = "", globalSearchResults = emptyList()) }
    fun dismissGlobalSearch() = _state.update { it.copy(showGlobalSearch = false) }

    fun updateGlobalSearch(query: String) {
        _state.update { it.copy(globalSearchQuery = query) }
        if (query.length < 2) {
            _state.update { it.copy(globalSearchResults = emptyList()) }
            return
        }
        val q = query.lowercase()
        val results = mutableListOf<GlobalSearchResult>()
        // Tables
        _state.value.tablesCache?.payload?.filter { it.name.lowercase().contains(q) || it.schema.lowercase().contains(q) }?.forEach {
            results.add(GlobalSearchResult("table", "${it.schema}.${it.name}", "Table", NavSection.DATABASE, NavScreen.TABLES))
        }
        // Users
        _state.value.usersCache?.payload?.filter { (it.email ?: it.phone ?: "").lowercase().contains(q) }?.take(5)?.forEach {
            results.add(GlobalSearchResult("user", it.displayName, "User", NavSection.AUTH, NavScreen.USERS))
        }
        // Functions
        _state.value.functionsCache?.payload?.filter { it.displayName.lowercase().contains(q) }?.forEach {
            results.add(GlobalSearchResult("function", it.displayName, "Edge Function", NavSection.DEVTOOLS, NavScreen.FUNCTIONS))
        }
        // Secrets
        _state.value.secretsCache?.payload?.filter { it.name.lowercase().contains(q) }?.forEach {
            results.add(GlobalSearchResult("secret", it.name, "Secret", NavSection.AUTH, NavScreen.SECRETS))
        }
        // Buckets
        _state.value.storageCache?.payload?.filter { it.name.lowercase().contains(q) }?.forEach {
            results.add(GlobalSearchResult("bucket", it.name, "Storage Bucket", NavSection.DEVTOOLS, NavScreen.STORAGE))
        }
        _state.update { it.copy(globalSearchResults = results.take(20)) }
    }

    fun navigateFromSearch(result: GlobalSearchResult) {
        _state.update { it.copy(showGlobalSearch = false) }
        navigateTo(result.navScreen)
    }

    // ── WebView credentials ───────────────────────────────────────────────

    fun showWebViewCredDialog() = _state.update { it.copy(showWebViewCredDialog = true) }
    fun dismissWebViewCredDialog() = _state.update { it.copy(showWebViewCredDialog = false) }

    fun updateWebViewCredDraft(field: String, value: String) {
        val d = _state.value.webViewCredDraft
        _state.update { it.copy(webViewCredDraft = when (field) {
            "email"      -> d.copy(email = value)
            "password"   -> d.copy(password = value)
            "rememberMe" -> d.copy(rememberMe = value == "true")
            else         -> d
        })}
    }

    fun saveWebViewCredentials() {
        val creds = _state.value.webViewCredDraft
        SupabaseStorage.saveWebViewCredentials(creds)
        _state.update { it.copy(webViewCredentials = creds, showWebViewCredDialog = false) }
        snack(if (creds.rememberMe) "Web Dashboard credentials saved." else "Web Dashboard credentials cleared.")
    }

    fun clearWebViewCredentials() {
        SupabaseStorage.clearWebViewCredentials()
        val empty = WebViewCredentials()
        _state.update { it.copy(webViewCredentials = empty, webViewCredDraft = empty, showWebViewCredDialog = false) }
        snack("Web Dashboard credentials cleared.")
    }

    // ── Settings ──────────────────────────────────────────────────────────

    fun updateTimeoutDraft(v: String)     = _state.update { it.copy(settingsTimeoutDraft = v) }
    fun updatePreviewRowsDraft(v: String) = _state.update { it.copy(settingsPreviewRowsDraft = v) }

    fun saveSettings() {
        val s = _state.value.settings.copy(
            timeout_seconds    = _state.value.settingsTimeoutDraft.toIntOrNull()?.coerceIn(10, 120) ?: 40,
            table_preview_rows = _state.value.settingsPreviewRowsDraft.toIntOrNull()?.coerceIn(1, 20) ?: 5
        )
        SupabaseStorage.saveSettings(s)
        _state.update { it.copy(settings = s) }
        snack("Settings saved.")
    }

    fun toggleDarkMode() {
        val s = _state.value.settings.copy(dark_mode = !_state.value.settings.dark_mode)
        SupabaseStorage.saveSettings(s)
        _state.update { it.copy(settings = s) }
    }

    fun toggleCompactMode() {
        val s = _state.value.settings.copy(compact_mode = !_state.value.settings.compact_mode)
        SupabaseStorage.saveSettings(s)
        _state.update { it.copy(settings = s) }
    }

    fun clearCache() {
        _state.update {
            it.copy(
                overviewCache = null, projectsCache = null, usersCache = null,
                tablesCache = null, storageCache = null, functionsCache = null,
                secretsCache = null, policiesCache = null, migrationsCache = null,
                usageCache = null, logsCache = null, cronCache = null, webhooksCache = null,
                tablePreviewCache = emptyMap(), sqlResult = "",
                sqlResultRows = emptyList(), sqlResultColumns = emptyList(),
                bucketObjects = emptyList(), tableColumns = emptyList(),
                tableDataRows = emptyList(), schemaDump = ""
            )
        }
        snack("Session cache cleared.")
    }

    fun setPin(pin: String) {
        val s = SupabaseStorage.loadSettings().copy(app_pin = pin)
        SupabaseStorage.saveSettings(s)
        _state.update { it.copy(settings = s) }
        snack(if (pin.isBlank()) "PIN removed." else "PIN set.")
    }

    fun showSetPin()    = _state.update { it.copy(showSetPinDialog = true) }
    fun dismissSetPin() = _state.update { it.copy(showSetPinDialog = false) }

    // ── Dialog toggles ────────────────────────────────────────────────────

    fun showCreateUser()    = _state.update { it.copy(showCreateUser = true) }
    fun dismissCreateUser() = _state.update { it.copy(showCreateUser = false) }
    fun showAddSecret()     = _state.update { it.copy(showAddSecretPayload = true) }
    fun dismissAddSecret()  = _state.update { it.copy(showAddSecretPayload = false) }
}

// ── Schema dump query builder (unchanged from original) ───────────────────────

fun buildSchemaDumpQueries(section: String): List<Pair<String, String>> {
    val all = linkedMapOf<String, String>()
    all["Database Info"] = "SELECT current_database() AS database, pg_size_pretty(pg_database_size(current_database())) AS total_size, version() AS postgres_version, current_setting('server_version') AS server_version;"
    all["Schemas"] = "SELECT schema_name, schema_owner FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog','information_schema','pg_toast') ORDER BY schema_name;"
    all["Tables"] = "SELECT t.table_schema, t.table_name, t.table_type, pg_size_pretty(pg_total_relation_size(quote_ident(t.table_schema)||'.'||quote_ident(t.table_name))) AS total_size FROM information_schema.tables t WHERE t.table_schema NOT IN ('pg_catalog','information_schema') ORDER BY t.table_schema, t.table_name;"
    all["Columns"] = "SELECT c.table_schema, c.table_name, c.column_name, c.ordinal_position, c.column_default, c.is_nullable, c.data_type, c.udt_name FROM information_schema.columns c WHERE c.table_schema NOT IN ('pg_catalog','information_schema') ORDER BY c.table_schema, c.table_name, c.ordinal_position;"
    all["Primary Keys"] = "SELECT tc.table_schema, tc.table_name, tc.constraint_name, string_agg(kcu.column_name, ', ' ORDER BY kcu.ordinal_position) AS key_columns FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema NOT IN ('pg_catalog','information_schema') GROUP BY tc.table_schema, tc.table_name, tc.constraint_name ORDER BY tc.table_schema, tc.table_name;"
    all["Foreign Keys"] = "SELECT tc.table_schema, tc.table_name, tc.constraint_name, kcu.column_name, ccu.table_schema AS foreign_schema, ccu.table_name AS foreign_table, ccu.column_name AS foreign_column FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema NOT IN ('pg_catalog','information_schema') ORDER BY tc.table_schema, tc.table_name;"
    all["Indexes"] = "SELECT schemaname, tablename, indexname, indexdef FROM pg_indexes WHERE schemaname NOT IN ('pg_catalog','information_schema') ORDER BY schemaname, tablename, indexname;"
    all["Views"] = "SELECT table_schema, table_name, view_definition FROM information_schema.views WHERE table_schema NOT IN ('pg_catalog','information_schema') ORDER BY table_schema, table_name;"
    all["Functions & Procedures"] = "SELECT n.nspname AS schema, p.proname AS name, pg_get_function_arguments(p.oid) AS arguments, t.typname AS return_type, p.prokind AS kind, l.lanname AS language FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace JOIN pg_type t ON t.oid = p.prorettype JOIN pg_language l ON l.oid = p.prolang WHERE n.nspname NOT IN ('pg_catalog','information_schema','pg_toast') ORDER BY n.nspname, p.proname;"
    all["Triggers"] = "SELECT trigger_schema, trigger_name, event_manipulation, event_object_schema, event_object_table, action_timing FROM information_schema.triggers WHERE trigger_schema NOT IN ('pg_catalog','information_schema') ORDER BY trigger_schema, event_object_table, trigger_name;"
    all["RLS Policies"] = "SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check FROM pg_policies WHERE schemaname NOT IN ('pg_catalog','information_schema') ORDER BY schemaname, tablename, policyname;"
    all["Row Level Security Status"] = "SELECT n.nspname AS schema, c.relname AS table, c.relrowsecurity AS rls_enabled, c.relforcerowsecurity AS rls_forced FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'r' AND n.nspname NOT IN ('pg_catalog','information_schema') ORDER BY n.nspname, c.relname;"
    all["Extensions"] = "SELECT extname, extversion FROM pg_extension ORDER BY extname;"
    all["Enums (Custom Types)"] = "SELECT n.nspname AS schema, t.typname AS enum_name, string_agg(e.enumlabel, ', ' ORDER BY e.enumsortorder) AS values FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace JOIN pg_enum e ON e.enumtypid = t.oid WHERE n.nspname NOT IN ('pg_catalog','information_schema') GROUP BY n.nspname, t.typname ORDER BY n.nspname, t.typname;"
    all["Auth Users Summary"] = "SELECT COUNT(*) AS total_users, COUNT(CASE WHEN confirmed_at IS NOT NULL THEN 1 END) AS confirmed, COUNT(CASE WHEN banned_until > NOW() THEN 1 END) AS banned, COUNT(CASE WHEN last_sign_in_at > NOW() - INTERVAL '30 days' THEN 1 END) AS active_30d FROM auth.users;"
    all["Storage Buckets"] = "SELECT id, name, public, file_size_limit, created_at, updated_at FROM storage.buckets ORDER BY name;"
    all["Cron Jobs"] = "SELECT jobid, schedule, command, nodename, active, jobname FROM cron.job ORDER BY jobname;"
    all["Table Sizes"] = "SELECT n.nspname AS schema, c.relname AS table, pg_size_pretty(pg_table_size(c.oid)) AS table_size, pg_size_pretty(pg_indexes_size(c.oid)) AS indexes_size, pg_size_pretty(pg_total_relation_size(c.oid)) AS total_size, c.reltuples::bigint AS estimated_rows FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'r' AND n.nspname NOT IN ('pg_catalog','information_schema') ORDER BY pg_total_relation_size(c.oid) DESC;"
    return if (section == "All") all.entries.map { it.key to it.value }
    else all.entries.filter { it.key == section }.map { it.key to it.value }
}
