package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.NoOpDictionaryRepository
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.WordRepository
import com.wordnest.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phân biệt trạng thái đang tải với trường hợp từ không tồn tại / đã bị xoá mềm,
 * giúp màn sửa từ tự quay về danh sách mà không crash.
 */
sealed interface WordLoadState {
    data object Loading : WordLoadState
    data class Found(val word: Word) : WordLoadState
    data object Missing : WordLoadState
}

/** Trạng thái tra cứu từ điển & dịch tự động kiểu Quizlet. */
sealed interface WordSuggestStatus {
    data object Idle : WordSuggestStatus
    data object Loading : WordSuggestStatus
    data object Empty : WordSuggestStatus
    data class Error(val message: String) : WordSuggestStatus
}

data class WordSuggestUiState(
    val suggestions: List<String> = emptyList(),
    val meaningOptions: List<String> = emptyList(),
    val selectedMeanings: List<String> = emptyList(),
    val exampleOptions: List<String> = emptyList(),
    val status: WordSuggestStatus = WordSuggestStatus.Idle,
)

sealed interface WordFormEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : WordFormEvent
    data class WordCreated(val word: Word) : WordFormEvent
    data class WordUpdated(val word: Word) : WordFormEvent
    data object WordDeleted : WordFormEvent

    /** Điền tự động các ô còn trống / chưa sửa tay sau khi chọn gợi ý hoặc bấm "Tra & dịch". */
    data class ApplySuggestions(
        val term: String,
        val phoneticToFill: String? = null,
        val meaningToFill: String? = null,
    ) : WordFormEvent
}

