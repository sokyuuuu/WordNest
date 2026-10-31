package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTimeTest {

    private val reminder = LocalTime.of(20, 0)

    @Test
    fun beforeReminder_sameDay() {
        val now = LocalDateTime.of(2026, 10, 25, 8, 30)
        assertEquals(LocalDateTime.of(2026, 10, 25, 20, 0), nextReminderAt(now, reminder))
    }

    @Test
    fun exactlyAtReminder_nextDay() {
        val now = LocalDateTime.of(2026, 10, 25, 20, 0)
        assertEquals(LocalDateTime.of(2026, 10, 26, 20, 0), nextReminderAt(now, reminder))
    }

    @Test
    fun afterReminder_nextDay() {
        val now = LocalDateTime.of(2026, 10, 25, 21, 15)
        assertEquals(LocalDateTime.of(2026, 10, 26, 20, 0), nextReminderAt(now, reminder))
    }

    @Test
    fun justBeforeMidnight_reminderNextMorning() {
        val now = LocalDateTime.of(2026, 10, 25, 23, 59)
        val time = LocalTime.of(7, 30)
        assertEquals(LocalDateTime.of(2026, 10, 26, 7, 30), nextReminderAt(now, time))
    }

    @Test
    fun midnightReminder_fromJustAfterMidnight() {
        val now = LocalDateTime.of(2026, 10, 25, 0, 1)
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 0).plusDays(1), nextReminderAt(now, LocalTime.MIDNIGHT))
    }

    @Test
    fun newYearRollover() {
        val now = LocalDateTime.of(2026, 12, 31, 22, 0)
        assertEquals(LocalDateTime.of(2027, 1, 1, 20, 0), nextReminderAt(now, reminder))
    }

    @Test
    fun delayIsDifferenceToNextReminder() {
        val now = LocalDateTime.of(2026, 10, 25, 18, 0)
        assertEquals(Duration.ofHours(2), delayUntilNextReminder(now, reminder))
    }

    @Test
    fun delayAfterReminder_isAlmostTwentyFourHours() {
        val now = LocalDateTime.of(2026, 10, 25, 20, 30)
        assertEquals(Duration.ofHours(23).plusMinutes(30), delayUntilNextReminder(now, reminder))
    }
}
