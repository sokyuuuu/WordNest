package com.wordnest.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException

data class TranslateRequest(
    @SerializedName("text")
    val texts: List<String>,
    @SerializedName("target_lang")
    val targetLanguage: String,
    @SerializedName("source_lang")
    val sourceLanguage: String? = null
)

data class DeepLTranslation(
    @SerializedName("detected_source_language")
    val detectedSourceLanguage: String? = null,
    @SerializedName("text")
    val text: String = ""
)

data class TranslateResponse(
    @SerializedName("translations")
    val translations: List<DeepLTranslation> = emptyList()
)

enum class TargetLanguage(val code: String) {
    VIETNAMESE("VI"),
    ENGLISH("EN");

    companion object {
        fun fromCode(raw: String): TargetLanguage = when (raw.trim().uppercase()) {
            "EN", "EN-US", "EN-GB", "ENGLISH" -> ENGLISH
            else -> VIETNAMESE
        }
    }
}

class TranslationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

interface TranslationApi {
    @POST("v2/translate")
    suspend fun translate(
        @Header("Authorization") authHeader: String,
        @Body request: TranslateRequest
    ): TranslateResponse

    companion object {
        const val DEEPL_FREE_BASE_URL = "https://api-free.deepl.com/"
        const val DEEPL_PRO_BASE_URL = "https://api.deepl.com/"

        fun resolveBaseUrl(apiKey: String): String =
            if (apiKey.trim().endsWith(":fx", ignoreCase = true)) {
                DEEPL_FREE_BASE_URL
            } else {
                DEEPL_PRO_BASE_URL
            }

        fun create(baseUrl: String = DEEPL_FREE_BASE_URL): TranslationApi {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TranslationApi::class.java)
        }
    }
}

class TranslationRepository(
    private val api: TranslationApi? = null,
    private val apiKeyProvider: () -> String?
) {
    private val freeApi: TranslationApi by lazy {
        TranslationApi.create(TranslationApi.DEEPL_FREE_BASE_URL)
    }
    private val proApi: TranslationApi by lazy {
        TranslationApi.create(TranslationApi.DEEPL_PRO_BASE_URL)
    }

    private fun resolveClient(apiKey: String): TranslationApi {
        api?.let { return it }
        return if (TranslationApi.resolveBaseUrl(apiKey) == TranslationApi.DEEPL_FREE_BASE_URL) {
            freeApi
        } else {
            proApi
        }
    }

    suspend fun translate(
        texts: List<String>,
        target: TargetLanguage = TargetLanguage.VIETNAMESE,
        source: TargetLanguage? = null
    ): Result<List<String>> {
        if (texts.isEmpty() || texts.any { it.isBlank() }) {
            return Result.failure(
                TranslationException("Văn bản cần dịch không được để trống")
            )
        }

        val apiKey = apiKeyProvider()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(
                TranslationException("Chưa cấu hình DeepL API Key trong phần Cài đặt")
            )
        }

        val cleanedTexts = texts.map { it.trim() }
        val client = resolveClient(apiKey)

        return try {
            val response = client.translate(
                authHeader = "DeepL-Auth-Key $apiKey",
                request = TranslateRequest(
                    texts = cleanedTexts,
                    targetLanguage = target.code,
                    sourceLanguage = source?.code
                )
            )
            if (response.translations.size != cleanedTexts.size) {
                return Result.failure(
                    TranslationException("Số lượng kết quả dịch không khớp yêu cầu")
                )
            }
            val results = response.translations.map { it.text.trim() }
            if (results.any { it.isEmpty() }) {
                Result.failure(TranslationException("DeepL trả về bản dịch rỗng"))
            } else {
                Result.success(results)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: HttpException) {
            val msg = when (e.code()) {
                400 -> "Yêu cầu dịch không hợp lệ"
                401, 403 -> "DeepL API Key không hợp lệ hoặc bị từ chối (${e.code()})"
                429 -> "Quá giới hạn tần suất gọi DeepL API (429)"
                456 -> "Đã vượt quá hạn mức ký tự của tài khoản DeepL"
                else -> "Dịch vụ DeepL đang gặp lỗi, thử lại sau"
            }
            Result.failure(TranslationException(msg, e))
        } catch (e: IOException) {
            Result.failure(TranslationException("Không có kết nối mạng", e))
        } catch (e: Exception) {
            Result.failure(
                TranslationException(
                    e.message ?: "Lỗi không xác định khi dịch",
                    e
                )
            )
        }
    }

    suspend fun translateOne(
        text: String,
        sourceLang: String? = null,
        targetLang: String = "vi"
    ): Result<String> {
        val target = TargetLanguage.fromCode(targetLang)
        val source = sourceLang?.takeIf { it.isNotBlank() }?.let { raw ->
            val upper = raw.trim().uppercase()
            // Nếu source trùng target thì để DeepL tự phát hiện ngôn ngữ nguồn
            val parsed = TargetLanguage.fromCode(upper)
            if (parsed == target && upper != parsed.code) null else parsed
        }
        return translate(
            texts = listOf(text),
            target = target,
            source = if (source == target) null else source
        ).map { it.first() }
    }
}
