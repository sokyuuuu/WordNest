package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.data.study.RoomStudyWordSource
import com.wordnest.util.WordStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeckDetailViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var tick = 0
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository

    @Before
    fun setUpVm() {
        Dispatchers.setMain(testDispatcher)
        tick = 0
        val nowProvider = {
            tick++
            "2026-04-20T10:00:%02dZ".format(tick)
        }
        deckRepo = DeckRepository(db.deckDao(), nowProvider, { "2026-04-20" })
        wordRepo = WordRepository(db.wordDao(), db.deckDao(), nowProvider, { "2026-04-20" })
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyDeck_emitsZeroStatsAndEmptyWords() = runTest {
        val deck = deckRepo.createDeck(testUserId, "IELTS", "Từ học thuật").getOrThrow()
        val vm = DeckDetailViewModel(deck.deckId, deckRepo, wordRepo)

        val state = awaitReal { vm.uiState.first { !it.isLoading } }
        assertFalse(state.isMissing)
        assertEquals("IELTS", state.deck?.nameDeck)
        assertEquals(0, state.totalWords)
        assertEquals(0, state.learnedWords)
        assertEquals(0, state.eggCount)
        assertEquals(0, state.chickCount)
        assertEquals(0, state.birdCount)
        assertEquals(0f, state.progressPercent, 0.0001f)
        assertTrue(state.words.isEmpty())
    }

    @Test
    fun deckWithWords_countsThreeStatusesCalculatesProgressAndOrdersNewestFirst() = runTest {
        val deck = deckRepo.createDeck(testUserId, "Du lịch").getOrThrow()
        db.wordDao().insertAll(
            listOf(
                Word("w1", deck.deckId, "airport", "Sân bay", status = WordStatus.EGG.storage, createAt = "2026-04-20T09:59:01Z"),
                Word("w2", deck.deckId, "hotel", "Khách sạn", status = WordStatus.EGG.storage, createAt = "2026-04-20T09:59:02Z"),
                Word("w3", deck.deckId, "passport", "Hộ chiếu", status = WordStatus.CHICK.storage, createAt = "2026-04-20T09:59:03Z"),
                Word("w4", deck.deckId, "itinerary", "Lịch trình", status = WordStatus.BIRD.storage, createAt = "2026-04-20T09:59:04Z"),
            ),
        )

        val vm = DeckDetailViewModel(deck.deckId, deckRepo, wordRepo)
        val state = awaitReal { vm.uiState.first { !it.isLoading && it.totalWords == 4 } }

        assertEquals(4, state.totalWords)
        assertEquals(2, state.eggCount)
        assertEquals(1, state.chickCount)
        assertEquals(1, state.birdCount)
        assertEquals(2, state.learnedWords)
        assertEquals(0.5f, state.progressPercent, 0.0001f)
        // Sắp xếp mới nhất lên đầu (create_at DESC)
        assertEquals(listOf("itinerary", "passport", "hotel", "airport"), state.words.map { it.term })
        assertEquals(state.words, state.deckWithWords?.words)

        // Thêm từ mới -> hiện ngay đầu danh sách với trạng thái EGG (quickstart C7)
        val added = wordRepo.addWord(deck.deckId, "boarding pass", "Thẻ lên máy bay").getOrThrow()
        val updated = awaitReal { vm.uiState.first { it.totalWords == 5 } }
        assertEquals(added.wordId, updated.words.first().wordId)
        assertEquals(WordStatus.EGG.storage, updated.words.first().status)
        assertEquals(3, updated.eggCount)
    }

    @Test
    fun softDeletingDeck_marksDetailMissingAndRemovesWordsFromSearchAndStudySource() = runTest {
        val deck = deckRepo.createDeck(testUserId, "Tổ cần xoá").getOrThrow()
        val word = wordRepo.addWord(deck.deckId, "luggage", "Hành lý").getOrThrow()
        val studySource = RoomStudyWordSource(db.wordDao(), userId = testUserId)
        val vm = DeckDetailViewModel(deck.deckId, deckRepo, wordRepo)

        val loaded = awaitReal { vm.uiState.first { !it.isLoading && it.totalWords == 1 } }
        assertFalse(loaded.isMissing)
        assertEquals(1, awaitReal { wordRepo.observeSearchAllWords(testUserId, "lug").first() }.size)
        assertEquals(1, awaitReal { studySource.words().first() }.size)

        // Xoá mềm Tổ (quickstart C5 + T035)
        assertTrue(deckRepo.deleteDeck(deck.deckId).isSuccess)

        val missing = awaitReal { vm.uiState.first { it.isMissing } }
        assertTrue(missing.isMissing)
        assertNull(missing.deck)
        assertTrue(missing.words.isEmpty())

        // Từ bên trong bị xoá mềm và biến mất khỏi tìm kiếm + luồng học / Thống kê
        assertTrue(db.wordDao().getByIdIncludingDeleted(word.wordId)!!.deleted)
        assertTrue(awaitReal { wordRepo.observeSearchAllWords(testUserId, "lug").first() }.isEmpty())
        assertTrue(awaitReal { studySource.words().first() }.isEmpty())
    }

    @Test
    fun deleteWord_softDeletesWordAndEmitsEventAndUpdatesCounts() = runTest {
        val deck = deckRepo.createDeck(testUserId, "CNTT").getOrThrow()
        val w1 = wordRepo.addWord(deck.deckId, "latency", "Độ trễ").getOrThrow()
        wordRepo.addWord(deck.deckId, "cache", "Bộ nhớ đệm").getOrThrow()

        val vm = DeckDetailViewModel(deck.deckId, deckRepo, wordRepo)
        awaitReal { vm.uiState.first { it.totalWords == 2 } }

        val events = mutableListOf<DeckDetailEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            vm.events.collect { events.add(it) }
        }

        var onSuccessCalled = false
        vm.deleteWord(w1.wordId, onSuccess = { onSuccessCalled = true })
        awaitCondition { onSuccessCalled }

        val after = awaitReal { vm.uiState.first { it.totalWords == 1 } }
        assertEquals(listOf("cache"), after.words.map { it.term })
        assertTrue(db.wordDao().getByIdIncludingDeleted(w1.wordId)!!.deleted)
        assertTrue(events.any { it is DeckDetailEvent.WordDeleted })

        collectJob.cancel()
    }

    @Test
    fun studyStatusChange_updatesDeckDetailCountsAndProgressLive() = runTest {
        val deck = deckRepo.createDeck(testUserId, "Du lịch").getOrThrow()
        val w1 = wordRepo.addWord(deck.deckId, "airport", "Sân bay").getOrThrow()
        wordRepo.addWord(deck.deckId, "hotel", "Khách sạn").getOrThrow()
        val studySource = RoomStudyWordSource(db.wordDao(), userId = testUserId)

        val vm = DeckDetailViewModel(deck.deckId, deckRepo, wordRepo)
        val initial = awaitReal { vm.uiState.first { !it.isLoading && it.totalWords == 2 } }
        assertEquals(2, initial.eggCount)
        assertEquals(0, initial.learnedWords)
        assertEquals(0f, initial.progressPercent, 0.0001f)

        // Sau 1 buổi học, trạng thái từ đổi sang CHICK rồi BIRD -> DeckDetail tự cập nhật qua Flow
        val studyWord = awaitReal { studySource.words().first { it.size == 2 } }.first { it.id == w1.wordId }
        studySource.save(studyWord.copy(status = WordStatus.BIRD))

        val updated = awaitReal { vm.uiState.first { it.birdCount == 1 } }
        assertEquals(1, updated.eggCount)
        assertEquals(0, updated.chickCount)
        assertEquals(1, updated.birdCount)
        assertEquals(1, updated.learnedWords)
        assertEquals(0.5f, updated.progressPercent, 0.0001f)
    }

    @Test
    fun factory_createsDeckDetailViewModel_andRejectsOtherClasses() {
        val factory = DeckDetailViewModelFactory("deck-1", deckRepo, wordRepo)
        val created = factory.create(DeckDetailViewModel::class.java)
        assertNotNull(created)
        assertEquals("deck-1", created.deckId)

        class DummyViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(DummyViewModel::class.java)
        }
    }
}
