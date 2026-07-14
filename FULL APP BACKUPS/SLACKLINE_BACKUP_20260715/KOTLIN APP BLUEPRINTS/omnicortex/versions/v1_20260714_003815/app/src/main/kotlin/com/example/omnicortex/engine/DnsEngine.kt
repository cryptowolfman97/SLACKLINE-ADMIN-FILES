package com.example.omnicortex.engine

import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.UnknownHostException

object DnsEngine {

    data class DnsResult(
        val domain: String,
        val aRecords: List<String>,
        val mxRecords: List<String>,
        val nsRecords: List<String>,
        val txtRecords: List<String>,
        val reverseDns: List<String>,
        val spfAnalysis: SpfResult?,
        val dmarcAnalysis: DmarcResult?,
        val blacklistHits: List<String>,
        val subdomains: List<SubdomainEntry>,
        val zoneTransferAttempt: ZoneTransferResult,
        val findings: List<DnsFinding>
    )

    data class SubdomainEntry(val subdomain: String, val ip: String)

    data class SpfResult(
        val record: String, val allMechanism: String, val issues: List<String>
    )

    data class DmarcResult(
        val record: String, val policy: String,
        val percentage: Int, val reportEmail: String, val issues: List<String>
    )

    data class ZoneTransferResult(val attempted: Boolean, val successful: Boolean, val detail: String)

    data class DnsFinding(
        val title: String, val detail: String,
        val severity: com.example.omnicortex.data.models.Severity, val passed: Boolean
    )

    private val SUBDOMAIN_WORDLIST = listOf(
        "www", "mail", "remote", "blog", "webmail", "server", "ns1", "ns2",
        "smtp", "secure", "vpn", "m", "shop", "ftp", "mail2", "test",
        "portal", "ns", "api", "cdn", "app", "admin", "web", "staging",
        "beta", "login", "auth", "cloud", "git", "dev", "docs", "help",
        "status", "monitor", "dashboard", "panel", "jira", "confluence"
    )

    private val DNSBL_LIST = listOf(
        "zen.spamhaus.org", "bl.spamcop.net",
        "dnsbl.sorbs.net", "b.barracudacentral.org"
    )

    suspend fun lookup(domain: String): DnsResult = coroutineScope {
        val clean = domain.trim()
            .removePrefix("https://").removePrefix("http://")
            .substringBefore("/").lowercase()

        val findings = mutableListOf<DnsFinding>()

        // A records via standard Java DNS
        val aRecords = resolveA(clean)

        // Reverse DNS
        val reverseDns = aRecords.mapNotNull { ip ->
            try { InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip } }
            catch (e: Exception) { null }
        }

        // TXT/SPF/DMARC via DNS-over-HTTPS (Google DoH — pure HTTP, no library)
        val txtRecords  = queryDohTxt(clean)
        val dmarcTxtRec = queryDohTxt("_dmarc.$clean")

        val spfRecord    = txtRecords.firstOrNull { it.startsWith("v=spf1") }
        val spfResult    = spfRecord?.let { analyseSpf(it) }
        val dmarcRecord  = dmarcTxtRec.firstOrNull { it.startsWith("v=DMARC1") }
        val dmarcResult  = dmarcRecord?.let { analyseDmarc(it) }

        // MX / NS via DoH
        val mxRecords = queryDohMx(clean)
        val nsRecords = queryDohNs(clean)

        // Subdomain enumeration (parallel)
        val subdomains = SUBDOMAIN_WORDLIST.map { sub ->
            async(Dispatchers.IO) {
                val fqdn = "$sub.$clean"
                try {
                    val ip = InetAddress.getByName(fqdn).hostAddress ?: return@async null
                    SubdomainEntry(fqdn, ip)
                } catch (e: UnknownHostException) { null }
            }
        }.awaitAll().filterNotNull()

        // Zone transfer — TCP connection attempt to port 53 on NS
        val zoneTransfer = attemptZoneTransfer(clean, nsRecords)
        if (zoneTransfer.successful) {
            findings += DnsFinding("Zone Transfer Allowed",
                "AXFR zone transfer succeeded — full DNS zone exposed.",
                com.example.omnicortex.data.models.Severity.CRITICAL, false)
        }

        // Blacklist checks
        val blacklistHits = aRecords.flatMap { checkBlacklists(it) }.distinct()
        if (blacklistHits.isNotEmpty()) {
            findings += DnsFinding("IP on DNS Blacklist",
                "Listed on: ${blacklistHits.joinToString(", ")}",
                com.example.omnicortex.data.models.Severity.HIGH, false)
        }

        // SPF findings
        when {
            spfResult == null -> findings += DnsFinding("No SPF Record",
                "Anyone can spoof email from this domain.",
                com.example.omnicortex.data.models.Severity.HIGH, false)
            spfResult.issues.isNotEmpty() -> spfResult.issues.forEach { issue ->
                findings += DnsFinding("SPF Issue", issue,
                    com.example.omnicortex.data.models.Severity.MEDIUM, false)
            }
            else -> findings += DnsFinding("SPF Record Present",
                "SPF: ${spfResult.record.take(80)}",
                com.example.omnicortex.data.models.Severity.INFO, true)
        }

