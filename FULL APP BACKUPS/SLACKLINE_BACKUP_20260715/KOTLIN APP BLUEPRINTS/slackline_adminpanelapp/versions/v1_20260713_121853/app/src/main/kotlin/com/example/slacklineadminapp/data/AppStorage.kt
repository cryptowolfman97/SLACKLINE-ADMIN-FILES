package com.example.slacklineadminapp.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Data model holding payment parameters for custom preset persistence
data class PaymentPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val isDefault: Boolean = false,
    val bankName: String = "",
    val bankAccName: String = "",
    val bankAccount: String = "",
    val bankBranch: String = "",
    val bankSwift: String = "",
    val cryptoUsdtBsc: String = "",
    val cryptoUsdtTrc: String = "",
    val cryptoUsdtPlasma: String = "",
    val cryptoEth: String = "",
    val cryptoLtc: String = ""
)

object AppStorage {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Compact Gson used ONLY for canonical signing — no pretty printing
    // so output matches Python's json.dumps(sort_keys=True, separators=(',',':'))
    private val compactGson: Gson = GsonBuilder().create()

    fun downloadsDir(): File =
        File(
            Environment.getExternalStorageDirectory(),
            "SLACKLINE ADMIN FILES"
        ).also { it.mkdirs() }

    // ── SINGLE SOURCE OF TRUTH ──────────────────────────────────────────────
    // Every module's save location is defined ONCE here, as a named folder
    // directly under "SLACKLINE ADMIN FILES". No other file in the app should
    // ever build its own path — it should call one of the functions below (or
    // a file-specific helper further down) so that any future restructuring
    // only ever requires editing this file.
    //
    // Folder names are ALL CAPS and must match the module names exactly.
    // Where two modules physically share the same saved data, the folder is
    // named as a "MODULE A & MODULE B" combo rather than being split apart.

    fun kotlinAppsManagerDir(): File =
        File(downloadsDir(), "KOTLIN APPS MANAGER").also { it.mkdirs() }

    fun legacyAndNewLicenseManagerDir(): File =
        File(downloadsDir(), "LEGACY LICENSE MANAGER & NEW LICENSE MANAGER").also { it.mkdirs() }

    fun kotlinAppInjectorDir(): File =
        File(downloadsDir(), "KOTLIN APP INJECTOR").also { it.mkdirs() }

    fun kotlinAppGeneratorDir(): File =
        File(downloadsDir(), "KOTLIN APP GENERATOR").also { it.mkdirs() }

    fun pythonKivyLicenseToolsDir(): File =
        File(downloadsDir(), "PYTHON-KIVY LICENSE TOOLS").also { it.mkdirs() }

    fun githubAdminDir(): File =
        File(downloadsDir(), "GITHUB ADMIN").also { it.mkdirs() }

    fun supaStudioDir(): File =
        File(downloadsDir(), "SUPA STUDIO").also { it.mkdirs() }

    fun invoiceMakerDir(): File =
        File(downloadsDir(), "INVOICE MAKER").also { it.mkdirs() }

    fun websitesRegistryDir(): File =
        File(downloadsDir(), "WEBSITES REGISTRY").also { it.mkdirs() }

    fun kotlinAppBlueprintsDir(): File =
        File(downloadsDir(), "KOTLIN APP BLUEPRINTS").also { it.mkdirs() }

    fun pythonKivyAppBlueprintsDir(): File =
        File(downloadsDir(), "PYTHON-KIVY APP BLUEPRINTS").also { it.mkdirs() }

    fun projectManagementDir(): File =
        File(downloadsDir(), "PROJECT MANAGEMENT").also { it.mkdirs() }

    fun documentsAndGuidesDir(): File =
        File(downloadsDir(), "DOCUMENTS & GUIDES").also { it.mkdirs() }

    fun workflowGuidesDir(): File =
        File(downloadsDir(), "WORKFLOW GUIDES").also { it.mkdirs() }

    fun kotlinLicenseToolsDir(): File =
        File(downloadsDir(), "KOTLIN LICENSE TOOLS").also { it.mkdirs() }

    fun activityLogDir(): File =
        File(downloadsDir(), "ACTIVITY LOG").also { it.mkdirs() }

    fun cloudSettingsDir(): File =
        File(downloadsDir(), "CLOUD SETTINGS").also { it.mkdirs() }

    // ── Recycle Bin ──────────────────────────────────────────────────────────
    // Currently only used by the App Backup/Restore feature (Full Replace
    // restores move overwritten local files here instead of deleting them).
    // Planned to expand to app-wide delete actions later.
    fun recycleBinDir(): File =
        File(downloadsDir(), "RECYCLE BIN").also { it.mkdirs() }

