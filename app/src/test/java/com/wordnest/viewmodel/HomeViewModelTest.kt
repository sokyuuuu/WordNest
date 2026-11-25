package com.wordnest.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.wordnest.BaseDbTest
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.DeckRepository
import com.wordnest.data.repository.WordRepository
import com.wordnest.data.study.RoomStudyWordSource
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest : BaseDbTest() {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 4, 20)
    private lateinit var app: Application
    private lateinit var deckRepo: DeckRepository
    private lateinit var wordSource: RoomStudyWordSource

    @Before
    fun setUpVm() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        deckRepo = DeckRepository(
            deckDao = db.deckDao(),
            nowProvider = { "2026-04-20T10:00:00Z" },
            todayProvider = { "2026-04-20" }
        )
        wordSource = RoomStudyWordSource(db.wordDao(), userId = testUserId)
    }

    @After
    fun tearDownVm() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(studyDays: Set<LocalDate> = emptySet()) = HomeViewModel(
        application = app,
        studyDays = flowOf(studyDays),
        source = wordSource,
        deckRepository = deckRepo,
        userId = testUserId,
        todayProvider = { today },
    )

    @Test
    fun emptyDatabase_emitsZeroCountsEmptyTopDecksAndHasDecksFalse() = runTest {
        val vm = createViewModel()

        vm.dueCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        vm.flockCounts.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        vm.topDecks.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        vm.hasDecks.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun populatedDatabase_computesDueCountFlockCountsTop2NewestDecksAndEggDay() = runTest {
        db.deckDao().insertAll(
            listOf(
                Deck("d1", testUserId, "Cũ nhất", createAt = "2026-04-18T08:00:00Z"),
                Deck("d2", testUserId, "Thứ hai", createAt = "2026-04-19T08:00:00Z"),
                Deck("d3", testUserId, "Mới nhất", createAt = "2026-04-20T08:00:00Z")
            )
        )
        db.wordDao().insertAll(
            listOf(
                Word("w1", "d3", "airport", "Sân bay", status = WordStatus.EGG.storage, createAt = "2026-04-20T08:00:01Z"),
                Word("w2", "d3", "apple", "Quả táo", status = WordStatus.EGG.storage, createAt = "2026-04-20T08:00:02Z"),
                Word("w3", "d3", "book", "Cuốn sách", status = WordStatus.EGG.storage, createAt = "2026-04-20T08:00:03Z"),
                Word("w4", "d2", "computer", "Máy tính", status = WordStatus.CHICK.storage, createAt = "2026-04-20T08:00:04Z"),
                Word("w5", "d2", "hospital", "Bệnh viện", status = WordStatus.CHICK.storage, createAt = "2026-04-20T08:00:05Z"),
                Word("w6", "d1", "pen", "Cây bút", status = WordStatus.CHICK.storage, createAt = "2026-04-20T08:00:06Z"),
                Word("w7", "d1", "television", "Tivi", status = WordStatus.BIRD.storage, createAt = "2026-04-20T08:00:07Z"),
                Word("w8", "d1", "clock", "Đồng hồ", status = WordStatus.BIRD.storage, createAt = "2026-04-20T08:00:08Z")
            )
        )

        val vm = createViewModel(studyDays = setOf(today.minusDays(1), today))

        val eggDay = awaitReal { vm.eggDay.first { it == 2 } }
        assertEquals(2, eggDay)

        val due = awaitReal { vm.dueCount.first { it == 6 } }
        assertEquals(6, due)

        val flock = awaitReal { vm.flockCounts.first { it.isNotEmpty() } }
        assertEquals(3, flock[WordStatus.EGG])
        assertEquals(3, flock[WordStatus.CHICK])
        assertEquals(2, flock[WordStatus.BIRD])

        val top2 = awaitReal { vm.topDecks.first { it.size == 2 } }
        assertEquals(listOf("Mới nhất", "Thứ hai"), top2.map { it.deck.nameDeck })
        assertEquals(listOf(3, 2), top2.map { it.wordCount })

        assertTrue(awaitReal { vm.hasDecks.first { it } })

        // Xoá mềm Tổ d3 (3 từ EGG) -> dueCount giảm từ 6 xuống 3 và topDecks tự đẩy d1 lên
        deckRepo.deleteDeck("d3")

        val dueAfterDelete = awaitReal { vm.dueCount.first { it == 3 } }
        assertEquals(3, dueAfterDelete)

        val topAfterDelete = awaitReal {
            vm.topDecks.first { list -> list.map { it.deck.deckId } == listOf("d2", "d1") }
        }
        assertEquals(listOf("Thứ hai", "Cũ nhất"), topAfterDelete.map { it.deck.nameDeck })
    }

    @Test
    fun addingDeletingWordsAndDeletingAllDecks_syncsHomeDueCountFlockCountsAndHasDecks() = runTest {
        var tick = 0
        val nowProvider = {
            tick++
            "2026-04-20T10:00:%02dZ".format(tick)
        }
        val localDeckRepo = DeckRepository(db.deckDao(), nowProvider, { "2026-04-20" })
        val localWordRepo = WordRepository(db.wordDao(), db.deckDao(), nowProvider, { "2026-04-20" })
        val vm = HomeViewModel(
            application = app,
            studyDays = flowOf(emptySet()),
            source = wordSource,
            deckRepository = localDeckRepo,
            userId = testUserId,
            todayProvider = { today },
        )

        // Tạo 3 Tổ -> topDecks chỉ lấy 2 Tổ mới nhất (quickstart B2)
        val d1 = localDeckRepo.createDeck(testUserId, "Tổ 1").getOrThrow()
        val d2 = localDeckRepo.createDeck(testUserId, "Tổ 2").getOrThrow()
        val d3 = localDeckRepo.createDeck(testUserId, "Tổ 3").getOrThrow()

        val w1 = localWordRepo.addWord(d3.deckId, "airport", "Sân bay").getOrThrow()
        localWordRepo.addWord(d2.deckId, "book", "Cuốn sách").getOrThrow()

        assertTrue(awaitReal { vm.hasDecks.first { it } })
        val top2 = awaitReal {
            vm.topDecks.first { it.size == 2 && it.map { d -> d.wordCount } == listOf(1, 1) }
        }
        assertEquals(listOf("Tổ 3", "Tổ 2"), top2.map { it.deck.nameDeck })
        assertEquals(2, awaitReal { vm.dueCount.first { it == 2 } })
        assertEquals(2, awaitReal { vm.flockCounts.first { it[WordStatus.EGG] == 2 } }[WordStatus.EGG])

        // Học xong nâng w1 lên BIRD -> dueCount giảm xuống 1, BIRD = 1 (T053)
        val studyWord = awaitReal { wordSource.words().first { it.size == 2 } }.first { it.id == w1.wordId }
        wordSource.save(studyWord.copy(status = WordStatus.BIRD))
        assertEquals(1, awaitReal { vm.dueCount.first { it == 1 } })
        val flockAfterStudy = awaitReal { vm.flockCounts.first { it[WordStatus.BIRD] == 1 } }
        assertEquals(1, flockAfterStudy[WordStatus.EGG])
        assertEquals(1, flockAfterStudy[WordStatus.BIRD])

        // Xoá hết Tổ -> hasDecks = false, topDecks rỗng, dueCount = 0 (quickstart B1)
        localDeckRepo.deleteDeck(d1.deckId)
        localDeckRepo.deleteDeck(d2.deckId)
        localDeckRepo.deleteDeck(d3.deckId)
        assertFalse(awaitReal { vm.hasDecks.first { !it } })
        assertTrue(awaitReal { vm.topDecks.first { it.isEmpty() } }.isEmpty())
        assertEquals(0, awaitReal { vm.dueCount.first { it == 0 } })
    }
}
