
package com.example.flashcardscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningLogicTest {

    @Test
    fun correctAnswer_returnsTrue() {
        val result = LearningLogic.isCorrectAnswer(1, 1)
        assertTrue(result)
    }

    @Test
    fun wrongAnswer_returnsFalse() {
        val result = LearningLogic.isCorrectAnswer(2, 1)
        assertFalse(result)
    }

    @Test
    fun calculateScore_returnsThree() {
        val selected = listOf(0, 1, 2, 0)
        val correct = listOf(0, 1, 2, 1)

        val score = LearningLogic.calculateScore(
            selected,
            correct
        )

        assertEquals(3, score)
    }

    @Test
    fun calculateScore_allWrong_returnsZero() {
        val selected = listOf(1, 2, 3, 0)
        val correct = listOf(0, 1, 2, 1)

        assertEquals(
            0,
            LearningLogic.calculateScore(selected, correct)
        )
    }

    @Test
    fun calculateScore_differentSizes_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            LearningLogic.calculateScore(
                listOf(0, 1),
                listOf(0)
            )
        }
    }

    @Test
    fun updateWordStatus_toRemembered() {
        val result = LearningLogic.updateWordStatus(
            false,
            true
        )

        assertTrue(result)
    }

    @Test
    fun updateWordStatus_toNotRemembered() {
        val result = LearningLogic.updateWordStatus(
            true,
            false
        )

        assertFalse(result)
    }

    @Test
    fun calculatePercentage_returns75() {
        val result = LearningLogic.calculatePercentage(3, 4)
        assertEquals(75, result)
    }

    @Test
    fun calculatePercentage_zeroTotal_returnsZero() {
        val result = LearningLogic.calculatePercentage(0, 0)
        assertEquals(0, result)
    }
}
