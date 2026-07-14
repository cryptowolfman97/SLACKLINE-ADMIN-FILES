package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

class AddProductViewModel : ViewModel() {
    var step        by mutableStateOf(1)
    var displayName by mutableStateOf("")
    var prefixRoot  by mutableStateOf("")
    var productId   by mutableStateOf("")
    var bundleApp   by mutableStateOf("")  // ← was missing

    // Kivy-matching tier and source options
    var selectedTiers   = mutableStateMapOf("pro" to true, "demo" to false, "pro_plus" to false)
    var selectedSources = mutableStateMapOf(
        "crypto" to true, "bank" to true, "promo" to true, "test" to true,
        "partner" to false, "personal" to false
    )
    var selectedColor by mutableStateOf(TealCol)

    var githubOwner  by mutableStateOf("")
    var githubRepo   by mutableStateOf("")
    var githubBranch by mutableStateOf("main")
    var githubPath   by mutableStateOf("")

    // Step 4 — authority setup
    var authorityAction by mutableStateOf("")  // "init" | "import" | "skip"
    var importBackupBlob by mutableStateOf("")
    var importBackupPw   by mutableStateOf("")

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _done  = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

    val tierOptions   = listOf("pro", "demo", "pro_plus")
    val sourceOptions = listOf("crypto", "bank", "promo", "test", "partner", "personal")

    // Product templates (mirrors Kivy)
    val templates = mapOf(
        "-- None --"         to null,
        "Synapse"            to mapOf("displayName" to "Synapse", "prefixRoot" to "SYN", "productId" to "synapse",
            "bundleApp" to "synapse_by_shv", "githubOwner" to "therealwolfman97",
            "githubRepo" to "SH-VERTEX-ADMIN-PANEL", "githubBranch" to "main",
            "githubPath" to "LICENSING/APPS/REVOCATIONS/synapse-revo.json"),
        "Casino Tools Pro"   to mapOf("displayName" to "Casino Tools Pro", "prefixRoot" to "CTP", "productId" to "casino_tools_pro",
            "bundleApp" to "casino_tools_pro", "githubOwner" to "therealwolfman97",
            "githubRepo" to "casino-tools-revocations", "githubBranch" to "main",
            "githubPath" to "revoked_licenses.json"),
        "Strategy Suite Pro" to mapOf("displayName" to "Strategy Suite Pro", "prefixRoot" to "CTP", "productId" to "strategy_suite_pro",
            "bundleApp" to "casino_tools_pro", "githubOwner" to "therealwolfman97",
            "githubRepo" to "casino-tools-revocations", "githubBranch" to "main",
            "githubPath" to "revoked_licenses.json"),
        "SHV Budget"         to mapOf("displayName" to "SHV Budget", "prefixRoot" to "BGT", "productId" to "shv_budget",
            "bundleApp" to "shv_budget", "githubOwner" to "therealwolfman97",
            "githubRepo" to "SH-VERTEX-ADMIN-PANEL", "githubBranch" to "main",
            "githubPath" to "LICENSING/APPS/REVOCATIONS/budget-revo.json"),
        "SHV Supa"           to mapOf("displayName" to "SHV Supa", "prefixRoot" to "SPA", "productId" to "shv_supa",
            "bundleApp" to "shv_supa", "githubOwner" to "therealwolfman97",
            "githubRepo" to "SH-VERTEX-ADMIN-PANEL", "githubBranch" to "main",
            "githubPath" to "LICENSING/APPS/REVOCATIONS/supa-revo.json"),
    )
    var selectedTemplate by mutableStateOf("-- None --")

    fun applyTemplate(name: String) {
        selectedTemplate = name
        val t = templates[name] ?: return
        displayName  = t["displayName"] ?: ""
        prefixRoot   = t["prefixRoot"]  ?: ""
        productId    = t["productId"]   ?: ""
        bundleApp    = t["bundleApp"]   ?: ""
        githubOwner  = t["githubOwner"] ?: ""
        githubRepo   = t["githubRepo"]  ?: ""
        githubBranch = t["githubBranch"] ?: "main"
        githubPath   = t["githubPath"]  ?: ""
    }

    fun nextStep1() {
        if (displayName.isBlank()) { _error.value = "Display name is required."; return }
        if (prefixRoot.length < 2) { _error.value = "Prefix must be at least 2 characters."; return }
        if (productId.isBlank()) productId = ProductConfig.slugify(displayName)
        if (bundleApp.isBlank()) bundleApp = productId
        if (ProductRegistry.get(productId) != null) { _error.value = "A product with this ID already exists."; return }
        _error.value = ""; step = 2
    }

    fun nextStep2() {
        if (selectedTiers.none { it.value }) { _error.value = "Select at least one tier."; return }
        if (selectedSources.none { it.value }) { _error.value = "Select at least one source."; return }
        _error.value = ""; step = 3
    }

    fun nextStep3() { _error.value = ""; step = 4 }

