package com.wordnest.data.dictionary

/**
 * Kết quả tra cứu chi tiết một từ vựng (phiên âm IPA, danh sách nghĩa tiếng Việt gợi ý, câu ví dụ).
 */
data class WordSuggestionDetails(
    val phonetic: String? = null,
    val meanings: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

/**
 * Cổng tra cứu từ điển & dịch tự động kiểu Quizlet cho màn Thêm/Sửa từ (F04, F05):
 * - [suggest]: gợi ý từ tiếng Anh theo tiền tố (autocomplete).
 * - [getPhonetic]: lấy phiên âm IPA.
 * - [getMeanings]: lấy danh sách nghĩa tiếng Việt gợi ý.
 * - [getExamples]: lấy danh sách câu ví dụ tiếng Anh gợi ý.
 * - [lookupDetails]: tải trọn gói phiên âm + nghĩa + câu ví dụ cho một từ.
 */
interface DictionaryRepository {
    suspend fun suggest(prefix: String): List<String>
    suspend fun getPhonetic(term: String): String?
    suspend fun getMeanings(term: String): List<String>
    suspend fun getExamples(term: String): List<String>

    suspend fun lookupDetails(term: String): WordSuggestionDetails = WordSuggestionDetails(
        phonetic = getPhonetic(term),
        meanings = getMeanings(term),
        examples = getExamples(term),
    )
}

/** Triển khai rỗng mặc định cho các kiểm thử chỉ quan tâm tới lưu trữ Room. */
object NoOpDictionaryRepository : DictionaryRepository {
    override suspend fun suggest(prefix: String): List<String> = emptyList()
    override suspend fun getPhonetic(term: String): String? = null
    override suspend fun getMeanings(term: String): List<String> = emptyList()
    override suspend fun getExamples(term: String): List<String> = emptyList()
}
