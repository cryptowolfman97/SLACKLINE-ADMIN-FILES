package com.example.slacklineadminapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.KotlinLicense
import com.example.slacklineadminapp.data.KotlinLicenseEngine
import com.example.slacklineadminapp.data.KotlinLicenseRepository
import com.example.slacklineadminapp.data.KotlinProduct
import com.example.slacklineadminapp.data.ProductStatus
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotlinProductManagerScreen(
    productId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var product by remember { mutableStateOf<KotlinProduct?>(null) }
    var licenses by remember { mutableStateOf(emptyList<KotlinLicense>()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Dashboard", "Authority", "Generate License", "License List", "Revocations", "Backup")

    fun reloadData() {
        val allProds = KotlinLicenseRepository.getAllProducts()
        product = allProds.find { it.id == productId }
        licenses = KotlinLicenseRepository.getLicensesForProduct(productId)
    }

    LaunchedEffect(productId) {
        reloadData()
    }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentProduct = product!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProduct.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> DashboardTabContent(currentProduct, licenses)
                    1 -> AuthorityTabContent(currentProduct) { reloadData() }
                    2 -> GenerateLicenseTabContent(currentProduct) { reloadData() }
                    3 -> LicenseListTabContent(licenses, false)
                    4 -> LicenseListTabContent(licenses, true)
                    5 -> BackupTabContent(currentProduct, licenses, context)
                }
            }
        }
    }
}

// ── TAB 1: DASHBOARD CONTENT ───────────────────────────────────────────
@Composable
private fun DashboardTabContent(product: KotlinProduct, licenses: List<KotlinLicense>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Product Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Licenses", licenses.size.toString(), Modifier.weight(1f))
            val activeCount = licenses.count { !it.isRevoked }
            StatCard("Active Keys", activeCount.toString(), Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val proCount = licenses.count { it.licenseType == "PRO" && !it.isRevoked }
            val proPlusCount = licenses.count { it.licenseType == "PRO+" && !it.isRevoked }
            StatCard("Active PRO", proCount.toString(), Modifier.weight(1f))
            StatCard("Active PRO+", proPlusCount.toString(), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Authority Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = when (product.status) {
                        ProductStatus.ACTIVE -> Color(0xFF00E701)
                        ProductStatus.WARNING -> Color(0xFFFFB300)
                        ProductStatus.REVOKED -> Color(0xFFFF4E4E)
                    }
                    Box(modifier = Modifier.size(12.dp).background(dotColor, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(product.status.name, fontWeight = FontWeight.SemiBold, color = dotColor)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── TAB 2: AUTHORITY CONTENT ───────────────────────────────────────────
@Composable
private fun AuthorityTabContent(product: KotlinProduct, onStatusChanged: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var showPrivateKey by remember { mutableStateOf(false) }

    val fingerprint = remember(product.publicKeyPem) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(product.publicKeyPem.toByteArray())
            hash.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "ERROR_GENERATING_FINGERPRINT"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cryptographic Authority keys", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // Fingerprint
        KeyDisplayField(label = "SHA-256 Fingerprint", content = fingerprint, clipboard = clipboard)

        // Public Key
        KeyDisplayField(label = "Public Key PEM", content = product.publicKeyPem, clipboard = clipboard)

        // Private Key Drawer Concept
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Key PEM", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row {
                        IconButton(onClick = { showPrivateKey = !showPrivateKey }) {
                            Icon(if (showPrivateKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Reveal")
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(product.privateKeyPem)) }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                    }
                }
                AnimatedVisibility(visible = showPrivateKey) {
                    Text(
                        text = product.privateKeyPem,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(8.dp)
                    )
                }
                if (!showPrivateKey) {
                    Text("[ HIDDEN BY DEFAULT TO PREVENT ACCIDENTAL EXPOSURE ]", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Change Status Control
        Text("Emergency Management", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { product.status = ProductStatus.ACTIVE; KotlinLicenseRepository.saveProduct(product); onStatusChanged() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E701))
            ) { Text("Active", color = Color.Black) }
            Button(
                onClick = { product.status = ProductStatus.WARNING; KotlinLicenseRepository.saveProduct(product); onStatusChanged() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) { Text("Warning", color = Color.Black) }
            Button(
                onClick = { product.status = ProductStatus.REVOKED; KotlinLicenseRepository.saveProduct(product); onStatusChanged() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4E4E))
            ) { Text("Revoke Engine", color = Color.White) }
        }
    }
}

@Composable
private fun KeyDisplayField(label: String, content: String, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    var isCopied by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(content))
                    isCopied = true
                }) {
                    Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = "Copy", tint = if (isCopied) Color(0xFF00E701) else MaterialTheme.colorScheme.onSurface)
                }
            }
            LaunchedEffect(isCopied) {
                if (isCopied) {
                    kotlinx.coroutines.delay(2000)
                    isCopied = false
                }
            }
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(8.dp)
            )
        }
    }
}

