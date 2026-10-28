package com.wordnest.util

import java.time.LocalDate

/**
 * Tính số ngày ấp trứng: số ngày học liên tiếp tính đến hiện tại.
 *
 * Chuỗi vẫn còn hiệu lực nếu hôm nay hoặc hôm qua có học.
 * Bỏ qua mọi ngày sau [today].
 */
fun calculateEggDay(studyDays: Set<LocalDate>, today: LocalDate): Int {
    val lastStudyDay = studyDays
        .filter { !it.isAfter(today) }
        .maxOrNull()
        ?: return 0

    if (lastStudyDay.isBefore(today.minusDays(1))) return 0

    var count = 0
    var day = lastStudyDay
    while (day in studyDays) {
        count++
        day = day.minusDays(1)
    }
    return count
}
