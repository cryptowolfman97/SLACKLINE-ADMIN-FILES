package com.example.slacklineadminapp.data

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.Deflater

object KotlinLicenseEngine {

    data class KeyPairResult(val publicKeyPem: String, val privateKeyPem: String)

    fun generateKeyPair(): KeyPairResult {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()

        val pubBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val privBase64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)

        val pubPem = "-----BEGIN RSA PUBLIC KEY-----\n${pubBase64.chunked(64).joinToString("\n")}\n-----END RSA PUBLIC KEY-----"
        val privPem = "-----BEGIN RSA PRIVATE KEY-----\n${privBase64.chunked(64).joinToString("\n")}\n-----END RSA PRIVATE KEY-----"

        return KeyPairResult(pubPem, privPem)
    }

    fun generateLicenseKey(
        privateKeyPem: String,
        prefix: String,
        appCode: String,
        deviceCode: String,
        tier: String,
        licenseId: String
    ): String {
        // 1. Strip PEM headers to get raw PKCS8 bytes
        val privStripped = privateKeyPem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\n", "")
            .trim()

        val pkcs8Bytes = Base64.decode(privStripped, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes))

        // 2. Build payload as JSONObject so canonical serializer can sort keys
        val payload = org.json.JSONObject().apply {
            put("app", appCode)
            put("device_code", deviceCode.uppercase().trim())
            put("issued_at", System.currentTimeMillis().toString())
            put("license_id", licenseId)
            put("tier", tier.lowercase())
        }

        // 3. Sign using the SAME canonical format as the verifier (sorted keys, no spaces)
        val canonical = buildCanonicalJson(payload)
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(canonical.toByteArray(Charsets.UTF_8))
        val sigBytes = signature.sign()
        val sigB64 = Base64.encodeToString(sigBytes, Base64.URL_SAFE or Base64.NO_WRAP)

        // 4. Wrap payload + signature into blob using JSONObject (not Gson)
        val blob = org.json.JSONObject().apply {
            put("p", payload)
            put("s", sigB64)
        }
        val blobJson = blob.toString().toByteArray(Charsets.UTF_8)

        // 5. Compress
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(blobJson)
        deflater.finish()
        val outputBuffer = ByteArray(65536)
        val compressedLength = deflater.deflate(outputBuffer)
        deflater.end()
        val compressedBytes = outputBuffer.copyOf(compressedLength)

        // 6. Base64 URL_SAFE encode, strip padding
        val tokenBase64 = Base64.encodeToString(
            compressedBytes, Base64.URL_SAFE or Base64.NO_WRAP
        ).trimEnd('=')

        // 7. Chunk and prepend prefix
        val chunks = tokenBase64.chunked(24).joinToString(".")
        return "$prefix-$chunks"
    }

    // Matches SHVLicense.buildCanonicalJson exactly — sorted keys, no spaces
    private fun buildCanonicalJson(obj: org.json.JSONObject): String {
        val parts = obj.keys().asSequence().sorted()
            .map { k -> "\"$k\":${canonicalValue(obj.get(k))}" }.toList()
        return "{${parts.joinToString(",")}}"
    }

    private fun canonicalValue(value: Any?): String = when (value) {
        is org.json.JSONObject -> buildCanonicalJson(value)
        is String              -> "\"$value\""
        is Boolean             -> if (value) "true" else "false"
        null, org.json.JSONObject.NULL -> "null"
        else                   -> value.toString()
    }
}