/**
 * ViewModel cho form Thêm mới & Chỉnh sửa từ vựng dùng chung (F04, F05):
 * - [createWord]: thêm từ qua [WordRepository.addWord] với trạng thái mặc định `egg` (🥚 Trứng).
 * - [updateWord]: sửa nội dung qua [WordRepository.updateWord], giữ nguyên tuyệt đối
 *   `status`, `correctStreak`, `wrongCount`, `intervalDays`, `nextReviewAt`, `lastReviewedAt`.
 * - Gợi ý từ điển & dịch tự động kiểu Quizlet qua [DictionaryRepository] (autocomplete từ,
 *   tự điền phiên âm IPA, chọn nhiều chip nghĩa tiếng Việt và câu ví dụ tiếng Anh).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WordFormViewModel(
    private val wordRepository: WordRepository,
    private val dictionaryRepository: DictionaryRepository = NoOpDictionaryRepository,
    private val debounceMs: Long = DEFAULT_SUGGEST_DEBOUNCE_MS,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _suggestState = MutableStateFlow(WordSuggestUiState())
    val suggestState: StateFlow<WordSuggestUiState> = _suggestState.asStateFlow()

    private val _events = MutableSharedFlow<WordFormEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WordFormEvent> = _events.asSharedFlow()

    private val autocompleteQuery = MutableStateFlow("")

    @Volatile
    private var lockedTerm: String? = null
    private var detailsTerm: String? = null
    private var phoneticEditedByUser = false
    private var phoneticOwnerTerm: String? = null
    private var detailsJob: Job? = null
    private var statusFromDetails = false

    init {
        if (dictionaryRepository !== NoOpDictionaryRepository) {
            viewModelScope.launch {
                autocompleteFlow().collect { result -> applyAutocomplete(result) }
            }
        }
    }

    fun observeWordLoadState(wordId: String): Flow<WordLoadState> =
        wordRepository.observeWordById(wordId).map { word ->
            if (word == null) WordLoadState.Missing else WordLoadState.Found(word)
        }

    // ---------------------------------------------------------------------
    // Gợi ý từ điển & dịch tự động kiểu Quizlet
    // ---------------------------------------------------------------------

    /** Khoá từ ban đầu khi mở màn Sửa từ để không tự bật dropdown ngoài ý muốn. */
    fun lockInitialTerm(initialTerm: String, hasExistingPhonetic: Boolean) {
        val clean = initialTerm.trim()
        lockedTerm = clean
        if (hasExistingPhonetic && clean.isNotEmpty()) {
            phoneticEditedByUser = true
            phoneticOwnerTerm = normalize(clean)
        }
        _suggestState.update { it.copy(suggestions = emptyList()) }
    }

    /** Cập nhật khi người dùng gõ tay vào ô Từ tiếng Anh. */
    fun onTermInputChanged(value: String) {
        if (dictionaryRepository === NoOpDictionaryRepository) return
        if (!value.trim().equals(lockedTerm, ignoreCase = true)) {
            lockedTerm = null
        }

        val key = normalize(value)
        if (detailsTerm != null && key != detailsTerm) {
            detailsTerm = null
            detailsJob?.cancel()
            statusFromDetails = false
            _suggestState.update {
                it.copy(
                    meaningOptions = emptyList(),
                    selectedMeanings = emptyList(),
                    exampleOptions = emptyList(),
                    status = WordSuggestStatus.Idle,
                )
            }
        }
        autocompleteQuery.value = value
    }

    /** Ghi nhận người dùng tự sửa ô Phiên âm để không bị ghi đè khi tra lại cùng từ đó. */
    fun onPhoneticInputChanged(currentTerm: String, value: String) {
        phoneticEditedByUser = value.isNotBlank()
        phoneticOwnerTerm = normalize(currentTerm)
    }

    /** Đồng bộ trạng thái chọn của các chip nghĩa khi người dùng gõ/xóa tay trong ô Nghĩa. */
    fun onMeaningInputChanged(value: String) {
        val parts = splitMeaning(value)
        _suggestState.update { s ->
            val stillSelected = s.selectedMeanings.filter { sel ->
                parts.any { it.equals(sel, ignoreCase = true) }
            }
            if (stillSelected == s.selectedMeanings) s else s.copy(selectedMeanings = stillSelected)
        }
    }

    fun dismissSuggestions() {
        _suggestState.update { it.copy(suggestions = emptyList()) }
    }

    /** Người dùng chạm chọn một từ trong dropdown autocomplete. */
    fun onSuggestionSelected(
        selectedWord: String,
        currentPhonetic: String,
        currentMeaning: String,
    ) {
        val cleanWord = selectedWord.trim().take(Constants.MAX_WORD_TERM_LENGTH)
        if (cleanWord.isEmpty()) return
        lockedTerm = cleanWord
        _suggestState.update { it.copy(suggestions = emptyList()) }
        autocompleteQuery.value = cleanWord
        lookupDetailsInternal(
            term = cleanWord,
            currentPhonetic = currentPhonetic,
            currentMeaning = currentMeaning,
        )
    }

    /** Người dùng bấm nút "Tra & dịch" hoặc phím Done trên bàn phím sau khi nhập từ. */
    fun lookupAndTranslate(
        rawTerm: String,
        currentPhonetic: String,
        currentMeaning: String,
    ) {
        val cleanWord = rawTerm.trim().take(Constants.MAX_WORD_TERM_LENGTH)
        if (cleanWord.isEmpty() || dictionaryRepository === NoOpDictionaryRepository) return
        lockedTerm = cleanWord
        _suggestState.update { it.copy(suggestions = emptyList()) }
        autocompleteQuery.value = cleanWord
        lookupDetailsInternal(
            term = cleanWord,
            currentPhonetic = currentPhonetic,
            currentMeaning = currentMeaning,
        )
    }

    /**
     * Bật/tắt một chip nghĩa tiếng Việt gợi ý, đồng thời giữ nguyên phần nghĩa người dùng tự gõ ngoài chip.
     * @return chuỗi nội dung mới cho ô Nghĩa tiếng Việt.
     */
    fun toggleMeaningChip(option: String, currentMeaning: String): String {
        val currentState = _suggestState.value
        val alreadySelected = currentState.selectedMeanings.any { it.equals(option, ignoreCase = true) }
        val updatedSelected = if (alreadySelected) {
            currentState.selectedMeanings.filterNot { it.equals(option, ignoreCase = true) }
        } else {
            currentState.selectedMeanings + option
        }

        val manualParts = splitMeaning(currentMeaning).filter { part ->
            currentState.meaningOptions.none { opt -> opt.equals(part, ignoreCase = true) }
        }
        val combined = (manualParts + updatedSelected)
            .joinToString(MEANING_SEPARATOR)
            .take(Constants.MAX_WORD_MEANING_LENGTH)

        _suggestState.update { it.copy(selectedMeanings = updatedSelected) }
        return combined
    }

    private fun lookupDetailsInternal(
        term: String,
        currentPhonetic: String,
        currentMeaning: String,
    ) {
        val key = normalize(term)
        detailsJob?.cancel()
        statusFromDetails = true
        detailsJob = viewModelScope.launch {
            _suggestState.update { it.copy(suggestions = emptyList(), status = WordSuggestStatus.Loading) }
            try {
                val details = dictionaryRepository.lookupDetails(term)
                val options = details.meanings.take(MAX_MEANING_OPTIONS)
                val examples = details.examples.take(MAX_EXAMPLE_OPTIONS)
                val resolvedPhonetic = details.phonetic?.trim()?.takeIf { it.isNotEmpty() }

                val shouldFillPhonetic = resolvedPhonetic != null && (
                    currentPhonetic.isBlank() || !phoneticEditedByUser || phoneticOwnerTerm != key
                )
                if (shouldFillPhonetic) {
                    phoneticEditedByUser = false
                    phoneticOwnerTerm = key
                }

                // Tự điền nghĩa đầu tiên khi ô Nghĩa đang trống hoàn toàn; nếu đã có chữ thì giữ nguyên
                // và chỉ đánh dấu các chip khớp sẵn.
                val autoMeaning = if (currentMeaning.isBlank() && options.isNotEmpty()) {
                    options.first().take(Constants.MAX_WORD_MEANING_LENGTH)
                } else {
                    null
                }

                val existingParts = splitMeaning(autoMeaning ?: currentMeaning)
                val matchedChips = options.filter { opt ->
                    existingParts.any { it.equals(opt, ignoreCase = true) }
                }

                val hasAnything = resolvedPhonetic != null || options.isNotEmpty() || examples.isNotEmpty()
                detailsTerm = key
                _suggestState.update {
                    it.copy(
                        suggestions = emptyList(),
                        meaningOptions = options,
                        selectedMeanings = matchedChips,
                        exampleOptions = examples,
                        status = if (hasAnything) WordSuggestStatus.Idle else WordSuggestStatus.Empty,
                    )
                }

                _events.tryEmit(
                    WordFormEvent.ApplySuggestions(
                        term = term,
                        phoneticToFill = if (shouldFillPhonetic) resolvedPhonetic else null,
                        meaningToFill = autoMeaning,
                    ),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _suggestState.update {
                    it.copy(
                        suggestions = emptyList(),
                        status = WordSuggestStatus.Error(
                            t.message ?: "Không tải được gợi ý — bạn vẫn có thể nhập tay",
                        ),
                    )
                }
            }
        }
    }

    private fun autocompleteFlow(): Flow<AutocompleteOutcome> =
        autocompleteQuery
            .debounce(debounceMs)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < MIN_QUERY_LENGTH || q.equals(lockedTerm, ignoreCase = true)) {
                    flowOf<AutocompleteOutcome>(AutocompleteOutcome.Hidden)
                } else {
                    flow<AutocompleteOutcome> {
                        emit(AutocompleteOutcome.Loading)
                        val outcome = try {
                            AutocompleteOutcome.Data(
                                dictionaryRepository.suggest(q).take(MAX_SUGGESTIONS),
                            )
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                            AutocompleteOutcome.Failed(
                                t.message ?: "Không tải được gợi ý — bạn vẫn có thể nhập tay",
                            )
                        }
                        emit(outcome)
                    }
                }
            }

    private fun applyAutocomplete(outcome: AutocompleteOutcome) {
        if (outcome != AutocompleteOutcome.Hidden) {
            statusFromDetails = false
        }
        _suggestState.update { current ->
            when (outcome) {
                AutocompleteOutcome.Hidden -> current.copy(
                    suggestions = emptyList(),
                    status = if (statusFromDetails) current.status else WordSuggestStatus.Idle,
                )

                AutocompleteOutcome.Loading -> current.copy(
                    status = WordSuggestStatus.Loading,
                )

                is AutocompleteOutcome.Data -> current.copy(
                    suggestions = outcome.words,
                    status = if (outcome.words.isEmpty()) WordSuggestStatus.Empty else WordSuggestStatus.Idle,
                )

                is AutocompleteOutcome.Failed -> current.copy(
                    suggestions = emptyList(),
                    status = WordSuggestStatus.Error(outcome.message),
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Lưu trữ Room (F04, F05)
    // ---------------------------------------------------------------------

    /** F04: Thêm từ mới vào Tổ (status = egg). */
    fun createWord(
        deckId: String,
        term: String,
        meaning: String,
        phonetic: String? = null,
        example: String? = null,
        note: String? = null,
        onSuccess: (Word) -> Unit = {},
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                wordRepository.addWord(
                    deckId = deckId,
                    term = term,
                    meaning = meaning,
                    phonetic = phonetic,
                    example = example,
                    note = note,
                ).onSuccess { word ->
                    _events.tryEmit(WordFormEvent.WordCreated(word))
                    _events.tryEmit(WordFormEvent.ShowMessage("Đã thêm từ \"${word.term}\""))
                    onSuccess(word)
                }.onFailure { err ->
                    _events.tryEmit(
                        WordFormEvent.ShowMessage(err.message ?: "Không thêm được từ", isError = true),
                    )
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** F05: Sửa nội dung từ (không đụng status / lastReviewedAt). */
    fun updateWord(
        word: Word,
        newTerm: String,
        newMeaning: String,
        newPhonetic: String?,
        newExample: String?,
        newNote: String? = word.note,
        onSuccess: (Word) -> Unit = {},
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                wordRepository.updateWord(
                    word = word,
                    newTerm = newTerm,
                    newMeaning = newMeaning,
                    newPhonetic = newPhonetic,
                    newExample = newExample,
                    newNote = newNote,
                ).onSuccess { updated ->
                    _events.tryEmit(WordFormEvent.WordUpdated(updated))
                    _events.tryEmit(WordFormEvent.ShowMessage("Đã cập nhật từ \"${updated.term}\""))
                    onSuccess(updated)
                }.onFailure { err ->
                    _events.tryEmit(
                        WordFormEvent.ShowMessage(err.message ?: "Không sửa được từ", isError = true),
                    )
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** F05: Xoá mềm từ ngay trong màn sửa từ (tuân thủ hợp đồng Firestore FB3). */
    fun deleteWord(
        wordId: String,
        onSuccess: () -> Unit = {},
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                wordRepository.deleteWord(wordId)
                    .onSuccess {
                        _events.tryEmit(WordFormEvent.WordDeleted)
                        _events.tryEmit(WordFormEvent.ShowMessage("Đã xóa từ"))
                        onSuccess()
                    }
                    .onFailure { err ->
                        _events.tryEmit(
                            WordFormEvent.ShowMessage(err.message ?: "Không xóa được từ", isError = true),
                        )
                    }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun splitMeaning(text: String): List<String> =
        text.split(";").map { it.trim() }.filter { it.isNotEmpty() }

    private fun normalize(raw: String): String = raw.trim().lowercase()

    private sealed interface AutocompleteOutcome {
        data object Hidden : AutocompleteOutcome
        data object Loading : AutocompleteOutcome
        data class Data(val words: List<String>) : AutocompleteOutcome
        data class Failed(val message: String) : AutocompleteOutcome
    }

    companion object {
        const val DEFAULT_SUGGEST_DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
        const val MAX_SUGGESTIONS = 8
        const val MAX_MEANING_OPTIONS = 5
        const val MAX_EXAMPLE_OPTIONS = 3
        const val MEANING_SEPARATOR = "; "
    }
}
