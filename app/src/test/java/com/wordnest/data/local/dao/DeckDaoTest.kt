package com.wordnest.data.local.dao

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
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
        createdAt: String = "2026-04-20T10:00:00Z",
        updatedAt: String = createdAt,
        deleted: Boolean = false,
        icon: String = "globe",
        color: String = "yellow"
    ) = Deck(
        deckId = id,
        userId = testUserId,
        nameDeck = name,
        description = desc,
        createAt = createdAt,
        icon = icon,
        color = color,
        updatedAt = updatedAt,
        deleted = deleted
    )

    @Test
    fun insertAndGetById_returnsInsertedDeckWithIconColorAndV3Fields() = runTest {
        val deck = newDeck(icon = "bulb", color = "teal")
        deckDao.insert(deck)

        val loaded = deckDao.getById("deck-1")
        assertNotNull(loaded)
        assertEquals("IELTS", loaded?.nameDeck)
        assertEquals("Academic words", loaded?.description)
        assertEquals("bulb", loaded?.icon)
        assertEquals("teal", loaded?.color)
        assertEquals("2026-04-20T10:00:00Z", loaded?.updatedAt)
        assertFalse(loaded!!.deleted)
    }

    @Test
    fun updateInfo_changesNameDescriptionIconColorAndUpdatedAt() = runTest {
        deckDao.insert(newDeck())
        deckDao.updateInfo(
            deckId = "deck-1",
            name = "IELTS Advanced",
            description = "Updated desc",
            icon = "book",
            color = "pink",
            updatedAt = "2026-04-21T08:00:00Z"
        )

        val loaded = deckDao.getById("deck-1")
        assertEquals("IELTS Advanced", loaded?.nameDeck)
        assertEquals("Updated desc", loaded?.description)
        assertEquals("book", loaded?.icon)
        assertEquals("pink", loaded?.color)
        assertEquals("2026-04-21T08:00:00Z", loaded?.updatedAt)
    }

    @Test
    fun hardDeleteDeck_cascadesDeleteWords() = runTest {
        val deck = newDeck()
        deckDao.insert(deck)
        wordDao.insert(
            Word(
                wordId = "w-1",
                deckId = deck.deckId,
                term = "ubiquitous",
                meaning = "phổ biến",
                status = WordStatus.EGG.storage,
                createAt = "2026-04-20T10:00:00Z"
            )
        )
        assertEquals(1, wordDao.countByDeck(deck.deckId))

        deckDao.delete(deck)

        assertNull(deckDao.getByIdIncludingDeleted(deck.deckId))
        assertNull(wordDao.getByIdIncludingDeleted("w-1"))
        assertEquals(0, wordDao.countByDeck(deck.deckId))
    }

    @Test
    fun softDeleteWithWords_hidesDeckAndChildWordsFromDefaultQueries_butKeepsTombstones() = runTest {
        val deck = newDeck(id = "deck-soft", name = "Du lịch")
        deckDao.insert(deck)
        wordDao.insert(
            Word(
                wordId = "w-soft",
                deckId = deck.deckId,
                term = "luggage",
                meaning = "hành lý",
                status = WordStatus.CHICK.storage,
                nextReviewAt = "2026-04-20",
                createAt = "2026-04-20T10:00:00Z"
            )
        )

        val deletedAt = "2026-04-22T09:30:00Z"
        val affected = deckDao.softDeleteWithWords(deck.deckId, deletedAt)
        assertEquals(1, affected)

        // Mọi truy vấn mặc định đều không trả về bản ghi đã xoá mềm
        assertNull(deckDao.getById(deck.deckId))
        assertEquals(0, deckDao.getDecksByUser(testUserId).size)
        assertEquals(0, deckDao.countByUser(testUserId))
        assertEquals(0, deckDao.count())
        assertFalse(deckDao.existsByName(testUserId, "Du lịch"))
        assertNull(deckDao.getDeckWithStatsById(deck.deckId, "2026-04-20"))
        assertNull(deckDao.getDeckWithWords(deck.deckId))
        assertNull(wordDao.getById("w-soft"))
        assertEquals(0, wordDao.countByDeck(deck.deckId))

        deckDao.observeDecksWithStats(testUserId, "2026-04-20").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        deckDao.observeSearchDecks(testUserId, "Du lịch", "2026-04-20").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        wordDao.observeTotalWordCountByUser(testUserId).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Bản ghi tombstone vẫn nằm trong SQLite phục vụ đồng bộ FB3
        val rawDeck = deckDao.getByIdIncludingDeleted(deck.deckId)
        assertNotNull(rawDeck)
        assertTrue(rawDeck!!.deleted)
        assertEquals(deletedAt, rawDeck.updatedAt)

        val rawWord = wordDao.getByIdIncludingDeleted("w-soft")
        assertNotNull(rawWord)
        assertTrue(rawWord!!.deleted)
        assertEquals(deletedAt, rawWord.updatedAt)
    }

    @Test
    fun observeDecksWithStats_computesWordCountLearnedCountAndDueCount_andExcludesSoftDeletedWords() = runTest {
        val deck = newDeck()
        val emptyAfterSoftDelete = newDeck(id = "deck-empty", name = "Trống sau xoá từ")
        deckDao.insertAll(listOf(deck, emptyAfterSoftDelete))
        val today = "2026-04-20"
        val future = "2026-04-25"

        wordDao.insertAll(
            listOf(
                Word(
                    wordId = "w1", deckId = deck.deckId, term = "a", meaning = "1",
                    status = WordStatus.EGG.storage, nextReviewAt = today, createAt = today
                ),
                Word(
                    wordId = "w2", deckId = deck.deckId, term = "b", meaning = "2",
                    status = WordStatus.CHICK.storage, nextReviewAt = "2026-04-19", createAt = today
                ),
                Word(
                    wordId = "w3", deckId = deck.deckId, term = "c", meaning = "3",
                    status = WordStatus.BIRD.storage, nextReviewAt = future, createAt = today
                ),
                Word(
                    wordId = "w4-deleted", deckId = deck.deckId, term = "d", meaning = "4",
                    status = WordStatus.BIRD.storage, nextReviewAt = today, createAt = today,
                    deleted = true
                ),
                Word(
                    wordId = "w5-deleted", deckId = emptyAfterSoftDelete.deckId, term = "e", meaning = "5",
                    status = WordStatus.CHICK.storage, nextReviewAt = today, createAt = today,
                    deleted = true
                )
            )
        )

        deckDao.observeDecksWithStats(testUserId, today).test {
            val list = awaitItem()
            assertEquals(2, list.size)

            val stats = list.first { it.deck.deckId == deck.deckId }
            assertEquals(3, stats.wordCount)
            assertEquals(2, stats.learnedCount) // CHICK + BIRD (bỏ qua w4-deleted)
            assertEquals(2, stats.dueCount)     // today + past (bỏ qua w4-deleted)
            assertEquals(2f / 3f, stats.progressPercent, 0.001f)

            // Tổ chỉ còn từ đã xoá mềm vẫn phải hiển thị với wordCount = 0
            val emptyStats = list.first { it.deck.deckId == emptyAfterSoftDelete.deckId }
            assertEquals(0, emptyStats.wordCount)
            assertEquals(0, emptyStats.learnedCount)
            assertEquals(0, emptyStats.dueCount)
            assertEquals(0f, emptyStats.progressPercent, 0.001f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getDeckWithWords_excludesSoftDeletedWords() = runTest {
        val deck = newDeck()
        deckDao.insert(deck)
        wordDao.insertAll(
            listOf(
                Word(wordId = "w1", deckId = deck.deckId, term = "active", meaning = "hoạt động", createAt = "2026-04-20T10:00:00Z"),
                Word(wordId = "w2", deckId = deck.deckId, term = "removed", meaning = "đã xoá", createAt = "2026-04-20T10:00:00Z", deleted = true)
            )
        )

        val result = deckDao.getDeckWithWords(deck.deckId)
        assertNotNull(result)
        assertEquals(1, result!!.words.size)
        assertEquals("active", result.words.first().term)
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
