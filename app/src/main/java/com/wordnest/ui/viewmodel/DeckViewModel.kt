package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.repository.DeckRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckUiState(
    val decks: List<DeckWithStats> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

sealed interface DeckEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : DeckEvent
    data class DeckCreated(val deck: Deck) : DeckEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModel(
    private val userId: String,
    private val deckRepository: DeckRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val decks: StateFlow<List<DeckWithStats>> = searchQuery
        .flatMapLatest { query ->
            deckRepository.observeSearchDecks(userId, query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _queryState = MutableStateFlow("")
    val queryState: StateFlow<String> = _queryState.asStateFlow()

    private val _events = MutableSharedFlow<DeckEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DeckEvent> = _events.asSharedFlow()

    fun onSearchQueryChange(newQuery: String) {
        _queryState.value = newQuery
        searchQuery.value = newQuery
    }

    fun observeDeckWithStats(deckId: String): Flow<DeckWithStats?> =
        deckRepository.observeDeckWithStats(deckId)

    fun observeDeckById(deckId: String): Flow<Deck?> =
        deckRepository.observeDeckById(deckId)

    /** F01: Tạo Tổ */
    fun createDeck(
        name: String,
        description: String?,
        icon: String = DeckStyle.DEFAULT_ICON,
        color: String = DeckStyle.DEFAULT_COLOR,
        onSuccess: ((Deck) -> Unit)? = null
    ) {
        viewModelScope.launch {
            deckRepository.createDeck(userId, name, description, icon, color)
                .onSuccess { deck ->
                    _events.tryEmit(DeckEvent.DeckCreated(deck))
                    _events.tryEmit(DeckEvent.ShowMessage("Đã tạo Tổ \"${deck.nameDeck}\""))
                    onSuccess?.invoke(deck)
                }
                .onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không tạo được Tổ", isError = true)
                    )
                }
        }
    }

    /** F02: Sửa Tổ */
    fun updateDeck(
        deckId: String,
        newName: String,
        newDescription: String?,
        newIcon: String? = null,
        newColor: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            deckRepository.updateDeck(deckId, userId, newName, newDescription, newIcon, newColor)
                .onSuccess {
                    _events.tryEmit(DeckEvent.ShowMessage("Đã cập nhật Tổ"))
                    onSuccess?.invoke()
                }
                .onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không sửa được Tổ", isError = true)
                    )
                }
        }
    }

    /** F02: Xóa Tổ */
    fun deleteDeck(
        deckId: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            deckRepository.deleteDeck(deckId)
                .onSuccess {
                    _events.tryEmit(DeckEvent.ShowMessage("Đã xóa Tổ"))
                    onSuccess?.invoke()
                }
                .onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không xóa được Tổ", isError = true)
                    )
                }
        }
    }
}
