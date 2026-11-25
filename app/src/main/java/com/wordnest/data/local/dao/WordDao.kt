package com.wordnest.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/** Kết quả tìm kiếm từ toàn cục kèm tên/biểu tượng/màu của Tổ chứa từ (F07 — T048). */
data class WordWithDeck(
    @Embedded val word: Word,
    @ColumnInfo(name = "deck_name") val deckName: String,
    @ColumnInfo(name = "deck_icon") val deckIcon: String = DeckStyle.DEFAULT_ICON,
    @ColumnInfo(name = "deck_color") val deckColor: String = DeckStyle.DEFAULT_COLOR,
)

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Delete
    suspend fun delete(word: Word)

    @Update
    suspend fun update(word: Word)

    @Query("SELECT * FROM Word WHERE word_id = :wordId AND deleted = 0 LIMIT 1")
    suspend fun getById(wordId: String): Word?

    /** Đọc bản ghi kể cả đã xoá mềm (phục vụ đồng bộ FB3 & kiểm thử). */
    @Query("SELECT * FROM Word WHERE word_id = :wordId LIMIT 1")
    suspend fun getByIdIncludingDeleted(wordId: String): Word?

    @Query("SELECT * FROM Word WHERE word_id = :wordId AND deleted = 0 LIMIT 1")
    fun observeById(wordId: String): Flow<Word?>

    @Query("SELECT * FROM Word WHERE deck_id = :deckId AND deleted = 0 ORDER BY create_at DESC")
    fun observeWordsByDeck(deckId: String): Flow<List<Word>>

    @Query("SELECT * FROM Word WHERE deck_id = :deckId AND deleted = 0 ORDER BY create_at DESC")
    suspend fun getWordsByDeck(deckId: String): List<Word>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId AND status = :status AND deleted = 0
        ORDER BY create_at DESC
    """)
    fun observeWordsByStatus(deckId: String, status: String): Flow<List<Word>>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId
          AND deleted = 0
          AND (next_review_at IS NULL OR next_review_at <= :today)
        ORDER BY next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getDueWordsByDeck(deckId: String, today: String, limit: Int = 50): List<Word>

    @Query("""
        SELECT w.* FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND w.deleted = 0
          AND (w.next_review_at IS NULL OR w.next_review_at <= :today)
        ORDER BY w.next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getDueWordsByUser(userId: String, today: String, limit: Int = 50): List<Word>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId AND word_id != :excludeWordId AND deleted = 0
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomDistractors(deckId: String, excludeWordId: String, limit: Int = 3): List<Word>

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId AND deleted = 0")
    suspend fun countByDeck(deckId: String): Int

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId AND status = :status AND deleted = 0")
    suspend fun countByDeckAndStatus(deckId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId AND status = :status AND deleted = 0")
    fun observeCountByDeckAndStatus(deckId: String, status: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId AND d.deleted = 0 AND w.deleted = 0
    """)
    fun observeTotalWordCountByUser(userId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId AND d.deleted = 0 AND w.deleted = 0 AND w.status = :status
    """)
    fun observeCountByUserAndStatus(userId: String, status: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND w.deleted = 0
          AND w.next_review_at IS NOT NULL
          AND w.next_review_at <= :today
    """)
    fun observeDueCountByUser(userId: String, today: String): Flow<Int>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId
          AND deleted = 0
          AND (LOWER(term) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(meaning) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(IFNULL(example, '')) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\')
        ORDER BY term COLLATE NOCASE ASC
    """)
    fun observeSearchInDeck(deckId: String, keyword: String): Flow<List<Word>>

    @Query("""
        SELECT w.* FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND w.deleted = 0
          AND (LOWER(w.term) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(w.meaning) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(IFNULL(w.example, '')) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\')
        ORDER BY w.term COLLATE NOCASE ASC
    """)
    fun observeSearchAllWords(userId: String, keyword: String): Flow<List<Word>>

    @Query("""
        SELECT w.*,
               d.name_deck AS deck_name,
               d.icon AS deck_icon,
               d.color AS deck_color
        FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND w.deleted = 0
          AND (LOWER(w.term) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(w.meaning) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\'
               OR LOWER(IFNULL(w.example, '')) LIKE '%' || LOWER(:keyword) || '%' ESCAPE '\')
        ORDER BY w.term COLLATE NOCASE ASC
    """)
    fun observeSearchAllWordsWithDeck(userId: String, keyword: String): Flow<List<WordWithDeck>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM Word
            WHERE deck_id = :deckId
              AND deleted = 0
              AND LOWER(TRIM(term)) = LOWER(TRIM(:term))
              AND (:excludeId IS NULL OR word_id != :excludeId)
        )
    """)
    suspend fun existsByTerm(deckId: String, term: String, excludeId: String? = null): Boolean

    @Query("UPDATE Word SET deleted = 1, updated_at = :updatedAt WHERE word_id = :wordId AND deleted = 0")
    suspend fun softDelete(wordId: String, updatedAt: String): Int

    @Query("UPDATE Word SET deleted = 1, updated_at = :updatedAt WHERE deck_id = :deckId AND deleted = 0")
    suspend fun softDeleteByDeck(deckId: String, updatedAt: String): Int

    @Query("DELETE FROM Word WHERE word_id = :wordId")
    suspend fun deleteById(wordId: String)

    @Query("DELETE FROM Word WHERE deck_id = :deckId")
    suspend fun deleteByDeck(deckId: String)

    @Query("""
        SELECT w.* FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND w.deleted = 0
        ORDER BY w.create_at ASC, w.word_id ASC
    """)
    fun observeActiveWordsByUser(userId: String): Flow<List<Word>>

    @Query("""
        UPDATE Word
        SET status = :status,
            last_reviewed_at = :lastReviewedAt,
            updated_at = :updatedAt
        WHERE word_id = :wordId AND deleted = 0
    """)
    suspend fun updateStudyState(
        wordId: String,
        status: String,
        lastReviewedAt: String?,
        updatedAt: String
    ): Int

    @Query("UPDATE Word SET example = :example, updated_at = :updatedAt WHERE word_id = :wordId AND deleted = 0")
    suspend fun updateExample(wordId: String, example: String?, updatedAt: String): Int

    @Query("""
        UPDATE Word
        SET status = :status,
            correct_streak = :correctStreak,
            wrong_count = :wrongCount,
            interval_days = :intervalDays,
            next_review_at = :nextReviewAt,
            last_reviewed_at = :lastReviewedAt,
            updated_at = :updatedAt
        WHERE word_id = :wordId AND deleted = 0
    """)
    suspend fun updateReviewProgress(
        wordId: String,
        status: String,
        correctStreak: Int,
        wrongCount: Int,
        intervalDays: Int,
        nextReviewAt: String?,
        lastReviewedAt: String?,
        updatedAt: String
    )
}
