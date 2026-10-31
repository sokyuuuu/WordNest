package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "QuizOption",
    foreignKeys = [
        ForeignKey(
            entity = QuizItem::class,
            parentColumns = ["quiz_item_id"],
            childColumns = ["quiz_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quiz_item_id"])]
)
data class QuizOption(
    @PrimaryKey
    @ColumnInfo(name = "option_id")
    val optionId: String,

    @ColumnInfo(name = "quiz_item_id")
    val quizItemId: String,

    @ColumnInfo(name = "option_text")
    val optionText: String,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean = false,

    @ColumnInfo(name = "order_no")
    val orderNo: Int
)
