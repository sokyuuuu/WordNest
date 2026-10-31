package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.repository.WordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Trạng thái tải gợi ý (dùng chung cho autocomplete và chi tiết từ). */
sealed interface SuggestStatus {
    data object Idle : SuggestStatus
    data object Loading : SuggestStatus
    /** Đã tra nhưng không có gợi ý nào. */
    data object Empty : SuggestStatus
    data class Error(val message: String) : SuggestStatus
}

data class AddWordState(
    val term: String = "",
    val phonetic: String = "",
    val meaning: String = "",
    val example: String = "",
    /** Dropdown autocomplete (tối đa 8). */
    val suggestions: List<String> = emptyList(),
    /** Chip "Nghĩa gợi ý" (tối đa 5). */
    val meaningOptions: List<String> = emptyList(),
    /** Các chip nghĩa đang chọn, giữ đúng thứ tự người dùng chạm. */
    val selectedMeanings: List<String> = emptyList(),
    /** Câu ví dụ gợi ý (tối đa 2). */
    val exampleOptions: List<String> = emptyList(),
    val uiState: SuggestStatus = SuggestStatus.Idle,
    val autoSuggestEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false
) {
    /** Lưu được khi có từ và nghĩa — không phụ thuộc gợi ý thành công hay lỗi. */
    val canSave: Boolean
        get() = term.isNotBlank() && meaning.isNotBlank() && !isSaving
}

