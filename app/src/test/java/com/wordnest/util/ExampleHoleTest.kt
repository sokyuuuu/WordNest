package com.wordnest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kiểm thử đủ 9 ca bắt buộc trong `contracts/example-hole.md` + ca phụ (trim, cụm nhiều từ).
 */
class ExampleHoleTest {

    @Test
    fun exactLowercaseWord_isReplacedWithHole() {
        assertEquals(
            "I go to the ___ by bus.",
            ExampleHole.replace("I go to the airport by bus.", "airport"),
        )
    }

    @Test
    fun capitalizedWordInExample_matchesCaseInsensitive() {
        assertEquals(
            "The ___ is big.",
            ExampleHole.replace("The Airport is big.", "airport"),
        )
    }

    @Test
    fun multipleOccurrences_replacesAll() {
        assertEquals(
            "___ a ___ now",
            ExampleHole.replace("Book a book now", "book"),
        )
    }

    @Test
    fun substringInsideAnotherWord_doesNotMatchAndReturnsNull() {
        assertNull(ExampleHole.replace("A notebook", "book"))
    }

    @Test
    fun wordFollowedByPunctuationAndSentenceWithApostrophe_matchesCleanly() {
        assertEquals(
            "He's at ___.",
            ExampleHole.replace("He's at home.", "home"),
        )
    }

    @Test
    fun contractionWithApostrophe_matchesWholeTerm() {
        assertEquals(
            "___ stop",
            ExampleHole.replace("Don't stop", "don't"),
        )
    }

    @Test
    fun hyphenatedTerm_matchesWholeTerm() {
        assertEquals(
            "___ me",
            ExampleHole.replace("e-mail me", "e-mail"),
        )
    }

    @Test
    fun exampleAlreadyContainingHole_isReturnedAsIsTrimmed() {
        assertEquals(
            "Already has ___",
            ExampleHole.replace("  Already has ___  ", "book"),
        )
    }

    @Test
    fun nullOrBlankExampleOrBlankTerm_returnsNull() {
        assertNull(ExampleHole.replace(null, "x"))
        assertNull(ExampleHole.replace("", "x"))
        assertNull(ExampleHole.replace("   ", "x"))
        assertNull(ExampleHole.replace("Some sentence", "   "))
    }

    @Test
    fun multiWordTermAndSurroundingWhitespace_replacesAndTrims() {
        assertEquals(
            "Show your ___ at the gate.",
            ExampleHole.replace("  Show your boarding pass at the gate.  ", "boarding pass"),
        )
    }
}
