package com.wordnest

import android.app.Application
import com.wordnest.data.seed.SampleDataSeeder
import com.wordnest.di.AppContainer
import com.wordnest.reminder.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class WordNestApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderWorker.ensureChannel(this)

        appScope.launch {
            SampleDataSeeder(container.database).seedIfEmpty()

            val currentDays = container.studyHistoryRepository.studyDays.first()
            if (currentDays.isEmpty()) {
                val today = LocalDate.now()
                for (offset in 0L..4L) {
                    container.studyHistoryRepository.recordStudyDay(today.minusDays(offset))
                }
            }

            val settings = container.settingsRepository.settings.first()
            if (settings.reminderEnabled) {
                container.reminderScheduler.scheduleDaily(settings.reminderTime)
            }
        }
    }
}
