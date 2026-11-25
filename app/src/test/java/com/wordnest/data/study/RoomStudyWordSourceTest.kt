package com.wordnest.data.study

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.util.WordStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomStudyWordSourceTest : BaseDbTest() {

    private var fakeNow = "2026-04-20T12:00:00Z"
    private lateinit var source: RoomStudyWordSource

    @Before
    fun initSource() = runTest {
        fakeNow = "2026-04-20T12:00:00Z"
        source = RoomStudyWordSource(
            wordDao = db.wordDao(),
            userId = testUserId,
            nowProvider = { fakeNow },
            zoneId = ZoneOffset.UTC
        )
        db.deckDao().insertAll(
            listOf(
                Deck("d1", testUserId, "Cơ bản", createAt = "2026-04-20T08:00:00Z"),
                Deck("d2-deleted", testUserId, "Đã xoá", createAt = "2026-04-20T08:01:00Z", deleted = true)
            )
        )
    }

    @Test
    fun wordsFlow_mapsAllFieldsAndExcludesSoftDeletedWordsAndDecks() = runTest {
        db.wordDao().insertAll(
            listOf(
                Word(
                    wordId = "w1",
                    deckId = "d1",
                    term = "airport",
                    meaning = "Sân bay",
                    phonetic = "/ˈeəpɔːt/",
                    example = "I go to the airport by bus.",
                    status = WordStatus.CHICK.storage,
                    lastReviewedAt = "2026-04-19T10:00:00Z",
                    createAt = "2026-04-20T08:00:00Z",
                    updatedAt = "2026-04-20T08:05:00Z"
                ),
                Word(
                    wordId = "w2-deleted",
                    deckId = "d1",
                    term = "apple",
                    meaning = "Quả táo",
                    createAt = "2026-04-20T08:01:00Z",
                    deleted = true
                ),
                Word(
                    wordId = "w3-in-deleted-deck",
                    deckId = "d2-deleted",
                    term = "book",
                    meaning = "Cuốn sách",
                    createAt = "2026-04-20T08:02:00Z"
                )
            )
        )

        source.words().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            val w = items.first()
            assertEquals("w1", w.id)
            assertEquals("airport", w.term)
            assertEquals("Sân bay", w.meaning)
            assertEquals("/ˈeəpɔːt/", w.phonetic)
            assertEquals("I go to the airport by bus.", w.example)
            assertEquals("d1", w.deckId)
            assertEquals("2026-04-20T08:05:00Z", w.updatedAt)
            assertEquals(WordStatus.CHICK, w.status)
            assertEquals(LocalDate.of(2026, 4, 19), w.lastReviewedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_persistsLowercaseStatusIsoUtcLastReviewedAtAndUpdatedAt_withoutTouchingOtherFields() = runTest {
        db.wordDao().insert(
            Word(
                wordId = "w1",
                deckId = "d1",
                term = "airport",
                meaning = "Sân bay",
                example = "I go to the airport by bus.",
                note = "Giữ nguyên ghi chú",
                status = WordStatus.EGG.storage,
                correctStreak = 2,
                createAt = "2026-04-20T08:00:00Z",
                updatedAt = "2026-04-20T08:00:00Z"
            )
        )

        fakeNow = "2026-04-21T16:45:00Z"
        val reviewDate = LocalDate.of(2026, 4, 21)
        source.save(
            StudyWord(
                id = "w1",
                term = "airport",
                meaning = "Sân bay",
                status = WordStatus.BIRD,
                lastReviewedAt = reviewDate,
                deckId = "d1"
            )
        )

        val stored = db.wordDao().getById("w1")
        assertNotNull(stored)
        assertEquals("bird", stored!!.status)
        assertEquals("2026-04-21T16:45:00Z", stored.updatedAt)
        assertNotNull(stored.lastReviewedAt)
        // Hợp đồng Firestore: last_reviewed_at phải parse được bằng Instant.parse
        Instant.parse(stored.lastReviewedAt)
        // Các cột không thuộc trạng thái học phải còn nguyên
        assertEquals("I go to the airport by bus.", stored.example)
        assertEquals("Giữ nguyên ghi chú", stored.note)
        assertEquals(2, stored.correctStreak)

        // Đọc ngược qua Flow trả đúng LocalDate và WordStatus
        source.words().test {
            val reloaded = awaitItem().first()
            assertEquals(WordStatus.BIRD, reloaded.status)
            assertEquals(reviewDate, reloaded.lastReviewedAt)
            assertEquals("2026-04-21T16:45:00Z", reloaded.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun wordsFlow_emitsUpdatesWhenWordIsAddedOrSoftDeleted() = runTest {
        source.words().test {
            assertTrue(awaitItem().isEmpty())

            db.wordDao().insert(
                Word("w1", "d1", "clock", "Đồng hồ", createAt = "2026-04-20T08:00:00Z")
            )
            assertEquals(listOf("w1"), awaitItem().map { it.id })

            db.wordDao().softDelete("w1", "2026-04-20T09:00:00Z")
            assertTrue(awaitItem().isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
