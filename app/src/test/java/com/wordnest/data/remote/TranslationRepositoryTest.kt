package com.wordnest.data.remote

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

class TranslationRepositoryTest {

    private lateinit var api: TranslationApi
    private lateinit var repository: TranslationRepository
    private val testApiKey = "test-deepl-key-1234:fx"
    private val expectedAuthHeader = "DeepL-Auth-Key test-deepl-key-1234:fx"

    @Before
    fun setUp() {
        api = mockk()
        repository = TranslationRepository(
            api = api,
            apiKeyProvider = { testApiKey }
        )
    }

    @Test
    fun resolveBaseUrl_detectsFreeAndProEndpoints() {
        assertEquals(
            "https://api-free.deepl.com/",
            TranslationApi.resolveBaseUrl("abcd-1234:fx")
        )
        assertEquals(
            "https://api.deepl.com/",
            TranslationApi.resolveBaseUrl("abcd-1234-pro-key")
        )
    }

    @Test
    fun translate_missingApiKey_returnsFailureWithoutCallingApi() = runTest {
        val noKeyRepo = TranslationRepository(api = api, apiKeyProvider = { "   " })
        val result = noKeyRepo.translateOne("ubiquitous")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TranslationException)
        coVerify(exactly = 0) { api.translate(any(), any()) }
    }

    @Test
    fun translate_emptyOrBlankInput_returnsFailureWithoutCallingApi() = runTest {
        val emptyResult = repository.translate(emptyList(), TargetLanguage.VIETNAMESE)
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is TranslationException)

        val blankResult = repository.translate(listOf("hello", "   "), TargetLanguage.VIETNAMESE)
        assertTrue(blankResult.isFailure)
        assertTrue(blankResult.exceptionOrNull() is TranslationException)

        coVerify(exactly = 0) { api.translate(any(), any()) }
    }

    @Test
    fun translate_and_translateOne_happyPath_sendsDeepLAuthHeaderAndPreservesOrder() = runTest {
        coEvery {
            api.translate(
                authHeader = expectedAuthHeader,
                request = TranslateRequest(
                    texts = listOf("ubiquitous", "mitigate"),
                    targetLanguage = "VI",
                    sourceLanguage = null
                )
            )
        } returns TranslateResponse(
            translations = listOf(
                DeepLTranslation(detectedSourceLanguage = "EN", text = "có mặt khắp nơi"),
                DeepLTranslation(detectedSourceLanguage = "EN", text = "giảm nhẹ")
            )
        )

        val batchResult = repository.translate(
            texts = listOf(" ubiquitous ", " mitigate "),
            target = TargetLanguage.VIETNAMESE
        )
        assertTrue(batchResult.isSuccess)
        assertEquals(listOf("có mặt khắp nơi", "giảm nhẹ"), batchResult.getOrThrow())

        coEvery {
            api.translate(
                authHeader = expectedAuthHeader,
                request = TranslateRequest(
                    texts = listOf("xin chào"),
                    targetLanguage = "EN",
                    sourceLanguage = "VI"
                )
            )
        } returns TranslateResponse(
            translations = listOf(
                DeepLTranslation(detectedSourceLanguage = "VI", text = "hello")
            )
        )

        val singleResult = repository.translateOne(
            text = "xin chào",
            sourceLang = "vi",
            targetLang = "en"
        )
        assertTrue(singleResult.isSuccess)
        assertEquals("hello", singleResult.getOrThrow())
    }

    @Test
    fun translate_mismatchedResponseCount_returnsFailureWithTranslationException() = runTest {
        coEvery {
            api.translate(
                authHeader = expectedAuthHeader,
                request = TranslateRequest(listOf("one", "two"), "VI", null)
            )
        } returns TranslateResponse(
            translations = listOf(DeepLTranslation("EN", "một"))
        )

        val result = repository.translate(listOf("one", "two"), TargetLanguage.VIETNAMESE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TranslationException)
    }

    @Test
    fun translate_deeplHttpErrors_400_403_456_500_mapToFriendlyVietnameseMessages() = runTest {
        val codesAndMessages = listOf(
            400 to "Yêu cầu dịch không hợp lệ",
            403 to "DeepL API Key không hợp lệ hoặc bị từ chối (403)",
            456 to "Đã vượt quá hạn mức ký tự của tài khoản DeepL",
            500 to "Dịch vụ DeepL đang gặp lỗi, thử lại sau"
        )

        for ((code, expectedMsg) in codesAndMessages) {
            val httpErr = mockk<HttpException>(relaxed = true) {
                every { code() } returns code
            }
            coEvery { api.translate(any(), any()) } throws httpErr

            val result = repository.translateOne("test")
            assertTrue(result.isFailure)
            assertEquals(expectedMsg, result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun translate_networkIoException_mapsToOfflineError() = runTest {
        coEvery { api.translate(any(), any()) } throws IOException("Timeout")

        val result = repository.translateOne("hello")
        assertTrue(result.isFailure)
        assertEquals("Không có kết nối mạng", result.exceptionOrNull()?.message)
    }
}
