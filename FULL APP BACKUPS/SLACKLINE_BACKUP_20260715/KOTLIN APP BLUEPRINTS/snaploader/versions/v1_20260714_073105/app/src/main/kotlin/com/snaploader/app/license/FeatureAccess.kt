package com.snaploader.app.license

/**
 * FeatureAccess
 * ─────────────
 * Unlike Supa Studio, SHV Downloader doesn't gate whole screens — every
 * tab (Home / Web Tools / Downloads / Settings) is reachable by everyone.
 * What's gated is a set of granular actions and limits inside those
 * screens. This is the single source of truth for all of them.
 */
object FeatureAccess {

    /** Free = 1 concurrent, Pro = 3, Pro+ = 5. */
    fun maxConcurrentFor(tier: SHVLicense.Tier): Int = when (tier) {
        SHVLicense.Tier.FREE -> 1
        SHVLicense.Tier.PRO -> 3
        SHVLicense.Tier.PRO_PLUS -> 5
    }

    /** Batch/multi-URL download — Pro and above. */
    fun canBatchDownload(tier: SHVLicense.Tier): Boolean = tier != SHVLicense.Tier.FREE

    /** WebTools built-in ad-blocker — Pro and above. */
    fun canUseAdBlock(tier: SHVLicense.Tier): Boolean = tier != SHVLicense.Tier.FREE

    /** Accent colour customization — Pro and above. */
    fun canCustomizeAccent(tier: SHVLicense.Tier): Boolean = tier != SHVLicense.Tier.FREE

    /** Auto-quality prefs, filename template, subtitles, sequential queue — Pro and above. */
    fun canUseAdvancedSettings(tier: SHVLicense.Tier): Boolean = tier != SHVLicense.Tier.FREE

    /** Floating window / PiP — Pro+ only. */
    fun canUseFloatingWindow(tier: SHVLicense.Tier): Boolean = tier == SHVLicense.Tier.PRO_PLUS

    /** Adding custom WebTools sites — Pro+ only. */
    fun canAddCustomSite(tier: SHVLicense.Tier): Boolean = tier == SHVLicense.Tier.PRO_PLUS
}
