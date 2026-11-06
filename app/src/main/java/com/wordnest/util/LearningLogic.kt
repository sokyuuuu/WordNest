package com.wordnest.util

/**
 * Logic chấm điểm (F12, F13, F16): đếm số câu đúng, tính tỷ lệ phần trăm.
 */
object LearningLogic {

    fun isCorrectAnswer(selectedAnswer: Int, correctAnswer: Int): Boolean =
        selectedAnswer == correctAnswer

    fun calculateScore(selectedAnswers: List<Int>, correctAnswers: List<Int>): Int {
        require(selectedAnswers.size == correctAnswers.size) { "Số lượng đáp án không khớp" }
        return selectedAnswers.zip(correctAnswers).count { (selected, correct) ->
            isCorrectAnswer(selected, correct)
        }
    }

    fun calculatePercentage(correct: Int, total: Int): Int {
        if (total <= 0) return 0
        return correct * 100 / total
    }
}
