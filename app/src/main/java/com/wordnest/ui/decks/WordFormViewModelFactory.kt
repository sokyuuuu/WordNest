package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.NoOpDictionaryRepository
import com.wordnest.data.repository.WordRepository

class WordFormViewModelFactory(
    private val wordRepository: WordRepository,
    private val dictionaryRepository: DictionaryRepository = NoOpDictionaryRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordFormViewModel::class.java)) {
            return WordFormViewModel(
                wordRepository = wordRepository,
                dictionaryRepository = dictionaryRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
