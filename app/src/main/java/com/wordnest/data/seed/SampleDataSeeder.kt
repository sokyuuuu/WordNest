package com.wordnest.data.seed

import androidx.room.withTransaction
import com.wordnest.data.local.AppDatabase
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.Setting
import com.wordnest.data.local.entity.User
import com.wordnest.data.local.entity.Word
import com.wordnest.data.local.entity.WordStatus
import com.wordnest.util.Constants
import com.wordnest.util.DateTimeUtil

/**
 * Nạp dữ liệu mẫu cho buổi demo (chỉ chạy 1 lần khi user chưa có Tổ nào).
 * Gồm 1 user demo + 4 Tổ từ vựng theo các chủ đề thiết thực:
 *   1. "Du lịch"            – 10 từ
 *   2. "IELTS"              – 10 từ
 *   3. "Công nghệ thông tin" – 8 từ
 *   4. "Các loài chim"      – 8 từ (gắn với chủ đề WordNest)
 * Các từ được gán sẵn đa dạng trạng thái (egg / chick / bird), ghi chú (note) và lịch ôn để
 * demo ngay được tiến độ và bộ đếm "đến hạn ôn".
 */
class SampleDataSeeder(private val db: AppDatabase) {

    companion object {
        const val DEMO_USER_ID = Constants.DEFAULT_USER_ID
        const val DEMO_USER_EMAIL = "demo@wordnest.app"
    }