/**
 * ViewModel cho màn "Thêm từ" kiểu Quizlet.
 *
 * Chỉ phụ thuộc [DictionaryRepository] (interface) để sau này thay Fake bằng API thật.
 * @param autoSuggestEnabled giá trị Setting.auto_suggest_enabled; false -> form chỉ nhập tay.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AddWordViewModel(
    private val deckId: String,
    private val dictionary: DictionaryRepository,
    private val wordRepository: WordRepository,
    autoSuggestEnabled: Boolean = true,
    private val debounceMs: Long = DEBOUNCE_MS
) : ViewModel() {

    private val _state = MutableStateFlow(AddWordState(autoSuggestEnabled = autoSuggestEnabled))
    val state: StateFlow<AddWordState> = _state.asStateFlow()

    /** Chỉ phát khi người dùng GÕ vào ô từ — nguồn cho autocomplete. */
    private val query = MutableStateFlow("")

    /**
     * Từ vừa được chọn từ dropdown. Khi query trùng từ này thì không mở lại dropdown
     * (tránh việc set text sau khi chọn lại kích hoạt gợi ý).
     */
    @Volatile
    private var lockedTerm: String? = null

    /** Từ mà meaningOptions/exampleOptions hiện tại thuộc về. */
    private var detailsTerm: String? = null

    // --- Theo dõi phiên âm để không ghi đè nội dung người dùng đã tự sửa ---
    /** true nếu nội dung ô phiên âm hiện tại do người dùng tự gõ/sửa. */
    private var phoneticEditedByUser = false
    /** Từ (đã chuẩn hóa) đang "sở hữu" nội dung ô phiên âm hiện tại. */
    private var phoneticOwnerTerm: String? = null

    private var detailsJob: Job? = null

    /** true nếu uiState hiện tại do phần tải chi tiết (phiên âm/nghĩa/ví dụ) đặt, không phải autocomplete. */
    private var statusFromDetails = false

    init {
        if (autoSuggestEnabled) {
            viewModelScope.launch {
                autocompleteFlow().collect { status -> applyAutocomplete(status) }
            }
        }
    }

    /**
     * Pipeline autocomplete:
     *  - debounce: chờ người dùng ngừng gõ [debounceMs] rồi mới tra, tránh gọi theo từng phím.
     *  - distinctUntilChanged: bỏ qua nếu chuỗi sau khi trim không đổi.
     *  - flatMapLatest: có chuỗi mới thì HỦY request cũ đang chạy, chỉ giữ kết quả mới nhất.
     */
    private fun autocompleteFlow(): Flow<AutocompleteResult> =
        query
            .debounce(debounceMs)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < MIN_QUERY_LENGTH || q.equals(lockedTerm, ignoreCase = true)) {
                    flowOf<AutocompleteResult>(AutocompleteResult.Hidden)
                } else {
                    flow<AutocompleteResult> {
                        emit(AutocompleteResult.Loading)
                        val result = try {
                            AutocompleteResult.Data(dictionary.suggest(q).take(MAX_SUGGESTIONS))
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                            AutocompleteResult.Failed(t.message ?: "Lỗi tải gợi ý")
                        }
                        emit(result)
                    }
                }
            }

    private fun applyAutocomplete(result: AutocompleteResult) {
        if (result != AutocompleteResult.Hidden) statusFromDetails = false
        _state.update {
            when (result) {
                AutocompleteResult.Hidden -> it.copy(
                    suggestions = emptyList(),
                    // uiState đang phản ánh phần tải chi tiết (loading/empty/error) thì giữ nguyên.
                    uiState = if (statusFromDetails) it.uiState else SuggestStatus.Idle
                )
                AutocompleteResult.Loading -> it.copy(uiState = SuggestStatus.Loading)
                is AutocompleteResult.Data -> it.copy(
                    suggestions = result.words,
                    uiState = if (result.words.isEmpty()) SuggestStatus.Empty else SuggestStatus.Idle
                )
                is AutocompleteResult.Failed -> it.copy(
                    suggestions = emptyList(),
                    uiState = SuggestStatus.Error(result.message)
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Sự kiện từ UI
    // ---------------------------------------------------------------------

    fun onTermChange(value: String) {
        // Người dùng gõ khác từ đã chọn -> mở lại autocomplete.
        if (!value.trim().equals(lockedTerm, ignoreCase = true)) lockedTerm = null

        val key = normalize(value)
        _state.update { s ->
            if (detailsTerm != null && key != detailsTerm) {
                // Đổi sang từ khác: gợi ý nghĩa/ví dụ cũ không còn đúng. Giữ nguyên nội dung ô Nghĩa.
                detailsTerm = null
                detailsJob?.cancel()
                statusFromDetails = false
                s.copy(
                    term = value,
                    meaningOptions = emptyList(),
                    selectedMeanings = emptyList(),
                    exampleOptions = emptyList(),
                    uiState = SuggestStatus.Idle,
                    saveError = null
                )
            } else {
                s.copy(term = value, saveError = null)
            }
        }
        if (_state.value.autoSuggestEnabled) query.value = value
    }

    /** Người dùng chọn một từ trong dropdown. */
    fun onSuggestionSelected(word: String) {
        lockedTerm = word
        _state.update { it.copy(term = word, suggestions = emptyList()) }
        query.value = word // flatMapLatest sẽ hủy request autocomplete đang chạy
        loadDetails(word)
    }

    /** Người dùng nhấn "Xong" trên bàn phím sau khi gõ tay. */
    fun onTermDone() {
        val term = _state.value.term.trim()
        if (term.isEmpty()) return
        lockedTerm = term
        _state.update { it.copy(suggestions = emptyList()) }
        loadDetails(term)
    }

    fun dismissSuggestions() {
        _state.update { it.copy(suggestions = emptyList()) }
    }

    fun onPhoneticChange(value: String) {
        phoneticEditedByUser = value.isNotBlank()
        phoneticOwnerTerm = normalize(_state.value.term)
        _state.update { it.copy(phonetic = value) }
    }

    fun onMeaningChange(value: String) {
        _state.update { s ->
            // Nếu người dùng xóa tay một nghĩa gợi ý khỏi ô, bỏ chọn chip tương ứng.
            val parts = splitMeaning(value)
            s.copy(meaning = value, selectedMeanings = s.selectedMeanings.filter { it in parts })
        }
    }

    fun onExampleChange(value: String) {
        _state.update { it.copy(example = value) }
    }

    /** Chạm 1 câu ví dụ gợi ý để điền vào ô Ví dụ. */
    fun onExampleSelected(example: String) {
        _state.update { it.copy(example = example) }
    }

    /**
     * Chọn/bỏ chọn chip nghĩa. Ô Nghĩa = [phần người dùng tự gõ] + [các nghĩa đã chọn], nối bằng "; ".
     * Phần tự gõ (không thuộc danh sách gợi ý) được giữ nguyên, không bị mất khi chạm chip.
     */
    fun onMeaningToggled(option: String) {
        _state.update { s ->
            val selected = if (option in s.selectedMeanings) s.selectedMeanings - option
            else s.selectedMeanings + option
            val manualParts = splitMeaning(s.meaning).filter { it !in s.meaningOptions }
            s.copy(
                selectedMeanings = selected,
                meaning = (manualParts + selected).joinToString(MEANING_SEPARATOR)
            )
        }
    }

    fun onSave(onSuccess: () -> Unit = {}) {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            wordRepository.addWord(
                deckId = deckId,
                term = s.term,
                meaning = s.meaning,
                phonetic = s.phonetic.ifBlank { null },
                example = s.example.ifBlank { null }
            ).onSuccess {
                _state.update { it.copy(isSaving = false, saved = true) }
                onSuccess()
            }.onFailure { err ->
                _state.update { it.copy(isSaving = false, saveError = err.message ?: "Không lưu được từ") }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Tải chi tiết từ: phiên âm, nghĩa, ví dụ (song song)
    // ---------------------------------------------------------------------

    private fun loadDetails(term: String) {
        if (!_state.value.autoSuggestEnabled) return
        val key = normalize(term)
        detailsJob?.cancel()
        statusFromDetails = true
        detailsJob = viewModelScope.launch {
            _state.update { it.copy(uiState = SuggestStatus.Loading) }
            try {
                // 3 lời gọi chạy song song; lỗi ở bất kỳ lời gọi nào sẽ hủy các lời gọi còn lại.
                val (phonetic, meanings, examples) = coroutineScope {
                    val p = async { dictionary.getPhonetic(term) }
                    val m = async { dictionary.getMeanings(term) }
                    val e = async { dictionary.getExamples(term) }
                    Triple(p.await(), m.await(), e.await())
                }
                applyDetails(key, phonetic, meanings, examples)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _state.update { it.copy(uiState = SuggestStatus.Error(t.message ?: "Lỗi tải gợi ý")) }
            }
        }
    }

    private fun applyDetails(key: String, phonetic: String?, meanings: List<String>, examples: List<String>) {
        val sameTermAsBefore = detailsTerm == key
        detailsTerm = key
        _state.update { s ->
            // Quy tắc KHÔNG ghi đè phiên âm người dùng đã sửa:
            //  - ô trống -> điền;
            //  - nội dung hiện tại là do hệ thống điền -> cập nhật theo từ mới;
            //  - người dùng tự sửa NHƯNG đã đổi sang từ khác -> điền theo từ mới;
            //  - người dùng tự sửa cho CÙNG từ này -> giữ nguyên.
            val shouldFillPhonetic = !phonetic.isNullOrBlank() && (
                s.phonetic.isBlank() || !phoneticEditedByUser || phoneticOwnerTerm != key
            )
            if (shouldFillPhonetic) {
                phoneticEditedByUser = false
                phoneticOwnerTerm = key
            }
            val options = meanings.take(MAX_MEANING_OPTIONS)
            val hasAnything = !phonetic.isNullOrBlank() || options.isNotEmpty() || examples.isNotEmpty()
            s.copy(
                phonetic = if (shouldFillPhonetic) phonetic!! else s.phonetic,
                meaningOptions = options,
                // Tra lại cùng từ thì giữ chip đã chọn; từ khác thì bắt đầu lại.
                selectedMeanings = if (sameTermAsBefore) s.selectedMeanings.filter { it in options } else emptyList(),
                exampleOptions = examples.take(MAX_EXAMPLE_OPTIONS),
                suggestions = emptyList(),
                uiState = if (hasAnything) SuggestStatus.Idle else SuggestStatus.Empty
            )
        }
    }

    private fun splitMeaning(text: String): List<String> =
        text.split(";").map { it.trim() }.filter { it.isNotEmpty() }

    private fun normalize(raw: String) = raw.trim().lowercase()

    private sealed interface AutocompleteResult {
        data object Hidden : AutocompleteResult
        data object Loading : AutocompleteResult
        data class Data(val words: List<String>) : AutocompleteResult
        data class Failed(val message: String) : AutocompleteResult
    }

    companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
        const val MAX_SUGGESTIONS = 8
        const val MAX_MEANING_OPTIONS = 5
        const val MAX_EXAMPLE_OPTIONS = 2
        const val MEANING_SEPARATOR = "; "
    }
}
