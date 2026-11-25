package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordnest.data.repository.WordRepository
import com.wordnest.util.Constants

class SearchViewModelFactory(
    private val wordRepository: WordRepository,
    private val userId: String = Constants.DEFAULT_USER_ID,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(
                wordRepository = wordRepository,
                userId = userId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
