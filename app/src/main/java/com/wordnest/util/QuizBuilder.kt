package com.wordnest.util

import com.wordnest.data.study.StudyWord
import kotlin.random.Random

/**
 * Một câu trắc nghiệm (F12): mỗi câu có 4 đáp án, đúng 1 đáp án.
 */
data class QuizQuestion(
    val word: StudyWord,
    val options: List<String>,
    val correctIndex: Int,
)

/**
 * Tạo bộ câu hỏi trắc nghiệm từ danh sách từ của một Tổ (F12).
 *
 * - Trả về danh sách rỗng nếu Tổ có ít hơn 4 từ (TC12-05: gợi ý thêm từ).
 * - Các đáp án không trùng nhau (TC12-04).
 * - Theo Figma: hiện từ (term), người học chọn NGHĨA tiếng Việt đúng —
 *   đáp án sai là nghĩa của 3 từ khác.
 */
fun buildQuizQuestions(
    words: List<StudyWord>,
    seed: Long = System.nanoTime(),
): List<QuizQuestion> {
    if (words.size < 4) return emptyList()
    val random = Random(seed)

    return words.shuffled(random).map { word ->
        val distractors = words.asSequence()
            .filter { it.id != word.id }
            .map { it.meaning }
            .distinct()
            .shuffled(random)
            .take(3)
            .toList()
        val options = (distractors + word.meaning).shuffled(random)
        QuizQuestion(word, options, options.indexOf(word.meaning))
    }
}
