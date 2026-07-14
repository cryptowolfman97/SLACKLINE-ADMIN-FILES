package com.shvertex.supaadmin.license

/**
 * LicenseGateConfig
 * ─────────────────
 * Manual configuration for Supa Studio's Pro / Pro+ licensing.
 * Edit the values below directly — nothing here is generated or injected.
 *
 * This mirrors the fields normally produced by the Kotlin App Injector's
 * GateConfig template, but since Supa Studio's gate is a popup (not a
 * blocking startup screen) it's hand-maintained here instead.
 */
object LicenseGateConfig {

    // ── App identity (must match the Kotlin Apps Manager Product Authority) ──
    const val APP_DISPLAY_NAME = "Supa Studio by SHV"
    const val APP_CODE         = "supa_studio"
    const val PREFIX_ROOT      = "SUPASTUDIO2026"

    // Product UUID from Kotlin Apps License Manager (used for revocation /
    // "product still active" checks against kl_products).
    const val PRODUCT_ID = "09c8bc6b-a47f-4170-9eb9-e3abee83d781"

    // RSA public key baked in at build time (from the Product Authority).
    // Private key NEVER goes in the app — it stays only in Kotlin Apps Manager.
    val LICENSE_PUBLIC_KEY_PEM = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArMohhVYYil1CogmMt32b
a3/kx2l24+rckbOcPVt+VGt/0fKGGBnz17F9PRf8pmve/YWXcaoVQKDygnatXnBQ
7XrIKiw9/yXZWt5PGWdHfQpymZG5MUyt0C1OG1UA+o8abdqm5ML9/H/n/y0war+K
X/Ts2WwxrU4cgE3M3uyYqybG5QvSeOMFvh7zaAAYbEdGKjcAGi/0V/ek7meQbFPT
PWlOKTIhxEQpzGAzFDNQiZ8BYy6TNJosUuuMgdf2q8TKa+m82uTS+Uvm9lzg/ayw
Z6RSyq2IndePcoUzw2M9n382dfX+xBKrAnUVVKVRtTWU8oAfS32JgYQKOjQN+DMW
YQIDAQAB
-----END RSA PUBLIC KEY-----
""".trim()

    // ── Pricing ────────────────────────────────────────────────────────────
    const val PRO_PRICE      = "$29.99"
    const val PRO_PLUS_PRICE = "$39.99"
    const val PRICE_NOTE     = "Lifetime · Device-bound · No subscription"

    // Grace period (hours) — how long the app keeps the previous access level
    // usable after a payment notification, while you manually issue the key.
    const val GRACE_PERIOD_HOURS = 3

    // Shown in the gate popup after payment
    const val PAYMENT_DISCLAIMER =
        "After payment, tap your preferred contact method below. Your device " +
        "code will be included automatically. We will verify your payment and " +
        "deliver your license key within 3 hours. Your access level will be " +
        "extended immediately upon payment confirmation so you can keep using " +
        "the app."

    // ── Contact methods ────────────────────────────────────────────────────
    const val CONTACT_EMAIL          = "ceo.shvertex@gmail.com"
    const val CONTACT_WHATSAPP_NUMBER = "+94771363462" // TODO: set real WhatsApp number

    // ── Bank transfer details ──────────────────────────────────────────────
    const val BANK_NAME        = "Nations Trust Bank - Sri Lanka"
    const val BANK_ACCOUNT_NAME = "Sachith Sanka"
    const val BANK_ACCOUNT_NO  = "200080074322"
    const val BANK_BRANCH      = "Pettah 01 / Pettah - Main Street"
    const val BANK_SWIFT       = "NTBCLKLX"

    // ── Crypto wallet addresses ─────────────────────────────────────────────
    const val CRYPTO_USDT_BEP20  = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_USDT_TRC20  = "TBXiwbhm59cxmzw78CtPD9kgxShZ38WSFS"
    const val CRYPTO_USDT_PLASMA = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_ETH         = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"
    const val CRYPTO_LTC         = "LUmyxv1CYNEkJHXoh77ZDzN6wxKhCe8QgG"

    // ── SHVertex account (Supabase) ────────────────────────────────────────
    // Same SHVertex account backend used across all SHV apps.
    const val ACCOUNT_URL = "https://shvertex.online/account.html"
}
