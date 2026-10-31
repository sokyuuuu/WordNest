package com.wordnest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Admin",
    indices = [Index(value = ["email"], unique = true)]
)
data class Admin(
    @PrimaryKey
    @ColumnInfo(name = "admin_id")
    val adminId: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "create_at")
    val createAt: String,

    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: String? = null
)
