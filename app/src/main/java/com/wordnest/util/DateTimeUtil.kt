package com.wordnest.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun nowIso(): String = Instant.now().toString()

    fun todayDate(): String = LocalDate.now().format(DATE_FORMATTER)

    fun plusDays(dateStr: String, days: Int): String =
        LocalDate.parse(dateStr, DATE_FORMATTER)
            .plusDays(days.toLong())
            .format(DATE_FORMATTER)

    /**
     * Chuyển [LocalDate] sang chuỗi ISO-8601 UTC (`...Z`) theo múi giờ [zoneId]
     * để lưu Room/Firestore (`Instant.parse` luôn hợp lệ và đọc ngược lại đúng ngày).
     */
    fun localDateToIsoUtc(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String =
        date.atStartOfDay(zoneId).toInstant().toString()

    /**
     * Đọc chuỗi thời gian lưu trong Room (ISO-8601 UTC, ISO local datetime hoặc yyyy-MM-dd)
     * thành [LocalDate] theo múi giờ [zoneId].
     */
    fun parseIsoToLocalDate(raw: String?, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw).atZone(zoneId).toLocalDate() }
            .recoverCatching { LocalDate.parse(raw, DATE_FORMATTER) }
            .recoverCatching { LocalDateTime.parse(raw).toLocalDate() }
            .getOrNull()
    }
}
