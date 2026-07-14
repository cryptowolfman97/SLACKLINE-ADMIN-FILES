package com.example.slacklineadminapp.data

import android.util.Base64
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey as BCRSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey as BCRSAPublicKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import com.google.gson.annotations.SerializedName
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.zip.Deflater
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private val BC_INIT: Unit by lazy {
    // Android ships a stripped-down BC provider. Remove it first so the
    // full BouncyCastle (SHA256withRSA etc.) is available at position 1.
    Security.removeProvider("BC")
    Security.insertProviderAt(BouncyCastleProvider(), 1)
    Unit  // insertProviderAt returns Int; explicitly return Unit for the delegate
}

data class ProductConfig(
    @SerializedName("id")                val id: String = "",
    @SerializedName("display_name")      val displayName: String = "",
    @SerializedName("prefix_root")       val prefixRoot: String = "",
    @SerializedName("activation_prefix") val activationPrefix: String = "",
    @SerializedName("license_prefix")    val licensePrefix: String = "",
    @SerializedName("tiers")             val tiers: List<String> = listOf("pro"),
    @SerializedName("sources")           val sources: List<String> = listOf("crypto", "bank", "promo", "test"),
    @SerializedName("color")             val color: String = "#00A383",
    @SerializedName("github_owner")      val githubOwner: String = "",
    @SerializedName("github_repo")       val githubRepo: String = "",
    @SerializedName("github_branch")     val githubBranch: String = "main",
    @SerializedName("github_path")       val githubPath: String = "",
    @SerializedName("bundle_app")        val bundleApp: String = "",
    @SerializedName("created_at")        val createdAt: String = ""
) {
    companion object {
        fun slugify(s: String) = s.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "product" }

        fun build(
            displayName: String,
            prefixRoot: String,
            tiers: List<String>,
            sources: List<String>,
            color: String,
            githubOwner: String = "",
            githubRepo: String = "",
            githubBranch: String = "main",
            githubPath: String = "",
            productId: String? = null,
            bundleApp: String? = null
        ): ProductConfig {
            val clean = prefixRoot.uppercase().trimEnd('-')
            val pid   = productId ?: slugify(displayName)
            return ProductConfig(
                id               = pid,
                displayName      = displayName,
                prefixRoot       = clean,
                activationPrefix = "${clean}6A",
                licensePrefix    = clean,
                tiers            = tiers,
                sources          = sources,
                color            = color,
                githubOwner      = githubOwner,
                githubRepo       = githubRepo,
                githubBranch     = githubBranch.ifBlank { "main" },
                githubPath       = githubPath,
                bundleApp        = bundleApp ?: pid,
                createdAt        = AppStorage.utcNow()
            )
        }
    }
}

data class LicenseRecord(
    @SerializedName("license_id")      val licenseId: String = "",
    @SerializedName("tier")            val tier: String = "pro",
    @SerializedName("source")          val source: String = "",
    @SerializedName("payment_method")  val paymentMethod: String = "",
    @SerializedName("customer_name")   val customerName: String = "",
    @SerializedName("customer_email")  val customerEmail: String = "",
    @SerializedName("device_code")     val deviceCode: String = "",
    @SerializedName("label")           val label: String = "",
    @SerializedName("customer_note")   val customerNote: String = "",
    @SerializedName("expiry")          val expiry: String = "",
    @SerializedName("expires_at")      val expiresAt: String = "",
    @SerializedName("issued_at")       val issuedAt: String = "",
    @SerializedName("status")          val status: String = "active",
    @SerializedName("activation_code") val activationCode: String = "",
    @SerializedName("signature_valid") val signatureValid: Boolean = true,
    @SerializedName("revoked_at")      val revokedAt: String = ""
)

class LicenseEngine(val config: ProductConfig) {

    init { BC_INIT }

    private fun pdir() = AppStorage.productDir(config.id)
    fun privPath() = File(pdir(), "private.pem")
    fun pubPath()  = File(pdir(), "public.pem")
    fun dbPath()   = File(pdir(), "licenses_db.json")

    fun authBackupDir() = AppStorage.exportDir(config.displayName, "Authority Backups")
    fun listBackupDir() = AppStorage.exportDir(config.displayName, "License List Backups")
    fun fullBackupDir() = AppStorage.exportDir(config.displayName, "Full Backups")

    fun hasAuthority() = privPath().exists() && pubPath().exists()

    /** Returns true if the private key and public key on disk form a valid RSA keypair. */
    fun keypairIntact(): Boolean {
        if (!hasAuthority()) return false
        return try {
            val testData = "keypair_check".toByteArray()
            val sig = Signature.getInstance("SHA256withRSA", "BC")
            sig.initSign(loadPrivateKey())
            sig.update(testData)
            val signed = sig.sign()
            val ver = Signature.getInstance("SHA256withRSA", "BC")
            ver.initVerify(loadPublicKey())
            ver.update(testData)
            ver.verify(signed)
        } catch (e: Exception) { false }
    }

