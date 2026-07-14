package com.example.slacklineadminapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class InvoiceViewModel : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────────────
    sealed class Screen {
        object Hub : Screen()
        object Profile : Screen()
        data class Editor(val invoiceId: String?) : Screen()
        data class Preview(val invoiceId: String) : Screen()
    }

    private val _screen = MutableStateFlow<Screen>(Screen.Hub)
    val screen: StateFlow<Screen> = _screen

    private val _invoices = MutableStateFlow<List<InvoiceStorage.Invoice>>(emptyList())
    val invoices: StateFlow<List<InvoiceStorage.Invoice>> = _invoices

    private val _profile = MutableStateFlow(InvoiceStorage.CompanyProfile())
    val profile: StateFlow<InvoiceStorage.CompanyProfile> = _profile

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast
    fun consumeToast() { _toast.value = "" }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    // ── Editor working state ──────────────────────────────────────────────────
    private val _draft = MutableStateFlow(InvoiceStorage.Invoice())
    val draft: StateFlow<InvoiceStorage.Invoice> = _draft

    // ── Init ──────────────────────────────────────────────────────────────────
    fun init(ctx: Context) {
        _invoices.value = InvoiceStorage.loadInvoices()
        _profile.value  = InvoiceStorage.getProfile(ctx)
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    fun goHub()               { _screen.value = Screen.Hub }
    fun goProfile()           { _screen.value = Screen.Profile }
    fun goNew(ctx: Context)   {
        val p = _profile.value
        _draft.value = InvoiceStorage.Invoice(
            invoiceNumber = InvoiceStorage.nextInvoiceNumber(p.invoicePrefix),
            issueDate     = todayString(),
            dueDate       = plusDays(30),
            notes         = p.notesDefault,
            taxPercent    = 0.0
        )
        _screen.value = Screen.Editor(null)
    }
    fun goEdit(inv: InvoiceStorage.Invoice) {
        _draft.value  = inv
        _screen.value = Screen.Editor(inv.id)
    }
    fun goPreview(id: String) { _screen.value = Screen.Preview(id) }

    // ── Draft mutations ───────────────────────────────────────────────────────
    fun updateDraft(f: (InvoiceStorage.Invoice) -> InvoiceStorage.Invoice) {
        _draft.value = f(_draft.value)
    }
    fun addLine()    { updateDraft { it.copy(items = it.items + InvoiceStorage.LineItem()) } }
    fun removeLine(id: String) { updateDraft { it.copy(items = it.items.filter { l -> l.id != id }) } }
    fun updateLine(line: InvoiceStorage.LineItem) {
        updateDraft { inv -> inv.copy(items = inv.items.map { if (it.id == line.id) line else it }) }
    }

    // ── Save / delete ─────────────────────────────────────────────────────────
    fun saveDraft() {
        InvoiceStorage.saveInvoice(_draft.value)
        _invoices.value = InvoiceStorage.loadInvoices()
        _toast.value    = "Invoice saved."
        _screen.value   = Screen.Hub
    }

    fun deleteInvoice(id: String) {
        InvoiceStorage.deleteInvoice(id)
        _invoices.value = InvoiceStorage.loadInvoices()
        _toast.value    = "Invoice deleted."
        if (_screen.value is Screen.Preview) _screen.value = Screen.Hub
    }

    fun markStatus(id: String, status: String) {
        val inv = _invoices.value.firstOrNull { it.id == id } ?: return
        InvoiceStorage.saveInvoice(inv.copy(status = status))
        _invoices.value = InvoiceStorage.loadInvoices()
        _toast.value    = "Marked as $status."
    }

    // ── Profile save ──────────────────────────────────────────────────────────
    fun saveProfile(ctx: Context, p: InvoiceStorage.CompanyProfile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { InvoiceStorage.saveProfile(ctx, p) }
            _profile.value  = p
            _invoices.value = InvoiceStorage.loadInvoices()
            _toast.value    = "Company profile saved."
            _screen.value   = Screen.Hub
        }
    }

    // ── Image imports ─────────────────────────────────────────────────────────
    fun importLogo(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { inp ->
                        val bmp = BitmapFactory.decodeStream(inp)
                        FileOutputStream(InvoiceStorage.logoFile()).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                        }
                    }
                    val newProfile = _profile.value.copy(logoPath = InvoiceStorage.logoFile().absolutePath)
                    InvoiceStorage.saveProfile(ctx, newProfile)
                    _profile.value = newProfile
                    _toast.value   = "Logo saved."
                } catch (e: Exception) {
                    _toast.value = "Failed to import logo: ${e.message}"
                }
            }
        }
    }

    fun importSignature(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { inp ->
                        val bmp = BitmapFactory.decodeStream(inp)
                        FileOutputStream(InvoiceStorage.sigFile()).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                        }
                    }
                    val newProfile = _profile.value.copy(signaturePath = InvoiceStorage.sigFile().absolutePath)
                    InvoiceStorage.saveProfile(ctx, newProfile)
                    _profile.value = newProfile
                    _toast.value   = "Signature saved."
                } catch (e: Exception) {
                    _toast.value = "Failed to import signature: ${e.message}"
                }
            }
        }
    }

    // ── PDF generation ────────────────────────────────────────────────────────
    fun exportPdf(ctx: Context, invoiceId: String) {
        val inv = _invoices.value.firstOrNull { it.id == invoiceId } ?: return
        val p   = _profile.value
        _busy.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = generatePdf(ctx, inv, p)
                    _toast.value = "PDF saved: ${file.name}"
                    AppStorage.logActivity("INVOICE_PDF", "Exported ${inv.invoiceNumber}", "InvoiceMaker")
                } catch (e: Exception) {
                    _toast.value = "PDF error: ${e.message}"
                }
            }
            _busy.value = false
        }
    }

    // ── PDF builder (Android PdfDocument — no external library needed) ────────
    private fun generatePdf(
        ctx: Context,
        inv: InvoiceStorage.Invoice,
        profile: InvoiceStorage.CompanyProfile
    ): File {
        val pageW = 595   // A4 72dpi points
        val pageH = 842
        val pdf   = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page     = pdf.startPage(pageInfo)
        val c        = page.canvas

        val margin = 40f
        val col2   = pageW / 2f + margin / 2f
        var y      = margin

        // ── Paints ────────────────────────────────────────────────────────────
        fun paint(
            color: Int   = Color.BLACK,
            size: Float  = 10f,
            bold: Boolean = false,
            align: Paint.Align = Paint.Align.LEFT
        ) = Paint().apply {
            this.color     = color
            this.textSize  = size
            this.typeface  = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.textAlign = align
            this.isAntiAlias = true
        }
        val teal   = android.graphics.Color.rgb(0, 163, 131)
        val darkGy = android.graphics.Color.rgb(40, 40, 40)
        val midGy  = android.graphics.Color.rgb(100, 100, 100)
        val ltGy   = android.graphics.Color.rgb(230, 230, 230)
        val white  = android.graphics.Color.WHITE

        // ── Header background strip ───────────────────────────────────────────
        val headerPaint = Paint().apply { color = teal }
        c.drawRect(0f, 0f, pageW.toFloat(), 90f, headerPaint)

        // ── Logo (top-left inside header) ─────────────────────────────────────
        val logoFile = InvoiceStorage.logoFile()
        if (logoFile.exists()) {
            val bmp = BitmapFactory.decodeFile(logoFile.absolutePath)
            if (bmp != null) {
                val logoH = 60f
                val scale = logoH / bmp.height
                val logoW = bmp.width * scale
                val dst   = android.graphics.RectF(margin, 15f, margin + logoW, 15f + logoH)
                c.drawBitmap(bmp, null, dst, null)
            }
        } else {
            // Company name as text logo fallback
            c.drawText(
                profile.companyName.uppercase().take(22),
                margin, 55f,
                paint(white, 18f, bold = true)
            )
        }

        // ── "INVOICE" title (top-right) ───────────────────────────────────────
        c.drawText("INVOICE", pageW - margin, 45f, paint(white, 26f, bold = true, align = Paint.Align.RIGHT))
        c.drawText(inv.invoiceNumber, pageW - margin, 68f, paint(Color.argb(210, 255, 255, 255), 11f, align = Paint.Align.RIGHT))

        y = 110f

        // ── From / Bill-To two columns ────────────────────────────────────────
        fun drawMini(label: String, cx: Float, cy: Float): Float {
            c.drawText(label, cx, cy, paint(teal, 8f, bold = true))
            return cy + 13f
        }
        fun drawLine(text: String, cx: Float, cy: Float, bold: Boolean = false): Float {
            if (text.isBlank()) return cy
            c.drawText(text, cx, cy, paint(darkGy, if (bold) 10f else 9f, bold))
            return cy + 13f
        }

        var ly = drawMini("FROM", margin, y)
        ly = drawLine(profile.companyName,    margin, ly, bold = true)
        ly = drawLine(profile.companyAddress, margin, ly)
        ly = drawLine(profile.companyEmail,   margin, ly)
        ly = drawLine(profile.companyPhone,   margin, ly)
        if (profile.taxNumber.isNotBlank())
            ly = drawLine("Tax/VAT: ${profile.taxNumber}", margin, ly)

        var ry = drawMini("BILL TO", col2, y)
        ry = drawLine(inv.clientName,    col2, ry, bold = true)
        ry = drawLine(inv.clientAddress, col2, ry)
        ry = drawLine(inv.clientEmail,   col2, ry)

        y = maxOf(ly, ry) + 10f

        // ── Issue / Due date row ──────────────────────────────────────────────
        val dateBg = Paint().apply { color = ltGy }
        c.drawRoundRect(android.graphics.RectF(margin, y, pageW - margin, y + 26f), 4f, 4f, dateBg)
        c.drawText("Issue Date: ${inv.issueDate}", margin + 8f, y + 17f, paint(darkGy, 9f))
        c.drawText("Due Date: ${inv.dueDate}",     col2,        y + 17f, paint(darkGy, 9f))
        y += 36f

        // ── Line items table header ───────────────────────────────────────────
        val colDesc = margin
        val colQty  = pageW - margin - 200f
        val colUnit = pageW - margin - 120f
        val colTot  = pageW - margin.toFloat()

        val hdrBg = Paint().apply { color = android.graphics.Color.rgb(20, 20, 20) }
        c.drawRect(margin, y, pageW - margin, y + 22f, hdrBg)
        c.drawText("DESCRIPTION", colDesc + 4f, y + 15f, paint(white, 8f, bold = true))
        c.drawText("QTY",         colQty,       y + 15f, paint(white, 8f, bold = true, align = Paint.Align.CENTER))
        c.drawText("UNIT PRICE",  colUnit,      y + 15f, paint(white, 8f, bold = true, align = Paint.Align.RIGHT))
        c.drawText("TOTAL",       colTot,       y + 15f, paint(white, 8f, bold = true, align = Paint.Align.RIGHT))
        y += 22f

        // ── Line items ────────────────────────────────────────────────────────
        val cur = profile.currency
        inv.items.forEachIndexed { idx, item ->
            val rowBg = Paint().apply {
                color = if (idx % 2 == 0) android.graphics.Color.rgb(248, 248, 248)
                        else white
            }
            c.drawRect(margin, y, pageW - margin, y + 20f, rowBg)
            val desc = item.description.take(48)
            c.drawText(desc,                                       colDesc + 4f, y + 14f, paint(darkGy, 8.5f))
            c.drawText(fmtNum(item.quantity),                      colQty,       y + 14f, paint(midGy,  8.5f, align = Paint.Align.CENTER))
            c.drawText("$cur ${fmtMoney(item.unitPrice)}",        colUnit,      y + 14f, paint(midGy,  8.5f, align = Paint.Align.RIGHT))
            c.drawText("$cur ${fmtMoney(item.total)}",            colTot,       y + 14f, paint(darkGy, 8.5f, bold = true, align = Paint.Align.RIGHT))
            y += 20f
        }

        // ── Divider ───────────────────────────────────────────────────────────
        val divPaint = Paint().apply { color = ltGy; strokeWidth = 1f }
        c.drawLine(margin, y + 4f, pageW - margin, y + 4f, divPaint)
        y += 14f

        // ── Totals block (right-aligned) ──────────────────────────────────────
        val labelX = pageW - margin - 130f
        val valueX = pageW - margin.toFloat()

        fun totalRow(label: String, value: String, bold: Boolean = false, hiColor: Int = darkGy): Float {
            c.drawText(label, labelX, y, paint(midGy, 9f))
            c.drawText(value, valueX, y, paint(hiColor, if (bold) 11f else 9f, bold, align = Paint.Align.RIGHT))
            y += 16f
            return y
        }

        totalRow("Subtotal:", "$cur ${fmtMoney(inv.subtotal)}")
        if (inv.discountPercent > 0)
            totalRow("Discount (${fmtNum(inv.discountPercent)}%):", "- $cur ${fmtMoney(inv.discountAmount)}")
        if (inv.taxPercent > 0)
            totalRow("Tax (${fmtNum(inv.taxPercent)}%):", "$cur ${fmtMoney(inv.taxAmount)}")

        // Grand total row with teal background
        val totBgRect = android.graphics.RectF(labelX - 8f, y - 13f, pageW - margin, y + 5f)
        c.drawRoundRect(totBgRect, 4f, 4f, Paint().apply { color = teal })
        c.drawText("TOTAL DUE:", labelX, y, paint(white, 9f, bold = true))
        c.drawText("$cur ${fmtMoney(inv.grandTotal)}", valueX, y, paint(white, 13f, bold = true, align = Paint.Align.RIGHT))
        y += 24f

        // ── Bank details (left side, same row as totals) ──────────────────────
        if (profile.bankName.isNotBlank()) {
            val bankY = y - 56f
            c.drawText("PAYMENT DETAILS", margin, bankY, paint(teal, 8f, bold = true))
            var by2 = bankY + 13f
            if (profile.bankName.isNotBlank())    { c.drawText(profile.bankName,    margin, by2, paint(darkGy, 8.5f)); by2 += 12f }
            if (profile.bankAccount.isNotBlank()) { c.drawText("Acc: ${profile.bankAccount}", margin, by2, paint(darkGy, 8.5f)); by2 += 12f }
            if (profile.bankIban.isNotBlank())    { c.drawText("IBAN: ${profile.bankIban}", margin, by2, paint(darkGy, 8.5f)) }
        }

        y += 10f

        // ── Notes ─────────────────────────────────────────────────────────────
        if (inv.notes.isNotBlank()) {
            c.drawText("NOTES", margin, y, paint(teal, 8f, bold = true))
            y += 13f
            // word-wrap notes
            val words = inv.notes.split(" ")
            var line  = ""
            val noteP = paint(midGy, 8.5f)
            for (w in words) {
                val test = if (line.isEmpty()) w else "$line $w"
                if (noteP.measureText(test) > (pageW - margin * 2)) {
                    c.drawText(line, margin, y, noteP); y += 12f; line = w
                } else { line = test }
            }
            if (line.isNotEmpty()) { c.drawText(line, margin, y, noteP); y += 12f }
            y += 4f
        }

        // ── Signature ─────────────────────────────────────────────────────────
        val sigFile = InvoiceStorage.sigFile()
        if (sigFile.exists()) {
            val bmp = BitmapFactory.decodeFile(sigFile.absolutePath)
            if (bmp != null) {
                val sigH  = 50f
                val scale = sigH / bmp.height
                val sigW  = (bmp.width * scale).coerceAtMost(160f)
                val dst   = android.graphics.RectF(margin, y, margin + sigW, y + sigH)
                c.drawBitmap(bmp, null, dst, null)
                y += sigH + 4f
            }
        }
        c.drawLine(margin, y, margin + 160f, y, divPaint)
        y += 12f
        c.drawText("Authorised Signature", margin, y, paint(midGy, 8f))

        // ── Footer ────────────────────────────────────────────────────────────
        val footerY = pageH - 30f
        c.drawRect(0f, footerY - 14f, pageW.toFloat(), pageH.toFloat(), Paint().apply { color = teal })
        val footerText = buildString {
            if (profile.companyWebsite.isNotBlank()) append(profile.companyWebsite)
            if (profile.companyEmail.isNotBlank())   append("  •  ${profile.companyEmail}")
            if (profile.companyPhone.isNotBlank())   append("  •  ${profile.companyPhone}")
        }
        c.drawText(footerText, pageW / 2f, footerY + 2f, paint(white, 8f, align = Paint.Align.CENTER))

        pdf.finishPage(page)

        val outFile = File(
            InvoiceStorage.pdfExportDir(),
            "${inv.invoiceNumber.replace("/", "-")}_${AppStorage.timestamp()}.pdf"
        )
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    private fun fmtMoney(v: Double) = String.format("%.2f", v)
    private fun fmtNum(v: Double)   = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun plusDays(n: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, n)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
