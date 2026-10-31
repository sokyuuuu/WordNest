package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.entity.Word
import com.wordnest.data.remote.TranslationRepository
import com.wordnest.data.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WordEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : WordEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WordViewModel(
    val deckId: String,
    private val wordRepository: WordRepository,
    private val translationRepository: TranslationRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null = tất cả; "egg" | "chick" | "bird" = lọc theo cấp độ */
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _events = MutableSharedFlow<WordEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WordEvent> = _events.asSharedFlow()

    /** F06 + F07 (trong Tổ): kết hợp tìm kiếm và lọc trạng thái */
    val words: StateFlow<List<Word>> = combine(_searchQuery, _statusFilter) { q, status -> q to status }
        .flatMapLatest { (query, status) ->
            val base = if (query.isBlank()) {
                wordRepository.observeWordsByDeck(deckId, status)
            } else {
                wordRepository.observeSearchInDeck(deckId, query)
            }
            base
        }
        .combine(_statusFilter) { list, status ->
            if (status.isNullOrBlank()) list else list.filter { it.status == status }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun observeWordById(wordId: String): Flow<Word?> =
        wordRepository.observeWordById(wordId)

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onStatusFilterChange(status: String?) {
        _statusFilter.value = status
    }

    /** F04: Thêm từ */
    fun addWord(
        term: String,
        meaning: String,
        phonetic: String? = null,
        example: String? = null,
        note: String? = null,
        onSuccess: ((Word) -> Unit)? = null
    ) {
        viewModelScope.launch {
            wordRepository.addWord(deckId, term, meaning, phonetic, example, note)
                .onSuccess { word ->
                    _events.tryEmit(WordEvent.ShowMessage("Đã thêm \"${word.term}\""))
                    onSuccess?.invoke(word)
                }
                .onFailure { err ->
                    _events.tryEmit(
                        WordEvent.ShowMessage(err.message ?: "Không thêm được từ", isError = true)
                    )
                }
        }
    }

    /** F05: Sửa từ */
    fun updateWord(
        word: Word,
        newTerm: String,
        newMeaning: String,
        newPhonetic: String?,
        newExample: String?,
        newNote: String? = word.note,
        onSuccess: ((Word) -> Unit)? = null
    ) {
        viewModelScope.launch {
            wordRepository.updateWord(word, newTerm, newMeaning, newPhonetic, newExample, newNote)
                .onSuccess { updated ->
                    _events.tryEmit(WordEvent.ShowMessage("Đã cập nhật từ"))
                    onSuccess?.invoke(updated)
                }
                .onFailure { err ->
                    _events.tryEmit(
                        WordEvent.ShowMessage(err.message ?: "Không sửa được từ", isError = true)
                    )
                }
        }
    }

    /** F05: Xóa từ */
    fun deleteWord(
        wordId: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            wordRepository.deleteWord(wordId)
                .onSuccess {
                    _events.tryEmit(WordEvent.ShowMessage("Đã xóa từ"))
                    onSuccess?.invoke()
                }
                .onFailure { err ->
                    _events.tryEmit(
                        WordEvent.ShowMessage(err.message ?: "Không xóa được từ", isError = true)
                    )
                }
        }
    }

    /** Dịch tự động từ vựng bằng TranslationRepository (Gist API) */
    fun translateTerm(
        term: String,
        sourceLang: String = "en",
        targetLang: String = "vi",
        onResult: (String) -> Unit
    ) {
        val repo = translationRepository
        if (repo == null) {
            _events.tryEmit(WordEvent.ShowMessage("Chưa cấu hình dịch tự động", isError = true))
            return
        }
        val clean = term.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _isTranslating.value = true
            repo.translateOne(clean, sourceLang = sourceLang, targetLang = targetLang)
                .onSuccess { translated ->
                    _isTranslating.value = false
                    onResult(translated)
                }
                .onFailure { err ->
                    _isTranslating.value = false
                    _events.tryEmit(
                        WordEvent.ShowMessage(
                            err.message ?: "Không dịch được từ, vui lòng nhập thủ công",
                            isError = true
                        )
                    )
                }
        }
    }
}
