package com.wordnest.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StudyHistoryRepository(private val context: Context) {

    private val studyDaysKey = stringSetPreferencesKey("study_days")

    val studyDays: Flow<Set<LocalDate>> = context.wordNestDataStore.data.map { prefs ->
        prefs[studyDaysKey].orEmpty()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
    }

    suspend fun recordStudyDay(date: LocalDate) {
        context.wordNestDataStore.edit { prefs ->
            prefs[studyDaysKey] = prefs[studyDaysKey].orEmpty() + date.toString()
        }
    }
}
