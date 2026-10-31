package com.wordnest.data.dictionary

import kotlinx.coroutines.delay

/**
 * Giao diện tra cứu từ điển khi thêm từ mới:
 * - Gợi ý autocomplete theo prefix
 * - Lấy phiên âm IPA
 * - Lấy danh sách nghĩa tiếng Việt gợi ý
 * - Lấy danh sách câu ví dụ gợi ý
 */
interface DictionaryRepository {
    suspend fun suggest(prefix: String): List<String>
    suspend fun getPhonetic(term: String): String?
    suspend fun getMeanings(term: String): List<String>
    suspend fun getExamples(term: String): List<String>
}

private data class DictItem(
    val term: String,
    val phonetic: String,
    val meanings: List<String>,
    val examples: List<String>
)

/**
 * Từ điển giả lập nội bộ hoạt động offline, hỗ trợ debounce test với độ trễ tuỳ chỉnh.
 */
class FakeDictionaryRepository(
    private val minDelayMs: Long = 100L,
    private val maxDelayMs: Long = 200L
) : DictionaryRepository {

    private val entries: List<DictItem> = listOf(
        DictItem(
            term = "apple",
            phonetic = "/ˈæp.əl/",
            meanings = listOf("quả táo", "cây táo", "táo tây"),
            examples = listOf(
                "She eats an apple every morning.",
                "An apple a day keeps the doctor away."
            )
        ),
        DictItem(
            term = "apply",
            phonetic = "/əˈplaɪ/",
            meanings = listOf("ứng tuyển", "áp dụng", "bôi / thoa"),
            examples = listOf(
                "He decided to apply for the software engineer position.",
                "This rule does not apply to beginners."
            )
        ),
        DictItem(
            term = "application",
            phonetic = "/ˌæp.lɪˈkeɪ.ʃən/",
            meanings = listOf("ứng dụng", "đơn xin việc", "sự áp dụng"),
            examples = listOf(
                "WordNest is a vocabulary learning application.",
                "Please submit your application before Friday."
            )
        ),
        DictItem(
            term = "appliance",
            phonetic = "/əˈplaɪ.əns/",
            meanings = listOf("thiết bị gia dụng", "đồ điện trong nhà"),
            examples = listOf(
                "Modern kitchen appliances save a lot of time."
            )
        ),
        DictItem(
            term = "book",
            phonetic = "/bʊk/",
            meanings = listOf("quyển sách", "đặt chỗ / đặt vé"),
            examples = listOf(
                "I am reading a fascinating book about nature.",
                "We should book a table in advance."
            )
        ),
        DictItem(
            term = "run",
            phonetic = "/rʌn/",
            meanings = listOf("chạy", "vận hành", "điều hành"),
            examples = listOf(
                "They run five kilometers every morning.",
                "She runs her own small business."
            )
        ),
        DictItem(
            term = "water",
            phonetic = "/ˈwɔː.tər/",
            meanings = listOf("nước", "tưới nước"),
            examples = listOf(
                "Remember to drink enough water every day.",
                "Could you water the plants while I'm away?"
            )
        ),
        DictItem(
            term = "happy",
            phonetic = "/ˈhæp.i/",
            meanings = listOf("hạnh phúc", "vui vẻ", "hài lòng"),
            examples = listOf(
                "She felt happy after finishing her vocabulary review."
            )
        ),
        DictItem(
            term = "learn",
            phonetic = "/lɜːn/",
            meanings = listOf("học", "tìm hiểu", "ghi nhớ"),
            examples = listOf(
                "Learning a few new words every day builds a strong vocabulary."
            )
        ),
        DictItem(
            term = "friend",
            phonetic = "/frend/",
            meanings = listOf("người bạn", "bạn bè"),
            examples = listOf(
                "A good friend always supports you in difficult times."
            )
        ),
        DictItem(
            term = "time",
            phonetic = "/taɪm/",
            meanings = listOf("thời gian", "lần", "thời điểm"),
            examples = listOf(
                "Take your time to review each flashcard carefully."
            )
        ),
        DictItem(
            term = "world",
            phonetic = "/wɜːld/",
            meanings = listOf("thế giới", "hoàn cầu"),
            examples = listOf(
                "English helps you connect with people around the world."
            )
        ),
        DictItem(
            term = "light",
            phonetic = "/laɪt/",
            meanings = listOf("ánh sáng", "đèn", "nhẹ nhàng"),
            examples = listOf(
                "Morning light streamed through the window."
            )
        ),
        DictItem(
            term = "nest",
            phonetic = "/nest/",
            meanings = listOf("tổ chim", "nơi ấm áp", "làm tổ"),
            examples = listOf(
                "The bird built a cozy nest in the tall tree."
            )
        ),
        DictItem(
            term = "resilient",
            phonetic = "/rɪˈzɪl.i.ənt/",
            meanings = listOf("kiên cường", "có khả năng phục hồi nhanh"),
            examples = listOf(
                "Children are often more resilient than adults."
            )
        ),
        DictItem(
            term = "sustainable",
            phonetic = "/səˈsteɪ.nə.bəl/",
            meanings = listOf("bền vững", "thân thiện với môi trường"),
            examples = listOf(
                "Sustainable energy is essential for our future."
            )
        ),
        DictItem(
            term = "algorithm",
            phonetic = "/ˈæl.ɡə.rɪð.əm/",
            meanings = listOf("thuật toán"),
            examples = listOf(
                "Spaced repetition uses an algorithm to schedule reviews."
            )
        ),
        DictItem(
            term = "serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            meanings = listOf("sự tình cờ may mắn", "cơ duyên"),
            examples = listOf(
                "Finding that rare book was pure serendipity."
            )
        )
    )

    private val byKey: Map<String, DictItem> = entries.associateBy { it.term.lowercase() }

    private suspend fun simulateDelay() {
        val wait = if (maxDelayMs > minDelayMs) minDelayMs else maxDelayMs
        if (wait > 0) delay(wait)
    }

    private fun checkSimulatedError(input: String) {
        if (input.trim().equals("error", ignoreCase = true)) {
            throw RuntimeException("Không thể kết nối từ điển")
        }
    }

    override suspend fun suggest(prefix: String): List<String> {
        val normalized = prefix.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        simulateDelay()
        checkSimulatedError(normalized)
        return entries
            .map { it.term }
            .filter { it.lowercase().startsWith(normalized) }
            .take(8)
    }

    override suspend fun getPhonetic(term: String): String? {
        val normalized = term.trim().lowercase()
        if (normalized.isEmpty()) return null
        simulateDelay()
        checkSimulatedError(normalized)
        return byKey[normalized]?.phonetic
    }

    override suspend fun getMeanings(term: String): List<String> {
        val normalized = term.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        simulateDelay()
        checkSimulatedError(normalized)
        return byKey[normalized]?.meanings ?: emptyList()
    }

    override suspend fun getExamples(term: String): List<String> {
        val normalized = term.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        simulateDelay()
        checkSimulatedError(normalized)
        return byKey[normalized]?.examples ?: emptyList()
    }
}
