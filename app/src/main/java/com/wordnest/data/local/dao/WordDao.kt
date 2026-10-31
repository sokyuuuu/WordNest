package com.wordnest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM Word WHERE word_id = :wordId LIMIT 1")
    suspend fun getById(wordId: String): Word?

    @Query("SELECT * FROM Word WHERE word_id = :wordId LIMIT 1")
    fun observeById(wordId: String): Flow<Word?>

    @Query("SELECT * FROM Word WHERE deck_id = :deckId ORDER BY create_at DESC")
    fun observeWordsByDeck(deckId: String): Flow<List<Word>>

    @Query("SELECT * FROM Word WHERE deck_id = :deckId ORDER BY create_at DESC")
    suspend fun getWordsByDeck(deckId: String): List<Word>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId AND status = :status
        ORDER BY create_at DESC
    """)
    fun observeWordsByStatus(deckId: String, status: String): Flow<List<Word>>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId
          AND (next_review_at IS NULL OR next_review_at <= :today)
        ORDER BY next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getDueWordsByDeck(deckId: String, today: String, limit: Int = 50): List<Word>

    @Query("""
        SELECT w.* FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND (w.next_review_at IS NULL OR w.next_review_at <= :today)
        ORDER BY w.next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getDueWordsByUser(userId: String, today: String, limit: Int = 50): List<Word>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId AND word_id != :excludeWordId
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomDistractors(deckId: String, excludeWordId: String, limit: Int = 3): List<Word>

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId")
    suspend fun countByDeck(deckId: String): Int

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId AND status = :status")
    suspend fun countByDeckAndStatus(deckId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM Word WHERE deck_id = :deckId AND status = :status")
    fun observeCountByDeckAndStatus(deckId: String, status: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
    """)
    fun observeTotalWordCountByUser(userId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId AND w.status = :status
    """)
    fun observeCountByUserAndStatus(userId: String, status: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND w.next_review_at IS NOT NULL
          AND w.next_review_at <= :today
    """)
    fun observeDueCountByUser(userId: String, today: String): Flow<Int>

    @Query("""
        SELECT * FROM Word
        WHERE deck_id = :deckId
          AND (term LIKE '%' || :keyword || '%' ESCAPE '\'
               OR meaning LIKE '%' || :keyword || '%' ESCAPE '\'
               OR IFNULL(example, '') LIKE '%' || :keyword || '%' ESCAPE '\')
        ORDER BY term COLLATE NOCASE ASC
    """)
    fun observeSearchInDeck(deckId: String, keyword: String): Flow<List<Word>>

    @Query("""
        SELECT w.* FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE d.user_id = :userId
          AND (w.term LIKE '%' || :keyword || '%' ESCAPE '\'
               OR w.meaning LIKE '%' || :keyword || '%' ESCAPE '\'
               OR IFNULL(w.example, '') LIKE '%' || :keyword || '%' ESCAPE '\')
        ORDER BY w.term COLLATE NOCASE ASC
    """)
    fun observeSearchAllWords(userId: String, keyword: String): Flow<List<Word>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM Word
            WHERE deck_id = :deckId
              AND LOWER(TRIM(term)) = LOWER(TRIM(:term))
              AND (:excludeId IS NULL OR word_id != :excludeId)
        )
    """)
    suspend fun existsByTerm(deckId: String, term: String, excludeId: String? = null): Boolean

    @Query("DELETE FROM Word WHERE word_id = :wordId")
    suspend fun deleteById(wordId: String)

    @Query("DELETE FROM Word WHERE deck_id = :deckId")
    suspend fun deleteByDeck(deckId: String)

    @Query("""
        UPDATE Word
        SET status = :status,
            correct_streak = :correctStreak,
            wrong_count = :wrongCount,
            interval_days = :intervalDays,
            next_review_at = :nextReviewAt,
            last_review_at = :lastReviewAt
        WHERE word_id = :wordId
    """)
    suspend fun updateReviewProgress(
        wordId: String,
        status: String,
        correctStreak: Int,
        wrongCount: Int,
        intervalDays: Int,
        nextReviewAt: String,
        lastReviewAt: String
    )
}
