package com.wordnest.di

import android.content.Context
import android.content.SharedPreferences
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.FakeDictionaryRepository
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.remote.TranslationRepository
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.DefaultUserRepository
import com.wordnest.data.repository.SettingsRepository
import com.wordnest.data.repository.StudyHistoryRepository
import com.wordnest.data.repository.UserRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.data.repository.wordNestDataStore
import com.wordnest.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("wordnest_prefs", Context.MODE_PRIVATE)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    val userRepository: UserRepository by lazy {
        DefaultUserRepository(database.userDao(), database.settingDao())
    }

    val deckRepository: DeckRepository by lazy {
        DeckRepository(database.deckDao())
    }

    val wordRepository: WordRepository by lazy {
        WordRepository(database.wordDao(), database.deckDao())
    }

    val dictionaryRepository: DictionaryRepository by lazy {
        FakeDictionaryRepository()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.wordNestDataStore)
    }

    val studyHistoryRepository: StudyHistoryRepository by lazy {
        StudyHistoryRepository(appContext.wordNestDataStore)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext)
    }

    private val _autoSuggestEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SUGGEST, true)
    )
    val autoSuggestEnabled: StateFlow<Boolean> = _autoSuggestEnabled.asStateFlow()

    private val _deeplApiKey = MutableStateFlow(
        prefs.getString(KEY_DEEPL_API_KEY, "") ?: ""
    )
    val deeplApiKey: StateFlow<String> = _deeplApiKey.asStateFlow()

    val translationRepository: TranslationRepository by lazy {
        TranslationRepository(apiKeyProvider = { _deeplApiKey.value })
    }

    fun setAutoSuggestEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SUGGEST, enabled).apply()
        _autoSuggestEnabled.value = enabled
    }

    fun setDeeplApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_DEEPL_API_KEY, trimmed).apply()
        _deeplApiKey.value = trimmed
    }

    companion object {
        private const val KEY_AUTO_SUGGEST = "auto_suggest_enabled"
        private const val KEY_DEEPL_API_KEY = "deepl_api_key"
    }
}
