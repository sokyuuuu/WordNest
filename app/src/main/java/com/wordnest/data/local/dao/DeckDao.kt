package com.wordnest.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

data class DeckWithWords(
    @Embedded val deck: Deck,
    @Relation(
        parentColumn = "deck_id",
        entityColumn = "deck_id"
    )
    val words: List<Word>
)

data class DeckWithStats(
    @Embedded val deck: Deck,
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
    @ColumnInfo(name = "learned_count") val learnedCount: Int = 0,
    @ColumnInfo(name = "due_count") val dueCount: Int = 0
) {
    val progressPercent: Float
        get() = if (wordCount <= 0) 0f else (learnedCount.toFloat() / wordCount.toFloat()).coerceIn(0f, 1f)
}

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deck: Deck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(decks: List<Deck>)

    @Delete
    suspend fun delete(deck: Deck)

    @Update
    suspend fun update(deck: Deck)

    @Query("SELECT * FROM Deck WHERE deck_id = :deckId LIMIT 1")
    suspend fun getById(deckId: String): Deck?

    @Query("SELECT * FROM Deck WHERE deck_id = :deckId LIMIT 1")
    fun observeById(deckId: String): Flow<Deck?>

    @Query("SELECT * FROM Deck WHERE user_id = :userId ORDER BY create_at DESC")
    fun observeDecksByUser(userId: String): Flow<List<Deck>>

    @Query("SELECT * FROM Deck WHERE user_id = :userId ORDER BY create_at DESC")
    suspend fun getDecksByUser(userId: String): List<Deck>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND UPPER(w.status) != 'EGG' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id
        WHERE d.user_id = :userId
        GROUP BY d.deck_id
        ORDER BY d.create_at DESC
    """)
    fun observeDecksWithStats(userId: String, today: String): Flow<List<DeckWithStats>>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND UPPER(w.status) != 'EGG' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id
        WHERE d.deck_id = :deckId
        GROUP BY d.deck_id
        LIMIT 1
    """)
    fun observeDeckWithStatsById(deckId: String, today: String): Flow<DeckWithStats?>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND UPPER(w.status) != 'EGG' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id
        WHERE d.deck_id = :deckId
        GROUP BY d.deck_id
        LIMIT 1
    """)
    suspend fun getDeckWithStatsById(deckId: String, today: String): DeckWithStats?

    @Transaction
    @Query("SELECT * FROM Deck WHERE deck_id = :deckId LIMIT 1")
    fun observeDeckWithWords(deckId: String): Flow<DeckWithWords?>

    @Transaction
    @Query("SELECT * FROM Deck WHERE deck_id = :deckId LIMIT 1")
    suspend fun getDeckWithWords(deckId: String): DeckWithWords?

    @Query("""
        SELECT * FROM Deck
        WHERE user_id = :userId
          AND (name_deck LIKE '%' || :keyword || '%' ESCAPE '\'
               OR description LIKE '%' || :keyword || '%' ESCAPE '\')
        ORDER BY create_at DESC
    """)
    fun searchDecks(userId: String, keyword: String): Flow<List<Deck>>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND UPPER(w.status) != 'EGG' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id
        WHERE d.user_id = :userId
          AND (d.name_deck LIKE '%' || :keyword || '%' ESCAPE '\'
               OR IFNULL(d.description, '') LIKE '%' || :keyword || '%' ESCAPE '\')
        GROUP BY d.deck_id
        ORDER BY d.create_at DESC
    """)
    fun observeSearchDecks(userId: String, keyword: String, today: String): Flow<List<DeckWithStats>>

    @Query("SELECT COUNT(*) FROM Deck WHERE user_id = :userId")
    suspend fun countByUser(userId: String): Int

    @Query("SELECT name_deck FROM Deck WHERE user_id = :userId AND (:excludeId IS NULL OR deck_id != :excludeId)")
    suspend fun getDeckNames(userId: String, excludeId: String? = null): List<String>

    suspend fun existsByName(userId: String, name: String, excludeId: String? = null): Boolean {
        val target = name.trim()
        return getDeckNames(userId, excludeId).any { it.trim().equals(target, ignoreCase = true) }
    }

    @Query("""
        UPDATE Deck
        SET name_deck = :name, description = :description, icon = :icon, color = :color
        WHERE deck_id = :deckId
    """)
    suspend fun updateInfo(deckId: String, name: String, description: String?, icon: String, color: String)

    @Query("DELETE FROM Deck WHERE deck_id = :deckId")
    suspend fun deleteById(deckId: String)
}
