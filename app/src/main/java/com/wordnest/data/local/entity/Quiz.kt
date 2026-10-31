package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Quiz",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["deck_id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deck_id"])]
)
data class Quiz(
    @PrimaryKey
    @ColumnInfo(name = "quiz_id")
    val quizId: String,

    @ColumnInfo(name = "deck_id")
    val deckId: String,

    @ColumnInfo(name = "quiz_type")
    val quizType: String,

    @ColumnInfo(name = "total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int = 0,

    @ColumnInfo(name = "score")
    val score: Double = 0.0,

    @ColumnInfo(name = "taken_at")
    val takenAt: String
)
