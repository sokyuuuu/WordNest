package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningLogicTest {

    @Test
    fun correctAnswer_returnsTrue() {
        assertTrue(LearningLogic.isCorrectAnswer(1, 1))
    }

    @Test
    fun wrongAnswer_returnsFalse() {
        assertFalse(LearningLogic.isCorrectAnswer(2, 1))
    }

    @Test
    fun calculateScore_returnsThree() {
        val selected = listOf(0, 1, 2, 0)
        val correct = listOf(0, 1, 2, 1)
        assertEquals(3, LearningLogic.calculateScore(selected, correct))
    }

    @Test
    fun calculateScore_allWrong_returnsZero() {
        val selected = listOf(1, 2, 3, 0)
        val correct = listOf(0, 1, 2, 1)
        assertEquals(0, LearningLogic.calculateScore(selected, correct))
    }

    @Test
    fun calculateScore_differentSizes_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            LearningLogic.calculateScore(listOf(0, 1), listOf(0))
        }
    }

    @Test
    fun calculatePercentage_returns75() {
        assertEquals(75, LearningLogic.calculatePercentage(3, 4))
    }

    @Test
    fun calculatePercentage_zeroTotal_returnsZero() {
        assertEquals(0, LearningLogic.calculatePercentage(0, 0))
    }
}
