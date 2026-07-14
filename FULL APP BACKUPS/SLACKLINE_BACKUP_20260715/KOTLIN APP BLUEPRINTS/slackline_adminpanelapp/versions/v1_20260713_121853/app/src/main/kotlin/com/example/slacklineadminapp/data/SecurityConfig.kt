package com.example.slacklineadminapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "security_prefs")

object SecurityConfig {
    private val APP_PIN      = stringPreferencesKey("app_pin")
    private val ADV_PIN      = stringPreferencesKey("adv_pin")
    private val THEME        = stringPreferencesKey("theme")
    private val COMPANY_NAME   = stringPreferencesKey("company_name")
    private val STORE_EMAIL    = stringPreferencesKey("store_email")
    private val STORE_PASSWORD = stringPreferencesKey("store_password")
    private val MATRIX_ANIMATION_ENABLED = booleanPreferencesKey("matrix_animation_enabled")

    data class Cfg(
        val appPin: String = "",
        val advPin: String = "",
        val theme: String = "Dark",
        val companyName: String = "SLACKLINE by SHV",
        val storeEmail: String = "",
        val storePassword: String = "",
        val matrixAnimationEnabled: Boolean = true
    )

    fun getFlow(ctx: Context): Flow<Cfg> = ctx.dataStore.data.map { p ->
        Cfg(
            appPin      = p[APP_PIN]      ?: "",
            advPin      = p[ADV_PIN]      ?: "",
            theme       = p[THEME]        ?: "Dark",
            companyName   = p[COMPANY_NAME]   ?: "SLACKLINE by SHV",
            storeEmail    = p[STORE_EMAIL]    ?: "",
            storePassword = p[STORE_PASSWORD] ?: "",
            matrixAnimationEnabled = p[MATRIX_ANIMATION_ENABLED] ?: true
        )
    }

    fun get(ctx: Context): Cfg = runBlocking { getFlow(ctx).first() }

    suspend fun setAppPin(ctx: Context, pin: String)    { ctx.dataStore.edit { it[APP_PIN]      = pin } }
    suspend fun setAdvPin(ctx: Context, pin: String)    { ctx.dataStore.edit { it[ADV_PIN]      = pin } }
    suspend fun setTheme(ctx: Context, theme: String)   { ctx.dataStore.edit { it[THEME]        = theme } }
    suspend fun setCompanyName(ctx: Context, n: String)     { ctx.dataStore.edit { it[COMPANY_NAME]   = n } }
    suspend fun setStoreEmail(ctx: Context, email: String)    { ctx.dataStore.edit { it[STORE_EMAIL]    = email } }
    suspend fun setStorePassword(ctx: Context, pwd: String)   { ctx.dataStore.edit { it[STORE_PASSWORD] = pwd } }
    suspend fun setMatrixAnimationEnabled(ctx: Context, enabled: Boolean) { ctx.dataStore.edit { it[MATRIX_ANIMATION_ENABLED] = enabled } }
}
