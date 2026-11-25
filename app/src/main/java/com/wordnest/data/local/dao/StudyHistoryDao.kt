package com.wordnest.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordnest.data.local.entity.StudyHistory
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

data class HistoryActivity(
    @ColumnInfo(name = "term") val term: String,
    @ColumnInfo(name = "deck_name") val deckName: String,
    @ColumnInfo(name = "result") val result: String,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "created_at") val createdAt: String
)

@Dao
interface StudyHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entry: StudyHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StudyHistory>)

    /**
     * Độ chính xác: correct / (correct + wrong) trên các từ/Tổ chưa bị xoá mềm.
     * Trả về 0.0 nếu chưa có dữ liệu.
     */
    @Query("""
        SELECT COALESCE(
            CAST(SUM(CASE WHEN sh.result = 'correct' THEN 1 ELSE 0 END) AS REAL)
            / NULLIF(SUM(CASE WHEN sh.result IN ('correct', 'wrong') THEN 1 ELSE 0 END), 0),
            0.0
        )
        FROM study_history sh
        INNER JOIN Word w ON w.word_id = sh.word_id
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE w.deleted = 0 AND d.deleted = 0
    """)
    fun observeAccuracy(): Flow<Double>

    /**
     * Hoạt động gần đây (mới nhất trước) kèm tên từ và tên Tổ, bỏ qua từ/Tổ đã xoá mềm.
     */
    @Query("""
        SELECT w.term AS term,
               d.name_deck AS deck_name,
               sh.result AS result,
               sh.mode AS mode,
               sh.created_at AS created_at
        FROM study_history sh
        INNER JOIN Word w ON w.word_id = sh.word_id
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        WHERE w.deleted = 0 AND d.deleted = 0
        ORDER BY sh.created_at DESC, sh.entry_id DESC
        LIMIT :limit
    """)
    fun observeRecentActivity(limit: Int = 5): Flow<List<HistoryActivity>>

    /**
     * Danh sách từ từng làm sai gần nhất (distinct theo word_id, lần sai mới nhất lên trước),
     * bỏ qua từ/Tổ đã xoá mềm.
     */
    @Query("""
        SELECT w.*
        FROM Word w
        INNER JOIN Deck d ON d.deck_id = w.deck_id
        INNER JOIN (
            SELECT word_id, MAX(created_at) AS last_wrong_at
            FROM study_history
            WHERE result = 'wrong'
            GROUP BY word_id
        ) wrong_words ON wrong_words.word_id = w.word_id
        WHERE w.deleted = 0 AND d.deleted = 0
        ORDER BY wrong_words.last_wrong_at DESC
        LIMIT :limit
    """)
    fun observeWordsToReview(limit: Int = 20): Flow<List<Word>>

    @Query("SELECT * FROM study_history WHERE word_id = :wordId ORDER BY created_at DESC")
    suspend fun getByWordId(wordId: String): List<StudyHistory>

    @Query("SELECT COUNT(*) FROM study_history")
    suspend fun countAll(): Int
}
