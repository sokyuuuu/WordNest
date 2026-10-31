package com.wordnest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wordnest.data.local.dao.AdminDao
import com.wordnest.data.local.dao.DeckDao
import com.wordnest.data.local.dao.QuizDao
import com.wordnest.data.local.dao.SettingDao
import com.wordnest.data.local.dao.StudySessionDao
import com.wordnest.data.local.dao.UserDao
import com.wordnest.data.local.dao.WordDao
import com.wordnest.data.local.entity.Admin
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Quiz
import com.wordnest.data.local.entity.QuizItem
import com.wordnest.data.local.entity.QuizOption
import com.wordnest.data.local.entity.Setting
import com.wordnest.data.local.entity.StudyItem
import com.wordnest.data.local.entity.Study_Session
import com.wordnest.data.local.entity.User
import com.wordnest.data.local.entity.Word

@Database(
    entities = [
        Admin::class,
        User::class,
        Deck::class,
        Setting::class,
        Word::class,
        Study_Session::class,
        StudyItem::class,
        Quiz::class,
        QuizItem::class,
        QuizOption::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adminDao(): AdminDao
    abstract fun userDao(): UserDao
    abstract fun deckDao(): DeckDao
    abstract fun settingDao(): SettingDao
    abstract fun wordDao(): WordDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun quizDao(): QuizDao

    companion object {
        private const val DATABASE_NAME = "wordnest.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
