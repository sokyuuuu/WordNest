package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordStatusTest {

    @Test
    fun storageValuesAreLowercaseMatchingContract() {
        assertEquals("egg", WordStatus.EGG.storage)
        assertEquals("chick", WordStatus.CHICK.storage)
        assertEquals("bird", WordStatus.BIRD.storage)
    }

    @Test
    fun emojisAndLabelsMatchDesignSystem() {
        assertEquals("🥚", WordStatus.EGG.emoji)
        assertEquals("🐣", WordStatus.CHICK.emoji)
        assertEquals("🕊️", WordStatus.BIRD.emoji)
        assertEquals("Trứng", WordStatus.EGG.label)
        assertEquals("Chim non", WordStatus.CHICK.label)
        assertEquals("Chim bay", WordStatus.BIRD.label)
    }

    @Test
    fun fromStorageParsesCaseInsensitivelyAndFallsBackToEgg() {
        assertEquals(WordStatus.EGG, WordStatus.fromStorage("egg"))
        assertEquals(WordStatus.CHICK, WordStatus.fromStorage("chick"))
        assertEquals(WordStatus.BIRD, WordStatus.fromStorage("bird"))
        assertEquals(WordStatus.BIRD, WordStatus.fromStorage("BIRD"))
        assertEquals(WordStatus.EGG, WordStatus.fromStorage(null))
        assertEquals(WordStatus.EGG, WordStatus.fromStorage("unknown"))
    }

    @Test
    fun isValidStorageAcceptsOnlyCanonicalLowercaseValues() {
        assertTrue(WordStatus.isValidStorage("egg"))
        assertTrue(WordStatus.isValidStorage("chick"))
        assertTrue(WordStatus.isValidStorage("bird"))
        assertFalse(WordStatus.isValidStorage("EGG"))
        assertFalse(WordStatus.isValidStorage(""))
        assertFalse(WordStatus.isValidStorage(null))
    }
}
