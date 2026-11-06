package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Kiểm tra quy tắc chuyển trạng thái từ F11 theo quyết định đã chốt:
 * lên hạng theo chuỗi ngày học, "chưa thuộc" không hạ hạng, không học lâu thì tự giảm một bậc.
 * Ngưỡng ngày còn chờ nhóm xác nhận nên test tham chiếu tới hằng số của [StudyProgression].
 */
class StudyProgressionTest {

    private val today = LocalDate.of(2026, 11, 7)

    // --- promote (TC11-01, TC11-02, TC11-03, TC11-04) ---

    @Test
    fun promote_eggBelowChickStreak_staysEgg() {
        val result = StudyProgression.promote(
            WordStatus.EGG,
            StudyProgression.CHICK_PROMOTION_STREAK - 1,
        )
        assertEquals(WordStatus.EGG, result)
    }

    @Test
    fun promote_eggAtChickStreak_becomesChick() {
        val result = StudyProgression.promote(WordStatus.EGG, StudyProgression.CHICK_PROMOTION_STREAK)
        assertEquals(WordStatus.CHICK, result)
    }

    @Test
    fun promote_chickAtBirdStreak_becomesBird() {
        val result = StudyProgression.promote(WordStatus.CHICK, StudyProgression.BIRD_PROMOTION_STREAK)
        assertEquals(WordStatus.BIRD, result)
    }

    @Test
    fun promote_chickBelowBirdStreak_staysChick() {
        val result = StudyProgression.promote(
            WordStatus.CHICK,
            StudyProgression.BIRD_PROMOTION_STREAK - 1,
        )
        val level = StudyProgression.CHICK_PROMOTION_STREAK
        val expected = if (level >= 0) WordStatus.CHICK else WordStatus.EGG
        assertEquals(expected, result)
    }

    @Test
    fun promote_birdStreak_neverGoesBackwards() {
        val result = StudyProgression.promote(WordStatus.BIRD, StudyProgression.CHICK_PROMOTION_STREAK)
        assertEquals(WordStatus.BIRD, result)
    }

    @Test
    fun promote_orderPreserved_neverDemotes() {
        WordStatus.entries.forEach { status ->
            WordStatus.entries.forEach { other ->
                val promoted = StudyProgression.promote(status, other.ordinal + 1)
                assertTrue(promoted.ordinal >= status.ordinal)
            }
        }
    }

    // --- demoteIfIdle (TC11-05) ---

    @Test
    fun demoteIfIdle_belowIdleDays_stays() {
        val result = StudyProgression.demoteIfIdle(
            WordStatus.CHICK,
            today.minusDays(StudyProgression.IDLE_DEMOTE_DAYS - 1),
            today,
        )
        assertEquals(WordStatus.CHICK, result)
    }

    @Test
    fun demoteIfIdle_atIdleDays_demotesOneLevel() {
        val result = StudyProgression.demoteIfIdle(
            WordStatus.BIRD,
            today.minusDays(StudyProgression.IDLE_DEMOTE_DAYS),
            today,
        )
        assertEquals(WordStatus.CHICK, result)
    }

    @Test
    fun demoteIfIdle_chick_becomesEgg() {
        val result = StudyProgression.demoteIfIdle(
            WordStatus.CHICK,
            today.minusDays(StudyProgression.IDLE_DEMOTE_DAYS) ,
            today,
        )
        assertEquals(WordStatus.EGG, result)
    }

    @Test
    fun demoteIfIdle_egg_staysEgg() {
        val result = StudyProgression.demoteIfIdle(
            WordStatus.EGG,
            today.minusDays(StudyProgression.IDLE_DEMOTE_DAYS),
            today,
        )
        assertEquals(WordStatus.EGG, result)
    }

    @Test
    fun demoteIfIdle_neverStudied_stays() {
        val result = StudyProgression.demoteIfIdle(WordStatus.CHICK, null, today)
        assertEquals(WordStatus.CHICK, result)
    }
}
