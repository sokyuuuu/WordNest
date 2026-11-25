package com.wordnest.data.study

import kotlinx.coroutines.flow.Flow

/**
 * Nguồn từ cho luồng học (Flashcard, Trắc nghiệm, Điền từ), Trang chủ và Thống kê.
 * Trả [Flow] để giao diện tự cập nhật ngay khi dữ liệu Room hoặc đồng bộ thay đổi.
 */
interface StudyWordSource {
    fun words(): Flow<List<StudyWord>>
    suspend fun save(word: StudyWord)
}
