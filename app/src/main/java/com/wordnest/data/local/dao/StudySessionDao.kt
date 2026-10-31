package com.wordnest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.wordnest.data.local.entity.StudyItem
import com.wordnest.data.local.entity.Study_Session
import kotlinx.coroutines.flow.Flow

data class StudySessionWithItems(
    @Embedded val session: Study_Session,
    @Relation(
        parentColumn = "session_id",
        entityColumn = "session_id"
    )
    val items: List<StudyItem>
)

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: Study_Session): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: StudyItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<StudyItem>)

    @Delete
    suspend fun deleteSession(session: Study_Session)

    @Update
    suspend fun updateSession(session: Study_Session)

    @Update
    suspend fun updateItem(item: StudyItem)

    @Query("SELECT * FROM study_session WHERE session_id = :id")
    suspend fun getSessionById(id: String): Study_Session?

    @Transaction
    @Query("SELECT * FROM study_session WHERE session_id = :id")
    suspend fun getSessionWithItems(id: String): StudySessionWithItems?

    @Query("SELECT * FROM study_session WHERE deck_id = :deckId ORDER BY started_at DESC")
    fun observeByDeck(deckId: String): Flow<List<Study_Session>>

    @Query("SELECT * FROM StudyItem WHERE session_id = :sessionId ORDER BY order_no ASC")
    suspend fun getItemsBySession(sessionId: String): List<StudyItem>

    @Query("SELECT COUNT(*) FROM StudyItem WHERE session_id = :sessionId AND result = :result")
    suspend fun countByResult(sessionId: String, result: String): Int

    @Query("""
        UPDATE study_session
        SET ended_at = :endedAt, known_count = :known, unknown_count = :unknown
        WHERE session_id = :id
    """)
    suspend fun finishSession(id: String, endedAt: String, known: Int, unknown: Int)

    @Query("DELETE FROM study_session WHERE session_id = :id")
    suspend fun deleteSessionById(id: String)
}
