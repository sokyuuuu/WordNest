package com.wordnest.util

/**
 * So khớp câu trả lời gõ lại từ (F14).
 * Quy tắc theo TC14-03: không phân biệt hoa thường, bỏ khoảng trắng hai đầu và gộp nhiều khoảng trắng.
 */
object TypedAnswer {

    fun normalize(input: String): String =
        input.trim().lowercase().replace(Regex("\\s+"), " ")

    fun isCorrect(userInput: String, term: String): Boolean =
        normalize(userInput).isNotEmpty() && normalize(userInput) == normalize(term)
}
