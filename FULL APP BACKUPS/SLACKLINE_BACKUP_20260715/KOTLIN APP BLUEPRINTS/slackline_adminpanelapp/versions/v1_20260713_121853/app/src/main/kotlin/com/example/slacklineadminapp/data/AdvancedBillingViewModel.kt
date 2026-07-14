package com.example.slacklineadminapp.data

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AdvancedBillingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).billingDao()

    // ── Navigation ─────────────────────────────────────────────────────────────
    sealed class ActiveScreen {
        object Dashboard       : ActiveScreen()
        object ClientManager   : ActiveScreen()
        object CatalogManager  : ActiveScreen()
        object CompanyProfile  : ActiveScreen()
        data class DocumentEditor(val docId: Long?)  : ActiveScreen()
        data class DocumentPreview(val docId: Long)  : ActiveScreen()
    }

    val activeScreen = MutableStateFlow<ActiveScreen>(ActiveScreen.Dashboard)

    // ── Reactive Data ──────────────────────────────────────────────────────────
    val documents    = dao.getAllDocumentsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients      = dao.getAllClientsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val catalogItems = dao.getAllCatalogItemsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val companyProfile = dao.getProfileFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanyProfile())

    // ── UI State ───────────────────────────────────────────────────────────────
    val toastMessage  = MutableStateFlow("")
    val isProcessing  = MutableStateFlow(false)

    // ── Dashboard Filters ──────────────────────────────────────────────────────
    val filterStatus  = MutableStateFlow<DocStatus?>(null)
    val filterType    = MutableStateFlow<DocumentType?>(null)

    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(documents, filterStatus, filterType) { docs, status, type ->
        docs.filter { doc ->
            (status == null || doc.status == status) &&
            (type   == null || doc.docType == type)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Editor State ───────────────────────────────────────────────────────────
    val editorDocument = MutableStateFlow(
        DocumentEntity(
            docNumber = "", docType = DocumentType.INVOICE, status = DocStatus.DRAFT,
            issueDate = "", dueDate = "", clientId = null, clientNameCopy = "",
            clientAddressCopy = "", clientEmailCopy = "", subtotal = 0.0, taxPercent = 0.0,
            taxAmount = 0.0, discountPercent = 0.0, discountAmount = 0.0, grandTotal = 0.0, notes = ""
        )
    )
    val editorLineItems = MutableStateFlow<List<LineItemEntity>>(emptyList())

    // ── Company Profile ────────────────────────────────────────────────────────
    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            dao.saveProfile(profile)
            toastMessage.value = "Company profile saved"
        }
    }

    enum class BrandingTarget { LOGO, SIGNATURE }

    /**
     * Copies a picked gallery image into SLACKLINE ADMIN FILES/INVOICE MAKER/Data/Branding/
     * as a fixed filename, re-encoded as PNG, and points the company profile at it.
     * The original gallery file is never referenced again after this — the copy is
     * the source of truth, so it travels with App Backup and survives the original
     * being moved or deleted.
     */
    fun importBrandingImage(uri: android.net.Uri, target: BrandingTarget) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } ?: run {
                    toastMessage.value = "Couldn't read that image."
                    return@launch
                }

                val filename = if (target == BrandingTarget.LOGO) "company_logo.png" else "company_signature.png"
                val destFile = File(AppStorage.invoiceBrandingDir(), filename)

                withContext(Dispatchers.IO) {
                    FileOutputStream(destFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                }

                val current = companyProfile.value
                val updated = if (target == BrandingTarget.LOGO)
                    current.copy(logoPath = destFile.absolutePath)
                else
                    current.copy(signaturePath = destFile.absolutePath)

                dao.saveProfile(updated)
                toastMessage.value = if (target == BrandingTarget.LOGO) "Logo updated" else "Signature updated"
            } catch (e: Exception) {
                toastMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun removeBrandingImage(target: BrandingTarget) {
        viewModelScope.launch {
            val current = companyProfile.value
            val path = if (target == BrandingTarget.LOGO) current.logoPath else current.signaturePath
            if (path.isNotBlank()) File(path).delete()
            val updated = if (target == BrandingTarget.LOGO)
                current.copy(logoPath = "") else current.copy(signaturePath = "")
            dao.saveProfile(updated)
            toastMessage.value = if (target == BrandingTarget.LOGO) "Logo removed" else "Signature removed"
        }
    }

    // ── Document Lifecycle ─────────────────────────────────────────────────────
    fun startNewDocument(type: DocumentType) {
        viewModelScope.launch {
            val profile = companyProfile.value
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val count = dao.getDocumentCountByType(type)
            val prefix = docPrefix(type)
            val generatedNumber = String.format("%s-%s-%04d", prefix, SimpleDateFormat("yy", Locale.getDefault()).format(Date()), count + 1)
            editorDocument.value = DocumentEntity(
                docNumber = generatedNumber, docType = type, status = DocStatus.DRAFT,
                issueDate = today, dueDate = today, clientId = null, clientNameCopy = "",
                clientAddressCopy = "", clientEmailCopy = "", subtotal = 0.0, taxPercent = 0.0,
                taxAmount = 0.0, discountPercent = 0.0, discountAmount = 0.0, grandTotal = 0.0,
                notes = "", template = profile.defaultTemplate
            )
            editorLineItems.value = emptyList()
            activeScreen.value = ActiveScreen.DocumentEditor(null)
        }
    }

    fun loadDocumentToEditor(docId: Long) {
        viewModelScope.launch {
            isProcessing.value = true
            val doc = dao.getDocumentById(docId)
            if (doc != null) {
                editorDocument.value = doc
                editorLineItems.value = dao.getLineItemsForDocument(docId)
                activeScreen.value = ActiveScreen.DocumentEditor(docId)
            }
            isProcessing.value = false
        }
    }

    fun selectClientForDocument(client: ClientEntity) {
        editorDocument.value = editorDocument.value.copy(
            clientId = client.id,
            clientNameCopy = client.name,
            clientAddressCopy = client.address,
            clientEmailCopy = client.email
        )
    }

    fun setDocumentTemplate(template: InvoiceTemplate) {
        editorDocument.value = editorDocument.value.copy(template = template)
    }

    fun addAdHocLineItem() {
        val list = editorLineItems.value.toMutableList()
        list.add(LineItemEntity(documentId = editorDocument.value.id, catalogItemId = null, description = "New Item / Service", quantity = 1.0, unitPrice = 0.0, total = 0.0))
        editorLineItems.value = list
        recalculateDocumentTotals()
    }

    fun addCatalogItemToLines(item: CatalogItemEntity) {
        val list = editorLineItems.value.toMutableList()
        list.add(LineItemEntity(documentId = editorDocument.value.id, catalogItemId = item.id, description = item.name, quantity = 1.0, unitPrice = item.baseUnitPrice, total = item.baseUnitPrice))
        editorLineItems.value = list
        recalculateDocumentTotals()
    }

    fun updateLineItemRow(index: Int, updated: LineItemEntity) {
        val list = editorLineItems.value.toMutableList()
        if (index in list.indices) {
            list[index] = updated.copy(total = updated.quantity * updated.unitPrice)
            editorLineItems.value = list
            recalculateDocumentTotals()
        }
    }

    fun removeLineItemRow(index: Int) {
        val list = editorLineItems.value.toMutableList()
        if (index in list.indices) { list.removeAt(index); editorLineItems.value = list; recalculateDocumentTotals() }
    }

    fun updateDocumentModifiers(tax: Double, discount: Double, notes: String, docNumber: String, issueDate: String, dueDate: String) {
        editorDocument.value = editorDocument.value.copy(
            taxPercent = tax, discountPercent = discount,
            notes = notes, docNumber = docNumber, issueDate = issueDate, dueDate = dueDate
        )
        recalculateDocumentTotals()
    }

    private fun recalculateDocumentTotals() {
        val doc = editorDocument.value
        val sub = editorLineItems.value.sumOf { it.total }
        val discAmt = sub * (doc.discountPercent / 100.0)
        val taxAmt  = (sub - discAmt) * (doc.taxPercent / 100.0)
        editorDocument.value = doc.copy(
            subtotal = sub, discountAmount = discAmt, taxAmount = taxAmt, grandTotal = (sub - discAmt) + taxAmt
        )
    }

    fun saveDocumentToDatabase() {
        viewModelScope.launch {
            isProcessing.value = true
            val finalDoc = editorDocument.value
            val docId = dao.insertDocument(finalDoc)
            dao.deleteLineItemsByDocId(docId)
            dao.insertLineItems(editorLineItems.value.map { it.copy(documentId = docId) })
            isProcessing.value = false
            toastMessage.value = "${finalDoc.docType.name} saved successfully"
            activeScreen.value = ActiveScreen.Dashboard
        }
    }

    fun mutateDocumentType(sourceDocId: Long, targetType: DocumentType) {
        viewModelScope.launch {
            isProcessing.value = true
            val src = dao.getDocumentById(sourceDocId)
            if (src != null) {
                val lines = dao.getLineItemsForDocument(sourceDocId)
                val count = dao.getDocumentCountByType(targetType)
                val nextNum = String.format("%s-%s-%04d", docPrefix(targetType), SimpleDateFormat("yy", Locale.getDefault()).format(Date()), count + 1)
                editorDocument.value = src.copy(id = 0, docType = targetType, docNumber = nextNum, status = DocStatus.DRAFT, notes = "Converted from ${src.docType.name} ${src.docNumber}. " + src.notes)
                editorLineItems.value = lines.map { it.copy(id = 0, documentId = 0) }
                activeScreen.value = ActiveScreen.DocumentEditor(null)
                toastMessage.value = "Converted to ${targetType.name}"
            }
            isProcessing.value = false
        }
    }

    fun updateDocumentStatusDirectly(docId: Long, targetStatus: DocStatus) {
        viewModelScope.launch {
            val doc = dao.getDocumentById(docId)
            if (doc != null) { dao.updateDocument(doc.copy(status = targetStatus)); toastMessage.value = "Marked as ${targetStatus.name}" }
        }
    }

    fun removeDocumentFromDatabase(docId: Long) {
        viewModelScope.launch {
            dao.deleteDocument(docId)
            toastMessage.value = "Document deleted"
            activeScreen.value = ActiveScreen.Dashboard
        }
    }

    fun createQuickClient(name: String, address: String, email: String, phone: String, tax: String) {
        viewModelScope.launch {
            dao.insertClient(ClientEntity(name = name, address = address, email = email, phone = phone, taxNumber = tax))
            toastMessage.value = "Client saved"
        }
    }

    fun deleteClient(id: Long) {
        viewModelScope.launch { dao.deleteClient(id); toastMessage.value = "Client removed" }
    }

    fun createQuickCatalogItem(sku: String, name: String, rate: Double, type: ItemType) {
        viewModelScope.launch {
            dao.insertCatalogItem(CatalogItemEntity(sku = sku, name = name, baseUnitPrice = rate, itemType = type))
            toastMessage.value = "Item saved"
        }
    }

    fun deleteCatalogItem(id: Long) {
        viewModelScope.launch { dao.deleteCatalogItem(id); toastMessage.value = "Item removed" }
    }

    // ── PDF Export Engine ──────────────────────────────────────────────────────
    fun exportDocumentToPDFFile(docId: Long) {
        viewModelScope.launch {
            isProcessing.value = true
            val doc     = dao.getDocumentById(docId)
            val lines   = dao.getLineItemsForDocument(docId)
            val profile = companyProfile.value

            if (doc == null) { isProcessing.value = false; return@launch }

            try {
                val pdf      = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page     = pdf.startPage(pageInfo)
                val canvas   = page.canvas

                when (doc.template ?: InvoiceTemplate.PROFESSIONAL) {
                    InvoiceTemplate.MINIMAL      -> renderMinimalTemplate(canvas, doc, lines, profile)
                    InvoiceTemplate.BOLD         -> renderBoldTemplate(canvas, doc, lines, profile)
                    else                         -> renderProfessionalTemplate(canvas, doc, lines, profile)
                }

                pdf.finishPage(page)

                val dir = AppStorage.accountAndInvoicesDir()

                val outFile = File(dir, "${doc.docType.name}_${doc.docNumber}.pdf")
                pdf.writeTo(FileOutputStream(outFile))
                pdf.close()

                toastMessage.value = "Saved to SLACKLINE ADMIN FILES/Account and Invoices"
            } catch (e: Exception) {
                e.printStackTrace()
                toastMessage.value = "PDF export failed: ${e.message}"
            }
            isProcessing.value = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF Template 1: MINIMAL — clean, white, lots of space
    // ─────────────────────────────────────────────────────────────────────────
    private fun renderMinimalTemplate(canvas: Canvas, doc: DocumentEntity, lines: List<LineItemEntity>, p: CompanyProfile) {
        val black   = android.graphics.Color.BLACK
        val gray    = android.graphics.Color.parseColor("#888888")
        val ltGray  = android.graphics.Color.parseColor("#DDDDDD")

        val titlePaint  = Paint().apply { color = black; textSize = 22f; isFakeBoldText = true; typeface = Typeface.DEFAULT_BOLD }
        val headPaint   = Paint().apply { color = black; textSize = 12f; isFakeBoldText = true }
        val bodyPaint   = Paint().apply { color = black; textSize = 10f }
        val mutedPaint  = Paint().apply { color = gray;  textSize = 9f }
        val linePaint   = Paint().apply { color = ltGray; strokeWidth = 1f }

        // Company name top left
        canvas.drawText(p.companyName.uppercase(), 40f, 55f, titlePaint)
        var y = 70f
        if (p.tagline.isNotBlank()) { canvas.drawText(p.tagline, 40f, y, mutedPaint); y += 14f }
        canvas.drawText(p.address, 40f, y, mutedPaint); y += 13f
        if (p.email.isNotBlank())   canvas.drawText(p.email,   40f, y, mutedPaint)
        if (p.phone.isNotBlank())   canvas.drawText(p.phone,  300f, y, mutedPaint)
        y += 13f
        if (p.taxNumber.isNotBlank()) { canvas.drawText("Tax Reg: ${p.taxNumber}", 40f, y, mutedPaint); y += 13f }

        // Doc type top right
        val docLabel = doc.docType.name
        canvas.drawText(docLabel, 555f - docLabel.length * 7f, 55f, titlePaint)
        canvas.drawText(doc.docNumber, 555f - doc.docNumber.length * 6f, 70f, bodyPaint)
        canvas.drawText("Issued: ${doc.issueDate}", 555f - 100f, 83f, mutedPaint)
        canvas.drawText("Due:    ${doc.dueDate}",   555f - 100f, 96f, mutedPaint)

        // Divider
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 20f

        // Bill to
        canvas.drawText("BILL TO", 40f, y, mutedPaint); y += 14f
        canvas.drawText(doc.clientNameCopy, 40f, y, headPaint); y += 14f
        if (doc.clientAddressCopy.isNotBlank()) { canvas.drawText(doc.clientAddressCopy, 40f, y, bodyPaint); y += 13f }
        if (doc.clientEmailCopy.isNotBlank())   { canvas.drawText(doc.clientEmailCopy,   40f, y, mutedPaint); y += 13f }

        y += 10f; canvas.drawLine(40f, y, 555f, y, linePaint); y += 18f

        // Table header
        canvas.drawText("DESCRIPTION", 40f, y, mutedPaint)
        canvas.drawText("QTY",         340f, y, mutedPaint)
        canvas.drawText("UNIT PRICE",  390f, y, mutedPaint)
        canvas.drawText("TOTAL",       500f, y, mutedPaint)
        y += 6f; canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f

        // Line items
        lines.forEach { line ->
            canvas.drawText(line.description.take(42), 40f, y, bodyPaint)
            canvas.drawText(String.format("%.1f", line.quantity), 340f, y, bodyPaint)
            canvas.drawText(String.format("%.2f", line.unitPrice), 390f, y, bodyPaint)
            canvas.drawText(String.format("%.2f", line.total), 500f, y, bodyPaint)
            y += 18f
        }

        y += 6f; canvas.drawLine(40f, y, 555f, y, linePaint); y += 18f

        // Totals right-aligned
        canvas.drawText("Subtotal",                                     370f, y, mutedPaint)
        canvas.drawText("${p.currencySymbol} ${String.format("%.2f", doc.subtotal)}", 490f, y, bodyPaint); y += 16f
        canvas.drawText("Discount (${doc.discountPercent}%)",           370f, y, mutedPaint)
        canvas.drawText("- ${String.format("%.2f", doc.discountAmount)}", 490f, y, bodyPaint); y += 16f
        canvas.drawText("Tax (${doc.taxPercent}%)",                     370f, y, mutedPaint)
        canvas.drawText("+ ${String.format("%.2f", doc.taxAmount)}",    490f, y, bodyPaint); y += 20f
        canvas.drawLine(370f, y, 555f, y, linePaint); y += 16f
        canvas.drawText("TOTAL",                                        370f, y, headPaint)
        canvas.drawText("${p.currencySymbol} ${String.format("%.2f", doc.grandTotal)}", 470f, y, titlePaint)

        // Payment details
        if (p.bankName.isNotBlank()) {
            y += 40f; canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f
            canvas.drawText("PAYMENT DETAILS", 40f, y, mutedPaint); y += 14f
            canvas.drawText("Bank: ${p.bankName}",                  40f, y, bodyPaint); y += 13f
            canvas.drawText("Account Name: ${p.bankAccountName}",   40f, y, bodyPaint); y += 13f
            canvas.drawText("Account No: ${p.bankAccountNumber}",   40f, y, bodyPaint); y += 13f
            if (p.bankBranch.isNotBlank()) { canvas.drawText("Branch: ${p.bankBranch}", 40f, y, bodyPaint); y += 13f }
        }

        // Notes
        if (doc.notes.isNotBlank()) {
            y += 10f; canvas.drawText("Notes: ${doc.notes}", 40f, y, mutedPaint)
        }

        // Signature
        drawSignatureIfExists(canvas, p.signaturePath, 400f, y + 40f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF Template 2: PROFESSIONAL — header band, structured layout
    // ─────────────────────────────────────────────────────────────────────────
    private fun renderProfessionalTemplate(canvas: Canvas, doc: DocumentEntity, lines: List<LineItemEntity>, p: CompanyProfile) {
        val brandColor  = android.graphics.Color.parseColor("#0EA5E9")
        val darkColor   = android.graphics.Color.parseColor("#1E293B")
        val white       = android.graphics.Color.WHITE
        val gray        = android.graphics.Color.parseColor("#64748B")
        val ltGray      = android.graphics.Color.parseColor("#E2E8F0")
        val black       = android.graphics.Color.BLACK

        // Header band
        val headerPaint = Paint().apply { color = darkColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 90f, headerPaint)

        val accentPaint = Paint().apply { color = brandColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 85f, 595f, 90f, accentPaint)

        val wTitle  = Paint().apply { color = white; textSize = 20f; isFakeBoldText = true }
        val wSub    = Paint().apply { color = android.graphics.Color.parseColor("#94A3B8"); textSize = 9f }
        val wDoc    = Paint().apply { color = brandColor; textSize = 18f; isFakeBoldText = true }

        canvas.drawText(p.companyName.uppercase(), 30f, 40f, wTitle)
        var y = 55f
        if (p.tagline.isNotBlank()) { canvas.drawText(p.tagline, 30f, y, wSub); y += 13f }
        canvas.drawText("${p.address}  |  ${p.email}", 30f, y, wSub)

        canvas.drawText(doc.docType.name, 595f - doc.docType.name.length * 11f - 20f, 38f, wDoc)
        val wNum = Paint().apply { color = white; textSize = 10f }
        canvas.drawText(doc.docNumber, 595f - doc.docNumber.length * 6f - 20f, 55f, wNum)
        canvas.drawText("Issued: ${doc.issueDate}  Due: ${doc.dueDate}", 595f - 160f, 70f, wSub)

        y = 115f

        // Two-column info
        val headPaint  = Paint().apply { color = darkColor; textSize = 9f; isFakeBoldText = true }
        val bodyPaint  = Paint().apply { color = black;     textSize = 10f }
        val mutedPaint = Paint().apply { color = gray;      textSize = 9f }
        val linePaint  = Paint().apply { color = ltGray;    strokeWidth = 1f }

        canvas.drawText("BILLED TO", 30f, y, headPaint); y += 14f
        canvas.drawText(doc.clientNameCopy, 30f, y, bodyPaint); y += 13f
        if (doc.clientAddressCopy.isNotBlank()) { canvas.drawText(doc.clientAddressCopy, 30f, y, mutedPaint); y += 12f }
        if (doc.clientEmailCopy.isNotBlank())   { canvas.drawText(doc.clientEmailCopy,   30f, y, mutedPaint); y += 12f }

        y += 16f; canvas.drawLine(30f, y, 565f, y, linePaint); y += 16f

        // Table header band
        val thPaint = Paint().apply { color = android.graphics.Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
        canvas.drawRect(30f, y - 12f, 565f, y + 8f, thPaint)
        val thText = Paint().apply { color = gray; textSize = 9f; isFakeBoldText = true }
        canvas.drawText("DESCRIPTION", 36f,  y, thText)
        canvas.drawText("QTY",         340f, y, thText)
        canvas.drawText("UNIT PRICE",  390f, y, thText)
        canvas.drawText("AMOUNT",      505f, y, thText)
        y += 18f

        lines.forEachIndexed { i, line ->
            if (i % 2 == 0) {
                val rowPaint = Paint().apply { color = android.graphics.Color.parseColor("#FAFAFA"); style = Paint.Style.FILL }
                canvas.drawRect(30f, y - 13f, 565f, y + 5f, rowPaint)
            }
            canvas.drawText(line.description.take(44), 36f, y, bodyPaint)
            canvas.drawText(String.format("%.1f", line.quantity), 340f, y, bodyPaint)
            canvas.drawText(String.format("%.2f", line.unitPrice), 390f, y, bodyPaint)
            canvas.drawText(String.format("%.2f", line.total), 505f, y, bodyPaint)
            y += 20f
        }

        y += 8f; canvas.drawLine(30f, y, 565f, y, linePaint); y += 18f

        // Totals
        fun drawRow(label: String, value: String, bold: Boolean = false) {
            val lp = if (bold) Paint().apply { color = darkColor; textSize = 11f; isFakeBoldText = true }
                     else mutedPaint
            val vp = if (bold) Paint().apply { color = brandColor; textSize = 13f; isFakeBoldText = true }
                     else bodyPaint
            canvas.drawText(label, 370f, y, lp)
            canvas.drawText(value, 490f, y, vp)
            y += if (bold) 22f else 16f
        }

        drawRow("Subtotal",  "${p.currencySymbol} ${String.format("%.2f", doc.subtotal)}")
        drawRow("Discount",  "- ${String.format("%.2f", doc.discountAmount)}")
        drawRow("Tax (${doc.taxPercent}%)", "+ ${String.format("%.2f", doc.taxAmount)}")
        canvas.drawLine(370f, y, 565f, y, linePaint); y += 14f
        drawRow("GRAND TOTAL", "${p.currencySymbol} ${String.format("%.2f", doc.grandTotal)}", bold = true)

        // Payment details box
        if (p.bankName.isNotBlank()) {
            y += 20f
            val boxPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }
            canvas.drawRect(30f, y - 12f, 280f, y + 60f, boxPaint)
            val boxLine = Paint().apply { color = brandColor; strokeWidth = 2f }
            canvas.drawLine(30f, y - 12f, 30f, y + 60f, boxLine)
            canvas.drawText("PAYMENT DETAILS", 38f, y, headPaint); y += 14f
            canvas.drawText("${p.bankName} — ${p.bankAccountName}", 38f, y, bodyPaint); y += 13f
            canvas.drawText("Account: ${p.bankAccountNumber}", 38f, y, bodyPaint); y += 13f
            if (p.bankBranch.isNotBlank()) { canvas.drawText("Branch: ${p.bankBranch}", 38f, y, mutedPaint) }
        }

        // Notes & signature
        if (doc.notes.isNotBlank()) {
            y += 30f; canvas.drawText("Terms & Notes:", 30f, y, headPaint); y += 14f
            doc.notes.split("\n").forEach { l -> canvas.drawText(l, 30f, y, mutedPaint); y += 13f }
        }

        drawSignatureIfExists(canvas, p.signaturePath, 400f, y + 30f)

        if (p.paymentNotes.isNotBlank()) {
            val footPaint = Paint().apply { color = gray; textSize = 8f }
            canvas.drawText(p.paymentNotes, 30f, 820f, footPaint)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF Template 3: BOLD — high contrast, large type, strong color blocks
    // ─────────────────────────────────────────────────────────────────────────
    private fun renderBoldTemplate(canvas: Canvas, doc: DocumentEntity, lines: List<LineItemEntity>, p: CompanyProfile) {
        val accent  = android.graphics.Color.parseColor("#7C3AED")
        val dark    = android.graphics.Color.parseColor("#0F0F0F")
        val white   = android.graphics.Color.WHITE
        val silver  = android.graphics.Color.parseColor("#AAAAAA")
        val black   = android.graphics.Color.BLACK

        // Full-width top stripe
        canvas.drawRect(0f, 0f, 595f, 110f, Paint().apply { color = accent; style = Paint.Style.FILL })
        // Accent side bar
        canvas.drawRect(0f, 0f, 8f, 842f, Paint().apply { color = accent; style = Paint.Style.FILL })

        val bigTitle = Paint().apply { color = white; textSize = 26f; isFakeBoldText = true; typeface = Typeface.DEFAULT_BOLD }
        val wSub     = Paint().apply { color = android.graphics.Color.parseColor("#DDD6FE"); textSize = 9f }
        val docLabel = Paint().apply { color = white; textSize = 32f; isFakeBoldText = true; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }

        canvas.drawText(p.companyName.uppercase(), 24f, 45f, bigTitle)
        var y = 60f
        canvas.drawText(p.address, 24f, y, wSub); y += 13f
        canvas.drawText("${p.email}  ${p.phone}", 24f, y, wSub)
        if (p.taxNumber.isNotBlank()) canvas.drawText("TAX: ${p.taxNumber}", 24f, y + 13f, wSub)

        canvas.drawText(doc.docType.name, 580f, 58f, docLabel)
        val wNum = Paint().apply { color = android.graphics.Color.parseColor("#DDD6FE"); textSize = 12f; textAlign = Paint.Align.RIGHT }
        canvas.drawText(doc.docNumber, 580f, 76f, wNum)
        canvas.drawText("${doc.issueDate}  →  Due ${doc.dueDate}", 580f, 92f, Paint().apply { color = android.graphics.Color.parseColor("#DDD6FE"); textSize = 9f; textAlign = Paint.Align.RIGHT })

        y = 130f
        val headPaint  = Paint().apply { color = dark; textSize = 10f; isFakeBoldText = true }
        val bodyPaint  = Paint().apply { color = black; textSize = 10f }
        val mutedPaint = Paint().apply { color = silver; textSize = 9f }

        canvas.drawText("BILLED TO", 24f, y, mutedPaint); y += 15f
        canvas.drawText(doc.clientNameCopy, 24f, y, Paint().apply { color = dark; textSize = 14f; isFakeBoldText = true }); y += 16f
        if (doc.clientAddressCopy.isNotBlank()) { canvas.drawText(doc.clientAddressCopy, 24f, y, mutedPaint); y += 13f }
        if (doc.clientEmailCopy.isNotBlank())   { canvas.drawText(doc.clientEmailCopy,   24f, y, mutedPaint); y += 13f }

        y += 14f
        // Table header — solid accent band
        canvas.drawRect(0f, y - 14f, 595f, y + 8f, Paint().apply { color = dark; style = Paint.Style.FILL })
        val thPaint = Paint().apply { color = white; textSize = 9f; isFakeBoldText = true }
        canvas.drawText("DESCRIPTION", 24f,  y, thPaint)
        canvas.drawText("QTY",         340f, y, thPaint)
        canvas.drawText("UNIT PRICE",  390f, y, thPaint)
        canvas.drawText("TOTAL",       505f, y, thPaint)
        y += 20f

        lines.forEachIndexed { i, line ->
            if (i % 2 == 0) canvas.drawRect(8f, y - 13f, 595f, y + 6f, Paint().apply { color = android.graphics.Color.parseColor("#F3F0FF"); style = Paint.Style.FILL })
            canvas.drawText(line.description.take(44), 24f,  y, bodyPaint)
            canvas.drawText(String.format("%.1f",  line.quantity),  340f, y, bodyPaint)
            canvas.drawText(String.format("%.2f",  line.unitPrice), 390f, y, bodyPaint)
            canvas.drawText(String.format("%.2f",  line.total),     505f, y, bodyPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(8f, y, 595f, y, Paint().apply { color = accent; strokeWidth = 2f }); y += 20f

        // Totals
        fun totRow(label: String, value: String, highlight: Boolean = false) {
            val lp = if (highlight) Paint().apply { color = accent; textSize = 13f; isFakeBoldText = true } else mutedPaint
            val vp = if (highlight) Paint().apply { color = accent; textSize = 16f; isFakeBoldText = true } else bodyPaint
            canvas.drawText(label, 360f, y, lp)
            canvas.drawText(value, 475f, y, vp)
            y += if (highlight) 24f else 17f
        }

        totRow("Subtotal",             "${p.currencySymbol} ${String.format("%.2f", doc.subtotal)}")
        totRow("Discount",             "- ${String.format("%.2f", doc.discountAmount)}")
        totRow("Tax (${doc.taxPercent}%)", "+ ${String.format("%.2f", doc.taxAmount)}")
        canvas.drawLine(360f, y, 587f, y, Paint().apply { color = accent; strokeWidth = 1.5f }); y += 14f
        totRow("GRAND TOTAL",          "${p.currencySymbol} ${String.format("%.2f", doc.grandTotal)}", highlight = true)

        // Payment
        if (p.bankName.isNotBlank()) {
            y += 20f
            canvas.drawRect(8f, y - 14f, 280f, y + 56f, Paint().apply { color = android.graphics.Color.parseColor("#F3F0FF"); style = Paint.Style.FILL })
            canvas.drawRect(8f, y - 14f, 12f, y + 56f, Paint().apply { color = accent; style = Paint.Style.FILL })
            canvas.drawText("PAYMENT DETAILS", 20f, y, headPaint); y += 14f
            canvas.drawText("${p.bankName} — ${p.bankAccountName}", 20f, y, bodyPaint); y += 13f
            canvas.drawText("Account: ${p.bankAccountNumber}", 20f, y, bodyPaint); y += 13f
            if (p.bankBranch.isNotBlank()) canvas.drawText("Branch: ${p.bankBranch}", 20f, y, mutedPaint)
        }

        if (doc.notes.isNotBlank()) {
            y += 30f; canvas.drawText("Notes:", 24f, y, headPaint); y += 14f
            doc.notes.split("\n").forEach { l -> canvas.drawText(l, 24f, y, mutedPaint); y += 13f }
        }

        drawSignatureIfExists(canvas, p.signaturePath, 400f, y + 30f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helper — draws signature bitmap if path is valid
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawSignatureIfExists(canvas: Canvas, path: String, x: Float, y: Float) {
        if (path.isBlank()) return
        try {
            val bmp = BitmapFactory.decodeFile(path) ?: return
            val scaled = Bitmap.createScaledBitmap(bmp, 140, 50, true)
            canvas.drawBitmap(scaled, x, y, null)
            val lp = Paint().apply { color = android.graphics.Color.parseColor("#888888"); textSize = 8f }
            canvas.drawText("Authorised Signature", x, y + 62f, lp)
        } catch (_: Exception) {}
    }

    private fun docPrefix(type: DocumentType) = when (type) {
        DocumentType.INVOICE     -> "INV"
        DocumentType.QUOTATION   -> "QT"
        DocumentType.CREDIT_NOTE -> "CRN"
        DocumentType.PROFORMA    -> "PRO"
    }
}
