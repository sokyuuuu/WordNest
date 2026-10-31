package com.wordnest.data.repository

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.WordStatus
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
class WordRepositoryTest : BaseDbTest() {

    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var deckId: String

    @Before
    fun initRepos() = runTest {
        deckRepo = DeckRepository(db.deckDao(), { "2026-04-20T12:00:00" }, { "2026-04-20" })
        wordRepo = WordRepository(db.wordDao(), db.deckDao(), { "2026-04-20T12:00:00" }, { "2026-04-20" })
        deckId = deckRepo.createDeck(testUserId, "IELTS").getOrThrow().deckId
    }

    @Test
    fun addWord_setsDefaultEggStatusAndTodayReviewDate_andSavesNote() = runTest {
        val result = wordRepo.addWord(
            deckId = deckId,
            term = "  ubiquitous ",
            meaning = " có mặt khắp nơi ",
            phonetic = " /juːˈbɪkwɪtəs/ ",
            example = " Smartphones are ubiquitous. ",
            note = " Đồng nghĩa: omnipresent "
        )
        assertTrue(result.isSuccess)
        val word = result.getOrThrow()
        assertEquals("ubiquitous", word.term)
        assertEquals("có mặt khắp nơi", word.meaning)
        assertEquals("Đồng nghĩa: omnipresent", word.note)
        assertEquals(WordStatus.EGG, word.status)
        assertEquals(0, word.correctStreak)
        assertEquals("2026-04-20", word.nextReviewAt)
    }

    @Test
    fun addWord_duplicateTermInSameDeck_fails() = runTest {
        assertTrue(wordRepo.addWord(deckId, "mitigate", "giảm nhẹ").isSuccess)
        val dup = wordRepo.addWord(deckId, "  MITIGATE ", "làm dịu")
        assertTrue(dup.isFailure)
        assertTrue(dup.exceptionOrNull() is ValidationException)
    }

    @Test
    fun addWord_nonExistentDeck_fails() = runTest {
        val r = wordRepo.addWord("missing-deck", "term", "meaning")
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateWord_preservesStudyProgress_andUpdatesNote() = runTest {
        val created = wordRepo.addWord(deckId, "hatch", "nở", note = "Ghi chú cũ").getOrThrow()
        db.wordDao().updateReviewProgress(
            wordId = created.wordId,
            status = WordStatus.BIRD,
            correctStreak = 5,
            wrongCount = 1,
            intervalDays = 7,
            nextReviewAt = "2026-04-27",
            lastReviewAt = "2026-04-20T12:00:00"
        )
        val progressed = wordRepo.getWordById(created.wordId)!!

        val updated = wordRepo.updateWord(
            word = progressed,
            newTerm = "hatch",
            newMeaning = "ấp nở (trứng)",
            newPhonetic = "/hætʃ/",
            newExample = "Chicks hatch in 20 days",
            newNote = "Ghi chú đã cập nhật"
        ).getOrThrow()

        assertEquals("ấp nở (trứng)", updated.meaning)
        assertEquals("Ghi chú đã cập nhật", updated.note)
        assertEquals(WordStatus.BIRD, updated.status)
        assertEquals(5, updated.correctStreak)
        assertEquals(7, updated.intervalDays)
    }

    @Test
    fun deleteWord_removesFromDb() = runTest {
        val w = wordRepo.addWord(deckId, "beak", "mỏ chim").getOrThrow()
        assertTrue(wordRepo.deleteWord(w.wordId).isSuccess)
        assertNull(wordRepo.getWordById(w.wordId))
    }

    @Test
    fun observeSearchAllWords_searchesAcrossMultipleDecks() = runTest {
        val deck2 = deckRepo.createDeck(testUserId, "CNTT").getOrThrow().deckId
        wordRepo.addWord(deckId, "ubiquitous", "phổ biến")
        wordRepo.addWord(deck2, "algorithm", "thuật toán")

        wordRepo.observeSearchAllWords(testUserId, "thuật toán").test {
            val found = awaitItem()
            assertEquals(1, found.size)
            assertEquals("algorithm", found.first().term)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
