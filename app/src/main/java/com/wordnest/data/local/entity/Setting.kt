package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "setting",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"], unique = true)]
)
data class Setting(
    @PrimaryKey
    @ColumnInfo(name = "setting_id")
    val settingId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "reminder_enable")
    val reminderEnable: Boolean = false,

    @ColumnInfo(name = "reminder_time")
    val reminderTime: String? = "20:00",

    @ColumnInfo(name = "dark_mode")
    val darkMode: Boolean = false,

    @ColumnInfo(name = "tts_enable")
    val ttsEnable: Boolean = true,

    @ColumnInfo(name = "auto_suggest_enabled")
    val autoSuggestEnabled: Boolean = true
)
