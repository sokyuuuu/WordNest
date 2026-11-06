package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.StudyHistoryRepository
import com.wordnest.data.study.InMemoryStudyWordSource
import com.wordnest.data.study.StudyWord
import com.wordnest.data.study.StudyWordSource
import com.wordnest.util.QuizQuestion
import com.wordnest.util.StudyProgression
import com.wordnest.util.TypedAnswer
import com.wordnest.util.buildQuizQuestions
import com.wordnest.util.calculateEggDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class StudyMode {
    FLASHCARD,
    FLASHCARD_RESULT,
    QUIZ,
    QUIZ_RESULT,
    TYPE_IN,
    STATISTICS,
}

data class StudyResult(
    val title: String,
    val correctCount: Int,
    val totalCount: Int,
    val wrongWords: List<StudyWord>,
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val source: StudyWordSource = InMemoryStudyWordSource()
    private val studyHistory = StudyHistoryRepository(application.applicationContext)
    private val today: LocalDate = LocalDate.now()

    private val _words = MutableStateFlow(source.words())
    val words: StateFlow<List<StudyWord>> = _words

    private val _mode = MutableStateFlow(StudyMode.FLASHCARD)
    val mode: StateFlow<StudyMode> = _mode

    private val _result = MutableStateFlow<StudyResult?>(null)
    val result: StateFlow<StudyResult?> = _result

    private var modeBeforeStatistics: StudyMode = StudyMode.FLASHCARD

    // --- Flashcard (F08, F09) ---
    private var sessionIds: List<String> = emptyList()
    private val _cardIndex = MutableStateFlow(0)
    val cardIndex: StateFlow<Int> = _cardIndex
    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped
    private var knownIds: MutableSet<String> = mutableSetOf()
    val knownCount: Int get() = knownIds.size

    /** Thẻ đang hiện trong phiên học (có thể chỉ là các từ chưa thuộc khi học lại — US10). */
    val currentCard: StudyWord? get() = currentCardInSession()

    /** Tổng số thẻ trong phiên học hiện tại. */
    val sessionSize: Int get() = sessionIds.size

    /** Danh sách từ hiện tại chưa đánh dấu "Đã thuộc", dùng cho nút học lại (US10). */
    private var unmasteredIds: MutableSet<String> = mutableSetOf()

    // --- Trắc nghiệm (F12) ---
    private var questions: List<QuizQuestion> = emptyList()
    private val _quizIndex = MutableStateFlow(0)
    val quizIndex: StateFlow<Int> = _quizIndex
    val quizTotal: Int get() = questions.size
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(_quizIndex.value)
    private val _selected = MutableStateFlow<Int?>(null)
    val selected: StateFlow<Int?> = _selected
    private var quizCorrect = 0

    val canStartQuiz: Boolean get() = _words.value.size >= 4

    // --- Điền từ (F14) ---
    private var typeQueue: List<StudyWord> = emptyList()
    private val _typeIndex = MutableStateFlow(0)
    val typeIndex: StateFlow<Int> = _typeIndex
    val typeTotal: Int get() = typeQueue.size
    val currentTypeWord: StudyWord? get() = typeQueue.getOrNull(_typeIndex.value)
    private val _typedCorrect = MutableStateFlow<Boolean?>(null)
    val typedCorrect: StateFlow<Boolean?> = _typedCorrect
    private var typeCorrect = 0

    /** Danh sách id các từ làm sai trong lượt làm bài hiện tại (F13). */
    private val wrongIds = mutableSetOf<String>()

    init {
        startFlashcardSession(_words.value)
        applyIdleDemotion()
    }

    // ================= Flashcard =================

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun markCard(known: Boolean) {
        val word = currentCardInSession() ?: return
        if (known) {
            knownIds.add(word.id)
            unmasteredIds.remove(word.id)
        } else {
            unmasteredIds.add(word.id)
        }

        if (_cardIndex.value < sessionIds.lastIndex) {
            _cardIndex.value++
            _isFlipped.value = false
        } else {
            finishFlashcard()
        }
    }

    private fun currentCardInSession(): StudyWord? {
        val id = sessionIds.getOrNull(_cardIndex.value) ?: return null
        return _words.value.firstOrNull { it.id == id }
    }

    private fun startFlashcardSession(wordsForSession: List<StudyWord>) {
        sessionIds = wordsForSession.map { it.id }
        _cardIndex.value = 0
        _isFlipped.value = false
        knownIds = mutableSetOf()
    }

    /**
     * Kết thúc buổi học: ghi nhận ngày học, nâng hạng từ theo chuỗi ngày (F11),
     * lưu kết quả rồi hiện màn hình kết quả (F10).
     */
    private fun finishFlashcard() = viewModelScope.launch {
        val streak = currentStreak()
        applyStatusChange(sessionIds) { word ->
            word.copy(
                status = StudyProgression.promote(word.status, streak),
                lastReviewedAt = today,
            )
        }
        val sessionWords = _words.value.filter { it.id in sessionIds }
        _result.value = StudyResult(
            title = "Kết quả buổi học",
            correctCount = knownIds.size,
            totalCount = sessionWords.size,
            wrongWords = sessionWords.filter { it.id !in knownIds },
        )
        _mode.value = StudyMode.FLASHCARD_RESULT
    }

    /** Đang có từ chưa thuộc để học lại (US10: chỉ học lại từ chưa thuộc). */
    val hasUnknownWordsToReview: Boolean
        get() = unmasteredIds.isNotEmpty()

    fun reviewUnknown() {
        val unknown = _words.value.filter { it.id in unmasteredIds }
        if (unknown.isEmpty()) return
        startFlashcardSession(unknown)
        _mode.value = StudyMode.FLASHCARD
    }

    // ================= Trắc nghiệm =================

    fun startQuiz() {
        val newQuestions = buildQuizQuestions(_words.value)
        if (newQuestions.isEmpty()) return
        questions = newQuestions
        wrongIds.clear()
        quizCorrect = 0
        _quizIndex.value = 0
        _selected.value = null
        _mode.value = StudyMode.QUIZ
    }

    /** Chọn đáp án; báo đúng/sai ngay sau khi chọn (TC12-02, TC12-03). */
    fun answerQuestion(optionIndex: Int) {
        if (_selected.value != null) return
        val question = questions.getOrNull(_quizIndex.value) ?: return
        _selected.value = optionIndex
        if (optionIndex == question.correctIndex) {
            quizCorrect++
        } else {
            wrongIds.add(question.word.id)
        }
    }

    fun nextQuizQuestion() {
        if (_selected.value == null) return
        if (_quizIndex.value < questions.lastIndex) {
            _quizIndex.value++
            _selected.value = null
        } else {
            finishQuizOrTyped("Kết quả kiểm tra", quizCorrect)
        }
    }

    // ================= Điền từ =================

    fun startTypeIn() {
        if (!canStartQuiz) return
        typeQueue = _words.value.shuffled()
        wrongIds.clear()
        typeCorrect = 0
        _typeIndex.value = 0
        _typedCorrect.value = null
        _mode.value = StudyMode.TYPE_IN
    }

    /** Chấm ngay sau khi gửi (TC14-01, TC14-02). */
    fun submitTyped(userInput: String) {
        if (_typedCorrect.value != null) return
        val word = typeQueue.getOrNull(_typeIndex.value) ?: return
        val correct = TypedAnswer.isCorrect(userInput, word.term)
        _typedCorrect.value = correct
        if (correct) {
            typeCorrect++
        } else {
            wrongIds.add(word.id)
        }
    }

    fun nextTypeWord() {
        if (_typedCorrect.value == null) return
        if (_typeIndex.value < typeQueue.lastIndex) {
            _typeIndex.value++
            _typedCorrect.value = null
        } else {
            finishQuizOrTyped("Kết quả điền từ", typeCorrect)
        }
    }

    // ================= Kết quả và thống kê =================

    private fun finishQuizOrTyped(title: String, correct: Int) = viewModelScope.launch {
        applyStatusChange(_words.value.map { it.id }) { it.copy(lastReviewedAt = today) }
        val wrongWords = _words.value.filter { it.id in wrongIds }
        _result.value = StudyResult(
            title = title,
            correctCount = correct,
            totalCount = _words.value.size,
            wrongWords = wrongWords,
        )
        _mode.value = StudyMode.QUIZ_RESULT
    }

    fun showStatistics() {
        modeBeforeStatistics = _mode.value
        _mode.value = StudyMode.STATISTICS
    }

    /** Rời thống kê, trở về màn hình trước khi xem. */
    fun backFromStatistics() {
        _mode.value = modeBeforeStatistics
    }

    // ================= Hỗ trợ =================

    /** Cập nhật statuses của các từ, lưu xuống nguồn và ghi nhận ngày ôn hôm nay. */
    private fun applyStatusChange(ids: List<String>, transform: (StudyWord) -> StudyWord) {
        val updated = _words.value.map { word ->
            if (word.id in ids) transform(word).also { source.save(it) } else word
        }
        _words.value = updated
    }

    private suspend fun currentStreak(): Int =
        calculateEggDay(studyHistory.studyDays.first(), today)

    private fun applyIdleDemotion() {
        applyStatusChange(_words.value.map { it.id }) { word ->
            word.copy(
                status = StudyProgression.demoteIfIdle(word.status, word.lastReviewedAt, today),
            )
        }
    }
}
