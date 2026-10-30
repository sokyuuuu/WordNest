
package com.example.flashcardscreen

object LearningLogic {

    fun isCorrectAnswer(
        selectedAnswer: Int,
        correctAnswer: Int
    ): Boolean {
        return selectedAnswer == correctAnswer
    }

    fun calculateScore(
        selectedAnswers: List<Int>,
        correctAnswers: List<Int>
    ): Int {
        require(selectedAnswers.size == correctAnswers.size) {
            "Số lượng đáp án không khớp"
        }

        return selectedAnswers.zip(correctAnswers)
            .count { (selected, correct) ->
                isCorrectAnswer(selected, correct)
            }
    }
    fun updateWordStatus(
        currentStatus: Boolean,
        isRemembered: Boolean
    ): Boolean {
        return isRemembered
    }
    fun calculatePercentage(
        correct: Int,
        total: Int
    ): Int {
        if (total <= 0) return 0
        return correct * 100 / total
    }
}
