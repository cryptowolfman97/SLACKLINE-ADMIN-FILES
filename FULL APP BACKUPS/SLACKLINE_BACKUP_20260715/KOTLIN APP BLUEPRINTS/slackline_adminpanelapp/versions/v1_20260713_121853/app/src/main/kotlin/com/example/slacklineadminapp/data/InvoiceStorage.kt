package com.example.slacklineadminapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

// ── DataStore for company profile & invoice settings ─────────────────────────
private val Context.invoiceDataStore by preferencesDataStore(name = "invoice_prefs")

object InvoiceStorage {

    // ── Preference keys ───────────────────────────────────────────────────────
    private val KEY_COMPANY_NAME    = stringPreferencesKey("inv_company_name")
    private val KEY_COMPANY_ADDRESS = stringPreferencesKey("inv_company_address")
    private val KEY_COMPANY_EMAIL   = stringPreferencesKey("inv_company_email")
    private val KEY_COMPANY_PHONE   = stringPreferencesKey("inv_company_phone")
    private val KEY_COMPANY_WEBSITE = stringPreferencesKey("inv_company_website")
    private val KEY_TAX_NUMBER      = stringPreferencesKey("inv_tax_number")
    private val KEY_BANK_NAME       = stringPreferencesKey("inv_bank_name")
    private val KEY_BANK_ACCOUNT    = stringPreferencesKey("inv_bank_account")
    private val KEY_BANK_IBAN       = stringPreferencesKey("inv_bank_iban")
    private val KEY_CURRENCY        = stringPreferencesKey("inv_currency")
    private val KEY_INVOICE_PREFIX  = stringPreferencesKey("inv_prefix")
    private val KEY_NOTES_DEFAULT   = stringPreferencesKey("inv_notes_default")
    private val KEY_LOGO_PATH       = stringPreferencesKey("inv_logo_path")
    private val KEY_SIG_PATH        = stringPreferencesKey("inv_sig_path")

    // ── Company profile data class ────────────────────────────────────────────
    data class CompanyProfile(
        val companyName: String    = "",
        val companyAddress: String = "",
        val companyEmail: String   = "",
        val companyPhone: String   = "",
        val companyWebsite: String = "",
        val taxNumber: String      = "",
        val bankName: String       = "",
        val bankAccount: String    = "",
        val bankIban: String       = "",
        val currency: String       = "USD",
        val invoicePrefix: String  = "INV",
        val notesDefault: String   = "Thank you for your business.",
        val logoPath: String       = "",
        val signaturePath: String  = ""
    )

    // ── Invoice line item ─────────────────────────────────────────────────────
    data class LineItem(
        val id: String          = java.util.UUID.randomUUID().toString(),
        val description: String = "",
        val quantity: Double    = 1.0,
        val unitPrice: Double   = 0.0
    ) {
        val total: Double get() = quantity * unitPrice
    }

    // ── Full invoice model ────────────────────────────────────────────────────
    data class Invoice(
        val id: String              = java.util.UUID.randomUUID().toString(),
        val invoiceNumber: String   = "",
        val issueDate: String       = "",
        val dueDate: String         = "",
        val clientName: String      = "",
        val clientAddress: String   = "",
        val clientEmail: String     = "",
        val items: List<LineItem>   = emptyList(),
        val taxPercent: Double      = 0.0,
        val discountPercent: Double = 0.0,
        val notes: String           = "",
        val status: String          = "draft",   // draft | sent | paid
        val createdAt: String       = AppStorage.utcNow()
    ) {
        val subtotal: Double get() = items.sumOf { it.total }
        val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
        val taxableAmount: Double get() = subtotal - discountAmount
        val taxAmount: Double get() = taxableAmount * (taxPercent / 100.0)
        val grandTotal: Double get() = taxableAmount + taxAmount
    }

