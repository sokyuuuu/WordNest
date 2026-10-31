package com.wordnest.util

import com.wordnest.data.local.entity.DeckStyle
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Constants {
    const val DEFAULT_USER_ID = "user_default"
    const val DEFAULT_DISPLAY_NAME = "Người học WordNest"

    const val MAX_DECK_NAME_LENGTH = 80
    const val MAX_DECK_DESC_LENGTH = 300
    const val MAX_WORD_TERM_LENGTH = 100
    const val MAX_WORD_MEANING_LENGTH = 500
    const val MAX_WORD_PHONETIC_LENGTH = 100
    const val MAX_WORD_EXAMPLE_LENGTH = 500
    const val MAX_WORD_NOTE_LENGTH = 500
}

object DateTimeUtil {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun nowIso(): String = Instant.now().toString()

    fun todayDate(): String = LocalDate.now().format(DATE_FORMATTER)

    fun plusDays(dateStr: String, days: Int): String =
        LocalDate.parse(dateStr, DATE_FORMATTER)
            .plusDays(days.toLong())
            .format(DATE_FORMATTER)
}

class ValidationException(message: String) : IllegalArgumentException(message)

object Validation {
    fun validateDeckInput(
        name: String,
        description: String?,
        icon: String = DeckStyle.DEFAULT_ICON,
        color: String = DeckStyle.DEFAULT_COLOR
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw ValidationException("Tên Tổ từ vựng không được để trống")
        }
        if (trimmedName.length > Constants.MAX_DECK_NAME_LENGTH) {
            throw ValidationException(
                "Tên Tổ tối đa ${Constants.MAX_DECK_NAME_LENGTH} ký tự"
            )
        }
        if (description != null && description.trim().length > Constants.MAX_DECK_DESC_LENGTH) {
            throw ValidationException(
                "Mô tả Tổ tối đa ${Constants.MAX_DECK_DESC_LENGTH} ký tự"
            )
        }
        if (!DeckStyle.isValidIcon(icon)) {
            throw ValidationException("Biểu tượng Tổ không hợp lệ: $icon")
        }
        if (!DeckStyle.isValidColor(color)) {
            throw ValidationException("Màu sắc Tổ không hợp lệ: $color")
        }
    }

    fun validateWordInput(
        term: String,
        meaning: String,
        phonetic: String? = null,
        example: String? = null,
        note: String? = null
    ) {
        val trimmedTerm = term.trim()
        val trimmedMeaning = meaning.trim()

        if (trimmedTerm.isEmpty()) {
            throw ValidationException("Từ tiếng Anh không được để trống")
        }
        if (trimmedTerm.length > Constants.MAX_WORD_TERM_LENGTH) {
            throw ValidationException(
                "Từ tiếng Anh tối đa ${Constants.MAX_WORD_TERM_LENGTH} ký tự"
            )
        }
        if (trimmedMeaning.isEmpty()) {
            throw ValidationException("Nghĩa tiếng Việt không được để trống")
        }
        if (trimmedMeaning.length > Constants.MAX_WORD_MEANING_LENGTH) {
            throw ValidationException(
                "Nghĩa tiếng Việt tối đa ${Constants.MAX_WORD_MEANING_LENGTH} ký tự"
            )
        }
        if (phonetic != null && phonetic.trim().length > Constants.MAX_WORD_PHONETIC_LENGTH) {
            throw ValidationException(
                "Phiên âm tối đa ${Constants.MAX_WORD_PHONETIC_LENGTH} ký tự"
            )
        }
        if (example != null && example.trim().length > Constants.MAX_WORD_EXAMPLE_LENGTH) {
            throw ValidationException(
                "Câu ví dụ tối đa ${Constants.MAX_WORD_EXAMPLE_LENGTH} ký tự"
            )
        }
        if (note != null && note.trim().length > Constants.MAX_WORD_NOTE_LENGTH) {
            throw ValidationException(
                "Ghi chú tối đa ${Constants.MAX_WORD_NOTE_LENGTH} ký tự"
            )
        }
    }

    fun escapeLike(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
