package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val decks: List<DeckWithStats> = emptyList(),
    val words: List<Word> = emptyList()
)

/**
 * ViewModel cho màn hình Tìm kiếm toàn cục (F07):
 * Tìm đồng thời các Tổ từ và Từ vựng khớp từ khóa trên toàn bộ tài khoản user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val userId: String,
    private val deckRepository: DeckRepository,
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<SearchUiState> = _query
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.isEmpty()) {
                flowOf(SearchUiState(query = q))
            } else {
                combine(
                    deckRepository.observeSearchDecks(userId, trimmed),
                    wordRepository.observeSearchAllWords(userId, trimmed)
                ) { decks, words ->
                    SearchUiState(query = q, decks = decks, words = words)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clear() {
        _query.value = ""
    }
}
