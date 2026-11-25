package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "study_history",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["word_id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["word_id"]),
        Index(value = ["created_at"])
    ]
)
data class StudyHistory(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    val entryId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "word_id")
    val wordId: String,

    @ColumnInfo(name = "result")
    val result: String,

    @ColumnInfo(name = "mode")
    val mode: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
