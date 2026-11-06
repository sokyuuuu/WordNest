package com.wordnest.util

/**
 * Ba trạng thái của một từ, theo motif "chim" của WordNest.
 */
enum class WordStatus(val label: String) {
    EGG("Trứng"),
    CHICK("Chim non"),
    BIRD("Chim bay");
}
