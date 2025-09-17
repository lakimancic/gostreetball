package com.example.gostreetball.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class ThemeEnum {
    LIGHT, DARK
}

class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val LOC_TRACKING_ENABLED_KEY = booleanPreferencesKey("location_tracking_enabled")
        private val CHECK_RADIUS_KEY = intPreferencesKey("check_radius")
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private const val DEFAULT_RADIUS = 100
    }

    val isTrackingEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOC_TRACKING_ENABLED_KEY] ?: true
    }

    val checkRadiusMeters: Flow<Int> = dataStore.data.map { preferences ->
        preferences[CHECK_RADIUS_KEY] ?: DEFAULT_RADIUS
    }

    val selectedTheme: Flow<ThemeEnum> = dataStore.data.map { preferences ->
        val theme = preferences[THEME_KEY] ?: ThemeEnum.LIGHT.name
        ThemeEnum.valueOf(theme)
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOC_TRACKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun setCheckRadiusMeters(radiusMeters: Int) {
        val clamped = radiusMeters.coerceIn(1, 1000)
        dataStore.edit { preferences ->
            preferences[CHECK_RADIUS_KEY] = clamped
        }
    }

    suspend fun saveTheme(theme: ThemeEnum) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}