package com.shvertex.supaadmin.license

import com.shvertex.supaadmin.data.NavScreen

/**
 * FeatureAccess
 * ─────────────
 * Single source of truth for which tier a given screen or granular action
 * requires. Nothing else in the app should hardcode a tier check — route
 * everything through here so the gating rules stay in one place.
 *
 * Tab-level mapping (per spec):
 *   Home:      Dashboard/Overview/Projects = Free · Usage = Pro
 *   Database:  Tables/SQL/Policies/Migrations/Cron/Webhooks = Pro
 *              (row delete + Schema Dump = Pro+, see [PRO_PLUS_DELETE_ROW] / [PRO_PLUS_SCHEMA_DUMP])
 *   Auth:      Users/Secrets = Pro
 *   DevTools:  Storage/Functions/Logs/Realtime = Free
 *   More:      Connections/Credentials/Settings = Free · Web Dashboard = Pro
 */
object FeatureAccess {

    // Marker constants for the two granular Pro+ actions that live *inside*
    // a Pro-tier screen rather than being their own NavScreen.
    const val PRO_PLUS_DELETE_ROW    = "pro_plus_delete_row"
    const val PRO_PLUS_SCHEMA_DUMP   = "pro_plus_schema_dump"

    fun requiredTier(screen: NavScreen): SupaLicense.Tier = when (screen) {
        // Home
        NavScreen.DASHBOARD, NavScreen.OVERVIEW, NavScreen.PROJECTS -> SupaLicense.Tier.FREE
        NavScreen.USAGE -> SupaLicense.Tier.PRO

        // Database
        NavScreen.TABLES, NavScreen.SQL, NavScreen.POLICIES,
        NavScreen.MIGRATIONS, NavScreen.CRON, NavScreen.WEBHOOKS -> SupaLicense.Tier.PRO

        // Auth
        NavScreen.USERS, NavScreen.SECRETS -> SupaLicense.Tier.PRO

        // DevTools
        NavScreen.STORAGE, NavScreen.FUNCTIONS,
        NavScreen.LOGS, NavScreen.REALTIME -> SupaLicense.Tier.FREE

        // More
        NavScreen.CONNECTIONS, NavScreen.CREDENTIALS, NavScreen.SETTINGS -> SupaLicense.Tier.FREE
        NavScreen.WEB_DASHBOARD -> SupaLicense.Tier.PRO
    }

    /** true if [have] meets or exceeds [need]. FREE < PRO < PRO_PLUS. */
    fun meets(have: SupaLicense.Tier, need: SupaLicense.Tier): Boolean {
        fun rank(t: SupaLicense.Tier) = when (t) {
            SupaLicense.Tier.FREE -> 0
            SupaLicense.Tier.PRO -> 1
            SupaLicense.Tier.PRO_PLUS -> 2
        }
        return rank(have) >= rank(need)
    }

    fun canAccess(currentTier: SupaLicense.Tier, screen: NavScreen): Boolean =
        meets(currentTier, requiredTier(screen))
}
