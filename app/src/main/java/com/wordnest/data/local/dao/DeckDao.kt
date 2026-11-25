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
import kotlinx.coroutines.flow.map

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

    @Query("SELECT * FROM Deck WHERE deck_id = :deckId AND deleted = 0 LIMIT 1")
    suspend fun getById(deckId: String): Deck?

    /** Đọc bản ghi kể cả đã xoá mềm (phục vụ đồng bộ FB3 & kiểm thử). */
    @Query("SELECT * FROM Deck WHERE deck_id = :deckId LIMIT 1")
    suspend fun getByIdIncludingDeleted(deckId: String): Deck?

    @Query("SELECT * FROM Deck WHERE deck_id = :deckId AND deleted = 0 LIMIT 1")
    fun observeById(deckId: String): Flow<Deck?>

    @Query("SELECT * FROM Deck WHERE user_id = :userId AND deleted = 0 ORDER BY create_at DESC")
    fun observeDecksByUser(userId: String): Flow<List<Deck>>

    @Query("SELECT * FROM Deck WHERE user_id = :userId AND deleted = 0 ORDER BY create_at DESC")
    suspend fun getDecksByUser(userId: String): List<Deck>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND LOWER(w.status) != 'egg' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id AND w.deleted = 0
        WHERE d.user_id = :userId AND d.deleted = 0
        GROUP BY d.deck_id
        ORDER BY d.create_at DESC
    """)
    fun observeDecksWithStats(userId: String, today: String): Flow<List<DeckWithStats>>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND LOWER(w.status) != 'egg' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id AND w.deleted = 0
        WHERE d.deck_id = :deckId AND d.deleted = 0
        GROUP BY d.deck_id
        LIMIT 1
    """)
    fun observeDeckWithStatsById(deckId: String, today: String): Flow<DeckWithStats?>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND LOWER(w.status) != 'egg' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id AND w.deleted = 0
        WHERE d.deck_id = :deckId AND d.deleted = 0
        GROUP BY d.deck_id
        LIMIT 1
    """)
    suspend fun getDeckWithStatsById(deckId: String, today: String): DeckWithStats?

    @Transaction
    @Query("SELECT * FROM Deck WHERE deck_id = :deckId AND deleted = 0 LIMIT 1")
    fun observeRawDeckWithWords(deckId: String): Flow<DeckWithWords?>

    @Transaction
    @Query("SELECT * FROM Deck WHERE deck_id = :deckId AND deleted = 0 LIMIT 1")
    suspend fun getRawDeckWithWords(deckId: String): DeckWithWords?

    fun observeDeckWithWords(deckId: String): Flow<DeckWithWords?> =
        observeRawDeckWithWords(deckId).map { item ->
            item?.copy(words = item.words.filterNot { it.deleted })
        }

    suspend fun getDeckWithWords(deckId: String): DeckWithWords? =
        getRawDeckWithWords(deckId)?.let { item ->
            item.copy(words = item.words.filterNot { it.deleted })
        }

    @Query("""
        SELECT * FROM Deck
        WHERE user_id = :userId
          AND deleted = 0
          AND (name_deck LIKE '%' || :keyword || '%' ESCAPE '\'
               OR description LIKE '%' || :keyword || '%' ESCAPE '\')
        ORDER BY create_at DESC
    """)
    fun searchDecks(userId: String, keyword: String): Flow<List<Deck>>

    @Query("""
        SELECT d.*,
               COUNT(w.word_id) AS word_count,
               COUNT(CASE WHEN w.status IS NOT NULL AND LOWER(w.status) != 'egg' THEN 1 END) AS learned_count,
               COUNT(CASE WHEN w.next_review_at IS NOT NULL AND w.next_review_at <= :today THEN 1 END) AS due_count
        FROM Deck d
        LEFT JOIN Word w ON w.deck_id = d.deck_id AND w.deleted = 0
        WHERE d.user_id = :userId
          AND d.deleted = 0
          AND (d.name_deck LIKE '%' || :keyword || '%' ESCAPE '\'
               OR IFNULL(d.description, '') LIKE '%' || :keyword || '%' ESCAPE '\')
        GROUP BY d.deck_id
        ORDER BY d.create_at DESC
    """)
    fun observeSearchDecks(userId: String, keyword: String, today: String): Flow<List<DeckWithStats>>

    @Query("SELECT COUNT(*) FROM Deck WHERE deleted = 0")
    suspend fun count(): Int

    /** Đếm toàn bộ Tổ kể cả đã xoá mềm (giúp seeder không bơm lại dữ liệu mẫu sau khi user tự xoá hết Tổ). */
    @Query("SELECT COUNT(*) FROM Deck")
    suspend fun countAllIncludingDeleted(): Int

    @Query("SELECT COUNT(*) FROM Deck WHERE user_id = :userId AND deleted = 0")
    suspend fun countByUser(userId: String): Int

    @Query("""
        SELECT name_deck FROM Deck
        WHERE user_id = :userId
          AND deleted = 0
          AND (:excludeId IS NULL OR deck_id != :excludeId)
    """)
    suspend fun getDeckNames(userId: String, excludeId: String? = null): List<String>

    suspend fun existsByName(userId: String, name: String, excludeId: String? = null): Boolean {
        val target = name.trim()
        return getDeckNames(userId, excludeId).any { it.trim().equals(target, ignoreCase = true) }
    }

    @Query("""
        UPDATE Deck
        SET name_deck = :name,
            description = :description,
            icon = :icon,
            color = :color,
            updated_at = :updatedAt
        WHERE deck_id = :deckId AND deleted = 0
    """)
    suspend fun updateInfo(
        deckId: String,
        name: String,
        description: String?,
        icon: String,
        color: String,
        updatedAt: String
    )

    @Query("UPDATE Deck SET deleted = 1, updated_at = :updatedAt WHERE deck_id = :deckId AND deleted = 0")
    suspend fun softDeleteDeckOnly(deckId: String, updatedAt: String): Int

    @Query("UPDATE Word SET deleted = 1, updated_at = :updatedAt WHERE deck_id = :deckId AND deleted = 0")
    suspend fun softDeleteWordsByDeck(deckId: String, updatedAt: String): Int

    /** Xoá mềm Tổ và toàn bộ từ con trong cùng 1 transaction (tuân thủ hợp đồng Firestore FB3). */
    @Transaction
    suspend fun softDeleteWithWords(deckId: String, updatedAt: String): Int {
        softDeleteWordsByDeck(deckId, updatedAt)
        return softDeleteDeckOnly(deckId, updatedAt)
    }

    @Query("DELETE FROM Deck WHERE deck_id = :deckId")
    suspend fun deleteById(deckId: String)
}
