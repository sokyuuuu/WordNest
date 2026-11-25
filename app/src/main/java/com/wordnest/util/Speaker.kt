package com.wordnest.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Cổng phát âm – để UI không phụ thuộc trực tiếp vào android.speech.tts. */
interface Speaker {
    /** Luồng trạng thái khởi tạo / lỗi của bộ phát âm. */
    val status: Flow<SpeakStatus> get() = emptyFlow()

    /** Đọc to [text] (tiếng Anh), huỷ âm đang phát. */
    fun speak(text: String)

    /** Dừng âm đang phát và giải phóng tài nguyên bộ đọc. */
    fun shutdown()
}

sealed interface SpeakStatus {
    data object Ready : SpeakStatus
    data object MissingData : SpeakStatus     // máy chưa có dữ liệu giọng Anh
    data object Unsupported : SpeakStatus     // máy không hỗ trợ TTS
}
