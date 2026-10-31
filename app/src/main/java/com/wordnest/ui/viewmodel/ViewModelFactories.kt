package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordnest.di.AppContainer
import com.wordnest.util.Constants

object ViewModelFactories {
    fun deck(container: AppContainer): ViewModelProvider.Factory =
        singleFactory {
            DeckViewModel(
                userId = Constants.DEFAULT_USER_ID,
                deckRepository = container.deckRepository
            )
        }

    fun word(container: AppContainer, deckId: String): ViewModelProvider.Factory =
        singleFactory {
            WordViewModel(
                deckId = deckId,
                wordRepository = container.wordRepository,
                translationRepository = container.translationRepository
            )
        }

    fun addWord(container: AppContainer, deckId: String): ViewModelProvider.Factory =
        singleFactory {
            AddWordViewModel(
                deckId = deckId,
                dictionary = container.dictionaryRepository,
                wordRepository = container.wordRepository,
                autoSuggestEnabled = container.autoSuggestEnabled.value
            )
        }

    fun search(container: AppContainer): ViewModelProvider.Factory =
        singleFactory {
            SearchViewModel(
                userId = Constants.DEFAULT_USER_ID,
                deckRepository = container.deckRepository,
                wordRepository = container.wordRepository
            )
        }

    fun home(container: AppContainer): ViewModelProvider.Factory =
        singleFactory {
            HomeViewModel(
                studyHistoryRepository = container.studyHistoryRepository
            )
        }

    fun settings(container: AppContainer): ViewModelProvider.Factory =
        singleFactory {
            SettingsViewModel(
                repository = container.settingsRepository,
                scheduler = container.reminderScheduler,
                autoSuggestEnabled = container.autoSuggestEnabled,
                deeplApiKey = container.deeplApiKey,
                onSetAutoSuggest = container::setAutoSuggestEnabled,
                onSetDeeplApiKey = container::setDeeplApiKey
            )
        }

    private inline fun <reified VM : ViewModel> singleFactory(
        crossinline creator: () -> VM
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }
}
