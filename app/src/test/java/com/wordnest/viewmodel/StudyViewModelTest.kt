package com.wordnest.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.HistoryMode
import com.wordnest.data.repository.HistoryResult
import com.wordnest.data.repository.StudyHistoryRoomRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.data.study.RoomStudyWordSource
import com.wordnest.util.ExampleHole
import com.wordnest.util.WordStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StudyViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 4, 20)
    private val fixedInstant = Instant.parse("2026-04-20T10:00:00Z")
    private lateinit var app: Application
    private lateinit var wordSource: RoomStudyWordSource
    private lateinit var historyRepo: StudyHistoryRoomRepository

    @Before
    fun setUpVm() = runTest {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        wordSource = RoomStudyWordSource(
            wordDao = db.wordDao(),
            userId = testUserId,
            nowProvider = { fixedInstant.toString() },
            zoneId = ZoneOffset.UTC
        )
        historyRepo = StudyHistoryRoomRepository(db.studyHistoryDao())

        db.deckDao().insert(
            Deck(
                deckId = "d-basic",
                userId = testUserId,
                nameDeck = "Từ vựng cơ bản",
                createAt = "2026-04-01T08:00:00Z"
            )
        )
        db.wordDao().insertAll(
            listOf(
                Word("w1", "d-basic", "airport", "Sân bay", status = WordStatus.EGG.storage, createAt = "2026-04-01T08:00:01Z", updatedAt = "2026-04-01T08:00:01Z"),
                Word("w2", "d-basic", "apple", "Quả táo", status = WordStatus.EGG.storage, createAt = "2026-04-01T08:00:02Z", updatedAt = "2026-04-01T08:00:02Z"),
                Word("w3", "d-basic", "book", "Cuốn sách", status = WordStatus.CHICK.storage, lastReviewedAt = "2026-04-19T08:00:00Z", createAt = "2026-04-01T08:00:03Z", updatedAt = "2026-04-01T08:00:03Z"),
                Word("w4", "d-basic", "computer", "Máy tính", status = WordStatus.BIRD.storage, lastReviewedAt = "2026-04-10T08:00:00Z", createAt = "2026-04-01T08:00:04Z", updatedAt = "2026-04-01T08:00:04Z"),
                Word("w5", "d-basic", "clock", "Đồng hồ", status = WordStatus.BIRD.storage, lastReviewedAt = "2026-04-19T08:00:00Z", createAt = "2026-04-01T08:00:05Z", updatedAt = "2026-04-01T08:00:05Z")
            )
        )
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    private suspend fun createLoadedViewModel(
        studyDays: Set<LocalDate> = emptySet(),
    ): StudyViewModel {
        val vm = StudyViewModel(
            application = app,
            source = wordSource,
            studyDays = flowOf(studyDays),
            studyHistoryRoom = historyRepo,
            today = today,
            clock = { fixedInstant },
        )
        awaitReal {
            vm.words.first { it.size == 5 }
        }
        return vm
    }

    @Test
    fun init_loadsWordsFromRoom_andDemotesOnlyIdleWords() = runTest {
        val vm = createLoadedViewModel()

        assertEquals(5, vm.sessionSize)
        assertEquals("w1", vm.currentCard?.id)

        // w4 bỏ quên 10 ngày (2026-04-10 -> 2026-04-20 >= 7 ngày): BIRD -> CHICK và lưu xuống Room
        awaitCondition { db.wordDao().getById("w4")?.status == WordStatus.CHICK.storage }
        val w4 = db.wordDao().getById("w4")!!
        assertEquals(WordStatus.CHICK.storage, w4.status)
        assertEquals(fixedInstant.toString(), w4.updatedAt)

        // w5 mới ôn hôm qua (1 ngày < 7): vẫn giữ BIRD và KHÔNG bị ghi đè updatedAt
        val w5 = db.wordDao().getById("w5")!!
        assertEquals(WordStatus.BIRD.storage, w5.status)
        assertEquals("2026-04-01T08:00:05Z", w5.updatedAt)
    }

    @Test
    fun finishFlashcard_promotesAndPersistsToRoom_recordsHistory_andSupportsReviewUnknown() = runTest {
        // Chuỗi 3 ngày liên tiếp -> EGG nâng lên CHICK
        val threeDayStreak = setOf(today.minusDays(2), today.minusDays(1), today)
        val vm = createLoadedViewModel(studyDays = threeDayStreak)

        // Đánh dấu: w1 thuộc, w2 chưa thuộc, w3 thuộc, w4 chưa thuộc, w5 thuộc
        vm.markCard(true)
        vm.markCard(false)
        vm.markCard(true)
        vm.markCard(false)
        vm.markCard(true)

        awaitReal {
            vm.mode.first { it == StudyMode.FLASHCARD_RESULT }
        }

        val result = vm.result.value
        assertNotNull(result)
        assertEquals(3, result!!.correctCount)
        assertEquals(5, result.totalCount)
        assertEquals(listOf("w2", "w4"), result.wrongWords.map { it.id })
        assertTrue(vm.hasUnknownWordsToReview)

        // Lịch sử StudyHistory đã ghi 5 dòng mode = flashcard (3 correct, 2 wrong)
        assertEquals(5, db.studyHistoryDao().countAll())
        val w1History = db.studyHistoryDao().getByWordId("w1")
        assertEquals(1, w1History.size)
        assertEquals(HistoryResult.CORRECT.storage, w1History.first().result)
        assertEquals(HistoryMode.FLASHCARD.storage, w1History.first().mode)
        val w2History = db.studyHistoryDao().getByWordId("w2")
        assertEquals(HistoryResult.WRONG.storage, w2History.first().result)

        // Mở lại ViewModel mới trên cùng Room DB (mô phỏng tắt app mở lại — quickstart A3):
        // w1 (EGG) đã nâng lên CHICK và tồn tại bền vững trong Room
        val reopenedVm = createLoadedViewModel(studyDays = threeDayStreak)
        val persistedW1 = reopenedVm.words.value.first { it.id == "w1" }
        assertEquals(WordStatus.CHICK, persistedW1.status)
        assertEquals(today, persistedW1.lastReviewedAt)

        // US10: Học lại chỉ gồm 2 từ chưa thuộc (w2, w4)
        vm.reviewUnknown()
        assertEquals(StudyMode.FLASHCARD, vm.mode.value)
        assertEquals(2, vm.sessionSize)
        assertEquals("w2", vm.currentCard?.id)
        vm.markCard(true)
        vm.markCard(true)
        awaitReal {
            vm.mode.first { it == StudyMode.FLASHCARD_RESULT }
        }
        assertFalse(vm.hasUnknownWordsToReview)
    }

    @Test
    fun quizFlow_recordsHistoryOncePerQuestion_andComputes60PercentFor3Correct2Wrong() = runTest {
        val vm = createLoadedViewModel()
        vm.startQuiz()
        assertEquals(StudyMode.QUIZ, vm.mode.value)
        assertEquals(5, vm.quizTotal)

        // 3 câu đầu trả lời đúng, 2 câu cuối trả lời sai -> 3/5 = 0.60 (quickstart A5)
        repeat(5) { idx ->
            val q = vm.currentQuestion!!
            val chosen = if (idx < 3) q.correctIndex else (q.correctIndex + 1) % q.options.size
            vm.answerQuestion(chosen)
            // Chọn lặp lại trên cùng câu hỏi phải bị bỏ qua (không ghi trùng history)
            vm.answerQuestion(q.correctIndex)
            vm.nextQuizQuestion()
        }

        awaitReal {
            vm.mode.first { it == StudyMode.QUIZ_RESULT }
        }
        assertEquals(3, vm.result.value?.correctCount)
        assertEquals(5, vm.result.value?.totalCount)
        assertEquals(2, vm.result.value?.wrongWords?.size)

        // Đúng 5 bản ghi QUIZ, không trùng, độ chính xác = 0.60
        awaitCondition { db.studyHistoryDao().countAll() >= 5 }
        assertEquals(5, db.studyHistoryDao().countAll())
        val accuracy = awaitReal { historyRepo.observeAccuracy().first() }
        assertEquals(0.60, accuracy, 0.0001)
    }

    @Test
    fun typeInFlow_recordsTypeInHistoryOncePerWord() = runTest {
        val vm = createLoadedViewModel()
        vm.startTypeIn()
        assertEquals(StudyMode.TYPE_IN, vm.mode.value)
        assertEquals(5, vm.typeTotal)

        // 4 từ gõ đúng, 1 từ gõ sai
        repeat(5) { idx ->
            val word = vm.currentTypeWord!!
            val input = if (idx < 4) word.term else "wrong-answer"
            vm.submitTyped(input)
            // Gửi lặp lại bị bỏ qua
            vm.submitTyped(word.term)
            vm.nextTypeWord()
        }

        awaitReal {
            vm.mode.first { it == StudyMode.QUIZ_RESULT }
        }
        assertEquals(4, vm.result.value?.correctCount)
        assertEquals(5, vm.result.value?.totalCount)
        awaitCondition { db.studyHistoryDao().countAll() >= 5 }
        assertEquals(5, db.studyHistoryDao().countAll())
        val accuracy = awaitReal { historyRepo.observeAccuracy().first() }
        assertEquals(0.80, accuracy, 0.0001)
        val firstWordEntries = db.studyHistoryDao().getByWordId("w1")
        assertEquals(1, firstWordEntries.size)
        assertEquals(HistoryMode.TYPE_IN.storage, firstWordEntries.first().mode)
    }

    @Test
    fun flashcard_doubleTapOnLastCard_doesNotDuplicateHistoryOrCorruptResult() = runTest {
        val vm = createLoadedViewModel()
        repeat(4) { vm.markCard(true) }
        // Nhấn đúp ở thẻ cuối (thẻ 5)
        vm.markCard(true)
        vm.markCard(false)

        awaitReal {
            vm.mode.first { it == StudyMode.FLASHCARD_RESULT }
        }
        assertEquals(5, vm.result.value?.correctCount)
        assertEquals(5, vm.result.value?.totalCount)
        assertTrue(vm.result.value?.wrongWords.orEmpty().isEmpty())
        assertEquals(5, db.studyHistoryDao().countAll())
    }

    @Test
    fun typeIn_currentTypeExample_replacesWordWithHoleAndReturnsNullWhenMissingOrUnmatched() = runTest {
        val wordRepo = WordRepository(db.wordDao(), db.deckDao(), { fixedInstant.toString() }, { "2026-04-20" })
        // w1: ví dụ chứa nguyên từ; w2: ví dụ AI đã có sẵn ___; w3: ví dụ không khớp nguyên từ; w4/w5: null
        assertTrue(wordRepo.updateExample("w1", "I go to the airport by bus.").isSuccess)
        assertTrue(wordRepo.updateExample("w2", "I eat an ___ every day.").isSuccess)
        assertTrue(wordRepo.updateExample("w3", "A notebook is on the desk.").isSuccess)

        val vm = createLoadedViewModel()
        assertNull(vm.currentTypeExample.value)

        vm.startTypeIn()
        val seenById = mutableMapOf<String, String?>()
        repeat(vm.typeTotal) {
            val current = vm.currentTypeWord!!
            seenById[current.id] = vm.currentTypeExample.value
            vm.submitTyped(current.term)
            vm.nextTypeWord()
        }

        assertEquals("I go to the ___ by bus.", seenById["w1"])
        assertEquals("I eat an ___ every day.", seenById["w2"])
        assertNull(seenById["w3"])
        assertNull(seenById["w4"])
        assertNull(seenById["w5"])
    }

    @Test
    fun typeIn_currentTypeExample_updatesLiveWhenWordExampleSavedViaRepository() = runTest {
        val wordRepo = WordRepository(db.wordDao(), db.deckDao(), { fixedInstant.toString() }, { "2026-04-20" })
        val vm = createLoadedViewModel()
        vm.startTypeIn()

        // Ban đầu mọi từ đều chưa có ví dụ -> currentTypeExample == null
        val current = vm.currentTypeWord!!
        assertNull(vm.currentTypeExample.value)

        // Khi lưu ví dụ mới qua WordRepository.updateExample (AI2 / sửa từ) -> currentTypeExample cập nhật live
        val generated = "Check the ${current.term} carefully."
        assertTrue(wordRepo.updateExample(current.id, generated).isSuccess)
        val expected = ExampleHole.replace(generated, current.term)
        val liveExample = awaitReal { vm.currentTypeExample.first { it == expected } }
        assertEquals("Check the ___ carefully.", liveExample)
    }
}
