package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordnest.data.repository.DeckRepository
import com.wordnest.util.Constants

class DeckViewModelFactory(
    private val deckRepository: DeckRepository,
    private val userId: String = Constants.DEFAULT_USER_ID,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            return DeckViewModel(
                deckRepository = deckRepository,
                userId = userId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
