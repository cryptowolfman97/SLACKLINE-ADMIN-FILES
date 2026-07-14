package com.example.omnicortex.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.ds by preferencesDataStore("aegis_prefs")

object AegisPreferences {

    private val KEY_IS_PRO              = booleanPreferencesKey("is_pro")
    private val KEY_APP_PIN             = stringPreferencesKey("app_pin")
    private val KEY_BIOMETRIC_ENABLED   = booleanPreferencesKey("biometric_enabled")
    private val KEY_BREACH_WATCHLIST    = stringPreferencesKey("breach_watchlist")
    private val KEY_LAST_BREACH_CHECK   = longPreferencesKey("last_breach_check")
    private val KEY_NOTIFICATIONS_ON    = booleanPreferencesKey("notifications_on")
    private val KEY_LAST_POSTURE_SCAN   = longPreferencesKey("last_posture_scan")
    private val KEY_DARK_THEME          = booleanPreferencesKey("dark_theme")
    private val KEY_NETWORK_PERM_GRANTED = booleanPreferencesKey("network_perm_granted")

    // ── Flows ─────────────────────────────────────────────────────────────────
    fun isProFlow(ctx: Context): Flow<Boolean> =
        ctx.ds.data.map { it[KEY_IS_PRO] ?: false }

    fun appPinFlow(ctx: Context): Flow<String> =
        ctx.ds.data.map { it[KEY_APP_PIN] ?: "" }

    fun biometricFlow(ctx: Context): Flow<Boolean> =
        ctx.ds.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    fun watchlistFlow(ctx: Context): Flow<String> =
        ctx.ds.data.map { it[KEY_BREACH_WATCHLIST] ?: "[]" }

    fun notificationsFlow(ctx: Context): Flow<Boolean> =
        ctx.ds.data.map { it[KEY_NOTIFICATIONS_ON] ?: true }

    fun lastPostureScanFlow(ctx: Context): Flow<Long> =
        ctx.ds.data.map { it[KEY_LAST_POSTURE_SCAN] ?: 0L }

    fun darkThemeFlow(ctx: Context): Flow<Boolean> =
        ctx.ds.data.map { it[KEY_DARK_THEME] ?: true }

    fun networkPermGrantedFlow(ctx: Context): Flow<Boolean> =
        ctx.ds.data.map { it[KEY_NETWORK_PERM_GRANTED] ?: false }

    // ── Synchronous read for startup (PIN check before first frame) ───────────
    fun getAppPin(ctx: Context): String =
        runBlocking { appPinFlow(ctx).first() }

    fun getBiometric(ctx: Context): Boolean =
        runBlocking { biometricFlow(ctx).first() }

    // ── Setters ───────────────────────────────────────────────────────────────
    suspend fun setIsPro(ctx: Context, v: Boolean) =
        ctx.ds.edit { it[KEY_IS_PRO] = v }

    suspend fun setAppPin(ctx: Context, pin: String) =
        ctx.ds.edit { it[KEY_APP_PIN] = pin }

    suspend fun setBiometric(ctx: Context, v: Boolean) =
        ctx.ds.edit { it[KEY_BIOMETRIC_ENABLED] = v }

    suspend fun setWatchlist(ctx: Context, json: String) =
        ctx.ds.edit { it[KEY_BREACH_WATCHLIST] = json }

    suspend fun setLastBreachCheck(ctx: Context, t: Long) =
        ctx.ds.edit { it[KEY_LAST_BREACH_CHECK] = t }

    suspend fun setNotifications(ctx: Context, v: Boolean) =
        ctx.ds.edit { it[KEY_NOTIFICATIONS_ON] = v }

    suspend fun setLastPostureScan(ctx: Context, t: Long) =
        ctx.ds.edit { it[KEY_LAST_POSTURE_SCAN] = t }

    suspend fun setDarkTheme(ctx: Context, v: Boolean) =
        ctx.ds.edit { it[KEY_DARK_THEME] = v }

    suspend fun setNetworkPermGranted(ctx: Context, v: Boolean) =
        ctx.ds.edit { it[KEY_NETWORK_PERM_GRANTED] = v }
}
