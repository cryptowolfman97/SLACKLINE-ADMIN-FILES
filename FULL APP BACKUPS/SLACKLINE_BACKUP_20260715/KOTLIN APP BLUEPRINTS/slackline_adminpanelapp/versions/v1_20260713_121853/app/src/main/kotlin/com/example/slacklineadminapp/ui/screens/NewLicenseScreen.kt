package com.example.slacklineadminapp.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════════
// NEW LICENSE MANAGER — list screen
// ═══════════════════════════════════════════════════════════════════════════════

class NewLicenseViewModel : ViewModel() {
    private val _products   = MutableStateFlow<List<NewProduct>>(emptyList())
    val products: StateFlow<List<NewProduct>> = _products

    private val _toast      = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _confirmDel = MutableStateFlow<NewProduct?>(null)
    val confirmDel: StateFlow<NewProduct?> = _confirmDel

    private val _loading    = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Add product dialog fields
    var newName   by mutableStateOf("")
    var newPrefix by mutableStateOf("")
    var newColor  by mutableStateOf("#00A383")
    var newBundle by mutableStateOf("")
    var showAdd   by mutableStateOf(false)

    // Backup-all dialog
    var backupAllPw by mutableStateOf("")
    var showBackupAll by mutableStateOf(false)

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        _products.value = NewLicenseStore.allProducts()
    }

    fun consumeToast()               { _toast.value = "" }
    fun requestDelete(p: NewProduct) { _confirmDel.value = p }
    fun cancelDelete()               { _confirmDel.value = null }

    fun confirmDelete(p: NewProduct) = viewModelScope.launch(Dispatchers.IO) {
        NewLicenseStore.removeProduct(p.id)
        load()
        _toast.value      = "\"${p.displayName}\" deleted."
        _confirmDel.value = null
    }

    fun addProduct() = viewModelScope.launch(Dispatchers.IO) {
        if (newName.isBlank() || newPrefix.length < 2) {
            _toast.value = "Name and prefix (min 2 chars) required."
            return@launch
        }
        val clean  = newPrefix.uppercase().trimEnd('-')
        val pid    = ProductConfig.slugify(newName)
        val bundle = newBundle.trim().ifBlank { pid }
        if (NewLicenseStore.allProducts().any { it.id == pid }) {
            _toast.value = "Product ID \"$pid\" already exists."
            return@launch
        }
        NewLicenseStore.addProduct(
            NewProduct(
                id               = pid,
                displayName      = newName,
                prefixRoot       = clean,
                activationPrefix = "${clean}6A",
                licensePrefix    = clean,
                bundleApp        = bundle,
                color            = newColor.ifBlank { "#00A383" },
                createdAt        = AppStorage.utcNow()
            )
        )
        try { NewLicenseStore.initAuthority(pid) } catch (_: Exception) { }
        load()
        showAdd = false; newName = ""; newPrefix = ""; newBundle = ""; newColor = "#00A383"
        _toast.value = "Product added and authority initialized."
    }

    fun backupAllProducts() = viewModelScope.launch(Dispatchers.IO) {
        val pw = backupAllPw
        if (pw.isBlank()) { _toast.value = "Password required."; return@launch }
        _loading.value = true
        var ok = 0; var skipped = 0
        _products.value.forEach { prod ->
            if (!NewLicenseStore.hasAuthority(prod.id)) { skipped++; return@forEach }
            try {
                listOf("authority_only" to "auth", "license_list_only" to "list", "full_backup" to "full")
                    .forEach { (bt, kind) ->
                        val blob = NewLicenseStore.buildBackupBlob(prod.id, pw, bt)
                        NewLicenseStore.saveBackupFile(prod.id, blob, kind)
                    }
                ok++
            } catch (_: Exception) { skipped++ }
        }
        _loading.value   = false
        showBackupAll    = false
        backupAllPw      = ""
        _toast.value     = "Backed up $ok product(s). Skipped $skipped (no authority)."
    }
}