// ── TAB 3: GENERATE LICENSE CONTENT ────────────────────────────────────
@Composable
private fun GenerateLicenseTabContent(product: KotlinProduct, onLicenseGenerated: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var deviceCode by remember { mutableStateOf("") }
    var custName by remember { mutableStateOf("") }
    var custEmail by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PRO") }
    var generatedKeyBlock by remember { mutableStateOf("") }
    // FIX: hold the license UUID so it matches both the token payload and the Supabase record
    var pendingLicenseId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Generate Highly Secure Activation Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = deviceCode,
                onValueChange = { deviceCode = it },
                label = { Text("Device Unique Code") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { deviceCode = clipboard.getText()?.text ?: "" },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
            }
        }

        OutlinedTextField(value = custName, onValueChange = { custName = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = custEmail, onValueChange = { custEmail = it }, label = { Text("Customer Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        // Change line 348 to:
        OutlinedTextField(
            value = payMethod, 
            onValueChange = { payMethod = it }, 
            label = { Text("Payment Source / Method") }, 
            singleLine = true, 
            modifier = Modifier.fillMaxWidth()
        )


        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("License Assignment Tier: ", fontSize = 14.sp)
            RadioButton(selected = selectedType == "PRO", onClick = { selectedType = "PRO" })
            Text("PRO")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = selectedType == "PRO+", onClick = { selectedType = "PRO+" })
            Text("PRO+")
        }

        Button(
            onClick = {
                if (deviceCode.isNotBlank()) {
                    pendingLicenseId = UUID.randomUUID().toString()
                    generatedKeyBlock = KotlinLicenseEngine.generateLicenseKey(
                        privateKeyPem = product.privateKeyPem,
                        prefix = product.prefix,
                        appCode = product.appCode,
                        deviceCode = deviceCode,
                        tier = selectedType,
                        licenseId = pendingLicenseId
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cryptographically Sign License Block")
        }

        if (generatedKeyBlock.isNotEmpty()) {
            var copiedToken by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Output Compressed Key Bundle", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { clipboard.setText(AnnotatedString(generatedKeyBlock)); copiedToken = true }) {
                            Icon(if (copiedToken) Icons.Default.Check else Icons.Default.ContentCopy, "Copy", tint = if (copiedToken) Color(0xFF00E701) else MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(text = generatedKeyBlock, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Button(
                onClick = {
                    // FIX: pass pendingLicenseId as id so it matches the license_id baked into the token
                    val cleanLicense = KotlinLicense(
                        id = pendingLicenseId,
                        productId = product.id,
                        licenseKey = generatedKeyBlock,
                        deviceCode = deviceCode.trim().uppercase(),
                        customerName = custName.trim(),
                        customerEmail = custEmail.trim(),
                        paymentMethod = payMethod.trim(),
                        licenseType = selectedType
                    )
                    KotlinLicenseRepository.saveLicense(cleanLicense)
                    generatedKeyBlock = ""
                    pendingLicenseId = ""
                    deviceCode = ""; custName = ""; custEmail = ""; payMethod = ""
                    onLicenseGenerated()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E701))
            ) {
                Text("Commit & Save Record to Local Registry", color = Color.Black)
            }
        }
    }
}

// ── TAB 4 & 5: LICENSE DIRECTORY LISTS ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseListTabContent(licenses: List<KotlinLicense>, filterRevoked: Boolean) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(licenses, searchQuery, filterRevoked) {
        licenses.filter { it.isRevoked == filterRevoked }.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
            it.customerEmail.contains(searchQuery, ignoreCase = true) ||
            it.deviceCode.contains(searchQuery, ignoreCase = true)
        }
    }
    var activeSheetLicense by remember { mutableStateOf<KotlinLicense?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, email, or hardware hash...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching operational license registry records found.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { license ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSheetLicense = license },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(license.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(license.customerEmail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(license.licenseType, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (activeSheetLicense != null) {
            ModalBottomSheet(onDismissRequest = { activeSheetLicense = null }) {
                LicenseInspectionView(license = activeSheetLicense!!) {
                    KotlinLicenseRepository.saveLicense(activeSheetLicense!!)
                    activeSheetLicense = null
                }
            }
        }
    }
}

@Composable
fun LicenseInspectionView(license: KotlinLicense, onSavedState: () -> Unit) {
    val clp = LocalClipboardManager.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Client License Inspector", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        InspectionRow(label = "Customer Name", valStr = license.customerName, clp = clp)
        InspectionRow(label = "Email Address", valStr = license.customerEmail, clp = clp)
        InspectionRow(label = "Device ID Binding", valStr = license.deviceCode, clp = clp)
        InspectionRow(label = "Payment Source Reference", valStr = license.paymentMethod, clp = clp)
        InspectionRow(label = "Tier Setting", valStr = license.licenseType, clp = clp)
        InspectionRow(label = "Issued Date", valStr = sdf.format(Date(license.issuedAt)), clp = clp)
        InspectionRow(label = "Full Token String", valStr = license.licenseKey, clp = clp)

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                license.isRevoked = !license.isRevoked
                onSavedState()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (license.isRevoked) Color(0xFF00E701) else Color(0xFFFF4E4E))
        ) {
            Text(if (license.isRevoked) "Restore / Lift Blacklist" else "Revoke & Void License", color = if (license.isRevoked) Color.Black else Color.White)
        }
    }
}

@Composable
fun InspectionRow(label: String, valStr: String, clp: androidx.compose.ui.platform.ClipboardManager) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(valStr, fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { clp.setText(AnnotatedString(valStr)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── TAB 6: BACKUP LAB CONTENT ──────────────────────────────────────────
@Composable
private fun BackupTabContent(product: KotlinProduct, licenses: List<KotlinLicense>, context: Context) {
    val gson = remember { com.google.gson.Gson() }
    val clp = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Smart Backup Control panel", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        BackupActionRow(title = "Extract Authority Crypt", desc = "Exports only the raw RSA cryptographic keypairs to your local backup clipboard.") {
            val wrapMap = mapOf("publicKey" to product.publicKeyPem, "privateKey" to product.privateKeyPem)
            clp.setText(AnnotatedString(gson.toJson(wrapMap)))
            Toast.makeText(context, "Asymmetric authority keys wrapped to clipboard!", Toast.LENGTH_SHORT).show()
        }

        BackupActionRow(title = "Package License Registry", desc = "Transforms all issued client license logs into a unified flat structural JSON dump string.") {
            clp.setText(AnnotatedString(gson.toJson(licenses)))
            Toast.makeText(context, "License lists encrypted string sent to clipboard!", Toast.LENGTH_SHORT).show()
        }

        BackupActionRow(title = "Consolidate Complete Archive Bundle", desc = "Merges core authority metadata along with active client validation tables into a local text asset.") {
            val heavyPack = mapOf("meta" to product, "records" to licenses)
            clp.setText(AnnotatedString(gson.toJson(heavyPack)))
            Toast.makeText(context, "Full recovery stack archived to local clipboard!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun BackupActionRow(title: String, desc: String, onExecute: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onExecute, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Refresh, contentDescription = "Process")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Run Archiver")
            }
        }
    }
}
