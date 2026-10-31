package com.wordnest.data.repository

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.util.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeckRepositoryTest : BaseDbTest() {

    private lateinit var repo: DeckRepository

    @Before
    fun initRepo() {
        repo = DeckRepository(
            deckDao = db.deckDao(),
            nowProvider = { "2026-04-20T12:00:00" },
            todayProvider = { "2026-04-20" }
        )
    }

    @Test
    fun createDeck_success_trimsInputAndPersistsWithIconAndColor() = runTest {
        val result = repo.createDeck(
            userId = testUserId,
            name = "  Du lịch  ",
            description = "  Từ đi phượt ",
            icon = "plane",
            color = "mint"
        )
        assertTrue(result.isSuccess)
        val deck = result.getOrThrow()
        assertEquals("Du lịch", deck.nameDeck)
        assertEquals("Từ đi phượt", deck.description)
        assertEquals("plane", deck.icon)
        assertEquals("mint", deck.color)

        val stored = repo.getDeckById(deck.deckId)
        assertEquals("Du lịch", stored?.nameDeck)
        assertEquals("plane", stored?.icon)
        assertEquals("mint", stored?.color)
    }

    @Test
    fun createDeck_blankName_failsWithValidationException() = runTest {
        val result = repo.createDeck(testUserId, "   ", "desc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
    }

    @Test
    fun createDeck_invalidIconOrColor_failsWithValidationException() = runTest {
        val badIcon = repo.createDeck(testUserId, "Valid Name", "desc", icon = "rocket", color = "mint")
        assertTrue(badIcon.isFailure)
        assertTrue(badIcon.exceptionOrNull() is ValidationException)

        val badColor = repo.createDeck(testUserId, "Valid Name", "desc", icon = "plane", color = "neon")
        assertTrue(badColor.isFailure)
        assertTrue(badColor.exceptionOrNull() is ValidationException)
    }

    @Test
    fun createDeck_duplicateName_fails() = runTest {
        assertTrue(repo.createDeck(testUserId, "IELTS").isSuccess)
        val dup = repo.createDeck(testUserId, "ielts")
        assertTrue(dup.isFailure)
        assertTrue(dup.exceptionOrNull() is ValidationException)
    }

    @Test
    fun updateDeck_success_andRejectsDuplicateAgainstOtherDeck() = runTest {
        val d1 = repo.createDeck(testUserId, "IELTS").getOrThrow()
        val d2 = repo.createDeck(testUserId, "Du lịch").getOrThrow()

        val ok = repo.updateDeck(
            deckId = d1.deckId,
            userId = testUserId,
            newName = "IELTS Academic",
            newDescription = "Band 7+",
            newIcon = "globe",
            newColor = "yellow"
        )
        assertTrue(ok.isSuccess)
        val updated = repo.getDeckById(d1.deckId)
        assertEquals("IELTS Academic", updated?.nameDeck)
        assertEquals("globe", updated?.icon)
        assertEquals("yellow", updated?.color)

        // Đổi tên d2 trùng với d1 -> lỗi
        val clash = repo.updateDeck(d2.deckId, testUserId, "ielts academic", null)
        assertTrue(clash.isFailure)
    }

    @Test
    fun deleteDeck_removesDeck_andFailsWhenNotFound() = runTest {
        val deck = repo.createDeck(testUserId, "CNTT").getOrThrow()
        assertTrue(repo.deleteDeck(deck.deckId).isSuccess)
        assertNull(repo.getDeckById(deck.deckId))

        val again = repo.deleteDeck(deck.deckId)
        assertTrue(again.isFailure)
    }

    @Test
    fun observeSearchDecks_emptyKeywordReturnsAll_nonEmptyFilters() = runTest {
        repo.createDeck(testUserId, "Du lịch", "Sân bay")
        repo.createDeck(testUserId, "IELTS", "Academic")

        repo.observeSearchDecks(testUserId, "").test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        repo.observeSearchDecks(testUserId, "Sân bay").test {
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("Du lịch", filtered.first().deck.nameDeck)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
