package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm tra so khớp từ khi điền (F14) theo TC14-01, TC14-02, TC14-03. */
class TypedAnswerTest {

    @Test
    fun exactInput_isCorrect() {
        assertTrue(TypedAnswer.isCorrect("hello", "hello"))
    }

    @Test
    fun wrongInput_isIncorrect() {
        assertFalse(TypedAnswer.isCorrect("helo", "hello"))
    }

    @Test
    fun caseInsensitive() {
        assertTrue(TypedAnswer.isCorrect(" HELLO ", "hello"))
    }

    @Test
    fun multipleSpacesCollapse() {
        assertTrue(TypedAnswer.isCorrect("ice  cream", "ice cream"))
    }

    @Test
    fun emptyInput_isIncorrect() {
        assertFalse(TypedAnswer.isCorrect("   ", "hello"))
    }
}
