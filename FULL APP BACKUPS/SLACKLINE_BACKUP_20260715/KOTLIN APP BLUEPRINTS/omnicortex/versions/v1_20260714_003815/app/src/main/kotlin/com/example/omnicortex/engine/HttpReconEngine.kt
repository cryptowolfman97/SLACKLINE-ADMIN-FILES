package com.example.omnicortex.engine

import com.example.omnicortex.data.models.Severity
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object HttpReconEngine {

    data class HttpReconResult(
        val url: String,
        val finalUrl: String,
        val statusCode: Int,
        val responseTimeMs: Long,
        val serverBanner: String,
        val techStack: List<String>,
        val headers: List<HeaderEntry>,
        val securityHeaders: List<SecurityHeaderCheck>,
        val cookieIssues: List<CookieIssue>,
        val corsResult: CorsResult?,
        val allowedMethods: List<String>,
        val redirectChain: List<RedirectHop>,
        val grade: String,
        val findings: List<HttpFinding>
    )

    data class HeaderEntry(val name: String, val value: String)

    data class SecurityHeaderCheck(
        val header: String,
        val present: Boolean,
        val value: String,
        val severity: Severity,
        val recommendation: String
    )

    data class CookieIssue(
        val cookieName: String,
        val missingFlags: List<String>,
        val severity: Severity
    )

    data class CorsResult(
        val allowsAnyOrigin: Boolean,
        val allowsCredentials: Boolean,
        val reflectsOrigin: Boolean,
        val severity: Severity
    )

    data class RedirectHop(
        val url: String,
        val statusCode: Int,
        val isHttpToHttps: Boolean
    )

    data class HttpFinding(
        val title: String,
        val detail: String,
        val severity: Severity,
        val passed: Boolean
    )

    private val client = HttpClient(Android) {
        followRedirects = false
        engine {
            connectTimeout = 10_000
            socketTimeout  = 10_000
        }
    }

    private val noRedirectClient = HttpClient(Android) {
        followRedirects = false
        engine { connectTimeout = 8_000; socketTimeout = 8_000 }
    }

    suspend fun analyse(rawUrl: String): HttpReconResult {
        val url = normaliseUrl(rawUrl)
        val findings = mutableListOf<HttpFinding>()
        val start = System.currentTimeMillis()

        // ── Main request ──────────────────────────────────────────────────────
        val response: HttpResponse = client.get(url) {
            header("User-Agent", "OmniCortex-SHV/1.0 SecurityScanner")
        }
        val elapsed = System.currentTimeMillis() - start
        val statusCode = response.status.value
        val allHeaders = response.headers.entries()
            .map { HeaderEntry(it.key, it.value.joinToString("; ")) }

        // ── Server / tech fingerprinting ──────────────────────────────────────
        val serverBanner = response.headers["Server"] ?: ""
        val techStack    = fingerprintTech(response.headers, response.bodyAsText().take(4096))

        // ── Security headers ──────────────────────────────────────────────────
        val securityHeaders = auditSecurityHeaders(response.headers)
        securityHeaders.forEach { h ->
            findings += HttpFinding(h.header, h.recommendation, h.severity, h.present)
        }

        // ── Cookie analysis ───────────────────────────────────────────────────
        val cookieIssues = analyseCookies(response.headers)
        cookieIssues.forEach { c ->
            findings += HttpFinding(
                "Cookie \"${c.cookieName}\" missing ${c.missingFlags.joinToString(", ")}",
                "Cookie lacks security flags: ${c.missingFlags.joinToString(", ")}.",
                c.severity, false
            )
        }

        // ── CORS test ─────────────────────────────────────────────────────────
        val corsResult = testCors(url)
        corsResult?.let { cors ->
            if (cors.allowsAnyOrigin) {
                findings += HttpFinding(
                    "CORS Misconfiguration",
                    if (cors.allowsCredentials) "Access-Control-Allow-Origin: * combined with credentials — critical misconfiguration."
                    else "Access-Control-Allow-Origin: * allows any origin to read responses.",
                    if (cors.allowsCredentials) Severity.CRITICAL else Severity.HIGH,
                    false
                )
            } else if (cors.reflectsOrigin) {
                findings += HttpFinding(
                    "CORS Origin Reflection",
                    "Server reflects the Origin header back, accepting any origin dynamically.",
                    Severity.HIGH, false
                )
            }
        }

        // ── HTTP method enumeration ───────────────────────────────────────────
        val allowedMethods = enumerateMethods(url)
        val dangerousMethods = allowedMethods.filter { it in listOf("PUT", "DELETE", "TRACE", "CONNECT") }
        if (dangerousMethods.isNotEmpty()) {
            findings += HttpFinding(
                "Dangerous HTTP Methods Enabled",
                "Methods ${dangerousMethods.joinToString(", ")} are enabled. TRACE enables XST attacks; PUT/DELETE allow content manipulation.",
                Severity.HIGH, false
            )
        }

        // ── Redirect chain ────────────────────────────────────────────────────
        val redirectChain = buildRedirectChain(url)
        val httpToHttps = redirectChain.any { it.isHttpToHttps }
        if (url.startsWith("http://") && !httpToHttps) {
            findings += HttpFinding(
                "No HTTP → HTTPS Redirect",
                "Plain HTTP requests are not redirected to HTTPS, allowing cleartext transmission.",
                Severity.HIGH, false
            )
        }

        // ── Info disclosure ───────────────────────────────────────────────────
        if (serverBanner.isNotBlank()) {
            findings += HttpFinding(
                "Server Version Disclosure",
                "Server header reveals: \"$serverBanner\". Version disclosure aids targeted attacks.",
                Severity.LOW, false
            )
        }

        val xPowered = response.headers["X-Powered-By"] ?: ""
        if (xPowered.isNotBlank()) {
            findings += HttpFinding(
                "Technology Disclosure (X-Powered-By)",
                "X-Powered-By header reveals: \"$xPowered\".",
                Severity.LOW, false
            )
        }

        val grade = computeGrade(findings)

        return HttpReconResult(
            url            = url,
            finalUrl       = response.request.url.toString(),
            statusCode     = statusCode,
            responseTimeMs = elapsed,
            serverBanner   = serverBanner,
            techStack      = techStack,
            headers        = allHeaders,
            securityHeaders = securityHeaders,
            cookieIssues   = cookieIssues,
            corsResult     = corsResult,
            allowedMethods = allowedMethods,
            redirectChain  = redirectChain,
            grade          = grade,
            findings       = findings.sortedBy { it.passed }
        )
    }

    private fun auditSecurityHeaders(headers: Headers): List<SecurityHeaderCheck> = listOf(
        SecurityHeaderCheck(
            "Strict-Transport-Security",
            headers["Strict-Transport-Security"] != null,
            headers["Strict-Transport-Security"] ?: "",
            if (headers["Strict-Transport-Security"] != null) Severity.INFO else Severity.HIGH,
            "Add: Strict-Transport-Security: max-age=31536000; includeSubDomains"
        ),
        SecurityHeaderCheck(
            "Content-Security-Policy",
            headers["Content-Security-Policy"] != null,
            headers["Content-Security-Policy"] ?: "",
            if (headers["Content-Security-Policy"] != null) Severity.INFO else Severity.HIGH,
            "Implement a Content-Security-Policy to prevent XSS and data injection."
        ),
        SecurityHeaderCheck(
            "X-Frame-Options",
            headers["X-Frame-Options"] != null,
            headers["X-Frame-Options"] ?: "",
            if (headers["X-Frame-Options"] != null) Severity.INFO else Severity.MEDIUM,
            "Add: X-Frame-Options: DENY or SAMEORIGIN to prevent clickjacking."
        ),
        SecurityHeaderCheck(
            "X-Content-Type-Options",
            headers["X-Content-Type-Options"] != null,
            headers["X-Content-Type-Options"] ?: "",
            if (headers["X-Content-Type-Options"] != null) Severity.INFO else Severity.MEDIUM,
            "Add: X-Content-Type-Options: nosniff"
        ),
        SecurityHeaderCheck(
            "Referrer-Policy",
            headers["Referrer-Policy"] != null,
            headers["Referrer-Policy"] ?: "",
            if (headers["Referrer-Policy"] != null) Severity.INFO else Severity.LOW,
            "Add: Referrer-Policy: strict-origin-when-cross-origin"
        ),
        SecurityHeaderCheck(
            "Permissions-Policy",
            headers["Permissions-Policy"] != null,
            headers["Permissions-Policy"] ?: "",
            if (headers["Permissions-Policy"] != null) Severity.INFO else Severity.LOW,
            "Add Permissions-Policy to restrict browser feature access."
        ),
        SecurityHeaderCheck(
            "X-XSS-Protection",
            headers["X-XSS-Protection"] != null,
            headers["X-XSS-Protection"] ?: "",
            if (headers["X-XSS-Protection"] != null) Severity.INFO else Severity.LOW,
            "Add: X-XSS-Protection: 1; mode=block (legacy browsers)"
        )
    )

    private fun analyseCookies(headers: Headers): List<CookieIssue> {
        val issues = mutableListOf<CookieIssue>()
        headers.getAll("Set-Cookie")?.forEach { raw ->
            val name    = raw.substringBefore("=").trim()
            val missing = mutableListOf<String>()
            if (!raw.contains("HttpOnly", ignoreCase = true)) missing += "HttpOnly"
            if (!raw.contains("Secure", ignoreCase = true))   missing += "Secure"
            if (!raw.contains("SameSite", ignoreCase = true)) missing += "SameSite"
            if (missing.isNotEmpty()) {
                val sev = if ("HttpOnly" in missing && "Secure" in missing) Severity.HIGH else Severity.MEDIUM
                issues += CookieIssue(name, missing, sev)
            }
        }
        return issues
    }

    private suspend fun testCors(url: String): CorsResult? = try {
        val resp = noRedirectClient.get(url) {
            header("Origin", "https://evil.omnicortex.test")
            header("User-Agent", "OmniCortex-SHV/1.0")
        }
        val acao = resp.headers["Access-Control-Allow-Origin"] ?: ""
        val acac = resp.headers["Access-Control-Allow-Credentials"] ?: ""
        CorsResult(
            allowsAnyOrigin    = acao == "*",
            allowsCredentials  = acac.equals("true", ignoreCase = true),
            reflectsOrigin     = acao == "https://evil.omnicortex.test",
            severity           = when {
                (acao == "*" || acao == "https://evil.omnicortex.test") &&
                        acac.equals("true", ignoreCase = true) -> Severity.CRITICAL
                acao == "*" || acao == "https://evil.omnicortex.test" -> Severity.HIGH
                else -> Severity.INFO
            }
        )
    } catch (e: Exception) { null }

    private suspend fun enumerateMethods(url: String): List<String> {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "TRACE", "PATCH", "HEAD")
        val allowed = mutableListOf<String>()
        // Check OPTIONS first — server may advertise allowed methods
        try {
            val optResp = noRedirectClient.options(url) {
                header("User-Agent", "OmniCortex-SHV/1.0")
            }
            val allow = optResp.headers["Allow"] ?: ""
            if (allow.isNotBlank()) {
                return allow.split(",").map { it.trim().uppercase() }.filter { it in methods }
            }
        } catch (e: Exception) { /* fall through to individual checks */ }
        // Individual method checks
        methods.forEach { method ->
            try {
                val resp = noRedirectClient.request(url) {
                    this.method = HttpMethod(method)
                    header("User-Agent", "OmniCortex-SHV/1.0")
                }
                if (resp.status.value !in listOf(405, 501)) allowed += method
            } catch (e: Exception) { /* not allowed */ }
        }
        return allowed
    }

    private suspend fun buildRedirectChain(url: String): List<RedirectHop> {
        val chain = mutableListOf<RedirectHop>()
        var current = url
        repeat(10) {
            try {
                val resp = noRedirectClient.get(current) {
                    header("User-Agent", "OmniCortex-SHV/1.0")
                }
                val code = resp.status.value
                chain += RedirectHop(
                    url          = current,
                    statusCode   = code,
                    isHttpToHttps = current.startsWith("http://")
                )
                if (code in 300..399) {
                    current = resp.headers["Location"] ?: return chain
                    if (!current.startsWith("http")) return chain
                } else return chain
            } catch (e: Exception) { return chain }
        }
        return chain
    }

    private fun fingerprintTech(headers: Headers, body: String): List<String> {
        val tech = mutableListOf<String>()
        val server     = headers["Server"] ?: ""
        val powered    = headers["X-Powered-By"] ?: ""
        val generator  = body.substringAfter("name=\"generator\"", "").substringBefore("\"").take(50)

        if (server.contains("nginx", ignoreCase = true)) tech += "Nginx"
        if (server.contains("apache", ignoreCase = true)) tech += "Apache"
        if (server.contains("IIS", ignoreCase = true)) tech += "IIS"
        if (server.contains("cloudflare", ignoreCase = true)) tech += "Cloudflare"
        if (powered.contains("PHP", ignoreCase = true)) tech += "PHP"
        if (powered.contains("ASP.NET", ignoreCase = true)) tech += "ASP.NET"
        if (powered.contains("Express", ignoreCase = true)) tech += "Express.js"
        if (body.contains("wp-content", ignoreCase = true)) tech += "WordPress"
        if (body.contains("Shopify", ignoreCase = true)) tech += "Shopify"
        if (body.contains("React", ignoreCase = true) ||
            body.contains("__NEXT_DATA__")) tech += "React/Next.js"
        if (body.contains("ng-version", ignoreCase = true)) tech += "Angular"
        if (generator.isNotBlank()) tech += generator

        return tech.distinct()
    }

    private fun computeGrade(findings: List<HttpFinding>): String {
        val crits = findings.count { !it.passed && it.severity == Severity.CRITICAL }
        val highs = findings.count { !it.passed && it.severity == Severity.HIGH }
        val meds  = findings.count { !it.passed && it.severity == Severity.MEDIUM }
        return when {
            crits >= 1  -> "F"
            highs >= 3  -> "D"
            highs >= 2  -> "C"
            highs == 1  -> "B"
            meds  >= 2  -> "B"
            meds  == 1  -> "A"
            else        -> "A+"
        }
    }

    private fun normaliseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    }
}
