package com.wordnest.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Thời điểm nhắc học kế tiếp sau [now].
 *
 * Nếu đúng giờ nhắc (không còn sau đó) thì lần nhắc là ngày mai.
 */
fun nextReminderAt(now: LocalDateTime, reminderTime: LocalTime): LocalDateTime {
    val todayAtReminder = now.toLocalDate().atTime(reminderTime)
    return if (now.isBefore(todayAtReminder)) todayAtReminder else todayAtReminder.plusDays(1)
}

fun delayUntilNextReminder(now: LocalDateTime, reminderTime: LocalTime): Duration =
    Duration.between(now, nextReminderAt(now, reminderTime))
