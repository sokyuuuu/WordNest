package com.wordnest.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** Bọc tối thiểu quanh [TextToSpeech] để [AndroidSpeaker] có thể kiểm thử đơn vị độc lập. */
internal interface TtsEngine {
    fun setLanguage(locale: Locale): Int
    fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int
    fun stop(): Int
    fun shutdown()
}

private class AndroidTtsEngine(private val tts: TextToSpeech) : TtsEngine {
    override fun setLanguage(locale: Locale): Int = tts.setLanguage(locale)

    override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int =
        tts.speak(text, queueMode, params, utteranceId)

    override fun stop(): Int = tts.stop()

    override fun shutdown() {
        tts.shutdown()
    }
}

/**
 * Triển khai [Speaker] dùng [TextToSpeech] của Android (F19).
 *
 * - Khởi tạo lười ở lần [speak] đầu tiên (và tự khởi tạo lại nếu gọi [speak] sau [shutdown]).
 * - Giữ từ chờ mới nhất khi engine đang khởi tạo; khi sẵn sàng dùng [TextToSpeech.QUEUE_FLUSH]
 *   để chỉ đọc 1 từ tại một thời điểm (FR-021).
 * - Mọi lỗi khởi tạo hoặc thiếu dữ liệu giọng đều phát qua [status] và không làm crash app (FR-020).
 */
