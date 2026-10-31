package com.wordnest.ui.viewmodel

import com.wordnest.data.dictionary.DictionaryRepository
import com.wordnest.data.dictionary.FakeDictionaryRepository
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.WordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDictionary: FakeDictionaryRepository
    private lateinit var wordRepository: WordRepository
    private val deckId = "deck-1"

    /** Spy wrapper quanh FakeDictionaryRepository để đếm số lần gọi suggest(). */
    private class CountingDictionaryRepository(
        private val delegate: DictionaryRepository
    ) : DictionaryRepository by delegate {
        var suggestCallCount = 0
            private set

        override suspend fun suggest(prefix: String): List<String> {
            suggestCallCount++
            return delegate.suggest(prefix)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDictionary = FakeDictionaryRepository(minDelayMs = 50, maxDelayMs = 50)
        wordRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(
        dictionary: DictionaryRepository = fakeDictionary,
        autoSuggestEnabled: Boolean = true
    ) = AddWordViewModel(
        deckId = deckId,
        dictionary = dictionary,
        wordRepository = wordRepository,
        autoSuggestEnabled = autoSuggestEnabled,
        debounceMs = 300L
    )

    @Test
    fun typeLessThan2Chars_doesNotCallSuggest() = runTest(testDispatcher) {
        val countingDict = CountingDictionaryRepository(fakeDictionary)
        val vm = createVm(dictionary = countingDict)

        vm.onTermChange("a")
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals(0, countingDict.suggestCallCount)
        assertTrue(vm.state.value.suggestions.isEmpty())
    }

    @Test
    fun typeAtLeast2Chars_afterDebounce_returnsSuggestions() = runTest(testDispatcher) {
        val vm = createVm()

        vm.onTermChange("ap")
        // Chưa qua 300ms debounce -> chưa có gợi ý
        advanceTimeBy(200)
        assertTrue(vm.state.value.suggestions.isEmpty())

        // Qua debounce 300ms + delay fake 50ms -> có danh sách gợi ý bắt đầu bằng "ap"
        advanceUntilIdle()
        val suggestions = vm.state.value.suggestions
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.containsAll(listOf("apple", "apply", "appliance")))
    }

    @Test
    fun selectSuggestion_populatesPhoneticMeaningOptionsAndExamples() = runTest(testDispatcher) {
        val vm = createVm()

        vm.onSuggestionSelected("apple")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("apple", state.term)
        assertEquals("/ˈæp.əl/", state.phonetic)
        assertEquals(listOf("quả táo", "cây táo", "táo tây"), state.meaningOptions)
        assertEquals(2, state.exampleOptions.size)
        assertTrue(state.suggestions.isEmpty())
        assertEquals(SuggestStatus.Idle, state.uiState)
    }

    @Test
    fun userEditedPhonetic_reselectingSameWord_doesNotOverwritePhonetic_butDifferentWordUpdatesIt() = runTest(testDispatcher) {
        val vm = createVm()

        vm.onSuggestionSelected("apple")
        advanceUntilIdle()
        assertEquals("/ˈæp.əl/", vm.state.value.phonetic)

        // Người dùng tự sửa phiên âm
        vm.onPhoneticChange("/custom-apple/")
        assertEquals("/custom-apple/", vm.state.value.phonetic)

        // Chọn lại hoặc nhấn Done trên cùng từ "apple" -> KHÔNG ghi đè phiên âm người dùng đã sửa
        vm.onTermDone()
        advanceUntilIdle()
        assertEquals("/custom-apple/", vm.state.value.phonetic)

        // Đổi sang từ khác ("book") -> cập nhật phiên âm của từ mới
        vm.onSuggestionSelected("book")
        advanceUntilIdle()
        assertEquals("/bʊk/", vm.state.value.phonetic)
    }

    @Test
    fun toggleMeaningChips_joinsSelectedMeaningsWithSemicolon() = runTest(testDispatcher) {
        val vm = createVm()

        vm.onSuggestionSelected("apple")
        advanceUntilIdle()

        // Chọn 2 chip
        vm.onMeaningToggled("quả táo")
        assertEquals("quả táo", vm.state.value.meaning)

        vm.onMeaningToggled("cây táo")
        assertEquals("quả táo; cây táo", vm.state.value.meaning)
        assertEquals(listOf("quả táo", "cây táo"), vm.state.value.selectedMeanings)

        // Bỏ chọn chip đầu tiên
        vm.onMeaningToggled("quả táo")
        assertEquals("cây táo", vm.state.value.meaning)
        assertEquals(listOf("cây táo"), vm.state.value.selectedMeanings)
    }

    @Test
    fun repositoryThrowsError_setsErrorUiState_andDoesNotBlockManualSave() = runTest(testDispatcher) {
        val vm = createVm()

        // Gõ từ đặc biệt "error" gây lỗi từ điển
        vm.onTermChange("error")
        advanceUntilIdle()

        val stateAfterError = vm.state.value
        assertTrue(stateAfterError.uiState is SuggestStatus.Error)
        // Chưa nhập nghĩa -> canSave = false
        assertFalse(stateAfterError.canSave)

        // Người dùng tự gõ nghĩa -> canSave = true dù gợi ý đang báo lỗi
        vm.onMeaningChange("lỗi hệ thống")
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun canSave_isFalseWhenTermOrMeaningBlank_andTrueWhenBothFilled() = runTest(testDispatcher) {
        val vm = createVm()

        assertFalse(vm.state.value.canSave)

        vm.onTermChange("brave")
        assertFalse(vm.state.value.canSave)

        vm.onMeaningChange("dũng cảm")
        assertTrue(vm.state.value.canSave)

        vm.onTermChange("   ")
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun autoSuggestDisabled_neverCallsDictionary_andAllowsManualEntry() = runTest(testDispatcher) {
        val countingDict = CountingDictionaryRepository(fakeDictionary)
        val vm = createVm(dictionary = countingDict, autoSuggestEnabled = false)

        vm.onTermChange("apple")
        vm.onTermDone()
        advanceUntilIdle()

        assertEquals(0, countingDict.suggestCallCount)
        assertTrue(vm.state.value.suggestions.isEmpty())
        assertTrue(vm.state.value.meaningOptions.isEmpty())
        assertEquals("", vm.state.value.phonetic)
    }

    @Test
    fun onSave_callsWordRepositoryAddWordWithDeckId_andMarksSaved() = runTest(testDispatcher) {
        coEvery {
            wordRepository.addWord(
                deckId = deckId,
                term = "brave",
                meaning = "dũng cảm",
                phonetic = "/breɪv/",
                example = "Be brave."
            )
        } returns Result.success(
            Word(
                wordId = "w-1",
                deckId = deckId,
                term = "brave",
                meaning = "dũng cảm",
                phonetic = "/breɪv/",
                example = "Be brave.",
                createAt = "2026-10-24T20:00:00"
            )
        )

        val vm = createVm()
        vm.onTermChange("brave")
        vm.onPhoneticChange("/breɪv/")
        vm.onMeaningChange("dũng cảm")
        vm.onExampleChange("Be brave.")

        var callbackInvoked = false
        vm.onSave { callbackInvoked = true }
        advanceUntilIdle()

        assertTrue(vm.state.value.saved)
        assertTrue(callbackInvoked)
        coVerify(exactly = 1) {
            wordRepository.addWord(
                deckId = deckId,
                term = "brave",
                meaning = "dũng cảm",
                phonetic = "/breɪv/",
                example = "Be brave."
            )
        }
    }
}
