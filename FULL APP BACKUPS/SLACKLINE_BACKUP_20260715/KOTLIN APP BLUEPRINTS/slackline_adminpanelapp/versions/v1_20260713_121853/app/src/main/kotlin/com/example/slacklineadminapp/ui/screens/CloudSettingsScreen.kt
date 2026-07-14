package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CloudSettingsViewModel : ViewModel() {
    private val _presets = MutableStateFlow<List<CloudPreset>>(emptyList())
    val presets: StateFlow<List<CloudPreset>> = _presets

    private val _showAdd = MutableStateFlow(false)
    val showAdd: StateFlow<Boolean> = _showAdd

    private val _toast   = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _pinGate = MutableStateFlow(false)
    val pinGate: StateFlow<Boolean> = _pinGate

    private val _pinBuf  = MutableStateFlow("")
    val pinBuf: StateFlow<String> = _pinBuf

    private val _pinErr  = MutableStateFlow("")
    val pinErr: StateFlow<String> = _pinErr

    fun load() { _presets.value = CloudPresetsStore.loadAll() }

    fun checkPin(ctx: android.content.Context) {
        _pinGate.value = SecurityConfig.get(ctx).advPin.isNotEmpty()
    }

    fun appendPin(d: String) { _pinBuf.value += d }
    fun clearPin()            { _pinBuf.value = ""; _pinErr.value = "" }
    fun backPin()             { _pinBuf.value = _pinBuf.value.dropLast(1); _pinErr.value = "" }

    fun verifyPin(ctx: android.content.Context) {
        val adv = SecurityConfig.get(ctx).advPin
        if (_pinBuf.value == adv) {
            _pinGate.value = false; _pinBuf.value = ""; _pinErr.value = ""
        } else {
            _pinErr.value = "Incorrect PIN"; _pinBuf.value = ""
        }
    }

    fun openAdd()      { _showAdd.value = true }
    fun closeAdd()     { _showAdd.value = false }
    fun consumeToast() { _toast.value = "" }

    fun savePreset(p: CloudPreset) = viewModelScope.launch {
        if (p.name.isBlank()) { _toast.value = "Preset name required."; return@launch }
        CloudPresetsStore.add(p)
        load()
        _showAdd.value = false
        _toast.value = "Preset saved."
    }

    fun deletePreset(name: String) = viewModelScope.launch {
        CloudPresetsStore.delete(name)
        load()
        _toast.value = "Preset deleted."
    }
}

@Composable
fun CloudSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenMainBackup: () -> Unit,
    onOpenUniversalBackup: () -> Unit,
    onOpenAppBackup: () -> Unit,
    vm: CloudSettingsViewModel = viewModel()
) {
    val ctx       = LocalContext.current
    val presets   by vm.presets.collectAsState()
    val showAdd   by vm.showAdd.collectAsState()
    val pinGate   by vm.pinGate.collectAsState()
    val pinBuf    by vm.pinBuf.collectAsState()
    val pinErr    by vm.pinErr.collectAsState()
    val toast     by vm.toast.collectAsState()
    val appColors  = LocalAppColors.current

    LaunchedEffect(Unit) { vm.checkPin(ctx); vm.load() }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    if (pinGate) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.bg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Advanced PIN Required", color = TealCol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            BodyText("Enter your 6-digit PIN to access Cloud Settings.", SubText)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { i ->
                    Text(
                        text     = if (i < pinBuf.length) "●" else "○",
                        fontSize = 28.sp,
                        color    = if (i < pinBuf.length) TealCol else SubText
                    )
                }
            }
            if (pinErr.isNotEmpty()) Text(pinErr, color = RedCol, fontSize = 13.sp)
            NumberPad(
                enteredDigits = pinBuf,
                maxLen        = 6,
                onDigit       = { d ->
                    vm.appendPin(d)
                    if ((pinBuf + d).length == 6) vm.verifyPin(ctx)
                },
                onClear = { vm.clearPin() },
                onBack  = { vm.backPin() }
            )
            TextButton(onClick = onNavigateBack) { Text("Cancel", color = SubText) }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Cloud Settings", TealCol, 20)
            BodyText("Manage cloud service credentials and presets.", SubText)

            // ── App Backup & Restore ────────────────────────────────────────
            // Deliberately styled to stand out from everything below it.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(18.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF241A00)),
                border   = BorderStroke(1.5.dp, AmberCol),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = AmberCol)
                        Spacer(Modifier.width(8.dp))
                        Text("⭐ APP BACKUP & RESTORE", color = AmberCol, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    BodyText(
                        "Back up every module — or just the ones you pick — straight to GitHub, mirrored exactly as it's stored on your device. Restore any backup later with Full Replace or Merge.",
                        Color(0xFFE0C68A)
                    )
                    ActionButton("Open Backup & Restore", AmberCol, onClick = onOpenAppBackup)
                }
            }

            AppCard {
                SectionLabel("Cloud Presets", TealCol)
                BodyText("Manage presets for GitHub paths, GitHub Manager accounts, and Supabase credentials.", SubText)
                ActionButton("Manage Presets", TealCol) { vm.openAdd() }
            }

            AppCard {
                SectionLabel("Main Universal Backup & Import", TealCol)
                BodyText("Backup or restore the entire system, including all products and non-licensing data.", SubText)
                ActionButton("Open Main Backup", TealCol, onClick = onOpenMainBackup)
            }

            AppCard {
                SectionLabel("Universal Backup (Non-Licensing)", TealCol)
                BodyText("Export or import all non-licensing data as a single encrypted bundle.", SubText)
                ActionButton("Open Universal Backup", TealCol, onClick = onOpenUniversalBackup)
            }

            if (presets.isNotEmpty()) {
                AppCard(color = CardBg2) {
                    SectionLabel("Saved Presets", CyanCol)
                    presets.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = TextCol, fontWeight = FontWeight.SemiBold)
                                Text(p.type, color = SubText, fontSize = 11.sp)
                            }
                            IconButton(onClick = { vm.deletePreset(p.name) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedCol)
                            }
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }

    if (showAdd) {
        AddPresetDialog(
            onDismiss = { vm.closeAdd() },
            onSave    = { vm.savePreset(it) }
        )
    }
}

