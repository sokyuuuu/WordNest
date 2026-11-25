package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository

class DeckDetailViewModelFactory(
    private val deckId: String,
    private val deckRepository: DeckRepository,
    private val wordRepository: WordRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckDetailViewModel::class.java)) {
            return DeckDetailViewModel(
                deckId = deckId,
                deckRepository = deckRepository,
                wordRepository = wordRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
