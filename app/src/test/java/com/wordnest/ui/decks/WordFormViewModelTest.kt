package com.wordnest.ui.decks

import androidx.lifecycle.ViewModel
import com.wordnest.BaseDbTest
import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.WordSuggestionDetails
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
class WordFormViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var tick = 0
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var viewModel: WordFormViewModel
    private lateinit var deckId: String

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
        viewModel = WordFormViewModel(wordRepository = wordRepo)
        deckId = deckRepo.createDeck(testUserId, "IELTS").getOrThrow().deckId
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    @Test
    fun createWord_validInput_savesWithEggStatusAndTrimmedFields() = runTest {
        var savedId: String? = null
        viewModel.createWord(
            deckId = deckId,
            term = "  apple  ",
            meaning = "  Quả táo  ",
            phonetic = "  /ˈæp.əl/  ",
            example = "  I eat an apple every day.  ",
            onSuccess = { savedId = it.wordId },
        )

        awaitCondition { savedId != null }
        val stored = wordRepo.getWordById(savedId!!)
        assertNotNull(stored)
        assertEquals("apple", stored!!.term)
        assertEquals("Quả táo", stored.meaning)
        assertEquals("/ˈæp.əl/", stored.phonetic)
        assertEquals("I eat an apple every day.", stored.example)
        assertEquals(WordStatus.EGG.storage, stored.status)
        assertNull(stored.lastReviewedAt)
        assertFalse(stored.deleted)
    }

    @Test
    fun createWord_blankTermMeaningOrDuplicate_emitsErrorWithoutInserting() = runTest {
        val events = mutableListOf<WordFormEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        // Thiếu từ
        var successCalled = false
        viewModel.createWord(deckId, "   ", "Nghĩa", onSuccess = { successCalled = true })
        awaitCondition { events.any { it is WordFormEvent.ShowMessage && it.isError } }
        assertFalse(successCalled)
        assertTrue(db.wordDao().getWordsByDeck(deckId).isEmpty())

        // Thiếu nghĩa (quickstart C6)
        events.clear()
        viewModel.createWord(deckId, "apple", "   ", onSuccess = { successCalled = true })
        awaitCondition { events.any { it is WordFormEvent.ShowMessage && it.isError } }
        assertFalse(successCalled)
        assertTrue(db.wordDao().getWordsByDeck(deckId).isEmpty())

        // Trùng từ trong cùng Tổ
        var created = false
        viewModel.createWord(deckId, "apple", "Quả táo", onSuccess = { created = true })
        awaitCondition { created }

        events.clear()
        viewModel.createWord(deckId, "APPLE", "Táo", onSuccess = { successCalled = true })
        awaitCondition { events.any { it is WordFormEvent.ShowMessage && it.isError } }
        assertFalse(successCalled)
        assertEquals(1, db.wordDao().getWordsByDeck(deckId).size)

        collectJob.cancel()
    }

    @Test
    fun updateWord_preservesBirdStatusAndLastReviewedAt() = runTest {
        val created = wordRepo.addWord(deckId, "hatch", "Nở", note = "Ghi chú gốc").getOrThrow()
        db.wordDao().updateReviewProgress(
            wordId = created.wordId,
            status = WordStatus.BIRD.storage,
            correctStreak = 6,
            wrongCount = 1,
            intervalDays = 14,
            nextReviewAt = "2026-05-04",
            lastReviewedAt = "2026-04-20T10:00:00Z",
            updatedAt = "2026-04-20T10:00:00Z",
        )
        val progressed = wordRepo.getWordById(created.wordId)!!

        var updatedWordId: String? = null
        viewModel.updateWord(
            word = progressed,
            newTerm = "hatch",
            newMeaning = "Ấp nở (trứng)",
            newPhonetic = "/hætʃ/",
            newExample = "Chicks hatch from eggs.",
            onSuccess = { updatedWordId = it.wordId },
        )

        awaitCondition { updatedWordId != null }
        val reloaded = wordRepo.getWordById(created.wordId)!!
        assertEquals("Ấp nở (trứng)", reloaded.meaning)
        assertEquals("/hætʃ/", reloaded.phonetic)
        assertEquals("Chicks hatch from eggs.", reloaded.example)
        assertEquals("Ghi chú gốc", reloaded.note)
        assertEquals(WordStatus.BIRD.storage, reloaded.status)
        assertEquals(6, reloaded.correctStreak)
        assertEquals(14, reloaded.intervalDays)
        assertEquals("2026-04-20T10:00:00Z", reloaded.lastReviewedAt)
    }

    @Test
    fun updateWord_rejectsDuplicateTermInSameDeck() = runTest {
        wordRepo.addWord(deckId, "apple", "Quả táo").getOrThrow()
        val second = wordRepo.addWord(deckId, "banana", "Quả chuối").getOrThrow()

        val events = mutableListOf<WordFormEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        var successCalled = false
        viewModel.updateWord(
            word = second,
            newTerm = "APPLE",
            newMeaning = "Quả chuối",
            newPhonetic = null,
            newExample = null,
            onSuccess = { successCalled = true },
        )

        awaitCondition { events.any { it is WordFormEvent.ShowMessage && it.isError } }
        assertFalse(successCalled)
        assertEquals("banana", wordRepo.getWordById(second.wordId)?.term)
        collectJob.cancel()
    }

    @Test
    fun deleteWord_softDeletesWordRemovesFromSearchAndStudySource() = runTest {
        val word = wordRepo.addWord(deckId, "beak", "Mỏ chim").getOrThrow()
        val studySource = RoomStudyWordSource(db.wordDao(), userId = testUserId)
        assertEquals(1, awaitReal { wordRepo.observeSearchAllWordsWithDeck(testUserId, "beak").first() }.size)
        assertEquals(1, awaitReal { studySource.words().first() }.size)

        val events = mutableListOf<WordFormEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        var deletedCallback = false
        viewModel.deleteWord(word.wordId, onSuccess = { deletedCallback = true })
        awaitCondition { deletedCallback }

        assertNull(wordRepo.getWordById(word.wordId))
        assertTrue(db.wordDao().getByIdIncludingDeleted(word.wordId)!!.deleted)
        assertTrue(events.any { it is WordFormEvent.WordDeleted })
        assertTrue(awaitReal { wordRepo.observeSearchAllWordsWithDeck(testUserId, "beak").first() }.isEmpty())
        assertTrue(awaitReal { studySource.words().first() }.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun observeWordLoadState_emitsFoundThenMissingAfterSoftDelete() = runTest {
        val word = wordRepo.addWord(deckId, "beak", "Mỏ chim").getOrThrow()
        val found = awaitReal {
            viewModel.observeWordLoadState(word.wordId).first { it is WordLoadState.Found }
        } as WordLoadState.Found
        assertEquals("beak", found.word.term)

        wordRepo.deleteWord(word.wordId)
        val missing = awaitReal {
            viewModel.observeWordLoadState(word.wordId).first { it is WordLoadState.Missing }
        }
        assertTrue(missing is WordLoadState.Missing)
    }

    @Test
    fun factory_createsWordFormViewModel_andRejectsOtherClasses() {
        val factory = WordFormViewModelFactory(wordRepo)
        val created = factory.create(WordFormViewModel::class.java)
        assertNotNull(created)

        class DummyViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(DummyViewModel::class.java)
        }
    }

    @Test
    fun suggestFlow_autocompletes_fillsPhoneticAndMeaning_andTogglesChipsPreservingManualText() = runTest {
        val fakeDict = object : DictionaryRepository {
            override suspend fun suggest(prefix: String): List<String> =
                listOf("ocean", "occur").filter { it.startsWith(prefix.lowercase()) }

            override suspend fun getPhonetic(term: String): String? = "/ˈəʊ.ʃən/"
            override suspend fun getMeanings(term: String): List<String> = listOf("đại dương", "biển lớn")
            override suspend fun getExamples(term: String): List<String> =
                listOf("Plastic waste is polluting the ocean.")
        }

        val suggestVm = WordFormViewModel(
            wordRepository = wordRepo,
            dictionaryRepository = fakeDict,
            debounceMs = 20L,
        )
        val events = mutableListOf<WordFormEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            suggestVm.events.collect { events.add(it) }
        }

        // Gõ "oc" -> phát danh sách gợi ý sau debounce
        suggestVm.onTermInputChanged("oc")
        awaitCondition { suggestVm.suggestState.value.suggestions == listOf("ocean", "occur") }

        // Chạm chọn "ocean" khi ô phiên âm & nghĩa đang trống -> tự điền cả 2 và mở chip/ví dụ
        suggestVm.onSuggestionSelected("ocean", currentPhonetic = "", currentMeaning = "")
        awaitCondition { events.any { it is WordFormEvent.ApplySuggestions } }

        val apply = events.filterIsInstance<WordFormEvent.ApplySuggestions>().last()
        assertEquals("ocean", apply.term)
        assertEquals("/ˈəʊ.ʃən/", apply.phoneticToFill)
        assertEquals("đại dương", apply.meaningToFill)
        assertEquals(listOf("đại dương", "biển lớn"), suggestVm.suggestState.value.meaningOptions)
        assertEquals(listOf("đại dương"), suggestVm.suggestState.value.selectedMeanings)
        assertEquals(listOf("Plastic waste is polluting the ocean."), suggestVm.suggestState.value.exampleOptions)

        // Người dùng gõ thêm phần nghĩa riêng + bật thêm chip thứ 2 -> giữ nguyên phần tự gõ
        val manualPlusFirst = "trùng khơi; đại dương"
        val combined = suggestVm.toggleMeaningChip("biển lớn", manualPlusFirst)
        assertEquals("trùng khơi; đại dương; biển lớn", combined)

        // Bỏ chọn "đại dương" -> chỉ còn phần tự gõ + "biển lớn"
        val afterUncheck = suggestVm.toggleMeaningChip("đại dương", combined)
        assertEquals("trùng khơi; biển lớn", afterUncheck)

        collectJob.cancel()
    }

    @Test
    fun lookupAndTranslate_respectsUserEditedPhonetic_andHandlesNetworkErrorWithoutBlockingSave() = runTest {
        var shouldFail = false
        val fakeDict = object : DictionaryRepository {
            override suspend fun suggest(prefix: String): List<String> = emptyList()
            override suspend fun getPhonetic(term: String): String? = null
            override suspend fun getMeanings(term: String): List<String> = emptyList()
            override suspend fun getExamples(term: String): List<String> = emptyList()
            override suspend fun lookupDetails(term: String): WordSuggestionDetails {
                if (shouldFail) throw RuntimeException("Mất kết nối mạng")
                return WordSuggestionDetails(
                    phonetic = "/ˈæp.əl/",
                    meanings = listOf("quả táo"),
                    examples = listOf("She eats an apple."),
                )
            }
        }

        val suggestVm = WordFormViewModel(
            wordRepository = wordRepo,
            dictionaryRepository = fakeDict,
            debounceMs = 10L,
        )
        val events = mutableListOf<WordFormEvent>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            suggestVm.events.collect { events.add(it) }
        }

        // Người dùng đã tự sửa phiên âm cho "apple" -> bấm Tra & dịch KHÔNG ghi đè phiên âm của họ
        suggestVm.onPhoneticInputChanged("apple", "/custom-ipa/")
        suggestVm.lookupAndTranslate("apple", currentPhonetic = "/custom-ipa/", currentMeaning = "")
        awaitCondition { events.any { it is WordFormEvent.ApplySuggestions } }
        val applied = events.filterIsInstance<WordFormEvent.ApplySuggestions>().last()
        assertNull(applied.phoneticToFill)
        assertEquals("quả táo", applied.meaningToFill)

        // Khi mất mạng -> trạng thái Error nhưng vẫn cho lưu từ nhập tay bình thường
        shouldFail = true
        suggestVm.lookupAndTranslate("unknownword", currentPhonetic = "", currentMeaning = "")
        awaitCondition { suggestVm.suggestState.value.status is WordSuggestStatus.Error }

        var saved = false
        suggestVm.createWord(deckId, "unknownword", "Từ tự nhập", onSuccess = { saved = true })
        awaitCondition { saved }
        assertEquals(1, db.wordDao().getWordsByDeck(deckId).size)

        collectJob.cancel()
    }
}
