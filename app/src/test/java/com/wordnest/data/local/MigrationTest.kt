package com.wordnest.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.wordnest.data.local.entity.StudyHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "wordnest-migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java
    )

    @Test
    fun migrate2To3_preservesDecksWordsAndChildTables_lowercasesStatus_renamesLastReviewedAt_andCreatesStudyHistory() = runTest {
        // 1. Tạo DB v2 với dữ liệu thực (gồm User, Deck, Word chữ HOA + last_review_at, Study_Session/StudyItem, Quiz/QuizItem/QuizOption)
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO `User` (`user_id`, `email`, `password_hash`, `display_name`, `is_active`, `create_at`, `last_login_at`)
                VALUES ('u1', 'khang@wordnest.app', 'hash', 'Khang', 1, '2026-04-20T08:00:00Z', '2026-04-20T08:00:00Z')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `Deck` (`deck_id`, `user_id`, `name_deck`, `description`, `create_at`, `icon`, `color`)
                VALUES ('d1', 'u1', 'IELTS', 'Academic', '2026-04-20T08:00:00Z', 'globe', 'yellow')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `Word` (
                    `word_id`, `deck_id`, `term`, `meaning`, `phonetic`, `example`, `note`,
                    `status`, `correct_streak`, `wrong_count`, `interval_days`,
                    `next_review_at`, `last_review_at`, `create_at`
                ) VALUES
                    ('w1', 'd1', 'airport', 'sân bay', '/ˈeəpɔːt/', 'Go to the airport.', 'note-1', 'EGG', 0, 0, 0, '2026-04-20', NULL, '2026-04-20T08:01:00Z'),
                    ('w2', 'd1', 'computer', 'máy tính', '/kəmˈpjuːtər/', 'Use a computer.', NULL, 'CHICK', 3, 1, 3, '2026-04-23', '2026-04-20T09:00:00Z', '2026-04-20T08:02:00Z'),
                    ('w3', 'd1', 'clock', 'đồng hồ', '/klɒk/', NULL, NULL, 'BIRD', 7, 0, 7, '2026-04-27', '2026-04-20T10:00:00Z', '2026-04-20T08:03:00Z')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `study_session` (`session_id`, `deck_id`, `started_at`, `ended_at`, `known_count`, `unknown_count`)
                VALUES ('s1', 'd1', '2026-04-20T09:00:00Z', '2026-04-20T09:10:00Z', 2, 1)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `StudyItem` (`item_id`, `session_id`, `word_id`, `result`, `order_no`)
                VALUES ('si1', 's1', 'w2', 'KNOWN', 1)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `Quiz` (`quiz_id`, `deck_id`, `quiz_type`, `total_questions`, `correct_count`, `score`, `taken_at`)
                VALUES ('q1', 'd1', 'MC', 1, 1, 100.0, '2026-04-20T09:15:00Z')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `QuizItem` (`quiz_item_id`, `quiz_id`, `word_id`, `question_type`, `user_answer`, `is_correct`)
                VALUES ('qi1', 'q1', 'w3', 'MEANING', 'đồng hồ', 1)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `QuizOption` (`option_id`, `quiz_item_id`, `option_text`, `is_correct`, `order_no`)
                VALUES ('qo1', 'qi1', 'đồng hồ', 1, 1)
                """.trimIndent()
            )
            close()
        }

        // 2. Chạy MIGRATION_2_3 và xác thực schema v3 khớp 100% với app/schemas/.../3.json
        val migratedSqlite = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            DbMigrations.MIGRATION_2_3
        )

        // Kiểm tra không có vi phạm khóa ngoại sau migration
        migratedSqlite.query("PRAGMA foreign_key_check").use { cursor ->
            assertEquals("PRAGMA foreign_key_check must return 0 violations", 0, cursor.count)
        }
        // Kiểm tra bảng con tham chiếu Word vẫn còn nguyên bản ghi
        migratedSqlite.query("SELECT COUNT(*) FROM `StudyItem`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migratedSqlite.query("SELECT COUNT(*) FROM `QuizItem`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migratedSqlite.query("SELECT COUNT(*) FROM `QuizOption`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migratedSqlite.close()

        // 3. Mở lại DB bằng Room AppDatabase v3 và kiểm chứng dữ liệu qua DAO
        val context = ApplicationProvider.getApplicationContext<Context>()
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*DbMigrations.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val deck = roomDb.deckDao().getById("d1")
            assertNotNull(deck)
            assertEquals("IELTS", deck!!.nameDeck)
            assertEquals("Academic", deck.description)
            assertEquals(DbMigrations.DEFAULT_MIGRATED_UPDATED_AT, deck.updatedAt)
            assertFalse(deck.deleted)
            // updated_at phải parse được bằng Instant.parse theo hợp đồng Firestore
            Instant.parse(deck.updatedAt)

            val w1 = roomDb.wordDao().getById("w1")
            assertNotNull(w1)
            assertEquals("egg", w1!!.status)
            assertNull(w1.lastReviewedAt)
            assertEquals(DbMigrations.DEFAULT_MIGRATED_UPDATED_AT, w1.updatedAt)
            assertFalse(w1.deleted)
            assertEquals("note-1", w1.note)

            val w2 = roomDb.wordDao().getById("w2")
            assertNotNull(w2)
            assertEquals("chick", w2!!.status)
            assertEquals("2026-04-20T09:00:00Z", w2.lastReviewedAt)
            assertEquals(3, w2.correctStreak)
            assertEquals(1, w2.wrongCount)
            assertFalse(w2.deleted)

            val w3 = roomDb.wordDao().getById("w3")
            assertNotNull(w3)
            assertEquals("bird", w3!!.status)
            assertEquals("2026-04-20T10:00:00Z", w3.lastReviewedAt)
            assertEquals(7, w3.correctStreak)
            assertFalse(w3.deleted)

            // Bảng study_history hoạt động bình thường trên DB vừa migrate
            roomDb.studyHistoryDao().record(
                StudyHistory(
                    entryId = "hist-1",
                    wordId = "w2",
                    result = "correct",
                    mode = "flashcard",
                    createdAt = "2026-04-21T10:00:00Z"
                )
            )
            assertEquals(1, roomDb.studyHistoryDao().getByWordId("w2").size)
        } finally {
            roomDb.close()
        }
    }
}
