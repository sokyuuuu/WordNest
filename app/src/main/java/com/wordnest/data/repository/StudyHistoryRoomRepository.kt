package com.wordnest.data.repository

import com.wordnest.data.local.dao.HistoryActivity
import com.wordnest.data.local.dao.StudyHistoryDao
import com.wordnest.data.local.entity.StudyHistory
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

enum class HistoryResult(val storage: String) {
    CORRECT("correct"),
    WRONG("wrong"),
}

enum class HistoryMode(val storage: String) {
    FLASHCARD("flashcard"),
    QUIZ("quiz"),
    TYPE_IN("type_in"),
}

/**
 * Repository tầng Room cho bảng `study_history` theo hợp đồng `contracts/study-history-contract.md`.
 * - `StudyViewModel` gọi [record] khi kết thúc thẻ/trả lời câu hỏi.
 * - Màn Thống kê (Ngọc ST1) đọc [observeAccuracy], [observeRecentActivity], [observeWordsToReview].
 */
class StudyHistoryRoomRepository(
    private val dao: StudyHistoryDao,
) {
    /** F23 – Độ chính xác %: correct / (correct + wrong). Trả 0.0 nếu chưa có dữ liệu. */
    fun observeAccuracy(): Flow<Double> = dao.observeAccuracy()

    /** F23 – Hoạt động gần đây (mới nhất trước) kèm tên từ & Tổ. */
    fun observeRecentActivity(limit: Int = 5): Flow<List<HistoryActivity>> =
        dao.observeRecentActivity(limit)

    /** F23 – Từ làm sai gần nhất (distinct theo từ, mới nhất trước). */
    fun observeWordsToReview(limit: Int = 20): Flow<List<Word>> =
        dao.observeWordsToReview(limit)

    suspend fun record(
        wordId: String,
        result: HistoryResult,
        mode: HistoryMode,
        at: Instant = Instant.now(),
    ) {
        dao.record(
            StudyHistory(
                entryId = UUID.randomUUID().toString(),
                wordId = wordId,
                result = result.storage,
                mode = mode.storage,
                createdAt = at.toString(),
            )
        )
    }
}
