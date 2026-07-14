package com.example.omnicortex.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket

object PortScanEngine {

    data class ScanResult(
        val host: String,
        val resolvedIp: String,
        val openPorts: List<PortEntry>,
        val closedCount: Int,
        val filteredCount: Int,
        val scanDurationMs: Long,
        val osGuess: String
    )

    data class PortEntry(
        val port: Int,
        val service: String,
        val banner: String,
        val protocol: String = "TCP"
    )

    // Common port definitions
    private val COMMON_PORTS = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
        53 to "DNS", 80 to "HTTP", 110 to "POP3", 111 to "RPC",
        135 to "MSRPC", 139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS",
        445 to "SMB", 465 to "SMTPS", 587 to "SMTP-TLS", 993 to "IMAPS",
        995 to "POP3S", 1433 to "MSSQL", 1521 to "Oracle", 2181 to "Zookeeper",
        3306 to "MySQL", 3389 to "RDP", 4444 to "Metasploit", 5432 to "PostgreSQL",
        5900 to "VNC", 6379 to "Redis", 6443 to "Kubernetes", 7001 to "WebLogic",
        8080 to "HTTP-Alt", 8443 to "HTTPS-Alt", 8888 to "HTTP-Alt2",
        9200 to "Elasticsearch", 9300 to "Elasticsearch-Cluster",
        27017 to "MongoDB", 27018 to "MongoDB-Alt"
    )

    val TOP_100_PORTS = listOf(
        21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 143, 443, 445,
        465, 587, 993, 995, 1433, 1521, 2181, 3306, 3389, 4444, 5432,
        5900, 6379, 6443, 7001, 8080, 8443, 8888, 9200, 9300, 27017, 27018
    )

    val TOP_1000_PORTS = (1..1024).toList() + TOP_100_PORTS

    suspend fun scan(
        host: String,
        ports: List<Int>,
        timeoutMs: Int = 1500,
        concurrency: Int = 50,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): ScanResult = coroutineScope {
        val start      = System.currentTimeMillis()
        val cleanHost  = host.trim()
            .removePrefix("https://").removePrefix("http://").substringBefore("/")

        // Resolve IP
        val resolvedIp = try {
            withContext(Dispatchers.IO) {
                java.net.InetAddress.getByName(cleanHost).hostAddress ?: cleanHost
            }
        } catch (e: Exception) { cleanHost }

        val openPorts   = mutableListOf<PortEntry>()
        var closed      = 0
        var filtered    = 0
        val total       = ports.size
        var done        = 0

        // Semaphore-controlled concurrent scanning
        val semaphore = Semaphore(concurrency)
        val jobs = ports.map { port ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val result = probePort(cleanHost, port, timeoutMs)
                    synchronized(openPorts) {
                        when (result) {
                            PortState.OPEN     -> { /* handled below */ }
                            PortState.CLOSED   -> closed++
                            PortState.FILTERED -> filtered++
                        }
                        done++
                        onProgress(done, total)
                    }
                    Pair(port, result)
                }
            }
        }

        jobs.awaitAll().forEach { pair ->
            val port  = pair.first
            val state = pair.second
            if (state == PortState.OPEN) {
                val service = COMMON_PORTS[port] ?: "Unknown"
                val banner  = grabBanner(cleanHost, port, timeoutMs)
                openPorts += PortEntry(port, service, banner)
            }
        }

        val elapsed = System.currentTimeMillis() - start
        val osGuess = guessOs(openPorts)

        ScanResult(
            host          = cleanHost,
            resolvedIp    = resolvedIp,
            openPorts     = openPorts.sortedBy { it.port },
            closedCount   = closed,
            filteredCount = filtered,
            scanDurationMs = elapsed,
            osGuess       = osGuess
        )
    }

    private enum class PortState { OPEN, CLOSED, FILTERED }

    private fun probePort(host: String, port: Int, timeoutMs: Int): PortState {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.close()
            PortState.OPEN
        } catch (e: java.net.ConnectException) {
            PortState.CLOSED
        } catch (e: java.net.SocketTimeoutException) {
            PortState.FILTERED
        } catch (e: Exception) {
            PortState.FILTERED
        }
    }

    private fun grabBanner(host: String, port: Int, timeoutMs: Int): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.soTimeout = 2000
            // Send a probe for HTTP ports
            if (port in listOf(80, 8080, 8888, 8443, 443)) {
                val out = socket.getOutputStream()
                out.write("HEAD / HTTP/1.0\r\nHost: $host\r\n\r\n".toByteArray())
                out.flush()
            }
            val banner = socket.getInputStream()
                .bufferedReader()
                .readLine()
                ?.take(200)
                ?: ""
            socket.close()
            banner
        } catch (e: Exception) { "" }
    }

    private fun guessOs(openPorts: List<PortEntry>): String {
        val ports = openPorts.map { it.port }.toSet()
        return when {
            445 in ports && 135 in ports && 3389 in ports -> "Windows (RDP + SMB + RPC)"
            445 in ports && 135 in ports                  -> "Windows (SMB + RPC)"
            22 in ports && 80 in ports && 443 in ports    -> "Linux/Unix (SSH + Web)"
            22 in ports && 3306 in ports                  -> "Linux (SSH + MySQL)"
            22 in ports                                   -> "Linux/Unix (SSH)"
            23 in ports                                   -> "Network Device (Telnet)"
            openPorts.isEmpty()                           -> "Unknown / Filtered"
            else                                          -> "Unknown"
        }
    }
}