    // ── Profile flow / get / save ─────────────────────────────────────────────
    fun profileFlow(ctx: Context): Flow<CompanyProfile> =
        ctx.invoiceDataStore.data.map { p ->
            CompanyProfile(
                companyName    = p[KEY_COMPANY_NAME]    ?: "",
                companyAddress = p[KEY_COMPANY_ADDRESS] ?: "",
                companyEmail   = p[KEY_COMPANY_EMAIL]   ?: "",
                companyPhone   = p[KEY_COMPANY_PHONE]   ?: "",
                companyWebsite = p[KEY_COMPANY_WEBSITE] ?: "",
                taxNumber      = p[KEY_TAX_NUMBER]      ?: "",
                bankName       = p[KEY_BANK_NAME]       ?: "",
                bankAccount    = p[KEY_BANK_ACCOUNT]    ?: "",
                bankIban       = p[KEY_BANK_IBAN]       ?: "",
                currency       = p[KEY_CURRENCY]        ?: "USD",
                invoicePrefix  = p[KEY_INVOICE_PREFIX]  ?: "INV",
                notesDefault   = p[KEY_NOTES_DEFAULT]   ?: "Thank you for your business.",
                logoPath       = p[KEY_LOGO_PATH]       ?: "",
                signaturePath  = p[KEY_SIG_PATH]        ?: ""
            )
        }

    fun getProfile(ctx: Context): CompanyProfile =
        runBlocking { profileFlow(ctx).first() }

    suspend fun saveProfile(ctx: Context, p: CompanyProfile) {
        ctx.invoiceDataStore.edit { prefs ->
            prefs[KEY_COMPANY_NAME]    = p.companyName
            prefs[KEY_COMPANY_ADDRESS] = p.companyAddress
            prefs[KEY_COMPANY_EMAIL]   = p.companyEmail
            prefs[KEY_COMPANY_PHONE]   = p.companyPhone
            prefs[KEY_COMPANY_WEBSITE] = p.companyWebsite
            prefs[KEY_TAX_NUMBER]      = p.taxNumber
            prefs[KEY_BANK_NAME]       = p.bankName
            prefs[KEY_BANK_ACCOUNT]    = p.bankAccount
            prefs[KEY_BANK_IBAN]       = p.bankIban
            prefs[KEY_CURRENCY]        = p.currency
            prefs[KEY_INVOICE_PREFIX]  = p.invoicePrefix
            prefs[KEY_NOTES_DEFAULT]   = p.notesDefault
            prefs[KEY_LOGO_PATH]       = p.logoPath
            prefs[KEY_SIG_PATH]        = p.signaturePath
        }
    }

    // ── Invoice list persistence (JSON on disk, same pattern as AppStorage) ───
    private fun invoicesFile(): File =
        File(AppStorage.invoiceMakerDataDir(), "invoices.json")

    fun loadInvoices(): List<Invoice> =
        AppStorage.loadJson(invoicesFile(), emptyList<Invoice>())

    fun saveInvoices(list: List<Invoice>) =
        AppStorage.saveJson(invoicesFile(), list)

    fun saveInvoice(inv: Invoice) {
        val list = loadInvoices().toMutableList()
        val idx  = list.indexOfFirst { it.id == inv.id }
        if (idx >= 0) list[idx] = inv else list.add(0, inv)
        saveInvoices(list)
    }

    fun deleteInvoice(id: String) {
        saveInvoices(loadInvoices().filter { it.id != id })
    }

    // ── Next invoice number ───────────────────────────────────────────────────
    fun nextInvoiceNumber(prefix: String): String {
        val existing = loadInvoices()
        val max = existing.mapNotNull { inv ->
            inv.invoiceNumber.removePrefix("${prefix}-").toIntOrNull()
        }.maxOrNull() ?: 0
        return "$prefix-${String.format("%04d", max + 1)}"
    }

    // ── Logo / signature image paths under admin data dir ────────────────────
    fun logoFile(): File    = File(AppStorage.invoiceMakerDataDir(), "invoice_logo.png")
    fun sigFile(): File     = File(AppStorage.invoiceMakerDataDir(), "invoice_signature.png")

    // ── PDF export dir ────────────────────────────────────────────────────────
    fun pdfExportDir(): File =
        File(AppStorage.downloadsDir(), "Invoices").also { it.mkdirs() }
}
