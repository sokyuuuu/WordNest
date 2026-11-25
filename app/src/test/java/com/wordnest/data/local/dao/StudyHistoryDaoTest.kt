package com.wordnest.data.local.dao

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.StudyHistory
import com.wordnest.data.local.entity.Word
import com.wordnest.util.WordStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyHistoryDaoTest : BaseDbTest() {

    private lateinit var deckDao: DeckDao
    private lateinit var wordDao: WordDao
    private lateinit var historyDao: StudyHistoryDao

    @Before
    fun initDaos() = runTest {
        deckDao = db.deckDao()
        wordDao = db.wordDao()
        historyDao = db.studyHistoryDao()

        deckDao.insertAll(
            listOf(
                Deck(
                    deckId = "d-travel",
                    userId = testUserId,
                    nameDeck = "Du lịch",
                    createAt = "2026-04-20T08:00:00Z"
                ),
                Deck(
                    deckId = "d-ielts",
                    userId = testUserId,
                    nameDeck = "IELTS",
                    createAt = "2026-04-20T08:00:00Z"
                )
            )
        )
        wordDao.insertAll(
            listOf(
                Word(
                    wordId = "w1",
                    deckId = "d-travel",
                    term = "airport",
                    meaning = "sân bay",
                    status = WordStatus.EGG.storage,
                    createAt = "2026-04-20T08:00:00Z"
                ),
                Word(
                    wordId = "w2",
                    deckId = "d-travel",
                    term = "luggage",
                    meaning = "hành lý",
                    status = WordStatus.CHICK.storage,
                    createAt = "2026-04-20T08:00:00Z"
                ),
                Word(
                    wordId = "w3",
                    deckId = "d-ielts",
                    term = "ubiquitous",
                    meaning = "khắp nơi",
                    status = WordStatus.BIRD.storage,
                    createAt = "2026-04-20T08:00:00Z"
                )
            )
        )
    }

    @Test
    fun observeAccuracy_returnsZeroWhenEmpty() = runTest {
        historyDao.observeAccuracy().test {
            assertEquals(0.0, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAccuracy_3Correct1Wrong_returns075() = runTest {
        historyDao.insertAll(
            listOf(
                StudyHistory("h1", "w1", "correct", "flashcard", "2026-04-20T09:00:00Z"),
                StudyHistory("h2", "w2", "correct", "quiz", "2026-04-20T09:01:00Z"),
                StudyHistory("h3", "w3", "correct", "type_in", "2026-04-20T09:02:00Z"),
                StudyHistory("h4", "w1", "wrong", "quiz", "2026-04-20T09:03:00Z")
            )
        )

        historyDao.observeAccuracy().test {
            assertEquals(0.75, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeRecentActivity_ordersNewestFirstWithTermAndDeckName_andRespectsLimit() = runTest {
        historyDao.insertAll(
            listOf(
                StudyHistory("h1", "w1", "wrong", "flashcard", "2026-04-20T09:00:00Z"),
                StudyHistory("h2", "w3", "correct", "quiz", "2026-04-20T10:00:00Z"),
                StudyHistory("h3", "w2", "correct", "type_in", "2026-04-20T09:30:00Z")
            )
        )

        historyDao.observeRecentActivity(limit = 2).test {
            val items = awaitItem()
            assertEquals(2, items.size)

            assertEquals("ubiquitous", items[0].term)
            assertEquals("IELTS", items[0].deckName)
            assertEquals("correct", items[0].result)
            assertEquals("quiz", items[0].mode)
            assertEquals("2026-04-20T10:00:00Z", items[0].createdAt)

            assertEquals("luggage", items[1].term)
            assertEquals("Du lịch", items[1].deckName)
            assertEquals("type_in", items[1].mode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeWordsToReview_returnsDistinctWrongWordsOrderedByLatestWrongFirst() = runTest {
        historyDao.insertAll(
            listOf(
                // w1 sai ở 09:00 rồi lại sai ở 11:00 -> phải lên đầu và chỉ xuất hiện 1 lần
                StudyHistory("h1", "w1", "wrong", "flashcard", "2026-04-20T09:00:00Z"),
                // w2 sai ở 10:00 -> đứng thứ 2
                StudyHistory("h2", "w2", "wrong", "quiz", "2026-04-20T10:00:00Z"),
                // w3 chỉ đúng -> không có trong wordsToReview
                StudyHistory("h3", "w3", "correct", "type_in", "2026-04-20T10:30:00Z"),
                StudyHistory("h4", "w1", "wrong", "type_in", "2026-04-20T11:00:00Z")
            )
        )

        historyDao.observeWordsToReview(limit = 20).test {
            val words = awaitItem()
            assertEquals(listOf("w1", "w2"), words.map { it.wordId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun softDeletedWordOrDeck_excludedFromAllStudyHistoryQueries() = runTest {
        historyDao.insertAll(
            listOf(
                StudyHistory("h1", "w1", "wrong", "flashcard", "2026-04-20T09:00:00Z"),
                StudyHistory("h2", "w2", "wrong", "quiz", "2026-04-20T09:05:00Z"),
                StudyHistory("h3", "w3", "correct", "type_in", "2026-04-20T09:10:00Z")
            )
        )

        // Xoá mềm w1 -> chỉ còn w2 (wrong) + w3 (correct) => accuracy = 0.5
        wordDao.softDelete("w1", "2026-04-21T00:00:00Z")
        historyDao.observeAccuracy().test {
            assertEquals(0.5, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
        historyDao.observeWordsToReview().test {
            assertEquals(listOf("w2"), awaitItem().map { it.wordId })
            cancelAndIgnoreRemainingEvents()
        }

        // Xoá mềm cả Tổ d-travel -> chỉ còn w3 (correct) => accuracy = 1.0, wordsToReview rỗng
        deckDao.softDeleteWithWords("d-travel", "2026-04-21T01:00:00Z")
        historyDao.observeAccuracy().test {
            assertEquals(1.0, awaitItem(), 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
        historyDao.observeRecentActivity(limit = 10).test {
            val activities = awaitItem()
            assertEquals(1, activities.size)
            assertEquals("ubiquitous", activities.first().term)
            cancelAndIgnoreRemainingEvents()
        }
        historyDao.observeWordsToReview().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hardDeleteWord_cascadesDeleteStudyHistory() = runTest {
        historyDao.record(StudyHistory("h1", "w1", "correct", "flashcard", "2026-04-20T09:00:00Z"))
        assertEquals(1, historyDao.getByWordId("w1").size)

        wordDao.deleteById("w1")
        assertTrue(historyDao.getByWordId("w1").isEmpty())
        assertEquals(0, historyDao.countAll())
    }
}
