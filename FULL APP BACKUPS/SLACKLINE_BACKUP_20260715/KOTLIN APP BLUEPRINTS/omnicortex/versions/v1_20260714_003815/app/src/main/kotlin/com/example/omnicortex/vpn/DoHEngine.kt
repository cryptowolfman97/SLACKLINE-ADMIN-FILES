package com.example.omnicortex.vpn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * DoHEngine
 *
 * Resolves DNS queries via DNS-over-HTTPS (DoH) using Cloudflare's
 * 1.1.1.1 and WARP endpoints. Also blocks known tracker/ad domains
 * at the DNS layer by returning NXDOMAIN for blocked hosts.
 */
object DoHEngine {

    private const val DOH_PRIMARY   = "https://cloudflare-dns.com/dns-query"
    private const val DOH_SECONDARY = "https://1.1.1.1/dns-query"
    private const val TAG           = "DoHEngine"
    private const val TIMEOUT_MS    = 3000

    // Simple LRU-ish cache: domain → resolved IP string
    private val cache = ConcurrentHashMap<String, CachedResponse>(256)

    // ── Tracker blocklist ──────────────────────────────────────────────────────
    // Sourced from public domain: EasyList, Steven Black hosts list (condensed)
    private val BLOCKLIST = setOf(
        // Analytics
        "google-analytics.com", "analytics.google.com", "googletagmanager.com",
        "googletagservices.com", "stats.g.doubleclick.net", "ssl.google-analytics.com",
        "segment.com", "api.segment.io", "cdn.segment.com",
        "mixpanel.com", "api.mixpanel.com",
        "amplitude.com", "api.amplitude.com", "api2.amplitude.com",
        "hotjar.com", "static.hotjar.com", "script.hotjar.com",
        "fullstory.com", "rs.fullstory.com",
        "heap.io", "heapanalytics.com",
        "mouseflow.com", "cdn.mouseflow.com",
        "clarity.ms", "www.clarity.ms",
        "crazyegg.com", "script.crazyegg.com",

        // Advertising
        "doubleclick.net", "ad.doubleclick.net", "googleadservices.com",
        "pagead2.googlesyndication.com", "adservice.google.com",
        "facebook.net", "connect.facebook.net", "an.facebook.com",
        "graph.facebook.com",
        "ads.twitter.com", "t.co", "ads-twitter.com",
        "advertising.com", "aol.com", "aolcdn.com",
        "adnxs.com", "ib.adnxs.com",
        "rubiconproject.com", "fastlane.rubiconproject.com",
        "openx.net", "openx.com",
        "outbrain.com", "widgets.outbrain.com",
        "taboola.com", "trc.taboola.com",
        "criteo.com", "static.criteo.net",
        "pubmatic.com", "ads.pubmatic.com",
        "moatads.com", "z.moatads.com",
        "scorecardresearch.com", "sb.scorecardresearch.com",
        "quantserve.com", "pixel.quantserve.com",
        "adsafeprotected.com", "pixel.adsafeprotected.com",
        "spotxchange.com", "spotx.tv",
        "casalemedia.com", "cm.g.doubleclick.net",
        "smartadserver.com", "www3.smartadserver.com",

        // Trackers / Spyware
        "gstatic.com",  // partially — fine-grained below
        "ssl.gstatic.com",
        "branch.io", "api.branch.io",
        "appsflyer.com", "launches.appsflyer.com",
        "adjust.com", "app.adjust.com",
        "kochava.com", "control.kochava.com",
        "singular.net", "sdk.singular.net",
        "firebase.io",
        "app-measurement.com",
        "crashlytics.com",
        "flurry.com", "data.flurry.com",
        "chartboost.com", "live.chartboost.com",
        "ironsrc.com", "sdk.ironSource.com",
        "mobvista.com",
        "inmobi.com", "api.inmobi.com",
        "mopub.com",
        "vungle.com", "cdn.vungle.com",
        "adcolony.com", "adc3-launch.adcolony.com",

        // Telemetry / Fingerprinting
        "sentry.io", "browser.sentry-cdn.com",
        "bugsnag.com", "notify.bugsnag.com",
        "loggly.com", "logs.loggly.com",
        "nr-data.net", "js-agent.newrelic.com",
        "datadoghq.com", "browser-intake-datadoghq.com",
        "tealiumiq.com", "tags.tiqcdn.com",
    )

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Resolves a raw DNS query payload over HTTPS.
     * Returns the raw DNS response bytes, or null on failure.
     * Blocked domains return a synthesised NXDOMAIN response.
     */
    suspend fun resolveRaw(dnsQuery: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        val domain = extractDomainFromQuery(dnsQuery) ?: return@withContext null

        if (isBlocked(domain)) {
            WarpVpnEngine.recordQuery(blocked = true)
            Log.d(TAG, "Blocked: $domain")
            return@withContext buildNxdomainResponse(dnsQuery)
        }

        WarpVpnEngine.recordQuery(blocked = false)

        // Check cache
        val cached = cache[domain]
        if (cached != null && !cached.isExpired()) {
            return@withContext cached.rawResponse
        }

        // Try primary DoH, fall back to secondary
        return@withContext try {
            val response = doHttpQuery(DOH_PRIMARY, dnsQuery)
            if (response != null) {
                cache[domain] = CachedResponse(response, ttlSeconds = 300)
                response
            } else {
                doHttpQuery(DOH_SECONDARY, dnsQuery)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH resolve failed for $domain: ${e.message}")
            doHttpQuery(DOH_SECONDARY, dnsQuery)
        }
    }

    /** High-level domain resolution returning a human-readable IP string. Used by UI. */
    suspend fun resolveDomain(domain: String): String? = withContext(Dispatchers.IO) {
        if (isBlocked(domain)) return@withContext null
        try {
            val query = buildDnsQuery(domain)
            val response = resolveRaw(query) ?: return@withContext null
            return@withContext parseARecord(response)
        } catch (e: Exception) {
            Log.w(TAG, "High-level resolve failed: ${e.message}")
            null
        }
    }

    fun isBlocked(domain: String): Boolean {
        val lower = domain.lowercase().trimEnd('.')
        if (BLOCKLIST.contains(lower)) return true
        // Check parent domains (e.g. sub.tracker.com → tracker.com)
        val parts = lower.split(".")
        for (i in 1 until parts.size - 1) {
            if (BLOCKLIST.contains(parts.drop(i).joinToString("."))) return true
        }
        return false
    }

    fun blocklistSize() = BLOCKLIST.size

    // ── HTTP DoH Request ───────────────────────────────────────────────────────

    private fun doHttpQuery(endpoint: String, dnsQuery: ByteArray): ByteArray? {
        return try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout    = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/dns-message")
                setRequestProperty("Accept", "application/dns-message")
                doOutput = true
                doInput  = true
            }
            conn.outputStream.use { it.write(dnsQuery) }
            val response = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            response
        } catch (e: Exception) {
            Log.w(TAG, "HTTP DoH query failed to $endpoint: ${e.message}")
            null
        }
    }

    // ── DNS Packet Helpers ─────────────────────────────────────────────────────

    /** Extracts the queried domain name from a raw DNS query packet */
    private fun extractDomainFromQuery(query: ByteArray): String? {
        return try {
            if (query.size < 12) return null
            val sb = StringBuilder()
            var i = 12  // skip DNS header
            while (i < query.size) {
                val len = query[i].toInt() and 0xFF
                if (len == 0) break
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(String(query, i + 1, len, Charsets.US_ASCII))
                i += len + 1
            }
            if (sb.isEmpty()) null else sb.toString()
        } catch (_: Exception) { null }
    }

    /** Builds a minimal DNS A-record query for a domain */
    private fun buildDnsQuery(domain: String): ByteArray {
        val header = byteArrayOf(
            0x00, 0x01,  // ID
            0x01, 0x00,  // Flags: standard query, recursion desired
            0x00, 0x01,  // QDCOUNT = 1
            0x00, 0x00,  // ANCOUNT
            0x00, 0x00,  // NSCOUNT
            0x00, 0x00   // ARCOUNT
        )
        val qname = domain.split(".").fold(byteArrayOf()) { acc, part ->
            acc + byteArrayOf(part.length.toByte()) + part.toByteArray()
        } + byteArrayOf(0x00)
        val qtype  = byteArrayOf(0x00, 0x01)  // A record
        val qclass = byteArrayOf(0x00, 0x01)  // IN
        return header + qname + qtype + qclass
    }

    /** Parses the first A record IP from a DNS response */
    private fun parseARecord(response: ByteArray): String? {
        return try {
            if (response.size < 12) return null
            val ancount = ((response[6].toInt() and 0xFF) shl 8) or (response[7].toInt() and 0xFF)
            if (ancount == 0) return null
            // Skip question section
            var i = 12
            while (i < response.size && response[i] != 0.toByte()) {
                i += (response[i].toInt() and 0xFF) + 1
            }
            i += 5  // null terminator + qtype + qclass
            // Parse first answer
            if (i + 12 >= response.size) return null
            i += 2  // name (pointer or label)
            val type = ((response[i].toInt() and 0xFF) shl 8) or (response[i+1].toInt() and 0xFF)
            if (type != 1) return null  // Not A record
            i += 8  // type + class + ttl
            val rdlen = ((response[i].toInt() and 0xFF) shl 8) or (response[i+1].toInt() and 0xFF)
            i += 2
            if (rdlen == 4 && i + 4 <= response.size) {
                "${response[i].toInt() and 0xFF}.${response[i+1].toInt() and 0xFF}" +
                ".${response[i+2].toInt() and 0xFF}.${response[i+3].toInt() and 0xFF}"
            } else null
        } catch (_: Exception) { null }
    }

    /** Synthesises an NXDOMAIN (name not found) DNS response for a blocked query */
    private fun buildNxdomainResponse(query: ByteArray): ByteArray {
        if (query.size < 2) return query
        val response = query.copyOf()
        // Set QR=1 (response), RCODE=3 (NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()
        return response
    }

    // ── Cache ──────────────────────────────────────────────────────────────────

    private data class CachedResponse(
        val rawResponse: ByteArray,
        val ttlSeconds: Int,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired() = System.currentTimeMillis() - createdAt > ttlSeconds * 1000L
    }
}
