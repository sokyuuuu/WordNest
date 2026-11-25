package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.DeckRepository
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
class DeckViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var tick = 0
    private lateinit var deckRepo: DeckRepository
    private lateinit var viewModel: DeckViewModel

    @Before
    fun setUpVm() {
        Dispatchers.setMain(testDispatcher)
        tick = 0
        deckRepo = DeckRepository(
            deckDao = db.deckDao(),
            nowProvider = {
                tick++
                "2026-04-20T10:00:%02dZ".format(tick)
            },
            todayProvider = { "2026-04-20" },
        )
        viewModel = DeckViewModel(deckRepository = deckRepo, userId = testUserId)
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyDatabase_emitsNotLoadingAndEmptyDecks() = runTest {
        val state = awaitReal { viewModel.uiState.first { !it.isLoading } }
        assertFalse(state.isLoading)
        assertTrue(state.decks.isEmpty())
    }

    @Test
    fun createDeck_appearsFirstWithZeroWordsAndZeroProgress_andComputesProgressWithWords() = runTest {
        var createdId: String? = null
        viewModel.createDeck(
            name = "  IELTS  ",
            description = "  Từ học thuật  ",
            icon = DeckStyle.ICON_GLOBE,
            color = DeckStyle.COLOR_YELLOW,
            onSuccess = { createdId = it.deckId },
        )

        awaitCondition { createdId != null }
        val listAfterFirst = awaitReal { viewModel.decks.first { it.size == 1 } }
        val firstDeck = listAfterFirst.first()
        assertEquals("IELTS", firstDeck.deck.nameDeck)
        assertEquals("Từ học thuật", firstDeck.deck.description)
        assertEquals(DeckStyle.ICON_GLOBE, firstDeck.deck.icon)
        assertEquals(DeckStyle.COLOR_YELLOW, firstDeck.deck.color)
        assertEquals(0, firstDeck.wordCount)
        assertEquals(0f, firstDeck.progressPercent, 0.0001f)

        // Thêm 4 từ: 2 egg, 1 chick, 1 bird -> tiến độ = 2/4 = 50%
        db.wordDao().insertAll(
            listOf(
                Word("w1", firstDeck.deck.deckId, "a", "A", status = WordStatus.EGG.storage, createAt = "2026-04-20T10:01:00Z"),
                Word("w2", firstDeck.deck.deckId, "b", "B", status = WordStatus.EGG.storage, createAt = "2026-04-20T10:01:01Z"),
                Word("w3", firstDeck.deck.deckId, "c", "C", status = WordStatus.CHICK.storage, createAt = "2026-04-20T10:01:02Z"),
                Word("w4", firstDeck.deck.deckId, "d", "D", status = WordStatus.BIRD.storage, createAt = "2026-04-20T10:01:03Z"),
            ),
        )

        val updated = awaitReal { viewModel.decks.first { it.firstOrNull()?.wordCount == 4 } }
        assertEquals(2, updated.first().learnedCount)
        assertEquals(0.5f, updated.first().progressPercent, 0.0001f)

        // Tạo thêm Tổ mới hơn -> đứng đầu danh sách
        var secondId: String? = null
        viewModel.createDeck(name = "Du lịch", description = null, onSuccess = { secondId = it.deckId })
        awaitCondition { secondId != null }
        val twoDecks = awaitReal { viewModel.decks.first { it.size == 2 } }
        assertEquals(listOf("Du lịch", "IELTS"), twoDecks.map { it.deck.nameDeck })
    }

    @Test
    fun createDeck_blankOrDuplicateName_emitsErrorEventWithoutInserting() = runTest {
        val events = mutableListOf<DeckEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        var blankSuccessCalled = false
        viewModel.createDeck(name = "   ", description = null, onSuccess = { blankSuccessCalled = true })
        awaitCondition { events.any { it is DeckEvent.ShowMessage && it.isError } }
        assertFalse(blankSuccessCalled)
        assertTrue(awaitReal { viewModel.uiState.first { !it.isLoading } }.decks.isEmpty())

        // Tạo hợp lệ rồi tạo trùng tên -> báo lỗi
        var created = false
        viewModel.createDeck(name = "IELTS", description = null, onSuccess = { created = true })
        awaitCondition { created }

        events.clear()
        var dupSuccessCalled = false
        viewModel.createDeck(name = "ielts", description = null, onSuccess = { dupSuccessCalled = true })
        awaitCondition { events.any { it is DeckEvent.ShowMessage && it.isError } }
        assertFalse(dupSuccessCalled)
        assertEquals(1, awaitReal { viewModel.decks.first { it.isNotEmpty() } }.size)

        collectJob.cancel()
    }

    @Test
    fun updateAndDeleteDeck_updatesFlowRemovesChildWordsAndEmitsMissingLoadState() = runTest {
        val deck = deckRepo.createDeck(testUserId, "IELTS", "Mô tả cũ").getOrThrow()
        db.wordDao().insert(
            Word("w1", deck.deckId, "ubiquitous", "khắp nơi", createAt = "2026-04-20T10:02:00Z"),
        )

        // Kiểm tra sửa Tổ (C4)
        var updatedCalled = false
        viewModel.updateDeck(
            deckId = deck.deckId,
            newName = "IELTS Reading",
            newDescription = "Mô tả mới",
            newIcon = DeckStyle.ICON_BULB,
            newColor = DeckStyle.COLOR_TEAL,
            onSuccess = { updatedCalled = true },
        )
        awaitCondition { updatedCalled }

        val foundState = awaitReal {
            viewModel.observeDeckLoadState(deck.deckId).first { it is DeckLoadState.Found && it.deck.nameDeck == "IELTS Reading" }
        } as DeckLoadState.Found
        assertEquals("Mô tả mới", foundState.deck.description)
        assertEquals(DeckStyle.ICON_BULB, foundState.deck.icon)
        assertEquals(DeckStyle.COLOR_TEAL, foundState.deck.color)

        // Xoá Tổ -> từ bên trong bị xoá mềm và observeDeckLoadState trả Missing (T035)
        var deletedCalled = false
        viewModel.deleteDeck(deck.deckId, onSuccess = { deletedCalled = true })
        awaitCondition { deletedCalled }

        val missingState = awaitReal {
            viewModel.observeDeckLoadState(deck.deckId).first { it is DeckLoadState.Missing }
        }
        assertTrue(missingState is DeckLoadState.Missing)
        assertNull(db.wordDao().getById("w1"))
        val afterDelete = awaitReal { viewModel.decks.first { it.isEmpty() } }
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun factory_createsDeckViewModel_andRejectsOtherClasses() {
        val factory = DeckViewModelFactory(deckRepo, testUserId)
        val created = factory.create(DeckViewModel::class.java)
        assertNotNull(created)

        class DummyViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(DummyViewModel::class.java)
        }
    }
}
