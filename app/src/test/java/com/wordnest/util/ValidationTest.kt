package com.wordnest.util

import com.wordnest.data.local.entity.DeckStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidationTest {

    // --- validateDeckName ---

    @Test
    fun validateDeckName_blankOrSpacesOnly_returnsError() {
        assertNotNull(Validation.validateDeckName(""))
        assertNotNull(Validation.validateDeckName("   \t\n  "))
    }

    @Test
    fun validateDeckName_validAndBoundary100Chars_returnsNull() {
        assertNull(Validation.validateDeckName("IELTS"))
        assertNull(Validation.validateDeckName("  Từ vựng du lịch ✈️  "))
        assertNull(Validation.validateDeckName("a".repeat(Constants.MAX_DECK_NAME_LENGTH)))
        // Khoảng trắng hai đầu sau trim vẫn ≤ 100
        assertNull(Validation.validateDeckName("  " + "a".repeat(Constants.MAX_DECK_NAME_LENGTH) + "  "))
    }

    @Test
    fun validateDeckName_over100Chars_returnsError() {
        val error = Validation.validateDeckName("a".repeat(Constants.MAX_DECK_NAME_LENGTH + 1))
        assertNotNull(error)
    }

    // --- validateDeckDescription ---

    @Test
    fun validateDeckDescription_nullEmptyOrWithin500Chars_returnsNull() {
        assertNull(Validation.validateDeckDescription(null))
        assertNull(Validation.validateDeckDescription(""))
        assertNull(Validation.validateDeckDescription("   "))
        assertNull(Validation.validateDeckDescription("a".repeat(Constants.MAX_DECK_DESC_LENGTH)))
    }

    @Test
    fun validateDeckDescription_over500Chars_returnsError() {
        val error = Validation.validateDeckDescription("a".repeat(Constants.MAX_DECK_DESC_LENGTH + 1))
        assertNotNull(error)
    }

    // --- validateDeckInput ---

    @Test
    fun validateDeckInput_validInputs_doNotThrow() {
        Validation.validateDeckInput(
            name = "IELTS Academic",
            description = "Từ học thuật band 7+",
            icon = DeckStyle.ICON_GLOBE,
            color = DeckStyle.COLOR_YELLOW,
        )
    }

    @Test
    fun validateDeckInput_invalidFields_throwValidationException() {
        assertThrows(ValidationException::class.java) {
            Validation.validateDeckInput("   ", null)
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateDeckInput("a".repeat(101), null)
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateDeckInput("Valid", "b".repeat(501))
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateDeckInput("Valid", null, icon = "unknown-icon")
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateDeckInput("Valid", null, color = "neon-pink")
        }
    }

    // --- validateWordInput ---

    @Test
    fun validateWordInput_validWithOptionalNulls_doesNotThrow() {
        Validation.validateWordInput(
            term = "airport",
            meaning = "Sân bay",
            phonetic = null,
            example = null,
            note = null,
        )
        Validation.validateWordInput(
            term = "a".repeat(Constants.MAX_WORD_TERM_LENGTH),
            meaning = "b".repeat(Constants.MAX_WORD_MEANING_LENGTH),
            phonetic = "c".repeat(Constants.MAX_WORD_PHONETIC_LENGTH),
            example = "d".repeat(Constants.MAX_WORD_EXAMPLE_LENGTH),
            note = "e".repeat(Constants.MAX_WORD_NOTE_LENGTH),
        )
    }

    @Test
    fun validateWordInput_blankOrOverflow_throwsValidationException() {
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("  ", "Sân bay")
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("a".repeat(101), "Sân bay")
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("airport", "  ")
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("airport", "b".repeat(501))
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("airport", "Sân bay", phonetic = "c".repeat(101))
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("airport", "Sân bay", example = "d".repeat(501))
        }
        assertThrows(ValidationException::class.java) {
            Validation.validateWordInput("airport", "Sân bay", note = "e".repeat(501))
        }
    }

    // --- escapeLike ---

    @Test
    fun escapeLike_escapesWildcardsAndBackslash() {
        assertEquals("100\\%", Validation.escapeLike("100%"))
        assertEquals("word\\_nest", Validation.escapeLike("word_nest"))
        assertEquals("a\\\\b", Validation.escapeLike("a\\b"))
        assertEquals("\\\\\\%\\_test", Validation.escapeLike("\\%_test"))
    }
}
