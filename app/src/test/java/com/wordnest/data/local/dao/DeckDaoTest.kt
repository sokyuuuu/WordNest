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
class DeckDaoTest : BaseDbTest() {

    private lateinit var deckDao: DeckDao
    private lateinit var wordDao: WordDao

    @Before
    fun initDaos() {
        deckDao = db.deckDao()
        wordDao = db.wordDao()
    }

    private fun newDeck(
        id: String = "deck-1",
        name: String = "IELTS",
        desc: String? = "Academic words",
        createdAt: String = "2026-04-20T10:00:00",
        icon: String = "globe",
        color: String = "yellow"
    ) = Deck(
        deckId = id,
        userId = testUserId,
        nameDeck = name,
        description = desc,
        createAt = createdAt,
        icon = icon,
        color = color
    )

    @Test
    fun insertAndGetById_returnsInsertedDeckWithIconAndColor() = runTest {
        val deck = newDeck(icon = "bulb", color = "teal")
        deckDao.insert(deck)

        val loaded = deckDao.getById("deck-1")
        assertNotNull(loaded)
        assertEquals("IELTS", loaded?.nameDeck)
        assertEquals("Academic words", loaded?.description)
        assertEquals("bulb", loaded?.icon)
        assertEquals("teal", loaded?.color)
    }

    @Test
    fun updateInfo_changesNameDescriptionIconAndColor() = runTest {
        deckDao.insert(newDeck())
        deckDao.updateInfo("deck-1", "IELTS Advanced", "Updated desc", "book", "pink")

        val loaded = deckDao.getById("deck-1")
        assertEquals("IELTS Advanced", loaded?.nameDeck)
        assertEquals("Updated desc", loaded?.description)
        assertEquals("book", loaded?.icon)
        assertEquals("pink", loaded?.color)
    }

    @Test
    fun deleteDeck_cascadesDeleteWords() = runTest {
        val deck = newDeck()
        deckDao.insert(deck)
        wordDao.insert(
            Word(
                wordId = "w-1",
                deckId = deck.deckId,
                term = "ubiquitous",
                meaning = "phổ biến",
                status = WordStatus.EGG,
                createAt = "2026-04-20T10:00:00"
            )
        )
        assertEquals(1, wordDao.countByDeck(deck.deckId))

        deckDao.delete(deck)

        assertNull(deckDao.getById(deck.deckId))
        assertEquals(0, wordDao.countByDeck(deck.deckId))
    }

    @Test
    fun observeDecksWithStats_computesWordCountLearnedCountAndDueCount() = runTest {
        val deck = newDeck()
        deckDao.insert(deck)
        val today = "2026-04-20"
        val future = "2026-04-25"

        wordDao.insertAll(
            listOf(
                Word(
                    wordId = "w1", deckId = deck.deckId, term = "a", meaning = "1",
                    status = WordStatus.EGG, nextReviewAt = today, createAt = today
                ),
                Word(
                    wordId = "w2", deckId = deck.deckId, term = "b", meaning = "2",
                    status = WordStatus.CHICK, nextReviewAt = "2026-04-19", createAt = today
                ),
                Word(
                    wordId = "w3", deckId = deck.deckId, term = "c", meaning = "3",
                    status = WordStatus.BIRD, nextReviewAt = future, createAt = today
                )
            )
        )

        deckDao.observeDecksWithStats(testUserId, today).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            val stats = list.first()
            assertEquals(3, stats.wordCount)
            assertEquals(2, stats.learnedCount) // CHICK + BIRD
            assertEquals(2, stats.dueCount)     // today + past
            assertEquals(2f / 3f, stats.progressPercent, 0.001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun existsByName_isCaseAndWhitespaceInsensitive_andRespectsExcludeId() = runTest {
        deckDao.insert(newDeck(id = "deck-1", name = "Du lịch"))

        assertTrue(deckDao.existsByName(testUserId, "  du LỊCH "))
        assertFalse(deckDao.existsByName(testUserId, "du lịch", excludeId = "deck-1"))
        assertFalse(deckDao.existsByName(testUserId, "Công nghệ"))
    }

    @Test
    fun observeSearchDecks_matchesNameOrDescription() = runTest {
        deckDao.insertAll(
            listOf(
                newDeck(id = "d1", name = "Du lịch", desc = "Sân bay & khách sạn"),
                newDeck(id = "d2", name = "IELTS", desc = "Academic reading"),
                newDeck(id = "d3", name = "CNTT", desc = "Phần mềm")
            )
        )

        deckDao.observeSearchDecks(testUserId, "khách sạn", "2026-04-20").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("d1", result.first().deck.deckId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
