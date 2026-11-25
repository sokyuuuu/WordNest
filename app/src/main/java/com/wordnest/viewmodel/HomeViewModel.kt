package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.study.StudyWord
import com.wordnest.data.study.StudyWordSource
import com.wordnest.di.appContainer
import com.wordnest.util.Constants
import com.wordnest.util.WordStatus
import com.wordnest.util.calculateEggDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel internal constructor(
    application: Application,
    studyDays: Flow<Set<LocalDate>>,
    source: StudyWordSource,
    deckRepository: DeckRepository,
    userId: String = Constants.DEFAULT_USER_ID,
    todayProvider: () -> LocalDate = LocalDate::now,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        studyDays = application.appContainer().studyDayRepository.studyDays,
        source = application.appContainer().roomStudyWordSource,
        deckRepository = application.appContainer().deckRepository,
    )

    val eggDay: StateFlow<Int> = studyDays
        .map { calculateEggDay(it, todayProvider()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val wordsState: Flow<List<StudyWord>> = source.words()

    /** Số từ còn cần học/ôn (mọi trạng thái trừ "Chim bay"). */
    val dueCount: StateFlow<Int> = wordsState
        .map { words -> words.count { it.status != WordStatus.BIRD } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Đếm từng trạng thái của đàn từ (Trứng / Chim non / Chim bay). */
    val flockCounts: StateFlow<Map<WordStatus, Int>> = wordsState
        .map { words -> words.groupingBy { it.status }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val decksFlow: Flow<List<DeckWithStats>> =
        deckRepository.observeDecksWithStats(userId)

    /** Tối đa 2 Tổ mới nhất (theo create_at DESC) để hiển thị trên Trang chủ (NOI1). */
    val topDecks: StateFlow<List<DeckWithStats>> = decksFlow
        .map { decks -> decks.take(2) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Người dùng có ít nhất 1 Tổ chưa xoá mềm hay không. */
    val hasDecks: StateFlow<Boolean> = decksFlow
        .map { decks -> decks.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
