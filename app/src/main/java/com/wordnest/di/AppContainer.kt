package com.wordnest.di

import android.app.Application
import android.content.Context
import com.wordnest.WordNestApplication
import com.wordnest.data.StudyHistoryRepository
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.OnlineDictionaryRepository
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.DefaultUserRepository
import com.wordnest.data.repository.StudyHistoryRoomRepository
import com.wordnest.data.repository.UserRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.data.seed.SampleDataSeeder
import com.wordnest.data.study.RoomStudyWordSource
import com.wordnest.data.study.StudyWordSource
import com.wordnest.util.AndroidSpeaker
import com.wordnest.util.Speaker

/**
 * Container DI thủ công (không dùng Hilt): giữ 1 instance [AppDatabase] duy nhất
 * cùng các Repository, nguồn từ Room cho luồng học và [SampleDataSeeder].
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val userRepository: UserRepository by lazy {
        DefaultUserRepository(database.userDao(), database.settingDao())
    }

    val deckRepository: DeckRepository by lazy {
        DeckRepository(database.deckDao())
    }

    val wordRepository: WordRepository by lazy {
        WordRepository(database.wordDao(), database.deckDao())
    }

    val studyHistoryRoomRepository: StudyHistoryRoomRepository by lazy {
        StudyHistoryRoomRepository(database.studyHistoryDao())
    }

    /** Lịch sử ngày học (DataStore) phục vụ bộ đếm "N ngày ấp" (F15). */
    val studyDayRepository: StudyHistoryRepository by lazy {
        StudyHistoryRepository(appContext)
    }

    val roomStudyWordSource: StudyWordSource by lazy {
        RoomStudyWordSource(database.wordDao())
    }

    val seeder: SampleDataSeeder by lazy {
        SampleDataSeeder(database)
    }

    /** Bộ phát âm tiếng Anh dùng chung (khởi tạo lười TextToSpeech — F19). */
    val speaker: Speaker by lazy {
        AndroidSpeaker(appContext)
    }

    /** Tra cứu từ điển (IPA, câu ví dụ) & dịch nghĩa tiếng Việt kiểu Quizlet (F04, F05). */
    val dictionaryRepository: DictionaryRepository by lazy {
        OnlineDictionaryRepository()
    }
}

/**
 * Lấy [AppContainer] dùng chung từ [WordNestApplication].
 * Khi chạy trong môi trường test không khởi tạo [WordNestApplication], tự tạo container theo context.
 */
fun Application.appContainer(): AppContainer =
    (this as? WordNestApplication)?.container ?: AppContainer(this)
