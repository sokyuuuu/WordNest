package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.StudyHistoryRepository
import com.wordnest.util.calculateEggDay
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
}
