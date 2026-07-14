package com.example.slacklineadminapp.data

import android.util.Base64
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey as BCRSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey as BCRSAPublicKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.zip.Deflater
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class NewProduct(
    @com.google.gson.annotations.SerializedName("id")                val id: String = "",
    @com.google.gson.annotations.SerializedName("display_name")      val displayName: String = "",
    @com.google.gson.annotations.SerializedName("prefix_root")       val prefixRoot: String = "",
    @com.google.gson.annotations.SerializedName("activation_prefix") val activationPrefix: String = "",
    @com.google.gson.annotations.SerializedName("license_prefix")    val licensePrefix: String = "",
    @com.google.gson.annotations.SerializedName("color")             val color: String = "#00A383",
    @com.google.gson.annotations.SerializedName("github_owner")      val githubOwner: String = "",
    @com.google.gson.annotations.SerializedName("github_repo")       val githubRepo: String = "",
    @com.google.gson.annotations.SerializedName("github_branch")     val githubBranch: String = "main",
    @com.google.gson.annotations.SerializedName("github_path")       val githubPath: String = "",
    @com.google.gson.annotations.SerializedName("bundle_app")        val bundleApp: String = "",
    @com.google.gson.annotations.SerializedName("created_at")        val createdAt: String = ""
)

object NewLicenseStore {
    private fun regFile() = AppStorage.newProductsRegistryFile()
    private fun licDir(pid: String) = AppStorage.newLicensesDir(pid)
    private fun licFile(pid: String) = File(licDir(pid), "licenses.json")
    fun revFile()            = AppStorage.newRevocationsFile()
    fun pubKeyPath(pid: String)  = File(AppStorage.productDir(pid), "new_public.pem")
    fun privKeyPath(pid: String) = File(AppStorage.productDir(pid), "new_private.pem")
    fun hasAuthority(pid: String) = pubKeyPath(pid).exists() && privKeyPath(pid).exists()

    fun allProducts(): List<NewProduct> =
        AppStorage.loadJson(regFile(), emptyList<NewProduct>())
    fun saveProducts(list: List<NewProduct>) = AppStorage.saveJson(regFile(), list)
    fun addProduct(p: NewProduct) = saveProducts(allProducts().filter { it.id != p.id } + p)
    fun removeProduct(id: String) = saveProducts(allProducts().filter { it.id != id })

    fun loadLicenses(pid: String): List<LicenseRecord> =
        AppStorage.loadJson(licFile(pid), emptyList<LicenseRecord>())
    fun saveLicenses(pid: String, list: List<LicenseRecord>) =
        AppStorage.saveJson(licFile(pid), list)

    fun loadRevocations(): Map<String, List<String>> =
        AppStorage.loadJson(revFile(), emptyMap<String, List<String>>())
    fun saveRevocations(m: Map<String, List<String>>) = AppStorage.saveJson(revFile(), m)
    fun addRevocation(pid: String, lid: String) {
        val m = loadRevocations().toMutableMap()
        m[pid] = (m.getOrDefault(pid, emptyList()) + lid).distinct()
        saveRevocations(m)
    }
    fun removeRevocation(pid: String, lid: String) {
        val m = loadRevocations().toMutableMap()
        m[pid] = m.getOrDefault(pid, emptyList()).filter { it != lid }
        saveRevocations(m)
    }

    fun fingerprint(pid: String): String {
        if (!hasAuthority(pid)) return "No authority"
        return MessageDigest.getInstance("SHA-256")
            .digest(pubKeyPath(pid).readBytes())
            .take(8).joinToString("") { "%02X".format(it) }
    }

    fun publicKeyPem(pid: String): String =
        if (pubKeyPath(pid).exists()) pubKeyPath(pid).readText() else ""

    // ── Key I/O ───────────────────────────────────────────────────────────────

    private fun bytesToPem(label: String, bytes: ByteArray): String {
        val b64   = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val lines = b64.chunked(64).joinToString("\n")
        return "-----BEGIN $label-----\n$lines\n-----END $label-----\n"
    }

