package com.wordnest.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

fun nextReminderAt(now: LocalDateTime, reminderTime: LocalTime): LocalDateTime {
    val candidate = now.toLocalDate().atTime(reminderTime)
    return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
}

fun delayUntilNextReminder(now: LocalDateTime, reminderTime: LocalTime): Duration {
    return Duration.between(now, nextReminderAt(now, reminderTime))
}
