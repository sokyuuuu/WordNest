package com.wordnest.data.local.dao

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.data.local.entity.WordStatus
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
                createAt = "2026-04-20T10:00:00"
            )
        )
    }

    private fun newWord(
        id: String = "w-1",
        term: String = "algorithm",
        meaning: String = "thuật toán",
        example: String? = "Sorting algorithm",
        note: String? = "Thuật ngữ cơ bản",
        status: String = WordStatus.EGG,
        nextReviewAt: String? = "2026-04-20"
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
        createAt = "2026-04-20T10:00:00"
    )

    @Test
    fun insertUpdateDelete_wordLifecycleWithNote() = runTest {
        val word = newWord(note = "Ghi chú ban đầu")
        wordDao.insert(word)

        val fetched = wordDao.getById("w-1")
        assertNotNull(fetched)
        assertEquals("algorithm", fetched?.term)
        assertEquals("Ghi chú ban đầu", fetched?.note)

        val updated = fetched!!.copy(meaning = "thuật giải", note = "Ghi chú mới")
        wordDao.update(updated)
        val afterUpdate = wordDao.getById("w-1")
        assertEquals("thuật giải", afterUpdate?.meaning)
        assertEquals("Ghi chú mới", afterUpdate?.note)

        wordDao.deleteById("w-1")
        assertNull(wordDao.getById("w-1"))
    }

    @Test
    fun observeWordsByStatus_filtersCorrectly() = runTest {
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "a", status = WordStatus.EGG),
                newWord(id = "w2", term = "b", status = WordStatus.CHICK),
                newWord(id = "w3", term = "c", status = WordStatus.BIRD),
                newWord(id = "w4", term = "d", status = WordStatus.EGG)
            )
        )

        wordDao.observeWordsByStatus(deckId, WordStatus.EGG).test {
            val eggs = awaitItem()
            assertEquals(2, eggs.size)
            assertTrue(eggs.all { it.status == WordStatus.EGG })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeUserStats_countsTotalStatusAndDueWordsAcrossDecks() = runTest {
        val today = "2026-04-20"
        wordDao.insertAll(
            listOf(
                newWord(id = "w1", term = "a", status = WordStatus.EGG, nextReviewAt = today),
                newWord(id = "w2", term = "b", status = WordStatus.CHICK, nextReviewAt = today),
                newWord(id = "w3", term = "c", status = WordStatus.BIRD, nextReviewAt = "2026-04-25")
            )
        )

        wordDao.observeTotalWordCountByUser(testUserId).test {
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeCountByUserAndStatus(testUserId, WordStatus.CHICK).test {
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
}
