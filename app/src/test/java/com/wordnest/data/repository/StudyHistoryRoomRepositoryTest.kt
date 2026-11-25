package com.wordnest.data.repository

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.util.WordStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class StudyHistoryRoomRepositoryTest : BaseDbTest() {

    private lateinit var repo: StudyHistoryRoomRepository

    @Before
    fun initRepo() = runTest {
        repo = StudyHistoryRoomRepository(db.studyHistoryDao())
        db.deckDao().insert(
            Deck(
                deckId = "d1",
                userId = testUserId,
                nameDeck = "Từ vựng cơ bản",
                createAt = "2026-04-20T08:00:00Z"
            )
        )
        db.wordDao().insertAll(
            listOf(
                Word("w1", "d1", "airport", "Sân bay", status = WordStatus.EGG.storage, createAt = "2026-04-20T08:00:00Z"),
                Word("w2", "d1", "apple", "Quả táo", status = WordStatus.EGG.storage, createAt = "2026-04-20T08:00:00Z"),
                Word("w3", "d1", "book", "Cuốn sách", status = WordStatus.CHICK.storage, createAt = "2026-04-20T08:00:00Z")
            )
        )
    }

    @Test
    fun observeAccuracy_emptyReturnsZero_andComputesCorrectRatio() = runTest {
        repo.observeAccuracy().test {
            assertEquals(0.0, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }

        repo.record("w1", HistoryResult.CORRECT, HistoryMode.FLASHCARD, Instant.parse("2026-04-20T09:00:00Z"))
        repo.record("w2", HistoryResult.CORRECT, HistoryMode.QUIZ, Instant.parse("2026-04-20T09:01:00Z"))
        repo.record("w3", HistoryResult.CORRECT, HistoryMode.TYPE_IN, Instant.parse("2026-04-20T09:02:00Z"))
        repo.record("w1", HistoryResult.WRONG, HistoryMode.QUIZ, Instant.parse("2026-04-20T09:03:00Z"))

        repo.observeAccuracy().test {
            assertEquals(0.75, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeRecentActivity_andObserveWordsToReview_followContract() = runTest {
        repo.record("w1", HistoryResult.WRONG, HistoryMode.FLASHCARD, Instant.parse("2026-04-20T09:00:00Z"))
        repo.record("w2", HistoryResult.WRONG, HistoryMode.QUIZ, Instant.parse("2026-04-20T09:05:00Z"))
        repo.record("w1", HistoryResult.WRONG, HistoryMode.TYPE_IN, Instant.parse("2026-04-20T09:10:00Z"))
        repo.record("w3", HistoryResult.CORRECT, HistoryMode.QUIZ, Instant.parse("2026-04-20T09:15:00Z"))

        repo.observeRecentActivity(limit = 2).test {
            val recent = awaitItem()
            assertEquals(2, recent.size)
            assertEquals("book", recent[0].term)
            assertEquals("Từ vựng cơ bản", recent[0].deckName)
            assertEquals("correct", recent[0].result)
            assertEquals("quiz", recent[0].mode)
            assertEquals("airport", recent[1].term)
            assertEquals("type_in", recent[1].mode)
            cancelAndIgnoreRemainingEvents()
        }

        repo.observeWordsToReview(limit = 10).test {
            val review = awaitItem()
            assertEquals(listOf("w1", "w2"), review.map { it.wordId })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