@Composable
fun NewLicenseManagerScreen(
    onNavigateBack: () -> Unit,
    onOpenProduct:  (String) -> Unit,
    vm: NewLicenseViewModel = viewModel()
) {
    val products   by vm.products.collectAsState()
    val toast      by vm.toast.collectAsState()
    val confirmDel by vm.confirmDel.collectAsState()
    val loading    by vm.loading.collectAsState()
    val appColors   = LocalAppColors.current

    // Per-product backup dialog
    var backupFor by remember { mutableStateOf<NewProduct?>(null) }
    var backupPw  by remember { mutableStateOf("") }

    LaunchedEffect(Unit)  { vm.load() }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    // ── Confirm delete ──────────────────────────────────────────────────────
    confirmDel?.let { p ->
        ConfirmDialog("Delete Product", "Delete \"${p.displayName}\" permanently?",
            "Delete", RedCol,
            onConfirm = { vm.confirmDelete(p) },
            onDismiss = { vm.cancelDelete() })
    }

    // ── Backup All password dialog ──────────────────────────────────────────
    if (vm.showBackupAll) {
        AlertDialog(
            onDismissRequest = { vm.showBackupAll = false; vm.backupAllPw = "" },
            containerColor   = CardBg,
            title = { Text("Backup All Products", color = OrangeCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Enter a password for all backup files.", SubText)
                    AppTextField(vm.backupAllPw, { vm.backupAllPw = it }, "Backup Password", password = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.backupAllProducts() }) {
                    Text("BACKUP ALL", color = OrangeCol, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.showBackupAll = false; vm.backupAllPw = "" }) {
                    Text("CANCEL", color = SubText)
                }
            }
        )
    }

    // ── Per-product backup dialog ───────────────────────────────────────────
    backupFor?.let { prod ->
        AlertDialog(
            onDismissRequest = { backupFor = null; backupPw = "" },
            containerColor   = CardBg,
            title = { Text("Backup — ${prod.displayName}", color = TealCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Password for encrypted backup files.", SubText)
                    AppTextField(backupPw, { backupPw = it }, "Backup Password", password = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (backupPw.isBlank()) return@TextButton
                    val pid = prod.id; val pw = backupPw
                    backupFor = null; backupPw = ""
                    listOf("authority_only" to "auth", "license_list_only" to "list", "full_backup" to "full")
                        .forEach { (bt, kind) ->
                            try { NewLicenseStore.saveBackupFile(pid, NewLicenseStore.buildBackupBlob(pid, pw, bt), kind) }
                            catch (_: Exception) { }
                        }
                }) { Text("BACKUP", color = GreenCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { backupFor = null; backupPw = "" }) { Text("CANCEL", color = SubText) }
            }
        )
    }

    // ── Add product dialog ──────────────────────────────────────────────────
    if (vm.showAdd) {
        AlertDialog(
            onDismissRequest = { vm.showAdd = false },
            containerColor   = CardBg,
            title = { Text("Add New Product", color = CyanCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(vm.newName,   { vm.newName   = it; if (vm.newBundle.isBlank()) vm.newBundle = ProductConfig.slugify(it) }, "Display Name")
                    AppTextField(vm.newPrefix, { vm.newPrefix = it.uppercase() }, "Prefix Root (e.g. NEWPROD)")
                    AppTextField(vm.newBundle, { vm.newBundle = it }, "Bundle App ID (e.g. my_app)")
                    AppTextField(vm.newColor,  { vm.newColor  = it }, "Color Hex (e.g. #00A383)")
                    BodyText("Authority keys will be auto-generated.", SubText)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.addProduct() }) {
                    Text("ADD", color = GreenCol, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.showAdd = false }) { Text("CANCEL", color = SubText) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("New License Manager", CyanCol, 20)
            BodyText("Manage products and licenses for new apps (separate from legacy).", SubText)

            if (toast.isNotEmpty()) AppCard { BodyText(toast, TealCol) }

            // Backup All button
            ActionButton("Backup All Products", OrangeCol) { vm.showBackupAll = true }

            if (loading) {
                Box(Modifier.fillMaxWidth().height(80.dp)) { LoadingOverlay() }
            } else if (products.isEmpty()) {
                AppCard { BodyText("No products yet. Tap + Add New Product.", SubText) }
            } else {
                products.forEach { prod ->
                    val licenses = remember(prod.id) { NewLicenseStore.loadLicenses(prod.id) }
                    val col = remember(prod.color) {
                        runCatching { Color(android.graphics.Color.parseColor(prod.color)) }
                            .getOrDefault(CyanCol)
                    }
                    val hasAuth = remember(prod.id) { NewLicenseStore.hasAuthority(prod.id) }

                    AppCard(color = CardBg2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prod.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (hasAuth) "Auth" else "No Auth",
                                color = if (hasAuth) GreenCol else RedCol,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        BodyText("Prefix: ${prod.activationPrefix}-  •  FP: ${NewLicenseStore.fingerprint(prod.id)}", SubText)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { StatCard(licenses.count { it.status == "active" },  "Active",  col) }
                            Box(Modifier.weight(1f)) { StatCard(licenses.count { it.status == "revoked" }, "Revoked", RedCol) }
                            Box(Modifier.weight(1f)) { StatCard(licenses.size, "Total", BlueCol) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onOpenProduct(prod.id) }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = col)) {
                                Text("Open Manager", fontSize = 11.sp, color = Color.White)
                            }
                            Button(onClick = { backupFor = prod }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeCol)) {
                                Text("Backup All", fontSize = 11.sp, color = Color.White)
                            }
                            Button(onClick = { vm.requestDelete(prod) }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                                Text("Delete", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            AppCard { ActionButton("+ Add New Product", CyanCol) { vm.showAdd = true } }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NEW PRODUCT MANAGER
// ═══════════════════════════════════════════════════════════════════════════════

class NewProductManagerViewModel(private val productId: String) : ViewModel() {

    enum class Tab { DASHBOARD, AUTHORITY, GENERATE, LICENSES, REVOCATIONS, BACKUPS }

    private val _tab      = MutableStateFlow(Tab.DASHBOARD)
    val tab: StateFlow<Tab> = _tab

    private val _product  = MutableStateFlow<NewProduct?>(null)
    val product: StateFlow<NewProduct?> = _product

    private val _records  = MutableStateFlow<List<LicenseRecord>>(emptyList())
    val records: StateFlow<List<LicenseRecord>> = _records

    private val _loading  = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _toast    = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _lastCode = MutableStateFlow("")
    val lastCode: StateFlow<String> = _lastCode

    private val _lastLid  = MutableStateFlow("")
    val lastLid: StateFlow<String> = _lastLid

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating

    private val _genStatus  = MutableStateFlow("")
    val genStatus: StateFlow<String> = _genStatus

    private val _genIsError = MutableStateFlow(false)
    val genIsError: StateFlow<Boolean> = _genIsError

    private val _backupText = MutableStateFlow("")
    val backupText: StateFlow<String> = _backupText

    private val _revoText   = MutableStateFlow("")
    val revoText: StateFlow<String> = _revoText

    private val _ghFiles    = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val ghFiles: StateFlow<List<Map<String, String>>> = _ghFiles

    // Generate form
    var genDevice   by mutableStateOf("")
    var genCustomer by mutableStateOf("")
    var genEmail    by mutableStateOf("")
    var genLabel    by mutableStateOf("")
    var genNote     by mutableStateOf("")
    var genExpiry   by mutableStateOf("")
    var genTier     by mutableStateOf("pro")
    var genSource   by mutableStateOf("crypto")

    // Authority tab
    var authBackupPw    by mutableStateOf("")
    var allBackupsPw    by mutableStateOf("")
    var importPastePw   by mutableStateOf("")
    var importPasteBlob by mutableStateOf("")
    var showRemoveAuth  by mutableStateOf(false)

    // GitHub upload
    var ghUpOwner  by mutableStateOf("")
    var ghUpRepo   by mutableStateOf("")
    var ghUpBranch by mutableStateOf("main")
    var ghUpFolder by mutableStateOf("Backups")
    var ghUpToken  by mutableStateOf("")

    // GitHub import
    var ghImOwner  by mutableStateOf("")
    var ghImRepo   by mutableStateOf("")
    var ghImBranch by mutableStateOf("main")
    var ghImFolder by mutableStateOf("Backups")
    var ghImToken  by mutableStateOf("")
    var ghImPw     by mutableStateOf("")

    // Revocations
    var ghRvOwner  by mutableStateOf("")
    var ghRvRepo   by mutableStateOf("")
    var ghRvBranch by mutableStateOf("main")
    var ghRvPath   by mutableStateOf("")
    var ghRvToken  by mutableStateOf("")

    // Backups tab
    var bkPw by mutableStateOf("")

    // Confirm delete license
    var confirmDelLid by mutableStateOf<String?>(null)

    // Search / filter
    var searchQuery  by mutableStateOf("")
    var filterStatus by mutableStateOf("all")
    var filterSort   by mutableStateOf("newest")

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        _product.value  = NewLicenseStore.allProducts().find { it.id == productId }
        _records.value  = NewLicenseStore.loadLicenses(productId)
        _product.value?.let { p ->
            ghRvOwner  = p.githubOwner
            ghRvRepo   = p.githubRepo
            ghRvBranch = p.githubBranch.ifBlank { "main" }
            ghRvPath   = p.githubPath
            ghUpOwner  = p.githubOwner
            ghUpRepo   = p.githubRepo
            ghUpBranch = p.githubBranch.ifBlank { "main" }
            ghImOwner  = p.githubOwner
            ghImRepo   = p.githubRepo
            ghImBranch = p.githubBranch.ifBlank { "main" }
        }
    }

    fun reload() = viewModelScope.launch(Dispatchers.IO) {
        _records.value = NewLicenseStore.loadLicenses(productId)
    }

    fun switchTab(t: Tab) { _tab.value = t }
    fun consumeToast()    { _toast.value = "" }

    // ── Authority ──────────────────────────────────────────────────────────────

    fun initAuthority() = viewModelScope.launch(Dispatchers.IO) {
        _loading.value = true
        try {
            NewLicenseStore.initAuthority(productId)
            _toast.value = "Authority initialized! Copy the Public Key PEM and embed it in your app."
        } catch (e: Exception) { _toast.value = e.message ?: "Init failed." }
        _loading.value = false
    }

    fun removeAuthority() = viewModelScope.launch(Dispatchers.IO) {
        NewLicenseStore.removeAuthority(productId)
        _toast.value = "Local authority removed."
        showRemoveAuth = false
    }

    fun generateBackup(kind: String) = viewModelScope.launch(Dispatchers.IO) {
        val pw = authBackupPw
        if (pw.isBlank()) { _toast.value = "Password required."; return@launch }
        if (!NewLicenseStore.hasAuthority(productId)) { _toast.value = "No authority loaded."; return@launch }
        try {
            val bt = when(kind) { "auth" -> "authority_only"; "list" -> "license_list_only"; else -> "full_backup" }
            _backupText.value = NewLicenseStore.buildBackupBlob(productId, pw, bt)
            _toast.value = "Backup generated."
        } catch (e: Exception) { _toast.value = "Backup failed: ${e.message}" }
    }

    fun saveBackupFile(kind: String) = viewModelScope.launch(Dispatchers.IO) {
        val blob = _backupText.value
        if (blob.isBlank()) { _toast.value = "Generate a backup first."; return@launch }
        try {
            NewLicenseStore.saveBackupFile(productId, blob, kind)
            _toast.value = "Backup saved."
        } catch (e: Exception) { _toast.value = "Save failed: ${e.message}" }
    }

    fun exportAllBackups() = viewModelScope.launch(Dispatchers.IO) {
        val pw = allBackupsPw
        if (pw.isBlank()) { _toast.value = "Password required."; return@launch }
        if (!NewLicenseStore.hasAuthority(productId)) { _toast.value = "No authority loaded."; return@launch }
        _loading.value = true
        try {
            listOf("authority_only" to "auth", "license_list_only" to "list", "full_backup" to "full")
                .forEach { (bt, kind) ->
                    NewLicenseStore.saveBackupFile(productId, NewLicenseStore.buildBackupBlob(productId, pw, bt), kind)
                }
            _toast.value = "All 3 backups saved."
        } catch (e: Exception) { _toast.value = "Export failed: ${e.message}" }
        _loading.value = false
    }

    fun uploadBackupsToGitHub() = viewModelScope.launch(Dispatchers.IO) {
        val pw = allBackupsPw
        if (pw.isBlank()) { _toast.value = "Enter password in Export All Backups first."; return@launch }
        if (!NewLicenseStore.hasAuthority(productId)) { _toast.value = "No authority loaded."; return@launch }
        if (ghUpOwner.isBlank() || ghUpRepo.isBlank() || ghUpToken.isBlank()) {
            _toast.value = "Owner, repo, and token required."; return@launch
        }
        _loading.value = true
        try {
            listOf("authority_only" to ".ctp", "license_list_only" to ".ctlist", "full_backup" to ".ctfull")
                .forEach { (bt, ext) ->
                    val blob = NewLicenseStore.buildBackupBlob(productId, pw, bt)
                    val name = "${productId}_${bt}_${AppStorage.timestamp()}$ext"
                    NewLicenseStore.uploadFileToGitHub(ghUpOwner, ghUpRepo, ghUpBranch.ifBlank{"main"},
                        ghUpFolder, name, blob, ghUpToken)
                }
            _toast.value = "All 3 backups uploaded to GitHub!"
        } catch (e: Exception) { _toast.value = "Upload failed: ${e.message}" }
        _loading.value = false
    }

    fun listGitHubFiles() = viewModelScope.launch(Dispatchers.IO) {
        if (ghImOwner.isBlank() || ghImRepo.isBlank() || ghImToken.isBlank()) {
            _toast.value = "Owner, repo, and token required."; return@launch
        }
        _loading.value = true
        try {
            _ghFiles.value = NewLicenseStore.listGitHubFiles(
                ghImOwner, ghImRepo, ghImFolder, ghImBranch.ifBlank{"main"}, ghImToken)
            if (_ghFiles.value.isEmpty()) _toast.value = "No .ctp or .ctfull files found."
        } catch (e: Exception) { _toast.value = "List failed: ${e.message}" }
        _loading.value = false
    }

    fun importFromGitHub(path: String) = viewModelScope.launch(Dispatchers.IO) {
        if (ghImPw.isBlank()) { _toast.value = "Enter backup password."; return@launch }
        _loading.value = true
        try {
            val blob = NewLicenseStore.fetchFileFromGitHub(
                ghImOwner, ghImRepo, path, ghImBranch.ifBlank{"main"}, ghImToken)
            NewLicenseStore.applyBackupBlob(productId, blob, ghImPw)
            load()
            _toast.value = "Backup imported successfully."
        } catch (e: Exception) { _toast.value = "Import failed: ${e.message}" }
        _loading.value = false
    }

    fun importFromPaste() = viewModelScope.launch(Dispatchers.IO) {
        if (importPasteBlob.isBlank()) { _toast.value = "Paste a backup first."; return@launch }
        _loading.value = true
        try {
            NewLicenseStore.applyBackupBlob(productId, importPasteBlob, importPastePw)
            load()
            _toast.value = "Backup imported."
        } catch (e: Exception) { _toast.value = "Import failed: ${e.message}" }
        _loading.value = false
    }

    // ── Generate ───────────────────────────────────────────────────────────────

    fun generateLicense() = viewModelScope.launch(Dispatchers.IO) {
        val prod = _product.value ?: return@launch
        _generating.value = true
        _genStatus.value  = ""
        try {
            val rec = NewLicenseStore.generateLicense(
                product = prod, tier = genTier, source = genSource,
                deviceCode = genDevice, customerName = genCustomer,
                customerEmail = genEmail, label = genLabel, note = genNote, expiry = genExpiry
            )
            _lastCode.value   = rec.activationCode
            _lastLid.value    = rec.licenseId
            _records.value    = NewLicenseStore.loadLicenses(productId)
            _genStatus.value  = "Generated: ${rec.licenseId}"
            _genIsError.value = false
        } catch (e: Exception) {
            _genStatus.value  = e.message ?: "Generation failed."
            _genIsError.value = true
        }
        _generating.value = false
    }

    fun clearGenForm() {
        genDevice = ""; genCustomer = ""; genEmail = ""; genLabel = ""; genNote = ""; genExpiry = ""
        _lastCode.value = ""; _lastLid.value = ""; _genStatus.value = ""
    }

    // ── Licenses ───────────────────────────────────────────────────────────────

    fun filteredRecords(): List<LicenseRecord> {
        val q = searchQuery.lowercase()
        var out = _records.value.filter { r ->
            (filterStatus == "all" || r.status == filterStatus) &&
            (q.isBlank() || listOf(r.licenseId, r.deviceCode, r.customerName, r.customerEmail, r.label, r.tier)
                .joinToString(" ").lowercase().contains(q))
        }
        out = when (filterSort) {
            "oldest" -> out.sortedBy { it.issuedAt }
            "status" -> out.sortedBy { it.status }
            else     -> out.sortedByDescending { it.issuedAt }
        }
        return out
    }

    fun toggleRevoke(lid: String) = viewModelScope.launch(Dispatchers.IO) {
        val result = NewLicenseStore.toggleRevoke(productId, lid)
        reload()
        _toast.value = "$lid is now ${result.uppercase()}."
    }

    fun deleteLicense(lid: String) = viewModelScope.launch(Dispatchers.IO) {
        NewLicenseStore.deleteLicense(productId, lid)
        reload()
        _toast.value = "$lid deleted."
    }

    fun exportCsv() = viewModelScope.launch(Dispatchers.IO) {
        val prod = _product.value ?: return@launch
        val file = NewLicenseStore.exportCsv(productId, prod.displayName)
        _toast.value = "Exported to ${file.name}"
    }

    // ── Revocations ────────────────────────────────────────────────────────────

    fun generateRevoJson() = viewModelScope.launch(Dispatchers.IO) {
        val prod = _product.value ?: return@launch
        val revoked = NewLicenseStore.loadRevocations()[productId] ?: emptyList()
        _revoText.value = AppStorage.gson.toJson(mapOf(
            "app" to prod.bundleApp, "version" to 1,
            "updated_at" to AppStorage.utcNow(), "revoked_ids" to revoked
        ))
        _toast.value = "Revocation JSON generated (${revoked.size} IDs)."
    }

    fun saveRevoFile() = viewModelScope.launch(Dispatchers.IO) {
        val prod = _product.value ?: return@launch
        val text = _revoText.value
        if (text.isBlank()) { _toast.value = "Generate revocation first."; return@launch }
        val dir  = AppStorage.exportDir(prod.displayName, "Revocation Jsons").also { it.mkdirs() }
        val file = File(dir, "${productId}_revo_${AppStorage.timestamp()}.json")
        file.writeText(text)
        _toast.value = "Saved: ${file.name}"
    }

    fun uploadRevocation() = viewModelScope.launch(Dispatchers.IO) {
        if (_revoText.value.isBlank()) { _toast.value = "Generate revocation JSON first."; return@launch }
        if (ghRvOwner.isBlank() || ghRvRepo.isBlank() || ghRvPath.isBlank() || ghRvToken.isBlank()) {
            _toast.value = "Owner, repo, path, and token required."; return@launch
        }
        _loading.value = true
        try {
            NewLicenseStore.uploadRevocationToGitHub(
                productId, ghRvOwner, ghRvRepo, ghRvBranch.ifBlank{"main"}, ghRvPath, ghRvToken)
            _toast.value = "Revocation uploaded to GitHub!"
        } catch (e: Exception) { _toast.value = "Upload failed: ${e.message}" }
        _loading.value = false
    }

    fun exportBackup(kind: String) = viewModelScope.launch(Dispatchers.IO) {
        if (bkPw.isBlank()) { _toast.value = "Password required."; return@launch }
        if (!NewLicenseStore.hasAuthority(productId) && kind != "list") {
            _toast.value = "No authority loaded."; return@launch
        }
        try {
            val bt = when(kind) { "auth" -> "authority_only"; "list" -> "license_list_only"; else -> "full_backup" }
            NewLicenseStore.saveBackupFile(productId, NewLicenseStore.buildBackupBlob(productId, bkPw, bt), kind)
            _toast.value = "Backup saved."
        } catch (e: Exception) { _toast.value = "Backup failed: ${e.message}" }
    }

    class Factory(private val pid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            NewProductManagerViewModel(pid) as T
    }
}

@Composable
fun NewProductManagerScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    vm: NewProductManagerViewModel = viewModel(factory = NewProductManagerViewModel.Factory(productId))
) {
    val clipboard  = LocalClipboardManager.current
    val product    by vm.product.collectAsState()
    val tab        by vm.tab.collectAsState()
    val records    by vm.records.collectAsState()
    val loading    by vm.loading.collectAsState()
    val toast      by vm.toast.collectAsState()
    val lastCode   by vm.lastCode.collectAsState()
    val lastLid    by vm.lastLid.collectAsState()
    val generating by vm.generating.collectAsState()
    val genStatus  by vm.genStatus.collectAsState()
    val genIsError by vm.genIsError.collectAsState()
    val backupText by vm.backupText.collectAsState()
    val revoText   by vm.revoText.collectAsState()
    val ghFiles    by vm.ghFiles.collectAsState()
    val appColors   = LocalAppColors.current

    LaunchedEffect(Unit)  { vm.load() }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    val col = remember(product?.color) {
        runCatching { product?.color?.let { Color(android.graphics.Color.parseColor(it)) } }
            .getOrNull() ?: CyanCol
    }

    // Details dialog
    var detailsRec by remember { mutableStateOf<LicenseRecord?>(null) }

    detailsRec?.let { rec ->
        AlertDialog(
            onDismissRequest = { detailsRec = null },
            containerColor   = CardBg,
            title = { Text("License Details", color = TealCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ID" to rec.licenseId, "Tier" to rec.tier.uppercase(),
                        "Status" to rec.status.uppercase(), "Source" to rec.source,
                        "Customer" to rec.customerName.ifBlank{"-"},
                        "Email" to rec.customerEmail.ifBlank{"-"},
                        "Device" to rec.deviceCode.ifBlank{"-"},
                        "Issued" to rec.issuedAt.take(10),
                        "Expiry" to rec.expiry.ifBlank{"-"},
                        "Label" to rec.label.ifBlank{"-"},
                        "Note" to rec.customerNote.ifBlank{"-"}
                    ).forEach { (k, v) ->
                        BodyText("$k: $v", if (k == "Status" && v == "REVOKED") RedCol else SubText)
                    }
                    Spacer(Modifier.height(8.dp))
                    var copied by remember { mutableStateOf(false) }
                    if (copied) LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); copied = false }
                    Button(onClick = { clipboard.setText(AnnotatedString(rec.activationCode)); copied = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (copied) GreenCol else TealCol)) {
                        Text(if (copied) "✓ Copied!" else "Copy Activation Code", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsRec = null }) { Text("CLOSE", color = SubText) }
            }
        )
    }

    // Remove auth confirm
    if (vm.showRemoveAuth) {
        ConfirmDialog("Remove Authority", "Delete the local keypair? Have a backup first!",
            "Remove", RedCol,
            onConfirm = { vm.removeAuthority() },
            onDismiss = { vm.showRemoveAuth = false })
    }

    // Delete license confirm
    vm.confirmDelLid?.let { lid ->
        ConfirmDialog("Delete License", "Permanently delete $lid?", "Delete", RedCol,
            onConfirm = { vm.deleteLicense(lid); vm.confirmDelLid = null },
            onDismiss = { vm.confirmDelLid = null })
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        // ── Tab bar — 2 rows of 3 ─────────────────────────────────────────────
        val allTabs   = NewProductManagerViewModel.Tab.values().toList()
        val tabLabels = mapOf(
            NewProductManagerViewModel.Tab.DASHBOARD   to "Dashboard",
            NewProductManagerViewModel.Tab.AUTHORITY   to "Authority",
            NewProductManagerViewModel.Tab.GENERATE    to "Generate",
            NewProductManagerViewModel.Tab.LICENSES    to "Licenses",
            NewProductManagerViewModel.Tab.REVOCATIONS to "Revocations",
            NewProductManagerViewModel.Tab.BACKUPS     to "Backups"
        )
        Column(modifier = Modifier.fillMaxWidth().background(CardBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            allTabs.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { t ->
                        Button(onClick = { vm.switchTab(t) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = if (t == tab) col else CardBg2),
                            contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(tabLabels[t] ?: t.name, fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }

        if (toast.isNotEmpty()) {
            AppCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                BodyText(toast, TealCol)
            }
        }

        if (loading) {
            Box(Modifier.weight(1f)) { LoadingOverlay() }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {

                product?.let { prod ->
                    val hasAuth = NewLicenseStore.hasAuthority(prod.id)

                    when (tab) {

                        // ── DASHBOARD ─────────────────────────────────────────
                        NewProductManagerViewModel.Tab.DASHBOARD -> {
                            AppCard(color = CardBg2) {
                                Text(prod.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                BodyText("Activation prefix: ${prod.activationPrefix}-", SubText)
                                BodyText("Bundle app: ${prod.bundleApp}", SubText)
                            }
                            // Keypair check on IO thread
                            var keypairOk by remember(prod.id) { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(prod.id) {
                                keypairOk = withContext(Dispatchers.IO) { NewLicenseStore.keypairIntact(prod.id) }
                            }
                            AppCard(color = CardBg2) {
                                SectionLabel("Authority Status",
                                    if (!hasAuth) RedCol else if (keypairOk == false) OrangeCol else GreenCol)
                                when {
                                    !hasAuth          -> BodyText("No authority. Go to Authority tab.", RedCol)
                                    keypairOk == null -> BodyText("Checking keypair…", SubText)
                                    keypairOk == true -> {
                                        BodyText("Authority loaded.", GreenCol)
                                        BodyText("Fingerprint: ${NewLicenseStore.fingerprint(prod.id)}", SubText)
                                    }
                                    else              -> {
                                        BodyText("⚠ KEYPAIR MISMATCH — cannot generate licenses.", OrangeCol)
                                        BodyText("Go to Authority tab → Import Backup to restore.", SubText)
                                    }
                                }
                            }
                            AppCard(color = CardBg2) {
                                SectionLabel("License Totals")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) { StatCard(records.count{it.status=="active"},  "Active",  col) }
                                    Box(Modifier.weight(1f)) { StatCard(records.count{it.status=="revoked"}, "Revoked", RedCol) }
                                    Box(Modifier.weight(1f)) { StatCard(records.size, "Total", BlueCol) }
                                }
                            }
                            if (records.isNotEmpty()) {
                                AppCard(color = CardBg2) {
                                    SectionLabel("Latest Issued")
                                    records.take(5).forEach { rec ->
                                        BodyText(
                                            "${rec.licenseId}  •  ${rec.tier.uppercase()}  •  ${rec.customerName.ifBlank{"-"}}  •  ${rec.status.uppercase()}",
                                            if (rec.status == "revoked") RedCol else SubText
                                        )
                                    }
                                }
                            }
                            AppCard {
                                SectionLabel("Quick Actions")
                                ActionButton("Generate License",   col)      { vm.switchTab(NewProductManagerViewModel.Tab.GENERATE) }
                                ActionButton("Manage Authority",   BlueCol)  { vm.switchTab(NewProductManagerViewModel.Tab.AUTHORITY) }
                                ActionButton("Revocation Export",  OrangeCol){ vm.switchTab(NewProductManagerViewModel.Tab.REVOCATIONS) }
                            }
                        }

                        // ── AUTHORITY ─────────────────────────────────────────
                        NewProductManagerViewModel.Tab.AUTHORITY -> {
                            AppCard(color = CardBg2) {
                                SectionLabel("Authority Status", if (hasAuth) GreenCol else RedCol)
                                if (hasAuth) {
                                    BodyText("Loaded", GreenCol)
                                    BodyText("Fingerprint: ${NewLicenseStore.fingerprint(prod.id)}", SubText)
                                    ActionButton("Copy Public Key PEM", TealCol) {
                                        clipboard.setText(AnnotatedString(NewLicenseStore.publicKeyPem(prod.id)))
                                    }
                                    ActionButton("Remove Local Authority", RedCol) { vm.showRemoveAuth = true }
                                } else {
                                    BodyText("No authority loaded.", RedCol)
                                }
                            }
                            AppCard {
                                SectionLabel("Initialize Fresh Authority")
                                BodyText("Generates a new RSA 2048-bit keypair. Copy the public key PEM and embed it in your customer app.", SubText)
                                ActionButton("Initialize Authority", GreenCol) { vm.initAuthority() }
                            }
                            // Manual backup
                            AppCard {
                                SectionLabel("Backup", BlueCol)
                                AppTextField(vm.authBackupPw, { vm.authBackupPw = it }, "Backup Password", password = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { vm.generateBackup("auth") }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                        Text("Auth Backup", fontSize = 11.sp, color = Color.White)
                                    }
                                    Button(onClick = { vm.generateBackup("full") }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                                        Text("Full Backup", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                ActionButton("License List Backup", BlueCol) { vm.generateBackup("list") }
                                if (backupText.isNotEmpty()) {
                                    OutlinedTextField(value = backupText, onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = GreenCol, unfocusedTextColor = GreenCol))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(onClick = { clipboard.setText(AnnotatedString(backupText)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                            Text("Copy Backup", fontSize = 11.sp, color = Color.White)
                                        }
                                        Button(onClick = {
                                            val kind = if ("authority_only" in backupText) "auth"
                                                       else if ("license_list" in backupText) "list" else "full"
                                            vm.saveBackupFile(kind)
                                        }, modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                            Text("Save to File", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            // Export all
                            AppCard {
                                SectionLabel("Export All Backups", OrangeCol)
                                AppTextField(vm.allBackupsPw, { vm.allBackupsPw = it }, "Password for all backups", password = true)
                                ActionButton("Generate & Save All 3 Backups", OrangeCol) { vm.exportAllBackups() }
                            }
                            // Upload to GitHub
                            AppCard {
                                SectionLabel("Upload Backups to GitHub", CyanCol)
                                BodyText("Uses password from Export All Backups above.", SubText)
                                val presets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" || it.type == "github_admin" } }
                                if (presets.isNotEmpty()) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { expanded = true }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = CardBg) {
                                            presets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghUpOwner = p.owner; vm.ghUpRepo = p.repo
                                                    vm.ghUpBranch = p.branch.ifBlank{"main"}; vm.ghUpToken = p.token
                                                    expanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                AppTextField(vm.ghUpOwner,  { vm.ghUpOwner  = it }, "GitHub Owner")
                                AppTextField(vm.ghUpRepo,   { vm.ghUpRepo   = it }, "Repository Name")
                                AppTextField(vm.ghUpBranch, { vm.ghUpBranch = it }, "Branch")
                                AppTextField(vm.ghUpFolder, { vm.ghUpFolder = it }, "Folder Path")
                                AppTextField(vm.ghUpToken,  { vm.ghUpToken  = it }, "GitHub Token", password = true)
                                ActionButton("Upload All 3 Backups to GitHub", GreenCol) { vm.uploadBackupsToGitHub() }
                            }
                            // Import from GitHub
                            AppCard {
                                SectionLabel("Import from GitHub", CyanCol)
                                val presets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" || it.type == "github_admin" } }
                                if (presets.isNotEmpty()) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { expanded = true }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = CardBg) {
                                            presets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghImOwner = p.owner; vm.ghImRepo = p.repo
                                                    vm.ghImBranch = p.branch.ifBlank{"main"}; vm.ghImToken = p.token
                                                    expanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                AppTextField(vm.ghImOwner,  { vm.ghImOwner  = it }, "GitHub Owner")
                                AppTextField(vm.ghImRepo,   { vm.ghImRepo   = it }, "Repository Name")
                                AppTextField(vm.ghImBranch, { vm.ghImBranch = it }, "Branch")
                                AppTextField(vm.ghImFolder, { vm.ghImFolder = it }, "Folder Path")
                                AppTextField(vm.ghImToken,  { vm.ghImToken  = it }, "GitHub Token", password = true)
                                ActionButton("List Backup Files", CyanCol) { vm.listGitHubFiles() }
                                if (ghFiles.isNotEmpty()) {
                                    AppTextField(vm.ghImPw, { vm.ghImPw = it }, "Backup Password (to decrypt)", password = true)
                                    ghFiles.forEach { f ->
                                        val size = f["size"]?.toLongOrNull() ?: 0L
                                        ActionButton("${f["name"]}  (${if (size > 1024) "${size/1024}KB" else "${size}B"})", PurpleCol) {
                                            vm.importFromGitHub(f["path"] ?: "")
                                        }
                                    }
                                }
                            }
                            // Import from paste
                            AppCard {
                                SectionLabel("Import Backup (Paste)")
                                AppTextField(vm.importPastePw,   { vm.importPastePw   = it }, "Backup Password", password = true)
                                AppTextField(vm.importPasteBlob, { vm.importPasteBlob = it }, "Paste backup text here")
                                ActionButton("Import Backup", GreenCol) { vm.importFromPaste() }
                            }
                        }

                        // ── GENERATE ──────────────────────────────────────────
                        NewProductManagerViewModel.Tab.GENERATE -> {
                            var kpOk by remember(prod.id) { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(prod.id) {
                                kpOk = withContext(Dispatchers.IO) { NewLicenseStore.keypairIntact(prod.id) }
                            }
                            AppCard {
                                SectionLabel("Generate License", col)
                                when {
                                    !hasAuth      -> BodyText("No authority. Initialize in Authority tab first.", RedCol)
                                    kpOk == false -> { BodyText("⚠ KEYPAIR MISMATCH", OrangeCol); BodyText("Go to Authority tab → Import Backup.", SubText) }
                                    kpOk == null  -> BodyText("Checking keypair…", SubText)
                                    else          -> {
                                        BodyText("Device Code", SubText)
                                        AppTextField(vm.genDevice, { vm.genDevice = it }, "Device code (e.g. APP-DEV-ABCD1234)")
                                        // Tier
                                        BodyText("Tier", SubText)
                                        var tierExp by remember { mutableStateOf(false) }
                                        Box {
                                            Button(onClick = { tierExp = true }, modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                                Text(vm.genTier, color = Color.White)
                                            }
                                            DropdownMenu(expanded = tierExp, onDismissRequest = { tierExp = false }, containerColor = CardBg) {
                                                listOf("pro","demo","pro_plus").forEach { t ->
                                                    DropdownMenuItem(text = { Text(t, color = Color.White) }, onClick = { vm.genTier = t; tierExp = false })
                                                }
                                            }
                                        }
                                        // Source
                                        BodyText("Payment Source", SubText)
                                        var srcExp by remember { mutableStateOf(false) }
                                        Box {
                                            Button(onClick = { srcExp = true }, modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                                Text(vm.genSource, color = Color.White)
                                            }
                                            DropdownMenu(expanded = srcExp, onDismissRequest = { srcExp = false }, containerColor = CardBg) {
                                                listOf("crypto","bank","promo","test","partner","personal").forEach { s ->
                                                    DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.genSource = s; srcExp = false })
                                                }
                                            }
                                        }
                                        BodyText("Customer Name", SubText)
                                        AppTextField(vm.genCustomer, { vm.genCustomer = it }, "Customer name")
                                        BodyText("Customer Email (optional)", SubText)
                                        AppTextField(vm.genEmail,  { vm.genEmail  = it }, "Customer email")
                                        BodyText("Label / Tag (optional)", SubText)
                                        AppTextField(vm.genLabel,  { vm.genLabel  = it }, "Internal label or tag")
                                        BodyText("Note (optional)", SubText)
                                        AppTextField(vm.genNote,   { vm.genNote   = it }, "Notes")
                                        BodyText("Expiry  YYYY-MM-DD  (optional)", SubText)
                                        AppTextField(vm.genExpiry, { vm.genExpiry = it }, "Expiry date")
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { vm.generateLicense() }, modifier = Modifier.weight(1f),
                                                enabled = !generating,
                                                colors = ButtonDefaults.buttonColors(containerColor = col)) {
                                                if (generating) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                                else Text("Generate License", color = Color.White)
                                            }
                                            Button(onClick = { vm.clearGenForm() }, modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = OrangeCol)) {
                                                Text("Clear Form", color = Color.White)
                                            }
                                        }
                                        // Activation code output — always visible
                                        BodyText("Activation Code", SubText)
                                        OutlinedTextField(
                                            value = lastCode.ifBlank { "" }, onValueChange = {}, readOnly = true,
                                            placeholder = { Text("Generated code will appear here", color = SubText, fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = GreenCol, unfocusedTextColor = GreenCol,
                                                unfocusedBorderColor = SubText, focusedBorderColor = GreenCol)
                                        )
                                        if (genStatus.isNotEmpty()) {
                                            Text(genStatus, color = if (genIsError) RedCol else GreenCol, fontSize = 13.sp)
                                        }
                                        if (lastCode.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { clipboard.setText(AnnotatedString(lastCode)) },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                                    Text("Copy Code", color = Color.White)
                                                }
                                                Button(onClick = { clipboard.setText(AnnotatedString(lastLid)) },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                                    Text("Copy License ID", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── LICENSES ──────────────────────────────────────────
                        NewProductManagerViewModel.Tab.LICENSES -> {
                            AppCard {
                                SectionLabel("Search & Filter", TealCol)
                                AppTextField(vm.searchQuery, { vm.searchQuery = it }, "Search by ID / name / email / device / label")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var statusExp by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        Button(onClick = { statusExp = true }, modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.filterStatus, color = Color.White, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = statusExp, onDismissRequest = { statusExp = false }, containerColor = CardBg) {
                                            listOf("all","active","revoked").forEach { s ->
                                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.filterStatus = s; statusExp = false })
                                            }
                                        }
                                    }
                                    var sortExp by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        Button(onClick = { sortExp = true }, modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.filterSort, color = Color.White, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = sortExp, onDismissRequest = { sortExp = false }, containerColor = CardBg) {
                                            listOf("newest","oldest","status").forEach { s ->
                                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.filterSort = s; sortExp = false })
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { vm.reload() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                                        Text("Refresh", color = Color.White)
                                    }
                                    Button(onClick = { vm.exportCsv() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                        Text("Export CSV", color = Color.White)
                                    }
                                }
                            }
                            val filtered = vm.filteredRecords()
                            BodyText("${filtered.size} license(s) shown.", SubText)
                            if (filtered.isEmpty()) {
                                AppCard { BodyText("No licenses match.", SubText) }
                            } else {
                                filtered.forEach { rec ->
                                    AppCard(color = CardBg2) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(rec.licenseId, color = col, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(rec.status.uppercase(),
                                                color = if (rec.status == "active") GreenCol else RedCol,
                                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        BodyText("${rec.tier.uppercase()}  •  ${rec.source}  •  ${rec.customerName.ifBlank{"-"}}  •  ${rec.issuedAt.take(10)}", SubText)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(onClick = { detailsRec = rec }) {
                                                Text("Details", color = TealCol, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { vm.toggleRevoke(rec.licenseId) }) {
                                                Text(if (rec.status == "active") "Revoke" else "Restore",
                                                    color = if (rec.status == "active") OrangeCol else GreenCol,
                                                    fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { vm.confirmDelLid = rec.licenseId }) {
                                                Text("Delete", color = RedCol, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── REVOCATIONS ───────────────────────────────────────
                        NewProductManagerViewModel.Tab.REVOCATIONS -> {
                            AppCard {
                                SectionLabel("Revocation Export", OrangeCol)
                                BodyText("Generate a revocation JSON and upload to GitHub so customer apps can check it on launch.", SubText)
                                ActionButton("Generate Revocation JSON", col) { vm.generateRevoJson() }
                                if (revoText.isNotEmpty()) {
                                    OutlinedTextField(value = revoText, onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = GreenCol, unfocusedTextColor = GreenCol))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { clipboard.setText(AnnotatedString(revoText)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                            Text("Copy JSON", color = Color.White)
                                        }
                                        Button(onClick = { vm.saveRevoFile() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                            Text("Save File", color = Color.White)
                                        }
                                    }
                                }
                            }
                            AppCard {
                                SectionLabel("GitHub Upload", GreenCol)
                                val presets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" } }
                                if (presets.isNotEmpty()) {
                                    var exp by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { exp = true }
                                        DropdownMenu(expanded = exp, onDismissRequest = { exp = false }, containerColor = CardBg) {
                                            presets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghRvOwner = p.owner; vm.ghRvRepo = p.repo
                                                    vm.ghRvBranch = p.branch.ifBlank{"main"}
                                                    vm.ghRvPath = p.path; vm.ghRvToken = p.token
                                                    exp = false
                                                })
                                            }
                                        }
                                    }
                                }
                                BodyText("Owner", SubText);   AppTextField(vm.ghRvOwner,  { vm.ghRvOwner  = it }, "GitHub Owner")
                                BodyText("Repo", SubText);    AppTextField(vm.ghRvRepo,   { vm.ghRvRepo   = it }, "Repository Name")
                                BodyText("Branch", SubText);  AppTextField(vm.ghRvBranch, { vm.ghRvBranch = it }, "Branch")
                                BodyText("Path", SubText);    AppTextField(vm.ghRvPath,   { vm.ghRvPath   = it }, "Path in repo")
                                BodyText("Token", SubText);   AppTextField(vm.ghRvToken,  { vm.ghRvToken  = it }, "GitHub Token", password = true)
                                ActionButton("Upload Revocation to GitHub", GreenCol) { vm.uploadRevocation() }
                                if (hasAuth) {
                                    ActionButton("Copy Public Key PEM", TealCol) {
                                        clipboard.setText(AnnotatedString(NewLicenseStore.publicKeyPem(prod.id)))
                                    }
                                }
                                // Show revoked IDs list
                                val revIds = remember(prod.id) { NewLicenseStore.loadRevocations()[prod.id] ?: emptyList() }
                                if (revIds.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    BodyText("Revoked IDs (${revIds.size}):", SubText)
                                    revIds.take(20).forEach { BodyText("• $it", RedCol) }
                                }
                            }
                        }

                        // ── BACKUPS ───────────────────────────────────────────
                        NewProductManagerViewModel.Tab.BACKUPS -> {
                            AppCard {
                                SectionLabel("Backups", BlueCol)
                                BodyText("Export encrypted backups to Downloads/SLACKLINE ADMIN FILES/License_Backups/", SubText)
                                AppTextField(vm.bkPw, { vm.bkPw = it }, "Backup Password", password = true)
                                ActionButton("Export Authority Backup",    BlueCol) { vm.exportBackup("auth") }
                                ActionButton("Export License List Backup", BlueCol) { vm.exportBackup("list") }
                                ActionButton("Export Full Backup",         BlueCol) { vm.exportBackup("full") }
                            }
                        }
                    }
                } ?: BodyText("Loading product...", SubText)
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }
}
