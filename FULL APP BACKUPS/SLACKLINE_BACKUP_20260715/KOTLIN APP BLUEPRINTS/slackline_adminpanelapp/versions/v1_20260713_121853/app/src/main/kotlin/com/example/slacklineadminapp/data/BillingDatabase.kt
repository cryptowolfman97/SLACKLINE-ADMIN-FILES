package com.example.slacklineadminapp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Enums & Domain Models
// ─────────────────────────────────────────────────────────────────────────────
enum class DocumentType { INVOICE, QUOTATION, CREDIT_NOTE, PROFORMA }
enum class ItemType { PRODUCT, SERVICE }
enum class DocStatus { DRAFT, SENT, PAID, CANCELLED }
enum class InvoiceTemplate { MINIMAL, PROFESSIONAL, BOLD }

// ─────────────────────────────────────────────────────────────────────────────
// Company Profile
// ─────────────────────────────────────────────────────────────────────────────
data class CompanyProfile(
    val companyName: String = "SH Vertex Technologies",
    val tagline: String = "",
    val address: String = "Moratuwa, Sri Lanka",
    val email: String = "info@shvertex.online",
    val phone: String = "",
    val website: String = "shvertex.online",
    val taxNumber: String = "",
    val logoPath: String = "",
    val signaturePath: String = "",
    val currencySymbol: String = "LKR",
    val bankName: String = "",
    val bankAccountName: String = "",
    val bankAccountNumber: String = "",
    val bankBranch: String = "",
    val paymentNotes: String = "",
    val defaultTemplate: InvoiceTemplate = InvoiceTemplate.PROFESSIONAL
)

// ─────────────────────────────────────────────────────────────────────────────
// Entities
// ─────────────────────────────────────────────────────────────────────────────
data class ClientEntity(
    val id: Long = 0,
    val name: String,
    val address: String,
    val email: String,
    val phone: String,
    val taxNumber: String
)

data class CatalogItemEntity(
    val id: Long = 0,
    val sku: String,
    val name: String,
    val baseUnitPrice: Double,
    val itemType: ItemType
)

data class DocumentEntity(
    val id: Long = 0,
    val docNumber: String,
    val docType: DocumentType,
    val status: DocStatus,
    val issueDate: String,
    val dueDate: String,
    val clientId: Long?,
    val clientNameCopy: String,
    val clientAddressCopy: String,
    val clientEmailCopy: String,
    val subtotal: Double,
    val taxPercent: Double,
    val taxAmount: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val notes: String,
    val template: InvoiceTemplate? = InvoiceTemplate.PROFESSIONAL
)

