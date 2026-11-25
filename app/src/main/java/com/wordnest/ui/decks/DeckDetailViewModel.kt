package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.DeckWithWords
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.util.WordStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckDetailUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val deck: Deck? = null,
    val deckWithWords: DeckWithWords? = null,
    val words: List<Word> = emptyList(),
    val eggCount: Int = 0,
    val chickCount: Int = 0,
    val birdCount: Int = 0,
) {
    val totalWords: Int get() = words.size

    /** Tỉ lệ đã thuộc = chim non + chim bay trên tổng số từ (khớp US2 AC 2 và DeckDao). */
    val learnedWords: Int get() = chickCount + birdCount

    val progressPercent: Float
        get() = if (totalWords <= 0) 0f else (learnedWords.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)
}

sealed interface DeckDetailEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : DeckDetailEvent
    data object WordDeleted : DeckDetailEvent
}

/**
 * ViewModel cho màn chi tiết Tổ từ vựng (F06):
 * - Quan sát [deckWithWords] và danh sách [words] (sắp xếp mới nhất lên đầu) trực tiếp qua Room Flow.
 * - Tính tổng từ, số từ theo 3 trạng thái chim (🥚/🐣/🕊️) và tỉ lệ tiến độ %.
 * - Hỗ trợ nhận biết Tổ bị xoá ([DeckDetailUiState.isMissing]) và xoá mềm từ ([deleteWord]).
 *
 * Lưu ý: bộ lọc trạng thái từ của nhánh cũ để dành v1.2 để giữ layout gọn gàng khớp Figma (T040).
 */
class DeckDetailViewModel(
    val deckId: String,
    private val deckRepository: DeckRepository,
    private val wordRepository: WordRepository,
) : ViewModel() {

    val uiState: StateFlow<DeckDetailUiState> = combine(
        deckRepository.observeDeckWithWords(deckId),
        wordRepository.observeWordsByDeck(deckId),
    ) { rawDeckWithWords, orderedWords ->
        if (rawDeckWithWords == null) {
            DeckDetailUiState(
                isLoading = false,
                isMissing = true,
            )
        } else {
            var eggCount = 0
            var chickCount = 0
            var birdCount = 0
            for (word in orderedWords) {
                when (WordStatus.fromStorage(word.status)) {
                    WordStatus.EGG -> eggCount++
                    WordStatus.CHICK -> chickCount++
                    WordStatus.BIRD -> birdCount++
                }
            }
            DeckDetailUiState(
                isLoading = false,
                isMissing = false,
                deck = rawDeckWithWords.deck,
                deckWithWords = rawDeckWithWords.copy(words = orderedWords),
                words = orderedWords,
                eggCount = eggCount,
                chickCount = chickCount,
                birdCount = birdCount,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckDetailUiState(isLoading = true),
    )

    val deckWithWords: StateFlow<DeckWithWords?> = uiState
        .map { it.deckWithWords }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val words: StateFlow<List<Word>> = uiState
        .map { it.words }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _events = MutableSharedFlow<DeckDetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DeckDetailEvent> = _events.asSharedFlow()

    /** F05: Xoá mềm từ qua Room (tuân thủ hợp đồng Firestore FB3). */
    fun deleteWord(
        wordId: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            wordRepository.deleteWord(wordId)
                .onSuccess {
                    _events.tryEmit(DeckDetailEvent.WordDeleted)
                    _events.tryEmit(DeckDetailEvent.ShowMessage("Đã xóa từ"))
                    onSuccess()
                }
                .onFailure { err ->
                    _events.tryEmit(
                        DeckDetailEvent.ShowMessage(err.message ?: "Không xóa được từ", isError = true),
                    )
                }
        }
    }
}
