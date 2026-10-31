package com.wordnest.data.repository

import com.wordnest.data.local.dao.SettingDao
import com.wordnest.data.local.dao.UserDao
import com.wordnest.data.local.dao.UserWithSetting
import com.wordnest.data.local.entity.Setting
import com.wordnest.data.local.entity.User
import com.wordnest.util.Constants
import com.wordnest.util.DateTimeUtil
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(userId: String = Constants.DEFAULT_USER_ID): Flow<User?>
    fun observeUserWithSetting(userId: String = Constants.DEFAULT_USER_ID): Flow<UserWithSetting?>
    suspend fun getUserById(userId: String): User?
    suspend fun ensureDefaultUser(): User

    companion object {
        const val DEFAULT_USER_ID = Constants.DEFAULT_USER_ID
    }
}

class DefaultUserRepository(
    private val userDao: UserDao,
    private val settingDao: SettingDao,
    private val nowProvider: () -> String = DateTimeUtil::nowIso
) : UserRepository {

    override fun observeCurrentUser(userId: String): Flow<User?> =
        userDao.observeById(userId)

    override fun observeUserWithSetting(userId: String): Flow<UserWithSetting?> =
        userDao.observeUserWithSetting(userId)

    override suspend fun getUserById(userId: String): User? =
        userDao.getById(userId)

    override suspend fun ensureDefaultUser(): User {
        val existing = userDao.getById(Constants.DEFAULT_USER_ID)
        if (existing != null) return existing

        val defaultUser = User(
            userId = Constants.DEFAULT_USER_ID,
            email = "learner@wordnest.app",
            passwordHash = "hashed_default_password",
            displayName = "WordNest Learner",
            isActive = true,
            createAt = nowProvider(),
            lastLoginAt = nowProvider()
        )
        userDao.insert(defaultUser)

        val defaultSetting = Setting(
            userId = Constants.DEFAULT_USER_ID,
            reminderEnable = true,
            reminderTime = "20:00",
            darkMode = false,
            ttsEnable = true,
            autoSuggestEnabled = true
        )
        settingDao.upsert(defaultSetting)
        return defaultUser
    }
}
