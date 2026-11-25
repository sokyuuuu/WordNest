package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.WordWithDeck
import com.wordnest.data.repository.WordRepository
import com.wordnest.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<WordWithDeck> = emptyList(),
    val isSearching: Boolean = false,
) {
    val isQueryBlank: Boolean get() = query.trim().isEmpty()
    val hasSearchedAndEmpty: Boolean get() = !isQueryBlank && !isSearching && results.isEmpty()
}

private data class SearchResultSnapshot(
    val normalizedQuery: String = "",
    val results: List<WordWithDeck> = emptyList(),
)

/**
 * ViewModel cho màn hình Tìm kiếm toàn cục (F07 — T047–T049):
 * - Áp dụng [debounce] 200ms cho truy vấn không rỗng để tránh truy vấn Room liên tục khi đang gõ.
 * - Khi xoá trắng ô tìm kiếm, trả về trạng thái gợi ý ngay lập tức (0ms delay).
 * - Theo dõi `normalizedQuery` đã hoàn tất để không nhấp nháy "Không tìm thấy từ nào" trong lúc đang debounce.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val wordRepository: WordRepository,
    private val userId: String = Constants.DEFAULT_USER_ID,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MS,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val searchSnapshot: Flow<SearchResultSnapshot> = _query
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { trimmed -> if (trimmed.isEmpty()) 0L else debounceMillis }
        .flatMapLatest { trimmed ->
            if (trimmed.isEmpty()) {
                flowOf(SearchResultSnapshot())
            } else {
                wordRepository.observeSearchAllWordsWithDeck(userId, trimmed)
                    .map { items -> SearchResultSnapshot(normalizedQuery = trimmed, results = items) }
            }
        }

    val uiState: StateFlow<SearchUiState> = combine(_query, searchSnapshot) { rawQuery, snapshot ->
        val trimmed = rawQuery.trim()
        when {
            trimmed.isEmpty() -> SearchUiState(
                query = rawQuery,
                results = emptyList(),
                isSearching = false,
            )

            snapshot.normalizedQuery != trimmed -> SearchUiState(
                query = rawQuery,
                results = snapshot.results,
                isSearching = true,
            )

            else -> SearchUiState(
                query = rawQuery,
                results = snapshot.results,
                isSearching = false,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS: Long = 200L
    }
}