    fun fingerprint(): String {
        if (!hasAuthority()) return "No authority"
        return MessageDigest.getInstance("SHA-256")
            .digest(pubPath().readBytes())
            .take(8).joinToString("") { "%02X".format(it) }
    }

    fun publicKeyPem(): String = if (pubPath().exists()) pubPath().readText() else ""

    private fun pemToBytes(file: File): ByteArray {
        val b64 = file.readText().lines()
            .filter { !it.startsWith("-----") }
            .joinToString("")
        return Base64.decode(b64, Base64.DEFAULT)
    }

    private fun bytesToPem(label: String, bytes: ByteArray): String {
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val lines = b64.chunked(64).joinToString("\n")
        return "-----BEGIN $label-----\n$lines\n-----END $label-----\n"
    }

    private fun loadPrivateKey(): PrivateKey {
        val pem      = privPath().readText()
        val keyBytes = pemToBytes(privPath())
        return if ("BEGIN RSA PRIVATE KEY" in pem) {
            // PKCS#1 format (Python rsa library / Kivy app) — wrap into PKCS#8 for Java
            val algId = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
            val pkcs8 = PrivateKeyInfo(algId, BCRSAPrivateKey.getInstance(keyBytes)).encoded
            KeyFactory.getInstance("RSA", "BC").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        } else {
            // PKCS#8 format (BEGIN PRIVATE KEY)
            KeyFactory.getInstance("RSA", "BC").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        }
    }

    private fun loadPublicKey(): PublicKey {
        val pem      = pubPath().readText()
        val keyBytes = pemToBytes(pubPath())
        return if ("BEGIN RSA PUBLIC KEY" in pem) {
            // PKCS#1 format (Python rsa library / Kivy app) — wrap into X.509 SPKI for Java
            val algId = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
            val spki  = SubjectPublicKeyInfo(algId, BCRSAPublicKey.getInstance(keyBytes)).encoded
            KeyFactory.getInstance("RSA", "BC").generatePublic(X509EncodedKeySpec(spki))
        } else {
            // X.509 format (BEGIN PUBLIC KEY)
            KeyFactory.getInstance("RSA", "BC").generatePublic(X509EncodedKeySpec(keyBytes))
        }
    }

    fun initializeAuthority() {
        if (hasAuthority()) throw RuntimeException("Authority already exists.")
        val kp = KeyPairGenerator.getInstance("RSA", "BC")
            .also { it.initialize(2048) }.generateKeyPair()
        // Save in PKCS#1 format so Python `rsa` library (load_pkcs1) can read them
        // Private: PKCS#8 -> PKCS#1 RSA via BouncyCastle PrivateKeyInfo
        // Save PKCS#1 so Python `rsa.PrivateKey.load_pkcs1` / `rsa.PublicKey.load_pkcs1` works
        val privPkcs1 = PrivateKeyInfo.getInstance(kp.private.encoded).parsePrivateKey().toASN1Primitive().encoded
        privPath().writeText(bytesToPem("RSA PRIVATE KEY", privPkcs1))
        val pubPkcs1  = SubjectPublicKeyInfo.getInstance(kp.public.encoded).parsePublicKey().toASN1Primitive().encoded
        pubPath().writeText(bytesToPem("RSA PUBLIC KEY", pubPkcs1))
        AppStorage.logActivity("Authority Initialized", "RSA 2048-bit keypair generated (PKCS#1).", config.displayName)
    }

    fun removeLocalAuthority() {
        privPath().delete()
        pubPath().delete()
    }

