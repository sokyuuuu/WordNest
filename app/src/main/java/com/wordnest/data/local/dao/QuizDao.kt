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
import com.wordnest.data.local.entity.Quiz
import com.wordnest.data.local.entity.QuizItem
import com.wordnest.data.local.entity.QuizOption
import com.wordnest.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

data class QuizItemWithOptions(
    @Embedded val quizItem: QuizItem,
    @Relation(
        parentColumn = "quiz_item_id",
        entityColumn = "quiz_item_id"
    )
    val options: List<QuizOption>
)

data class QuizWithItems(
    @Embedded val quiz: Quiz,
    @Relation(
        entity = QuizItem::class,
        parentColumn = "quiz_id",
        entityColumn = "quiz_id"
    )
    val items: List<QuizItemWithOptions>
)

data class QuizItemWithWord(
    @Embedded val quizItem: QuizItem,
    @Relation(
        parentColumn = "word_id",
        entityColumn = "word_id"
    )
    val word: Word
)

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuiz(quiz: Quiz): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: QuizItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<QuizItem>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOption(option: QuizOption): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOptions(options: List<QuizOption>)

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Update
    suspend fun updateItem(item: QuizItem)

    @Transaction
    suspend fun insertFullQuiz(
        quiz: Quiz,
        items: List<QuizItem>,
        options: List<QuizOption>
    ) {
        insertQuiz(quiz)
        insertItems(items)
        insertOptions(options)
    }

    @Query("SELECT * FROM Quiz WHERE quiz_id = :id")
    suspend fun getQuizById(id: String): Quiz?

    @Transaction
    @Query("SELECT * FROM Quiz WHERE quiz_id = :id")
    suspend fun getQuizWithItems(id: String): QuizWithItems?

    @Query("SELECT * FROM Quiz WHERE deck_id = :deckId ORDER BY taken_at DESC")
    fun observeByDeck(deckId: String): Flow<List<Quiz>>

    @Query("SELECT * FROM Quiz WHERE deck_id = :deckId ORDER BY taken_at DESC LIMIT 1")
    suspend fun getLatestByDeck(deckId: String): Quiz?

    @Query("SELECT * FROM QuizItem WHERE quiz_id = :quizId")
    suspend fun getItemsByQuiz(quizId: String): List<QuizItem>

    @Transaction
    @Query("SELECT * FROM QuizItem WHERE quiz_id = :quizId")
    suspend fun getItemsWithWord(quizId: String): List<QuizItemWithWord>

    @Query("SELECT COUNT(*) FROM QuizItem WHERE quiz_id = :quizId AND is_correct = 1")
    suspend fun countCorrect(quizId: String): Int

    @Query("SELECT * FROM QuizOption WHERE quiz_item_id = :quizItemId ORDER BY order_no ASC")
    suspend fun getOptionsByItem(quizItemId: String): List<QuizOption>

    @Query("UPDATE Quiz SET total_questions = :total, correct_count = :correct, score = :score WHERE quiz_id = :id")
    suspend fun updateResult(id: String, total: Int, correct: Int, score: Double)

    @Query("DELETE FROM Quiz WHERE quiz_id = :id")
    suspend fun deleteQuizById(id: String)

    @Query("UPDATE QuizItem SET user_answer = :answer, is_correct = :isCorrect WHERE quiz_item_id = :id")
    suspend fun submitAnswer(id: String, answer: String?, isCorrect: Boolean)
}
