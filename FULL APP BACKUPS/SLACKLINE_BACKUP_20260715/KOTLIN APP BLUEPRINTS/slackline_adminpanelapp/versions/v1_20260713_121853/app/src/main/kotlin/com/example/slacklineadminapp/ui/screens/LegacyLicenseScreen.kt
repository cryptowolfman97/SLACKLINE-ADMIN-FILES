package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

class LegacyLicenseViewModel : ViewModel() {

    private val _products   = MutableStateFlow<List<ProductConfig>>(emptyList())
    val products: StateFlow<List<ProductConfig>> = _products

    private val _loading    = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _toast      = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _confirmDel = MutableStateFlow<ProductConfig?>(null)
    val confirmDel: StateFlow<ProductConfig?> = _confirmDel

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        _loading.value  = true
        _products.value = ProductRegistry.all()
        _loading.value  = false
    }

    fun requestDelete(c: ProductConfig) { _confirmDel.value = c }
    fun cancelDelete()                   { _confirmDel.value = null }
    fun consumeToast()                   { _toast.value = "" }

    fun confirmDelete(c: ProductConfig) = viewModelScope.launch(Dispatchers.IO) {
        ProductRegistry.remove(c.id)
        EngineCache.invalidate(c.id)
        load()
        _toast.value      = "\"${c.displayName}\" deleted."
        _confirmDel.value = null
    }

    fun backupProduct(c: ProductConfig, pw: String) = viewModelScope.launch(Dispatchers.IO) {
        if (pw.isBlank()) { _toast.value = "Backup password required."; return@launch }
        val eng = EngineCache.get(c)
        if (!eng.hasAuthority()) { _toast.value = "No authority for ${c.displayName}."; return@launch }
        try {
            eng.saveAuthBackupFile(eng.buildBackupBlob(pw, "authority_only",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText())))
            eng.saveListBackupFile(eng.buildBackupBlob(pw, "license_list_only", eng.loadRecords()))
            eng.saveFullBackupFile(eng.buildBackupBlob(pw, "full_backup",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                      "licenses" to eng.loadRecords())))
            _toast.value = "All 3 backups saved for ${c.displayName}."
        } catch (e: Exception) { _toast.value = "Backup failed: ${e.message}" }
    }

    fun backupAllProducts(pw: String) = viewModelScope.launch(Dispatchers.IO) {
        if (pw.isBlank()) { _toast.value = "Backup password required."; return@launch }
        val prods = _products.value
        var success = 0; var skipped = 0
        prods.forEach { c ->
            val eng = EngineCache.get(c)
            if (!eng.hasAuthority()) { skipped++; return@forEach }
            try {
                eng.saveAuthBackupFile(eng.buildBackupBlob(pw, "authority_only",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText())))
                eng.saveListBackupFile(eng.buildBackupBlob(pw, "license_list_only", eng.loadRecords()))
                eng.saveFullBackupFile(eng.buildBackupBlob(pw, "full_backup",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                          "licenses" to eng.loadRecords())))
                success++
            } catch (_: Exception) { skipped++ }
        }
        _toast.value = "Backed up $success product(s). Skipped $skipped (no authority)."
    }
}

@Composable
fun LegacyLicenseScreen(
    onNavigateBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddProduct: () -> Unit,
    vm: LegacyLicenseViewModel = viewModel()
) {
    val products   by vm.products.collectAsState()
    val loading    by vm.loading.collectAsState()
    val toast      by vm.toast.collectAsState()
    val confirmDel by vm.confirmDel.collectAsState()
    val appColors   = LocalAppColors.current

    // Per-product backup dialog
    var backupFor  by remember { mutableStateOf<ProductConfig?>(null) }
    var backupPw   by remember { mutableStateOf("") }

    // Backup all dialog
    var showBackupAll    by remember { mutableStateOf(false) }
    var backupAllPw      by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    confirmDel?.let { c ->
        ConfirmDialog(
            title        = "Delete Product",
            message      = "Delete \"${c.displayName}\"? This cannot be undone.",
            confirmText  = "Delete",
            confirmColor = RedCol,
            onConfirm    = { vm.confirmDelete(c) },
            onDismiss    = { vm.cancelDelete() }
        )
    }

    // Backup All password dialog
    if (showBackupAll) {
        AlertDialog(
            onDismissRequest = { showBackupAll = false; backupAllPw = "" },
            containerColor   = CardBg,
            title = { Text("Backup All Products", color = OrangeCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Enter a password for all generated backup files.", SubText)
                    AppTextField(backupAllPw, { backupAllPw = it }, "Backup Password", password = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.backupAllProducts(backupAllPw)
                    showBackupAll = false; backupAllPw = ""
                }) { Text("BACKUP ALL", color = OrangeCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showBackupAll = false; backupAllPw = "" }) {
                    Text("CANCEL", color = SubText)
                }
            }
        )
    }

    // Per-product backup password dialog
    backupFor?.let { c ->
        AlertDialog(
            onDismissRequest = { backupFor = null; backupPw = "" },
            containerColor   = CardBg,
            title = { Text("Backup Password", color = TealCol, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Enter a password for the encrypted backup files.", SubText)
                    AppTextField(backupPw, { backupPw = it }, "Backup Password", password = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.backupProduct(c, backupPw)
                    backupFor = null; backupPw = ""
                }) { Text("BACKUP", color = GreenCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { backupFor = null; backupPw = "" }) {
                    Text("CANCEL", color = SubText)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("License Manager", TealCol, 20)

            if (toast.isNotEmpty()) {
                AppCard { BodyText(toast, TealCol) }
            }

            // ── Backup All Products ─────────────────────────────────────────
            ActionButton("Backup All Products", OrangeCol) { showBackupAll = true }

            if (loading) {
                Box(Modifier.fillMaxWidth().height(100.dp)) { LoadingOverlay() }
            } else if (products.isEmpty()) {
                AppCard { BodyText("No products yet. Tap + ADD to create one.", SubText) }
            } else {
                products.forEach { cfg ->
                    val engine = remember(cfg.id) { EngineCache.get(cfg) }
                    val stats  = remember(cfg.id) { engine.stats() }
                    val col = remember(cfg.color) {
                        runCatching { Color(android.graphics.Color.parseColor(cfg.color)) }
                            .getOrDefault(TealCol)
                    }

                    AppCard(color = CardBg2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cfg.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (engine.hasAuthority()) "Auth" else "No Auth",
                                color = if (engine.hasAuthority()) GreenCol else RedCol,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        BodyText("Prefix: ${cfg.activationPrefix}-  •  FP: ${engine.fingerprint()}", SubText)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { StatCard(stats.first,  "Active",  col) }
                            Box(Modifier.weight(1f)) { StatCard(stats.second, "Revoked", RedCol) }
                            Box(Modifier.weight(1f)) { StatCard(stats.third,  "Total",   BlueCol) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick  = { onOpenProduct(cfg.id) },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = col)
                            ) { Text("Open Manager", fontSize = 11.sp, color = Color.White) }
                            Button(
                                onClick  = { backupFor = cfg },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = OrangeCol)
                            ) { Text("Backup All", fontSize = 11.sp, color = Color.White) }
                            Button(
                                onClick  = { vm.requestDelete(cfg) },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = RedCol)
                            ) { Text("Delete Product", fontSize = 11.sp, color = Color.White) }
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("DASHBOARD" to onNavigateBack, "+ ADD" to onAddProduct))
    }
}
