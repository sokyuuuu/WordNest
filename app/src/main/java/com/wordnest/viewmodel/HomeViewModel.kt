package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.StudyHistoryRepository
import com.wordnest.data.study.InMemoryStudyWordSource
import com.wordnest.data.study.StudyWordSource
import com.wordnest.util.WordStatus
import com.wordnest.util.calculateEggDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val studyHistory = StudyHistoryRepository(application)

    val eggDay: StateFlow<Int> = studyHistory.studyDays
        .map { calculateEggDay(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Dữ liệu mẫu của đàn từ (nguồn trung gian trước khi Room ghép vào,
     * F01–F07). Dùng cho thẻ "Hôm nay cần ôn" và "Đàn chim" ở Trang chủ.
     */
    private val source: StudyWordSource = InMemoryStudyWordSource()
    private val wordsState = MutableStateFlow(source.words())

    /** Số từ còn cần học/ôn (mọi trạng thái trừ "Chim bay"). */
    val dueCount: StateFlow<Int> = wordsState
        .map { words -> words.count { it.status != WordStatus.BIRD } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Đếm từng trạng thái của đàn từ (Trứng / Chim non / Chim bay). */
    val flockCounts: StateFlow<Map<WordStatus, Int>> = wordsState
        .map { words -> words.groupingBy { it.status }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
