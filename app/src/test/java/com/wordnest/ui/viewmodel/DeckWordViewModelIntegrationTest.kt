package com.wordnest.ui.viewmodel

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration test kết hợp:
 *   Room (in-memory DB) + DeckRepository / WordRepository + DeckViewModel / WordViewModel / SearchViewModel
 * Kiểm thử trọn vẹn luồng nghiệp vụ F01 -> F07.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeckWordViewModelIntegrationTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var deckVm: DeckViewModel
    private lateinit var searchVm: SearchViewModel

    @Before
    fun setUpViewModels() {
        Dispatchers.setMain(testDispatcher)
        deckRepo = DeckRepository(db.deckDao(), { "2026-04-20T10:00:00" }, { "2026-04-20" })
        wordRepo = WordRepository(db.wordDao(), db.deckDao(), { "2026-04-20T10:00:00" }, { "2026-04-20" })
        deckVm = DeckViewModel(testUserId, deckRepo)
        searchVm = SearchViewModel(testUserId, deckRepo, wordRepo)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    private suspend fun ReceiveTurbine<List<DeckWithStats>>.awaitUntil(
        predicate: (List<DeckWithStats>) -> Boolean
    ): List<DeckWithStats> {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun fullFlow_F01_to_F07_createDeck_addWords_search_edit_delete() = runTest {
        deckVm.decks.test {
            // Initial empty list
            assertEquals(0, awaitItem().size)

            // F01: Tạo 2 Tổ với icon và color
            deckVm.createDeck("Du lịch", "Sân bay & khách sạn", icon = "plane", color = "mint")
            val afterFirst = awaitUntil { it.size == 1 }
            val travelDeck = afterFirst.first().deck
            assertEquals("plane", travelDeck.icon)
            assertEquals("mint", travelDeck.color)

            deckVm.createDeck("IELTS", "Từ học thuật", icon = "globe", color = "yellow")
            val afterSecond = awaitUntil { it.size == 2 }
            assertEquals(2, afterSecond.size)

            // F04 & F06: Thêm từ vào Tổ "Du lịch" qua WordViewModel (kèm note)
            val wordVm = WordViewModel(travelDeck.deckId, wordRepo)
            wordVm.words.test {
                assertEquals(0, awaitItem().size)

                wordVm.addWord(
                    term = "boarding pass",
                    meaning = "thẻ lên máy bay",
                    phonetic = "/ˈbɔːrdɪŋ pæs/",
                    example = "Show your boarding pass",
                    note = "Mang theo hộ chiếu"
                )
                val list1 = awaitItem()
                assertEquals(1, list1.size)
                assertEquals("Mang theo hộ chiếu", list1.first().note)

                wordVm.addWord("customs", "hải quan", "/ˈkʌstəmz/", "Go through customs")
                val list2 = awaitItem()
                assertEquals(2, list2.size)

                // F05: Sửa từ "customs"
                val customsWord = list2.first { it.term == "customs" }
                wordVm.updateWord(
                    word = customsWord,
                    newTerm = "customs",
                    newMeaning = "thuế & hải quan sân bay",
                    newPhonetic = customsWord.phonetic,
                    newExample = customsWord.example,
                    newNote = "Cửa kiểm tra hành lý"
                )
                val listAfterUpdate = awaitItem()
                val updatedCustoms = listAfterUpdate.first { it.term == "customs" }
                assertEquals("thuế & hải quan sân bay", updatedCustoms.meaning)
                assertEquals("Cửa kiểm tra hành lý", updatedCustoms.note)

                // F07 (trong Tổ): Tìm từ "boarding"
                wordVm.onSearchQueryChange("boarding")
                val filteredInDeck = awaitItem()
                assertEquals(1, filteredInDeck.size)
                assertEquals("boarding pass", filteredInDeck.first().term)

                // Bỏ lọc tìm kiếm -> quay về 2 từ
                wordVm.onSearchQueryChange("")
                assertEquals(2, awaitItem().size)

                // F05: Xóa từ "boarding pass"
                val boarding = list2.first { it.term == "boarding pass" }
                wordVm.deleteWord(boarding.wordId)
                val afterDeleteWord = awaitItem()
                assertEquals(1, afterDeleteWord.size)

                cancelAndIgnoreRemainingEvents()
            }

            // F07 (toàn cục): SearchViewModel tìm cả Tổ và Từ
            searchVm.results.test {
                awaitItem() // initial blank
                searchVm.onQueryChange("hải quan")
                val globalResult = awaitItem()
                assertEquals(1, globalResult.words.size)
                assertEquals("customs", globalResult.words.first().term)
                cancelAndIgnoreRemainingEvents()
            }

            // F02: Sửa tên, icon, color Tổ "Du lịch" -> "Du lịch Quốc tế"
            deckVm.updateDeck(
                deckId = travelDeck.deckId,
                newName = "Du lịch Quốc tế",
                newDescription = "Updated",
                newIcon = "briefcase",
                newColor = "teal"
            )
            val currentDecks = awaitUntil { list ->
                list.find { it.deck.deckId == travelDeck.deckId }?.deck?.nameDeck == "Du lịch Quốc tế"
            }
            val renamedDeck = currentDecks.find { it.deck.deckId == travelDeck.deckId }?.deck
            assertEquals("Du lịch Quốc tế", renamedDeck?.nameDeck)
            assertEquals("briefcase", renamedDeck?.icon)
            assertEquals("teal", renamedDeck?.color)

            // F02: Xóa Tổ "Du lịch Quốc tế" -> CASCADE xóa luôn các từ bên trong
            deckVm.deleteDeck(travelDeck.deckId)
            val afterDeleteDecks = awaitUntil { list -> list.size == 1 }
            assertEquals(1, afterDeleteDecks.size)
            assertEquals("IELTS", afterDeleteDecks.first().deck.nameDeck)
            assertEquals(0, db.wordDao().countByDeck(travelDeck.deckId))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createDeck_withInvalidName_emitsErrorEvent() = runTest {
        deckVm.events.test {
            deckVm.createDeck("   ", "desc")
            val event = awaitItem()
            assertTrue(event is DeckEvent.ShowMessage && event.isError)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
