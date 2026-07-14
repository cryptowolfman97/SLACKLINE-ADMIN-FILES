package com.example.omnicortex.license

/**
 * SHV OMNI-CORTEX — LICENSE GATE CONFIG
 * ----------------------------------------------------------------
 * Hand-edit this file only. Nothing here touches app behaviour —
 * it is read exclusively by the license/account layer
 * (SHVLicense.kt, SHVAccount.kt, LicenseState.kt, LicenseDialogs.kt).
 * ----------------------------------------------------------------
 */
object LicenseGateConfig {

    // ── Product identity (from Slackline → Kotlin Apps Manager) ────────────
    const val APP_CODE   = "omni_cortex"
    const val ACT_PREFIX = "OMNICORTEX2026"
    const val PRODUCT_ID = "3b7c4416-64c5-44b3-983a-9748a43ebefd"
    const val APP_NAME   = "SHV Omni-Cortex"

    // RSA public key used ONLY to verify license tokens signed by Slackline.
    // Never place a private key in this app.
    val PUBLIC_KEY_PEM = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1iMONGQPKL0bAYNAdNGg
Ff8JLJDU4VUklMv9IuJuqYehkXtObXXl0Hsd3h49+Aml50B6NFrdNbkOdMRG/gwr
xgKzq78EyogBQVsGWG0UZh3Qv7wk+BRxCS3TNkkX1flpZT1S2otwFUFYdQ44B1+8
KHSqgY+U7BsjOSbAbA3cU6XcoG8RWQHbTkud7qLcIX9QIfrvPVAYbrAvqPYbwST0
DzE6uX+r1M5r+LIYXiGlmsAvXRpiKMgn/fWhInrKmnrV/jjPYsi8vDQzOXWAh5b2
NTDiE/xLukJHSUwVL/C3Z9DBZPNtb1P6iOLYq17wGkG3gGbiQi3CGcY6dHZM55+e
AwIDAQAB
-----END RSA PUBLIC KEY-----
    """.trimIndent()

    // ── Supabase (shared SH Vertex backend — same project as other apps) ───
    const val SUPABASE_URL    = "https://ovdxetyadfsxehwnbyuz.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz"

    // ── Pricing ──────────────────────────────────────────────────────────
    const val PRO_PRICE_LABEL       = "$14.99"
    const val PRO_PLUS_PRICE_LABEL  = "$19.99"
    const val PRICE_TERMS           = "Lifetime · Device-bound · No subscription"

    // Grace period (hours) — how long access is extended immediately after
    // the user marks "I've paid", ahead of manual verification.
    const val GRACE_PERIOD_HOURS = 3

    // ── License check cadence ───────────────────────────────────────────
    const val PERIODIC_CHECK_MINUTES = 5L

    // ── Bank transfer details ───────────────────────────────────────────
    const val BANK_NAME        = "Nations Trust Bank - Sri Lanka"
    const val BANK_ACCOUNT_NAME = "Sachith Sanka"
    const val BANK_ACCOUNT_NO  = "2000080074322"
    const val BANK_BRANCH      = "Pettah 01 / Pettah - Main Street"
    const val BANK_SWIFT       = "NTBCLKLX"

    // ── Crypto wallet addresses (leave blank "" to hide a row) ─────────────
    const val CRYPTO_USDT_BSC    = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_USDT_TRC20  = "TBXiwbhm59cxmzw78CtPD9kgxShZ38WSFS"
    const val CRYPTO_ETH         = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_LTC         = "LUmyxv1CYNEkJHXoh77ZDzN6wxKhCe8QgG"
    const val CRYPTO_BTC         = "18MuyVK2BKF1TL9eoMrVa8JbEaQNeEYwNu"

    // ── Contact targets for "Request License" / "I've Paid" notifications ──
    const val CONTACT_EMAIL          = "ceo.shvertex@gmail.com"
    const val CONTACT_WHATSAPP_NUMBER = "+94771363462"   // international format, digits only, e.g. "94771234567"

    // ── Disclaimer shown in the License Details dialog ─────────────────────
    const val PAYMENT_DISCLAIMER =
        "After payment, tap your preferred contact method below. Your device code " +
        "will be included automatically. We will verify your payment and deliver " +
        "your license key within 3 hours. Your access will be extended immediately " +
        "upon payment confirmation so you can keep using the app."

    // ── Tier definitions (for reference / UI labels only — actual gating logic
    //     lives in HomeScreen's click interceptors) ─────────────────────────
    val FREE_MODULES = listOf("Device Posture", "Breach Monitor", "HTTP Recon")
    val PRO_MODULES = listOf(
        "App Permissions", "Network Intel", "Comms Validator",
        "Port Scanner", "DNS Intel", "APK Analyser"
    )
    val PRO_PLUS_MODULES = listOf(
        "Privacy Shield", "Shizuku Firewall", "Shizuku Permissions", "Shizuku Net Monitor"
    )
}
