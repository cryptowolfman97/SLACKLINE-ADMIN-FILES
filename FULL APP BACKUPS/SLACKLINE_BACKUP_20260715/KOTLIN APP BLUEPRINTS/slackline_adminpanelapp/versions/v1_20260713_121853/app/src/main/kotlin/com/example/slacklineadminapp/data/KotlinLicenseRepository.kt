package com.example.slacklineadminapp.data

import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object KotlinLicenseRepository {
    private val gson = Gson()
    private val rootDir = AppStorage.kotlinLicensesDir()

    private val productsFile = File(rootDir, "kotlin_products.json")

    // ── SUPABASE CLOUD CREDENTIALS ────────────────────────────────────
    private const val SUPABASE_URL  = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    // Read-only — safe in patched APKs
    private const val ANON_KEY      = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"
    // Admin write key — only used here in the admin app, never in patched APKs
    private const val SERVICE_KEY   = "sb_secret_HRCqR6Is4zwBBdEesGKgFg_FgtRb3St"

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    private fun getLicensesFile(productId: String) = File(rootDir, "licenses_$productId.json")

    @Synchronized
    fun saveProduct(product: KotlinProduct) {
        // 1. Safe Local Save
        val products = getAllProducts().toMutableList()
        val index = products.indexOfFirst { it.id == product.id }
        if (index != -1) {
            products[index] = product
        } else {
            products.add(product)
        }
        productsFile.writeText(gson.toJson(products))

        // 2. Fire-and-Forget Cloud Sync
        syncProductToCloud(product)
    }

    @Synchronized
    fun getAllProducts(): List<KotlinProduct> {
        if (!productsFile.exists()) return emptyList()
        val type = object : TypeToken<List<KotlinProduct>>() {}.type
        return try {
            gson.fromJson(productsFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveLicense(license: KotlinLicense) {
        // 1. Safe Local Save
        val file = getLicensesFile(license.productId)
        val licenses = getLicensesForProduct(license.productId).toMutableList()
        val index = licenses.indexOfFirst { it.id == license.id }
        if (index != -1) {
            licenses[index] = license
        } else {
            licenses.add(0, license)
        }
        file.writeText(gson.toJson(licenses))

        // 2. Fire-and-Forget Cloud Sync
        syncLicenseToCloud(license)
    }

    @Synchronized
    fun getLicensesForProduct(productId: String): List<KotlinLicense> {
        val file = getLicensesFile(productId)
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<KotlinLicense>>() {}.type
        return try {
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun deleteProduct(productId: String) {
        // 1. Safe Local Delete
        val products = getAllProducts().filter { it.id != productId }
        productsFile.writeText(gson.toJson(products))

        val licensesFile = getLicensesFile(productId)
        if (licensesFile.exists()) {
            licensesFile.delete()
        }

        // 2. Fire-and-Forget Cloud Delete (product + all its licenses)
        deleteProductFromCloud(productId)
    }

    // ── BACKGROUND CLOUD SYNC LOGIC ───────────────────────────────────

    private fun syncProductToCloud(product: KotlinProduct) {
        Thread {
            try {
                val url = URL("$SUPABASE_URL/rest/v1/kl_products?on_conflict=product_id")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SERVICE_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SERVICE_KEY")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("product_id", product.id)
                    put("display_name", product.name)
                    put("prefix", product.prefix)
                    put("bundle_app", product.appCode)
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.readText()
                conn.disconnect()
            } catch (e: Exception) {
                // Silently drop errors to protect local stability
            }
        }.start()
    }

    private fun syncLicenseToCloud(license: KotlinLicense) {
        Thread {
            try {
                val url = URL("$SUPABASE_URL/rest/v1/kl_licenses?on_conflict=license_id")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SERVICE_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SERVICE_KEY")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.doOutput = true

                val newStatus = if (license.isRevoked) "revoked" else "active"
                val payload = JSONObject().apply {
                    put("license_id", license.id)
                    put("product_id", license.productId)
                    put("tier", license.licenseType)
                    put("device_code", license.deviceCode)
                    put("customer_name", license.customerName)
                    put("customer_email", license.customerEmail)
                    put("status", newStatus)
                    // Set revoked_at timestamp when revoking
                    if (license.isRevoked) {
                        put("revoked_at", java.time.Instant.now().toString())
                    }
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode

                // If revoked, also push a record to kl_revocations
                if (license.isRevoked) {
                    pushToRevocationsTable(license)
                }
            } catch (e: Exception) {
                // Silently drop errors
            }
        }.start()
    }

    private fun pushToRevocationsTable(license: KotlinLicense) {
        try {
            val url = URL("$SUPABASE_URL/rest/v1/kl_revocations")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SERVICE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SERVICE_KEY")
            // Ignore duplicate revocations gracefully
            conn.setRequestProperty("Prefer", "resolution=ignore-duplicates")
            conn.doOutput = true

            // kl_revocations schema: product_id, payload (jsonb), signature (text NOT NULL)
            // We store license_id inside the payload jsonb so the client can query it.
            // signature is required — we store the license_id as a simple reference token.
            val payload = JSONObject().apply {
                put("product_id", license.productId)
                put("payload", JSONObject().apply {
                    put("license_id", license.id)
                    put("device_code", license.deviceCode)
                    put("revoked_at", java.time.Instant.now().toString())
                })
                // signature column is NOT NULL — store license id as the revocation reference
                put("signature", license.id)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            conn.responseCode
        } catch (e: Exception) { }
    }

    private fun deleteProductFromCloud(productId: String) {
        Thread {
            // Each delete is isolated — one failure won't block the rest
            cloudDelete("$SUPABASE_URL/rest/v1/kl_licenses?product_id=eq.$productId")
            cloudDelete("$SUPABASE_URL/rest/v1/kl_revocations?product_id=eq.$productId")
            cloudDelete("$SUPABASE_URL/rest/v1/kl_demo_sessions?product_id=eq.$productId")
            cloudDelete("$SUPABASE_URL/rest/v1/kl_products?product_id=eq.$productId")
        }.start()
    }

    // Executes a DELETE and fully drains the response stream so the connection
    // doesn't hang or throw on a 4xx — each call is fully independent.
    private fun cloudDelete(urlStr: String) {
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", SERVICE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SERVICE_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            // Always drain whichever stream is available to avoid connection leaks
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.readText()
            conn.disconnect()
        } catch (e: Exception) {
            // Silently drop — local delete already succeeded
        }
    }
}
