package com.wordnest.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

val Context.wordNestDataStore: DataStore<Preferences> by preferencesDataStore(name = "wordnest_datastore")

data class AppSettings(
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
    val darkMode: Boolean = false
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val enabled = prefs[KEY_REMINDER_ENABLED] ?: false
        val hour = prefs[KEY_REMINDER_HOUR] ?: DEFAULT_HOUR
        val minute = prefs[KEY_REMINDER_MINUTE] ?: DEFAULT_MINUTE
        val dark = prefs[KEY_DARK_MODE] ?: false
        AppSettings(
            reminderEnabled = enabled,
            reminderTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
            darkMode = dark
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(time: LocalTime) {
        dataStore.edit {
            it[KEY_REMINDER_HOUR] = time.hour
            it[KEY_REMINDER_MINUTE] = time.minute
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    companion object {
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

        const val DEFAULT_HOUR = 20
        const val DEFAULT_MINUTE = 0
    }
}

class StudyHistoryRepository(private val dataStore: DataStore<Preferences>) {

    val studyDays: Flow<Set<LocalDate>> = dataStore.data.map { prefs ->
        prefs[KEY_STUDY_DAYS].orEmpty().mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }.toSet()
    }

    suspend fun recordStudyDay(date: LocalDate = LocalDate.now()) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_STUDY_DAYS].orEmpty()
            prefs[KEY_STUDY_DAYS] = current + date.toString()
        }
    }

    companion object {
        private val KEY_STUDY_DAYS = stringSetPreferencesKey("study_days")
    }
}
