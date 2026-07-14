package com.example.omnicortex.navigation

object Routes {
    const val LOCK_SCREEN      = "lock_screen"
    const val HOME             = "home"
    // Defensive
    const val DEVICE_POSTURE   = "device_posture"
    const val APP_PERMISSION   = "app_permission"
    const val NETWORK_INTEL    = "network_intel"
    const val BREACH_MONITOR   = "breach_monitor"
    const val COMMS_VALIDATOR  = "comms_validator"
    const val PRIVACY_SHIELD   = "privacy_shield"   // ← NEW
    // Offensive / Recon
    const val HTTP_RECON       = "http_recon"
    const val PORT_SCAN        = "port_scan"
    const val DNS_INTEL        = "dns_intel"
    const val APK_ANALYSER     = "apk_analyser"
    const val SHIZUKU_MODE     = "shizuku_mode/{tab}"   // ← NEW (Pro+)

    fun shizukuModeRoute(tab: String) = "shizuku_mode/$tab"
    // Settings
    const val SETTINGS         = "settings"
}
