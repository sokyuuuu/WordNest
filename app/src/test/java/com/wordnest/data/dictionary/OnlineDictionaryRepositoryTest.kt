package com.wordnest.data.dictionary

import com.wordnest.util.ExampleHole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class OnlineDictionaryRepositoryTest {

    @Test
    fun suggest_parsesDatamuseJson_mergesOffline_andCachesResult() = runTest {
        val networkCalls = AtomicInteger(0)
        val repo = OnlineDictionaryRepository { url ->
            networkCalls.incrementAndGet()
            assertTrue(url.startsWith(OnlineDictionaryRepository.DATAMUSE_SUGGEST_URL))
            HttpResult(
                statusCode = 200,
                body = """
                    [
                      {"word": "ocean", "score": 3000},
                      {"word": "oceanic", "score": 2200},
                      {"word": "occur", "score": 1800}
                    ]
                """.trimIndent(),
            )
        }

        val first = repo.suggest("oc")
        val second = repo.suggest("  OC  ")

        assertEquals(listOf("ocean", "oceanic", "occur"), first)
        assertEquals(first, second)
        assertEquals("Should hit cache on second identical prefix", 1, networkCalls.get())
    }

    @Test
    fun suggest_whenOffline_fallsBackToLocalEntriesOrThrowsWhenNoLocalMatch() = runTest {
        val repo = OnlineDictionaryRepository {
            throw IOException("No internet")
        }

        // Tiền tố có trong OfflineDictionaryData -> trả danh sách offline mượt mà
        val fallback = repo.suggest("app")
        assertTrue(fallback.contains("apple"))
        assertTrue(fallback.contains("application"))

        // Tiền tố không có trong kho dự phòng -> ném IOException để ViewModel hiện thông báo nhẹ
        var threwIo = false
        try {
            repo.suggest("zzqx")
        } catch (_: IOException) {
            threwIo = true
        }
        assertTrue(threwIo)
    }

    @Test
    fun lookupDetails_parsesFreeDictionaryAndMyMemory_prioritizingExampleHoleSentences() = runTest {
        val repo = OnlineDictionaryRepository { url ->
            when {
                url.startsWith(OnlineDictionaryRepository.FREE_DICTIONARY_BASE_URL) -> HttpResult(
                    statusCode = 200,
                    body = """
                        [
                          {
                            "word": "lantern",
                            "phonetics": [
                              {"audio": "https://example.com/a.mp3"},
                              {"text": "/ˈlæn.tən/"}
                            ],
                            "meanings": [
                              {
                                "partOfSpeech": "noun",
                                "definitions": [
                                  {
                                    "definition": "A portable light.",
                                    "example": "Many colourful lanterns lit up the street."
                                  },
                                  {
                                    "definition": "A case for a light.",
                                    "example": "He carried a bright lantern&#39;s glow, holding the lantern high."
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                    """.trimIndent(),
                )

                url.startsWith(OnlineDictionaryRepository.MYMEMORY_TRANSLATE_URL) -> HttpResult(
                    statusCode = 200,
                    body = """
                        {
                          "responseData": {"translatedText": "đèn lồng.", "match": 1.0},
                          "matches": [
                            {"segment": "lantern", "translation": "đèn xách tay", "quality": "74", "match": 0.95},
                            {"segment": "lantern", "translation": "lantern", "quality": "74", "match": 0.90},
                            {"segment": "other", "translation": "không liên quan", "quality": "74", "match": 0.40},
                            {"segment": "lantern", "translation": "MYMEMORY WARNING: QUOTA", "quality": "74", "match": 1.0}
                          ]
                        }
                    """.trimIndent(),
                )

                else -> HttpResult(404, "")
            }
        }

        val details = repo.lookupDetails("lantern")
        assertEquals("/ˈlæn.tən/", details.phonetic)
        assertEquals(listOf("đèn lồng", "đèn xách tay"), details.meanings)
        assertEquals(2, details.examples.size)
        // Câu chứa nguyên từ "lantern" (khớp ExampleHole) được xếp lên đầu
        assertNotNull(ExampleHole.replace(details.examples.first(), "lantern"))
        assertTrue(details.examples.first().contains("lantern's"))
    }

    @Test
    fun lookupDetails_handlesFreeDictionary404ForPhrase_whileStillReturningTranslation() = runTest {
        val repo = OnlineDictionaryRepository { url ->
            when {
                url.startsWith(OnlineDictionaryRepository.FREE_DICTIONARY_BASE_URL) ->
                    HttpResult(statusCode = 404, body = """{"title":"No Definitions Found"}""")

                url.startsWith(OnlineDictionaryRepository.MYMEMORY_TRANSLATE_URL) ->
                    HttpResult(
                        statusCode = 200,
                        body = """{"responseData":{"translatedText":"chăm sóc","match":1.0},"matches":[]}""",
                    )

                else -> HttpResult(404, "")
            }
        }

        val details = repo.lookupDetails("take care of")
        assertNull(details.phonetic)
        assertEquals(listOf("chăm sóc"), details.meanings)
        assertTrue(details.examples.isEmpty())
    }

    @Test
    fun lookupDetails_whenCompletelyOffline_returnsCuratedDataForKnownWordAndThrowsForUnknown() = runTest {
        val repo = OnlineDictionaryRepository {
            throw IOException("Offline")
        }

        val known = repo.lookupDetails("ocean")
        assertEquals("/ˈəʊ.ʃən/", known.phonetic)
        assertTrue(known.meanings.contains("đại dương"))
        assertTrue(known.examples.isNotEmpty())

        var threwForUnknown = false
        try {
            repo.lookupDetails("hyperboloid99")
        } catch (_: IOException) {
            threwForUnknown = true
        }
        assertTrue(threwForUnknown)
    }
}