    private fun pemToBytes(file: File): ByteArray {
        val b64 = file.readText().lines()
            .filter { !it.startsWith("-----") }.joinToString("")
        return Base64.decode(b64, Base64.DEFAULT)
    }

    private fun loadPrivateKey(pid: String): PrivateKey {
        val pem      = privKeyPath(pid).readText()
        val keyBytes = pemToBytes(privKeyPath(pid))
        return if ("BEGIN RSA PRIVATE KEY" in pem) {
            val algId = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
            val pkcs8 = PrivateKeyInfo(algId, BCRSAPrivateKey.getInstance(keyBytes)).encoded
            KeyFactory.getInstance("RSA", "BC").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        } else {
            KeyFactory.getInstance("RSA", "BC").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        }
    }

    private fun loadPublicKey(pid: String): PublicKey {
        val pem      = pubKeyPath(pid).readText()
        val keyBytes = pemToBytes(pubKeyPath(pid))
        return if ("BEGIN RSA PUBLIC KEY" in pem) {
            val algId = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
            val spki  = SubjectPublicKeyInfo(algId, BCRSAPublicKey.getInstance(keyBytes)).encoded
            KeyFactory.getInstance("RSA", "BC").generatePublic(X509EncodedKeySpec(spki))
        } else {
            KeyFactory.getInstance("RSA", "BC").generatePublic(X509EncodedKeySpec(keyBytes))
        }
    }

    fun keypairIntact(pid: String): Boolean {
        if (!hasAuthority(pid)) return false
        return try {
            val test = "kp_check".toByteArray()
            val sig  = Signature.getInstance("SHA256withRSA", "BC")
                .also { it.initSign(loadPrivateKey(pid)); it.update(test) }.sign()
            Signature.getInstance("SHA256withRSA", "BC")
                .also { it.initVerify(loadPublicKey(pid)); it.update(test) }.verify(sig)
        } catch (e: Exception) { false }
    }

    // ── Authority init ─────────────────────────────────────────────────────────

    fun initAuthority(pid: String) {
        if (hasAuthority(pid)) throw RuntimeException("Authority already exists.")
        val kp = KeyPairGenerator.getInstance("RSA", "BC")
            .also { it.initialize(2048) }.generateKeyPair()
        // Save PKCS#1 so Python rsa.PublicKey.load_pkcs1 / rsa.PrivateKey.load_pkcs1 works
        val privPkcs1 = PrivateKeyInfo.getInstance(kp.private.encoded)
            .parsePrivateKey().toASN1Primitive().encoded
        val pubPkcs1  = SubjectPublicKeyInfo.getInstance(kp.public.encoded)
            .parsePublicKey().toASN1Primitive().encoded
        privKeyPath(pid).writeText(bytesToPem("RSA PRIVATE KEY", privPkcs1))
        pubKeyPath(pid).writeText(bytesToPem("RSA PUBLIC KEY",  pubPkcs1))
        AppStorage.logActivity("New Authority Initialized", "RSA 2048-bit PKCS#1 keypair.", pid)
    }

    fun removeAuthority(pid: String) {
        privKeyPath(pid).delete()
        pubKeyPath(pid).delete()
    }

    // ── License generation ─────────────────────────────────────────────────────