    private fun sign(payload: Map<String, Any?>): String {
        val sig = Signature.getInstance("SHA256withRSA", "BC")
        sig.initSign(loadPrivateKey())
        sig.update(AppStorage.canonicalJson(payload))
        return Base64.encodeToString(
            sig.sign(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun verify(payload: Map<String, Any?>, sigB64: String): Boolean = try {
        val sig = Signature.getInstance("SHA256withRSA", "BC")
        sig.initVerify(loadPublicKey())
        sig.update(AppStorage.canonicalJson(payload))
        sig.verify(Base64.decode(sigB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
    } catch (e: Exception) { false }

    fun encodeActivation(payload: Map<String, Any?>, sigB64: String): String {
        val blob = AppStorage.gson.toJson(mapOf("p" to payload, "s" to sigB64))
            .toByteArray(Charsets.UTF_8)
        val def = Deflater(9).also { it.setInput(blob); it.finish() }
        val buf = ByteArray(blob.size * 2 + 64)
        val n   = def.deflate(buf)
        def.end()
        val token = Base64.encodeToString(
            buf.copyOf(n), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "${config.activationPrefix}-${token.chunked(24).joinToString(".")}"
    }

    fun generate(
        tier: String, source: String, deviceCode: String,
        customerName: String = "", customerEmail: String = "",
        label: String = "", note: String = "", expiry: String = ""
    ): LicenseRecord {
        if (!hasAuthority()) throw RuntimeException("No authority loaded.")
        if (deviceCode.isBlank()) throw IllegalArgumentException("Device code required.")

        val charset = "0123456789ABCDEF"
        val lid = "${config.licensePrefix}-${(1..8).map { charset.random() }.joinToString("")}"
        val now = AppStorage.utcNow()

        val payload = mutableMapOf<String, Any?>(
            "app"            to config.bundleApp,
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

        val sigB64 = sign(payload)
        val code   = encodeActivation(payload, sigB64)

        val rec = LicenseRecord(
            licenseId      = lid,
            tier           = tier,
            source         = source,
            paymentMethod  = source,
            customerName   = customerName,
            customerEmail  = customerEmail,
            deviceCode     = deviceCode.uppercase(),
            label          = label,
            customerNote   = note,
            expiry         = expiry,
            expiresAt      = expiry,
            issuedAt       = now,
            status         = "active",
            activationCode = code,
            signatureValid = verify(payload, sigB64)
        )

        val updated = mutableListOf(rec).also { it.addAll(loadRecords()) }
        AppStorage.saveJson(dbPath(), updated)
        AppStorage.logActivity(
            "License Generated",
            "ID: $lid  Tier: ${tier.uppercase()}  Customer: ${customerName.ifBlank { "N/A" }}",
            config.displayName
        )
        return rec
    }

    fun loadRecords(): List<LicenseRecord> =
        AppStorage.loadJson(dbPath(), emptyList<LicenseRecord>())

    fun saveRecords(r: List<LicenseRecord>) = AppStorage.saveJson(dbPath(), r)

    fun stats(): Triple<Int, Int, Int> {
        val r = loadRecords()
        return Triple(
            r.count { it.status == "active" },
            r.count { it.status == "revoked" },
            r.size
        )
    }

    fun toggleRevoke(lid: String): String {
        val recs = loadRecords().toMutableList()
        val idx  = recs.indexOfFirst { it.licenseId == lid }
        if (idx == -1) return "not_found"
        val newStatus = if (recs[idx].status == "active") "revoked" else "active"
        recs[idx] = recs[idx].copy(
            status    = newStatus,
            revokedAt = if (newStatus == "revoked") AppStorage.utcNow() else ""
        )
        saveRecords(recs)
        return newStatus
    }

    fun deleteLicense(lid: String): Boolean {
        val recs = loadRecords().toMutableList()
        val before = recs.size
        recs.removeAll { it.licenseId == lid }
        return if (recs.size < before) { saveRecords(recs); true } else false
    }

    fun buildBackupBlob(password: String, bundleType: String, payload: Any): String {
        if (password.isBlank()) throw IllegalArgumentException("Password required.")
        val raw  = AppStorage.canonicalJson(
            mapOf("schema" to 2, "app" to config.bundleApp,
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
            "app"         to config.bundleApp,
            "bundle_type" to bundleType,
            "salt"        to Base64.encodeToString(salt, enc),
            "ciphertext"  to Base64.encodeToString(ct,   enc),
            "mac"         to Base64.encodeToString(mac,  enc)
        ))
    }

    private fun xorStream(key: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var offset = 0
        var ctr = 0
        while (offset < data.size) {
            val ctrBytes = byteArrayOf(
                (ctr shr 24).toByte(), (ctr shr 16).toByte(),
                (ctr shr 8).toByte(), ctr.toByte()
            )
            val block = MessageDigest.getInstance("SHA-256").digest(key + ctrBytes)
            val take  = minOf(block.size, data.size - offset)
            for (i in 0 until take)
                out[offset + i] = (data[offset + i].toInt() xor block[i].toInt()).toByte()
            offset += take
            ctr++
        }
        return out
    }

    fun saveAuthBackupFile(blob: String) =
        File(authBackupDir(), "${config.id}_auth_${AppStorage.timestamp()}.ctp")
            .also { it.writeText(blob) }

    fun saveListBackupFile(blob: String) =
        File(listBackupDir(), "${config.id}_list_${AppStorage.timestamp()}.ctlist")
            .also { it.writeText(blob) }

    fun saveFullBackupFile(blob: String) =
        File(fullBackupDir(), "${config.id}_full_${AppStorage.timestamp()}.ctfull")
            .also { it.writeText(blob) }
}

object ProductRegistry {
    private fun file() = AppStorage.legacyProductsRegistryFile()

    fun all(): List<ProductConfig> = AppStorage.loadJson(file(), emptyList<ProductConfig>())
    fun get(id: String) = all().find { it.id == id }
    fun add(c: ProductConfig) = AppStorage.saveJson(file(), all().filter { it.id != c.id } + c)
    fun remove(id: String)    = AppStorage.saveJson(file(), all().filter { it.id != id })
}

object EngineCache {
    private val cache = mutableMapOf<String, LicenseEngine>()
    fun get(c: ProductConfig) = cache.getOrPut(c.id) { LicenseEngine(c) }
    fun invalidate(id: String) = cache.remove(id)
}