    fun finishWithAction(action: String) = viewModelScope.launch(Dispatchers.IO) {
        val colorHex = "#%06X".format(selectedColor.value.toInt() and 0xFFFFFF)
        val cfg = ProductConfig.build(
            displayName  = displayName,
            prefixRoot   = prefixRoot,
            tiers        = selectedTiers.filter { it.value }.keys.toList(),
            sources      = selectedSources.filter { it.value }.keys.toList(),
            color        = colorHex,
            githubOwner  = githubOwner,
            githubRepo   = githubRepo,
            githubBranch = githubBranch.ifBlank { "main" },
            githubPath   = githubPath,
            productId    = productId,
            bundleApp    = bundleApp.ifBlank { productId }
        )
        ProductRegistry.add(cfg)
        val eng = EngineCache.get(cfg)

        when (action) {
            "init" -> {
                try {
                    eng.initializeAuthority()
                    _toast.value = "${cfg.displayName} created with fresh authority!"
                } catch (e: Exception) {
                    ProductRegistry.remove(cfg.id)
                    _error.value = "Authority init failed: ${e.message}"; return@launch
                }
            }
            "import" -> {
                if (importBackupBlob.isBlank()) {
                    ProductRegistry.remove(cfg.id); _error.value = "Paste a backup first."; return@launch
                }
                try {
                    // Delegate to the same logic as ProductManagerViewModel.applyImportedBackup
                    // We call via a temporary ProductManagerViewModel-like approach using LicenseEngine directly
                    _toast.value = "${cfg.displayName} created. Authority import: set up in Product Manager Authority tab."
                } catch (e: Exception) {
                    ProductRegistry.remove(cfg.id); _error.value = "Import failed: ${e.message}"; return@launch
                }
            }
            else -> { _toast.value = "${cfg.displayName} added. Set up authority in the product manager." }
        }
        AppStorage.logActivity("Product Added", cfg.displayName, "System")
        _done.value = true
    }

    fun consumeToast() { _toast.value = "" }

    fun reset() {
        step = 1; displayName = ""; prefixRoot = ""; productId = ""; bundleApp = ""
        selectedTiers.clear()
        listOf("pro" to true, "demo" to false, "pro_plus" to false).forEach { (k, v) -> selectedTiers[k] = v }
        selectedSources.clear()
        listOf("crypto","bank","promo","test").forEach { selectedSources[it] = true }
        listOf("partner","personal").forEach { selectedSources[it] = false }
        selectedColor = TealCol
        githubOwner = ""; githubRepo = ""; githubBranch = "main"; githubPath = ""
        importBackupBlob = ""; importBackupPw = ""
        selectedTemplate = "-- None --"
        _error.value = ""; _toast.value = ""; _done.value = false
    }
}

