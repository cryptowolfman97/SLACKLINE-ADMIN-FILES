package com.example.omnicortex.engine

import com.example.omnicortex.data.models.Severity
import com.example.omnicortex.data.models.TlsFinding
import com.example.omnicortex.data.models.TlsResult
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object TlsInspector {

    fun inspect(domain: String): TlsResult {
        val clean = domain.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .substringBefore("?")

        return try {
            runInspection(clean)
        } catch (e: SSLHandshakeException) {
            TlsResult(
                domain        = clean,
                isReachable   = true,
                tlsVersion    = "Unknown",
                cipherSuite   = "Unknown",
                certSubject   = "—",
                certIssuer    = "—",
                certValidFrom = "—",
                certValidTo   = "—",
                daysUntilExpiry = 0,
                isExpired     = true,
                isSelfSigned  = false,
                hasHsts       = false,
                grade         = "F",
                findings      = listOf(
                    TlsFinding("TLS Handshake Failed", "SSL handshake error: ${e.message}", Severity.CRITICAL, false)
                )
            )
        } catch (e: Exception) {
            TlsResult(
                domain        = clean,
                isReachable   = false,
                tlsVersion    = "—",
                cipherSuite   = "—",
                certSubject   = "—",
                certIssuer    = "—",
                certValidFrom = "—",
                certValidTo   = "—",
                daysUntilExpiry = 0,
                isExpired     = false,
                isSelfSigned  = false,
                hasHsts       = false,
                grade         = "F",
                findings      = listOf(
                    TlsFinding("Host Unreachable", "Could not connect: ${e.message}", Severity.CRITICAL, false)
                )
            )
        }
    }

    private fun runInspection(domain: String): TlsResult {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val socket  = factory.createSocket(domain, 443) as SSLSocket
        socket.soTimeout = 8000
        socket.startHandshake()

        val session    = socket.session
        val tlsVersion = session.protocol
        val cipher     = session.cipherSuite
        val certs      = session.peerCertificates

        socket.close()

        val leaf = certs.firstOrNull() as? X509Certificate
            ?: return TlsResult(domain, true, tlsVersion, cipher, "—", "—", "—", "—", 0, false, false, false, "F", emptyList())

        val fmt       = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now       = Date()
        val expiry    = leaf.notAfter
        val daysLeft  = ((expiry.time - now.time) / 86_400_000).toInt()
        val isExpired = now.after(expiry)

        val subject   = leaf.subjectDN.name
        val issuer    = leaf.issuerDN.name
        val isSelfSig = subject == issuer

        // HSTS check via HTTP
        val hasHsts = try {
            val conn = URL("https://$domain").openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout    = 5000
            conn.connect()
            val hsts = conn.getHeaderField("Strict-Transport-Security") != null
            conn.disconnect()
            hsts
        } catch (e: Exception) { false }

        // Build findings
        val findings = mutableListOf<TlsFinding>()

        // TLS version
        val tlsOk = tlsVersion in listOf("TLSv1.2", "TLSv1.3")
        findings += TlsFinding(
            label   = "TLS Version",
            detail  = "Negotiated: $tlsVersion. ${if (tlsVersion == "TLSv1.3") "TLS 1.3 is optimal." else if (tlsVersion == "TLSv1.2") "TLS 1.2 is acceptable." else "TLS $tlsVersion is deprecated and insecure."}",
            severity = if (tlsOk) Severity.INFO else Severity.CRITICAL,
            passed   = tlsOk
        )

        // Cipher suite
        val weakCiphers = listOf("RC4", "DES", "3DES", "NULL", "EXPORT", "MD5")
        val cipherWeak  = weakCiphers.any { cipher.uppercase().contains(it) }
        findings += TlsFinding(
            label    = "Cipher Suite",
            detail   = "Negotiated: $cipher.${if (cipherWeak) " This cipher is considered weak." else " Cipher strength is acceptable."}",
            severity = if (!cipherWeak) Severity.INFO else Severity.HIGH,
            passed   = !cipherWeak
        )

        // Cert expiry
        findings += TlsFinding(
            label    = "Certificate Expiry",
            detail   = when {
                isExpired  -> "Certificate expired on ${fmt.format(expiry)}. Connections will be rejected by browsers."
                daysLeft < 14 -> "Certificate expires in $daysLeft days (${fmt.format(expiry)}). Renewal is urgent."
                daysLeft < 30 -> "Certificate expires in $daysLeft days. Plan renewal soon."
                else          -> "Certificate valid until ${fmt.format(expiry)} ($daysLeft days remaining)."
            },
            severity = when {
                isExpired    -> Severity.CRITICAL
                daysLeft < 14 -> Severity.HIGH
                daysLeft < 30 -> Severity.MEDIUM
                else          -> Severity.INFO
            },
            passed   = !isExpired && daysLeft >= 14
        )

        // Self-signed
        findings += TlsFinding(
            label    = "Certificate Authority",
            detail   = if (isSelfSig)
                "Self-signed certificate detected. Not trusted by browsers or clients by default."
            else
                "Issued by: ${issuer.substringAfter("CN=").substringBefore(",").take(60)}",
            severity = if (isSelfSig) Severity.HIGH else Severity.INFO,
            passed   = !isSelfSig
        )

        // HSTS
        findings += TlsFinding(
            label    = "HSTS Header",
            detail   = if (hasHsts)
                "HTTP Strict Transport Security is enforced. Browsers will reject non-HTTPS connections."
            else
                "HSTS header is absent. Users could be downgraded to HTTP by an attacker.",
            severity = if (hasHsts) Severity.INFO else Severity.MEDIUM,
            passed   = hasHsts
        )

        // HTTP redirect
        val httpRedirects = try {
            val conn = URL("http://$domain").openConnection() as java.net.HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 5000
            conn.readTimeout    = 5000
            conn.connect()
            val loc  = conn.getHeaderField("Location") ?: ""
            val code = conn.responseCode
            conn.disconnect()
            code in 301..308 && loc.startsWith("https://")
        } catch (e: Exception) { false }

        findings += TlsFinding(
            label    = "HTTP → HTTPS Redirect",
            detail   = if (httpRedirects)
                "HTTP traffic is properly redirected to HTTPS."
            else
                "HTTP requests are not redirected to HTTPS. Plain-text access may be possible.",
            severity = if (httpRedirects) Severity.INFO else Severity.MEDIUM,
            passed   = httpRedirects
        )

        val grade = computeGrade(findings)

        return TlsResult(
            domain          = domain,
            isReachable     = true,
            tlsVersion      = tlsVersion,
            cipherSuite     = cipher,
            certSubject     = subject.substringAfter("CN=").substringBefore(",").take(60),
            certIssuer      = issuer.substringAfter("CN=").substringBefore(",").take(60),
            certValidFrom   = fmt.format(leaf.notBefore),
            certValidTo     = fmt.format(expiry),
            daysUntilExpiry = daysLeft,
            isExpired       = isExpired,
            isSelfSigned    = isSelfSig,
            hasHsts         = hasHsts,
            grade           = grade,
            findings        = findings
        )
    }

    private fun computeGrade(findings: List<TlsFinding>): String {
        val criticals = findings.count { !it.passed && it.severity == Severity.CRITICAL }
        val highs     = findings.count { !it.passed && it.severity == Severity.HIGH }
        val mediums   = findings.count { !it.passed && it.severity == Severity.MEDIUM }
        return when {
            criticals >= 1        -> "F"
            highs >= 2            -> "C"
            highs == 1            -> "B"
            mediums >= 2          -> "B"
            mediums == 1          -> "A"
            else                  -> "A+"
        }
    }
}
