package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
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

class AppBackupViewModel : ViewModel() {

    private val _tab = MutableStateFlow(0) // 0 = Backup, 1 = Restore
    val tab: StateFlow<Int> = _tab

    private val _presets = MutableStateFlow<List<CloudPreset>>(emptyList())
    val presets: StateFlow<List<CloudPreset>> = _presets

    private val _selectedPreset = MutableStateFlow<CloudPreset?>(null)
    val selectedPreset: StateFlow<CloudPreset?> = _selectedPreset

    private val _selectedModules = MutableStateFlow<Set<String>>(AppStorage.BACKUP_MODULES.map { it.label }.toSet())
    val selectedModules: StateFlow<Set<String>> = _selectedModules

    private val _backupName = MutableStateFlow(defaultBackupName())
    val backupName: StateFlow<String> = _backupName

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _backups = MutableStateFlow<List<BackupSummary>>(emptyList())
    val backups: StateFlow<List<BackupSummary>> = _backups

    private val _selectedBackup = MutableStateFlow<BackupSummary?>(null)
    val selectedBackup: StateFlow<BackupSummary?> = _selectedBackup

    private val _restoreMode = MutableStateFlow(RestoreMode.FULL_REPLACE)
    val restoreMode: StateFlow<RestoreMode> = _restoreMode

    private val _loadingBackups = MutableStateFlow(false)
    val loadingBackups: StateFlow<Boolean> = _loadingBackups

    private val _confirmRestore = MutableStateFlow(false)
    val confirmRestore: StateFlow<Boolean> = _confirmRestore

    companion object {
        fun defaultBackupName(): String = "SLACKLINE_BACKUP_${AppStorage.timestamp()}"
    }

    fun selectTab(i: Int) { _tab.value = i; _toast.value = "" }

    fun load() {
        _presets.value = CloudPresetsStore.loadAll().filter { it.type == "github_path" }
        if (_selectedPreset.value == null) _selectedPreset.value = _presets.value.firstOrNull()
    }

    fun selectPreset(p: CloudPreset) { _selectedPreset.value = p; _backups.value = emptyList() }
    fun setBackupName(s: String) { _backupName.value = s }
    fun toggleModule(label: String) {
        _selectedModules.value = if (_selectedModules.value.contains(label))
            _selectedModules.value - label else _selectedModules.value + label
    }
    fun selectAllModules()  { _selectedModules.value = AppStorage.BACKUP_MODULES.map { it.label }.toSet() }
    fun selectNoModules()   { _selectedModules.value = emptySet() }
    fun consumeToast()      { _toast.value = "" }

    fun runBackup() = viewModelScope.launch {
        val preset = _selectedPreset.value
        if (preset == null) { _toast.value = "Pick a GitHub preset first."; return@launch }
        if (_backupName.value.isBlank()) { _toast.value = "Backup name required."; return@launch }
        if (_selectedModules.value.isEmpty()) { _toast.value = "Select at least one module."; return@launch }

        _running.value = true
        _log.value = listOf("Starting backup \"${_backupName.value}\"…")
        try {
            withContext(Dispatchers.IO) {
                AppBackupEngine.runBackup(
                    preset, _backupName.value, _selectedModules.value.toList()
                ) { line -> _log.value = _log.value + line }
            }
            _toast.value = "Backup complete."
            _backupName.value = defaultBackupName()
        } catch (e: Exception) {
            _log.value = _log.value + "ERROR: ${e.message}"
            _toast.value = "Backup failed: ${e.message}"
        } finally {
            _running.value = false
        }
    }

    fun loadBackups() = viewModelScope.launch {
        val preset = _selectedPreset.value ?: return@launch
        _loadingBackups.value = true
        try {
            _backups.value = withContext(Dispatchers.IO) { AppBackupEngine.listBackups(preset) }
        } catch (e: Exception) {
            _toast.value = "Couldn't load backups: ${e.message}"
        } finally {
            _loadingBackups.value = false
        }
    }

