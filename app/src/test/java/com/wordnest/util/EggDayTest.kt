package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EggDayTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun noStudyDays_returnsZero() {
        assertEquals(0, calculateEggDay(emptySet(), today))
    }

    @Test
    fun studiedOnlyToday_returnsOne() {
        assertEquals(1, calculateEggDay(setOf(today), today))
    }

    @Test
    fun studiedYesterdayOnly_streakStillCounts() {
        assertEquals(1, calculateEggDay(setOf(today.minusDays(1)), today))
    }

    @Test
    fun studiedTwoDaysAgo_streakBroken() {
        assertEquals(0, calculateEggDay(setOf(today.minusDays(2)), today))
    }

    @Test
    fun consecutiveDaysIncludingToday_countsAll() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, calculateEggDay(days, today))
    }

    @Test
    fun consecutiveDaysEndingYesterday_countsAll() {
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(3, calculateEggDay(days, today))
    }

    @Test
    fun gapBeforeStreak_onlyCountsLatestRun() {
        val days = setOf(
            today,
            today.minusDays(1),
            today.minusDays(3),
            today.minusDays(4),
            today.minusDays(5),
        )
        assertEquals(2, calculateEggDay(days, today))
    }

    @Test
    fun futureStudyDaysAreIgnored() {
        val days = setOf(today.plusDays(1), today.plusDays(2))
        assertEquals(0, calculateEggDay(days, today))
    }

    @Test
    fun futureDayDoesNotExtendStreak() {
        val days = setOf(today, today.plusDays(1))
        assertEquals(1, calculateEggDay(days, today))
    }

    @Test
    fun streakAcrossMonthBoundary() {
        val march1 = LocalDate.of(2026, 3, 1)
        val days = setOf(march1, march1.minusDays(1), march1.minusDays(2))
        assertEquals(3, calculateEggDay(days, march1))
    }

    @Test
    fun longestStreak_emptyReturnsZero() {
        assertEquals(0, calculateLongestStreak(emptySet(), today))
    }

    @Test
    fun longestStreak_multipleRuns_returnsMaximum() {
        val days = setOf(
            today.minusDays(10),
            today.minusDays(9),
            today.minusDays(8),
            today.minusDays(7), // run 1: 4 days
            today.minusDays(4),
            today.minusDays(3), // run 2: 2 days
            today               // run 3: 1 day
        )
        assertEquals(4, calculateLongestStreak(days, today))
    }

    @Test
    fun longestStreak_futureDaysIgnored() {
        val days = setOf(
            today,
            today.plusDays(1),
            today.plusDays(2),
            today.plusDays(3)
        )
        assertEquals(1, calculateLongestStreak(days, today))
    }
}
