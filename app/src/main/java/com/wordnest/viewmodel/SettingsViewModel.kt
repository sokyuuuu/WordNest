package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.AppSettings
import com.wordnest.data.SettingsRepository
import com.wordnest.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            val current = settings.value.copy(reminderEnabled = enabled)
            applyReminder(current)
        }
    }

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            repository.setReminderTime(time)
            applyReminder(settings.value.copy(reminderTime = time))
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    private fun applyReminder(current: AppSettings) {
        val context = getApplication<Application>()
        if (current.reminderEnabled) {
            ReminderScheduler.schedule(context, current.reminderTime)
        } else {
            ReminderScheduler.cancel(context)
        }
    }
}
