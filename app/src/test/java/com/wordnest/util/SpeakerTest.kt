package com.wordnest.util

import android.app.Application
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.wordnest.data.study.StudyWord
import com.wordnest.data.study.StudyWordSource
import com.wordnest.viewmodel.StudyMode
import com.wordnest.viewmodel.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

private class FakeSpeaker : Speaker {
    private val _status = MutableSharedFlow<SpeakStatus>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val status: Flow<SpeakStatus> = _status.asSharedFlow()

    val spokenTerms = mutableListOf<String>()
    var shutdownCalls = 0
        private set

    override fun speak(text: String) {
        spokenTerms.add(text)
    }

    override fun shutdown() {
        shutdownCalls++
    }

    fun emitStatus(value: SpeakStatus) {
        _status.tryEmit(value)
    }
}

private data class SpokenCall(
    val text: String,
    val queueMode: Int,
    val utteranceId: String,
)

private class FakeTtsEngine(
    var languageResult: Int = TextToSpeech.LANG_AVAILABLE,
    var speakResult: Int = TextToSpeech.SUCCESS,
) : TtsEngine {
    val languageRequests = mutableListOf<Locale>()
    val spokenCalls = mutableListOf<SpokenCall>()
    var stopCount = 0
        private set
    var shutdownCount = 0
        private set

    override fun setLanguage(locale: Locale): Int {
        languageRequests.add(locale)
        return languageResult
    }

    override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
        spokenCalls.add(SpokenCall(text, queueMode, utteranceId))
        return speakResult
    }

    override fun stop(): Int {
        stopCount++
        return TextToSpeech.SUCCESS
    }

    override fun shutdown() {
        shutdownCount++
    }
}