    fun recycleBinModuleDir(moduleLabel: String): File =
        File(recycleBinDir(), moduleLabel).also { it.mkdirs() }

    /** Moves a file into the recycle bin, timestamping it so same-name files never collide. */
    fun moveToRecycleBin(moduleLabel: String, file: File) {
        if (!file.exists() || !file.isFile) return
        val dest = File(recycleBinModuleDir(moduleLabel), "${timestamp()}_${file.name}")
        try {
            file.copyTo(dest, overwrite = true)
            file.delete()
        } catch (_: Exception) { /* best-effort — never block a restore on recycle bin failure */ }
    }

    // ── Backup module registry ──────────────────────────────────────────────
    // Single list describing every module that has local data to back up.
    // Used by App Backup/Restore to build the module checklist and to walk
    // each module's folder. Add a new module here and it's automatically
    // picked up everywhere backup/restore is used.
    data class BackupModule(val label: String, val dir: () -> File)

    val BACKUP_MODULES: List<BackupModule> = listOf(
        BackupModule("KOTLIN APPS MANAGER", ::kotlinAppsManagerDir),
        BackupModule("LEGACY LICENSE MANAGER & NEW LICENSE MANAGER", ::legacyAndNewLicenseManagerDir),
        BackupModule("KOTLIN APP INJECTOR", ::kotlinAppInjectorDir),
        BackupModule("KOTLIN APP GENERATOR", ::kotlinAppGeneratorDir),
        BackupModule("PYTHON-KIVY LICENSE TOOLS", ::pythonKivyLicenseToolsDir),
        BackupModule("GITHUB ADMIN", ::githubAdminDir),
        BackupModule("SUPA STUDIO", ::supaStudioDir),
        BackupModule("INVOICE MAKER", ::invoiceMakerDir),
        BackupModule("WEBSITES REGISTRY", ::websitesRegistryDir),
        BackupModule("KOTLIN APP BLUEPRINTS", ::kotlinAppBlueprintsDir),
        BackupModule("PYTHON-KIVY APP BLUEPRINTS", ::pythonKivyAppBlueprintsDir),
        BackupModule("PROJECT MANAGEMENT", ::projectManagementDir),
        BackupModule("DOCUMENTS & GUIDES", ::documentsAndGuidesDir),
        BackupModule("WORKFLOW GUIDES", ::workflowGuidesDir),
        BackupModule("KOTLIN LICENSE TOOLS", ::kotlinLicenseToolsDir),
        BackupModule("ACTIVITY LOG", ::activityLogDir),
        BackupModule("CLOUD SETTINGS", ::cloudSettingsDir)
    )

    fun backupModuleDir(label: String): File? = BACKUP_MODULES.find { it.label == label }?.dir?.invoke()

    // ── Aliases ──────────────────────────────────────────────────────────────
    // These keep their original names because call sites already reference
    // AppStorage by these names (nothing to change outside this file) — only
    // their body/destination changes here.
    fun generatedAppsDir(): File = kotlinAppGeneratorDir()
    fun supabaseDataDir(): File = supaStudioDir()
    fun kotlinLicensesDir(): File = kotlinAppsManagerDir()
    fun accountAndInvoicesDir(): File = invoiceMakerDir()

    // ── File-specific helpers ───────────────────────────────────────────────
    // Where a module saves one specific file (or a clearly-named subfolder),
    // it's defined here so calling code never has to know the filename either.

    fun activityLogFile(): File = File(activityLogDir(), "activity_log.json")

    fun cloudSettingsFile(): File = File(cloudSettingsDir(), "cloud_settings.json")

    fun websitesFile(): File = File(websitesRegistryDir(), "websites.json")
    fun websitesExportDir(): File = File(websitesRegistryDir(), "Web_Exports").also { it.mkdirs() }

    fun documentsFile(): File = File(documentsAndGuidesDir(), "documents_db.json")
    fun documentsExportDir(): File = File(documentsAndGuidesDir(), "Notes_and_Docs").also { it.mkdirs() }

    fun workflowGuideFile(): File = File(workflowGuidesDir(), "workflow_guide.json")

    fun appBlueprintsFile(): File = File(pythonKivyAppBlueprintsDir(), "app_blueprints.json")
    fun appBlueprintsExportDir(): File =
        File(pythonKivyAppBlueprintsDir(), "Exports").also { it.mkdirs() }

    fun kotlinAppBlueprintsExportDir(): File =
        File(kotlinAppBlueprintsDir(), "Exports").also { it.mkdirs() }

