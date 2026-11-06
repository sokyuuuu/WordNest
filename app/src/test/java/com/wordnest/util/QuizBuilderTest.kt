package com.wordnest.util

import com.wordnest.data.study.StudyWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm tra tạo câu hỏi trắc nghiệm (F12) theo TC12-01, TC12-04, TC12-05. */
class QuizBuilderTest {

    private fun word(id: String, term: String, meaning: String) =
        StudyWord(id = id, term = term, meaning = meaning)

    private val eightWords = listOf(
        word("w1", "airport", "Sân bay"),
        word("w2", "apple", "Quả táo"),
        word("w3", "book", "Cuốn sách"),
        word("w4", "computer", "Máy tính"),
        word("w5", "hospital", "Bệnh viện"),
        word("w6", "pen", "Cây bút"),
        word("w7", "television", "Tivi"),
        word("w8", "clock", "Đồng hồ"),
    )

    @Test
    fun fewerThanFourWords_returnsEmpty() {
        val words = eightWords.take(2)
        assertEquals(emptyList<QuizQuestion>(), buildQuizQuestions(words))
    }

    @Test
    fun eachQuestion_hasFourOptions() {
        val questions = buildQuizQuestions(eightWords, seed = 42)
        assertTrue(questions.isNotEmpty())
        questions.forEach { question ->
            assertEquals(4, question.options.size)
        }
    }

    @Test
    fun options_haveNoDuplicates() {
        val questions = buildQuizQuestions(eightWords, seed = 42)
        questions.forEach { question ->
            assertEquals(question.options.size, question.options.toSet().size)
        }
    }

    @Test
    fun correctAnswer_isPresentExactlyOnce() {
        val questions = buildQuizQuestions(eightWords, seed = 42)
        questions.forEach { question ->
            assertEquals(1, question.options.count { it == question.word.meaning })
            assertEquals(question.word.meaning, question.options[question.correctIndex])
        }
    }

    @Test
    fun everyWordGetsAQuestion() {
        val questions = buildQuizQuestions(eightWords, seed = 42)
        assertEquals(eightWords.size, questions.size)
        assertEquals(eightWords.map { it.id }.toSet(), questions.map { it.word.id }.toSet())
    }

    @Test
    fun sameSeed_producesSameQuestions() {
        assertEquals(
            buildQuizQuestions(eightWords, seed = 7),
            buildQuizQuestions(eightWords, seed = 7),
        )
    }
}
