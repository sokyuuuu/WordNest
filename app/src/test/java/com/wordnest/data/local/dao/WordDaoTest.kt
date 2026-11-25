package com.wordnest.data.local.dao

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.Word
import com.wordnest.util.Validation
import com.wordnest.util.WordStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordDaoTest : BaseDbTest() {

    private lateinit var deckDao: DeckDao
    private lateinit var wordDao: WordDao
    private val deckId = "deck-test"

    @Before
    fun initDaos() = runTest {
        deckDao = db.deckDao()
        wordDao = db.wordDao()
        deckDao.insert(
            Deck(
                deckId = deckId,
                userId = testUserId,
                nameDeck = "Test Deck",
                description = null,
                createAt = "2026-04-20T10:00:00Z"
            )
        )
    }

    private fun newWord(
        id: String = "w-1",
        term: String = "algorithm",
        meaning: String = "thuật toán",
        example: String? = "Sorting algorithm",
        note: String? = "Thuật ngữ cơ bản",
        status: String = WordStatus.EGG.storage,
        nextReviewAt: String? = "2026-04-20",
        deleted: Boolean = false
    ) = Word(
        wordId = id,
        deckId = deckId,
        term = term,
        meaning = meaning,
        phonetic = "/ˈælɡərɪðəm/",
        example = example,
        note = note,
        status = status,
        nextReviewAt = nextReviewAt,
        createAt = "2026-04-20T10:00:00Z",
        deleted = deleted
    )

    @Test
    fun insertUpdateDelete_wordLifecycleWithNote() = runTest {
        val word = newWord(note = "Ghi chú ban đầu")
        wordDao.insert(word)

        val fetched = wordDao.getById("w-1")
        assertNotNull(fetched)
        assertEquals("algorithm", fetched?.term)
        assertEquals("Ghi chú ban đầu", fetched?.note)
        assertEquals("2026-04-20T10:00:00Z", fetched?.updatedAt)
        assertFalse(fetched!!.deleted)

        val updated = fetched.copy(
            meaning = "thuật giải",
            note = "Ghi chú mới",
            updatedAt = "2026-04-21T10:00:00Z"
        )
        wordDao.update(updated)
        val afterUpdate = wordDao.getById("w-1")
        assertEquals("thuật giải", afterUpdate?.meaning)
        assertEquals("Ghi chú mới", afterUpdate?.note)
        assertEquals("2026-04-21T10:00:00Z", afterUpdate?.updatedAt)

        wordDao.deleteById("w-1")
        assertNull(wordDao.getByIdIncludingDeleted("w-1"))
    }

    @Test
    fun softDelete_hidesWordFromAllDefaultQueries_andAllowsReusingTerm() = runTest {
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "algorithm", meaning = "thuật toán", example = "Sorting algorithm", status = WordStatus.CHICK.storage),
                newWord(id = "w2", term = "latency", meaning = "độ trễ", example = "Low latency is critical", status = WordStatus.EGG.storage)
            )
        )

        val deletedAt = "2026-04-22T15:00:00Z"
        assertEquals(1, wordDao.softDelete("w1", deletedAt))

        assertNull(wordDao.getById("w1"))
        assertEquals(1, wordDao.getWordsByDeck(deckId).size)
        assertEquals(1, wordDao.countByDeck(deckId))
        assertEquals(0, wordDao.countByDeckAndStatus(deckId, WordStatus.CHICK.storage))
        assertEquals(1, wordDao.getDueWordsByDeck(deckId, "2026-04-20").size)
        assertEquals(1, wordDao.getDueWordsByUser(testUserId, "2026-04-20").size)
        assertFalse(wordDao.existsByTerm(deckId, "algorithm"))

        wordDao.observeSearchInDeck(deckId, "algorithm").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeSearchAllWords(testUserId, "algorithm").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeTotalWordCountByUser(testUserId).test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeCountByUserAndStatus(testUserId, WordStatus.CHICK.storage).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeDueCountByUser(testUserId, "2026-04-20").test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Tombstone vẫn còn trong SQLite với deleted = true và updatedAt mới
        val raw = wordDao.getByIdIncludingDeleted("w1")
        assertNotNull(raw)
        assertTrue(raw!!.deleted)
        assertEquals(deletedAt, raw.updatedAt)
    }

    @Test
    fun updateReviewProgress_updatesLastReviewedAtAndUpdatedAt() = runTest {
        wordDao.insert(newWord(id = "w1"))

        wordDao.updateReviewProgress(
            wordId = "w1",
            status = WordStatus.BIRD.storage,
            correctStreak = 7,
            wrongCount = 1,
            intervalDays = 14,
            nextReviewAt = "2026-05-04",
            lastReviewedAt = "2026-04-20T18:00:00Z",
            updatedAt = "2026-04-20T18:00:00Z"
        )

        val fetched = wordDao.getById("w1")
        assertNotNull(fetched)
        assertEquals(WordStatus.BIRD.storage, fetched?.status)
        assertEquals(7, fetched?.correctStreak)
        assertEquals(1, fetched?.wrongCount)
        assertEquals(14, fetched?.intervalDays)
        assertEquals("2026-05-04", fetched?.nextReviewAt)
        assertEquals("2026-04-20T18:00:00Z", fetched?.lastReviewedAt)
        assertEquals("2026-04-20T18:00:00Z", fetched?.updatedAt)
    }

    @Test
    fun observeWordsByStatus_filtersCorrectly() = runTest {
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "a", status = WordStatus.EGG.storage),
                newWord(id = "w2", term = "b", status = WordStatus.CHICK.storage),
                newWord(id = "w3", term = "c", status = WordStatus.BIRD.storage),
                newWord(id = "w4", term = "d", status = WordStatus.EGG.storage)
            )
        )

        wordDao.observeWordsByStatus(deckId, WordStatus.EGG.storage).test {
            val eggs = awaitItem()
            assertEquals(2, eggs.size)
            assertTrue(eggs.all { it.status == WordStatus.EGG.storage })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeUserStats_countsTotalStatusAndDueWordsAcrossDecks() = runTest {
        val today = "2026-04-20"
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "a", status = WordStatus.EGG.storage, nextReviewAt = today),
                newWord(id = "w2", term = "b", status = WordStatus.CHICK.storage, nextReviewAt = today),
                newWord(id = "w3", term = "c", status = WordStatus.BIRD.storage, nextReviewAt = "2026-04-25")
            )
        )

        wordDao.observeTotalWordCountByUser(testUserId).test {
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeCountByUserAndStatus(testUserId, WordStatus.CHICK.storage).test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeDueCountByUser(testUserId, today).test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeSearchInDeck_and_observeSearchAllWords_matchTermMeaningExample() = runTest {
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "ubiquitous", meaning = "có mặt khắp nơi", example = "Smartphones are ubiquitous"),
                newWord(id = "w2", term = "mitigate", meaning = "giảm nhẹ", example = "Mitigate climate change"),
                newWord(id = "w3", term = "hatch", meaning = "nở trứng", example = "Chicks hatch in spring")
            )
        )

        wordDao.observeSearchInDeck(deckId, "mitig").test {
            val r = awaitItem()
            assertEquals(1, r.size)
            assertEquals("mitigate", r.first().term)
            cancelAndIgnoreRemainingEvents()
        }

        wordDao.observeSearchAllWords(testUserId, "nở trứng").test {
            val r = awaitItem()
            assertEquals(1, r.size)
            assertEquals("hatch", r.first().term)
            cancelAndIgnoreRemainingEvents()
        }

        wordDao.observeSearchAllWords(testUserId, "Smartphones").test {
            val r = awaitItem()
            assertEquals(1, r.size)
            assertEquals("ubiquitous", r.first().term)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun existsByTerm_detectsDuplicateCaseInsensitive() = runTest {
        wordDao.insert(newWord(id = "w1", term = "Algorithm"))

        assertTrue(wordDao.existsByTerm(deckId, "  algorithm "))
        assertFalse(wordDao.existsByTerm(deckId, "algorithm", excludeId = "w1"))
        assertFalse(wordDao.existsByTerm(deckId, "latency"))
    }

    @Test
    fun observeSearchAllWordsWithDeck_returnsDeckInfoCaseInsensitiveAndEscapesWildcards() = runTest {
        val travelDeckId = "deck-travel"
        val deletedDeckId = "deck-deleted"
        deckDao.insertAll(
            listOf(
                Deck(
                    deckId = travelDeckId,
                    userId = testUserId,
                    nameDeck = "Du lịch",
                    icon = DeckStyle.ICON_PLANE,
                    color = DeckStyle.COLOR_YELLOW,
                    createAt = "2026-04-20T10:00:00Z",
                ),
                Deck(
                    deckId = deletedDeckId,
                    userId = testUserId,
                    nameDeck = "Đã xoá",
                    createAt = "2026-04-20T10:00:00Z",
                    deleted = true,
                ),
            ),
        )
        wordDao.insertAll(
            listOf(
                newWord(id = "w-chair", term = "chair", meaning = "cái ghế", example = "Sit on a chair"),
                Word(
                    wordId = "w-airport",
                    deckId = travelDeckId,
                    term = "airport",
                    meaning = "sân bay",
                    example = "Go to the airport",
                    createAt = "2026-04-20T10:00:00Z",
                ),
                Word(
                    wordId = "w-hidden-deck",
                    deckId = deletedDeckId,
                    term = "airship",
                    meaning = "khí cầu",
                    createAt = "2026-04-20T10:00:00Z",
                ),
                newWord(id = "w-deleted-word", term = "airbag", meaning = "túi khí", deleted = true),
                newWord(id = "w-pct", term = "50% off", meaning = "giảm một nửa", example = null),
                newWord(id = "w-us", term = "word_nest", meaning = "tổ từ", example = null),
                newWord(id = "w-bs", term = "dir\\file", meaning = "đường dẫn", example = null),
            ),
        )

        wordDao.observeSearchAllWordsWithDeck(testUserId, "AIR").test {
            val found = awaitItem()
            assertEquals(listOf("airport", "chair"), found.map { it.word.term })
            assertEquals("Du lịch", found[0].deckName)
            assertEquals(DeckStyle.ICON_PLANE, found[0].deckIcon)
            assertEquals(DeckStyle.COLOR_YELLOW, found[0].deckColor)
            assertEquals("Test Deck", found[1].deckName)
            cancelAndIgnoreRemainingEvents()
        }

        wordDao.observeSearchAllWordsWithDeck(testUserId, Validation.escapeLike("%")).test {
            assertEquals(listOf("50% off"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeSearchAllWordsWithDeck(testUserId, Validation.escapeLike("_")).test {
            assertEquals(listOf("word_nest"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeSearchAllWordsWithDeck(testUserId, Validation.escapeLike("\\")).test {
            assertEquals(listOf("dir\\file"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
