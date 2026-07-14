package com.example.omnicortex.engine

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BreachEngine {

    const val HIBP_BASE = "https://haveibeenpwned.com/api/v3"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Serializable
    data class HibpBreach(
        val Name: String = "",
        val Domain: String = "",
        val BreachDate: String = "",
        val DataClasses: List<String> = emptyList(),
        val Description: String = ""
    )

    data class BreachItem(
        val name: String,
        val domain: String,
        val breachDate: String,
        val dataClasses: List<String>,
        val description: String
    )

    // ── Check a single email against HIBP ─────────────────────────────────────
    // apiKey is empty until user adds one — returns empty list gracefully
    suspend fun checkEmail(email: String, apiKey: String): Result<List<BreachItem>> {
        if (apiKey.isBlank()) return Result.success(emptyList())
        return try {
            val response: HttpResponse = client.get("$HIBP_BASE/breachedaccount/$email") {
                parameter("truncateResponse", "false")
                header("hibp-api-key", apiKey)
                header("User-Agent", "OmniCortex-SHV")
            }
            if (response.status.value == 404) {
                // 404 = no breaches found — not an error
                return Result.success(emptyList())
            }
            val breaches = json.decodeFromString<List<HibpBreach>>(response.bodyAsText())
            Result.success(breaches.map {
                BreachItem(it.Name, it.Domain, it.BreachDate, it.DataClasses, it.Description)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Get all known breaches for a domain ───────────────────────────────────
    suspend fun checkDomain(domain: String, apiKey: String): Result<List<BreachItem>> {
        if (apiKey.isBlank()) return Result.success(emptyList())
        return try {
            val response: HttpResponse = client.get("$HIBP_BASE/breaches") {
                parameter("domain", domain)
                header("hibp-api-key", apiKey)
                header("User-Agent", "OmniCortex-SHV")
            }
            val breaches = json.decodeFromString<List<HibpBreach>>(response.bodyAsText())
            Result.success(breaches.map {
                BreachItem(it.Name, it.Domain, it.BreachDate, it.DataClasses, it.Description)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
