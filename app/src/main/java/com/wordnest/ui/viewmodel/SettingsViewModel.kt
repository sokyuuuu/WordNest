package com.wordnest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.repository.AppSettings
import com.wordnest.data.repository.SettingsRepository
import com.wordnest.data.repository.StudyHistoryRepository
import com.wordnest.reminder.ReminderScheduler
import com.wordnest.util.calculateEggDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HomeViewModel(
    private val studyHistoryRepository: StudyHistoryRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val today: LocalDate get() = todayProvider()

    val studyDays: StateFlow<Set<LocalDate>> = studyHistoryRepository.studyDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val eggDay: StateFlow<Int> = studyDays
        .map { days -> calculateEggDay(days, todayProvider()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    fun recordStudyToday() {
        viewModelScope.launch {
            studyHistoryRepository.recordStudyDay(todayProvider())
        }
    }
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    val autoSuggestEnabled: StateFlow<Boolean>,
    val deeplApiKey: StateFlow<String>,
    private val onSetAutoSuggest: (Boolean) -> Unit,
    private val onSetDeeplApiKey: (String) -> Unit
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            if (enabled) {
                val time = repository.settings.first().reminderTime
                scheduler.scheduleDaily(time)
            } else {
                scheduler.cancel()
            }
        }
    }

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            repository.setReminderTime(time)
            if (repository.settings.first().reminderEnabled) {
                scheduler.scheduleDaily(time)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun setAutoSuggest(enabled: Boolean) {
        onSetAutoSuggest(enabled)
    }

    fun saveDeeplApiKey(rawKey: String) {
        onSetDeeplApiKey(rawKey.trim())
    }
}
