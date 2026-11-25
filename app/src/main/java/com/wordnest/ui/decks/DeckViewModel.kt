package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.repository.DeckRepository
import com.wordnest.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckListUiState(
    val decks: List<DeckWithStats> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Phân biệt trạng thái đang tải ban đầu với trường hợp Tổ không tồn tại / đã bị xoá (T035),
 * giúp màn chi tiết hoặc màn sửa tự quay về danh sách mà không crash.
 */
sealed interface DeckLoadState {
    data object Loading : DeckLoadState
    data class Found(val deck: Deck) : DeckLoadState
    data object Missing : DeckLoadState
}

sealed interface DeckEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : DeckEvent
    data class DeckCreated(val deck: Deck) : DeckEvent
    data object DeckUpdated : DeckEvent
    data object DeckDeleted : DeckEvent
}

/**
 * ViewModel quản lý Tổ từ vựng (F01, F02, F03):
 * - Quan sát danh sách Tổ kèm thống kê từ Room (`observeDecksWithStats`).
 * - Tạo, sửa và xoá mềm Tổ nguyên tử kèm toàn bộ từ con.
 */
class DeckViewModel(
    private val deckRepository: DeckRepository,
    private val userId: String = Constants.DEFAULT_USER_ID,
) : ViewModel() {

    val uiState: StateFlow<DeckListUiState> = deckRepository
        .observeDecksWithStats(userId)
        .map { list -> DeckListUiState(decks = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeckListUiState(isLoading = true),
        )

    val decks: StateFlow<List<DeckWithStats>> = uiState
        .map { it.decks }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<DeckEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DeckEvent> = _events.asSharedFlow()

    fun observeDecksWithStats(): Flow<List<DeckWithStats>> =
        deckRepository.observeDecksWithStats(userId)

    fun observeDeckById(deckId: String): Flow<Deck?> =
        deckRepository.observeDeckById(deckId)

    fun observeDeckLoadState(deckId: String): Flow<DeckLoadState> =
        deckRepository.observeDeckById(deckId).map { deck ->
            if (deck == null) DeckLoadState.Missing else DeckLoadState.Found(deck)
        }

    fun observeDeckWithStats(deckId: String): Flow<DeckWithStats?> =
        deckRepository.observeDeckWithStats(deckId)

    /** F01: Tạo Tổ mới. */
    fun createDeck(
        name: String,
        description: String?,
        icon: String = DeckStyle.DEFAULT_ICON,
        color: String = DeckStyle.DEFAULT_COLOR,
        onSuccess: (Deck) -> Unit = {},
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                deckRepository.createDeck(
                    userId = userId,
                    name = name,
                    description = description,
                    icon = icon,
                    color = color,
                ).onSuccess { deck ->
                    _events.tryEmit(DeckEvent.DeckCreated(deck))
                    _events.tryEmit(DeckEvent.ShowMessage("Đã tạo Tổ \"${deck.nameDeck}\""))
                    onSuccess(deck)
                }.onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không tạo được Tổ", isError = true),
                    )
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** F02: Sửa tên / mô tả / biểu tượng / màu Tổ. */
    fun updateDeck(
        deckId: String,
        newName: String,
        newDescription: String?,
        newIcon: String? = null,
        newColor: String? = null,
        onSuccess: () -> Unit = {},
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                deckRepository.updateDeck(
                    deckId = deckId,
                    userId = userId,
                    newName = newName,
                    newDescription = newDescription,
                    newIcon = newIcon,
                    newColor = newColor,
                ).onSuccess {
                    _events.tryEmit(DeckEvent.DeckUpdated)
                    _events.tryEmit(DeckEvent.ShowMessage("Đã cập nhật Tổ"))
                    onSuccess()
                }.onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không sửa được Tổ", isError = true),
                    )
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** F02: Xoá mềm Tổ cùng toàn bộ từ bên trong (tuân thủ hợp đồng Firestore FB3). */
    fun deleteDeck(
        deckId: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            deckRepository.deleteDeck(deckId)
                .onSuccess {
                    _events.tryEmit(DeckEvent.DeckDeleted)
                    _events.tryEmit(DeckEvent.ShowMessage("Đã xóa Tổ"))
                    onSuccess()
                }
                .onFailure { err ->
                    _events.tryEmit(
                        DeckEvent.ShowMessage(err.message ?: "Không xóa được Tổ", isError = true),
                    )
                }
        }
    }
}
