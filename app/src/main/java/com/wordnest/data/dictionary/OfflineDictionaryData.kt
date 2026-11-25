package com.wordnest.data.dictionary

/**
 * Kho từ điển nội bộ dùng làm nguồn dự phòng khi thiết bị ngoại tuyến (offline)
 * và bổ sung thêm các nghĩa tiếng Việt chọn lọc cho các từ thông dụng.
 */
internal object OfflineDictionaryData {

    data class Entry(
        val term: String,
        val phonetic: String,
        val meanings: List<String>,
        val examples: List<String>,
    )

    private val entries: List<Entry> = listOf(
        Entry(
            term = "apple",
            phonetic = "/ˈæp.əl/",
            meanings = listOf("quả táo", "cây táo"),
            examples = listOf(
                "She eats a fresh apple every morning.",
                "An apple a day keeps the doctor away.",
            ),
        ),
        Entry(
            term = "apply",
            phonetic = "/əˈplaɪ/",
            meanings = listOf("ứng tuyển", "áp dụng", "nộp đơn"),
            examples = listOf(
                "He decided to apply for the software engineer position.",
                "This rule does not apply to beginners.",
            ),
        ),
        Entry(
            term = "application",
            phonetic = "/ˌæp.lɪˈkeɪ.ʃən/",
            meanings = listOf("ứng dụng", "đơn xin việc", "sự áp dụng"),
            examples = listOf(
                "WordNest is a vocabulary learning application.",
                "Please submit your application before Friday.",
            ),
        ),
        Entry(
            term = "appliance",
            phonetic = "/əˈplaɪ.əns/",
            meanings = listOf("thiết bị gia dụng", "đồ điện trong nhà"),
            examples = listOf("Modern kitchen appliances save a lot of time."),
        ),
        Entry(
            term = "airport",
            phonetic = "/ˈeəpɔːt/",
            meanings = listOf("sân bay", "phi trường"),
            examples = listOf(
                "I go to the airport by bus.",
                "We arrived at the airport two hours early.",
            ),
        ),
        Entry(
            term = "algorithm",
            phonetic = "/ˈæl.ɡə.rɪð.əm/",
            meanings = listOf("thuật toán", "giải thuật"),
            examples = listOf("This sorting algorithm runs very fast."),
        ),
        Entry(
            term = "book",
            phonetic = "/bʊk/",
            meanings = listOf("quyển sách", "đặt chỗ", "đặt vé"),
            examples = listOf(
                "I read a good book before sleeping.",
                "We should book a table in advance.",
            ),
        ),
        Entry(
            term = "clock",
            phonetic = "/klɒk/",
            meanings = listOf("đồng hồ treo tường", "đồng hồ để bàn"),
            examples = listOf("The old clock on the wall still works."),
        ),
        Entry(
            term = "computer",
            phonetic = "/kəmˈpjuː.tə/",
            meanings = listOf("máy tính", "máy vi tính"),
            examples = listOf("He uses his computer for study every day."),
        ),
        Entry(
            term = "departure",
            phonetic = "/dɪˈpɑːrtʃər/",
            meanings = listOf("sự khởi hành", "giờ khởi hành"),
            examples = listOf("The departure of our flight was on time."),
        ),
        Entry(
            term = "destination",
            phonetic = "/ˌdestɪˈneɪʃn/",
            meanings = listOf("điểm đến", "nơi đến"),
            examples = listOf("Da Nang is a popular tourist destination."),
        ),
        Entry(
            term = "environment",
            phonetic = "/ɪnˈvaɪ.rən.mənt/",
            meanings = listOf("môi trường", "hoàn cảnh sống"),
            examples = listOf("Protecting the natural environment is everyone's duty."),
        ),
        Entry(
            term = "family",
            phonetic = "/ˈfæm.əl.i/",
            meanings = listOf("gia đình", "họ hàng"),
            examples = listOf("My family gathers for dinner every Sunday."),
        ),
        Entry(
            term = "flock",
            phonetic = "/flɒk/",
            meanings = listOf("đàn chim", "bầy đàn", "tụ tập"),
            examples = listOf("A flock of swallows flew across the sunset."),
        ),
        Entry(
            term = "friend",
            phonetic = "/frend/",
            meanings = listOf("người bạn", "bạn bè"),
            examples = listOf("A good friend always supports you in difficult times."),
        ),
        Entry(
            term = "happy",
            phonetic = "/ˈhæp.i/",
            meanings = listOf("hạnh phúc", "vui vẻ", "hài lòng"),
            examples = listOf("She felt happy after finishing her vocabulary review."),
        ),
        Entry(
            term = "hatch",
            phonetic = "/hætʃ/",
            meanings = listOf("nở (trứng)", "ấp nở"),
            examples = listOf("The chicks take about twenty days to hatch."),
        ),
        Entry(
            term = "hello",
            phonetic = "/həˈləʊ/",
            meanings = listOf("xin chào", "chào bạn"),
            examples = listOf("Say hello to your new classmates."),
        ),
        Entry(
            term = "hospital",
            phonetic = "/ˈhɒs.pɪ.təl/",
            meanings = listOf("bệnh viện"),
            examples = listOf("The hospital is near the city center."),
        ),
        Entry(
            term = "journey",
            phonetic = "/ˈdʒɜː.ni/",
            meanings = listOf("hành trình", "chuyến đi xa"),
            examples = listOf("Learning a language is an exciting journey."),
        ),
        Entry(
            term = "knowledge",
            phonetic = "/ˈnɒl.ɪdʒ/",
            meanings = listOf("kiến thức", "tri thức", "sự hiểu biết"),
            examples = listOf("Reading books expands our knowledge."),
        ),
        Entry(
            term = "learn",
            phonetic = "/lɜːn/",
            meanings = listOf("học", "tìm hiểu", "ghi nhớ"),
            examples = listOf("We learn five new English words every morning."),
        ),
        Entry(
            term = "library",
            phonetic = "/ˈlaɪ.brər.i/",
            meanings = listOf("thư viện"),
            examples = listOf("Students study quietly in the university library."),
        ),
        Entry(
            term = "light",
            phonetic = "/laɪt/",
            meanings = listOf("ánh sáng", "đèn", "nhẹ nhàng"),
            examples = listOf("Morning light streamed through the window."),
        ),
        Entry(
            term = "luggage",
            phonetic = "/ˈlʌɡɪdʒ/",
            meanings = listOf("hành lý"),
            examples = listOf("Never leave your luggage unattended."),
        ),
        Entry(
            term = "mitigate",
            phonetic = "/ˈmɪtɪɡeɪt/",
            meanings = listOf("giảm nhẹ", "làm dịu bớt"),
            examples = listOf("Planting trees helps mitigate climate change."),
        ),
        Entry(
            term = "music",
            phonetic = "/ˈmjuː.zɪk/",
            meanings = listOf("âm nhạc", "bản nhạc"),
            examples = listOf("Soft music helps me relax after class."),
        ),
        Entry(
            term = "nest",
            phonetic = "/nest/",
            meanings = listOf("tổ chim", "nơi ấm áp", "làm tổ"),
            examples = listOf("The bird built a cozy nest in the tall tree."),
        ),
        Entry(
            term = "ocean",
            phonetic = "/ˈəʊ.ʃən/",
            meanings = listOf("đại dương", "biển lớn"),
            examples = listOf("Plastic waste is polluting the ocean."),
        ),
        Entry(
            term = "pen",
            phonetic = "/pen/",
            meanings = listOf("cây bút", "bút mực"),
            examples = listOf("Please sign the form with a blue pen."),
        ),
        Entry(
            term = "pragmatic",
            phonetic = "/præɡˈmætɪk/",
            meanings = listOf("thực tế", "thực dụng"),
            examples = listOf("We need a pragmatic approach to solve this issue."),
        ),
        Entry(
            term = "resilient",
            phonetic = "/rɪˈzɪl.i.ənt/",
            meanings = listOf("kiên cường", "bền bỉ", "phục hồi nhanh"),
            examples = listOf("Birds are resilient creatures in winter."),
        ),
        Entry(
            term = "run",
            phonetic = "/rʌn/",
            meanings = listOf("chạy", "vận hành", "điều hành"),
            examples = listOf(
                "They run five kilometers every morning.",
                "She likes to run in the park.",
            ),
        ),
        Entry(
            term = "school",
            phonetic = "/skuːl/",
            meanings = listOf("trường học", "buổi học"),
            examples = listOf("The children walk to school together."),
        ),
        Entry(
            term = "science",
            phonetic = "/ˈsaɪ.əns/",
            meanings = listOf("khoa học"),
            examples = listOf("Modern science has improved human health."),
        ),
        Entry(
            term = "serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            meanings = listOf("sự tình cờ may mắn", "cơ duyên"),
            examples = listOf("Finding that rare book was pure serendipity."),
        ),
        Entry(
            term = "student",
            phonetic = "/ˈstjuː.dənt/",
            meanings = listOf("sinh viên", "học sinh"),
            examples = listOf("Every student prepared carefully for the exam."),
        ),
        Entry(
            term = "sustainable",
            phonetic = "/səˈsteɪ.nə.bəl/",
            meanings = listOf("bền vững", "thân thiện với môi trường"),
            examples = listOf("Sustainable energy is essential for our future."),
        ),
        Entry(
            term = "teacher",
            phonetic = "/ˈtiː.tʃər/",
            meanings = listOf("giáo viên", "giảng viên"),
            examples = listOf("Our English teacher explained the grammar clearly."),
        ),
        Entry(
            term = "technology",
            phonetic = "/tekˈnɒl.ə.dʒi/",
            meanings = listOf("công nghệ", "kỹ thuật"),
            examples = listOf("Mobile technology makes learning vocabulary easier."),
        ),
        Entry(
            term = "television",
            phonetic = "/ˈtel.ɪ.vɪ.ʒən/",
            meanings = listOf("tivi", "truyền hình"),
            examples = listOf("We watch television together after dinner."),
        ),
        Entry(
            term = "time",
            phonetic = "/taɪm/",
            meanings = listOf("thời gian", "lần", "thời điểm"),
            examples = listOf("Take your time to review each flashcard carefully."),
        ),
        Entry(
            term = "ubiquitous",
            phonetic = "/juːˈbɪkwɪtəs/",
            meanings = listOf("có mặt ở khắp nơi", "phổ biến rộng khắp"),
            examples = listOf("Smartphones have become ubiquitous in modern life."),
        ),
        Entry(
            term = "water",
            phonetic = "/ˈwɔː.tər/",
            meanings = listOf("nước", "tưới nước"),
            examples = listOf(
                "Remember to drink enough water every day.",
                "Could you water the plants while I am away?",
            ),
        ),
        Entry(
            term = "weather",
            phonetic = "/ˈweð.ər/",
            meanings = listOf("thời tiết"),
            examples = listOf("The weather is sunny and pleasant today."),
        ),
        Entry(
            term = "world",
            phonetic = "/wɜːld/",
            meanings = listOf("thế giới", "hoàn cầu"),
            examples = listOf("English helps you connect with people around the world."),
        ),
    )

    private val byNormalizedTerm: Map<String, Entry> =
        entries.associateBy { it.term.trim().lowercase() }

    fun find(term: String): Entry? = byNormalizedTerm[term.trim().lowercase()]

    fun suggest(prefix: String, limit: Int = 8): List<String> {
        val normalized = prefix.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return entries
            .asSequence()
            .map { it.term }
            .filter { it.lowercase().startsWith(normalized) }
            .take(limit)
            .toList()
    }
}
