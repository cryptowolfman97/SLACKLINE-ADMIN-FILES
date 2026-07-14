package com.example.omnicortex.shizuku

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShizukuUserService
 *
 * This class is NOT instantiated in the normal app process. Shizuku spins
 * up a separate process running as the shell UID (uid 2000) - the same
 * privilege level "adb shell" gives you, whether Shizuku itself is backed
 * by root or by wireless-debugging/ADB pairing - and instantiates this
 * class there. Every method here executes with shell privileges, which is
 * enough to call a handful of otherwise-restricted system commands without
 * needing root.
 *
 * IMPORTANT: this process is a separate Binder service, has its own
 * classloader, and can't touch the app's in-memory state directly - it
 * only communicates back through the IShizukuUserService interface's
 * return values. Treat every method as a narrow, auditable privileged
 * operation, not a general-purpose shell.
 */
class ShizukuUserService : IShizukuUserService.Stub() {

    companion object {
        private const val TAG = "ShizukuUserService"
    }

    // Shared shell exec helper (kept private - not exposed to the app)
    private fun runShell(vararg command: String): Pair<Int, String> {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val code = process.waitFor()
            code to output
        } catch (e: Exception) {
            Log.e(TAG, "Shell exec failed: ${command.joinToString(" ")}", e)
            -1 to (e.message ?: "unknown error")
        }
    }

    // ── Firewall ─────────────────────────────────────────────────────────

    override fun setUidNetworkBlocked(uid: Int, blocked: Boolean): Boolean {
        // "cmd netpolicy set-uid-policy <uid> reject|none" is the same
        // mechanism Android's own Data Saver / Restricted Networking uses
        // internally. It blocks (or unblocks) ALL network transports for
        // that uid - WiFi and cellular both - at the OS policy layer,
        // no VPN/packet-loop needed. Requires shell-level privilege, hence
        // Shizuku instead of a normal runtime permission.
        val policy = if (blocked) "reject" else "none"
        val (code, output) = runShell("cmd", "netpolicy", "set-uid-policy", uid.toString(), policy)
        if (code != 0) Log.w(TAG, "setUidNetworkBlocked($uid, $blocked) failed: $output")
        return code == 0
    }

    override fun isUidNetworkBlocked(uid: Int): Boolean {
        val (code, output) = runShell("cmd", "netpolicy", "list", "uid-policies")
        if (code != 0) return false
        // Output lines look like: "UID=10234 policy=REJECT"
        return output.lineSequence().any { line ->
            line.contains("UID=$uid") && line.contains("REJECT", ignoreCase = true)
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────

    override fun grantPermission(packageName: String, permission: String): Boolean {
        val (code, output) = runShell("pm", "grant", packageName, permission)
        if (code != 0) Log.w(TAG, "grantPermission($packageName, $permission) failed: $output")
        return code == 0
    }

    override fun revokePermission(packageName: String, permission: String): Boolean {
        val (code, output) = runShell("pm", "revoke", packageName, permission)
        if (code != 0) Log.w(TAG, "revokePermission($packageName, $permission) failed: $output")
        return code == 0
    }

    // ── Network stats ────────────────────────────────────────────────────

    override fun getNetstatsDump(): String {
        val (code, output) = runShell("dumpsys", "netstats", "detail")
        return if (code == 0) output else ""
    }

    // ── Health / lifecycle ───────────────────────────────────────────────

    override fun ping(): Int = android.os.Process.myUid()

    override fun destroy() {
        Log.i(TAG, "ShizukuUserService destroy() requested - exiting remote process")
        System.exit(0)
    }
}