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
import com.wordnest.data.local.entity.Setting
import com.wordnest.data.local.entity.User
import kotlinx.coroutines.flow.Flow

data class UserWithSetting(
    @Embedded val user: User,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    val setting: Setting?
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Delete
    suspend fun delete(user: User)

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM User WHERE user_id = :userId LIMIT 1")
    suspend fun getById(userId: String): User?

    @Query("SELECT * FROM User WHERE user_id = :userId LIMIT 1")
    fun observeById(userId: String): Flow<User?>

    @Query("SELECT * FROM User WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM User ORDER BY create_at DESC")
    fun observeAll(): Flow<List<User>>

    @Transaction
    @Query("SELECT * FROM User WHERE user_id = :userId LIMIT 1")
    suspend fun getUserWithSetting(userId: String): UserWithSetting?

    @Transaction
    @Query("SELECT * FROM User WHERE user_id = :userId LIMIT 1")
    fun observeUserWithSetting(userId: String): Flow<UserWithSetting?>

    @Query("UPDATE User SET last_login_at = :timestamp WHERE user_id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: String)

    @Query("UPDATE User SET is_active = :active WHERE user_id = :userId")
    suspend fun setActive(userId: String, active: Boolean)

    @Query("DELETE FROM User WHERE user_id = :userId")
    suspend fun deleteById(userId: String)
}
