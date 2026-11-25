package com.wordnest.util

import com.wordnest.data.local.entity.DeckStyle

class ValidationException(message: String) : IllegalArgumentException(message)

object Validation {

    /**
     * Kiểm tra tên Tổ từ vựng (F01/F02): bắt buộc, sau trim ≤ [Constants.MAX_DECK_NAME_LENGTH] ký tự.
     * @return thông báo lỗi tiếng Việt, hoặc `null` nếu hợp lệ.
     */
    fun validateDeckName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Tên Tổ từ vựng không được để trống"
            trimmed.length > Constants.MAX_DECK_NAME_LENGTH ->
                "Tên Tổ tối đa ${Constants.MAX_DECK_NAME_LENGTH} ký tự"
            else -> null
        }
    }

    /**
     * Kiểm tra mô tả Tổ từ vựng (tùy chọn): nếu có, sau trim ≤ [Constants.MAX_DECK_DESC_LENGTH] ký tự.
     * @return thông báo lỗi tiếng Việt, hoặc `null` nếu hợp lệ.
     */
    fun validateDeckDescription(description: String?): String? {
        val trimmed = description?.trim().orEmpty()
        return if (trimmed.length > Constants.MAX_DECK_DESC_LENGTH) {
            "Mô tả Tổ tối đa ${Constants.MAX_DECK_DESC_LENGTH} ký tự"
        } else {
            null
        }
    }

    fun validateDeckInput(
        name: String,
        description: String?,
        icon: String = DeckStyle.DEFAULT_ICON,
        color: String = DeckStyle.DEFAULT_COLOR
    ) {
        validateDeckName(name)?.let { throw ValidationException(it) }
        validateDeckDescription(description)?.let { throw ValidationException(it) }
        if (!DeckStyle.isValidIcon(icon)) {
            throw ValidationException("Biểu tượng Tổ không hợp lệ: $icon")
        }
        if (!DeckStyle.isValidColor(color)) {
            throw ValidationException("Màu sắc Tổ không hợp lệ: $color")
        }
    }

    fun validateWordTerm(term: String): String? {
        val trimmed = term.trim()
        return when {
            trimmed.isEmpty() -> "Từ tiếng Anh không được để trống"
            trimmed.length > Constants.MAX_WORD_TERM_LENGTH ->
                "Từ tiếng Anh tối đa ${Constants.MAX_WORD_TERM_LENGTH} ký tự"
            else -> null
        }
    }

    fun validateWordMeaning(meaning: String): String? {
        val trimmed = meaning.trim()
        return when {
            trimmed.isEmpty() -> "Nghĩa tiếng Việt không được để trống"
            trimmed.length > Constants.MAX_WORD_MEANING_LENGTH ->
                "Nghĩa tiếng Việt tối đa ${Constants.MAX_WORD_MEANING_LENGTH} ký tự"
            else -> null
        }
    }

    fun validateWordPhonetic(phonetic: String?): String? {
        val trimmed = phonetic?.trim().orEmpty()
        return if (trimmed.length > Constants.MAX_WORD_PHONETIC_LENGTH) {
            "Phiên âm tối đa ${Constants.MAX_WORD_PHONETIC_LENGTH} ký tự"
        } else {
            null
        }
    }

    fun validateWordExample(example: String?): String? {
        val trimmed = example?.trim().orEmpty()
        return if (trimmed.length > Constants.MAX_WORD_EXAMPLE_LENGTH) {
            "Câu ví dụ tối đa ${Constants.MAX_WORD_EXAMPLE_LENGTH} ký tự"
        } else {
            null
        }
    }

    fun validateWordNote(note: String?): String? {
        val trimmed = note?.trim().orEmpty()
        return if (trimmed.length > Constants.MAX_WORD_NOTE_LENGTH) {
            "Ghi chú tối đa ${Constants.MAX_WORD_NOTE_LENGTH} ký tự"
        } else {
            null
        }
    }

    fun validateWordInput(
        term: String,
        meaning: String,
        phonetic: String? = null,
        example: String? = null,
        note: String? = null
    ) {
        validateWordTerm(term)?.let { throw ValidationException(it) }
        validateWordMeaning(meaning)?.let { throw ValidationException(it) }
        validateWordPhonetic(phonetic)?.let { throw ValidationException(it) }
        validateWordExample(example)?.let { throw ValidationException(it) }
        validateWordNote(note)?.let { throw ValidationException(it) }
    }

    fun escapeLike(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