    fun selectBackup(b: BackupSummary?) { _selectedBackup.value = b }
    fun setRestoreMode(m: RestoreMode)  { _restoreMode.value = m }
    fun askConfirmRestore(show: Boolean) { _confirmRestore.value = show }

    fun runRestore() = viewModelScope.launch {
        val preset = _selectedPreset.value ?: return@launch
        val backup = _selectedBackup.value ?: return@launch
        _confirmRestore.value = false
        _running.value = true
        _log.value = listOf("Starting restore \"${backup.manifest.name}\" (${_restoreMode.value.name})…")
        try {
            withContext(Dispatchers.IO) {
                AppBackupEngine.runRestore(preset, backup, _restoreMode.value) { line ->
                    _log.value = _log.value + line
                }
            }
            _toast.value = "Restore complete."
        } catch (e: Exception) {
            _log.value = _log.value + "ERROR: ${e.message}"
            _toast.value = "Restore failed: ${e.message}"
        } finally {
            _running.value = false
        }
    }
}

@Composable
fun AppBackupScreen(
    onNavigateBack: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    vm: AppBackupViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val tab             by vm.tab.collectAsState()
    val presets         by vm.presets.collectAsState()
    val selectedPreset  by vm.selectedPreset.collectAsState()
    val selectedModules by vm.selectedModules.collectAsState()
    val backupName      by vm.backupName.collectAsState()
    val running         by vm.running.collectAsState()
    val log             by vm.log.collectAsState()
    val toast           by vm.toast.collectAsState()
    val backups         by vm.backups.collectAsState()
    val selectedBackup  by vm.selectedBackup.collectAsState()
    val restoreMode     by vm.restoreMode.collectAsState()
    val loadingBackups  by vm.loadingBackups.collectAsState()
    val confirmRestore  by vm.confirmRestore.collectAsState()

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(toast) { /* toast is one-shot; shown via Snackbar-less inline banner below */ }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("⭐ App Backup & Restore", AmberCol, 20)
            }
            BodyText("Full-app, per-module backups to GitHub — mirrors your local SLACKLINE ADMIN FILES structure exactly.", SubText)

            if (toast.isNotEmpty()) {
                AppCard(color = Color(0xFF241A00)) {
                    Text(toast, color = AmberCol, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Backup" to 0, "Restore" to 1).forEach { (label, i) ->
                    Button(
                        onClick  = { vm.selectTab(i); if (i == 1) vm.loadBackups() },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (tab == i) AmberCol else CardBg2
                        )
                    ) { Text(label, color = if (tab == i) Color.Black else TextCol, fontWeight = FontWeight.Bold) }
                }
            }

            AppCard {
                SectionLabel("GitHub Preset", TealCol)
                if (presets.isEmpty()) {
                    BodyText("No GitHub Path presets found. Add one in Cloud Presets first.", RedCol)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPreset?.name == p.name,
                                    onClick  = { vm.selectPreset(p); if (tab == 1) vm.loadBackups() },
                                    colors   = RadioButtonDefaults.colors(selectedColor = AmberCol)
                                )
                                Column {
                                    Text(p.name, color = TextCol, fontWeight = FontWeight.SemiBold)
                                    Text("${p.owner}/${p.repo}  ·  ${p.path.ifBlank { "/" }}", color = SubText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (tab == 0) {
                BackupTab(
                    backupName = backupName,
                    onNameChange = vm::setBackupName,
                    selectedModules = selectedModules,
                    onToggle = vm::toggleModule,
                    onAll = vm::selectAllModules,
                    onNone = vm::selectNoModules,
                    running = running,
                    onStart = vm::runBackup
                )
            } else {
                RestoreTab(
                    backups = backups,
                    loading = loadingBackups,
                    selectedBackup = selectedBackup,
                    onSelect = vm::selectBackup,
                    restoreMode = restoreMode,
                    onModeChange = vm::setRestoreMode,
                    running = running,
                    onRestore = { vm.askConfirmRestore(true) },
                    onRefresh = vm::loadBackups
                )
            }

            if (log.isNotEmpty()) {
                AppCard(color = CardBg2) {
                    SectionLabel(if (running) "Working…" else "Log", CyanCol)
                    log.takeLast(30).forEach { line -> Text(line, color = SubText, fontSize = 11.sp) }
                }
            }

            ActionButton("Recycle Bin (Backups)", PurpleCol, onClick = onOpenRecycleBin)
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "Restore \"${selectedBackup?.manifest?.name}\"?",
            message = "Mode: ${restoreMode.name}. " +
                if (restoreMode == RestoreMode.FULL_REPLACE)
                    "Existing files in the selected modules will be moved to the Recycle Bin and replaced exactly with the backup's files."
                else
                    "Backup files will be written over matching local files. Anything local not in the backup is left untouched.",
            confirmText = "Restore",
            confirmColor = AmberCol,
            onConfirm = { vm.runRestore() },
            onDismiss = { vm.askConfirmRestore(false) }
        )
    }
}

