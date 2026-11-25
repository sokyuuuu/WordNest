package com.wordnest.data.study

import com.wordnest.data.local.dao.WordDao
import com.wordnest.data.local.entity.Word
import com.wordnest.util.Constants
import com.wordnest.util.DateTimeUtil
import com.wordnest.util.WordStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

/**
 * Triển khai [StudyWordSource] nối trực tiếp vào Room (`WordDao`).
 * - Chỉ phát các từ chưa xoá mềm thuộc các Tổ chưa xoá mềm.
 * - Khi lưu tiến độ học, chỉ cập nhật `status`, `last_reviewed_at` (ISO-8601 UTC) và `updated_at`.
 */
class RoomStudyWordSource(
    private val wordDao: WordDao,
    private val userId: String = Constants.DEFAULT_USER_ID,
    private val nowProvider: () -> String = DateTimeUtil::nowIso,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : StudyWordSource {

    override fun words(): Flow<List<StudyWord>> =
        wordDao.observeActiveWordsByUser(userId).map { entities ->
            entities.map { it.toStudyWord() }
        }

    override suspend fun save(word: StudyWord) {
        val lastReviewedIso = word.lastReviewedAt?.let {
            DateTimeUtil.localDateToIsoUtc(it, zoneId)
        }
        wordDao.updateStudyState(
            wordId = word.id,
            status = word.status.storage,
            lastReviewedAt = lastReviewedIso,
            updatedAt = nowProvider(),
        )
    }

    private fun Word.toStudyWord(): StudyWord = StudyWord(
        id = wordId,
        term = term,
        meaning = meaning,
        phonetic = phonetic,
        status = WordStatus.fromStorage(status),
        lastReviewedAt = DateTimeUtil.parseIsoToLocalDate(lastReviewedAt, zoneId),
        example = example,
        deckId = deckId,
        updatedAt = updatedAt,
    )
}
