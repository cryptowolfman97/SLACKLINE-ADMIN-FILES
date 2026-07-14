package com.example.interstellarcalc.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.interstellarcalc.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object ThemePreference {
    private val KEY = stringPreferencesKey("app_theme")

    fun getTheme(context: Context): Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            val name = prefs[KEY] ?: AppTheme.AMOLED.name
            AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.AMOLED
        }

    suspend fun setTheme(context: Context, theme: AppTheme) {
        context.dataStore.edit { it[KEY] = theme.name }
    }
}
