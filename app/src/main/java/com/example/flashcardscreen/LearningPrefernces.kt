
package com.example.flashcardscreen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.learningDataStore by preferencesDataStore(
    name = "learning_data"
)

class LearningPreferences(private val context: Context) {

    companion object {
        private val REMEMBERED_COUNT =
            intPreferencesKey("remembered_count")

        private val QUIZ_SCORE =
            intPreferencesKey("quiz_score")
    }

    val rememberedCount = context.learningDataStore.data.map {
            preferences ->
        preferences[REMEMBERED_COUNT] ?: 0
    }

    val quizScore = context.learningDataStore.data.map {
            preferences ->
        preferences[QUIZ_SCORE] ?: 0
    }

    suspend fun saveRememberedCount(count: Int) {
        context.learningDataStore.edit { preferences ->
            preferences[REMEMBERED_COUNT] = count
        }
    }

    suspend fun saveQuizScore(score: Int) {
        context.learningDataStore.edit { preferences ->
            preferences[QUIZ_SCORE] = score
        }
    }
}
