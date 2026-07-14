package com.snaploader.app.license

/**
 * LicenseGateConfig
 * ─────────────────
 * Manual configuration for SHV Downloader's Pro / Pro+ licensing.
 * Edit the values below directly — nothing here is generated or injected.
 */
object LicenseGateConfig {

    // ── App identity (must match the Kotlin Apps Manager Product Authority) ──
    const val APP_DISPLAY_NAME = "SHV Downloader"
    const val APP_CODE         = "shv_downloader"
    const val PREFIX_ROOT      = "SHVD26"

    // product_id column value from kl_products (NOT the row's id/UUID column)
    const val PRODUCT_ID = "0fb5a729-4516-4b87-b7dd-20598c35724c"

    // RSA public key baked in at build time (from the Product Authority).
    // Private key NEVER goes in the app — it stays only in Kotlin Apps Manager.
    val LICENSE_PUBLIC_KEY_PEM = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxcK7eU3dJKs/mPWFDYqy
ZNnDLqoZu9FHo3S1faYgeh8G5L6Skcf6zSquiQjLxMNQMWjInBTyPJM1Aa0HQKs9
Rz2WCv7iOp3jGN9k9lwpDcsYyBjaZf529A7ysFigrUovzZ5Q5A+2NvdekNzdUrmz
4VFTA6ARGr9JYqCv7+K/JXIAS/BUyQAIkk/U0AuzQ5AhQBmTzPy6+uVZGaROY/qh
8pfphdQVSu7JmCFnvVPpTQhI6WClYK6c3FssXozc5sF/+DGjDCzNif4sMKhJYezE
gBtkdTXSm3dI1F5aw0Faa9RCq/N/8de4FImpK6UHNdhdcAvc7CuGGzRJ3uvSO8w3
nwIDAQAB
-----END RSA PUBLIC KEY-----
""".trim()

    // ── Pricing ────────────────────────────────────────────────────────────
    const val PRO_PRICE      = "$5.99"
    const val PRO_PLUS_PRICE = "$9.99"
    const val PRICE_NOTE     = "Lifetime · Device-bound · No subscription"

    // Grace period (hours) — how long the app keeps the previous access level
    // usable after a payment notification, while you manually issue the key.
    const val GRACE_PERIOD_HOURS = 6

    // Shown in the gate popup after payment
    const val PAYMENT_DISCLAIMER =
        "After payment, tap your preferred contact method below. Your device " +
        "code will be included automatically. We will verify your payment and " +
        "deliver your license key within 4 hours. Your access level will be " +
        "extended immediately upon payment confirmation so you can keep using " +
        "the app."

    // ── Contact methods (reused from other SHV apps — update if different) ──
    const val CONTACT_EMAIL           = "ceo.shvertex@gmail.com"
    const val CONTACT_WHATSAPP_NUMBER = "+94771363462"

    // ── Bank transfer details (reused — update if this app uses a different account) ──
    const val BANK_NAME         = "Nations Trust Bank - Sri Lanka"
    const val BANK_ACCOUNT_NAME = "Sachith Sanka"
    const val BANK_ACCOUNT_NO   = "200080074322"
    const val BANK_BRANCH       = "Pettah 01 / Pettah - Main Street"
    const val BANK_SWIFT        = "NTBCLKLX"

    // ── Crypto wallet addresses (reused) ─────────────────────────────────────
    const val CRYPTO_USDT_BEP20  = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_USDT_TRC20  = "TBXiwbhm59cxmzw78CtPD9kgxShZ38WSFS"
    const val CRYPTO_USDT_PLASMA = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_ETH         = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_LTC         = "LUmyxv1CYNEkJHXoh77ZDzN6wxKhCe8QgG"

    // ── SHVertex account (Supabase) ────────────────────────────────────────
    const val ACCOUNT_URL = "https://shvertex.online/account.html"
}
