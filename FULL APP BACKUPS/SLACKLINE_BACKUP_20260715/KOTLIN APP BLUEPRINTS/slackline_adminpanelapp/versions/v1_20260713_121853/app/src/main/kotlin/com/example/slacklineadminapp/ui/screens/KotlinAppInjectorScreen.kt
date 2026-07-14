package com.example.slacklineadminapp.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.AppStorage
import com.example.slacklineadminapp.data.KotlinLicenseRepository
import com.example.slacklineadminapp.data.KotlinProduct
import com.example.slacklineadminapp.data.PaymentPreset
import com.example.slacklineadminapp.engine.GateConfig
import com.example.slacklineadminapp.engine.KotlinInjectorEngine
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotlinAppInjectorScreen(onNavigateBack: () -> Unit) {
    val context  = LocalContext.current
    val products = remember { KotlinLicenseRepository.getAllProducts() }

    // ── Preset State Tracking Management ──────────────────────────────────
    var presetsList by remember { mutableStateOf(AppStorage.loadPaymentPresets()) }
    var showPresetsDialog by remember { mutableStateOf(false) }

    var selectedProduct   by remember { mutableStateOf<KotlinProduct?>(null) }
    var expandedDropdown  by remember { mutableStateOf(false) }
    var targetPackageName by remember { mutableStateOf("com.shvertex.app") }
    var appDisplayName    by remember { mutableStateOf("") }
    var proPrice          by remember { mutableStateOf("") }
    var gracePeriodHours  by remember { mutableStateOf("6") }

    val trialPresets    = listOf(24, 48, 72, 168)
    var selectedPreset  by remember { mutableStateOf(24) }
    var customHoursText by remember { mutableStateOf("") }
    var useCustomHours  by remember { mutableStateOf(false) }
    val effectiveDemoHours by remember {
        derivedStateOf {
            if (useCustomHours) (customHoursText.trim().toIntOrNull() ?: 24).coerceIn(1, 720)
            else selectedPreset
        }
    }

    var bankName      by remember { mutableStateOf("") }
    var bankAccount   by remember { mutableStateOf("") }
    var bankAccName   by remember { mutableStateOf("") }
    var bankBranch    by remember { mutableStateOf("") }
    var bankSwift     by remember { mutableStateOf("") }

    var cryptoUsdtBsc    by remember { mutableStateOf("") }
    var cryptoUsdtTrc    by remember { mutableStateOf("") }
    var cryptoUsdtPlasma by remember { mutableStateOf("") }
    var cryptoEth        by remember { mutableStateOf("") }
    var cryptoLtc        by remember { mutableStateOf("") }

    // Dropdown UI selection expansions inside wizard steps
    var expandedBankPresets by remember { mutableStateOf(false) }
    var expandedCryptoPresets by remember { mutableStateOf(false) }

    // Automatically apply default presets on initial layout initialization
    LaunchedEffect(Unit) {
        presetsList.find { it.isDefault }?.let { defaultPreset ->
            bankName = defaultPreset.bankName
            bankAccName = defaultPreset.bankAccName
            bankAccount = defaultPreset.bankAccount
            bankBranch = defaultPreset.bankBranch
            bankSwift = defaultPreset.bankSwift
            cryptoUsdtBsc = defaultPreset.cryptoUsdtBsc
            cryptoUsdtTrc = defaultPreset.cryptoUsdtTrc
            cryptoUsdtPlasma = defaultPreset.cryptoUsdtPlasma
            cryptoEth = defaultPreset.cryptoEth
            cryptoLtc = defaultPreset.cryptoLtc
        }
    }

    var disclaimer by remember {
        mutableStateOf(
            "After payment, tap your preferred contact method below. Your device code " +
            "will be included automatically. We will verify your payment and deliver " +
            "your Pro license key within 4 hours. Your trial will be extended " +
            "immediately upon payment confirmation so you can keep using the app."
        )
    }

    var showBankSection   by remember { mutableStateOf(false) }
    var showCryptoSection by remember { mutableStateOf(false) }

    var manifestUri    by remember { mutableStateOf<Uri?>(null) }
    var libsTomlUri    by remember { mutableStateOf<Uri?>(null) }
    var buildGradleUri by remember { mutableStateOf<Uri?>(null) }

    val manifestLauncher    = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) manifestUri = uri }
    val libsTomlLauncher    = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) libsTomlUri = uri }
    val buildGradleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) buildGradleUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KOTLIN APP INJECTOR", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { showPresetsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Payment Options - Presets", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. Product selector ───────────────────────────────────
            SectionHeader("1. Product Authority")
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedProduct?.name ?: "Select Product Authority",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Application Profile") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text("${product.name} (${product.appCode})") },
                            onClick = {
                                selectedProduct = product
                                expandedDropdown = false
                                if (appDisplayName.isBlank()) appDisplayName = product.name
                            }
                        )
                    }
                }
            }

            // ── 2. App identity ───────────────────────────────────────
            SectionHeader("2. App Identity")
            OutlinedTextField(
                value = appDisplayName,
                onValueChange = { appDisplayName = it },
                label = { Text("App Display Name on Gate Screen") },
                placeholder = { Text("Defaults to product name if blank") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = targetPackageName,
                onValueChange = { targetPackageName = it },
                label = { Text("Client App Package Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── 3. Pricing & Grace Period ─────────────────────────────
            SectionHeader("3. Pricing & Grace Period")
            OutlinedTextField(
                value = proPrice,
                onValueChange = { proPrice = it },
                label = { Text("Pro License Price") },
                placeholder = { Text("e.g.  \$49  /  LKR 14,900  /  £39") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = gracePeriodHours,
                onValueChange = { gracePeriodHours = it },
                label = { Text("Grace Period After Payment (hours)") },
                placeholder = { Text("e.g. 6") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Grace period extends the user's trial after payment confirmation, " +
                "giving them uninterrupted access while you deliver the key.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // ── 4. Trial Duration ─────────────────────────────────────
            SectionHeader("4. Trial Duration")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How many hours should the free trial last?",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        trialPresets.forEach { hours ->
                            val label = when (hours) { 24 -> "24h"; 48 -> "48h"; 72 -> "72h"; 168 -> "7d"; else -> "${hours}h" }
                            FilterChip(
                                selected = !useCustomHours && selectedPreset == hours,
                                onClick = { selectedPreset = hours; useCustomHours = false },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(checked = useCustomHours, onCheckedChange = { useCustomHours = it })
                        Text("Custom:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = customHoursText,
                            onValueChange = { customHoursText = it; useCustomHours = true },
                            placeholder = { Text("hours") },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            enabled = useCustomHours
                        )
                    }
                    val h = effectiveDemoHours
                    Text("→ Trial: $h hour${if (h == 1) "" else "s"} (baked into SHVAccount.kt)",
                        fontSize = 11.sp, color = Color(0xFF00E701))
                }
            }

            // ── 5. Bank Transfer Details ──────────────────────────────
            SectionHeader("5. Bank Transfer Details")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Bank Transfer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        // Quick Presets Dropdown Selector for Bank Data
                        Box {
                            TextButton(onClick = { expandedBankPresets = true }) {
                                Text("Select Preset", color = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expandedBankPresets, onDismissRequest = { expandedBankPresets = false }) {
                                if (presetsList.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No presets configured") }, onClick = { expandedBankPresets = false })
                                } else {
                                    presetsList.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(if (preset.isDefault) "${preset.name} (Default)" else preset.name) },
                                            onClick = {
                                                bankName = preset.bankName
                                                bankAccName = preset.bankAccName
                                                bankAccount = preset.bankAccount
                                                bankBranch = preset.bankBranch
                                                bankSwift = preset.bankSwift
                                                showBankSection = true
                                                expandedBankPresets = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { showBankSection = !showBankSection }) {
                            Text(if (showBankSection) "Collapse" else "Expand")
                        }
                    }
                    if (showBankSection) {
                        OutlinedTextField(value = bankName, onValueChange = { bankName = it },
                            label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = bankAccName, onValueChange = { bankAccName = it },
                            label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = bankAccount, onValueChange = { bankAccount = it },
                            label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = bankBranch, onValueChange = { bankBranch = it },
                            label = { Text("Branch") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = bankSwift, onValueChange = { bankSwift = it },
                            label = { Text("SWIFT / BIC Code (international)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                    } else {
                        val filled = listOf(bankName, bankAccount, bankAccName, bankBranch, bankSwift).count { it.isNotBlank() }
                        Text("$filled / 5 fields filled", fontSize = 12.sp,
                            color = if (filled == 5) Color(0xFF00E701) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── 6. Crypto Addresses ───────────────────────────────────
            SectionHeader("6. Crypto Wallet Addresses")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Crypto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        // Quick Presets Dropdown Selector for Crypto Data
                        Box {
                            TextButton(onClick = { expandedCryptoPresets = true }) {
                                Text("Select Preset", color = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expandedCryptoPresets, onDismissRequest = { expandedCryptoPresets = false }) {
                                if (presetsList.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No presets configured") }, onClick = { expandedCryptoPresets = false })
                                } else {
                                    presetsList.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(if (preset.isDefault) "${preset.name} (Default)" else preset.name) },
                                            onClick = {
                                                cryptoUsdtBsc = preset.cryptoUsdtBsc
                                                cryptoUsdtTrc = preset.cryptoUsdtTrc
                                                cryptoUsdtPlasma = preset.cryptoUsdtPlasma
                                                cryptoEth = preset.cryptoEth
                                                cryptoLtc = preset.cryptoLtc
                                                showCryptoSection = true
                                                expandedCryptoPresets = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { showCryptoSection = !showCryptoSection }) {
                            Text(if (showCryptoSection) "Collapse" else "Expand")
                        }
                    }
                    if (showCryptoSection) {
                        OutlinedTextField(value = cryptoUsdtBsc, onValueChange = { cryptoUsdtBsc = it },
                            label = { Text("USDT — BSC (BEP20)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = cryptoUsdtTrc, onValueChange = { cryptoUsdtTrc = it },
                            label = { Text("USDT — TRC20") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = cryptoUsdtPlasma, onValueChange = { cryptoUsdtPlasma = it },
                            label = { Text("USDT — Plasma") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = cryptoEth, onValueChange = { cryptoEth = it },
                            label = { Text("ETH Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = cryptoLtc, onValueChange = { cryptoLtc = it },
                            label = { Text("LTC Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    } else {
                        val filled = listOf(cryptoUsdtBsc, cryptoUsdtTrc, cryptoUsdtPlasma, cryptoEth, cryptoLtc).count { it.isNotBlank() }
                        Text("$filled / 5 addresses filled", fontSize = 12.sp,
                            color = if (filled > 0) Color(0xFF00E701) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── 7. Disclaimer ─────────────────────────────────────────
            SectionHeader("7. Payment Disclaimer")
            OutlinedTextField(
                value = disclaimer,
                onValueChange = { disclaimer = it },
                label = { Text("Disclaimer shown on gate screen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 4
            )

            // ── 8. Source Files ───────────────────────────────────────
            SectionHeader("8. Target Source Files")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilePickerRow("AndroidManifest.xml", manifestUri != null) { manifestLauncher.launch(arrayOf("*/*")) }
                    FilePickerRow("libs.versions.toml",  libsTomlUri != null) { libsTomlLauncher.launch(arrayOf("*/*")) }
                    FilePickerRow("build.gradle.kts",    buildGradleUri != null) { buildGradleLauncher.launch(arrayOf("*/*")) }
                    Text(
                        "ℹ️ The toml and gradle files are merged — only missing " +
                        "Compose/coroutines entries are injected. Existing versions preserved. ⚠️ When patching build Gradle it's the app version file and once patching check that file for errors and the license gate screen file for duplicate imports.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 9. Inject Button ──────────────────────────────────────
            Button(
                onClick = {
                    if (selectedProduct == null || manifestUri == null || libsTomlUri == null || buildGradleUri == null) {
                        Toast.makeText(context, "Please select a product and all three source files.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    try {
                        val config = GateConfig(
                            appDisplayName   = appDisplayName.trim().ifBlank { selectedProduct!!.name },
                            proPrice         = proPrice.trim().ifBlank { "Contact us for pricing" },
                            gracePeriodHours = gracePeriodHours.trim().toIntOrNull() ?: 6,
                            bankName         = bankName.trim(),
                            bankAccName      = bankAccName.trim(),
                            bankAccount      = bankAccount.trim(),
                            bankBranch       = bankBranch.trim(),
                            bankSwift        = bankSwift.trim(),
                            cryptoUsdtBsc    = cryptoUsdtBsc.trim(),
                            cryptoUsdtTrc    = cryptoUsdtTrc.trim(),
                            cryptoUsdtPlasma = cryptoUsdtPlasma.trim(),
                            cryptoEth        = cryptoEth.trim(),
                            cryptoLtc        = cryptoLtc.trim(),
                            disclaimer       = disclaimer.trim()
                        )
                        processInjection(
                            context     = context,
                            manifestUri = manifestUri!!,
                            libsTomlUri = libsTomlUri!!,
                            buildGradle = buildGradleUri!!,
                            product     = selectedProduct!!,
                            packageName = targetPackageName,
                            demoHours   = effectiveDemoHours,
                            config      = config
                        )
                        Toast.makeText(context, "Injection Complete! Check Downloads folder.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E701)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPILE & EXPORT PATCHED FILES",
                    color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Payment Presets Modal Management Dialog Window ────────────────────────
    if (showPresetsDialog) {
        var currentPresetId by remember { mutableStateOf<String?>(null) }
        var currentPresetName by remember { mutableStateOf("") }
        var isDefaultPreset by remember { mutableStateOf(false) }

        var pBankName by remember { mutableStateOf("") }
        var pBankAccName by remember { mutableStateOf("") }
        var pBankAccount by remember { mutableStateOf("") }
        var pBankBranch by remember { mutableStateOf("") }
        var pBankSwift by remember { mutableStateOf("") }

        var pCryptoUsdtBsc by remember { mutableStateOf("") }
        var pCryptoUsdtTrc by remember { mutableStateOf("") }
        var pCryptoUsdtPlasma by remember { mutableStateOf("") }
        var pCryptoEth by remember { mutableStateOf("") }
        var pCryptoLtc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
            title = { Text("Payment Presets Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // List existing profiles
                    if (presetsList.isNotEmpty()) {
                        Text("Existing Configuration Maps:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        presetsList.forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (preset.isDefault) "${preset.name} [DEFAULT]" else preset.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (preset.isDefault) Color(0xFF00E701) else Color.White
                                    )
                                    Text(
                                        "Bank: ${preset.bankName.ifBlank { "None" }} | Crypto Targets Loaded",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Row {
                                    TextButton(onClick = {
                                        currentPresetId = preset.id
                                        currentPresetName = preset.name
                                        isDefaultPreset = preset.isDefault
                                        pBankName = preset.bankName
                                        pBankAccName = preset.bankAccName
                                        pBankAccount = preset.bankAccount
                                        pBankBranch = preset.bankBranch
                                        pBankSwift = preset.bankSwift
                                        pCryptoUsdtBsc = preset.cryptoUsdtBsc
                                        pCryptoUsdtTrc = preset.cryptoUsdtTrc
                                        pCryptoUsdtPlasma = preset.cryptoUsdtPlasma
                                        pCryptoEth = preset.cryptoEth
                                        pCryptoLtc = preset.cryptoLtc
                                    }) {
                                        Text("Edit", fontSize = 11.sp)
                                    }
                                    IconButton(onClick = {
                                        val updated = presetsList.filter { it.id != preset.id }
                                        presetsList = updated
                                        AppStorage.savePaymentPresets(updated)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF262626))
                    }

                    Text("Create / Modify Target Preset:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = currentPresetName,
                        onValueChange = { currentPresetName = it },
                        label = { Text("Preset Blueprint Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDefaultPreset, onCheckedChange = { isDefaultPreset = it })
                        Text("Set as Default Preset", fontSize = 12.sp)
                    }

                    Text("Bank Parameters Mapping", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = pBankName, onValueChange = { pBankName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pBankAccName, onValueChange = { pBankAccName = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pBankAccount, onValueChange = { pBankAccount = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pBankBranch, onValueChange = { pBankBranch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pBankSwift, onValueChange = { pBankSwift = it }, label = { Text("SWIFT / BIC") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Text("Crypto Address Core Matrix", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = pCryptoUsdtBsc, onValueChange = { pCryptoUsdtBsc = it }, label = { Text("USDT - BSC (BEP20)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pCryptoUsdtTrc, onValueChange = { pCryptoUsdtTrc = it }, label = { Text("USDT - TRC20") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pCryptoUsdtPlasma, onValueChange = { pCryptoUsdtPlasma = it }, label = { Text("USDT - Plasma") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pCryptoEth, onValueChange = { pCryptoEth = it }, label = { Text("ETH Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pCryptoLtc, onValueChange = { pCryptoLtc = it }, label = { Text("LTC Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPresetName.isBlank()) {
                            Toast.makeText(context, "Please configure a distinct Preset Name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Adjust existing elements if updated to prevent conflicting default assignments
                        var updatedList = presetsList.map {
                            if (isDefaultPreset) it.copy(isDefault = false) else it
                        }.toMutableList()

                        val existingIndex = if (currentPresetId != null)
                            updatedList.indexOfFirst { it.id == currentPresetId }
                        else -1
                        val newPreset = PaymentPreset(
                            id = currentPresetId ?: UUID.randomUUID().toString(),
                            name = currentPresetName.trim(),
                            isDefault = isDefaultPreset,
                            bankName = pBankName.trim(),
                            bankAccName = pBankAccName.trim(),
                            bankAccount = pBankAccount.trim(),
                            bankBranch = pBankBranch.trim(),
                            bankSwift = pBankSwift.trim(),
                            cryptoUsdtBsc = pCryptoUsdtBsc.trim(),
                            cryptoUsdtTrc = pCryptoUsdtTrc.trim(),
                            cryptoUsdtPlasma = pCryptoUsdtPlasma.trim(),
                            cryptoEth = pCryptoEth.trim(),
                            cryptoLtc = pCryptoLtc.trim()
                        )

                        if (existingIndex != -1) {
                            updatedList[existingIndex] = newPreset
                        } else {
                            updatedList.add(newPreset)
                        }

                        presetsList = updatedList
                        AppStorage.savePaymentPresets(updatedList)

                        // If assigned as system default layout blueprint, auto-populate the wizard input parameters right away
                        if (isDefaultPreset) {
                            bankName = newPreset.bankName
                            bankAccName = newPreset.bankAccName
                            bankAccount = newPreset.bankAccount
                            bankBranch = newPreset.bankBranch
                            bankSwift = newPreset.bankSwift
                            cryptoUsdtBsc = newPreset.cryptoUsdtBsc
                            cryptoUsdtTrc = newPreset.cryptoUsdtTrc
                            cryptoUsdtPlasma = newPreset.cryptoUsdtPlasma
                            cryptoEth = newPreset.cryptoEth
                            cryptoLtc = newPreset.cryptoLtc
                        }

                        currentPresetId = null
                        currentPresetName = ""
                        pBankName = ""; pBankAccName = ""; pBankAccount = ""; pBankBranch = ""; pBankSwift = ""
                        pCryptoUsdtBsc = ""; pCryptoUsdtTrc = ""; pCryptoUsdtPlasma = ""; pCryptoEth = ""; pCryptoLtc = ""
                        isDefaultPreset = false
                    }
                ) {
                    Text("Save Preset Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetsDialog = false }) { Text("Close Options") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun FilePickerRow(label: String, selected: Boolean, onBrowse: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (selected) "$label  ✓" else "$label — not selected",
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            color = if (selected) Color(0xFF00E701) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onBrowse) { Text("Browse") }
    }
}

private fun processInjection(
    context: Context,
    manifestUri: Uri,
    libsTomlUri: Uri,
    buildGradle: Uri,
    product: KotlinProduct,
    packageName: String,
    demoHours: Int,
    config: GateConfig
) {
    val cr = context.contentResolver
    fun readUri(uri: Uri) = cr.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""

    val patchedManifest = KotlinInjectorEngine.patchManifest(readUri(manifestUri))
    val mergedToml      = KotlinInjectorEngine.mergeLibsVersionsToml(readUri(libsTomlUri))
    val mergedGradle    = KotlinInjectorEngine.mergeBuildGradleKts(readUri(buildGradle))
    val shvApplication  = KotlinInjectorEngine.generateSHVApplication(packageName, product)
    val gateScreen      = KotlinInjectorEngine.generateLicenseGateScreen(packageName, product, config)
    val shvLicense      = KotlinInjectorEngine.generateSHVLicense(packageName, product)
    val shvAccount      = KotlinInjectorEngine.generateSHVAccount(packageName, product, demoHours)

    val outputFolder  = File(KotlinInjectorEngine.outputDir, product.appCode).also { it.mkdirs() }
    val shvGateFolder = File(outputFolder, "shvgate").also { it.mkdirs() }
    val licenseFolder = File(outputFolder, "license").also { it.mkdirs() }
    val gradleFolder  = File(outputFolder, "gradle").also { it.mkdirs() }

    File(outputFolder,  "AndroidManifest_Patched.xml").writeText(patchedManifest)
    File(outputFolder,  "SHVApplication.kt").writeText(shvApplication)
    File(shvGateFolder, "LicenseGateScreen.kt").writeText(gateScreen)
    File(licenseFolder, "SHVLicense.kt").writeText(shvLicense)
    File(licenseFolder, "SHVAccount.kt").writeText(shvAccount)
    File(gradleFolder,  "libs.versions.toml").writeText(mergedToml)
    File(gradleFolder,  "build.gradle.kts").writeText(mergedGradle)
}
