package com.example.finalwork1.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.settingsStore by preferencesDataStore(name = "app_settings")

data class AppPreferences(val showHidden: Boolean = false, val preloadPages: Int = 2, val darkTheme: Boolean = true)

class PreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val showHiddenKey = booleanPreferencesKey("show_hidden")
    private val preloadKey = intPreferencesKey("preload_pages")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val preferences: Flow<AppPreferences> = context.settingsStore.data.map { p ->
        AppPreferences(
            showHidden = p[showHiddenKey] ?: false,
            preloadPages = (p[preloadKey] ?: 2).coerceIn(1, 5),
            darkTheme = p[darkThemeKey] ?: true,
        )
    }

    suspend fun setShowHidden(value: Boolean) { context.settingsStore.edit { it[showHiddenKey] = value } }
    suspend fun setPreloadPages(value: Int) { context.settingsStore.edit { it[preloadKey] = value.coerceIn(1, 5) } }
    suspend fun setDarkTheme(value: Boolean) { context.settingsStore.edit { it[darkThemeKey] = value } }
}
