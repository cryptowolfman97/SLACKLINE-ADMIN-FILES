package com.example.omnicortex.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * PermissionHelper
 *
 * Handles requesting the two special permissions needed for reliable
 * background VPN operation:
 *
 *  1. Battery optimization exemption — prevents Android from killing
 *     the VPN service when the screen is off or the app is in background.
 *
 *  2. Display over other apps (SYSTEM_ALERT_WINDOW) — required to show
 *     the floating PiP VPN status bubble over other apps.
 */
object PermissionHelper {

    const val REQUEST_OVERLAY    = 7002
    const val REQUEST_BATTERY    = 7003

    // ── Battery Optimization ──────────────────────────────────────────────────

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system dialog asking the user to exempt this app from
     * battery optimization. Required for the VPN service to stay alive
     * reliably when the screen is off.
     */
    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (isBatteryOptimizationExempt(activity)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivityForResult(intent, REQUEST_BATTERY)
    }

    // ── Display Over Other Apps ───────────────────────────────────────────────

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Opens the system settings page for "Display over other apps".
     * The user must manually toggle it on — Android doesn't allow a direct
     * grant dialog for this permission.
     */
    fun requestOverlayPermission(activity: Activity) {
        if (canDrawOverlays(activity)) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, REQUEST_OVERLAY)
    }

    // ── Permission State Summary ──────────────────────────────────────────────

    data class PermissionStatus(
        val batteryExempt:  Boolean,
        val overlayGranted: Boolean
    ) {
        val allGranted get() = batteryExempt && overlayGranted
    }

    fun getStatus(context: Context) = PermissionStatus(
        batteryExempt  = isBatteryOptimizationExempt(context),
        overlayGranted = canDrawOverlays(context)
    )
}
