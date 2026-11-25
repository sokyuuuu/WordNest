package com.wordnest.util

/**
 * Thay thế từ vựng bằng chỗ trống (`___`) trong câu ví dụ cho màn Điền từ (F14).
 * Tuân thủ đúng hợp đồng `specs/001-room-decks/contracts/example-hole.md`:
 * 1. Nếu câu ví dụ đã chứa `___` (do AI sinh sẵn) → trả nguyên câu sau khi trim.
 * 2. Khớp nguyên từ (`\bterm\b`) không phân biệt hoa/thường (`Regex.escape` cho `term`).
 * 3. Thay tất cả lần xuất hiện.
 * 4. Không khớp hoặc đầu vào null/trống → trả `null` (màn Điền từ giữ nguyên như v1.0).
 * 5. Trim khoảng trắng thừa hai đầu.
 */
object ExampleHole {
    const val HOLE = "___"

    fun replace(example: String?, term: String): String? {
        val trimmedExample = example?.trim().orEmpty()
        if (trimmedExample.isEmpty()) return null
        if (trimmedExample.contains(HOLE)) return trimmedExample

        val trimmedTerm = term.trim()
        if (trimmedTerm.isEmpty()) return null

        val regex = Regex("\\b${Regex.escape(trimmedTerm)}\\b", RegexOption.IGNORE_CASE)
        if (!regex.containsMatchIn(trimmedExample)) return null
        return regex.replace(trimmedExample, HOLE)
    }
}