class AndroidSpeaker internal constructor(
    context: Context,
    private val lang: Locale = Locale.ENGLISH,
    private val engineFactory: (Context, TextToSpeech.OnInitListener) -> TtsEngine,
) : Speaker {

    constructor(
        context: Context,
        lang: Locale = Locale.ENGLISH,
    ) : this(
        context = context,
        lang = lang,
        engineFactory = { appCtx, listener -> AndroidTtsEngine(TextToSpeech(appCtx, listener)) },
    )

    private val appContext: Context = context.applicationContext
    private val lock = Any()
    private val utteranceCounter = AtomicInteger(0)

    private var engine: TtsEngine? = null
    private var initializing: Boolean = false
    private var initialized: Boolean = false
    private var ready: Boolean = false
    private var pendingText: String? = null
    private var generation: Int = 0

    private val _status = MutableSharedFlow<SpeakStatus>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val status: SharedFlow<SpeakStatus> = _status.asSharedFlow()

    override fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        var engineToSpeak: TtsEngine? = null
        var shouldInit = false
        var initGen = 0
        var immediateStatus: SpeakStatus? = null
        var emitReadyBeforeSpeak = false

        synchronized(lock) {
            val current = engine
            when {
                current == null && !initializing -> {
                    initializing = true
                    initialized = false
                    ready = false
                    pendingText = trimmed
                    generation++
                    initGen = generation
                    shouldInit = true
                }
                !initialized -> {
                    // Đang chờ onInit: ghi đè bằng từ mới nhất (huỷ từ chờ trước đó)
                    pendingText = trimmed
                }
                ready && current != null -> {
                    engineToSpeak = current
                }
                current != null -> {
                    // Đã khởi tạo nhưng trước đó thiếu dữ liệu ngôn ngữ: thử kiểm tra lại
                    val langResult = safeSetLanguage(current)
                    if (isLanguageUnavailable(langResult)) {
                        immediateStatus = SpeakStatus.MissingData
                    } else {
                        ready = true
                        emitReadyBeforeSpeak = true
                        engineToSpeak = current
                    }
                }
            }
        }

        if (immediateStatus != null) {
            _status.tryEmit(immediateStatus!!)
            return
        }

        if (emitReadyBeforeSpeak) {
            _status.tryEmit(SpeakStatus.Ready)
        }

        val activeEngine = engineToSpeak
        if (activeEngine != null) {
            val speakResult = safeSpeak(activeEngine, trimmed)
            if (speakResult == TextToSpeech.ERROR) {
                var toRelease: TtsEngine? = null
                synchronized(lock) {
                    if (engine === activeEngine) {
                        toRelease = engine
                        engine = null
                        initializing = false
                        initialized = false
                        ready = false
                        pendingText = null
                        generation++
                    }
                }
                safeRelease(toRelease)
                _status.tryEmit(SpeakStatus.Unsupported)
            }
            return
        }

        if (shouldInit) {
            initializeEngine(initGen)
        }
    }

    override fun shutdown() {
        val toRelease: TtsEngine?
        synchronized(lock) {
            generation++
            toRelease = engine
            engine = null
            initializing = false
            initialized = false
            ready = false
            pendingText = null
        }
        safeRelease(toRelease)
    }

    private fun initializeEngine(initGen: Int) {
        var syncCallbackStatus: Int? = null
        var inConstructor = true
        val created = try {
            engineFactory(appContext) { initStatus ->
                var handleNow = false
                synchronized(lock) {
                    if (inConstructor) {
                        syncCallbackStatus = initStatus
                    } else {
                        handleNow = true
                    }
                }
                if (handleNow) {
                    handleOnInit(initGen, initStatus)
                }
            }
        } catch (_: Throwable) {
            synchronized(lock) {
                if (generation == initGen) {
                    engine = null
                    initializing = false
                    initialized = false
                    ready = false
                    pendingText = null
                }
            }
            _status.tryEmit(SpeakStatus.Unsupported)
            return
        }

        var stale = false
        var deferredStatus: Int? = null
        synchronized(lock) {
            inConstructor = false
            if (generation != initGen) {
                stale = true
            } else {
                engine = created
                initializing = false
                deferredStatus = syncCallbackStatus
            }
        }

        if (stale) {
            safeRelease(created)
            return
        }

        if (deferredStatus != null) {
            handleOnInit(initGen, deferredStatus!!)
        }
    }

    private fun handleOnInit(initGen: Int, initStatus: Int) {
        var textToSpeak: String? = null
        var readyEngine: TtsEngine? = null
        var releaseEngine: TtsEngine? = null
        var emittedStatus: SpeakStatus? = null

        synchronized(lock) {
            if (generation != initGen) return
            val current = engine ?: return
            if (initStatus != TextToSpeech.SUCCESS) {
                releaseEngine = current
                engine = null
                initializing = false
                initialized = false
                ready = false
                pendingText = null
                emittedStatus = SpeakStatus.Unsupported
            } else {
                initialized = true
                val langResult = safeSetLanguage(current)
                if (isLanguageUnavailable(langResult)) {
                    ready = false
                    pendingText = null
                    emittedStatus = SpeakStatus.MissingData
                } else {
                    ready = true
                    emittedStatus = SpeakStatus.Ready
                    textToSpeak = pendingText
                    pendingText = null
                    readyEngine = current
                }
            }
        }

        if (releaseEngine != null) {
            safeRelease(releaseEngine)
        }
        if (emittedStatus != null) {
            _status.tryEmit(emittedStatus!!)
        }
        if (readyEngine != null && textToSpeak != null) {
            val result = safeSpeak(readyEngine!!, textToSpeak!!)
            if (result == TextToSpeech.ERROR) {
                var toRelease: TtsEngine? = null
                synchronized(lock) {
                    if (engine === readyEngine) {
                        toRelease = engine
                        engine = null
                        initializing = false
                        initialized = false
                        ready = false
                        pendingText = null
                        generation++
                    }
                }
                safeRelease(toRelease)
                _status.tryEmit(SpeakStatus.Unsupported)
            }
        }
    }

    private fun isLanguageUnavailable(result: Int): Boolean =
        result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED

    private fun safeSetLanguage(target: TtsEngine): Int = try {
        target.setLanguage(lang)
    } catch (_: Throwable) {
        TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun safeSpeak(target: TtsEngine, text: String): Int = try {
        val utteranceId = "wn-${utteranceCounter.incrementAndGet()}"
        target.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
    } catch (_: Throwable) {
        TextToSpeech.ERROR
    }

    private fun safeRelease(target: TtsEngine?) {
        if (target == null) return
        try {
            target.stop()
        } catch (_: Throwable) {
            // Bỏ qua lỗi khi engine đã dừng hoặc chưa bind xong
        }
        try {
            target.shutdown()
        } catch (_: Throwable) {
            // Bỏ qua lỗi khi giải phóng engine
        }
    }
}
