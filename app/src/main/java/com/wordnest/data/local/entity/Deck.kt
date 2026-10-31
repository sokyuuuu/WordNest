package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "Deck",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class Deck(
    @PrimaryKey
    @ColumnInfo(name = "deck_id")
    val deckId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "name_deck")
    val nameDeck: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "create_at")
    val createAt: String,

    @ColumnInfo(name = "icon")
    val icon: String = DeckStyle.DEFAULT_ICON,

    @ColumnInfo(name = "color")
    val color: String = DeckStyle.DEFAULT_COLOR
)
