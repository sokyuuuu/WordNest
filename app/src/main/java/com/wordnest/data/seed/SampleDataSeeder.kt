package com.wordnest.data.seed

import androidx.room.withTransaction
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Setting
import com.wordnest.data.local.entity.User
import com.wordnest.data.local.entity.Word
import com.wordnest.util.Constants
import com.wordnest.util.DateTimeUtil
import com.wordnest.util.WordStatus
import java.time.Instant
import java.util.UUID

/**
 * Nạp dữ liệu mẫu lần đầu mở app (idempotent khi bảng Deck chưa có bản ghi nào).
 * Gồm 1 user mặc định + 5 Tổ từ vựng (44 từ):
 *   1. "Từ vựng cơ bản"     – đúng 8 từ stub v1.0 (`w1..w8`: 3 egg, 3 chick, 2 bird)
 *   2. "Du lịch"            – 10 từ
 *   3. "IELTS"              – 10 từ
 *   4. "Công nghệ thông tin" – 8 từ
 *   5. "Các loài chim"      – 8 từ (chủ đề WordNest)
 */
class SampleDataSeeder(private val db: AppDatabase) {

    companion object {
        const val DEMO_USER_ID = Constants.DEFAULT_USER_ID
        const val DEMO_USER_EMAIL = "demo@wordnest.app"
        const val BASIC_DECK_ID = "deck-basic"
    }