@Composable
private fun AddPresetDialog(onDismiss: () -> Unit, onSave: (CloudPreset) -> Unit) {
    var presetType by remember { mutableStateOf("github_path") }
    var name       by remember { mutableStateOf("") }
    var owner      by remember { mutableStateOf("") }
    var repo       by remember { mutableStateOf("") }
    var branch     by remember { mutableStateOf("main") }
    var path       by remember { mutableStateOf("") }
    var token      by remember { mutableStateOf("") }
    var alias      by remember { mutableStateOf("") }
    var username   by remember { mutableStateOf("") }
    var projUrl    by remember { mutableStateOf("") }
    var projRef    by remember { mutableStateOf("") }
    var anonKey    by remember { mutableStateOf("") }
    var pat        by remember { mutableStateOf("") }
    var adminKey   by remember { mutableStateOf("") }
    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        title = { Text("Add Cloud Preset", color = TealCol, fontWeight = FontWeight.Bold) },
        text  = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("github_path", "github_admin", "supabase").forEach { t ->
                        Button(
                            onClick  = { presetType = t },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (presetType == t) TealCol else CardBg2
                            )
                        ) {
                            Text(t.replace("_", "\n"), fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
                AppTextField(name, { name = it }, "Preset Name *")
                when (presetType) {
                    "github_path" -> {
                        AppTextField(owner,  { owner  = it }, "GitHub Owner")
                        AppTextField(repo,   { repo   = it }, "Repository Name")
                        AppTextField(branch, { branch = it }, "Branch")
                        AppTextField(path,   { path   = it }, "Path")
                        AppTextField(token,  { token  = it }, "Token (PAT)", password = true)
                    }
                    "github_admin" -> {
                        AppTextField(alias,    { alias    = it }, "Alias")
                        AppTextField(username, { username = it }, "Username")
                        AppTextField(token,    { token    = it }, "Token (PAT)", password = true)
                    }
                    "supabase" -> {
                        AppTextField(projUrl,  { projUrl  = it }, "Project URL")
                        AppTextField(projRef,  { projRef  = it }, "Project Ref")
                        AppTextField(anonKey,  { anonKey  = it }, "Anon Key")
                        AppTextField(pat,      { pat      = it }, "Personal Access Token", password = true)
                        AppTextField(adminKey, { adminKey = it }, "Admin / Service Key",   password = true)
                        AppTextField(email,    { email    = it }, "Email")
                        AppTextField(password, { password = it }, "Password", password = true)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    CloudPreset(
                        name = name, type = presetType,
                        owner = owner, repo = repo, branch = branch, path = path, token = token,
                        alias = alias, username = username,
                        projectUrl = projUrl, projectRef = projRef, anonKey = anonKey,
                        personalAccessToken = pat, projectAdminKey = adminKey,
                        email = email, password = password
                    )
                )
            }) { Text("SAVE", color = TealCol, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) }
        }
    )
}
