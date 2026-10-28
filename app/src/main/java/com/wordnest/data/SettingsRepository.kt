package com.wordnest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

data class AppSettings(
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
    val darkMode: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val settings: Flow<AppSettings> = context.wordNestDataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            reminderEnabled = prefs[reminderEnabledKey] ?: defaults.reminderEnabled,
            reminderTime = LocalTime.of(
                prefs[reminderHourKey] ?: defaults.reminderTime.hour,
                prefs[reminderMinuteKey] ?: defaults.reminderTime.minute,
            ),
            darkMode = prefs[darkModeKey] ?: defaults.darkMode,
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.wordNestDataStore.edit { it[reminderEnabledKey] = enabled }
    }

    suspend fun setReminderTime(time: LocalTime) {
        context.wordNestDataStore.edit {
            it[reminderHourKey] = time.hour
            it[reminderMinuteKey] = time.minute
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.wordNestDataStore.edit { it[darkModeKey] = enabled }
    }
}
