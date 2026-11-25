package com.wordnest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.local.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before

/**
 * Base test class khởi tạo Room in-memory database (chạy trên JVM nhờ Robolectric)
 * và seed sẵn 1 User mặc định để thỏa mãn ràng buộc khóa ngoại (FK User -> Deck -> Word).
 */
abstract class BaseDbTest {

    protected lateinit var db: AppDatabase
    protected val testUserId = "test-user-1"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            db.userDao().insert(
                User(
                    userId = testUserId,
                    email = "tester@wordnest.app",
                    passwordHash = "hash",
                    displayName = "Tester",
                    isActive = true,
                    createAt = "2026-04-20T00:00:00"
                )
            )
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    /**
     * Chờ theo thời gian THẬT (không dùng thời gian ảo của runTest) vì Room phát Flow
     * và thực thi truy vấn trên executor nền thật.
     */
    protected suspend fun <T> awaitReal(timeoutMs: Long = 5_000, block: suspend () -> T): T =
        withContext(Dispatchers.Default) { withTimeout(timeoutMs) { block() } }

    /** Thăm dò điều kiện tới khi đúng (thời gian thật), dùng cho các thao tác ghi bất đồng bộ. */
    protected suspend fun awaitCondition(timeoutMs: Long = 5_000, condition: suspend () -> Boolean) {
        awaitReal(timeoutMs) {
            while (!condition()) delay(20)
        }
    }
}