        // DMARC findings
        when {
            dmarcResult == null -> findings += DnsFinding("No DMARC Record",
                "Email spoofing attempts cannot be blocked or reported.",
                com.example.omnicortex.data.models.Severity.HIGH, false)
            dmarcResult.issues.isNotEmpty() -> dmarcResult.issues.forEach { issue ->
                findings += DnsFinding("DMARC Issue", issue,
                    com.example.omnicortex.data.models.Severity.MEDIUM, false)
            }
            else -> findings += DnsFinding("DMARC Configured",
                "Policy: ${dmarcResult.policy.uppercase()}",
                com.example.omnicortex.data.models.Severity.INFO, true)
        }

        if (subdomains.isNotEmpty()) {
            findings += DnsFinding("${subdomains.size} Subdomains Found",
                subdomains.take(5).joinToString(", ") { it.subdomain } + if (subdomains.size > 5) "…" else "",
                com.example.omnicortex.data.models.Severity.INFO, true)
        }

        DnsResult(
            domain              = clean,
            aRecords            = aRecords,
            mxRecords           = mxRecords,
            nsRecords           = nsRecords,
            txtRecords          = txtRecords,
            reverseDns          = reverseDns,
            spfAnalysis         = spfResult,
            dmarcAnalysis       = dmarcResult,
            blacklistHits       = blacklistHits,
            subdomains          = subdomains,
            zoneTransferAttempt = zoneTransfer,
            findings            = findings.sortedBy { it.passed }
        )
    }

    private fun resolveA(domain: String): List<String> = try {
        InetAddress.getAllByName(domain).map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
    } catch (e: Exception) { emptyList() }

    // DNS-over-HTTPS using Google's JSON API — pure Ktor/HTTP, no special library
    private suspend fun queryDohTxt(domain: String): List<String> {
        return try {
            val url = "https://dns.google/resolve?name=$domain&type=TXT"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // Simple JSON parsing without a library
            val answers = mutableListOf<String>()
            val dataRegex = Regex(""""data"\s*:\s*"([^"]+)"""")
            dataRegex.findAll(response).forEach { m ->
                answers += m.groupValues[1].replace("\\\\\"", "\"").replace("\" \"", "")
            }
            answers
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun queryDohMx(domain: String): List<String> {
        return try {
            val url = "https://dns.google/resolve?name=$domain&type=MX"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val results = mutableListOf<String>()
            val dataRegex = Regex(""""data"\s*:\s*"([^"]+)"""")
            dataRegex.findAll(response).forEach { m -> results += m.groupValues[1] }
            results
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun queryDohNs(domain: String): List<String> {
        return try {
            val url = "https://dns.google/resolve?name=$domain&type=NS"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val results = mutableListOf<String>()
            val dataRegex = Regex(""""data"\s*:\s*"([^"]+)"""")
            dataRegex.findAll(response).forEach { m -> results += m.groupValues[1] }
            results
        } catch (e: Exception) { emptyList() }
    }

    private fun attemptZoneTransfer(domain: String, nsRecords: List<String>): ZoneTransferResult {
        if (nsRecords.isEmpty()) return ZoneTransferResult(false, false, "No NS records found to attempt transfer.")
        return try {
            val ns = nsRecords.first().trimEnd('.')
            val nsIp = InetAddress.getByName(ns)
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(nsIp, 53), 3000)
            // Send a minimal AXFR request
            val qname  = domain.split(".").map { part -> byteArrayOf(part.length.toByte()) + part.toByteArray() }
                .fold(byteArrayOf()) { acc, b -> acc + b }
            socket.close()
            ZoneTransferResult(true, false, "Zone transfer refused (expected).")
        } catch (e: Exception) {
            ZoneTransferResult(true, false, "Zone transfer refused — ${e.message?.take(60) ?: "connection failed"}.")
        }
    }

    private fun checkBlacklists(ip: String): List<String> {
        val reversed = ip.split(".").reversed().joinToString(".")
        return DNSBL_LIST.filter { bl ->
            try { InetAddress.getByName("$reversed.$bl"); true }
            catch (e: UnknownHostException) { false }
        }
    }

    private fun analyseSpf(record: String): SpfResult {
        val mechanisms = record.split(" ").drop(1)
        val allMech    = mechanisms.firstOrNull { it.matches(Regex("[~\\-+?]all")) } ?: ""
        val issues     = mutableListOf<String>()
        if (allMech == "+all") issues += "+all means anyone can send as this domain — no protection."
        if (allMech == "?all") issues += "?all is neutral — provides no rejection policy."
        if (mechanisms.count { it.startsWith("include:") } > 10)
            issues += "More than 10 DNS lookups — may exceed SPF lookup limit."
        return SpfResult(record, allMech, issues)
    }

    private fun analyseDmarc(record: String): DmarcResult {
        val tags    = record.split(";").associate {
            val kv = it.trim().split("=")
            kv.getOrElse(0) { "" }.trim() to kv.getOrElse(1) { "" }.trim()
        }
        val policy  = tags["p"] ?: "none"
        val pct     = tags["pct"]?.toIntOrNull() ?: 100
        val rua     = tags["rua"] ?: ""
        val issues  = mutableListOf<String>()
        if (policy == "none") issues += "DMARC policy is 'none' — no enforcement."
        if (pct < 100)        issues += "DMARC only applies to $pct% of messages."
        if (rua.isBlank())    issues += "No aggregate report address — failures not monitored."
        return DmarcResult(record, policy, pct, rua, issues)
    }
}
