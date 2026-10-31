package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_session",
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
data class Study_Session(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "deck_id")
    val deckId: String,

    @ColumnInfo(name = "started_at")
    val startedAt: String,

    @ColumnInfo(name = "ended_at")
    val endedAt: String? = null,

    @ColumnInfo(name = "known_count")
    val knownCount: Int = 0,

    @ColumnInfo(name = "unknown_count")
    val unknownCount: Int = 0
)