    /** Idempotent: nếu user đã có ít nhất 1 Tổ thì bỏ qua, trả về false. */
    suspend fun seedIfEmpty(): Boolean {
        val deckDao = db.deckDao()
        if (deckDao.countByUser(DEMO_USER_ID) > 0) return false

        val now = DateTimeUtil.nowIso()
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

            // 2. Các Tổ mẫu với icon và color khớp Figma
            val deckTravel = Deck(
                deckId = "deck-travel",
                userId = DEMO_USER_ID,
                nameDeck = "Du lịch",
                description = "Từ vựng cần thiết khi đi du lịch nước ngoài: sân bay, khách sạn, hỏi đường.",
                createAt = now,
                icon = "plane",
                color = "mint"
            )
            val deckIelts = Deck(
                deckId = "deck-ielts",
                userId = DEMO_USER_ID,
                nameDeck = "IELTS",
                description = "Các từ học thuật xuất hiện phổ biến trong IELTS Reading & Writing Task 2.",
                createAt = now,
                icon = "globe",
                color = "yellow"
            )
            val deckIt = Deck(
                deckId = "deck-it",
                userId = DEMO_USER_ID,
                nameDeck = "Công nghệ thông tin",
                description = "Thuật ngữ tiếng Anh chuyên ngành phần mềm, cơ sở dữ liệu và mạng.",
                createAt = now,
                icon = "bulb",
                color = "grey"
            )
            val deckBirds = Deck(
                deckId = "deck-birds",
                userId = DEMO_USER_ID,
                nameDeck = "Các loài chim",
                description = "Từ vựng chủ đề loài chim và thiên nhiên — linh hồn của WordNest.",
                createAt = now,
                icon = "book",
                color = "teal"
            )
            deckDao.insertAll(listOf(deckTravel, deckIelts, deckIt, deckBirds))

            // 3. Từ vựng mẫu
            val words = buildList {
                // --- Du lịch (10 từ) ---
                add(seedWord(deckTravel.deckId, "itinerary", "lịch trình chuyến đi", "/aɪˈtɪnəreri/",
                    "Please check our travel itinerary before departure.", WordStatus.BIRD, 5, 7, future7, now,
                    note = "Danh từ đếm được, số nhiều: itineraries"))
                add(seedWord(deckTravel.deckId, "boarding pass", "thẻ lên máy bay", "/ˈbɔːrdɪŋ pæs/",
                    "Show your boarding pass at the gate.", WordStatus.BIRD, 4, 7, future7, now,
                    note = "Thường đi kèm với passport tại cổng an ninh"))
                add(seedWord(deckTravel.deckId, "accommodation", "chỗ ở, nơi lưu trú", "/əˌkɒməˈdeɪʃn/",
                    "Hotel accommodation is included in the tour package.", WordStatus.CHICK, 2, 3, today, now,
                    note = "Chú ý gấp đôi chữ c và chữ m"))
                add(seedWord(deckTravel.deckId, "customs", "hải quan", "/ˈkʌstəmz/",
                    "It took us an hour to get through customs.", WordStatus.CHICK, 1, 1, today, now))
                add(seedWord(deckTravel.deckId, "souvenir", "quà lưu niệm", "/ˌsuːvəˈnɪər/",
                    "She bought a small souvenir for her sister.", WordStatus.CHICK, 2, 3, future3, now))
                add(seedWord(deckTravel.deckId, "departure", "sự khởi hành", "/dɪˈpɑːrtʃər/",
                    "The departure of flight VN123 was delayed.", WordStatus.EGG, 0, 0, today, now,
                    note = "Trái nghĩa: arrival"))
                add(seedWord(deckTravel.deckId, "destination", "điểm đến", "/ˌdestɪˈneɪʃn/",
                    "Da Nang is a popular tourist destination.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckTravel.deckId, "luggage", "hành lý", "/ˈlʌɡɪdʒ/",
                    "Never leave your luggage unattended.", WordStatus.EGG, 0, 0, today, now,
                    note = "Danh từ không đếm được (tương tự baggage)"))
                add(seedWord(deckTravel.deckId, "currency exchange", "quầy đổi tiền", "/ˈkʌrənsi ɪksˈtʃeɪdʒ/",
                    "Where is the nearest currency exchange counter?", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckTravel.deckId, "embassy", "đại sứ quán", "/ˈembəsi/",
                    "Contact your embassy if you lose your passport.", WordStatus.EGG, 0, 0, today, now))

                // --- IELTS (10 từ) ---
                add(seedWord(deckIelts.deckId, "ubiquitous", "có mặt ở khắp nơi", "/juːˈbɪkwɪtəs/",
                    "Smartphones have become ubiquitous in modern life.", WordStatus.BIRD, 5, 7, future7, now,
                    note = "Đồng nghĩa: omnipresent, pervasive"))
                add(seedWord(deckIelts.deckId, "mitigate", "giảm nhẹ, làm dịu bớt", "/ˈmɪtɪɡeɪt/",
                    "Planting trees helps mitigate climate change.", WordStatus.CHICK, 2, 3, today, now,
                    note = "Hay đi với: mitigate risks / mitigate effects"))
                add(seedWord(deckIelts.deckId, "detrimental", "có hại, bất lợi", "/ˌdetrɪˈmentl/",
                    "Lack of sleep is detrimental to health.", WordStatus.CHICK, 2, 3, today, now,
                    note = "Cấu trúc: be detrimental to something"))
                add(seedWord(deckIelts.deckId, "proliferate", "tăng nhanh, sinh sôi", "/prəˈlɪfəreɪt/",
                    "Online courses have proliferated in recent years.", WordStatus.CHICK, 1, 1, future3, now))
                add(seedWord(deckIelts.deckId, "unprecedented", "chưa từng có tiền lệ", "/ʌnˈpresɪdentɪd/",
                    "The pandemic caused unprecedented disruption.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIelts.deckId, "pragmatic", "thực tế, thực dụng", "/præɡˈmætɪk/",
                    "We need a pragmatic approach to solve this issue.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIelts.deckId, "inevitable", "không thể tránh khỏi", "/ɪnˈevɪtəbl/",
                    "Change is an inevitable part of life.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIelts.deckId, "adversity", "nghịch cảnh, khó khăn", "/ədˈvɜːrsəti/",
                    "She showed great courage in the face of adversity.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIelts.deckId, "sustainable", "bền vững", "/səˈsteɪnəbl/",
                    "Sustainable development is a global priority.", WordStatus.BIRD, 4, 7, future7, now))
                add(seedWord(deckIelts.deckId, "scrutinize", "xem xét kỹ lưỡng", "/ˈskruːtənaɪz/",
                    "Auditors scrutinized the company's financial records.", WordStatus.EGG, 0, 0, today, now))

                // --- Công nghệ thông tin (8 từ) ---
                add(seedWord(deckIt.deckId, "algorithm", "thuật toán", "/ˈælɡərɪðəm/",
                    "This sorting algorithm runs in O(n log n) time.", WordStatus.BIRD, 5, 7, future7, now))
                add(seedWord(deckIt.deckId, "repository", "kho lưu trữ mã nguồn", "/rɪˈpɒzɪtəri/",
                    "Clone the Git repository before creating a new branch.", WordStatus.BIRD, 4, 7, future7, now,
                    note = "Viết tắt thông dụng: repo"))
                add(seedWord(deckIt.deckId, "concurrency", "tính đồng thời", "/kənˈkʌrənsi/",
                    "Kotlin coroutines simplify structured concurrency.", WordStatus.CHICK, 2, 3, today, now))
                add(seedWord(deckIt.deckId, "latency", "độ trễ", "/ˈleɪtənsi/",
                    "Low latency is critical for real-time gaming.", WordStatus.CHICK, 1, 1, today, now))
                add(seedWord(deckIt.deckId, "idempotent", "lũy đẳng (gọi nhiều lần kết quả như 1)", "/ˌaɪdemˈpoʊtənt/",
                    "PUT and DELETE requests should be idempotent.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIt.deckId, "deprecated", "không khuyến khích dùng nữa", "/ˈdeprəkeɪtɪd/",
                    "That API method is deprecated in Android 14.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIt.deckId, "scalability", "khả năng mở rộng", "/ˌskeɪləˈbɪləti/",
                    "Microservices can improve system scalability.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckIt.deckId, "encryption", "mã hóa", "/ɪnˈkrɪpʃn/",
                    "End-to-end encryption protects user privacy.", WordStatus.CHICK, 2, 3, future3, now))

                // --- Các loài chim (8 từ) ---
                add(seedWord(deckBirds.deckId, "hatch", "nở (trứng)", "/hætʃ/",
                    "The chicks take about twenty days to hatch.", WordStatus.BIRD, 5, 7, future7, now))
                add(seedWord(deckBirds.deckId, "fledgling", "chim non mới tập bay", "/ˈfledʒlɪŋ/",
                    "The fledgling hopped out of the nest for the first time.", WordStatus.CHICK, 2, 3, today, now))
                add(seedWord(deckBirds.deckId, "plumage", "bộ lông chim", "/ˈpluːmɪdʒ/",
                    "Male peacocks are famous for their colorful plumage.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckBirds.deckId, "migration", "sự di cư", "/maɪˈɡreɪʃn/",
                    "Autumn is the season of bird migration.", WordStatus.CHICK, 2, 3, future3, now))
                add(seedWord(deckBirds.deckId, "beak", "mỏ chim", "/biːk/",
                    "The toucan has a large, brightly colored beak.", WordStatus.BIRD, 4, 7, future7, now))
                add(seedWord(deckBirds.deckId, "flock", "đàn chim", "/flɒk/",
                    "A flock of swallows flew across the sunset.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckBirds.deckId, "perch", "đậu (trên cành cây)", "/pɜːrtʃ/",
                    "An owl perched silently on the highest branch.", WordStatus.EGG, 0, 0, today, now))
                add(seedWord(deckBirds.deckId, "wingspan", "sải cánh", "/ˈwɪŋspæn/",
                    "The wandering albatross has the largest wingspan of any bird.", WordStatus.EGG, 0, 0, today, now))
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
        example: String,
        status: String,
        streak: Int,
        interval: Int,
        nextReviewAt: String,
        now: String,
        note: String? = null
    ) = Word(
        deckId = deckId,
        term = term,
        meaning = meaning,
        phonetic = phonetic,
        example = example,
        note = note,
        status = status,
        correctStreak = streak,
        wrongCount = if (status == WordStatus.EGG) 0 else 1,
        intervalDays = interval,
        nextReviewAt = nextReviewAt,
        lastReviewAt = if (status == WordStatus.EGG) null else now,
        createAt = now
    )
}