    fun githubAccountsFile(): File = File(githubAdminDir(), "github_accounts.json")
    fun githubDownloadsDir(): File = File(githubAdminDir(), "Downloads").also { it.mkdirs() }

    // Legacy License Manager + New License Manager share this combo folder.
    fun productDir(id: String): File =
        File(legacyAndNewLicenseManagerDir(), id).also { it.mkdirs() }

    fun exportDir(vararg parts: String): File {
        var f = File(legacyAndNewLicenseManagerDir(), "License Backups")
        parts.forEach { p -> f = File(f, p) }
        return f.also { it.mkdirs() }
    }

    fun legacyProductsRegistryFile(): File =
        File(legacyAndNewLicenseManagerDir(), "products_registry.json")

    fun newProductsRegistryFile(): File =
        File(legacyAndNewLicenseManagerDir(), "new_products_registry.json")

    fun newLicensesDir(productId: String): File =
        File(legacyAndNewLicenseManagerDir(), "new_licenses/$productId").also { it.mkdirs() }

    fun newRevocationsFile(): File =
        File(legacyAndNewLicenseManagerDir(), "new_revocations.json")

    fun injectedOutputsDir(): File =
        File(kotlinAppInjectorDir(), "Injected_Outputs").also { it.mkdirs() }

    fun injectedAppsDir(): File = pythonKivyLicenseToolsDir()

    fun kotlinLicenseOutputDir(): File = kotlinLicenseToolsDir()

    // Invoice Maker: PDFs saved directly in the module folder, the module's
    // own JSON "database" (clients, catalog, documents, etc.) in a subfolder.
    fun invoiceMakerDataDir(): File =
        File(invoiceMakerDir(), "Data").also { it.mkdirs() }

    /** Logo/signature images for Invoice Maker — copied here on pick so they travel with backups. */
    fun invoiceBrandingDir(): File =
        File(invoiceMakerDataDir(), "Branding").also { it.mkdirs() }

    /** Logo / signature images live here — kept separate from the JSON "Data" folder for clarity. */
    fun invoiceMakerBrandingDir(): File =
        File(invoiceMakerDir(), "Branding").also { it.mkdirs() }

    /**
     * Copies a picked gallery/file image into Invoice Maker's own Branding folder as
     * a fixed filename (so the invoice PDF engine always finds it at a stable path,
     * and it's included automatically in App Backup). Returns the new absolute path,
     * or null if the copy failed.
     */
    fun importBrandingImage(context: Context, uri: Uri, fixedFileName: String): String? {
        return try {
            val ext = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "png"
            val dest = File(invoiceMakerBrandingDir(), "$fixedFileName.$ext")
            // Clear any previous version under a different extension first.
            invoiceMakerBrandingDir().listFiles()
                ?.filter { it.name.startsWith("$fixedFileName.") }
                ?.forEach { it.delete() }
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> loadJson(file: File, default: T): T = try {
        if (!file.exists()) default
        else gson.fromJson(file.readText(Charsets.UTF_8), object : TypeToken<T>() {}.type) ?: default
    } catch (e: Exception) { default }

    fun saveJson(file: File, data: Any) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(data), Charsets.UTF_8)
    }

    fun canonicalJson(data: Any): ByteArray =
        compactGson.toJson(sortKeys(data)).toByteArray(Charsets.UTF_8)

    private fun sortKeys(obj: Any?): Any? = when (obj) {
        is Map<*, *> -> obj.entries
            .sortedBy { it.key.toString() }
            .associate { it.key.toString() to sortKeys(it.value) }
        is List<*>   -> obj.map { sortKeys(it) }
        else         -> obj
    }

    fun logActivity(action: String, details: String, product: String) {
        val file = activityLogFile()
        val logs = loadJson<MutableList<Map<String, String>>>(file, mutableListOf())
        logs.add(0, mapOf("time" to utcNow(), "action" to action, "details" to details, "product" to product))
        saveJson(file, logs.take(500))
    }

    fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    // ── Payment Preset Data Operations ────────────────────────────────────────
    // Only used by the Kotlin App Injector module.
    private fun getPresetsFile(): File {
        val presetsDir = File(kotlinAppInjectorDir(), "Payment Presets")
        if (!presetsDir.exists()) presetsDir.mkdirs()
        return File(presetsDir, "payment_presets.json")
    }

    fun loadPaymentPresets(): List<PaymentPreset> {
        return loadJson<List<PaymentPreset>>(getPresetsFile(), emptyList())
    }

    fun savePaymentPresets(presets: List<PaymentPreset>) {
        saveJson(getPresetsFile(), presets)
    }
}
