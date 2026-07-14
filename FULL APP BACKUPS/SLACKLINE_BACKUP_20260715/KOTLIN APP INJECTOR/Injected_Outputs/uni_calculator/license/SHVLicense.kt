package com.shvertex.universalconv.license

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.Inflater

object SHVLicense {

    private const val BUNDLE_APP   = "uni_calculator"
    private const val ACT_PREFIX   = "UNICALC2026"
    private const val LICENSE_FILE = "shv_license_uni_calculator.json"
    private const val SUPABASE_URL = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    private const val ANON_KEY     = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    private val PUBLIC_KEY_PEM = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAskinM+pSxhUoOKDS9/zX
hE8ip5FoFLP81UYTgDEY+EePbqRu9sp4qqndNbF/l2cIwwRWsH6rBIEiqkuD+n8i
GqqFlFJJRbGAMMhOjQdTLBmPGSLnEPYnYE28W/sIN6vW3gKRrJVrhHAoQqiBsXdq
/7xTsY8/tWnqKTgflB2/oHHxoPjIVrC/4h4h0R34Dfa+Uw4WyU4Hab8cL6pzNVGY
Ccb3zL3SNt6fQZ0ODvxoFbiXm7Nnal6CgsutU3lW3Ma9lSyWDFckTfOPmUkZhybO
tcY0jslMku0Ye63pB56Bpxx3oDcbi2u3bQC/SC79er7VW9yTO0eRpO2rs+mFr1uq
kwIDAQAB
-----END RSA PUBLIC KEY-----
    """.trimIndent()

    fun getDeviceCode(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "fallback"
        return MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(8).uppercase()
    }

    fun decodeTokenPublic(code: String): Pair<JSONObject, String> {
        val prefix  = "${ACT_PREFIX}-"
        var cleaned = code.trim().replace("\n", "").replace(" ", "")
        if (cleaned.startsWith(prefix)) cleaned = cleaned.removePrefix(prefix)
        cleaned = cleaned.replace(".", "")
        val padded     = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
        val compressed = Base64.getUrlDecoder().decode(padded)
        val inflater   = Inflater(); inflater.setInput(compressed)
        val output = ByteArray(65536); val len = inflater.inflate(output); inflater.end()
        val json = JSONObject(String(output, 0, len, Charsets.UTF_8))
        return Pair(json.getJSONObject("p"), json.getString("s"))
    }

    private fun verify(payload: JSONObject, sigB64: String): Boolean {
        val canonical   = buildCanonicalJson(payload)
        val pemStripped = PUBLIC_KEY_PEM
            .replace(Regex("-----.*?-----"), "")
            .replace(Regex("\\s+"), "")
        val keyBytes = try { Base64.getDecoder().decode(pemStripped) }
                       catch (e: Exception) { throw Exception("Key Base64 decode failed") }
        val pubKey = try { KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes)) }
                     catch (e: Exception) {
                         try { KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(wrapPkcs1InX509(keyBytes))) }
                         catch (e2: Exception) { throw Exception("KeyFactory parsing failed") }
                     }
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(pubKey)
        sig.update(canonical.toByteArray(Charsets.UTF_8))
        val sigBytes = try { Base64.getUrlDecoder().decode(sigB64) }
                       catch (e: Exception) {
                           try { Base64.getDecoder().decode(sigB64) }
                           catch (e2: Exception) { throw Exception("Signature Base64 decode failed") }
                       }
        return sig.verify(sigBytes)
    }

    private fun wrapPkcs1InX509(pkcs1: ByteArray): ByteArray {
        val oid = byteArrayOf(0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,
            0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00)
        return derEncode(0x30, oid + derEncode(0x03, byteArrayOf(0x00) + pkcs1))
    }

    private fun derEncode(tag: Int, content: ByteArray): ByteArray {
        val len = content.size
        val lb  = when {
            len < 128 -> byteArrayOf(len.toByte())
            len < 256 -> byteArrayOf(0x81.toByte(), len.toByte())
            else      -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xff).toByte())
        }
        return byteArrayOf(tag.toByte()) + lb + content
    }

    private fun buildCanonicalJson(obj: JSONObject): String {
        val parts = obj.keys().asSequence().sorted()
            .map { k -> "\"$k\":${canonicalValue(obj.get(k))}" }.toList()
        return "{${parts.joinToString(",")}}"
    }

    private fun canonicalValue(value: Any?): String = when (value) {
        is JSONObject -> buildCanonicalJson(value)
        is String     -> "\"$value\""
        is Boolean    -> if (value) "true" else "false"
        null, JSONObject.NULL -> "null"
        else          -> value.toString()
    }

    fun isProductActive(productId: String): Boolean {
        return try {
            val url = "$SUPABASE_URL/rest/v1/kl_products" +
                "?product_id=eq.${productId}&select=product_id&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $ANON_KEY")
            conn.connectTimeout = 8_000; conn.readTimeout = 8_000
            val code = conn.responseCode
            val resp = if (code in 200..299) conn.inputStream.bufferedReader().readText() else "[]"
            conn.disconnect()
            org.json.JSONArray(resp).length() > 0
        } catch (e: Exception) { true }
    }

    fun isRevokedOnServer(licenseId: String): Boolean {
        return try {
            val licUrl = "$SUPABASE_URL/rest/v1/kl_licenses" +
                "?license_id=eq.${licenseId}&select=status&limit=1"
            val licConn = URL(licUrl).openConnection() as HttpURLConnection
            licConn.setRequestProperty("apikey", ANON_KEY)
            licConn.setRequestProperty("Authorization", "Bearer $ANON_KEY")
            licConn.connectTimeout = 8_000; licConn.readTimeout = 8_000
            val licResp = licConn.inputStream.bufferedReader().readText()
            val licArr  = JSONArray(licResp)
            if (licArr.length() > 0) {
                val status = licArr.getJSONObject(0).optString("status", "active")
                if (status == "revoked") return true
            }
            val revUrl = "$SUPABASE_URL/rest/v1/kl_revocations" +
                "?payload->>license_id=eq.${licenseId}&select=id&limit=1"
            val revConn = URL(revUrl).openConnection() as HttpURLConnection
            revConn.setRequestProperty("apikey", ANON_KEY)
            revConn.setRequestProperty("Authorization", "Bearer $ANON_KEY")
            revConn.connectTimeout = 8_000; revConn.readTimeout = 8_000
            val revResp = revConn.inputStream.bufferedReader().readText()
            JSONArray(revResp).length() > 0
        } catch (e: Exception) { false }
    }

    data class LicenseResult(
        val valid: Boolean,
        val tier: String = "",
        val message: String = "",
        val licenseId: String = "",
        val revoked: Boolean = false
    )

    fun checkLicense(code: String, context: Context, checkRevocation: Boolean = false): LicenseResult {
        if (code.isBlank()) return LicenseResult(false, message = "No activation code.")
        val deviceCode = getDeviceCode(context)
        return try {
            val (payload, sigB64) = decodeTokenPublic(code)
            val isVerified = try { verify(payload, sigB64) }
                             catch (e: Exception) { return LicenseResult(false, message = "Crash: ${e.message}") }
            if (!isVerified) return LicenseResult(false, message = "Signature invalid.")
            if (payload.optString("app").lowercase() != BUNDLE_APP.lowercase())
                return LicenseResult(false, message = "Wrong product.")
            val bound = payload.optString("device_code").trim().uppercase()
            if (bound.isNotEmpty() && bound != deviceCode.uppercase())
                return LicenseResult(false, message = "Device mismatch. Yours: $deviceCode")
            val expiry = payload.optString("expires_at").ifBlank { payload.optString("expiry") }
            if (expiry.isNotBlank()) {
                try {
                    val exp = java.time.Instant.parse(
                        expiry.replace(" ", "T").let { if (!it.endsWith("Z")) "${it}Z" else it }
                    )
                    if (java.time.Instant.now().isAfter(exp))
                        return LicenseResult(false, message = "License expired.")
                } catch (e: Exception) { }
            }
            val licId = payload.optString("license_id")
            if (checkRevocation && licId.isNotBlank()) {
                if (isRevokedOnServer(licId))
                    return LicenseResult(false, message = "License has been revoked.", revoked = true)
            }
            LicenseResult(true, payload.optString("tier", "pro").lowercase(), "License verified.", licId)
        } catch (e: Exception) {
            LicenseResult(false, message = "Decode error: ${e.message}")
        }
    }

    fun checkOnStartup(context: Context): LicenseResult {
        val saved = loadLicense(context) ?: return LicenseResult(false, message = "No license.")
        if (!isProductActive(saved.optString("product_id", BUNDLE_APP))) {
            deleteLicense(context)
            return LicenseResult(false, message = "Product no longer available.")
        }
        val result = checkLicense(saved.optString("activation_code"), context, checkRevocation = true)
        if (result.revoked) deleteLicense(context)
        return result
    }

    fun saveLicense(context: Context, code: String, payload: JSONObject) {
        File(context.filesDir, LICENSE_FILE).writeText(JSONObject().apply {
            put("activation_code", code)
            put("license_id", payload.optString("license_id"))
            put("product_id", BUNDLE_APP)
            put("tier", payload.optString("tier", "pro"))
            put("payload", payload)
            put("saved_at", java.time.Instant.now().toString())
        }.toString())
    }

    fun loadLicense(context: Context): JSONObject? = try {
        val f = File(context.filesDir, LICENSE_FILE)
        if (f.exists()) JSONObject(f.readText()) else null
    } catch (e: Exception) { null }

    fun deleteLicense(context: Context) { File(context.filesDir, LICENSE_FILE).delete() }
}