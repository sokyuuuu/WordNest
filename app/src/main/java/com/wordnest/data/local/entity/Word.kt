package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "Word",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["deck_id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deck_id"]),
        Index(value = ["term"])
    ]
)
data class Word(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "deck_id")
    val deckId: String,

    @ColumnInfo(name = "term")
    val term: String,

    @ColumnInfo(name = "meaning")
    val meaning: String,

    @ColumnInfo(name = "phonetic")
    val phonetic: String? = null,

    @ColumnInfo(name = "example")
    val example: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "status")
    val status: String = WordStatus.EGG,

    @ColumnInfo(name = "correct_streak")
    val correctStreak: Int = 0,

    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int = 0,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: String? = null,

    @ColumnInfo(name = "last_review_at")
    val lastReviewAt: String? = null,

    @ColumnInfo(name = "create_at")
    val createAt: String
)
