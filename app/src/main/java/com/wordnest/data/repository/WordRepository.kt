package com.wordnest.data.repository

import com.wordnest.data.local.dao.DeckDao
import com.wordnest.data.local.dao.WordDao
import com.wordnest.data.local.dao.WordWithDeck
import com.wordnest.data.local.entity.Word
import com.wordnest.util.Constants
import com.wordnest.util.DateTimeUtil
import com.wordnest.util.Validation
import com.wordnest.util.ValidationException
import com.wordnest.util.WordStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/**
 * Repository cho các nghiệp vụ Từ vựng:
 *  - F04: Thêm từ vào Tổ
 *  - F05: Sửa, xóa mềm từ
 *  - F06: Xem danh sách từ trong 1 Tổ (có thể lọc theo status)
 *  - F07 (phần Từ): Tìm kiếm từ trong 1 Tổ hoặc toàn bộ Tổ của user
 */
class WordRepository(
    private val wordDao: WordDao,
    private val deckDao: DeckDao,
    private val nowProvider: () -> String = DateTimeUtil::nowIso,
    private val todayProvider: () -> String = DateTimeUtil::todayDate
) {

    /** F06: Danh sách từ trong Tổ */
    fun observeWordsByDeck(deckId: String, statusFilter: String? = null): Flow<List<Word>> =
        if (statusFilter.isNullOrBlank()) {
            wordDao.observeWordsByDeck(deckId)
        } else {
            wordDao.observeWordsByStatus(deckId, statusFilter)
        }

    fun observeWordById(wordId: String): Flow<Word?> =
        wordDao.observeById(wordId)

    suspend fun getWordById(wordId: String): Word? =
        wordDao.getById(wordId)

    /** Thống kê cấp độ đàn chim toàn hệ thống của User (cho màn Home / Thống kê) */
    fun observeCountByUserAndStatus(userId: String, status: String): Flow<Int> =
        wordDao.observeCountByUserAndStatus(userId, status)

    fun observeTotalWordCountByUser(userId: String): Flow<Int> =
        wordDao.observeTotalWordCountByUser(userId)

    fun observeDueCountByUser(userId: String): Flow<Int> =
        wordDao.observeDueCountByUser(userId, todayProvider())

    /** F07: Tìm kiếm từ trong 1 Tổ */
    fun observeSearchInDeck(deckId: String, keyword: String): Flow<List<Word>> {
        val trimmed = keyword.trim()
        return if (trimmed.isEmpty()) {
            wordDao.observeWordsByDeck(deckId)
        } else {
            wordDao.observeSearchInDeck(deckId, Validation.escapeLike(trimmed))
        }
    }

    /** F07: Tìm kiếm toàn cục mọi từ của user */
    fun observeSearchAllWords(userId: String, keyword: String): Flow<List<Word>> {
        val trimmed = keyword.trim()
        return wordDao.observeSearchAllWords(userId, Validation.escapeLike(trimmed))
    }

    /** F07 (T048): Tìm kiếm toàn cục kèm thông tin Tổ chứa từ (`WordWithDeck`); từ khoá rỗng trả danh sách rỗng. */
    fun observeSearchAllWordsWithDeck(userId: String, keyword: String): Flow<List<WordWithDeck>> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return flowOf(emptyList())
        return wordDao.observeSearchAllWordsWithDeck(userId, Validation.escapeLike(trimmed))
    }

    /** F04: Thêm từ mới */
    suspend fun addWord(
        deckId: String,
        term: String,
        meaning: String,
        phonetic: String? = null,
        example: String? = null,
        note: String? = null
    ): Result<Word> = safeCall {
        Validation.validateWordInput(term, meaning, phonetic, example, note)
        if (deckDao.getById(deckId) == null) {
            throw NoSuchElementException("Tổ không tồn tại (id=$deckId)")
        }
        val cleanTerm = term.trim()
        val cleanMeaning = meaning.trim()
        val cleanPhonetic = phonetic?.trim()?.takeIf { it.isNotEmpty() }
        val cleanExample = example?.trim()?.takeIf { it.isNotEmpty() }
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }

        if (wordDao.existsByTerm(deckId, cleanTerm, excludeId = null)) {
            throw ValidationException("Từ \"$cleanTerm\" đã có trong Tổ này")
        }

        val now = nowProvider()
        val word = Word(
            wordId = UUID.randomUUID().toString(),
            deckId = deckId,
            term = cleanTerm,
            meaning = cleanMeaning,
            phonetic = cleanPhonetic,
            example = cleanExample,
            note = cleanNote,
            status = WordStatus.EGG.storage,
            correctStreak = 0,
            wrongCount = 0,
            intervalDays = 0,
            nextReviewAt = todayProvider(), // sẵn sàng ôn ngay hôm nay
            lastReviewedAt = null,
            createAt = now,
            updatedAt = now,
            deleted = false
        )
        wordDao.insert(word)
        word
    }

    /** F05: Sửa nội dung từ (giữ nguyên tiến độ học, cập nhật updated_at) */
    suspend fun updateWord(
        word: Word,
        newTerm: String,
        newMeaning: String,
        newPhonetic: String?,
        newExample: String?,
        newNote: String? = word.note
    ): Result<Word> = safeCall {
        Validation.validateWordInput(newTerm, newMeaning, newPhonetic, newExample, newNote)
        val existing = wordDao.getById(word.wordId)
            ?: throw NoSuchElementException("Không tìm thấy từ (id=${word.wordId})")

        val cleanTerm = newTerm.trim()
        val cleanMeaning = newMeaning.trim()
        val cleanPhonetic = newPhonetic?.trim()?.takeIf { it.isNotEmpty() }
        val cleanExample = newExample?.trim()?.takeIf { it.isNotEmpty() }
        val cleanNote = newNote?.trim()?.takeIf { it.isNotEmpty() }

        if (wordDao.existsByTerm(existing.deckId, cleanTerm, excludeId = existing.wordId)) {
            throw ValidationException("Từ \"$cleanTerm\" đã có trong Tổ này")
        }

        val updated = existing.copy(
            term = cleanTerm,
            meaning = cleanMeaning,
            phonetic = cleanPhonetic,
            example = cleanExample,
            note = cleanNote,
            updatedAt = nowProvider()
        )
        wordDao.update(updated)
        updated
    }

    /** AI2 / F14: Cập nhật câu ví dụ (không đổi status hay last_reviewed_at, có cập nhật updated_at). */
    suspend fun updateExample(wordId: String, example: String): Result<Unit> = safeCall {
        val trimmed = example.trim()
        if (trimmed.length > Constants.MAX_WORD_EXAMPLE_LENGTH) {
            throw ValidationException(
                "Câu ví dụ tối đa ${Constants.MAX_WORD_EXAMPLE_LENGTH} ký tự"
            )
        }
        val cleanExample = trimmed.takeIf { it.isNotEmpty() }
        val updatedRows = wordDao.updateExample(wordId, cleanExample, nowProvider())
        if (updatedRows == 0) {
            throw NoSuchElementException("Không tìm thấy từ (id=$wordId)")
        }
    }

    /** F05: Xóa mềm từ (tuân thủ hợp đồng Firestore FB3) */
    suspend fun deleteWord(wordId: String): Result<Unit> = safeCall {
        wordDao.getById(wordId)
            ?: throw NoSuchElementException("Không tìm thấy từ (id=$wordId)")
        wordDao.softDelete(wordId, nowProvider())
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
