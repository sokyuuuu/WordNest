package com.wordnest.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Quy tắc chuyển trạng thái của từ (F11), theo quyết định đã chốt:
 * - Lên hạng dựa trên thời gian học đều đặn (chuỗi ngày tương tự Duolingo).
 * - Đánh dấu "chưa thuộc" không làm giảm trạng thái.
 * - Không học trong khoảng [IDLE_DEMOTE_DAYS] ngày thì trạng thái giảm một bậc.
 *
 * Ngưỡng số ngày là giá trị mặc định, còn chờ nhóm xác nhận (TC11-02, TC11-03, TC11-05).
 */
object StudyProgression {

    const val CHICK_PROMOTION_STREAK = 3
    const val BIRD_PROMOTION_STREAK = 7
    const val IDLE_DEMOTE_DAYS = 7L

    /**
     * Trạng thái mới của từ sau khi được đánh dấu "đã thuộc".
     * Chỉ nâng hạng, không bao giờ hạ hạng.
     */
    fun promote(current: WordStatus, streakDays: Int): WordStatus {
        val target = when {
            streakDays >= BIRD_PROMOTION_STREAK -> WordStatus.BIRD
            streakDays >= CHICK_PROMOTION_STREAK -> WordStatus.CHICK
            else -> current
        }
        return if (target.ordinal > current.ordinal) target else current
    }

    /**
     * Giảm một bậc nếu từ không được học trong [IDLE_DEMOTE_DAYS] ngày.
     * Trứng là mức thấp nhất nên không giảm thêm.
     */
    fun demoteIfIdle(current: WordStatus, lastStudied: LocalDate?, today: LocalDate): WordStatus {
        lastStudied ?: return current
        val idleDays = ChronoUnit.DAYS.between(lastStudied, today)
        return if (idleDays >= IDLE_DEMOTE_DAYS) current.demote() else current
    }

    private fun WordStatus.demote(): WordStatus = when (this) {
        WordStatus.BIRD -> WordStatus.CHICK
        WordStatus.CHICK -> WordStatus.EGG
        WordStatus.EGG -> WordStatus.EGG
    }
}
