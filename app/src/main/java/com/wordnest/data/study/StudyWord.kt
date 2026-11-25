package com.wordnest.data.study

import com.wordnest.util.WordStatus
import java.time.LocalDate

/**
 * Một từ dùng cho luồng học (flashcard, trắc nghiệm, điền từ) và thống kê đàn chim.
 * Ánh xạ trực tiếp từ entity Room `Word`.
 */
data class StudyWord(
    val id: String,
    val term: String,
    val meaning: String,
    val phonetic: String? = null,
    val status: WordStatus = WordStatus.EGG,
    val lastReviewedAt: LocalDate? = null,
    val example: String? = null,
    val deckId: String = "",
    val updatedAt: String = "",
)