    /** Idempotent: nếu bảng Deck đã từng có ít nhất 1 Tổ (kể cả đã xoá mềm) thì bỏ qua, trả về false. */
    suspend fun seedIfEmpty(): Boolean {
        val deckDao = db.deckDao()
        if (deckDao.countAllIncludingDeleted() > 0) return false

        val baseInstant = Instant.now()
        val now = baseInstant.toString()
        val today = DateTimeUtil.todayDate()
        val future3 = DateTimeUtil.plusDays(today, 3)
        val future7 = DateTimeUtil.plusDays(today, 7)

        db.withTransaction {
            // 1. User + Setting mặc định
            if (db.userDao().getById(DEMO_USER_ID) == null) {
                db.userDao().insert(
                    User(
                        userId = DEMO_USER_ID,
                        email = DEMO_USER_EMAIL,
                        passwordHash = "demo",
                        displayName = "WordNest Learner",
                        isActive = true,
                        createAt = now,
                        lastLoginAt = now
                    )
                )
                db.settingDao().upsert(
                    Setting(
                        settingId = "setting-demo-0001",
                        userId = DEMO_USER_ID,
                        reminderEnable = true,
                        reminderTime = "20:00",
                        darkMode = false,
                        ttsEnable = true,
                        autoSuggestEnabled = true
                    )
                )
            }

            // 2. Các Tổ mẫu với create_at rõ thứ tự (ORDER BY create_at DESC) và không nằm ở tương lai
            val deckBasic = Deck(
                deckId = BASIC_DECK_ID,
                userId = DEMO_USER_ID,
                nameDeck = "Từ vựng cơ bản",
                description = "8 từ vựng quen thuộc để khởi động hành trình ấp trứng cùng WordNest.",
                createAt = now,
                icon = "book",
                color = "yellow",
                updatedAt = now
            )
            val deckTravel = Deck(
                deckId = "deck-travel",
                userId = DEMO_USER_ID,
                nameDeck = "Du lịch",
                description = "Từ vựng cần thiết khi đi du lịch nước ngoài: sân bay, khách sạn, hỏi đường.",
                createAt = baseInstant.minusSeconds(1).toString(),
                icon = "plane",
                color = "mint",
                updatedAt = now
            )
            val deckIelts = Deck(
                deckId = "deck-ielts",
                userId = DEMO_USER_ID,
                nameDeck = "IELTS",
                description = "Các từ học thuật xuất hiện phổ biến trong IELTS Reading & Writing Task 2.",
                createAt = baseInstant.minusSeconds(2).toString(),
                icon = "globe",
                color = "yellow",
                updatedAt = now
            )
            val deckIt = Deck(
                deckId = "deck-it",
                userId = DEMO_USER_ID,
                nameDeck = "Công nghệ thông tin",
                description = "Thuật ngữ tiếng Anh chuyên ngành phần mềm, cơ sở dữ liệu và mạng.",
                createAt = baseInstant.minusSeconds(3).toString(),
                icon = "bulb",
                color = "grey",
                updatedAt = now
            )
            val deckBirds = Deck(
                deckId = "deck-birds",
                userId = DEMO_USER_ID,
                nameDeck = "Các loài chim",
                description = "Từ vựng chủ đề loài chim và thiên nhiên — linh hồn của WordNest.",
                createAt = baseInstant.minusSeconds(4).toString(),
                icon = "book",
                color = "teal",
                updatedAt = now
            )
            deckDao.insertAll(listOf(deckBasic, deckTravel, deckIelts, deckIt, deckBirds))

            // 3. Từ vựng mẫu (8 từ stub v1.0 đứng trước theo create_at ASC, không ghi timestamp ở tương lai)
            val basicWordCreatedAt = baseInstant.minusSeconds(2).toString()
            val extraWordCreatedAt = baseInstant.minusSeconds(1).toString()
            val words = buildList {
                // --- Từ vựng cơ bản (8 từ stub v1.0: id w1..w8, 3 egg / 3 chick / 2 bird) ---
                add(seedWord(deckBasic.deckId, "airport", "Sân bay", "/ˈeəpɔːt/",
                    "I go to the airport by bus.", WordStatus.EGG, 0, 0, today, basicWordCreatedAt,
                    wordId = "w1"))
                add(seedWord(deckBasic.deckId, "apple", "Quả táo", "/ˈæp.əl/",
                    "She eats a fresh apple every morning.", WordStatus.EGG, 0, 0, today, basicWordCreatedAt,
                    wordId = "w2"))
                add(seedWord(deckBasic.deckId, "book", "Cuốn sách", "/bʊk/",
                    "I read a good book before sleeping.", WordStatus.EGG, 0, 0, today, basicWordCreatedAt,
                    wordId = "w3"))
                add(seedWord(deckBasic.deckId, "computer", "Máy tính", "/kəmˈpjuː.tə/",
                    "He uses his computer for study every day.", WordStatus.CHICK, 3, 3, today, basicWordCreatedAt,
                    wordId = "w4"))
                add(seedWord(deckBasic.deckId, "hospital", "Bệnh viện", "/ˈhɒs.pɪ.təl/",
                    "The hospital is near the city center.", WordStatus.CHICK, 3, 3, future3, basicWordCreatedAt,
                    wordId = "w5"))
                add(seedWord(deckBasic.deckId, "pen", "Cây bút", "/pen/",
                    null, WordStatus.CHICK, 3, 3, future3, basicWordCreatedAt,
                    wordId = "w6"))
                add(seedWord(deckBasic.deckId, "television", "Tivi", "/ˈtel.ɪ.vɪ.ʒən/",
                    "We watch television together after dinner.", WordStatus.BIRD, 7, 7, future7, basicWordCreatedAt,
                    wordId = "w7"))
                add(seedWord(deckBasic.deckId, "clock", "Đồng hồ", "/klɒk/",
                    null, WordStatus.BIRD, 7, 7, future7, basicWordCreatedAt,
                    wordId = "w8"))

                // --- Du lịch (10 từ) ---
                add(seedWord(deckTravel.deckId, "itinerary", "lịch trình chuyến đi", "/aɪˈtɪnəreri/",
                    "Please check our travel itinerary before departure.", WordStatus.BIRD, 5, 7, future7, extraWordCreatedAt,
                    note = "Danh từ đếm được, số nhiều: itineraries"))
                add(seedWord(deckTravel.deckId, "boarding pass", "thẻ lên máy bay", "/ˈbɔːrdɪŋ pæs/",
                    "Show your boarding pass at the gate.", WordStatus.BIRD, 4, 7, future7, extraWordCreatedAt,
                    note = "Thường đi kèm với passport tại cổng an ninh"))
                add(seedWord(deckTravel.deckId, "accommodation", "chỗ ở, nơi lưu trú", "/əˌkɒməˈdeɪʃn/",
                    "Hotel accommodation is included in the tour package.", WordStatus.CHICK, 2, 3, today, extraWordCreatedAt,
                    note = "Chú ý gấp đôi chữ c và chữ m"))
                add(seedWord(deckTravel.deckId, "customs", "hải quan", "/ˈkʌstəmz/",
                    "It took us an hour to get through customs.", WordStatus.CHICK, 1, 1, today, extraWordCreatedAt))
                add(seedWord(deckTravel.deckId, "souvenir", "quà lưu niệm", "/ˌsuːvəˈnɪər/",
                    "She bought a small souvenir for her sister.", WordStatus.CHICK, 2, 3, future3, extraWordCreatedAt))
                add(seedWord(deckTravel.deckId, "departure", "sự khởi hành", "/dɪˈpɑːrtʃər/",
                    "The departure of flight VN123 was delayed.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt,
                    note = "Trái nghĩa: arrival"))
                add(seedWord(deckTravel.deckId, "destination", "điểm đến", "/ˌdestɪˈneɪʃn/",
                    "Da Nang is a popular tourist destination.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckTravel.deckId, "luggage", "hành lý", "/ˈlʌɡɪdʒ/",
                    "Never leave your luggage unattended.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt,
                    note = "Danh từ không đếm được (tương tự baggage)"))
                add(seedWord(deckTravel.deckId, "currency exchange", "quầy đổi tiền", "/ˈkʌrənsi ɪksˈtʃeɪdʒ/",
                    "Where is the nearest currency exchange counter?", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckTravel.deckId, "embassy", "đại sứ quán", "/ˈembəsi/",
                    "Contact your embassy if you lose your passport.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))

                // --- IELTS (10 từ) ---
                add(seedWord(deckIelts.deckId, "ubiquitous", "có mặt ở khắp nơi", "/juːˈbɪkwɪtəs/",
                    "Smartphones have become ubiquitous in modern life.", WordStatus.BIRD, 5, 7, future7, extraWordCreatedAt,
                    note = "Đồng nghĩa: omnipresent, pervasive"))
                add(seedWord(deckIelts.deckId, "mitigate", "giảm nhẹ, làm dịu bớt", "/ˈmɪtɪɡeɪt/",
                    "Planting trees helps mitigate climate change.", WordStatus.CHICK, 2, 3, today, extraWordCreatedAt,
                    note = "Hay đi với: mitigate risks / mitigate effects"))
                add(seedWord(deckIelts.deckId, "detrimental", "có hại, bất lợi", "/ˌdetrɪˈmentl/",
                    "Lack of sleep is detrimental to health.", WordStatus.CHICK, 2, 3, today, extraWordCreatedAt,
                    note = "Cấu trúc: be detrimental to something"))
                add(seedWord(deckIelts.deckId, "proliferate", "tăng nhanh, sinh sôi", "/prəˈlɪfəreɪt/",
                    "Online courses have proliferated in recent years.", WordStatus.CHICK, 1, 1, future3, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "unprecedented", "chưa từng có tiền lệ", "/ʌnˈpresɪdentɪd/",
                    "The pandemic caused unprecedented disruption.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "pragmatic", "thực tế, thực dụng", "/præɡˈmætɪk/",
                    "We need a pragmatic approach to solve this issue.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "inevitable", "không thể tránh khỏi", "/ɪnˈevɪtəbl/",
                    "Change is an inevitable part of life.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "adversity", "nghịch cảnh, khó khăn", "/ədˈvɜːrsəti/",
                    "She showed great courage in the face of adversity.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "sustainable", "bền vững", "/səˈsteɪnəbl/",
                    "Sustainable development is a global priority.", WordStatus.BIRD, 4, 7, future7, extraWordCreatedAt))
                add(seedWord(deckIelts.deckId, "scrutinize", "xem xét kỹ lưỡng", "/ˈskruːtənaɪz/",
                    "Auditors scrutinized the company's financial records.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))

                // --- Công nghệ thông tin (8 từ) ---
                add(seedWord(deckIt.deckId, "algorithm", "thuật toán", "/ˈælɡərɪðəm/",
                    "This sorting algorithm runs in O(n log n) time.", WordStatus.BIRD, 5, 7, future7, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "repository", "kho lưu trữ mã nguồn", "/rɪˈpɒzɪtəri/",
                    "Clone the Git repository before creating a new branch.", WordStatus.BIRD, 4, 7, future7, extraWordCreatedAt,
                    note = "Viết tắt thông dụng: repo"))
                add(seedWord(deckIt.deckId, "concurrency", "tính đồng thời", "/kənˈkʌrənsi/",
                    "Kotlin coroutines simplify structured concurrency.", WordStatus.CHICK, 2, 3, today, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "latency", "độ trễ", "/ˈleɪtənsi/",
                    "Low latency is critical for real-time gaming.", WordStatus.CHICK, 1, 1, today, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "idempotent", "lũy đẳng (gọi nhiều lần kết quả như 1)", "/ˌaɪdemˈpoʊtənt/",
                    "PUT and DELETE requests should be idempotent.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "deprecated", "không khuyến khích dùng nữa", "/ˈdeprəkeɪtɪd/",
                    "That API method is deprecated in Android 14.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "scalability", "khả năng mở rộng", "/ˌskeɪləˈbɪləti/",
                    "Microservices can improve system scalability.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckIt.deckId, "encryption", "mã hóa", "/ɪnˈkrɪpʃn/",
                    "End-to-end encryption protects user privacy.", WordStatus.CHICK, 2, 3, future3, extraWordCreatedAt))

                // --- Các loài chim (8 từ) ---
                add(seedWord(deckBirds.deckId, "hatch", "nở (trứng)", "/hætʃ/",
                    "The chicks take about twenty days to hatch.", WordStatus.BIRD, 5, 7, future7, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "fledgling", "chim non mới tập bay", "/ˈfledʒlɪŋ/",
                    "The fledgling hopped out of the nest for the first time.", WordStatus.CHICK, 2, 3, today, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "plumage", "bộ lông chim", "/ˈpluːmɪdʒ/",
                    "Male peacocks are famous for their colorful plumage.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "migration", "sự di cư", "/maɪˈɡreɪʃn/",
                    "Autumn is the season of bird migration.", WordStatus.CHICK, 2, 3, future3, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "beak", "mỏ chim", "/biːk/",
                    "The toucan has a large, brightly colored beak.", WordStatus.BIRD, 4, 7, future7, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "flock", "đàn chim", "/flɒk/",
                    "A flock of swallows flew across the sunset.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "perch", "đậu (trên cành cây)", "/pɜːrtʃ/",
                    "An owl perched silently on the highest branch.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
                add(seedWord(deckBirds.deckId, "wingspan", "sải cánh", "/ˈwɪŋspæn/",
                    "The wandering albatross has the largest wingspan of any bird.", WordStatus.EGG, 0, 0, today, extraWordCreatedAt))
            }
            db.wordDao().insertAll(words)
        }
        return true
    }

    private fun seedWord(
        deckId: String,
        term: String,
        meaning: String,
        phonetic: String,
        example: String?,
        status: WordStatus,
        streak: Int,
        interval: Int,
        nextReviewAt: String,
        now: String,
        note: String? = null,
        wordId: String = UUID.randomUUID().toString()
    ) = Word(
        wordId = wordId,
        deckId = deckId,
        term = term,
        meaning = meaning,
        phonetic = phonetic,
        example = example,
        note = note,
        status = status.storage,
        correctStreak = streak,
        wrongCount = if (status == WordStatus.EGG) 0 else 1,
        intervalDays = interval,
        nextReviewAt = nextReviewAt,
        lastReviewedAt = if (status == WordStatus.EGG) null else now,
        createAt = now,
        updatedAt = now
    )
}
