
package com.wordnest.data.remote

import com.wordnest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiRepository {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = "gemini-3.1-flash"
    suspend fun sendMessage(message: String): String =
        withContext(Dispatchers.IO) {

            if (apiKey.isBlank()) {
                return@withContext "Chưa cấu hình Gemini API Key."
            }

            val endpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

            var connection: HttpURLConnection? = null

            try {
                connection = URL(endpoint)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "x-goog-api-key",
                    apiKey
                )

                val instruction = """
                    Bạn là WordNest AI, trợ lý học tiếng Anh.

                    Nhiệm vụ:
                    - Giải thích từ vựng tiếng Anh.
                    - Dịch nghĩa sang tiếng Việt.
                    - Giải thích ngữ pháp.
                    - Tạo câu ví dụ tiếng Anh.
                    - Tạo bài tập điền từ với chỗ trống ___.
                    - Sửa lỗi chính tả và ngữ pháp.

                    Hãy trả lời thân thiện, dễ hiểu,
                    phù hợp với người học tiếng Anh.
                    Khi giải thích từ vựng, hãy cung cấp
                    nghĩa, loại từ và câu ví dụ.
                """.trimIndent()

                val requestBody = JSONObject().apply {

                    put(
                        "system_instruction",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    instruction
                                )
                            )
                        )
                    )

                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                put(
                                    "parts",
                                    JSONArray().put(
                                        JSONObject().put(
                                            "text",
                                            message
                                        )
                                    )
                                )
                            }
                        )
                    )

                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.7)
                            put("maxOutputTokens", 2048)
                        }
                    )
                }

                connection.outputStream.use { stream ->
                    stream.write(
                        requestBody.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                val responseCode = connection.responseCode

                if (responseCode in 200..299) {

                    val response = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val json = JSONObject(response)

                    val parts = json
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")

                    val answer = buildString {
                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val part = parts.optJSONObject(i)

                                if (part?.optBoolean("thought") != true) {
                                    append(
                                        part?.optString("text").orEmpty()
                                    )
                                }
                            }
                        }
                    }

                    answer.ifBlank {
                        "AI chưa tạo được câu trả lời. Bạn thử lại nhé!"
                    }

                } else {

                    when (responseCode) {
                        400 -> "Yêu cầu không hợp lệ. Kiểm tra cấu hình Gemini."
                        401, 403 -> "API Key không hợp lệ hoặc không có quyền truy cập."
                        404 -> "Không tìm thấy mô hình Gemini."
                        429 -> "Đã vượt giới hạn sử dụng Gemini. Vui lòng thử lại sau."
                        500, 503 -> "Máy chủ Gemini đang gặp sự cố."
                        else -> "Gemini API gặp lỗi HTTP $responseCode."
                    }
                }

            } catch (e: Exception) {
                "Không thể kết nối Gemini. Vui lòng kiểm tra Internet và thử lại."
            } finally {
                connection?.disconnect()
            }
        }

    suspend fun generateExample(word: String): String {
        return sendMessage(
            "Hãy tạo 3 câu ví dụ tiếng Anh với từ '$word', " +
                    "kèm bản dịch tiếng Việt."
        )
    }

    suspend fun generateCloze(word: String): String {
        return sendMessage(
            "Hãy tạo 3 bài tập điền từ với từ '$word'. " +
                    "Thay từ cần điền bằng ___ và đặt đáp án ở cuối."
        )
    }
}
