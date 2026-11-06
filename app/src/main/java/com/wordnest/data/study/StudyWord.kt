package com.wordnest.data.study

import com.wordnest.util.WordStatus
import java.time.LocalDate

/**
 * Một từ dùng cho luồng học (flashcard, trắc nghiệm, điền từ).
 * Giao diện trung gian: khi Room được gắn vào, thay nguồn dữ liệu bằng bảng Word.
 */
data class StudyWord(
    val id: String,
    val term: String,
    val meaning: String,
    val phonetic: String? = null,
    val status: WordStatus = WordStatus.EGG,
    val lastReviewedAt: LocalDate? = null,
)
