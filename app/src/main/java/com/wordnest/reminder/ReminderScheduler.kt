package com.wordnest.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wordnest.util.delayUntilNextReminder
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    fun scheduleDaily(time: LocalTime, now: LocalDateTime = LocalDateTime.now()) {
        val initialDelay = delayUntilNextReminder(now, time)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "daily_reminder"

        fun schedule(context: Context, reminderTime: LocalTime) {
            ReminderScheduler(context).scheduleDaily(reminderTime)
        }

        fun cancel(context: Context) {
            ReminderScheduler(context).cancel()
        }
    }
}
