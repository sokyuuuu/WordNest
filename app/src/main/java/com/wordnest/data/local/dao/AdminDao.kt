package com.wordnest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordnest.data.local.entity.Admin
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(admin: Admin): Long

    @Delete
    suspend fun delete(admin: Admin)

    @Update
    suspend fun update(admin: Admin)

    @Query("SELECT * FROM Admin WHERE admin_id = :id")
    suspend fun getById(id: String): Admin?

    @Query("SELECT * FROM Admin WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): Admin?

    @Query("SELECT * FROM Admin ORDER BY create_at DESC")
    fun observeAll(): Flow<List<Admin>>

    @Query("SELECT COUNT(*) FROM Admin WHERE email = :email AND password_hash = :hash AND is_active = 1")
    suspend fun authenticate(email: String, hash: String): Int

    @Query("UPDATE Admin SET last_login_at = :time WHERE admin_id = :id")
    suspend fun updateLastLogin(id: String, time: String)

    @Query("UPDATE Admin SET is_active = :active WHERE admin_id = :id")
    suspend fun setActive(id: String, active: Boolean)

    @Query("DELETE FROM Admin WHERE admin_id = :id")
    suspend fun deleteById(id: String)
}
