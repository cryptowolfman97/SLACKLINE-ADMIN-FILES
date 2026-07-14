package com.shvertex.casinotoolspro.license

import com.shvertex.casinotoolspro.navigation.Routes

// ── CTP Access Singleton ──────────────────────────────────────────────────────
// Single source of truth for the current user's access tier.
// Set once after gate resolves, read everywhere in the app.

object CTPAccess {

    // Presentation Mode & PiP are not navigation routes — use these constants as access keys
    const val PRESENTATION_MODE_KEY = "presentation_mode"
    const val PIP_MODE_KEY = "pip_mode"

    // Current tier — set by LicenseGateScreen after access is granted
    var tier: String = "none"
        private set

    fun setTier(newTier: String) {
        tier = newTier.lowercase().trim()
    }

    // ── Tier check ────────────────────────────────────────────────────────────
    // "free"  → only free modules
    // "demo"  → full Pro+ access (time-limited)
    // "pro"   → Pro modules, no Evolution Lab, no Presentation Mode
    // "pro+"  → everything

    fun hasAccess(route: String): Boolean {
        return when (tierRequired(route)) {
            "free"  -> true // always accessible
            "pro"   -> tier in listOf("pro", "pro+", "demo")
            "pro+"  -> tier in listOf("pro+", "demo")
            else    -> true
        }
    }

    // Returns the minimum tier required for a given route
    fun tierRequired(route: String): String = when (route) {

        // ── Free tier — always accessible ────────────────────────────────────
        Routes.DICE_CALC,       // Dice/Limbo Calc
        Routes.MINES_ANALYTICS, // Mines Analytics
        Routes.COMPOUND,        // Compound Growth
        Routes.PATTERN,         // Pattern Master
        Routes.CONVERTER        // Crypto Converter
        -> "free"

        // ── Pro+ only — Evolution Lab ─────────────────────────────────────────
        Routes.STRATEGY_FORGE,
        Routes.STRESS_TEST,
        Routes.DICE_EVOLUTION,
        Routes.LIMBO_EVOLUTION,
        Routes.KENO_EVOLUTION,
        Routes.MINES_EVOLUTION
        -> "pro+"

        // ── Pro — everything else ─────────────────────────────────────────────
        else -> "pro"
    }

    // Presentation Mode requires Pro+
    fun canUsePresentation(): Boolean = tier in listOf("pro+", "demo")

    // Used by lockedMessage for presentation mode denied popup
    fun presentationLockedMessage(): String =
        "Presentation Mode is exclusive to Pro+ tier.\n\nUpgrade to Pro+ to unlock it."

    // Convenience
    fun isFree()    = tier == "free"
    fun isDemo()    = tier == "demo"
    fun isPro()     = tier == "pro"
    fun isProPlus() = tier == "pro+"
    fun hasAny()    = tier != "none"

    // Human-readable tier label
    fun tierLabel(): String = when (tier) {
        "free"  -> "FREE"
        "demo"  -> "TRIAL"
        "pro"   -> "PRO"
        "pro+"  -> "PRO+"
        else    -> "NONE"
    }

    // Human-readable message for locked module popup
    fun lockedMessage(route: String): String {
        val required = tierRequired(route)
        return when (required) {
            "pro+"  -> "This module is exclusive to Pro+ tier.\n\nUpgrade to Pro+ to unlock the Evolution Lab and all advanced modules."
            "pro"   -> "This module requires a Pro or Pro+ license.\n\nActivate a Pro license to unlock full access."
            else    -> "Access restricted."
        }
    }

    fun reset() { tier = "none" }
}
