package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "QuizItem",
    foreignKeys = [
        ForeignKey(
            entity = Quiz::class,
            parentColumns = ["quiz_id"],
            childColumns = ["quiz_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["word_id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quiz_id"]),
        Index(value = ["word_id"])
    ]
)
data class QuizItem(
    @PrimaryKey
    @ColumnInfo(name = "quiz_item_id")
    val quizItemId: String,

    @ColumnInfo(name = "quiz_id")
    val quizId: String,

    @ColumnInfo(name = "word_id")
    val wordId: String,

    @ColumnInfo(name = "question_type")
    val questionType: String,

    @ColumnInfo(name = "user_answer")
    val userAnswer: String? = null,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean = false
)
