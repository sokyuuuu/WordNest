package com.wordnest.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.wordnest.BaseDbTest
import com.wordnest.data.seed.SampleDataSeeder
import com.wordnest.data.study.RoomStudyWordSource
import com.wordnest.util.Constants
import com.wordnest.util.WordStatus
import com.wordnest.viewmodel.HomeViewModel
import com.wordnest.viewmodel.StudyViewModel
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
import org.junit.Assert.assertNotEquals
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

/**
 * Kiểm thử tích hợp liên gói (GĐ 9 – T067, SC-004, Quickstart B6 & kịch bản biên):
 *  1. Hợp đồng Room ⇄ Firestore với Khoa (FB3): LWW `updated_at` ISO-8601 UTC, `status` chữ thường,
 *     xoá mềm giữ tombstone vật lý (`deleted = 1`) nhưng ẩn sạch trên mọi luồng đọc/tìm kiếm/thống kê.
 *  2. Hợp đồng tầng dữ liệu với Ngọc (ST1 + AI2): khớp số liệu Trang chủ ↔ Các Tủ (SC-004),
 *     `StudyHistoryRoomRepository` và `WordRepository.updateExample` cập nhật live `currentTypeExample` (B6).
 *  3. Kịch bản biên: xoá Tổ rồi tạo lại cùng tên → là Tổ mới hoàn toàn, không hồi sinh từ cũ.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CrossPackageIntegrationTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 11, 25)
    private lateinit var app: Application
    private var tick = 0

    private val nextIsoNow: () -> String = {
        tick++
        "2026-11-25T10:00:%02dZ".format(tick)
    }

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        tick = 0
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun firestoreContract_softDeleteAndUpdatedAtLww_preservesTombstonesWhileHidingFromAllFlows() = runTest {
        val deckRepo = DeckRepository(db.deckDao(), nextIsoNow, { "2026-11-25" })
        val wordRepo = WordRepository(db.wordDao(), db.deckDao(), nextIsoNow, { "2026-11-25" })
        val historyRepo = StudyHistoryRoomRepository(db.studyHistoryDao())
        val wordSource = RoomStudyWordSource(db.wordDao(), testUserId, nextIsoNow, ZoneOffset.UTC)
        val homeVm = HomeViewModel(
            application = app,
            studyDays = flowOf(emptySet()),
            source = wordSource,
            deckRepository = deckRepo,
            userId = testUserId,
            todayProvider = { today }
        )

        val deck = deckRepo.createDeck(testUserId, "Du lịch", "Chủ đề sân bay").getOrThrow()
        val w1 = wordRepo.addWord(deck.deckId, "airport", "Sân bay").getOrThrow()
        val w2 = wordRepo.addWord(deck.deckId, "luggage", "Hành lý").getOrThrow()

        // Mọi timestamp là ISO-8601 UTC hợp lệ và status lưu chữ thường
        assertNotNull(Instant.parse(deck.updatedAt))
        assertNotNull(Instant.parse(w1.updatedAt))
        assertEquals("egg", w1.status)

        // Sửa từ nâng updatedAt (LWW) và ghi lịch sử học
        val updatedW1 = wordRepo.updateWord(w1, "airport", "Sân bay quốc tế", "/ˈeəpɔːt/", null).getOrThrow()
        assertTrue(Instant.parse(updatedW1.updatedAt).isAfter(Instant.parse(w1.updatedAt)))

        historyRepo.record(w1.wordId, HistoryResult.WRONG, HistoryMode.QUIZ, Instant.parse("2026-11-25T10:05:00Z"))
        historyRepo.record(w2.wordId, HistoryResult.CORRECT, HistoryMode.FLASHCARD, Instant.parse("2026-11-25T10:06:00Z"))
        assertEquals(0.50, awaitReal { historyRepo.observeAccuracy().first() }, 0.0001)

        // Xoá mềm w1 -> giữ bản ghi vật lý cho đồng bộ Firestore nhưng ẩn khỏi tìm kiếm/thống kê/luồng học
        assertTrue(wordRepo.deleteWord(w1.wordId).isSuccess)
        val w1Tombstone = db.wordDao().getByIdIncludingDeleted(w1.wordId)
        assertNotNull(w1Tombstone)
        assertTrue(w1Tombstone!!.deleted)
        assertTrue(Instant.parse(w1Tombstone.updatedAt).isAfter(Instant.parse(updatedW1.updatedAt)))
        assertNull(db.wordDao().getById(w1.wordId))

        assertEquals(listOf(w2.wordId), awaitReal { wordSource.words().first { it.size == 1 } }.map { it.id })
        assertTrue(awaitReal { wordRepo.observeSearchAllWordsWithDeck(testUserId, "airport").first() }.isEmpty())
        assertEquals(1.0, awaitReal { historyRepo.observeAccuracy().first() }, 0.0001)
        assertTrue(awaitReal { historyRepo.observeWordsToReview().first() }.isEmpty())

        // Xoá mềm cả Tổ -> giữ tombstone của cả Tổ lẫn từ con, Trang chủ về trống hoàn toàn
        assertTrue(deckRepo.deleteDeck(deck.deckId).isSuccess)
        val deckTombstone = db.deckDao().getByIdIncludingDeleted(deck.deckId)
        val w2Tombstone = db.wordDao().getByIdIncludingDeleted(w2.wordId)
        assertTrue(deckTombstone != null && deckTombstone.deleted)
        assertTrue(w2Tombstone != null && w2Tombstone.deleted)
        assertFalse(awaitReal { homeVm.hasDecks.first { !it } })
        assertEquals(0, awaitReal { homeVm.dueCount.first { it == 0 } })
        assertEquals(0.0, awaitReal { historyRepo.observeAccuracy().first() }, 0.0001)
    }

    @Test
    fun ngocContract_seederHomeParity_studyHistoryAndLiveGeneratedExample_workEndToEnd() = runTest {
        assertTrue(SampleDataSeeder(db).seedIfEmpty())

        val userId = Constants.DEFAULT_USER_ID
        val deckRepo = DeckRepository(db.deckDao(), nextIsoNow, { "2026-11-25" })
        val wordRepo = WordRepository(db.wordDao(), db.deckDao(), nextIsoNow, { "2026-11-25" })
        val historyRepo = StudyHistoryRoomRepository(db.studyHistoryDao())
        val wordSource = RoomStudyWordSource(db.wordDao(), userId, nextIsoNow, ZoneOffset.UTC)
        val homeVm = HomeViewModel(
            application = app,
            studyDays = flowOf(setOf(today)),
            source = wordSource,
            deckRepository = deckRepo,
            userId = userId,
            todayProvider = { today }
        )

        // SC-004: Tổng Đàn chim & Cần ôn trên Trang chủ khớp tuyệt đối với tổng từ trên các Tổ ở tab Các Tủ
        val allDecks = awaitReal { deckRepo.observeDecksWithStats(userId).first { it.size == 5 } }
        val flock = awaitReal { homeVm.flockCounts.first { it.values.sum() == 44 } }
        val dueOnHome = awaitReal { homeVm.dueCount.first { it > 0 } }

        val totalWordsAcrossDecks = allDecks.sumOf { it.wordCount }
        val learnedAcrossDecks = allDecks.sumOf { it.learnedCount }
        assertEquals(44, totalWordsAcrossDecks)
        assertEquals(totalWordsAcrossDecks, flock.values.sum())
        assertEquals(
            learnedAcrossDecks,
            (flock[WordStatus.CHICK] ?: 0) + (flock[WordStatus.BIRD] ?: 0)
        )
        assertEquals(
            (flock[WordStatus.EGG] ?: 0) + (flock[WordStatus.CHICK] ?: 0),
            dueOnHome
        )

        // Quickstart B6 (AI2 của Ngọc): w6 ("pen", CHICK) ban đầu example = null; lưu ví dụ không đổi status/lastReviewedAt
        val penBefore = db.wordDao().getById("w6")!!
        assertEquals(WordStatus.CHICK.storage, penBefore.status)
        assertNull(penBefore.example)

        val studyVm = StudyViewModel(
            application = app,
            source = wordSource,
            studyDays = flowOf(setOf(today)),
            studyHistoryRoom = historyRepo,
            today = today,
            clock = { Instant.parse("2026-11-25T10:00:00Z") }
        )
        awaitReal { studyVm.words.first { it.size == 44 } }
        studyVm.startTypeIn()

        // Tiến tới từ "w6" ("pen") trong phiên Điền từ
        while (studyVm.currentTypeWord?.id != "w6") {
            val current = studyVm.currentTypeWord!!
            studyVm.submitTyped(current.term)
            studyVm.nextTypeWord()
        }
        assertNull(studyVm.currentTypeExample.value)

        // Mô phỏng nút AI2 của Ngọc gọi WordRepository.updateExample -> currentTypeExample cập nhật live ngay
        assertTrue(wordRepo.updateExample("w6", "Please lend me your pen for a minute.").isSuccess)
        val liveHole = awaitReal {
            studyVm.currentTypeExample.first { it == "Please lend me your ___ for a minute." }
        }
        assertEquals("Please lend me your ___ for a minute.", liveHole)

        val penAfter = db.wordDao().getById("w6")!!
        assertEquals(WordStatus.CHICK.storage, penAfter.status)
        assertEquals(penBefore.lastReviewedAt, penAfter.lastReviewedAt)

        // Mở phiên mới về sau -> câu ví dụ đã lưu sẵn trong Room, không cần gọi lại AI
        val reopenedStudyVm = StudyViewModel(
            application = app,
            source = wordSource,
            studyDays = flowOf(setOf(today)),
            studyHistoryRoom = historyRepo,
            today = today,
            clock = { Instant.parse("2026-11-25T10:10:00Z") }
        )
        val persistedPen = awaitReal { reopenedStudyVm.words.first { it.size == 44 } }.first { it.id == "w6" }
        assertEquals("Please lend me your pen for a minute.", persistedPen.example)
    }

    @Test
    fun recreatingDeckWithSameNameAfterSoftDelete_createsCleanNewDeckWithoutResurrectingOldWords() = runTest {
        val deckRepo = DeckRepository(db.deckDao(), nextIsoNow, { "2026-11-25" })
        val wordRepo = WordRepository(db.wordDao(), db.deckDao(), nextIsoNow, { "2026-11-25" })

        val firstDeck = deckRepo.createDeck(testUserId, "IELTS", "Bộ cũ").getOrThrow()
        wordRepo.addWord(firstDeck.deckId, "mitigate", "Giảm nhẹ").getOrThrow()
        wordRepo.addWord(firstDeck.deckId, "pragmatic", "Thực tế").getOrThrow()
        assertEquals(2, db.wordDao().countByDeck(firstDeck.deckId))

        // Xoá mềm Tổ cũ rồi tạo lại Tổ cùng tên "IELTS"
        assertTrue(deckRepo.deleteDeck(firstDeck.deckId).isSuccess)
        val recreatedDeck = deckRepo.createDeck(testUserId, "IELTS", "Bộ mới").getOrThrow()

        assertNotEquals(firstDeck.deckId, recreatedDeck.deckId)
        val stats = awaitReal { deckRepo.observeDecksWithStats(testUserId).first { it.size == 1 } }.single()
        assertEquals(recreatedDeck.deckId, stats.deck.deckId)
        assertEquals(0, stats.wordCount)
        assertEquals(0f, stats.progressPercent, 0.0001f)
        assertTrue(awaitReal { wordRepo.observeSearchAllWordsWithDeck(testUserId, "mitigate").first() }.isEmpty())
    }
}