@Composable
private fun BackupTab(
    backupName: String,
    onNameChange: (String) -> Unit,
    selectedModules: Set<String>,
    onToggle: (String) -> Unit,
    onAll: () -> Unit,
    onNone: () -> Unit,
    running: Boolean,
    onStart: () -> Unit
) {
    AppCard {
        SectionLabel("Backup Name", TealCol)
        AppTextField(backupName, onNameChange, "Backup Name")
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Modules (${selectedModules.size}/${AppStorage.BACKUP_MODULES.size})", TealCol)
            Row {
                TextButton(onClick = onAll)  { Text("ALL", color = TealCol, fontSize = 12.sp) }
                TextButton(onClick = onNone) { Text("NONE", color = SubText, fontSize = 12.sp) }
            }
        }
        AppStorage.BACKUP_MODULES.forEach { m ->
            val checked = selectedModules.contains(m.label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(m.label) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (checked) TealCol else SubText,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(m.label, color = TextCol, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }

    ActionButton(
        text = if (running) "Backing up…" else "Start Backup",
        color = AmberCol,
        enabled = !running,
        onClick = onStart
    )
}

@Composable
private fun RestoreTab(
    backups: List<BackupSummary>,
    loading: Boolean,
    selectedBackup: BackupSummary?,
    onSelect: (BackupSummary) -> Unit,
    restoreMode: RestoreMode,
    onModeChange: (RestoreMode) -> Unit,
    running: Boolean,
    onRestore: () -> Unit,
    onRefresh: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Available Backups", TealCol)
            TextButton(onClick = onRefresh) { Text("REFRESH", color = TealCol, fontSize = 12.sp) }
        }
        when {
            loading -> BodyText("Loading backups…", SubText)
            backups.isEmpty() -> BodyText("No backups found at this preset's path.", SubText)
            else -> backups.forEach { b ->
                val isSel = selectedBackup?.manifest?.name == b.manifest.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSel,
                        onClick  = { onSelect(b) },
                        colors   = RadioButtonDefaults.colors(selectedColor = AmberCol)
                    )
                    Column {
                        Text(b.manifest.name, color = TextCol, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${b.manifest.createdAt}  ·  ${b.manifest.modules.size} modules  ·  ${b.manifest.fileCount} files",
                            color = SubText, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (selectedBackup != null) {
        AppCard {
            SectionLabel("Modules in this backup", CyanCol)
            selectedBackup.manifest.modules.forEach { Text("• $it", color = TextCol, fontSize = 12.sp) }
        }

        AppCard {
            SectionLabel("Restore Mode", TealCol)
            listOf(
                RestoreMode.FULL_REPLACE to "Full Replace — exact match to backup, existing files go to Recycle Bin",
                RestoreMode.MERGE to "Merge — overwrite matching files, leave the rest untouched"
            ).forEach { (mode, desc) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = restoreMode == mode,
                        onClick  = { onModeChange(mode) },
                        colors   = RadioButtonDefaults.colors(selectedColor = AmberCol)
                    )
                    Text(desc, color = TextCol, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }

        ActionButton(
            text = if (running) "Restoring…" else "Restore This Backup",
            color = AmberCol,
            enabled = !running,
            onClick = onRestore
        )
    }
}
