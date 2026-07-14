package com.example.slacklineadminapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// AMOLED Colour Palette
// ─────────────────────────────────────────────────────────────────────────────
private val Bg          = Color(0xFF000000)
private val Surface1    = Color(0xFF0D0D0D)
private val Surface2    = Color(0xFF161616)
private val Surface3    = Color(0xFF1F1F1F)
private val Border      = Color(0xFF2A2A2A)
private val Cyan        = Color(0xFF22D3EE)
private val CyanDim     = Color(0xFF0E7490)
private val Purple      = Color(0xFF7C3AED)
private val PurpleDim   = Color(0xFF4C1D95)
private val Green       = Color(0xFF10B981)
private val Orange      = Color(0xFFF59E0B)
private val Red         = Color(0xFFEF4444)
private val TextPri     = Color(0xFFFFFFFF)
private val TextSec     = Color(0xFFAAAAAA)
private val TextMuted   = Color(0xFF555555)

// ─────────────────────────────────────────────────────────────────────────────
// Reusable design tokens
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ACard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ATextField(value: String, onValueChange: (String) -> Unit, label: String,
                       modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text,
                       singleLine: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 11.sp) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan, unfocusedBorderColor = Border,
            focusedLabelColor = Cyan, unfocusedLabelColor = TextMuted,
            focusedTextColor = TextPri, unfocusedTextColor = TextPri,
            cursorColor = Cyan
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun StatusChip(status: DocStatus) {
    val (color, label) = when (status) {
        DocStatus.DRAFT     -> Orange to "DRAFT"
        DocStatus.SENT      -> Cyan   to "SENT"
        DocStatus.PAID      -> Green  to "PAID"
        DocStatus.CANCELLED -> Red    to "CANCELLED"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun DocTypeChip(type: DocumentType) {
    val color = when (type) {
        DocumentType.INVOICE     -> Cyan
        DocumentType.QUOTATION   -> Orange
        DocumentType.CREDIT_NOTE -> Red
        DocumentType.PROFORMA    -> Purple
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(type.name, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedInvoiceScreen(onBackClicked: () -> Unit) {
    val vm: AdvancedBillingViewModel = viewModel()
    val screenState by vm.activeScreen.collectAsState()
    val toastState  by vm.toastMessage.collectAsState()
    val processing  by vm.isProcessing.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(toastState) {
        if (toastState.isNotEmpty()) { snackbarHost.showSnackbar(toastState); vm.toastMessage.value = "" }
    }

    BackHandler {
        when (screenState) {
            is AdvancedBillingViewModel.ActiveScreen.Dashboard -> onBackClicked()
            else -> vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Bg
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize().background(Bg)) {
            AnimatedContent(
                targetState = screenState,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "nav"
            ) { target ->
                when (target) {
                    is AdvancedBillingViewModel.ActiveScreen.Dashboard      -> DashboardView(vm)
                    is AdvancedBillingViewModel.ActiveScreen.ClientManager  -> ClientManagerView(vm)
                    is AdvancedBillingViewModel.ActiveScreen.CatalogManager -> CatalogManagerView(vm)
                    is AdvancedBillingViewModel.ActiveScreen.CompanyProfile -> CompanyProfileView(vm)
                    is AdvancedBillingViewModel.ActiveScreen.DocumentEditor -> DocumentEditorView(vm)
                    is AdvancedBillingViewModel.ActiveScreen.DocumentPreview -> DocumentPreviewView(vm, target.docId)
                }
            }
            if (processing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan, strokeWidth = 2.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 1: Dashboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardView(vm: AdvancedBillingViewModel) {
    val docs     by vm.filteredDocuments.collectAsState()
    val allDocs  by vm.documents.collectAsState()
    val profile  by vm.companyProfile.collectAsState()
    var filterStatusExpanded by remember { mutableStateOf(false) }
    var filterTypeExpanded   by remember { mutableStateOf(false) }
    val filterStatus by vm.filterStatus.collectAsState()
    val filterType   by vm.filterType.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Top bar
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Surface2, Bg)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text("INVOICE MANAGER", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(profile.companyName, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = { vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.CompanyProfile },
                modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Settings, null, tint = TextSec)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Stats row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val paid    = allDocs.count { it.status == DocStatus.PAID }
                    val pending = allDocs.count { it.status == DocStatus.SENT || it.status == DocStatus.DRAFT }
                    StatTile("Total", "${allDocs.size}", Cyan,   Modifier.weight(1f))
                    StatTile("Paid",  "$paid",           Green,  Modifier.weight(1f))
                    StatTile("Open",  "$pending",        Orange, Modifier.weight(1f))
                }
            }

            // New document buttons
            item {
                SectionLabel("NEW DOCUMENT")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewDocButton("Invoice",   Cyan,   Modifier.weight(1f)) { vm.startNewDocument(DocumentType.INVOICE) }
                    NewDocButton("Quote",     Orange, Modifier.weight(1f)) { vm.startNewDocument(DocumentType.QUOTATION) }
                    NewDocButton("Proforma",  Purple, Modifier.weight(1f)) { vm.startNewDocument(DocumentType.PROFORMA) }
                    NewDocButton("Credit",    Red,    Modifier.weight(1f)) { vm.startNewDocument(DocumentType.CREDIT_NOTE) }
                }
            }

            // Quick nav
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickNavButton("Clients", Icons.Default.People, Modifier.weight(1f)) {
                        vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.ClientManager
                    }
                    QuickNavButton("Catalog", Icons.Default.Inventory, Modifier.weight(1f)) {
                        vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.CatalogManager
                    }
                    QuickNavButton("Profile", Icons.Default.Business, Modifier.weight(1f)) {
                        vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.CompanyProfile
                    }
                }
            }

            // Filters
            item {
                SectionLabel("DOCUMENTS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Status filter
                    Box {
                        FilterChip(
                            selected = filterStatus != null,
                            onClick = { filterStatusExpanded = true },
                            label = { Text(filterStatus?.name ?: "Status", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanDim,
                                selectedLabelColor = TextPri,
                                containerColor = Surface2,
                                labelColor = TextSec
                            )
                        )
                        DropdownMenu(expanded = filterStatusExpanded, onDismissRequest = { filterStatusExpanded = false },
                            modifier = Modifier.background(Surface3)) {
                            DropdownMenuItem(text = { Text("All", color = TextPri) }, onClick = { vm.filterStatus.value = null; filterStatusExpanded = false })
                            DocStatus.entries.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name, color = TextPri) }, onClick = { vm.filterStatus.value = s; filterStatusExpanded = false })
                            }
                        }
                    }
                    // Type filter
                    Box {
                        FilterChip(
                            selected = filterType != null,
                            onClick = { filterTypeExpanded = true },
                            label = { Text(filterType?.name ?: "Type", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleDim,
                                selectedLabelColor = TextPri,
                                containerColor = Surface2,
                                labelColor = TextSec
                            )
                        )
                        DropdownMenu(expanded = filterTypeExpanded, onDismissRequest = { filterTypeExpanded = false },
                            modifier = Modifier.background(Surface3)) {
                            DropdownMenuItem(text = { Text("All", color = TextPri) }, onClick = { vm.filterType.value = null; filterTypeExpanded = false })
                            DocumentType.entries.forEach { t ->
                                DropdownMenuItem(text = { Text(t.name, color = TextPri) }, onClick = { vm.filterType.value = t; filterTypeExpanded = false })
                            }
                        }
                    }
                    if (filterStatus != null || filterType != null) {
                        TextButton(onClick = { vm.filterStatus.value = null; vm.filterType.value = null }) {
                            Text("Clear", color = Red, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Document list
            if (docs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No documents yet", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(docs) { doc ->
                    DocumentListCard(doc) {
                        vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.DocumentPreview(doc.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NewDocButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        border = null,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("+ $label", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun QuickNavButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface2),
        border = ButtonDefaults.outlinedButtonBorder.copy(),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Icon(icon, null, tint = TextSec, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSec, fontSize = 11.sp)
    }
}

@Composable
private fun DocumentListCard(doc: DocumentEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DocTypeChip(doc.docType)
                Text(doc.docNumber, color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(doc.clientNameCopy.ifBlank { "No client" }, color = TextSec, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Due: ${doc.dueDate}", color = TextMuted, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(doc.status)
            Text(String.format("%.2f", doc.grandTotal), color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BrandingImagePicker(
    label: String,
    path: String,
    onPicked: (android.net.Uri) -> Unit,
    onRemoved: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPicked) }

    val bitmap = remember(path) {
        if (path.isNotBlank() && File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
    }

    Column {
        Text(label, color = TextSec, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface3)
                    .border(1.dp, Border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = TextMuted)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        Text(if (bitmap != null) "Change" else "Choose Image", color = Cyan, fontSize = 12.sp)
                    }
                    if (bitmap != null) {
                        TextButton(onClick = onRemoved) {
                            Text("Remove", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    if (bitmap != null) "Saved to INVOICE MAKER/Data/Branding" else "No image selected",
                    color = TextMuted, fontSize = 9.sp
                )
            }
        }
    }
}
@Composable
private fun CompanyProfileView(vm: AdvancedBillingViewModel) {
    val profile by vm.companyProfile.collectAsState()

    var name       by remember(profile) { mutableStateOf(profile.companyName) }
    var tagline    by remember(profile) { mutableStateOf(profile.tagline) }
    var address    by remember(profile) { mutableStateOf(profile.address) }
    var email      by remember(profile) { mutableStateOf(profile.email) }
    var phone      by remember(profile) { mutableStateOf(profile.phone) }
    var website    by remember(profile) { mutableStateOf(profile.website) }
    var taxNum     by remember(profile) { mutableStateOf(profile.taxNumber) }
    var logoPath   by remember(profile) { mutableStateOf(profile.logoPath) }
    var sigPath    by remember(profile) { mutableStateOf(profile.signaturePath) }
    var currency   by remember(profile) { mutableStateOf(profile.currencySymbol) }
    var bankName   by remember(profile) { mutableStateOf(profile.bankName) }
    var bankAccName by remember(profile) { mutableStateOf(profile.bankAccountName) }
    var bankAccNum  by remember(profile) { mutableStateOf(profile.bankAccountNumber) }
    var bankBranch  by remember(profile) { mutableStateOf(profile.bankBranch) }
    var payNotes    by remember(profile) { mutableStateOf(profile.paymentNotes) }
    var defTemplate by remember(profile) { mutableStateOf(profile.defaultTemplate) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenTopBar("Company Profile", Icons.Default.Business) {
            vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ACard {
                SectionLabel("COMPANY INFORMATION")
                ATextField(name, { name = it }, "Company Name")
                Spacer(Modifier.height(10.dp))
                ATextField(tagline, { tagline = it }, "Tagline / Slogan")
                Spacer(Modifier.height(10.dp))
                ATextField(address, { address = it }, "Address", singleLine = false)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(email,   { email = it },   "Email",   Modifier.weight(1f))
                    ATextField(phone,   { phone = it },   "Phone",   Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(website, { website = it }, "Website", Modifier.weight(1f))
                    ATextField(taxNum,  { taxNum = it },  "Tax/VAT No.", Modifier.weight(1f))
                }
            }

            ACard {
                SectionLabel("BRANDING")
                BrandingImagePicker(
                    label = "Company Logo",
                    path = logoPath,
                    onPicked = { uri -> vm.importBrandingImage(uri, AdvancedBillingViewModel.BrandingTarget.LOGO) },
                    onRemoved = { vm.removeBrandingImage(AdvancedBillingViewModel.BrandingTarget.LOGO) }
                )
                Spacer(Modifier.height(14.dp))
                BrandingImagePicker(
                    label = "Signature",
                    path = sigPath,
                    onPicked = { uri -> vm.importBrandingImage(uri, AdvancedBillingViewModel.BrandingTarget.SIGNATURE) },
                    onRemoved = { vm.removeBrandingImage(AdvancedBillingViewModel.BrandingTarget.SIGNATURE) }
                )
            }

            ACard {
                SectionLabel("CURRENCY & DEFAULTS")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(currency, { currency = it }, "Currency Symbol", Modifier.width(100.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Template", color = TextSec, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InvoiceTemplate.entries.forEach { t ->
                                val selected = defTemplate == t
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Cyan.copy(alpha = 0.2f) else Surface3)
                                        .border(1.dp, if (selected) Cyan else Border, RoundedCornerShape(8.dp))
                                        .clickable { defTemplate = t }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(t.name, color = if (selected) Cyan else TextSec, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            ACard {
                SectionLabel("PAYMENT DETAILS")
                ATextField(bankName,    { bankName = it },    "Bank Name")
                Spacer(Modifier.height(10.dp))
                ATextField(bankAccName, { bankAccName = it }, "Account Holder Name")
                Spacer(Modifier.height(10.dp))
                ATextField(bankAccNum,  { bankAccNum = it },  "Account Number", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(10.dp))
                ATextField(bankBranch,  { bankBranch = it },  "Branch")
                Spacer(Modifier.height(10.dp))
                ATextField(payNotes,    { payNotes = it },    "Payment Terms (e.g. Due within 30 days)", singleLine = false)
            }
        }

        // Save button
        Box(modifier = Modifier.fillMaxWidth().background(Surface1).padding(16.dp)) {
            Button(
                onClick = {
                    vm.saveCompanyProfile(CompanyProfile(
                        companyName = name, tagline = tagline, address = address,
                        email = email, phone = phone, website = website, taxNumber = taxNum,
                        logoPath = logoPath, signaturePath = sigPath, currencySymbol = currency,
                        bankName = bankName, bankAccountName = bankAccName, bankAccountNumber = bankAccNum,
                        bankBranch = bankBranch, paymentNotes = payNotes, defaultTemplate = defTemplate
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null, tint = Bg)
                Spacer(Modifier.width(8.dp))
                Text("Save Profile", color = Bg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 3: Document Editor
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DocumentEditorView(vm: AdvancedBillingViewModel) {
    val docState   by vm.editorDocument.collectAsState()
    val lineItems  by vm.editorLineItems.collectAsState()
    val clients    by vm.clients.collectAsState()
    val catalog    by vm.catalogItems.collectAsState()

    var docNumInput  by remember(docState.docNumber)  { mutableStateOf(docState.docNumber) }
    var issueInput   by remember(docState.issueDate)  { mutableStateOf(docState.issueDate) }
    var dueInput     by remember(docState.dueDate)    { mutableStateOf(docState.dueDate) }
    var taxInput     by remember(docState.taxPercent) { mutableStateOf(docState.taxPercent.toString()) }
    var discInput    by remember(docState.discountPercent) { mutableStateOf(docState.discountPercent.toString()) }
    var notesInput   by remember(docState.notes)      { mutableStateOf(docState.notes) }
    var showClientPicker  by remember { mutableStateOf(false) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenTopBar("${docState.docType.name} — ${docState.docNumber}", Icons.Default.Edit) {
            vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Document metadata
            ACard {
                SectionLabel("DOCUMENT DETAILS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(docNumInput, { docNumInput = it; vm.updateDocumentModifiers(taxInput.toDoubleOrNull() ?: 0.0, discInput.toDoubleOrNull() ?: 0.0, notesInput, it, issueInput, dueInput) }, "Doc Number", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(issueInput, { issueInput = it; vm.updateDocumentModifiers(taxInput.toDoubleOrNull() ?: 0.0, discInput.toDoubleOrNull() ?: 0.0, notesInput, docNumInput, it, dueInput) }, "Issue Date", Modifier.weight(1f))
                    ATextField(dueInput,  { dueInput = it;   vm.updateDocumentModifiers(taxInput.toDoubleOrNull() ?: 0.0, discInput.toDoubleOrNull() ?: 0.0, notesInput, docNumInput, issueInput, it) }, "Due Date", Modifier.weight(1f))
                }
            }

            // Client
            ACard {
                SectionLabel("CLIENT")
                if (docState.clientNameCopy.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(docState.clientNameCopy, color = TextPri, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(docState.clientEmailCopy, color = TextMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { showClientPicker = true }) { Icon(Icons.Default.Edit, null, tint = Cyan, modifier = Modifier.size(18.dp)) }
                    }
                } else {
                    OutlinedButton(onClick = { showClientPicker = true }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface3),
                        shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.PersonAdd, null, tint = Cyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Select Client", color = Cyan)
                    }
                }
            }

            // Template selector
            ACard {
                SectionLabel("INVOICE TEMPLATE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InvoiceTemplate.entries.forEach { t ->
                        val selected = (docState.template ?: InvoiceTemplate.PROFESSIONAL) == t
                        val color = when (t) { InvoiceTemplate.MINIMAL -> Cyan; InvoiceTemplate.PROFESSIONAL -> Green; InvoiceTemplate.BOLD -> Purple }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) color.copy(alpha = 0.15f) else Surface3)
                                .border(1.5.dp, if (selected) color else Border, RoundedCornerShape(10.dp))
                                .clickable { vm.setDocumentTemplate(t) }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val icon = when (t) { InvoiceTemplate.MINIMAL -> Icons.Default.CropSquare; InvoiceTemplate.PROFESSIONAL -> Icons.Default.WorkspacePremium; InvoiceTemplate.BOLD -> Icons.Default.Bolt }
                            Icon(icon, null, tint = if (selected) color else TextMuted, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(t.name, color = if (selected) color else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Line items
            ACard {
                SectionLabel("LINE ITEMS")
                lineItems.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface3)
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { vm.updateLineItemRow(index, item.copy(description = it)) },
                                label = { Text("Description", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Cyan, unfocusedBorderColor = Border,
                                    focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                                    focusedLabelColor = Cyan, unfocusedLabelColor = TextMuted
                                )
                            )
                            IconButton(onClick = { vm.removeLineItemRow(index) }) {
                                Icon(Icons.Default.Close, null, tint = Red, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { vm.updateLineItemRow(index, item.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Qty", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan, unfocusedBorderColor = Border, focusedTextColor = TextPri, unfocusedTextColor = TextPri, focusedLabelColor = Cyan, unfocusedLabelColor = TextMuted)
                            )
                            OutlinedTextField(
                                value = item.unitPrice.toString(),
                                onValueChange = { vm.updateLineItemRow(index, item.copy(unitPrice = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Unit Price", fontSize = 10.sp) },
                                modifier = Modifier.weight(1.5f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan, unfocusedBorderColor = Border, focusedTextColor = TextPri, unfocusedTextColor = TextPri, focusedLabelColor = Cyan, unfocusedLabelColor = TextMuted)
                            )
                            Text("= ${String.format("%.2f", item.total)}", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.addAdHocLineItem() }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Add, null, tint = TextSec, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Manual", color = TextSec, fontSize = 11.sp)
                    }
                    Button(onClick = { showCatalogPicker = true }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDim), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Inventory, null, tint = TextPri, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("From Catalog", color = TextPri, fontSize = 11.sp)
                    }
                }
            }

            // Financials
            ACard {
                SectionLabel("TAX & DISCOUNT")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(taxInput, { taxInput = it; vm.updateDocumentModifiers(it.toDoubleOrNull() ?: 0.0, discInput.toDoubleOrNull() ?: 0.0, notesInput, docNumInput, issueInput, dueInput) }, "Tax %", Modifier.weight(1f), KeyboardType.Decimal)
                    ATextField(discInput, { discInput = it; vm.updateDocumentModifiers(taxInput.toDoubleOrNull() ?: 0.0, it.toDoubleOrNull() ?: 0.0, notesInput, docNumInput, issueInput, dueInput) }, "Discount %", Modifier.weight(1f), KeyboardType.Decimal)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(10.dp))
                TotalsRow("Subtotal", String.format("%.2f", docState.subtotal))
                TotalsRow("Discount", "- ${String.format("%.2f", docState.discountAmount)}", Red)
                TotalsRow("Tax",      "+ ${String.format("%.2f", docState.taxAmount)}", Orange)
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GRAND TOTAL", color = TextPri, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(String.format("%.2f", docState.grandTotal), color = Green, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }

            // Notes
            ACard {
                SectionLabel("NOTES / TERMS")
                ATextField(notesInput, { notesInput = it; vm.updateDocumentModifiers(taxInput.toDoubleOrNull() ?: 0.0, discInput.toDoubleOrNull() ?: 0.0, it, docNumInput, issueInput, dueInput) }, "Notes or terms and conditions", singleLine = false)
            }
        }

        // Bottom action bar
        Row(modifier = Modifier.fillMaxWidth().background(Surface1).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard },
                modifier = Modifier.weight(0.6f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Close, null, tint = TextSec, modifier = Modifier.size(16.dp))
            }
            Button(onClick = { vm.saveDocumentToDatabase() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Save, null, tint = Bg)
                Spacer(Modifier.width(6.dp))
                Text("Save", color = Bg, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Client picker dialog
    if (showClientPicker) {
        PickerDialog(title = "Select Client", onDismiss = { showClientPicker = false }) {
            if (clients.isEmpty()) {
                item { Text("No clients saved yet", color = TextMuted, modifier = Modifier.padding(16.dp)) }
            } else {
                items(clients) { client ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { vm.selectClientForDocument(client); showClientPicker = false }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CyanDim), contentAlignment = Alignment.Center) {
                            Text(client.name.first().uppercase(), color = TextPri, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(client.name, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(client.email, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(color = Border)
                }
            }
        }
    }

    // Catalog picker dialog
    if (showCatalogPicker) {
        PickerDialog(title = "Add from Catalog", onDismiss = { showCatalogPicker = false }) {
            if (catalog.isEmpty()) {
                item { Text("No catalog items yet", color = TextMuted, modifier = Modifier.padding(16.dp)) }
            } else {
                items(catalog) { item ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { vm.addCatalogItemToLines(item); showCatalogPicker = false }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(item.sku, color = TextMuted, fontSize = 11.sp)
                        }
                        Text(String.format("%.2f", item.baseUnitPrice), color = Green, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Border)
                }
            }
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, color: Color = TextSec) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 4: Document Preview
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DocumentPreviewView(vm: AdvancedBillingViewModel, docId: Long) {
    val docs    by vm.documents.collectAsState()
    val profile by vm.companyProfile.collectAsState()
    val doc     = docs.firstOrNull { it.id == docId }

    if (doc == null) { vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard; return }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenTopBar("${doc.docType.name} Preview", Icons.Default.Description) {
            vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header card
            ACard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        DocTypeChip(doc.docType)
                        Spacer(Modifier.height(6.dp))
                        Text(doc.docNumber, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(doc.clientNameCopy.ifBlank { "No client assigned" }, color = TextSec, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip(doc.status)
                        Text(String.format("%.2f", doc.grandTotal), color = Green, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(profile.currencySymbol, color = TextMuted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    InfoPair("Issued", doc.issueDate)
                    InfoPair("Due", doc.dueDate)
                    InfoPair("Template", (doc.template ?: InvoiceTemplate.PROFESSIONAL).name)
                }
            }

            // Financials summary
            ACard {
                SectionLabel("FINANCIALS")
                TotalsRow("Subtotal",  String.format("%.2f", doc.subtotal))
                TotalsRow("Discount (${doc.discountPercent}%)", "- ${String.format("%.2f", doc.discountAmount)}", Red)
                TotalsRow("Tax (${doc.taxPercent}%)", "+ ${String.format("%.2f", doc.taxAmount)}", Orange)
                HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GRAND TOTAL", color = TextPri, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("${profile.currencySymbol} ${String.format("%.2f", doc.grandTotal)}", color = Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            // Status controls
            ACard {
                SectionLabel("UPDATE STATUS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DocStatus.DRAFT, DocStatus.SENT, DocStatus.PAID, DocStatus.CANCELLED).forEach { s ->
                        val color = when (s) { DocStatus.PAID -> Green; DocStatus.SENT -> Cyan; DocStatus.CANCELLED -> Red; else -> TextMuted }
                        val selected = doc.status == s
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) color.copy(alpha = 0.2f) else Surface3)
                                .border(1.dp, if (selected) color else Border, RoundedCornerShape(8.dp))
                                .clickable { vm.updateDocumentStatusDirectly(docId, s) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s.name, color = if (selected) color else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Convert document
            ACard {
                SectionLabel("CONVERT TO")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DocumentType.INVOICE, DocumentType.QUOTATION, DocumentType.PROFORMA, DocumentType.CREDIT_NOTE).forEach { t ->
                        val color = when (t) { DocumentType.INVOICE -> Cyan; DocumentType.QUOTATION -> Orange; DocumentType.PROFORMA -> Purple; DocumentType.CREDIT_NOTE -> Red }
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.1f))
                                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { vm.mutateDocumentType(docId, t) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.name.take(7), color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (doc.notes.isNotBlank()) {
                ACard {
                    SectionLabel("NOTES / TERMS")
                    Text(doc.notes, color = TextSec, fontSize = 12.sp)
                }
            }
        }

        // Bottom bar
        Row(modifier = Modifier.fillMaxWidth().background(Surface1).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.removeDocumentFromDatabase(docId) }, modifier = Modifier.weight(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Red.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Delete, null, tint = Red, modifier = Modifier.size(18.dp))
            }
            Button(onClick = { vm.loadDocumentToEditor(docId) }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Surface3), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Edit, null, tint = TextSec, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit", color = TextSec)
            }
            Button(onClick = { vm.exportDocumentToPDFFile(docId) }, modifier = Modifier.weight(1.4f),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Bg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export PDF", color = Bg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = TextSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 5: Client Manager
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ClientManagerView(vm: AdvancedBillingViewModel) {
    val clients by vm.clients.collectAsState()
    var showAdd   by remember { mutableStateOf(false) }
    var nameI     by remember { mutableStateOf("") }
    var addrI     by remember { mutableStateOf("") }
    var emailI    by remember { mutableStateOf("") }
    var phoneI    by remember { mutableStateOf("") }
    var taxI      by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenTopBar("Clients", Icons.Default.People, trailing = {
            IconButton(onClick = { showAdd = !showAdd }) { Icon(Icons.Default.PersonAdd, null, tint = Cyan) }
        }) { vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard }

        if (showAdd) {
            ACard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SectionLabel("ADD NEW CLIENT")
                ATextField(nameI,  { nameI = it },  "Full Name")
                Spacer(Modifier.height(8.dp))
                ATextField(addrI,  { addrI = it },  "Address")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(emailI, { emailI = it }, "Email", Modifier.weight(1f))
                    ATextField(phoneI, { phoneI = it }, "Phone", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ATextField(taxI, { taxI = it }, "Tax Number")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (nameI.isNotBlank()) {
                        vm.createQuickClient(nameI, addrI, emailI, phoneI, taxI)
                        nameI = ""; addrI = ""; emailI = ""; phoneI = ""; taxI = ""; showAdd = false
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan), shape = RoundedCornerShape(10.dp)) {
                    Text("Add Client", color = Bg, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (clients.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No clients yet", color = TextMuted) } }
            }
            items(clients) { client ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CyanDim), contentAlignment = Alignment.Center) {
                        Text(client.name.first().uppercase(), color = TextPri, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(client.name, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(client.email, color = TextMuted, fontSize = 11.sp)
                        if (client.phone.isNotBlank()) Text(client.phone, color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { vm.deleteClient(client.id) }) {
                        Icon(Icons.Default.Delete, null, tint = Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 6: Catalog Manager
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CatalogManagerView(vm: AdvancedBillingViewModel) {
    val items   by vm.catalogItems.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var skuI    by remember { mutableStateOf("") }
    var nameI   by remember { mutableStateOf("") }
    var priceI  by remember { mutableStateOf("") }
    var typeI   by remember { mutableStateOf(ItemType.SERVICE) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenTopBar("Catalog", Icons.Default.Inventory, trailing = {
            IconButton(onClick = { showAdd = !showAdd }) { Icon(Icons.Default.Add, null, tint = Cyan) }
        }) { vm.activeScreen.value = AdvancedBillingViewModel.ActiveScreen.Dashboard }

        if (showAdd) {
            ACard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SectionLabel("ADD CATALOG ITEM")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ATextField(skuI,   { skuI = it },   "SKU",  Modifier.weight(1f))
                    ATextField(nameI,  { nameI = it },  "Name", Modifier.weight(2f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ATextField(priceI, { priceI = it }, "Unit Price", Modifier.weight(1f), KeyboardType.Decimal)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ItemType.entries.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { typeI = t }.padding(4.dp)) {
                                RadioButton(selected = typeI == t, onClick = { typeI = t },
                                    colors = RadioButtonDefaults.colors(selectedColor = Cyan, unselectedColor = TextMuted))
                                Text(t.name, color = if (typeI == t) Cyan else TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (nameI.isNotBlank()) {
                        vm.createQuickCatalogItem(skuI, nameI, priceI.toDoubleOrNull() ?: 0.0, typeI)
                        skuI = ""; nameI = ""; priceI = ""; showAdd = false
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan), shape = RoundedCornerShape(10.dp)) {
                    Text("Add Item", color = Bg, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No items yet", color = TextMuted) } }
            }
            items(items) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (item.itemType == ItemType.SERVICE) CyanDim.copy(alpha = 0.3f) else PurpleDim.copy(alpha = 0.3f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(item.itemType.name, color = if (item.itemType == ItemType.SERVICE) Cyan else Purple, fontSize = 9.sp)
                            }
                        }
                        Text("SKU: ${item.sku}", color = TextMuted, fontSize = 11.sp)
                    }
                    Text(String.format("%.2f", item.baseUnitPrice), color = Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { vm.deleteCatalogItem(item.id) }) {
                        Icon(Icons.Default.Delete, null, tint = Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Composables
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScreenTopBar(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit = {},
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface2)
            .border(bottom = 1.dp, color = Border)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Cyan) }
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun PickerDialog(title: String, onDismiss: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Surface2).border(1.dp, Border, RoundedCornerShape(16.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPri, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
            }
            HorizontalDivider(color = Border)
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), content = content)
        }
    }
}

// Extension for border on one side only
private fun Modifier.border(bottom: androidx.compose.ui.unit.Dp, color: Color): Modifier =
    this.then(Modifier.border(bottom, color, RoundedCornerShape(0.dp)))
