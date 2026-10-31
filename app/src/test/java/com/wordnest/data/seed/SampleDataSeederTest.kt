package com.wordnest.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.WordStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun seedIfEmpty_populatesDemoDecksWordsStyleAndIsIdempotent() = runTest {
        val firstRun = seeder.seedIfEmpty()
        assertTrue(firstRun)

        val decks = db.deckDao().getDecksByUser(SampleDataSeeder.DEMO_USER_ID)
        assertEquals(4, decks.size)

        var totalWords = 0
        val allStatuses = mutableSetOf<String>()

        decks.forEach { deck ->
            assertTrue("Valid icon for ${deck.nameDeck}", DeckStyle.isValidIcon(deck.icon))
            assertTrue("Valid color for ${deck.nameDeck}", DeckStyle.isValidColor(deck.color))

            val words = db.wordDao().getWordsByDeck(deck.deckId)
            assertTrue("Deck '${deck.nameDeck}' must have >= 8 words", words.size >= 8)
            words.forEach { word ->
                assertTrue("Valid status for ${word.term}", WordStatus.isValid(word.status))
                allStatuses.add(word.status)
            }
            totalWords += words.size
        }

        assertTrue("Total words must be >= 30", totalWords >= 30)
        assertEquals(setOf(WordStatus.EGG, WordStatus.CHICK, WordStatus.BIRD), allStatuses)

        // Chạy lần 2 không được nhân đôi dữ liệu
        val secondRun = seeder.seedIfEmpty()
        assertFalse(secondRun)
        assertEquals(4, db.deckDao().getDecksByUser(SampleDataSeeder.DEMO_USER_ID).size)
    }
}