private class StaticStudyWordSource(
    private val items: List<StudyWord>,
) : StudyWordSource {
    override fun words(): Flow<List<StudyWord>> = flowOf(items)
    override suspend fun save(word: StudyWord) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpeakerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 4, 20)
    private val fixedInstant = Instant.parse("2026-04-20T10:00:00Z")
    private lateinit var app: Application

    private val sampleWords = listOf(
        StudyWord("w1", "airport", "Sân bay", "/ˈeə.pɔːt/", WordStatus.EGG),
        StudyWord("w2", "apple", "Quả táo", "/ˈæp.əl/", WordStatus.EGG),
        StudyWord("w3", "book", "Cuốn sách", "/bʊk/", WordStatus.CHICK),
        StudyWord("w4", "clock", "Đồng hồ", "/klɒk/", WordStatus.BIRD),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createViewModel(speaker: Speaker): StudyViewModel {
        val vm = StudyViewModel(
            application = app,
            source = StaticStudyWordSource(sampleWords),
            studyDays = flowOf(emptySet()),
            studyHistoryRoom = null,
            today = today,
            clock = { fixedInstant },
            speaker = speaker,
        )
        vm.words.first { it.size == sampleWords.size }
        return vm
    }

    // ================= StudyViewModel + FakeSpeaker (T062) =================

    @Test
    fun flashcard_speakCurrentWord_speaksCurrentCardAndNextCard() = runTest {
        val fakeSpeaker = FakeSpeaker()
        val vm = createViewModel(fakeSpeaker)

        assertEquals(StudyMode.FLASHCARD, vm.mode.value)
        vm.speakCurrentWord()
        vm.markCard(true)
        vm.speakCurrentWord()

        assertEquals(listOf("airport", "apple"), fakeSpeaker.spokenTerms)
    }

    @Test
    fun quiz_speakCurrentWord_speaksCurrentQuestionTerm() = runTest {
        val fakeSpeaker = FakeSpeaker()
        val vm = createViewModel(fakeSpeaker)

        vm.startQuiz()
        assertEquals(StudyMode.QUIZ, vm.mode.value)
        val firstTerm = vm.currentQuestion!!.word.term
        vm.speakCurrentWord()

        vm.answerQuestion(0)
        vm.nextQuizQuestion()
        val secondTerm = vm.currentQuestion!!.word.term
        vm.speakCurrentWord()

        assertEquals(listOf(firstTerm, secondTerm), fakeSpeaker.spokenTerms)
    }

    @Test
    fun shutdownSpeaker_delegatesToSpeakerShutdown() = runTest {
        val fakeSpeaker = FakeSpeaker()
        val vm = createViewModel(fakeSpeaker)

        vm.speakCurrentWord()
        vm.shutdownSpeaker()

        assertEquals(1, fakeSpeaker.shutdownCalls)
    }

    @Test
    fun missingDataAndUnsupported_emitUnavailableToastEvent_readyDoesNotEmit() = runTest {
        val fakeSpeaker = FakeSpeaker()
        val vm = createViewModel(fakeSpeaker)

        vm.speakerErrorEvents.test {
            fakeSpeaker.emitStatus(SpeakStatus.Ready)
            expectNoEvents()

            fakeSpeaker.emitStatus(SpeakStatus.MissingData)
            assertEquals(StudyViewModel.SPEAKER_UNAVAILABLE_MESSAGE, awaitItem())

            fakeSpeaker.emitStatus(SpeakStatus.Unsupported)
            assertEquals(StudyViewModel.SPEAKER_UNAVAILABLE_MESSAGE, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun speakCurrentWord_onStatisticsMode_doesNotSpeak() = runTest {
        val fakeSpeaker = FakeSpeaker()
        val vm = createViewModel(fakeSpeaker)

        vm.showStatistics()
        assertEquals(StudyMode.STATISTICS, vm.mode.value)
        vm.speakCurrentWord()

        assertTrue(fakeSpeaker.spokenTerms.isEmpty())
    }

    // ================= AndroidSpeaker (contract & lifecycle) =================

    @Test
    fun androidSpeaker_lazyInit_speaksPendingWordOnReadyWithQueueFlush() = runTest {
        var createdEngines = 0
        var initListener: TextToSpeech.OnInitListener? = null
        val fakeEngine = FakeTtsEngine(languageResult = TextToSpeech.LANG_AVAILABLE)

        val speaker = AndroidSpeaker(
            context = app,
            lang = Locale.ENGLISH,
            engineFactory = { _, listener ->
                createdEngines++
                initListener = listener
                fakeEngine
            },
        )

        // Khởi tạo lười: chưa tạo engine trước lần speak đầu tiên
        assertEquals(0, createdEngines)

        speaker.status.test {
            speaker.speak("airport")
            assertEquals(1, createdEngines)
            assertTrue(fakeEngine.spokenCalls.isEmpty())

            initListener!!.onInit(TextToSpeech.SUCCESS)

            assertEquals(SpeakStatus.Ready, awaitItem())
            assertEquals(listOf(Locale.ENGLISH), fakeEngine.languageRequests)
            assertEquals(1, fakeEngine.spokenCalls.size)
            assertEquals(SpokenCall("airport", TextToSpeech.QUEUE_FLUSH, "wn-1"), fakeEngine.spokenCalls[0])

            // Từ tiếp theo phát ngay bằng QUEUE_FLUSH và mã utterance tăng dần
            speaker.speak("apple")
            assertEquals(2, fakeEngine.spokenCalls.size)
            assertEquals(SpokenCall("apple", TextToSpeech.QUEUE_FLUSH, "wn-2"), fakeEngine.spokenCalls[1])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun androidSpeaker_rapidSpeakDuringInit_speaksOnlyLatestWord() = runTest {
        var initListener: TextToSpeech.OnInitListener? = null
        val fakeEngine = FakeTtsEngine()

        val speaker = AndroidSpeaker(
            context = app,
            engineFactory = { _, listener ->
                initListener = listener
                fakeEngine
            },
        )

        speaker.speak("airport")
        speaker.speak("book")
        initListener!!.onInit(TextToSpeech.SUCCESS)

        assertEquals(1, fakeEngine.spokenCalls.size)
        assertEquals("book", fakeEngine.spokenCalls.single().text)
    }

    @Test
    fun androidSpeaker_missingDataAndLangNotSupported_emitMissingDataWithoutCrash() = runTest {
        var initListener: TextToSpeech.OnInitListener? = null
        val fakeEngine = FakeTtsEngine(languageResult = TextToSpeech.LANG_MISSING_DATA)

        val speaker = AndroidSpeaker(
            context = app,
            engineFactory = { _, listener ->
                initListener = listener
                fakeEngine
            },
        )

        speaker.status.test {
            speaker.speak("airport")
            initListener!!.onInit(TextToSpeech.SUCCESS)
            assertEquals(SpeakStatus.MissingData, awaitItem())
            assertTrue(fakeEngine.spokenCalls.isEmpty())

            // Bấm lại khi vẫn LANG_NOT_SUPPORTED vẫn báo MissingData, không crash
            fakeEngine.languageResult = TextToSpeech.LANG_NOT_SUPPORTED
            speaker.speak("apple")
            assertEquals(SpeakStatus.MissingData, awaitItem())
            assertTrue(fakeEngine.spokenCalls.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun androidSpeaker_initErrorOrFactoryException_emitsUnsupportedWithoutCrash() = runTest {
        var initListener: TextToSpeech.OnInitListener? = null
        val fakeEngine = FakeTtsEngine()

        val speakerWithInitError = AndroidSpeaker(
            context = app,
            engineFactory = { _, listener ->
                initListener = listener
                fakeEngine
            },
        )

        speakerWithInitError.status.test {
            speakerWithInitError.speak("airport")
            initListener!!.onInit(TextToSpeech.ERROR)
            assertEquals(SpeakStatus.Unsupported, awaitItem())
            assertEquals(1, fakeEngine.stopCount)
            assertEquals(1, fakeEngine.shutdownCount)
            assertTrue(fakeEngine.spokenCalls.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        val speakerWithThrow = AndroidSpeaker(
            context = app,
            engineFactory = { _, _ -> throw IllegalStateException("TTS unavailable") },
        )
        speakerWithThrow.status.test {
            speakerWithThrow.speak("airport")
            assertEquals(SpeakStatus.Unsupported, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun androidSpeaker_shutdownReleasesEngine_ignoresStaleInit_andReinitializesOnNextSpeak() = runTest {
        val engines = mutableListOf<FakeTtsEngine>()
        val listeners = mutableListOf<TextToSpeech.OnInitListener>()

        val speaker = AndroidSpeaker(
            context = app,
            engineFactory = { _, listener ->
                val engine = FakeTtsEngine()
                engines.add(engine)
                listeners.add(listener)
                engine
            },
        )

        // Lần 1: đang init thì rời màn -> shutdown; callback trễ không được đọc trong nền
        speaker.speak("airport")
        speaker.shutdown()
        assertEquals(1, engines[0].stopCount)
        assertEquals(1, engines[0].shutdownCount)
        listeners[0].onInit(TextToSpeech.SUCCESS)
        assertTrue(engines[0].spokenCalls.isEmpty())

        // Lần 2: quay lại màn học và bấm loa -> tự khởi tạo engine mới và phát âm bình thường
        speaker.speak("clock")
        assertEquals(2, engines.size)
        listeners[1].onInit(TextToSpeech.SUCCESS)
        assertEquals(1, engines[1].spokenCalls.size)
        assertEquals("clock", engines[1].spokenCalls.single().text)
    }

    @Test
    fun androidSpeaker_syncOnInitDuringConstruction_speaksImmediately() = runTest {
        val fakeEngine = FakeTtsEngine()
        val speaker = AndroidSpeaker(
            context = app,
            engineFactory = { _, listener ->
                listener.onInit(TextToSpeech.SUCCESS)
                fakeEngine
            },
        )

        speaker.speak("book")
        assertEquals(1, fakeEngine.spokenCalls.size)
        assertEquals("book", fakeEngine.spokenCalls.single().text)
        assertFalse(fakeEngine.languageRequests.isEmpty())
    }

    @Test
    fun androidSpeaker_blankText_isIgnoredWithoutInitializing() {
        var created = 0
        val speaker = AndroidSpeaker(
            context = app,
            engineFactory = { _, _ ->
                created++
                FakeTtsEngine()
            },
        )

        speaker.speak("")
        speaker.speak("   ")
        assertEquals(0, created)
    }
}