    fun generateLicense(
        product: NewProduct,
        tier: String = "pro",
        source: String = "crypto",
        deviceCode: String,
        customerName: String = "",
        customerEmail: String = "",
        label: String = "",
        note: String = "",
        expiry: String = ""
    ): LicenseRecord {
        val pid = product.id
        if (!hasAuthority(pid)) throw RuntimeException("No authority for ${product.displayName}")
        if (deviceCode.isBlank()) throw IllegalArgumentException("Device code required.")

        val charset = "0123456789ABCDEF"
        val lid = "${product.licensePrefix}-${(1..8).map { charset.random() }.joinToString("")}"
        val now = AppStorage.utcNow()

        val payload = mutableMapOf<String, Any?>(
            "app"            to product.bundleApp,
            "schema"         to 1,
            "license_id"     to lid,
            "tier"           to tier,
            "payment_method" to source,
            "source"         to source,
            "device_code"    to deviceCode.uppercase(),
            "customer_name"  to customerName,
            "customer_email" to customerEmail,
            "label"          to label,
            "issued_at"      to now
        )
        if (note.isNotBlank())   payload["note"]       = note
        if (expiry.isNotBlank()) payload["expires_at"] = expiry

        // Sign with compact canonical JSON (matches Python json.dumps sort_keys=True)
        val canonical = AppStorage.canonicalJson(payload)
        val signer    = Signature.getInstance("SHA256withRSA", "BC")
            .also { it.initSign(loadPrivateKey(pid)); it.update(canonical) }
        val sigB64 = Base64.encodeToString(
            signer.sign(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val blob  = AppStorage.gson.toJson(mapOf("p" to payload, "s" to sigB64))
            .toByteArray(Charsets.UTF_8)
        val def   = Deflater(9).also { it.setInput(blob); it.finish() }
        val buf   = ByteArray(blob.size * 2 + 64)
        val n     = def.deflate(buf); def.end()
        val token = Base64.encodeToString(
            buf.copyOf(n), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val code = "${product.activationPrefix}-${token.chunked(24).joinToString(".")}"

        val rec = LicenseRecord(
            licenseId      = lid,
            tier           = tier,
            source         = source,
            paymentMethod  = source,
            deviceCode     = deviceCode.uppercase(),
            customerName   = customerName,
            customerEmail  = customerEmail,
            label          = label,
            customerNote   = note,
            expiresAt      = expiry,
            expiry         = expiry,
            issuedAt       = now,
            status         = "active",
            activationCode = code,
            signatureValid = true
        )
        saveLicenses(pid, listOf(rec) + loadLicenses(pid))
        AppStorage.logActivity("New License Generated", "ID: $lid  Tier: ${tier.uppercase()}", product.displayName)
        return rec
    }

    fun toggleRevoke(pid: String, lid: String): String {
        val recs = loadLicenses(pid).toMutableList()
        val idx  = recs.indexOfFirst { it.licenseId == lid }
        if (idx == -1) return "not_found"
        val newStatus = if (recs[idx].status == "active") "revoked" else "active"
        recs[idx] = recs[idx].copy(
            status    = newStatus,
            revokedAt = if (newStatus == "revoked") AppStorage.utcNow() else ""
        )
        saveLicenses(pid, recs)
        if (newStatus == "revoked") addRevocation(pid, lid)
        else removeRevocation(pid, lid)
        return newStatus
    }

    fun deleteLicense(pid: String, lid: String) {
        saveLicenses(pid, loadLicenses(pid).filter { it.licenseId != lid })
    }

    fun stats(pid: String): Triple<Int, Int, Int> {
        val r = loadLicenses(pid)
        return Triple(r.count { it.status == "active" }, r.count { it.status == "revoked" }, r.size)
    }

    fun exportCsv(pid: String, displayName: String): File {
        val recs = loadLicenses(pid)
        val dir  = AppStorage.exportDir(displayName, "License Exports").also { it.mkdirs() }
        val file = File(dir, "${pid}_export_${AppStorage.timestamp()}.csv")
        val hdr  = "license_id,tier,status,source,customer_name,customer_email,device_code,label,note,issued_at,expiry,revoked_at"
        file.writeText(hdr + "\n" + recs.joinToString("\n") { r ->
            listOf(r.licenseId, r.tier, r.status, r.source, r.customerName, r.customerEmail,
                   r.deviceCode, r.label, r.customerNote, r.issuedAt, r.expiry, r.revokedAt)
                .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        })
        return file
    }

    // ── Backups ────────────────────────────────────────────────────────────────

    fun buildBackupBlob(pid: String, password: String, bundleType: String): String {
        if (password.isBlank()) throw IllegalArgumentException("Password required.")
        val product = allProducts().find { it.id == pid }
            ?: throw RuntimeException("Product not found: $pid")
        val payload: Any = when (bundleType) {
            "authority_only"    -> mapOf(
                "private_key_pem" to privKeyPath(pid).readText(),
                "public_key_pem"  to pubKeyPath(pid).readText()
            )
            "license_list_only" -> mapOf("licenses" to loadLicenses(pid))
            else                -> mapOf(
                "private_key_pem" to privKeyPath(pid).readText(),
                "public_key_pem"  to pubKeyPath(pid).readText(),
                "licenses"        to loadLicenses(pid),
                "revoked_ids"     to (loadRevocations()[pid] ?: emptyList<String>())
            )
        }
        val raw  = AppStorage.canonicalJson(
            mapOf("schema" to 2, "app" to product.bundleApp,
                  "bundle_type" to bundleType, "exported_at" to AppStorage.utcNow(),
                  "payload" to payload)
        )
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 200_000, 256)).encoded
        val ct   = xorStream(key, raw)
        val mac  = Mac.getInstance("HmacSHA256")
            .also { it.init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(salt + ct)
        val enc  = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        return AppStorage.gson.toJson(mapOf(
            "schema"      to 2,
            "app"         to product.bundleApp,
            "bundle_type" to bundleType,
            "salt"        to Base64.encodeToString(salt, enc),
            "ciphertext"  to Base64.encodeToString(ct,   enc),
            "mac"         to Base64.encodeToString(mac,  enc)
        ))
    }

    fun saveBackupFile(pid: String, blob: String, kind: String): File {
        val dir = AppStorage.exportDir(
            allProducts().find { it.id == pid }?.displayName ?: pid,
            "License Backups"
        ).also { it.mkdirs() }
        val ext = when(kind) { "auth" -> ".ctp"; "list" -> ".ctlist"; else -> ".ctfull" }
        return File(dir, "${pid}_${kind}_${AppStorage.timestamp()}$ext").also { it.writeText(blob) }
    }

    fun applyBackupBlob(pid: String, blob: String, password: String) {
        val json       = org.json.JSONObject(blob)
        val bundleType = json.optString("bundle_type")
        val salt = b64d(json.getString("salt"))
        val ct   = b64d(json.getString("ciphertext"))
        val mac  = b64d(json.getString("mac"))
        val key  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 200_000, 256)).encoded
        val expectedMac = Mac.getInstance("HmacSHA256")
            .also { it.init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(salt + ct)
        if (!MessageDigest.isEqual(mac, expectedMac))
            throw RuntimeException("Wrong password or corrupted backup.")
        val plain   = String(xorStream(key, ct), Charsets.UTF_8)
        val payload = org.json.JSONObject(plain).optJSONObject("payload") ?: org.json.JSONObject()

        if (bundleType == "authority_only" || bundleType == "full_backup") {
            val priv = payload.optString("private_key_pem").trim()
            val pub  = payload.optString("public_key_pem").trim()
            if (priv.isBlank() || pub.isBlank()) throw RuntimeException("No keypair in backup.")
            privKeyPath(pid).writeText(priv)
            pubKeyPath(pid).writeText(pub)
        }
        if (bundleType == "license_list_only" || bundleType == "full_backup") {
            val arr = payload.optJSONArray("licenses")
            if (arr != null) {
                val recs = (0 until arr.length()).map {
                    AppStorage.gson.fromJson(arr.getJSONObject(it).toString(), LicenseRecord::class.java)
                }
                saveLicenses(pid, recs)
            }
        }
    }

    private fun b64d(s: String): ByteArray {
        val p = s.trim().replace('-','+').replace('_','/')
        val padded = p + "=".repeat((4 - p.length % 4) % 4)
        return Base64.decode(padded, Base64.DEFAULT)
    }

    private fun xorStream(key: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(data.size); var offset = 0; var ctr = 0
        while (offset < data.size) {
            val block = MessageDigest.getInstance("SHA-256")
                .digest(key + byteArrayOf((ctr shr 24).toByte(),(ctr shr 16).toByte(),(ctr shr 8).toByte(),ctr.toByte()))
            val take = minOf(block.size, data.size - offset)
            for (i in 0 until take) out[offset+i] = (data[offset+i].toInt() xor block[i].toInt()).toByte()
            offset += take; ctr++
        }
        return out
    }

    // ── GitHub helpers ─────────────────────────────────────────────────────────

    fun uploadFileToGitHub(owner: String, repo: String, branch: String,
                            folder: String, filename: String, content: String, token: String) {
        val path = "${folder.trim('/')}/$filename"
        val url  = "https://api.github.com/repos/$owner/$repo/contents/$path"
        val enc  = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val body = org.json.JSONObject(mapOf("message" to "Upload $filename",
            "content" to enc, "branch" to branch))
        val conn = (URL(url).openConnection() as HttpURLConnection).also {
            it.requestMethod = "PUT"
            it.setRequestProperty("Authorization", "Bearer $token")
            it.setRequestProperty("Accept", "application/vnd.github+json")
            it.setRequestProperty("Content-Type", "application/json")
            it.doOutput = true; it.connectTimeout = 30000; it.readTimeout = 30000
        }
        conn.outputStream.bufferedWriter().use { it.write(body.toString()) }
        val code = conn.responseCode
        if (code !in 200..201) throw RuntimeException("GitHub upload failed ($code)")
    }

    fun listGitHubFiles(owner: String, repo: String, folder: String,
                         branch: String, token: String): List<Map<String, String>> {
        val url  = "https://api.github.com/repos/$owner/$repo/contents/${folder.trim('/')}?ref=$branch"
        val conn = (URL(url).openConnection() as HttpURLConnection).also {
            it.setRequestProperty("Authorization", "Bearer $token")
            it.setRequestProperty("Accept", "application/vnd.github+json")
            it.connectTimeout = 15000; it.readTimeout = 15000
        }
        val arr = org.json.JSONArray(conn.inputStream.bufferedReader().readText())
        return (0 until arr.length()).mapNotNull { i ->
            val obj  = arr.getJSONObject(i)
            val name = obj.optString("name")
            if (name.endsWith(".ctp") || name.endsWith(".ctfull"))
                mapOf("name" to name, "path" to obj.optString("path"),
                      "size" to obj.optInt("size").toString())
            else null
        }
    }

    fun fetchFileFromGitHub(owner: String, repo: String, path: String,
                             branch: String, token: String): String {
        val url  = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
        val conn = (URL(url).openConnection() as HttpURLConnection).also {
            it.setRequestProperty("Authorization", "Bearer $token")
            it.setRequestProperty("Accept", "application/vnd.github+json")
            it.connectTimeout = 15000; it.readTimeout = 15000
        }
        val meta    = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
        val encoded = meta.getString("content").replace("\n", "")
        return String(Base64.decode(encoded, Base64.DEFAULT))
    }

    fun uploadRevocationToGitHub(pid: String, owner: String, repo: String,
                                  branch: String, path: String, token: String) {
        val product = allProducts().find { it.id == pid }
            ?: throw RuntimeException("Product not found")
        val revoked = loadRevocations()[pid] ?: emptyList()
        val json    = AppStorage.gson.toJson(mapOf(
            "app" to product.bundleApp, "version" to 1,
            "updated_at" to AppStorage.utcNow(), "revoked_ids" to revoked
        ))
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/${path.trim('/')}"
        var sha = ""
        runCatching {
            val get = (URL(apiUrl).openConnection() as HttpURLConnection).also {
                it.setRequestProperty("Authorization", "Bearer $token")
                it.setRequestProperty("Accept", "application/vnd.github+json")
                it.connectTimeout = 15000; it.readTimeout = 15000
            }
            if (get.responseCode == 200)
                sha = org.json.JSONObject(get.inputStream.bufferedReader().readText()).optString("sha")
        }
        val bodyMap = mutableMapOf(
            "message" to "Update ${product.displayName} revocations",
            "content" to Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            "branch"  to branch
        )
        if (sha.isNotBlank()) bodyMap["sha"] = sha
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).also {
            it.requestMethod = "PUT"
            it.setRequestProperty("Authorization", "Bearer $token")
            it.setRequestProperty("Accept", "application/vnd.github+json")
            it.setRequestProperty("Content-Type", "application/json")
            it.doOutput = true; it.connectTimeout = 30000; it.readTimeout = 30000
        }
        conn.outputStream.bufferedWriter().use { it.write(org.json.JSONObject(bodyMap as Map<*,*>).toString()) }
        val code = conn.responseCode
        if (code !in 200..201) throw RuntimeException("GitHub upload failed ($code)")
    }
}
