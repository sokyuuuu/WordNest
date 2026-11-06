package com.wordnest.data.study

import com.wordnest.util.WordStatus

/**
 * Nguồn từ cho luồng học. Khi Room được gắn vào, viết lại lớp triển khai này
 * theo WordRepository thay vì đổi chỗ gọi trong ViewModel và màn hình.
 */
interface StudyWordSource {
    fun words(): List<StudyWord>
    fun save(word: StudyWord)
}

/**
 * Dữ liệu mẫu trong bộ nhớ, dùng để phát triển và chạy thử trước khi có Room.
 * 8 từ: đủ để tạo câu hỏi trắc nghiệm (TC12-05 yêu cầu tối thiểu 4 từ).
 * Trạng thái được cập nhật theo phiên chạy để thống kê F16 phản ánh buổi học.
 */
class InMemoryStudyWordSource : StudyWordSource {

    private val words = listOf(
        StudyWord("w1", "airport", "Sân bay", "/ˈeəpɔːt/", WordStatus.EGG),
        StudyWord("w2", "apple", "Quả táo", "/ˈæp.əl/", WordStatus.EGG),
        StudyWord("w3", "book", "Cuốn sách", "/bʊk/", WordStatus.EGG),
        StudyWord("w4", "computer", "Máy tính", "/kəmˈpjuː.tə/", WordStatus.CHICK),
        StudyWord("w5", "hospital", "Bệnh viện", "/ˈhɒs.pɪ.təl/", WordStatus.CHICK),
        StudyWord("w6", "pen", "Cây bút", "/pen/", WordStatus.CHICK),
        StudyWord("w7", "television", "Tivi", "/ˈtel.ɪ.vɪ.ʒən/", WordStatus.BIRD),
        StudyWord("w8", "clock", "Đồng hồ", "/klɒk/", WordStatus.BIRD),
    ).associateBy { it.id }.toMutableMap()

    override fun words(): List<StudyWord> = words.values.toList()

    override fun save(word: StudyWord) {
        words[word.id] = word
    }
}
