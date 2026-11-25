package com.wordnest.util

/**
 * Ba trạng thái của một từ, theo motif "chim" của WordNest.
 * Lưu trong SQLite/Firestore dạng chuỗi chữ thường ([storage]): egg | chick | bird.
 */
enum class WordStatus(
    val label: String,
    val emoji: String,
    val storage: String,
) {
    EGG("Trứng", "🥚", "egg"),
    CHICK("Chim non", "🐣", "chick"),
    BIRD("Chim bay", "🕊️", "bird");

    companion object {
        fun fromStorage(raw: String?): WordStatus =
            entries.firstOrNull { it.storage.equals(raw, ignoreCase = true) } ?: EGG

        fun isValidStorage(raw: String?): Boolean =
            raw != null && entries.any { it.storage == raw }
    }
}