data class LineItemEntity(
    val id: Long = 0,
    val documentId: Long,
    val catalogItemId: Long?,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// JSON Store Engine
// ─────────────────────────────────────────────────────────────────────────────
class BillingStore(context: Context) {

    private val gson = Gson()
    private val dir = AppStorage.invoiceMakerDataDir()

    private val clientsFile   = File(dir, "clients.json")
    private val catalogFile   = File(dir, "catalog.json")
    private val documentsFile = File(dir, "documents.json")
    private val lineItemsFile = File(dir, "line_items.json")
    private val counterFile   = File(dir, "id_counter.json")
    private val profileFile   = File(dir, "company_profile.json")

    private data class IdCounter(
        var clients: Long = 0,
        var catalog: Long = 0,
        var documents: Long = 0,
        var lineItems: Long = 0
    )

    private fun loadCounter(): IdCounter =
        if (counterFile.exists())
            gson.fromJson(counterFile.readText(), IdCounter::class.java) ?: IdCounter()
        else IdCounter()

    private fun saveCounter(c: IdCounter) = counterFile.writeText(gson.toJson(c))

    private fun nextClientId():   Long { val c = loadCounter(); c.clients++;   saveCounter(c); return c.clients }
    private fun nextCatalogId():  Long { val c = loadCounter(); c.catalog++;   saveCounter(c); return c.catalog }
    private fun nextDocumentId(): Long { val c = loadCounter(); c.documents++; saveCounter(c); return c.documents }
    private fun nextLineItemId(): Long { val c = loadCounter(); c.lineItems++; saveCounter(c); return c.lineItems }

    private inline fun <reified T> readList(file: File): MutableList<T> {
        if (!file.exists()) return mutableListOf()
        val type = object : TypeToken<MutableList<T>>() {}.type
        return gson.fromJson(file.readText(), type) ?: mutableListOf()
    }

    private fun <T> writeList(file: File, list: List<T>) = file.writeText(gson.toJson(list))

    // Profile
    fun readProfile(): CompanyProfile =
        if (profileFile.exists())
            gson.fromJson(profileFile.readText(), CompanyProfile::class.java) ?: CompanyProfile()
        else CompanyProfile()

    fun saveProfile(profile: CompanyProfile) = profileFile.writeText(gson.toJson(profile))

    // Clients
    fun readClients(): MutableList<ClientEntity> = readList(clientsFile)

    fun insertClient(client: ClientEntity): Long {
        val list = readClients()
        val id = if (client.id == 0L) nextClientId() else client.id
        list.removeAll { it.id == id }
        list.add(client.copy(id = id))
        writeList(clientsFile, list)
        return id
    }

    fun deleteClient(id: Long) {
        val list = readClients(); list.removeAll { it.id == id }; writeList(clientsFile, list)
    }

    fun searchClients(query: String): List<ClientEntity> {
        val q = query.trim('%').replace("%", "")
        return readClients().filter { it.name.contains(q, true) || it.email.contains(q, true) }
    }

    // Catalog
    fun readCatalog(): MutableList<CatalogItemEntity> = readList(catalogFile)

    fun insertCatalogItem(item: CatalogItemEntity): Long {
        val list = readCatalog()
        val id = if (item.id == 0L) nextCatalogId() else item.id
        list.removeAll { it.id == id }
        list.add(item.copy(id = id))
        writeList(catalogFile, list)
        return id
    }

    fun deleteCatalogItem(id: Long) {
        val list = readCatalog(); list.removeAll { it.id == id }; writeList(catalogFile, list)
    }

    fun searchCatalog(query: String): List<CatalogItemEntity> {
        val q = query.trim('%').replace("%", "")
        return readCatalog().filter { it.name.contains(q, true) || it.sku.contains(q, true) }
    }

    // Documents
    fun readDocuments(): MutableList<DocumentEntity> = readList(documentsFile)

    fun insertDocument(doc: DocumentEntity): Long {
        val list = readDocuments()
        val id = if (doc.id == 0L) nextDocumentId() else doc.id
        list.removeAll { it.id == id }
        list.add(doc.copy(id = id))
        list.sortByDescending { it.id }
        writeList(documentsFile, list)
        return id
    }

    fun updateDocument(doc: DocumentEntity) {
        val list = readDocuments()
        val idx = list.indexOfFirst { it.id == doc.id }
        if (idx >= 0) list[idx] = doc
        list.sortByDescending { it.id }
        writeList(documentsFile, list)
    }

    fun deleteDocument(id: Long) {
        val list = readDocuments(); list.removeAll { it.id == id }; writeList(documentsFile, list)
        deleteLineItemsByDocId(id)
    }

    fun getDocumentById(id: Long): DocumentEntity? = readDocuments().firstOrNull { it.id == id }
    fun getDocumentCountByType(type: DocumentType): Int = readDocuments().count { it.docType == type }

    // Line Items
    fun readLineItems(): MutableList<LineItemEntity> = readList(lineItemsFile)

    fun insertLineItems(items: List<LineItemEntity>) {
        val list = readLineItems()
        items.forEach { item ->
            val id = if (item.id == 0L) nextLineItemId() else item.id
            list.removeAll { it.id == id }
            list.add(item.copy(id = id))
        }
        writeList(lineItemsFile, list)
    }

    fun deleteLineItemsByDocId(docId: Long) {
        val list = readLineItems(); list.removeAll { it.documentId == docId }; writeList(lineItemsFile, list)
    }

    fun getLineItemsForDocument(docId: Long): List<LineItemEntity> =
        readLineItems().filter { it.documentId == docId }.sortedBy { it.id }
}

// ─────────────────────────────────────────────────────────────────────────────
// BillingDao
// ─────────────────────────────────────────────────────────────────────────────
class BillingDao(private val store: BillingStore) {

    private val _documentsSignal = MutableStateFlow(0)
    private val _clientsSignal   = MutableStateFlow(0)
    private val _catalogSignal   = MutableStateFlow(0)
    private val _profileSignal   = MutableStateFlow(0)

    private fun bumpDocuments() { _documentsSignal.value++ }
    private fun bumpClients()   { _clientsSignal.value++ }
    private fun bumpCatalog()   { _catalogSignal.value++ }
    private fun bumpProfile()   { _profileSignal.value++ }

    fun getProfileFlow(): Flow<CompanyProfile> =
        _profileSignal.map { store.readProfile() }.flowOn(Dispatchers.IO)

    suspend fun saveProfile(profile: CompanyProfile) = withContext(Dispatchers.IO) {
        store.saveProfile(profile).also { bumpProfile() }
    }

    suspend fun insertDocument(doc: DocumentEntity): Long = withContext(Dispatchers.IO) {
        store.insertDocument(doc).also { bumpDocuments() }
    }

    suspend fun updateDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        store.updateDocument(doc).also { bumpDocuments() }
    }

    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        store.deleteDocument(id).also { bumpDocuments() }
    }

    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>> =
        _documentsSignal.map { store.readDocuments() }.flowOn(Dispatchers.IO)

    suspend fun getDocumentById(id: Long): DocumentEntity? = withContext(Dispatchers.IO) {
        store.getDocumentById(id)
    }

    suspend fun getDocumentCountByType(type: DocumentType): Int = withContext(Dispatchers.IO) {
        store.getDocumentCountByType(type)
    }

    suspend fun insertLineItems(items: List<LineItemEntity>) = withContext(Dispatchers.IO) {
        store.insertLineItems(items).also { bumpDocuments() }
    }

    suspend fun deleteLineItemsByDocId(docId: Long) = withContext(Dispatchers.IO) {
        store.deleteLineItemsByDocId(docId).also { bumpDocuments() }
    }

    suspend fun getLineItemsForDocument(docId: Long): List<LineItemEntity> = withContext(Dispatchers.IO) {
        store.getLineItemsForDocument(docId)
    }

    suspend fun insertClient(client: ClientEntity): Long = withContext(Dispatchers.IO) {
        store.insertClient(client).also { bumpClients() }
    }

    suspend fun deleteClient(id: Long) = withContext(Dispatchers.IO) {
        store.deleteClient(id).also { bumpClients() }
    }

    fun getAllClientsFlow(): Flow<List<ClientEntity>> =
        _clientsSignal.map { store.readClients().sortedBy { it.name } }.flowOn(Dispatchers.IO)

    suspend fun searchClients(query: String): List<ClientEntity> = withContext(Dispatchers.IO) {
        store.searchClients(query)
    }

    suspend fun insertCatalogItem(item: CatalogItemEntity): Long = withContext(Dispatchers.IO) {
        store.insertCatalogItem(item).also { bumpCatalog() }
    }

    suspend fun deleteCatalogItem(id: Long) = withContext(Dispatchers.IO) {
        store.deleteCatalogItem(id).also { bumpCatalog() }
    }

    fun getAllCatalogItemsFlow(): Flow<List<CatalogItemEntity>> =
        _catalogSignal.map { store.readCatalog().sortedBy { it.name } }.flowOn(Dispatchers.IO)

    suspend fun searchCatalogItems(query: String): List<CatalogItemEntity> = withContext(Dispatchers.IO) {
        store.searchCatalog(query)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppDatabase singleton
// ─────────────────────────────────────────────────────────────────────────────
class AppDatabase private constructor(context: Context) {
    private val store = BillingStore(context)
    val dao = BillingDao(store)
    fun billingDao(): BillingDao = dao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
    }
}
