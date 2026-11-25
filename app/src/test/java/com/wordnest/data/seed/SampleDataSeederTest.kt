package com.wordnest.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.util.ExampleHole
import com.wordnest.util.WordStatus
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
class SampleDataSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var seeder: SampleDataSeeder

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = SampleDataSeeder(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedIfEmpty_populatesFiveDecksIncluding8StubWords_andIsIdempotent() = runTest {
        val firstRun = seeder.seedIfEmpty()
        assertTrue(firstRun)

        val decks = db.deckDao().getDecksByUser(SampleDataSeeder.DEMO_USER_ID)
        assertEquals(5, decks.size)
        assertEquals("Từ vựng cơ bản", decks.first().nameDeck)

        var totalWords = 0
        val allStatuses = mutableSetOf<String>()

        decks.forEach { deck ->
            assertTrue("Valid icon for ${deck.nameDeck}", DeckStyle.isValidIcon(deck.icon))
            assertTrue("Valid color for ${deck.nameDeck}", DeckStyle.isValidColor(deck.color))

            val words = db.wordDao().getWordsByDeck(deck.deckId)
            assertTrue("Deck '${deck.nameDeck}' must have >= 8 words", words.size >= 8)
            words.forEach { word ->
                assertTrue("Valid status for ${word.term}", WordStatus.isValidStorage(word.status))
                allStatuses.add(word.status)
            }
            totalWords += words.size
        }

        assertEquals(44, totalWords)
        assertEquals(
            setOf(WordStatus.EGG.storage, WordStatus.CHICK.storage, WordStatus.BIRD.storage),
            allStatuses
        )

        // Kiểm chứng đúng 8 từ stub v1.0 (id w1..w8, 3 egg / 3 chick / 2 bird)
        val basicWords = db.wordDao().getWordsByDeck(SampleDataSeeder.BASIC_DECK_ID).associateBy { it.wordId }
        assertEquals(setOf("w1", "w2", "w3", "w4", "w5", "w6", "w7", "w8"), basicWords.keys)
        assertEquals(3, basicWords.values.count { it.status == WordStatus.EGG.storage })
        assertEquals(3, basicWords.values.count { it.status == WordStatus.CHICK.storage })
        assertEquals(2, basicWords.values.count { it.status == WordStatus.BIRD.storage })
        assertEquals("airport", basicWords["w1"]?.term)
        assertNotNull(basicWords["w1"]?.example)
        assertEquals("clock", basicWords["w8"]?.term)
        assertNull(basicWords["w8"]?.example)

        // Chạy lần 2 không nhân đôi dữ liệu
        assertFalse(seeder.seedIfEmpty())
        assertEquals(5, db.deckDao().getDecksByUser(SampleDataSeeder.DEMO_USER_ID).size)

        // Xoá mềm toàn bộ Tổ rồi gọi lại seeder -> KHÔNG nạp lại dữ liệu mẫu (quickstart B1)
        decks.forEach { deck ->
            db.deckDao().softDeleteWithWords(deck.deckId, "2026-11-22T10:00:00Z")
        }
        assertEquals(0, db.deckDao().count())
        assertFalse(seeder.seedIfEmpty())
        assertEquals(0, db.deckDao().count())
    }

    @Test
    fun seedIfEmpty_providesAtLeastTenWordsWithReplaceableExamplesForTypeIn() = runTest {
        assertTrue(seeder.seedIfEmpty())
        val decks = db.deckDao().getDecksByUser(SampleDataSeeder.DEMO_USER_ID)
        val allWords = decks.flatMap { db.wordDao().getWordsByDeck(it.deckId) }

        val replacedCount = allWords.count { word ->
            val replaced = ExampleHole.replace(word.example, word.term)
            replaced != null && replaced.contains(ExampleHole.HOLE)
        }
        // SC-007: >= 10 từ mẫu có câu ví dụ thay được bằng "___"
        assertTrue("Expected >= 10 words with replaceable examples, got $replacedCount", replacedCount >= 10)
    }
}
