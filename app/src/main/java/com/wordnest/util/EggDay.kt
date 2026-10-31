package com.wordnest.util

import java.time.LocalDate

fun calculateEggDay(studyDays: Set<LocalDate>, today: LocalDate): Int {
    val start = when {
        studyDays.contains(today) -> today
        studyDays.contains(today.minusDays(1)) -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    var cursor = start
    while (studyDays.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun calculateLongestStreak(studyDays: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    val validDays = studyDays.filter { !it.isAfter(today) }
    if (validDays.isEmpty()) return 0
    val sortedDays = validDays.sorted()
    var maxStreak = 0
    var currentStreak = 0
    var prevDate: LocalDate? = null

    for (date in sortedDays) {
        if (prevDate != null && date == prevDate.plusDays(1)) {
            currentStreak++
        } else {
            currentStreak = 1
        }
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak
        }
        prevDate = date
    }
    return maxStreak
}
