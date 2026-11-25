package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import com.wordnest.BaseDbTest
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var tick = 0
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var travelDeckId: String
    private lateinit var homeDeckId: String

    @Before
    fun setUpVm() = runTest {
        Dispatchers.setMain(testDispatcher)
        tick = 0
        val nowProvider = {
            tick++
            "2026-04-20T10:00:%02dZ".format(tick)
        }
        deckRepo = DeckRepository(db.deckDao(), nowProvider, { "2026-04-20" })
        wordRepo = WordRepository(db.wordDao(), db.deckDao(), nowProvider, { "2026-04-20" })

        travelDeckId = deckRepo.createDeck(testUserId, "Du lịch").getOrThrow().deckId
        homeDeckId = deckRepo.createDeck(testUserId, "Nội thất").getOrThrow().deckId
        wordRepo.addWord(travelDeckId, "airport", "Sân bay", example = "I go to the airport by bus.").getOrThrow()
        wordRepo.addWord(homeDeckId, "chair", "Cái ghế", example = "Please sit on the chair.").getOrThrow()
        wordRepo.addWord(homeDeckId, "table", "Cái bàn").getOrThrow()
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialAndBlankQuery_showsSuggestionWithoutEmptyError() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId, debounceMillis = 20L)
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        val initial = vm.uiState.value
        assertTrue(initial.isQueryBlank)
        assertFalse(initial.isSearching)
        assertFalse(initial.hasSearchedAndEmpty)
        assertTrue(initial.results.isEmpty())

        vm.onQueryChange("   ")
        val blankState = vm.uiState.value
        assertTrue(blankState.isQueryBlank)
        assertFalse(blankState.hasSearchedAndEmpty)
        assertTrue(blankState.results.isEmpty())

        collector.cancel()
    }

    @Test
    fun search_partialAndCaseInsensitiveAcrossDecks_returnsDeckNamesAndClearsImmediately() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId, debounceMillis = 20L)
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        // Quickstart C10: tìm "air" ra cả "airport" (Du lịch) và "chair" (Nội thất)
        vm.onQueryChange("air")
        val partial = awaitReal {
            vm.uiState.first { it.query == "air" && !it.isSearching && it.results.size == 2 }
        }
        assertEquals(listOf("airport", "chair"), partial.results.map { it.word.term })
        assertEquals(listOf("Du lịch", "Nội thất"), partial.results.map { it.deckName })
        assertEquals(listOf(travelDeckId, homeDeckId), partial.results.map { it.word.deckId })
        assertFalse(partial.hasSearchedAndEmpty)

        // Không phân biệt hoa/thường: "AIRP" -> "airport"
        vm.onQueryChange("AIRP")
        val upper = awaitReal {
            vm.uiState.first { it.query == "AIRP" && !it.isSearching && it.results.size == 1 }
        }
        assertEquals("airport", upper.results.first().word.term)
        assertEquals("Du lịch", upper.results.first().deckName)

        // Xoá trắng ô tìm kiếm -> quay ngay về trạng thái gợi ý
        vm.clearQuery()
        val cleared = awaitReal { vm.uiState.first { it.isQueryBlank } }
        assertEquals("", cleared.query)
        assertTrue(cleared.results.isEmpty())
        assertFalse(cleared.hasSearchedAndEmpty)

        collector.cancel()
    }

    @Test
    fun search_specialCharactersPercentUnderscoreBackslash_doNotMatchAllOrCrash() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId, debounceMillis = 20L)
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        // Khi chưa có từ chứa ký tự đặc biệt: %, _, \ phải báo không tìm thấy (không trả hết 3 từ)
        for (special in listOf("%", "_", "\\")) {
            vm.onQueryChange(special)
            val state = awaitReal {
                vm.uiState.first { it.query == special && !it.isSearching }
            }
            assertTrue("Query '$special' phải trả rỗng", state.results.isEmpty())
            assertTrue("Query '$special' phải đặt hasSearchedAndEmpty", state.hasSearchedAndEmpty)
        }

        // Thêm từ thật sự chứa %, _, \ -> chỉ khớp đúng từ chứa ký tự đó (quickstart C11 + T050)
        wordRepo.addWord(travelDeckId, "discount 50%", "Giảm nửa giá").getOrThrow()
        wordRepo.addWord(homeDeckId, "snake_case", "Gạch dưới").getOrThrow()
        wordRepo.addWord(homeDeckId, "back\\slash", "Gạch chéo ngược").getOrThrow()

        vm.onQueryChange("%")
        val pct = awaitReal { vm.uiState.first { it.query == "%" && !it.isSearching && it.results.isNotEmpty() } }
        assertEquals(listOf("discount 50%"), pct.results.map { it.word.term })
        assertEquals(listOf("Du lịch"), pct.results.map { it.deckName })

        vm.onQueryChange("_")
        val us = awaitReal { vm.uiState.first { it.query == "_" && !it.isSearching && it.results.isNotEmpty() } }
        assertEquals(listOf("snake_case"), us.results.map { it.word.term })

        vm.onQueryChange("\\")
        val bs = awaitReal { vm.uiState.first { it.query == "\\" && !it.isSearching && it.results.isNotEmpty() } }
        assertEquals(listOf("back\\slash"), bs.results.map { it.word.term })

        collector.cancel()
    }

    @Test
    fun search_noMatch_setsHasSearchedAndEmpty() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId, debounceMillis = 20L)
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        vm.onQueryChange("khongtontai123")
        val state = awaitReal {
            vm.uiState.first { it.query == "khongtontai123" && !it.isSearching }
        }
        assertFalse(state.isQueryBlank)
        assertTrue(state.results.isEmpty())
        assertTrue(state.hasSearchedAndEmpty)

        collector.cancel()
    }

    @Test
    fun search_defaultDebounce200ms_doesNotFlashEmptyWhileTypingAndSettlesOnLatest() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId) // DEFAULT_DEBOUNCE_MS = 200L
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        vm.onQueryChange("z")
        vm.onQueryChange("zz")
        vm.onQueryChange("air")

        // Ngay khi gõ xong chưa hết 200ms: đang ở trạng thái isSearching, KHÔNG bật hasSearchedAndEmpty
        val pending = vm.uiState.value
        assertEquals("air", pending.query)
        assertTrue(pending.isSearching)
        assertFalse(pending.hasSearchedAndEmpty)

        // Sau 200ms debounce, trả về đúng kết quả của truy vấn cuối "air"
        val settled = awaitReal {
            vm.uiState.first { it.query == "air" && !it.isSearching && it.results.size == 2 }
        }
        assertEquals(listOf("airport", "chair"), settled.results.map { it.word.term })

        collector.cancel()
    }

    @Test
    fun search_updatesLiveWhenWordOrDeckSoftDeleted() = runTest {
        val vm = SearchViewModel(wordRepo, userId = testUserId, debounceMillis = 20L)
        val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

        vm.onQueryChange("air")
        val before = awaitReal {
            vm.uiState.first { it.query == "air" && !it.isSearching && it.results.size == 2 }
        }
        val chairId = before.results.first { it.word.term == "chair" }.word.wordId

        // Xoá mềm từ "chair" -> kết quả tự cập nhật còn 1 ("airport")
        assertTrue(wordRepo.deleteWord(chairId).isSuccess)
        val afterWordDelete = awaitReal {
            vm.uiState.first { it.query == "air" && !it.isSearching && it.results.size == 1 }
        }
        assertEquals(listOf("airport"), afterWordDelete.results.map { it.word.term })

        // Xoá mềm Tổ "Du lịch" -> kết quả tự cập nhật về rỗng
        assertTrue(deckRepo.deleteDeck(travelDeckId).isSuccess)
        val afterDeckDelete = awaitReal {
            vm.uiState.first { it.query == "air" && !it.isSearching && it.results.isEmpty() }
        }
        assertTrue(afterDeckDelete.hasSearchedAndEmpty)

        collector.cancel()
    }

    @Test
    fun factory_createsSearchViewModel_andRejectsOtherClasses() {
        val factory = SearchViewModelFactory(wordRepo, userId = testUserId)
        val created = factory.create(SearchViewModel::class.java)
        assertNotNull(created)

        class DummyViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(DummyViewModel::class.java)
        }
    }
}
