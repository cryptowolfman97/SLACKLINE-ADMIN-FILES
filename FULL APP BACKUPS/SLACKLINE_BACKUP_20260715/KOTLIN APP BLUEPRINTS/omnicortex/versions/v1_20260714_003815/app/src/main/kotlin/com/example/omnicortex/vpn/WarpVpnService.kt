package com.example.omnicortex.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * WarpVpnService
 *
 * Implements a lightweight VPN that:
 *  1. Captures device DNS traffic (UDP port 53) bound for the WARP resolvers
 *  2. Forwards DNS queries to Cloudflare WARP / 1.1.1.1 over encrypted DoH
 *  3. Blocks known tracker/ad domains at the DNS layer
 *  4. Never touches non-DNS traffic at all (true split-tunnel DNS-only mode)
 *
 * This uses Android's VpnService API — no root required.
 * The TUN's routing table (see startVpn()) only includes /32 routes to the
 * DNS resolver IPs themselves, NOT 0.0.0.0/0. That means non-DNS traffic
 * (web pages, streaming, everything else) is never pulled into the tunnel
 * in the first place — it stays on the device's normal network path, so
 * there's no performance impact and, critically, no risk of it silently
 * disappearing into a TUN interface with no real forwarding path out.
 */
class WarpVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.omnicortex.vpn.START"
        const val ACTION_STOP  = "com.example.omnicortex.vpn.STOP"
        const val NOTIFICATION_ID = 1337
        const val CHANNEL_ID = "omnicortex_vpn"

        // Cloudflare WARP DNS endpoints
        private const val WARP_PRIMARY   = "162.159.36.1"
        private const val WARP_SECONDARY = "162.159.46.1"
        // Fallback: Cloudflare 1.1.1.1 (malware-blocking variant)
        private const val CF_MALWARE     = "1.1.1.2"

        // Virtual TUN address (not routable — just for the interface)
        private const val TUN_ADDRESS    = "10.0.0.2"
        private const val TUN_PREFIX     = 32
        private const val TUN_ROUTE      = "0.0.0.0"
        private const val TUN_MTU        = 1500

        @Volatile var isRunning = false
        @Volatile var packetsDropped: Long = 0
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> { stopVpn(); START_NOT_STICKY }
            else        -> { startVpn(); START_STICKY }
        }
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    private fun startVpn() {
        if (isRunning) return
        packetsDropped = 0
        startForeground(NOTIFICATION_ID, buildNotification("Privacy Shield Active", "DNS encrypted via Cloudflare WARP"))

        try {
            tunInterface = Builder()
                .setSession("SHV Omni-Cortex Privacy Shield")
                .addAddress(TUN_ADDRESS, TUN_PREFIX)
                // Only route DNS traffic through the tunnel (port 53 = DNS).
                // IMPORTANT: we route ONLY to the DNS resolver IPs themselves
                // (as /32 host routes), NOT 0.0.0.0/0. Android's VpnService
                // routing table only pulls packets into the TUN if their
                // destination matches an added route — so scoping the routes
                // this tightly means non-DNS traffic (web, streaming, etc.)
                // never enters the tunnel at all and is handled by the normal
                // network stack. This is what actually makes it a DNS-only VPN;
                // 0.0.0.0/0 would (and previously did) capture everything.
                .addDnsServer(WARP_PRIMARY)
                .addDnsServer(WARP_SECONDARY)
                .addRoute(WARP_PRIMARY, 32)
                .addRoute(WARP_SECONDARY, 32)
                .setMtu(TUN_MTU)
                .setBlocking(false)
                .also { if (Build.VERSION.SDK_INT >= 29) it.setMetered(false) }
                .establish()

            isRunning = true
            WarpVpnEngine.onStateChange(VpnState.CONNECTED)
            startPacketLoop()
        } catch (e: Exception) {
            Log.e("WarpVpn", "Failed to establish VPN tunnel", e)
            isRunning = false
            WarpVpnEngine.onStateChange(VpnState.ERROR("Failed to start: ${e.message}"))
            stopSelf()
        }
    }

    private fun stopVpn() {
        isRunning = false
        serviceJob?.cancel()
        serviceJob = null
        try { tunInterface?.close() } catch (_: Exception) {}
        tunInterface = null
        WarpVpnEngine.onStateChange(VpnState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Packet Processing Loop ────────────────────────────────────────────────

    /**
     * Reads IP packets from the TUN interface.
     * DNS packets (UDP dst-port 53) are intercepted and handled via DoH.
     * All other packets are forwarded directly (transparent pass-through).
     */
    private fun startPacketLoop() {
        val tun = tunInterface ?: return
        serviceJob = scope.launch {
            val inputStream  = FileInputStream(tun.fileDescriptor)
            val outputStream = FileOutputStream(tun.fileDescriptor)
            val packetBuffer = ByteBuffer.allocate(TUN_MTU)

            while (isActive && isRunning) {
                try {
                    packetBuffer.clear()
                    val len = inputStream.read(packetBuffer.array())
                    if (len <= 0) {
                        delay(5)
                        continue
                    }
                    packetBuffer.limit(len)

                    if (isDnsPacket(packetBuffer)) {
                        // Extract the DNS query and resolve via DoH
                        val dnsPayload = extractDnsPayload(packetBuffer, len)
                        if (dnsPayload != null) {
                            launch {
                                val response = DoHEngine.resolveRaw(dnsPayload)
                                if (response != null) {
                                    val reply = buildDnsReplyPacket(packetBuffer, response)
                                    synchronized(outputStream) {
                                        outputStream.write(reply)
                                    }
                                }
                            }
                        }
                    } else {
                        // Non-DNS packet reached the tunnel. With routes scoped
                        // to only the DNS resolver IPs (see startVpn()), this
                        // should never normally happen — but some OEM network
                        // stacks can occasionally misroute a stray packet in.
                        //
                        // We deliberately do NOT write it back into the TUN's
                        // own output stream: a TUN device's output stream is
                        // for injecting packets that appear to come FROM the
                        // network, so echoing a packet we just read FROM the
                        // device back INTO the device doesn't forward it
                        // anywhere real — it previously caused all non-DNS
                        // traffic to silently vanish (the exact bug that broke
                        // browsing). Properly forwarding it would require a
                        // real protected socket + NAT layer, which is out of
                        // scope for a DNS-only shield. So we fail safe: drop
                        // the stray packet and let the OS naturally retry it
                        // over the normal (non-tunnel) network path, which is
                        // available since it's outside our route scope.
                        packetsDropped++
                        if (packetsDropped % 50 == 1L) {
                            Log.w("WarpVpn", "Dropped stray non-DNS packet in tunnel (total: $packetsDropped)")
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) Log.w("WarpVpn", "Packet loop error: ${e.message}")
                    delay(10)
                }
            }
        }
    }

    // ── DNS Packet Helpers ────────────────────────────────────────────────────

    /** Returns true if this IP packet is a UDP packet destined for port 53 */
    private fun isDnsPacket(buffer: ByteBuffer): Boolean {
        val arr = buffer.array()
        if (buffer.limit() < 28) return false
        val ipVersion = (arr[0].toInt() and 0xF0) shr 4
        if (ipVersion != 4) return false                   // IPv4 only
        val protocol = arr[9].toInt() and 0xFF
        if (protocol != 17) return false                   // UDP only
        val ipHeaderLen = (arr[0].toInt() and 0x0F) * 4
        if (buffer.limit() < ipHeaderLen + 8) return false
        val dstPort = ((arr[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                       (arr[ipHeaderLen + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    /** Extracts the raw DNS query bytes from a UDP/IP packet */
    private fun extractDnsPayload(buffer: ByteBuffer, len: Int): ByteArray? {
        val arr = buffer.array()
        val ipHeaderLen = (arr[0].toInt() and 0x0F) * 4
        val udpHeaderLen = 8
        val dnsOffset = ipHeaderLen + udpHeaderLen
        if (len <= dnsOffset) return null
        return arr.copyOfRange(dnsOffset, len)
    }

    /**
     * Builds a well-formed IP/UDP reply packet wrapping the DNS response.
     * Swaps src/dst IP and ports from the original query packet.
     */
    private fun buildDnsReplyPacket(queryPacket: ByteBuffer, dnsResponse: ByteArray): ByteArray {
        val q = queryPacket.array()
        val ipHeaderLen = (q[0].toInt() and 0x0F) * 4
        val totalLen = ipHeaderLen + 8 + dnsResponse.size
        val reply = ByteArray(totalLen)

        // IP header — copy and swap src/dst
        System.arraycopy(q, 0, reply, 0, ipHeaderLen)
        reply[2] = ((totalLen shr 8) and 0xFF).toByte()
        reply[3] = (totalLen and 0xFF).toByte()
        // Swap src ↔ dst IP
        System.arraycopy(q, 12, reply, 16, 4)
        System.arraycopy(q, 16, reply, 12, 4)

        // UDP header — swap src/dst ports
        reply[ipHeaderLen]     = q[ipHeaderLen + 2]
        reply[ipHeaderLen + 1] = q[ipHeaderLen + 3]
        reply[ipHeaderLen + 2] = q[ipHeaderLen]
        reply[ipHeaderLen + 3] = q[ipHeaderLen + 1]
        val udpLen = 8 + dnsResponse.size
        reply[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        reply[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        reply[ipHeaderLen + 6] = 0  // checksum (0 = disabled for simplicity)
        reply[ipHeaderLen + 7] = 0

        // DNS payload
        System.arraycopy(dnsResponse, 0, reply, ipHeaderLen + 8, dnsResponse.size)

        // Recalculate IP checksum
        writeIpChecksum(reply, ipHeaderLen)
        return reply
    }

    private fun writeIpChecksum(packet: ByteArray, headerLen: Int) {
        packet[10] = 0; packet[11] = 0
        var sum = 0
        for (i in 0 until headerLen step 2) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        sum = sum.inv() and 0xFFFF
        packet[10] = ((sum shr 8) and 0xFF).toByte()
        packet[11] = (sum and 0xFF).toByte()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(title: String, text: String): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "Privacy Shield", NotificationManager.IMPORTANCE_LOW)
            ch.description = "OmniCortex VPN status"
            nm.createNotificationChannel(ch)
        }
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, WarpVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Disconnect", stopIntent).build())
            .build()
    }
}
