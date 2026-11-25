package com.wordnest.data.repository

import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.util.ValidationException
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
class WordRepositoryTest : BaseDbTest() {

    private var fakeNow = "2026-04-20T12:00:00Z"
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var deckId: String

    @Before
    fun initRepos() = runTest {
        fakeNow = "2026-04-20T12:00:00Z"
        deckRepo = DeckRepository(db.deckDao(), { fakeNow }, { "2026-04-20" })
        wordRepo = WordRepository(db.wordDao(), db.deckDao(), { fakeNow }, { "2026-04-20" })
        deckId = deckRepo.createDeck(testUserId, "IELTS").getOrThrow().deckId
    }

    @Test
    fun addWord_setsDefaultEggStatusTodayReviewDateAndUpdatedAt_andSavesNote() = runTest {
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
        assertEquals(WordStatus.EGG.storage, word.status)
        assertEquals(0, word.correctStreak)
        assertEquals("2026-04-20", word.nextReviewAt)
        assertNull(word.lastReviewedAt)
        assertEquals(fakeNow, word.createAt)
        assertEquals(fakeNow, word.updatedAt)
        assertFalse(word.deleted)
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
    fun updateWord_preservesStudyProgress_andUpdatesNoteAndUpdatedAt() = runTest {
        val created = wordRepo.addWord(deckId, "hatch", "nở", note = "Ghi chú cũ").getOrThrow()
        db.wordDao().updateReviewProgress(
            wordId = created.wordId,
            status = WordStatus.BIRD.storage,
            correctStreak = 5,
            wrongCount = 1,
            intervalDays = 7,
            nextReviewAt = "2026-04-27",
            lastReviewedAt = "2026-04-20T12:00:00Z",
            updatedAt = "2026-04-20T12:00:00Z"
        )
        val progressed = wordRepo.getWordById(created.wordId)!!

        fakeNow = "2026-04-21T09:15:00Z"
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
        assertEquals(WordStatus.BIRD.storage, updated.status)
        assertEquals(5, updated.correctStreak)
        assertEquals(7, updated.intervalDays)
        assertEquals("2026-04-20T12:00:00Z", updated.lastReviewedAt)
        assertEquals("2026-04-21T09:15:00Z", updated.updatedAt)
    }

    @Test
    fun deleteWord_softDeletesWithUpdatedAt_andAllowsReaddingSameTerm() = runTest {
        val w = wordRepo.addWord(deckId, "beak", "mỏ chim").getOrThrow()

        fakeNow = "2026-04-22T11:00:00Z"
        assertTrue(wordRepo.deleteWord(w.wordId).isSuccess)
        assertNull(wordRepo.getWordById(w.wordId))

        val raw = db.wordDao().getByIdIncludingDeleted(w.wordId)
        assertNotNull(raw)
        assertTrue(raw!!.deleted)
        assertEquals("2026-04-22T11:00:00Z", raw.updatedAt)

        // Xoá lần 2 phải báo lỗi không tìm thấy
        assertTrue(wordRepo.deleteWord(w.wordId).isFailure)

        // Thêm lại cùng từ sau xoá mềm phải thành công
        assertTrue(wordRepo.addWord(deckId, "beak", "mỏ chim mới").isSuccess)
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

    @Test
    fun observeSearchAllWordsWithDeck_handlesPartialCaseInsensitiveBlankAndWildcards() = runTest {
        val travelId = deckRepo.createDeck(testUserId, "Du lịch").getOrThrow().deckId
        val homeId = deckRepo.createDeck(testUserId, "Nội thất").getOrThrow().deckId
        wordRepo.addWord(travelId, "airport", "Sân bay")
        wordRepo.addWord(homeId, "chair", "Cái ghế")

        // Chuỗi rỗng / khoảng trắng trả về rỗng
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "   ").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Tìm một phần qua nhiều Tổ (quickstart C10)
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "air").test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals(listOf("airport", "chair"), items.map { it.word.term })
            assertEquals(listOf("Du lịch", "Nội thất"), items.map { it.deckName })
            cancelAndIgnoreRemainingEvents()
        }

        // Không phân biệt hoa/thường
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "AIRP").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("airport", items.first().word.term)
            assertEquals("Du lịch", items.first().deckName)
            cancelAndIgnoreRemainingEvents()
        }

        // Ký tự đặc biệt %, _, \ không khớp nhầm toàn bộ dữ liệu (quickstart C11 + T050)
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "%").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "_").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "\\").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Chỉ trả về từ thật sự chứa %, _, \
        wordRepo.addWord(deckId, "100% pure", "nguyên chất")
        wordRepo.addWord(deckId, "snake_case", "kiểu gạch dưới")
        wordRepo.addWord(deckId, "path\\dir", "đường dẫn")

        wordRepo.observeSearchAllWordsWithDeck(testUserId, "%").test {
            assertEquals(listOf("100% pure"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "_").test {
            assertEquals(listOf("snake_case"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
        wordRepo.observeSearchAllWordsWithDeck(testUserId, "\\").test {
            assertEquals(listOf("path\\dir"), awaitItem().map { it.word.term })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateExample_updatesOnlyExampleAndUpdatedAt_preservesProgressAndLastReviewedAt() = runTest {
        val created = wordRepo.addWord(deckId, "airport", "Sân bay").getOrThrow()
        db.wordDao().updateReviewProgress(
            wordId = created.wordId,
            status = WordStatus.CHICK.storage,
            correctStreak = 3,
            wrongCount = 1,
            intervalDays = 3,
            nextReviewAt = "2026-04-23",
            lastReviewedAt = "2026-04-20T12:00:00Z",
            updatedAt = "2026-04-20T12:00:00Z"
        )

        fakeNow = "2026-04-22T14:30:00Z"
        val res = wordRepo.updateExample(created.wordId, "  I go to the airport by bus.  ")
        assertTrue(res.isSuccess)

        val updated = wordRepo.getWordById(created.wordId)!!
        assertEquals("I go to the airport by bus.", updated.example)
        assertEquals("2026-04-22T14:30:00Z", updated.updatedAt)
        assertEquals(WordStatus.CHICK.storage, updated.status)
        assertEquals(3, updated.correctStreak)
        assertEquals(1, updated.wrongCount)
        assertEquals(3, updated.intervalDays)
        assertEquals("2026-04-23", updated.nextReviewAt)
        assertEquals("2026-04-20T12:00:00Z", updated.lastReviewedAt)
    }

    @Test
    fun updateExample_validatesLengthAndMissingOrSoftDeletedWord() = runTest {
        val created = wordRepo.addWord(deckId, "book", "Cuốn sách", example = "Old example").getOrThrow()

        // Quá 500 ký tự -> ValidationException
        val tooLong = wordRepo.updateExample(created.wordId, "a".repeat(501))
        assertTrue(tooLong.isFailure)
        assertTrue(tooLong.exceptionOrNull() is ValidationException)

        // Chuỗi rỗng -> xoá ví dụ thành null
        fakeNow = "2026-04-22T15:00:00Z"
        assertTrue(wordRepo.updateExample(created.wordId, "   ").isSuccess)
        assertNull(wordRepo.getWordById(created.wordId)?.example)

        // Từ không tồn tại hoặc đã xoá mềm -> NoSuchElementException
        assertTrue(wordRepo.updateExample("missing-id", "Example").exceptionOrNull() is NoSuchElementException)
        wordRepo.deleteWord(created.wordId)
        assertTrue(wordRepo.updateExample(created.wordId, "New example").exceptionOrNull() is NoSuchElementException)
    }
}