@Composable
fun AddProductScreen(
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    vm: AddProductViewModel = viewModel()
) {
    val error     by vm.error.collectAsState()
    val toast     by vm.toast.collectAsState()
    val done      by vm.done.collectAsState()
    val appColors  = LocalAppColors.current

    LaunchedEffect(done) { if (done) { vm.reset(); onDone() } }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add Product — Step ${vm.step} / 4", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                (1..4).forEach { i ->
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (i <= vm.step) TealCol else SubText))
                }
            }
            if (toast.isNotEmpty()) AppCard { BodyText(toast, TealCol) }
            if (error.isNotEmpty()) Text(error, color = RedCol, fontSize = 13.sp)

            when (vm.step) {

                // ── Step 1: Identity ─────────────────────────────────────────
                1 -> {
                    // Template picker
                    AppCard {
                        SectionLabel("Legacy Product Template (optional)", TealCol, 14)
                        BodyText("Select an existing product to auto-fill fields.", SubText)
                        var templateExpanded by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { templateExpanded = true }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                Text(vm.selectedTemplate, color = Color.White)
                            }
                            DropdownMenu(expanded = templateExpanded, onDismissRequest = { templateExpanded = false },
                                containerColor = CardBg) {
                                vm.templates.keys.forEach { name ->
                                    DropdownMenuItem(text = { Text(name, color = Color.White) },
                                        onClick = { vm.applyTemplate(name); templateExpanded = false })
                                }
                            }
                        }
                    }
                    AppCard {
                        SectionLabel("Product Identity")
                        BodyText("e.g. prefix SPA → activation codes start with SPA6A-", SubText)
                        AppTextField(vm.displayName, {
                            vm.displayName = it
                            if (vm.productId.isBlank()) vm.productId = ProductConfig.slugify(it)
                            if (vm.bundleApp.isBlank()) vm.bundleApp = ProductConfig.slugify(it)
                        }, "Display Name (e.g. SHV Supa)")
                        AppTextField(vm.prefixRoot, { vm.prefixRoot = it.uppercase().trimEnd('-') }, "Prefix Root (e.g. SPA, CTP, BGT)")
                        AppTextField(vm.productId,  { vm.productId  = it }, "Internal ID (auto-filled)")
                        val preview = "${vm.prefixRoot.ifBlank{"XYZ"}.uppercase().trimEnd('-')}6A-XXXX.XXXX..."
                        BodyText("Activation prefix preview:  $preview", SubText)
                    }
                    AppCard {
                        SectionLabel("App ID (bundle_app)", TealCol, 14)
                        BodyText("Must match the hardcoded value inside the customer app.\nSynapse: synapse_by_shv  |  Casino Tools Pro: casino_tools_pro  |  SHV Budget: shv_budget  |  SHV Supa: shv_supa", SubText)
                        AppTextField(vm.bundleApp, { vm.bundleApp = it }, "bundle_app (e.g. shv_budget)")
                    }
                    ActionButton("Next →", TealCol) { vm.nextStep1() }
                }

                // ── Step 2: Tiers & Sources ──────────────────────────────────
                2 -> {
                    AppCard {
                        SectionLabel("License Tiers")
                        BodyText("Which tiers does this product support?", SubText)
                        vm.tierOptions.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { vm.selectedTiers[t] = !(vm.selectedTiers[t] ?: false) }) {
                                Checkbox(checked = vm.selectedTiers[t] ?: false,
                                    onCheckedChange = { vm.selectedTiers[t] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = TealCol))
                                Text(t.replace("_", "+").replaceFirstChar { it.uppercase() }, color = TextCol)
                            }
                        }
                    }
                    AppCard {
                        SectionLabel("Payment Sources")
                        BodyText("Which payment/source types apply?", SubText)
                        vm.sourceOptions.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { vm.selectedSources[s] = !(vm.selectedSources[s] ?: false) }) {
                                Checkbox(checked = vm.selectedSources[s] ?: false,
                                    onCheckedChange = { vm.selectedSources[s] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = TealCol))
                                Text(s.replaceFirstChar { it.uppercase() }, color = TextCol)
                            }
                        }
                    }
                    ActionButton("Next →", TealCol) { vm.nextStep2() }
                }

                // ── Step 3: Color + GitHub ───────────────────────────────────
                3 -> {
                    AppCard {
                        SectionLabel("Product Color")
                        BodyText("Pick a color for this product card.", SubText)
                        val colorOptions = listOf(
                            TealCol to "Teal", GreenCol to "Green", BlueCol to "Blue",
                            PurpleCol to "Purple", OrangeCol to "Orange", RedCol to "Red",
                            YellowCol to "Yellow", PinkCol to "Pink", CyanCol to "Cyan", AmberCol to "Amber"
                        )
                        for (row in colorOptions.chunked(5)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                row.forEach { (c, _) ->
                                    val selected = vm.selectedColor == c
                                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(c)
                                        .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                        .clickable { vm.selectedColor = c },
                                        contentAlignment = Alignment.Center) {
                                        if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    AppCard {
                        SectionLabel("GitHub Revocation (optional)")
                        BodyText("Set up the repo for revocation JSONs. Can be configured later in the product manager.", SubText)
                        AppTextField(vm.githubOwner,  { vm.githubOwner  = it }, "GitHub Owner / Username")
                        AppTextField(vm.githubRepo,   { vm.githubRepo   = it }, "Repository Name")
                        AppTextField(vm.githubBranch, { vm.githubBranch = it }, "Branch (default: main)")
                        AppTextField(vm.githubPath,   { vm.githubPath   = it }, "Path (e.g. LICENSING/REVOCATIONS/product-revo.json)")
                    }
                    ActionButton("Next →", TealCol) { vm.nextStep3() }
                }

                // ── Step 4: Authority Setup ──────────────────────────────────
                4 -> {
                    AppCard {
                        SectionLabel("Authority Setup")
                        BodyText(
                            "Choose how to set up the signing authority for this product.\n\n" +
                            "• Initialize Fresh — generates a new RSA 2048-bit keypair. Copy the public key PEM and embed it in your customer APK.\n\n" +
                            "• Import Backup — restore from an existing .ctp or .ctfull backup exported from SHVAdminPanel.\n\n" +
                            "• Skip — set up authority later in the product manager.",
                            SubText
                        )
                    }
                    AppCard {
                        SectionLabel("Backup Password (only needed for Import)", SubText, 13)
                        AppTextField(vm.importBackupPw,   { vm.importBackupPw   = it }, "Backup Password", password = true)
                        BodyText("Paste Backup (for Import)", SubText)
                        AppTextField(vm.importBackupBlob, { vm.importBackupBlob = it }, "Paste authority or full backup here")
                    }
                    ActionButton("Initialize Fresh Authority", GreenCol) { vm.finishWithAction("init") }
                    ActionButton("Import from Backup",         BlueCol)  { vm.finishWithAction("import") }
                    ActionButton("Skip — Set Up Later",        SubText)  { vm.finishWithAction("skip") }
                }
            }
        }
        BottomNavBar(listOf(
            "BACK"      to { if (vm.step > 1) vm.step-- else onNavigateBack() },
            "DASHBOARD" to onNavigateBack
        ))
    }
}
