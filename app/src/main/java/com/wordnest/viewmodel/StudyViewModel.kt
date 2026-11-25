package com.wordnest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.repository.HistoryMode
import com.wordnest.data.repository.HistoryResult
import com.wordnest.data.repository.StudyHistoryRoomRepository
import com.wordnest.data.study.StudyWord
import com.wordnest.data.study.StudyWordSource
import com.wordnest.di.appContainer
import com.wordnest.util.ExampleHole
import com.wordnest.util.QuizQuestion
import com.wordnest.util.SpeakStatus
import com.wordnest.util.Speaker
import com.wordnest.util.StudyProgression
import com.wordnest.util.TypedAnswer
import com.wordnest.util.buildQuizQuestions
import com.wordnest.util.calculateEggDay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
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

private object NoOpSpeaker : Speaker {
    override fun speak(text: String) = Unit
    override fun shutdown() = Unit
}

class StudyViewModel internal constructor(
    application: Application,
    private val source: StudyWordSource,
    private val studyDays: Flow<Set<LocalDate>>,
    private val studyHistoryRoom: StudyHistoryRoomRepository?,
    private val today: LocalDate = LocalDate.now(),
    private val clock: () -> Instant = Instant::now,
    private val speaker: Speaker = NoOpSpeaker,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        source = application.appContainer().roomStudyWordSource,
        studyDays = application.appContainer().studyDayRepository.studyDays,
        studyHistoryRoom = application.appContainer().studyHistoryRoomRepository,
        speaker = application.appContainer().speaker,
    )

    companion object {
        const val SPEAKER_UNAVAILABLE_MESSAGE = "Chưa cài dữ liệu phát âm"
    }

    private val _words = MutableStateFlow<List<StudyWord>>(emptyList())
    val words: StateFlow<List<StudyWord>> = _words

    private val _speakerErrorEvents = Channel<String>(
        capacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Sự kiện thông báo lỗi phát âm (MissingData / Unsupported) để UI hiện Toast một lần. */
    val speakerErrorEvents: Flow<String> = _speakerErrorEvents.receiveAsFlow()
    private var hasSpoken = false

    private val _mode = MutableStateFlow(StudyMode.FLASHCARD)
    val mode: StateFlow<StudyMode> = _mode

    private val _result = MutableStateFlow<StudyResult?>(null)
    val result: StateFlow<StudyResult?> = _result

    private var modeBeforeStatistics: StudyMode = StudyMode.FLASHCARD

    // --- Flashcard (F08, F09) ---
    private var sessionInitialized = false
    private var idleDemotionApplied = false
    private var isFinishingSession = false
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
    val currentTypeWord: StudyWord? get() = latestTypeWordAt(_typeIndex.value)
    private val _currentTypeExample = MutableStateFlow<String?>(null)
    /** Câu ví dụ đã thay từ bằng `___` cho câu Điền từ hiện tại (null nếu không có ví dụ khớp). */
    val currentTypeExample: StateFlow<String?> = _currentTypeExample
    private val _typedCorrect = MutableStateFlow<Boolean?>(null)
    val typedCorrect: StateFlow<Boolean?> = _typedCorrect
    private var typeCorrect = 0

    /** Danh sách id các từ làm sai trong lượt làm bài hiện tại (F13). */
    private val wrongIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            speaker.status.collect { status ->
                if (status is SpeakStatus.MissingData || status is SpeakStatus.Unsupported) {
                    _speakerErrorEvents.trySend(SPEAKER_UNAVAILABLE_MESSAGE)
                }
            }
        }
        viewModelScope.launch {
            source.words().collect { incoming ->
                if (!sessionInitialized && incoming.isNotEmpty()) {
                    sessionInitialized = true
                    startFlashcardSession(incoming)
                }
                _words.value = incoming
                refreshCurrentTypeExample()
                if (!idleDemotionApplied && incoming.isNotEmpty()) {
                    idleDemotionApplied = true
                    applyIdleDemotion()
                }
            }
        }
    }

    // ================= Flashcard =================

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun markCard(known: Boolean) {
        if (_mode.value != StudyMode.FLASHCARD || isFinishingSession) return
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
     * Kết thúc buổi học: nâng hạng từ theo chuỗi ngày (F11), ghi lịch sử học vào `study_history`,
     * lưu kết quả xuống Room rồi hiện màn hình kết quả (F10).
     */
    private fun finishFlashcard() {
        if (isFinishingSession) return
        isFinishingSession = true
        viewModelScope.launch {
            try {
                val sessionIdSnapshot = sessionIds.toList()
                val knownIdSnapshot = knownIds.toSet()
                val streak = currentStreak()
                applyStatusChange(sessionIdSnapshot) { word ->
                    word.copy(
                        status = StudyProgression.promote(word.status, streak),
                        lastReviewedAt = today,
                    )
                }
                val recordedAt = clock()
                sessionIdSnapshot.forEach { wordId ->
                    val result = if (wordId in knownIdSnapshot) {
                        HistoryResult.CORRECT
                    } else {
                        HistoryResult.WRONG
                    }
                    studyHistoryRoom?.record(
                        wordId = wordId,
                        result = result,
                        mode = HistoryMode.FLASHCARD,
                        at = recordedAt,
                    )
                }
                val sessionWords = _words.value.filter { it.id in sessionIdSnapshot }
                _result.value = StudyResult(
                    title = "Kết quả buổi học",
                    correctCount = knownIdSnapshot.size,
                    totalCount = sessionWords.size,
                    wrongWords = sessionWords.filter { it.id !in knownIdSnapshot },
                )
                _mode.value = StudyMode.FLASHCARD_RESULT
            } finally {
                isFinishingSession = false
            }
        }
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

    /** Chọn đáp án; báo đúng/sai ngay sau khi chọn (TC12-02, TC12-03) và ghi `study_history` (lần đầu). */
    fun answerQuestion(optionIndex: Int) {
        if (_selected.value != null) return
        val question = questions.getOrNull(_quizIndex.value) ?: return
        _selected.value = optionIndex
        val isCorrect = optionIndex == question.correctIndex
        if (isCorrect) {
            quizCorrect++
        } else {
            wrongIds.add(question.word.id)
        }
        val recordedAt = clock()
        viewModelScope.launch {
            studyHistoryRoom?.record(
                wordId = question.word.id,
                result = if (isCorrect) HistoryResult.CORRECT else HistoryResult.WRONG,
                mode = HistoryMode.QUIZ,
                at = recordedAt,
            )
        }
    }

    fun nextQuizQuestion() {
        if (_selected.value == null || isFinishingSession) return
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
        refreshCurrentTypeExample()
        _mode.value = StudyMode.TYPE_IN
    }

    /** Chấm ngay sau khi gửi (TC14-01, TC14-02) và ghi `study_history` (lần đầu). */
    fun submitTyped(userInput: String) {
        if (_typedCorrect.value != null) return
        val word = currentTypeWord ?: return
        val correct = TypedAnswer.isCorrect(userInput, word.term)
        _typedCorrect.value = correct
        if (correct) {
            typeCorrect++
        } else {
            wrongIds.add(word.id)
        }
        val recordedAt = clock()
        viewModelScope.launch {
            studyHistoryRoom?.record(
                wordId = word.id,
                result = if (correct) HistoryResult.CORRECT else HistoryResult.WRONG,
                mode = HistoryMode.TYPE_IN,
                at = recordedAt,
            )
        }
    }

    fun nextTypeWord() {
        if (_typedCorrect.value == null || isFinishingSession) return
        if (_typeIndex.value < typeQueue.lastIndex) {
            _typeIndex.value++
            _typedCorrect.value = null
            refreshCurrentTypeExample()
        } else {
            finishQuizOrTyped("Kết quả điền từ", typeCorrect)
        }
    }

    private fun latestTypeWordAt(index: Int): StudyWord? {
        val snapshot = typeQueue.getOrNull(index) ?: return null
        return _words.value.firstOrNull { it.id == snapshot.id } ?: snapshot
    }

    private fun refreshCurrentTypeExample() {
        val word = currentTypeWord
        _currentTypeExample.value = word?.let { ExampleHole.replace(it.example, it.term) }
    }

    // ================= Kết quả và thống kê =================

    private fun finishQuizOrTyped(title: String, correct: Int) {
        if (isFinishingSession) return
        isFinishingSession = true
        viewModelScope.launch {
            try {
                applyStatusChange(_words.value.map { it.id }) { it.copy(lastReviewedAt = today) }
                val wrongWords = _words.value.filter { it.id in wrongIds }
                _result.value = StudyResult(
                    title = title,
                    correctCount = correct,
                    totalCount = _words.value.size,
                    wrongWords = wrongWords,
                )
                _mode.value = StudyMode.QUIZ_RESULT
            } finally {
                isFinishingSession = false
            }
        }
    }

    fun showStatistics() {
        modeBeforeStatistics = _mode.value
        _mode.value = StudyMode.STATISTICS
    }

    /** Rời thống kê, trở về màn hình trước khi xem. */
    fun backFromStatistics() {
        _mode.value = modeBeforeStatistics
    }

    // ================= Phát âm (F19) =================

    /**
     * Đọc to từ hiện tại theo màn đang mở (Flashcard / Trắc nghiệm / Điền từ).
     * Huỷ âm đang phát trước đó (QUEUE_FLUSH trong [Speaker]).
     */
    fun speakCurrentWord() {
        val term = when (_mode.value) {
            StudyMode.FLASHCARD -> currentCard?.term
            StudyMode.QUIZ -> currentQuestion?.word?.term
            StudyMode.TYPE_IN -> currentTypeWord?.term
            else -> null
        } ?: return
        speak(term)
    }

    /** Đọc to một từ cụ thể bằng giọng tiếng Anh. */
    fun speak(term: String) {
        if (term.isBlank()) return
        hasSpoken = true
        speaker.speak(term)
    }

    /** Dừng phát âm và giải phóng TTS khi rời màn học. */
    fun shutdownSpeaker() {
        hasSpoken = false
        speaker.shutdown()
    }

    override fun onCleared() {
        if (hasSpoken) {
            speaker.shutdown()
            hasSpoken = false
        }
        super.onCleared()
    }

    // ================= Hỗ trợ =================

    /** Cập nhật trạng thái các từ và lưu xuống Room (chỉ ghi khi thay đổi nếu [onlyIfChanged] = true). */
    private suspend fun applyStatusChange(
        ids: List<String>,
        onlyIfChanged: Boolean = false,
        transform: (StudyWord) -> StudyWord,
    ) {
        val idSet = ids.toSet()
        val updated = _words.value.map { word ->
            if (word.id in idSet) {
                val next = transform(word)
                if (!onlyIfChanged || next != word) {
                    source.save(next)
                }
                next
            } else {
                word
            }
        }
        _words.value = updated
    }

    private suspend fun currentStreak(): Int =
        calculateEggDay(studyDays.first(), today)

    private suspend fun applyIdleDemotion() {
        applyStatusChange(_words.value.map { it.id }, onlyIfChanged = true) { word ->
            word.copy(
                status = StudyProgression.demoteIfIdle(word.status, word.lastReviewedAt, today),
            )
        }
    }
}
