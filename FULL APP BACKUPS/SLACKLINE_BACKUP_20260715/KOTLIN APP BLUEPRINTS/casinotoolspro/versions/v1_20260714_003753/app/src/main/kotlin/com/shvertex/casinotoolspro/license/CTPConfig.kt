package com.shvertex.casinotoolspro.license

// ── CTP Config ────────────────────────────────────────────────────────────────
// All app-level constants in one place.
// Update pricing, contact info, and payment details here only.

object CTPConfig {

    // ── Pricing ───────────────────────────────────────────────────────────────
    const val PRICE_PRO      = "$19.99"
    const val PRICE_PRO_PLUS = "$29.99"

    // ── Contact ───────────────────────────────────────────────────────────────
    const val CONTACT_EMAIL = "ceo.shvertex@gmail.com"
    const val CONTACT_WA    = "+94771363462"
    const val ACCOUNT_URL   = "https://shvertex.online/account.html"

    // ── Bank Transfer ─────────────────────────────────────────────────────────
    // Leave blank to hide that field on the gate screen
    const val BANK_NAME     = "NATIONS TRUST BANK - SRI LANKA"
    const val BANK_ACCNAME  = "Sachith Hirimuthugoda"
    const val BANK_ACCOUNT  = "200080074322"
    const val BANK_BRANCH   = "Pettah Main Street / Pettah 1"
    const val BANK_SWIFT    = "NTBCLKLX"

    // ── Crypto Wallet Addresses ───────────────────────────────────────────────
    // Leave blank to hide that coin on the gate screen
    const val USDT_BSC      = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"   // USDT — BSC BEP20
    const val USDT_TRC      = "TBXiwbhm59cxmzw78CtPD9kgxShZ38WSFS"   // USDT — TRC20
    const val USDT_PLASMA   = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"   // USDT — Plasma
    const val ETH_ADDR      = "0x9c21d18a80da9a8caaff3f590ef6960edc2671fb"   // ETH
    const val LTC_ADDR      = "LUmyxv1CYNEkJHXoh77ZDzN6wxKhCe8QgG"   // LTC

    // ── Payment Disclaimer ────────────────────────────────────────────────────
    const val PAYMENT_DISCLAIMER =
        "Pay first via your preferred method. Notify us after payment — " +
        "your device code is included automatically. We will verify and " +
        "deliver your Pro license key within 4 hours. Your trial will be " +
        "extended immediately upon payment confirmation."

    // ── Grace Period ──────────────────────────────────────────────────────────
    const val GRACE_HOURS = 3

    // ── Helpers ───────────────────────────────────────────────────────────────
    val hasBankDetails get() = listOf(BANK_NAME, BANK_ACCOUNT, BANK_ACCNAME, BANK_BRANCH, BANK_SWIFT).any { it.isNotBlank() }
    val hasCrypto      get() = listOf(USDT_BSC, USDT_TRC, USDT_PLASMA, ETH_ADDR, LTC_ADDR).any { it.isNotBlank() }
}
