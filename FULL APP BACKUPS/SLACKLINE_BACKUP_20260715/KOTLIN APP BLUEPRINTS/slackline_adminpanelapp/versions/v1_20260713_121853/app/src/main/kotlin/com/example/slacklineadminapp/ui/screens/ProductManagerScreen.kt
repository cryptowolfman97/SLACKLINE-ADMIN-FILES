package com.example.slacklineadminapp.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.data.CloudPresetsStore
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class ProductManagerViewModel(private val productId: String) : ViewModel() {

    enum class Tab { DASHBOARD, AUTHORITY, GENERATE, LICENSES, REVOCATIONS, BACKUPS }

    private val _tab      = MutableStateFlow(Tab.DASHBOARD)
    val tab: StateFlow<Tab> = _tab

    private val _config   = MutableStateFlow<ProductConfig?>(null)
    val config: StateFlow<ProductConfig?> = _config

    private val _records  = MutableStateFlow<List<LicenseRecord>>(emptyList())
    val records: StateFlow<List<LicenseRecord>> = _records

    private val _loading  = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _toast    = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _lastCode = MutableStateFlow("")
    val lastCode: StateFlow<String> = _lastCode

    private val _lastLid  = MutableStateFlow("")
    val lastLid: StateFlow<String> = _lastLid

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating

    // Persistent status message shown in Generate tab (errors + success)
    private val _genStatus = MutableStateFlow("")
    val genStatus: StateFlow<String> = _genStatus

    private val _genIsError = MutableStateFlow(false)
    val genIsError: StateFlow<Boolean> = _genIsError

    private val _backupText = MutableStateFlow("")
    val backupText: StateFlow<String> = _backupText

    private val _revoText = MutableStateFlow("")
    val revoText: StateFlow<String> = _revoText

    private val _githubFiles = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val githubFiles: StateFlow<List<Map<String, String>>> = _githubFiles

    // Generate form
    var genTier       by mutableStateOf("pro")
    var genSource     by mutableStateOf("crypto")
    var genDevice     by mutableStateOf("")
    var genCustomer   by mutableStateOf("")
    var genEmail      by mutableStateOf("")
    var genLabel      by mutableStateOf("")
    var genNote       by mutableStateOf("")
    var genExpiry     by mutableStateOf("")

    // Search / filter
    var searchQuery   by mutableStateOf("")
    var filterStatus  by mutableStateOf("all")    // all | active | revoked
    var filterSort    by mutableStateOf("newest") // newest | oldest | tier | status

    // Authority tab fields
    var authBackupPw   by mutableStateOf("")
    var allBackupsPw   by mutableStateOf("")
    var importPasteBlob by mutableStateOf("")
    var importPastePw  by mutableStateOf("")

    // GitHub upload backup fields
    var ghUpOwner   by mutableStateOf("")
    var ghUpRepo    by mutableStateOf("")
    var ghUpBranch  by mutableStateOf("main")
    var ghUpFolder  by mutableStateOf("Backups")
    var ghUpToken   by mutableStateOf("")

    // GitHub import backup fields
    var ghImOwner   by mutableStateOf("")
    var ghImRepo    by mutableStateOf("")
    var ghImBranch  by mutableStateOf("main")
    var ghImFolder  by mutableStateOf("Backups")
    var ghImToken   by mutableStateOf("")
    var ghImPw      by mutableStateOf("")

    // Revocation tab GitHub fields
    var ghRvOwner   by mutableStateOf("")
    var ghRvRepo    by mutableStateOf("")
    var ghRvBranch  by mutableStateOf("main")
    var ghRvPath    by mutableStateOf("")
    var ghRvToken   by mutableStateOf("")

    // Editable bundle_app
    var bundleAppEdit by mutableStateOf("")

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        _config.value = ProductRegistry.get(productId)
        _config.value?.let { cfg ->
            _records.value = EngineCache.get(cfg).loadRecords()
            bundleAppEdit = cfg.bundleApp
            ghRvOwner  = cfg.githubOwner
            ghRvRepo   = cfg.githubRepo
            ghRvBranch = cfg.githubBranch.ifBlank { "main" }
            ghRvPath   = cfg.githubPath
            // Pre-fill GitHub upload/import with product's GitHub config
            ghUpOwner  = cfg.githubOwner
            ghUpRepo   = cfg.githubRepo
            ghUpBranch = cfg.githubBranch.ifBlank { "main" }
            ghImOwner  = cfg.githubOwner
            ghImRepo   = cfg.githubRepo
            ghImBranch = cfg.githubBranch.ifBlank { "main" }
        }
    }

    fun switchTab(t: Tab)  { _tab.value = t }
    fun consumeToast()     { _toast.value = "" }

    fun reload() = viewModelScope.launch(Dispatchers.IO) {
        _config.value?.let { cfg ->
            _records.value = EngineCache.get(cfg).loadRecords()
        }
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────

    fun saveBundleApp() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        if (bundleAppEdit.isBlank()) { _toast.value = "bundle_app cannot be empty."; return@launch }
        val updated = cfg.copy(bundleApp = bundleAppEdit.trim())
        ProductRegistry.add(updated)
        _config.value = updated
        EngineCache.invalidate(cfg.id)
        _toast.value = "bundle_app updated to ${bundleAppEdit.trim()}"
    }

    // ── Authority ──────────────────────────────────────────────────────────────

    fun initAuthority() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        _loading.value = true
        try {
            EngineCache.get(cfg).initializeAuthority()
            load()
            _toast.value = "Authority initialized! Copy the Public Key PEM and embed it in your app."
        } catch (e: Exception) { _toast.value = e.message ?: "Init failed." }
        _loading.value = false
    }

    fun removeAuthority() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        EngineCache.get(cfg).removeLocalAuthority()
        EngineCache.invalidate(cfg.id)
        load()
        _toast.value = "Local authority removed."
    }

    fun generateSingleBackup(kind: String) = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        if (!eng.hasAuthority()) { _toast.value = "No authority loaded."; return@launch }
        if (authBackupPw.isBlank()) { _toast.value = "Backup password required."; return@launch }
        try {
            val blob = when (kind) {
                "auth" -> eng.buildBackupBlob(authBackupPw, "authority_only",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText()))
                "list" -> eng.buildBackupBlob(authBackupPw, "license_list_only", eng.loadRecords())
                else   -> eng.buildBackupBlob(authBackupPw, "full_backup",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                          "licenses" to eng.loadRecords()))
            }
            _backupText.value = blob
            _toast.value = "Backup generated. Tap Copy or Save."
        } catch (e: Exception) { _toast.value = "Backup failed: ${e.message}" }
    }

    fun saveBackupFile(kind: String) = viewModelScope.launch(Dispatchers.IO) {
        val cfg  = _config.value ?: return@launch
        val eng  = EngineCache.get(cfg)
        val blob = _backupText.value
        if (blob.isBlank()) { _toast.value = "Generate a backup first."; return@launch }
        try {
            when (kind) {
                "auth" -> eng.saveAuthBackupFile(blob)
                "list" -> eng.saveListBackupFile(blob)
                else   -> eng.saveFullBackupFile(blob)
            }
            _toast.value = "Backup saved to License_Backups/"
        } catch (e: Exception) { _toast.value = "Save failed: ${e.message}" }
    }

    fun exportAllBackups() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        if (!eng.hasAuthority()) { _toast.value = "No authority loaded."; return@launch }
        if (allBackupsPw.isBlank()) { _toast.value = "Password required."; return@launch }
        _loading.value = true
        try {
            val auth = eng.buildBackupBlob(allBackupsPw, "authority_only",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText()))
            val list = eng.buildBackupBlob(allBackupsPw, "license_list_only", eng.loadRecords())
            val full = eng.buildBackupBlob(allBackupsPw, "full_backup",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                      "licenses" to eng.loadRecords()))
            eng.saveAuthBackupFile(auth)
            eng.saveListBackupFile(list)
            eng.saveFullBackupFile(full)
            _toast.value = "All 3 backups saved for ${cfg.displayName}"
        } catch (e: Exception) { _toast.value = "Export failed: ${e.message}" }
        _loading.value = false
    }

    fun uploadBackupsToGitHub() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        if (!eng.hasAuthority()) { _toast.value = "No authority loaded."; return@launch }
        if (allBackupsPw.isBlank()) { _toast.value = "Enter password in Export All Backups first."; return@launch }
        if (ghUpOwner.isBlank() || ghUpRepo.isBlank() || ghUpToken.isBlank()) {
            _toast.value = "GitHub owner, repo, and token are required."; return@launch
        }
        _loading.value = true
        try {
            val auth = eng.buildBackupBlob(allBackupsPw, "authority_only",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText()))
            val list = eng.buildBackupBlob(allBackupsPw, "license_list_only", eng.loadRecords())
            val full = eng.buildBackupBlob(allBackupsPw, "full_backup",
                mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                      "licenses" to eng.loadRecords()))
            uploadFileToGitHub(ghUpOwner, ghUpRepo, ghUpBranch.ifBlank{"main"}, ghUpFolder,
                "${cfg.id}_authority_${AppStorage.timestamp()}.ctp", auth, ghUpToken)
            uploadFileToGitHub(ghUpOwner, ghUpRepo, ghUpBranch.ifBlank{"main"}, ghUpFolder,
                "${cfg.id}_list_${AppStorage.timestamp()}.ctlist", list, ghUpToken)
            uploadFileToGitHub(ghUpOwner, ghUpRepo, ghUpBranch.ifBlank{"main"}, ghUpFolder,
                "${cfg.id}_full_${AppStorage.timestamp()}.ctfull", full, ghUpToken)
            _toast.value = "All 3 backups uploaded to GitHub!"
        } catch (e: Exception) { _toast.value = "Upload failed: ${e.message}" }
        _loading.value = false
    }

    fun listGitHubBackupFiles() = viewModelScope.launch(Dispatchers.IO) {
        if (ghImOwner.isBlank() || ghImRepo.isBlank() || ghImToken.isBlank() || ghImFolder.isBlank()) {
            _toast.value = "Owner, repo, folder, and token required."; return@launch
        }
        _loading.value = true
        try {
            val folder = ghImFolder.trim().trim('/')
            val apiUrl = "https://api.github.com/repos/$ghImOwner/$ghImRepo/contents/$folder?ref=${ghImBranch.ifBlank{"main"}}"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).also {
                it.setRequestProperty("Accept", "application/vnd.github+json")
                it.setRequestProperty("Authorization", "Bearer $ghImToken")
                it.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                it.connectTimeout = 15000; it.readTimeout = 15000
            }
            val body = conn.inputStream.bufferedReader().readText()
            val arr  = JSONArray(body)
            val files = mutableListOf<Map<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name")
                if (name.endsWith(".ctp") || name.endsWith(".ctfull")) {
                    files.add(mapOf(
                        "name" to name,
                        "path" to obj.optString("path"),
                        "size" to obj.optInt("size").toString()
                    ))
                }
            }
            _githubFiles.value = files
            if (files.isEmpty()) _toast.value = "No .ctp or .ctfull files found in that folder."
        } catch (e: Exception) { _toast.value = "List failed: ${e.message}" }
        _loading.value = false
    }

    fun importBackupFromGitHub(filePath: String) = viewModelScope.launch(Dispatchers.IO) {
        if (ghImPw.isBlank()) { _toast.value = "Enter the backup password to decrypt."; return@launch }
        val cfg = _config.value ?: return@launch
        _loading.value = true
        try {
            val apiUrl = "https://api.github.com/repos/$ghImOwner/$ghImRepo/contents/$filePath?ref=${ghImBranch.ifBlank{"main"}}"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).also {
                it.setRequestProperty("Accept", "application/vnd.github+json")
                it.setRequestProperty("Authorization", "Bearer $ghImToken")
                it.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                it.connectTimeout = 15000; it.readTimeout = 15000
            }
            val meta = JSONObject(conn.inputStream.bufferedReader().readText())
            val blob = String(Base64.decode(meta.getString("content").replace("\n",""), Base64.DEFAULT))
            applyImportedBackup(cfg, blob, ghImPw)
        } catch (e: Exception) { _toast.value = "GitHub import failed: ${e.message}" }
        _loading.value = false
    }

    fun importBackupFromPaste() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        if (importPasteBlob.isBlank()) { _toast.value = "Paste a backup first."; return@launch }
        _loading.value = true
        try { applyImportedBackup(cfg, importPasteBlob, importPastePw) }
        catch (e: Exception) { _toast.value = "Import failed: ${e.message}" }
        _loading.value = false
    }

    private fun applyImportedBackup(cfg: ProductConfig, blob: String, pw: String) {
        val json   = JSONObject(blob)
        val schema = json.optInt("schema", 1)
        if (schema != 2) throw RuntimeException("Unsupported backup schema $schema.")
        val bundleType = json.optString("bundle_type")
        val salt   = Base64.decode(json.getString("salt"), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val ct     = Base64.decode(json.getString("ciphertext"), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val mac    = Base64.decode(json.getString("mac"), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        // PBKDF2 + XOR - matches LicenseEngine.buildBackupBlob
        val keySpec = javax.crypto.spec.PBEKeySpec(pw.toCharArray(), salt, 200_000, 256)
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
        val expectedMac = javax.crypto.Mac.getInstance("HmacSHA256")
            .also { it.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")) }.doFinal(salt + ct)
        if (!java.security.MessageDigest.isEqual(mac, expectedMac))
            throw RuntimeException("Wrong password or corrupted backup.")

        // XOR decrypt
        val plain = xorDecrypt(key, ct)
        val payload = JSONObject(plain).optJSONObject("payload") ?: JSONObject()

        val eng = EngineCache.get(cfg)
        val parts = mutableListOf<String>()

        if (bundleType == "authority_only" || bundleType == "full_backup") {
            val priv = payload.optString("private_key_pem").trim()
            val pub  = payload.optString("public_key_pem").trim()
            if (priv.isBlank() || pub.isBlank()) throw RuntimeException("No keypair in backup.")
            eng.privPath().writeText(priv)
            eng.pubPath().writeText(pub)
            parts.add("Authority keys restored.")
        }
        if (bundleType == "license_list_only" || bundleType == "full_backup") {
            val arr = payload.optJSONArray("licenses")
            if (arr != null) {
                val recs = (0 until arr.length()).map { AppStorage.gson.fromJson(arr.getJSONObject(it).toString(), LicenseRecord::class.java) }
                eng.saveRecords(recs)
                parts.add("${recs.size} licenses restored.")
            }
        }
        EngineCache.invalidate(cfg.id)
        load()
        _toast.value = parts.joinToString(" ").ifBlank { "Backup applied (${bundleType})." }
    }

    private fun xorDecrypt(key: ByteArray, ct: ByteArray): String {
        val out = ByteArray(ct.size)
        var offset = 0; var ctr = 0
        while (offset < ct.size) {
            val ctrBytes = byteArrayOf((ctr shr 24).toByte(),(ctr shr 16).toByte(),(ctr shr 8).toByte(),ctr.toByte())
            val block = java.security.MessageDigest.getInstance("SHA-256").digest(key + ctrBytes)
            val take  = minOf(block.size, ct.size - offset)
            for (i in 0 until take) out[offset + i] = (ct[offset + i].toInt() xor block[i].toInt()).toByte()
            offset += take; ctr++
        }
        return String(out, Charsets.UTF_8)
    }

    private fun uploadFileToGitHub(owner: String, repo: String, branch: String, folder: String,
                                    filename: String, content: String, token: String) {
        val path = "${folder.trim('/')}/$filename"
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val body = JSONObject(mapOf("message" to "Upload $filename", "content" to encoded, "branch" to branch))
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).also {
            it.requestMethod = "PUT"
            it.setRequestProperty("Accept", "application/vnd.github+json")
            it.setRequestProperty("Authorization", "Bearer $token")
            it.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            it.setRequestProperty("Content-Type", "application/json")
            it.doOutput = true
            it.connectTimeout = 30000; it.readTimeout = 30000
        }
        conn.outputStream.bufferedWriter().use { it.write(body.toString()) }
        val code = conn.responseCode
        if (code !in 200..201) throw RuntimeException("GitHub upload failed ($code)")
    }

    // ── Generate ───────────────────────────────────────────────────────────────

    fun generateLicense() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        _generating.value = true
        _genStatus.value  = ""
        try {
            val rec = eng.generate(
                tier = genTier, source = genSource, deviceCode = genDevice,
                customerName = genCustomer, customerEmail = genEmail,
                label = genLabel, note = genNote, expiry = genExpiry
            )
            _lastCode.value   = rec.activationCode
            _lastLid.value    = rec.licenseId
            _records.value    = eng.loadRecords()
            _genStatus.value  = "License generated: ${rec.licenseId}"
            _genIsError.value = false
        } catch (e: Exception) {
            _genStatus.value  = e.message ?: "Generation failed."
            _genIsError.value = true
        }
        _generating.value = false
    }

    fun clearGenForm() {
        genDevice = ""; genCustomer = ""; genEmail = ""; genLabel = ""; genNote = ""; genExpiry = ""
        _lastCode.value = ""; _lastLid.value = ""
    }

    // ── Licenses ───────────────────────────────────────────────────────────────

    fun filteredRecords(): List<LicenseRecord> {
        val q = searchQuery.lowercase()
        val recs = _records.value
        var out = recs.filter { r ->
            (filterStatus == "all" || r.status == filterStatus) &&
            (q.isBlank() || listOf(r.licenseId, r.deviceCode, r.customerName, r.customerEmail, r.label, r.tier)
                .joinToString(" ").lowercase().contains(q))
        }
        out = when (filterSort) {
            "oldest" -> out.sortedBy { it.issuedAt }
            "tier"   -> out.sortedByDescending { it.tier }
            "status" -> out.sortedBy { it.status }
            else     -> out.sortedByDescending { it.issuedAt }
        }
        return out
    }

    fun toggleRevoke(lid: String) = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val result = EngineCache.get(cfg).toggleRevoke(lid)
        reload()
        _toast.value = "$lid is now ${result.uppercase()}."
    }

    fun deleteLicense(lid: String) = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        EngineCache.get(cfg).deleteLicense(lid)
        reload()
        _toast.value = "$lid deleted."
    }

    fun exportCsv() = viewModelScope.launch(Dispatchers.IO) {
        val cfg  = _config.value ?: return@launch
        val recs = filteredRecords()
        if (recs.isEmpty()) { _toast.value = "No records to export."; return@launch }
        val dir  = AppStorage.exportDir(cfg.displayName, "License Exports").also { it.mkdirs() }
        val file = File(dir, "${cfg.id}_export_${AppStorage.timestamp()}.csv")
        val header = "license_id,tier,status,source,customer_name,customer_email,device_code,label,note,issued_at,expiry,revoked_at"
        file.writeText(header + "\n" + recs.joinToString("\n") { r ->
            listOf(r.licenseId, r.tier, r.status, r.source, r.customerName, r.customerEmail,
                   r.deviceCode, r.label, r.customerNote, r.issuedAt, r.expiry, r.revokedAt)
                .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        })
        _toast.value = "Exported ${recs.size} records to ${file.name}"
    }

    // ── Revocations ────────────────────────────────────────────────────────────

    fun generateRevocation() = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        if (!eng.hasAuthority()) { _toast.value = "No authority loaded."; return@launch }
        try {
            val revoked = eng.loadRecords().filter { it.status == "revoked" }.map { it.licenseId }
            val payload = mapOf("app" to cfg.bundleApp, "version" to 1,
                "updated_at" to AppStorage.utcNow(), "revoked_ids" to revoked)
            _revoText.value = AppStorage.gson.toJson(payload)
            _toast.value = "Revocation JSON generated (${revoked.size} IDs)."
        } catch (e: Exception) { _toast.value = "Failed: ${e.message}" }
    }

    fun saveRevocationFile() = viewModelScope.launch(Dispatchers.IO) {
        val cfg  = _config.value ?: return@launch
        val text = _revoText.value
        if (text.isBlank()) { _toast.value = "Generate revocation first."; return@launch }
        val dir  = AppStorage.exportDir(cfg.displayName, "Revocation Jsons").also { it.mkdirs() }
        val file = File(dir, "${cfg.id}_revo_${AppStorage.timestamp()}.json")
        file.writeText(text)
        _toast.value = "Saved: ${file.name}"
    }

    fun uploadRevocationToGitHub() = viewModelScope.launch(Dispatchers.IO) {
        val cfg  = _config.value ?: return@launch
        val text = _revoText.value
        if (text.isBlank()) { _toast.value = "Generate revocation JSON first."; return@launch }
        if (ghRvOwner.isBlank() || ghRvRepo.isBlank() || ghRvPath.isBlank() || ghRvToken.isBlank()) {
            _toast.value = "Owner, repo, path, and token required."; return@launch
        }
        _loading.value = true
        try {
            val apiUrl = "https://api.github.com/repos/$ghRvOwner/$ghRvRepo/contents/${ghRvPath.trim('/')}"
            var sha = ""
            runCatching {
                val get = (URL(apiUrl).openConnection() as HttpURLConnection).also {
                    it.setRequestProperty("Authorization", "Bearer $ghRvToken")
                    it.setRequestProperty("Accept", "application/vnd.github+json")
                    it.connectTimeout = 15000; it.readTimeout = 15000
                }
                if (get.responseCode == 200)
                    sha = JSONObject(get.inputStream.bufferedReader().readText()).optString("sha")
            }
            val bodyMap = mutableMapOf(
                "message" to "Update ${cfg.displayName} revocations",
                "content" to Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
                "branch"  to ghRvBranch.ifBlank { "main" }
            )
            if (sha.isNotBlank()) bodyMap["sha"] = sha
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).also {
                it.requestMethod = "PUT"
                it.setRequestProperty("Authorization", "Bearer $ghRvToken")
                it.setRequestProperty("Accept", "application/vnd.github+json")
                it.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                it.setRequestProperty("Content-Type", "application/json")
                it.doOutput = true; it.connectTimeout = 30000; it.readTimeout = 30000
            }
            conn.outputStream.bufferedWriter().use { it.write(JSONObject(bodyMap as Map<*, *>).toString()) }
            val code = conn.responseCode
            if (code in 200..201) {
                // Save updated github config back to registry
                val updated = cfg.copy(githubOwner = ghRvOwner, githubRepo = ghRvRepo,
                    githubBranch = ghRvBranch.ifBlank{"main"}, githubPath = ghRvPath)
                ProductRegistry.add(updated)
                _config.value = updated
                _toast.value = "Revocation uploaded to GitHub!"
            } else {
                _toast.value = "GitHub upload failed ($code)"
            }
        } catch (e: Exception) { _toast.value = "Upload failed: ${e.message}" }
        _loading.value = false
    }

    // ── Backups tab ────────────────────────────────────────────────────────────

    var bkPw by mutableStateOf("")

    fun exportBackup(bundleType: String) = viewModelScope.launch(Dispatchers.IO) {
        val cfg = _config.value ?: return@launch
        val eng = EngineCache.get(cfg)
        if (!eng.hasAuthority() && bundleType != "license_list") { _toast.value = "No authority loaded."; return@launch }
        if (bkPw.isBlank()) { _toast.value = "Password required."; return@launch }
        try {
            when (bundleType) {
                "authority"    -> eng.saveAuthBackupFile(eng.buildBackupBlob(bkPw, "authority_only",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText())))
                "license_list" -> eng.saveListBackupFile(eng.buildBackupBlob(bkPw, "license_list_only", eng.loadRecords()))
                "full"         -> eng.saveFullBackupFile(eng.buildBackupBlob(bkPw, "full_backup",
                    mapOf("public_key_pem" to eng.publicKeyPem(), "private_key_pem" to eng.privPath().readText(),
                          "licenses" to eng.loadRecords())))
            }
            _toast.value = "Backup saved to License_Backups/"
        } catch (e: Exception) { _toast.value = "Backup failed: ${e.message}" }
    }

    class Factory(private val pid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = ProductManagerViewModel(pid) as T
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun ProductManagerScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    vm: ProductManagerViewModel = viewModel(factory = ProductManagerViewModel.Factory(productId))
) {
    val clipboard  = LocalClipboardManager.current
    val config     by vm.config.collectAsState()
    val tab        by vm.tab.collectAsState()
    val records    by vm.records.collectAsState()
    val loading    by vm.loading.collectAsState()
    val toast      by vm.toast.collectAsState()
    val lastCode   by vm.lastCode.collectAsState()
    val lastLid    by vm.lastLid.collectAsState()
    val generating by vm.generating.collectAsState()
    val genStatus  by vm.genStatus.collectAsState()
    val genIsError by vm.genIsError.collectAsState()
    val backupText by vm.backupText.collectAsState()
    val revoText   by vm.revoText.collectAsState()
    val githubFiles by vm.githubFiles.collectAsState()
    val appColors   = LocalAppColors.current

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    val col = remember(config?.color) {
        runCatching { config?.color?.let { Color(android.graphics.Color.parseColor(it)) } }
            .getOrNull() ?: TealCol
    }

    // Details dialog state
    var detailsRec by remember { mutableStateOf<LicenseRecord?>(null) }
    var confirmDelLid by remember { mutableStateOf<String?>(null) }
    var showRemoveAuthConfirm by remember { mutableStateOf(false) }

    confirmDelLid?.let { lid ->
        ConfirmDialog("Delete License", "Permanently delete $lid?", "Delete", RedCol,
            onConfirm = { vm.deleteLicense(lid); confirmDelLid = null },
            onDismiss = { confirmDelLid = null })
    }

    if (showRemoveAuthConfirm) {
        ConfirmDialog("Remove Authority", "This deletes the local keypair. Have a backup first!", "Remove", RedCol,
            onConfirm = { vm.removeAuthority(); showRemoveAuthConfirm = false },
            onDismiss = { showRemoveAuthConfirm = false })
    }

    detailsRec?.let { rec ->
        AlertDialog(
            onDismissRequest = { detailsRec = null },
            containerColor = CardBg,
            title = { Text("License Details", color = TealCol, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "ID" to rec.licenseId, "Tier" to rec.tier.uppercase(),
                        "Status" to rec.status.uppercase(), "Source" to rec.source,
                        "Customer" to rec.customerName.ifBlank { "-" },
                        "Email" to rec.customerEmail.ifBlank { "-" },
                        "Device" to rec.deviceCode.ifBlank { "-" },
                        "Issued" to rec.issuedAt.take(10),
                        "Expiry" to rec.expiry.ifBlank { "-" },
                        "Label" to rec.label.ifBlank { "-" },
                        "Note" to rec.customerNote.ifBlank { "-" }
                    ).forEach { (k, v) ->
                        BodyText("$k: $v", if (k == "Status" && v == "REVOKED") RedCol else SubText)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Copied feedback state
                    var showCopied by remember { mutableStateOf(false) }
                    if (showCopied) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1500)
                            showCopied = false
                        }
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(rec.activationCode))
                            showCopied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showCopied) GreenCol else TealCol)
                    ) {
                        Text(
                            if (showCopied) "✓ License Copied!" else "Copy Activation Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsRec = null }) { Text("CLOSE", color = SubText) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        // Tab bar — 2 rows of 3, full tab names
        val allTabs = ProductManagerViewModel.Tab.values().toList()
        val tabLabels = mapOf(
            ProductManagerViewModel.Tab.DASHBOARD   to "Dashboard",
            ProductManagerViewModel.Tab.AUTHORITY   to "Authority",
            ProductManagerViewModel.Tab.GENERATE    to "Generate",
            ProductManagerViewModel.Tab.LICENSES    to "Licenses",
            ProductManagerViewModel.Tab.REVOCATIONS to "Revocations",
            ProductManagerViewModel.Tab.BACKUPS     to "Backups"
        )
        Column(modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            allTabs.chunked(3).forEach { rowTabs ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowTabs.forEach { t ->
                        Button(
                            onClick  = { vm.switchTab(t) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = if (t == tab) col else CardBg2),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text(tabLabels[t] ?: t.name, fontSize = 10.sp, color = Color.White, maxLines = 1) }
                    }
                }
            }
        }

        if (toast.isNotEmpty()) {
            AppCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                BodyText(toast, TealCol)
            }
        }

        if (loading) {
            Box(Modifier.weight(1f)) { LoadingOverlay() }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                config?.let { cfg ->
                    val engine = remember(cfg.id) { EngineCache.get(cfg) }

                    when (tab) {

                        // ── DASHBOARD ──────────────────────────────────────────
                        ProductManagerViewModel.Tab.DASHBOARD -> {
                            val stats = remember(cfg.id, records) { engine.stats() }

                            // Header card
                            AppCard(color = CardBg2) {
                                Text(cfg.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                BodyText("Activation prefix: ${cfg.activationPrefix}-", SubText)
                                BodyText("License prefix: ${cfg.licensePrefix}-", SubText)
                                BodyText("Tiers: ${cfg.tiers.joinToString(", ")}", SubText)
                            }
                            // Authority status
                            // Check keypair on IO thread — RSA crypto must not run on main thread
                            var keypairOk by remember(cfg.id) { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(cfg.id) {
                                keypairOk = withContext(Dispatchers.IO) { engine.keypairIntact() }
                            }
                            AppCard(color = CardBg2) {
                                SectionLabel("Authority Status",
                                    if (!engine.hasAuthority()) RedCol
                                    else if (keypairOk == false) OrangeCol
                                    else GreenCol)
                                if (engine.hasAuthority()) {
                                    when (keypairOk) {
                                        null  -> BodyText("Checking keypair integrity…", SubText)
                                        true  -> {
                                            BodyText("Authority loaded.", GreenCol)
                                            BodyText("Fingerprint: ${engine.fingerprint()}", SubText)
                                        }
                                        false -> {
                                            BodyText("⚠ KEYPAIR MISMATCH — private key does not match public key.", OrangeCol)
                                            BodyText("The private.pem on disk was overwritten and no longer matches your public key. Go to Authority tab → Import Backup to restore the correct private key.", SubText)
                                            BodyText("Fingerprint: ${engine.fingerprint()}", SubText)
                                        }
                                    }
                                } else {
                                    BodyText("No authority loaded. Go to Authority tab.", RedCol)
                                }
                            }
                            // Stats
                            AppCard(color = CardBg2) {
                                SectionLabel("License Totals")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) { StatCard(stats.first, "Active", col) }
                                    Box(Modifier.weight(1f)) { StatCard(stats.second, "Revoked", RedCol) }
                                    Box(Modifier.weight(1f)) { StatCard(stats.third, "Total", BlueCol) }
                                }
                            }
                            // Latest issued
                            if (records.isNotEmpty()) {
                                AppCard(color = CardBg2) {
                                    SectionLabel("Latest Issued")
                                    records.take(5).forEach { rec ->
                                        BodyText(
                                            "${rec.licenseId}  •  ${rec.tier.uppercase()}  •  ${rec.customerName.ifBlank{"-"}}  •  ${rec.status.uppercase()}",
                                            if (rec.status == "revoked") RedCol else SubText
                                        )
                                    }
                                }
                            }
                            // Editable bundle_app
                            AppCard {
                                SectionLabel("License App ID (bundle_app)", TealCol)
                                BodyText("Must match the value hardcoded in the customer app.", SubText)
                                AppTextField(vm.bundleAppEdit, { vm.bundleAppEdit = it }, "bundle_app")
                                ActionButton("Save bundle_app", GreenCol) { vm.saveBundleApp() }
                            }
                            // Quick actions
                            AppCard {
                                SectionLabel("Quick Actions")
                                ActionButton("Generate License", col) { vm.switchTab(ProductManagerViewModel.Tab.GENERATE) }
                                ActionButton("Manage Authority", BlueCol) { vm.switchTab(ProductManagerViewModel.Tab.AUTHORITY) }
                                ActionButton("Revocation Export", OrangeCol) { vm.switchTab(ProductManagerViewModel.Tab.REVOCATIONS) }
                            }
                        }

                        // ── AUTHORITY ──────────────────────────────────────────
                        ProductManagerViewModel.Tab.AUTHORITY -> {
                            // Status
                            AppCard(color = CardBg2) {
                                SectionLabel("Authority Status", if (engine.hasAuthority()) GreenCol else RedCol)
                                if (engine.hasAuthority()) {
                                    BodyText("Loaded", GreenCol)
                                    BodyText("Fingerprint: ${engine.fingerprint()}", SubText)
                                    ActionButton("Copy Public Key PEM", TealCol) {
                                        clipboard.setText(AnnotatedString(engine.publicKeyPem()))
                                    }
                                    ActionButton("Remove Local Authority", RedCol) { showRemoveAuthConfirm = true }
                                } else {
                                    BodyText("No authority loaded.", RedCol)
                                }
                            }
                            // Initialize
                            AppCard {
                                SectionLabel("Initialize Fresh Authority")
                                BodyText("Generates a new RSA 2048-bit keypair. After initializing, copy the Public Key PEM and embed it in your customer app before building the APK.", SubText)
                                ActionButton("Initialize Authority", GreenCol) { vm.initAuthority() }
                            }
                            // Manual backup (generate/copy/save one at a time)
                            AppCard {
                                SectionLabel("Backup", BlueCol)
                                BodyText("Password for backup encryption:", SubText)
                                AppTextField(vm.authBackupPw, { vm.authBackupPw = it }, "Backup Password", password = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { vm.generateSingleBackup("auth") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                        Text("Auth Backup", fontSize = 11.sp, color = Color.White)
                                    }
                                    Button(onClick = { vm.generateSingleBackup("full") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                                        Text("Full Backup", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                ActionButton("License List Backup", BlueCol) { vm.generateSingleBackup("list") }
                                if (backupText.isNotEmpty()) {
                                    BodyText("Generated backup:", SubText)
                                    OutlinedTextField(value = backupText, onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = GreenCol, unfocusedTextColor = GreenCol))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(onClick = { clipboard.setText(AnnotatedString(backupText)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                            Text("Copy Backup", fontSize = 11.sp, color = Color.White)
                                        }
                                        Button(onClick = { vm.saveBackupFile(if (backupText.contains("authority_only")) "auth" else if (backupText.contains("license_list")) "list" else "full") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                            Text("Save to File", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            // Export all 3 backups
                            AppCard {
                                SectionLabel("Export All Backups (One Password)", OrangeCol)
                                BodyText("Generate and save all three backup types with a single password.", SubText)
                                AppTextField(vm.allBackupsPw, { vm.allBackupsPw = it }, "Password for all backups", password = true)
                                ActionButton("Generate & Save All 3 Backups", OrangeCol) { vm.exportAllBackups() }
                            }
                            // Upload backups to GitHub
                            AppCard {
                                SectionLabel("Upload Backups to GitHub", CyanCol)
                                BodyText("Upload authority, license list, and full backups to GitHub as separate timestamped files. Uses the same password as Export All Backups above.", SubText)
                                val upPresets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" || it.type == "github_admin" } }
                                if (upPresets.isNotEmpty()) {
                                    var upPresetExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { upPresetExpanded = true }
                                        DropdownMenu(expanded = upPresetExpanded, onDismissRequest = { upPresetExpanded = false }, containerColor = CardBg) {
                                            upPresets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghUpOwner  = p.owner
                                                    vm.ghUpRepo   = p.repo
                                                    vm.ghUpBranch = p.branch.ifBlank { "main" }
                                                    vm.ghUpToken  = p.token
                                                    upPresetExpanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                AppTextField(vm.ghUpOwner,  { vm.ghUpOwner  = it }, "GitHub Owner")
                                AppTextField(vm.ghUpRepo,   { vm.ghUpRepo   = it }, "Repository Name")
                                AppTextField(vm.ghUpBranch, { vm.ghUpBranch = it }, "Branch (default: main)")
                                AppTextField(vm.ghUpFolder, { vm.ghUpFolder = it }, "Folder Path (e.g. Backups)")
                                AppTextField(vm.ghUpToken,  { vm.ghUpToken  = it }, "GitHub Token (contents:write)", password = true)
                                ActionButton("Upload All 3 Backups to GitHub", GreenCol) { vm.uploadBackupsToGitHub() }
                            }
                            // Import from GitHub
                            AppCard {
                                SectionLabel("Import Authority from GitHub", CyanCol)
                                BodyText("List and import .ctp or .ctfull backup files from a GitHub folder.", SubText)
                                val imPresets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" || it.type == "github_admin" } }
                                if (imPresets.isNotEmpty()) {
                                    var imPresetExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { imPresetExpanded = true }
                                        DropdownMenu(expanded = imPresetExpanded, onDismissRequest = { imPresetExpanded = false }, containerColor = CardBg) {
                                            imPresets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghImOwner  = p.owner
                                                    vm.ghImRepo   = p.repo
                                                    vm.ghImBranch = p.branch.ifBlank { "main" }
                                                    vm.ghImToken  = p.token
                                                    imPresetExpanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                AppTextField(vm.ghImOwner,  { vm.ghImOwner  = it }, "GitHub Owner")
                                AppTextField(vm.ghImRepo,   { vm.ghImRepo   = it }, "Repository Name")
                                AppTextField(vm.ghImBranch, { vm.ghImBranch = it }, "Branch (default: main)")
                                AppTextField(vm.ghImFolder, { vm.ghImFolder = it }, "Folder Path (e.g. Backups)")
                                AppTextField(vm.ghImToken,  { vm.ghImToken  = it }, "GitHub Token", password = true)
                                ActionButton("List Backup Files", CyanCol) { vm.listGitHubBackupFiles() }
                                if (githubFiles.isNotEmpty()) {
                                    BodyText("Select a file to import:", SubText)
                                    githubFiles.forEach { file ->
                                        val name = file["name"] ?: ""
                                        val size = file["size"]?.toLongOrNull() ?: 0L
                                        val sizeTxt = if (size > 1024) "${size/1024} KB" else "$size B"
                                        ActionButton("$name  ($sizeTxt)", PurpleCol) {
                                            vm.importBackupFromGitHub(file["path"] ?: "")
                                        }
                                    }
                                    AppTextField(vm.ghImPw, { vm.ghImPw = it }, "Backup Password (for decryption)", password = true)
                                }
                            }
                            // Import from paste
                            AppCard {
                                SectionLabel("Import Backup (Paste)")
                                BodyText("Accepts authority-only (.ctp), license-list (.ctlist), and full backups (.ctfull).", SubText)
                                AppTextField(vm.importPastePw,   { vm.importPastePw   = it }, "Backup Password", password = true)
                                AppTextField(vm.importPasteBlob, { vm.importPasteBlob = it }, "Paste backup text here")
                                ActionButton("Import Backup", GreenCol) { vm.importBackupFromPaste() }
                            }
                        }

                        // ── GENERATE ───────────────────────────────────────────
                        ProductManagerViewModel.Tab.GENERATE -> {
                            var genKeypairOk by remember(cfg.id) { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(cfg.id) {
                                genKeypairOk = withContext(Dispatchers.IO) { engine.keypairIntact() }
                            }
                            AppCard {
                                SectionLabel("Generate Activation Code", col)
                                if (!engine.hasAuthority()) {
                                    BodyText("No authority. Initialize in Authority tab first.", RedCol)
                                } else if (genKeypairOk == false) {
                                    BodyText("⚠ KEYPAIR MISMATCH — cannot generate licenses.", OrangeCol)
                                    BodyText("The private.pem on disk does not match the public key. Go to Authority tab → Import Backup to restore the correct private key.", SubText)
                                } else if (genKeypairOk == null) {
                                    BodyText("Checking keypair…", SubText)
                                } else {
                                    // Device Code FIRST (matches Kivy layout)
                                    BodyText("Device Code", SubText)
                                    AppTextField(vm.genDevice, { vm.genDevice = it }, "Device code  (e.g. SPA-DEV-ABCD1234)")
                                    // Tier dropdown
                                    BodyText("Tier", SubText)
                                    var tierExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Button(onClick = { tierExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.genTier, color = Color.White)
                                        }
                                        DropdownMenu(expanded = tierExpanded, onDismissRequest = { tierExpanded = false },
                                            containerColor = CardBg) {
                                            cfg.tiers.forEach { t ->
                                                DropdownMenuItem(text = { Text(t, color = Color.White) }, onClick = { vm.genTier = t; tierExpanded = false })
                                            }
                                        }
                                    }
                                    // Source dropdown
                                    BodyText("Payment Source", SubText)
                                    var srcExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Button(onClick = { srcExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.genSource, color = Color.White)
                                        }
                                        DropdownMenu(expanded = srcExpanded, onDismissRequest = { srcExpanded = false },
                                            containerColor = CardBg) {
                                            cfg.sources.forEach { s ->
                                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.genSource = s; srcExpanded = false })
                                            }
                                        }
                                    }
                                    BodyText("Customer Name", SubText)
                                    AppTextField(vm.genCustomer, { vm.genCustomer = it }, "Customer name")
                                    BodyText("Customer Email  (optional)", SubText)
                                    AppTextField(vm.genEmail,  { vm.genEmail  = it }, "Customer email")
                                    BodyText("Label / Tag  (optional)", SubText)
                                    AppTextField(vm.genLabel,  { vm.genLabel  = it }, "Internal label or tag")
                                    BodyText("Note  (optional)", SubText)
                                    AppTextField(vm.genNote,   { vm.genNote   = it }, "Notes")
                                    BodyText("Expiry  YYYY-MM-DD  (optional)", SubText)
                                    AppTextField(vm.genExpiry, { vm.genExpiry = it }, "Expiry date")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.generateLicense() }, modifier = Modifier.weight(1f),
                                            enabled = !generating,
                                            colors = ButtonDefaults.buttonColors(containerColor = col)) {
                                            if (generating) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Text("Generate License", color = Color.White)
                                            }
                                        }
                                        Button(onClick = { vm.clearGenForm() }, modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangeCol)) {
                                            Text("Clear Form", color = Color.White)
                                        }
                                    }
                                    // ── Persistent status / error area (always visible) ──
                                    BodyText("Activation Code", SubText)
                                    OutlinedTextField(
                                        value = lastCode.ifBlank { "" },
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text("Generated code will appear here", color = SubText, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = GreenCol, unfocusedTextColor = GreenCol,
                                            unfocusedBorderColor = SubText, focusedBorderColor = GreenCol)
                                    )
                                    if (genStatus.isNotEmpty()) {
                                        Text(genStatus,
                                            color = if (genIsError) RedCol else GreenCol,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                    if (lastCode.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { clipboard.setText(AnnotatedString(lastCode)) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                                Text("Copy Code", color = Color.White)
                                            }
                                            Button(onClick = { clipboard.setText(AnnotatedString(lastLid)) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                                Text("Copy License ID", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── LICENSES ───────────────────────────────────────────
                        ProductManagerViewModel.Tab.LICENSES -> {
                            // Search & filter card
                            AppCard {
                                SectionLabel("Search & Filter", TealCol)
                                AppTextField(vm.searchQuery, { vm.searchQuery = it },
                                    "Search by ID / name / email / device / label")
                                // Status & sort dropdowns
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var statusExpanded by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        Button(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.filterStatus, color = Color.White, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, containerColor = CardBg) {
                                            listOf("all", "active", "revoked").forEach { s ->
                                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.filterStatus = s; statusExpanded = false })
                                            }
                                        }
                                    }
                                    var sortExpanded by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        Button(onClick = { sortExpanded = true }, modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CardBg2)) {
                                            Text(vm.filterSort, color = Color.White, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, containerColor = CardBg) {
                                            listOf("newest", "oldest", "tier", "status").forEach { s ->
                                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { vm.filterSort = s; sortExpanded = false })
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { vm.reload() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                                        Text("Refresh", color = Color.White)
                                    }
                                    Button(onClick = { vm.exportCsv() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                        Text("Export CSV", color = Color.White)
                                    }
                                }
                            }

                            val filtered = vm.filteredRecords()
                            BodyText("${filtered.size} license(s) shown.", SubText)

                            if (filtered.isEmpty()) {
                                AppCard { BodyText("No licenses match the current filters.", SubText) }
                            } else {
                                filtered.forEach { rec ->
                                    AppCard(color = CardBg2) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(rec.licenseId, color = col, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(rec.status.uppercase(),
                                                color = if (rec.status == "active") GreenCol else RedCol,
                                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        BodyText("${rec.tier.uppercase()}  •  ${rec.source}  •  ${rec.customerName.ifBlank{"-"}}  •  ${rec.issuedAt.take(10)}", SubText)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(onClick = { detailsRec = rec }) {
                                                Text("Details", color = TealCol, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { vm.toggleRevoke(rec.licenseId) }) {
                                                Text(if (rec.status == "active") "Revoke" else "Restore",
                                                    color = if (rec.status == "active") OrangeCol else GreenCol,
                                                    fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { confirmDelLid = rec.licenseId }) {
                                                Text("Delete", color = RedCol, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── REVOCATIONS ────────────────────────────────────────
                        ProductManagerViewModel.Tab.REVOCATIONS -> {
                            AppCard {
                                SectionLabel("Revocation Export", OrangeCol)
                                BodyText("Generate a signed revocation JSON and upload to GitHub so customer apps can check it on launch.", SubText)
                                ActionButton("Generate Signed Revocation File", col) { vm.generateRevocation() }
                                if (revoText.isNotEmpty()) {
                                    OutlinedTextField(value = revoText, onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = GreenCol, unfocusedTextColor = GreenCol))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { clipboard.setText(AnnotatedString(revoText)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                                            Text("Copy JSON", color = Color.White)
                                        }
                                        Button(onClick = { vm.saveRevocationFile() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                                            Text("Save File", color = Color.White)
                                        }
                                    }
                                }
                            }
                            AppCard {
                                SectionLabel("GitHub Upload", GreenCol)
                                // Cloud Preset loader
                                val rvPresets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" } }
                                if (rvPresets.isNotEmpty()) {
                                    var rvPresetExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        ActionButton("Load from Cloud Preset", CyanCol) { rvPresetExpanded = true }
                                        DropdownMenu(expanded = rvPresetExpanded, onDismissRequest = { rvPresetExpanded = false }, containerColor = CardBg) {
                                            rvPresets.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name, color = Color.White) }, onClick = {
                                                    vm.ghRvOwner  = p.owner
                                                    vm.ghRvRepo   = p.repo
                                                    vm.ghRvBranch = p.branch.ifBlank { "main" }
                                                    vm.ghRvPath   = p.path
                                                    vm.ghRvToken  = p.token
                                                    rvPresetExpanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                BodyText("Owner", SubText)
                                AppTextField(vm.ghRvOwner,  { vm.ghRvOwner  = it }, "GitHub owner")
                                BodyText("Repo", SubText)
                                AppTextField(vm.ghRvRepo,   { vm.ghRvRepo   = it }, "Repository name")
                                BodyText("Branch", SubText)
                                AppTextField(vm.ghRvBranch, { vm.ghRvBranch = it }, "Branch (default: main)")
                                BodyText("Path", SubText)
                                AppTextField(vm.ghRvPath,   { vm.ghRvPath   = it }, "Path in repo (e.g. LICENSING/revo.json)")
                                BodyText("Token", SubText)
                                AppTextField(vm.ghRvToken,  { vm.ghRvToken  = it }, "GitHub Token (contents:write)", password = true)
                                ActionButton("Upload Revocation to GitHub", GreenCol) { vm.uploadRevocationToGitHub() }
                                if (engine.hasAuthority()) {
                                    ActionButton("Copy Public Key PEM", TealCol) {
                                        clipboard.setText(AnnotatedString(engine.publicKeyPem()))
                                    }
                                }
                            }
                        }

                        // ── BACKUPS ────────────────────────────────────────────
                        ProductManagerViewModel.Tab.BACKUPS -> {
                            AppCard {
                                SectionLabel("Backups", BlueCol)
                                BodyText("Export encrypted backups to Downloads/SLACKLINE ADMIN FILES/License_Backups/", SubText)
                                AppTextField(vm.bkPw, { vm.bkPw = it }, "Backup Password", password = true)
                                ActionButton("Export Authority Backup",    BlueCol) { vm.exportBackup("authority") }
                                ActionButton("Export License List Backup", BlueCol) { vm.exportBackup("license_list") }
                                ActionButton("Export Full Backup",         BlueCol) { vm.exportBackup("full") }
                            }
                        }
                    }
                } ?: BodyText("Loading product...", SubText)
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }
}
