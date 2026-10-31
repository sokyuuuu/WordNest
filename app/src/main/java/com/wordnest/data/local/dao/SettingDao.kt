package com.wordnest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordnest.data.local.entity.Setting
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: Setting): Long

    @Delete
    suspend fun delete(setting: Setting)

    @Update
    suspend fun update(setting: Setting)

    @Query("SELECT * FROM setting WHERE user_id = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): Setting?

    @Query("SELECT * FROM setting WHERE user_id = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<Setting?>

    @Query("UPDATE setting SET dark_mode = :enabled WHERE user_id = :userId")
    suspend fun setDarkMode(userId: String, enabled: Boolean)

    @Query("UPDATE setting SET tts_enable = :enabled WHERE user_id = :userId")
    suspend fun setTts(userId: String, enabled: Boolean)

    @Query("UPDATE setting SET auto_suggest_enabled = :enabled WHERE user_id = :userId")
    suspend fun setAutoSuggest(userId: String, enabled: Boolean)

    @Query("UPDATE setting SET reminder_enable = :enabled, reminder_time = :time WHERE user_id = :userId")
    suspend fun setReminder(userId: String, enabled: Boolean, time: String?)

    @Query("DELETE FROM setting WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
