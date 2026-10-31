package com.wordnest.data.local.entity

object WordStatus {
    const val EGG = "EGG"
    const val CHICK = "CHICK"
    const val BIRD = "BIRD"

    val ALL = listOf(EGG, CHICK, BIRD)

    fun isValid(status: String): Boolean = status in ALL

    fun labelVi(status: String): String = when (status) {
        EGG -> "Trứng"
        CHICK -> "Chim non"
        BIRD -> "Chim trưởng thành"
        else -> "Trứng"
    }

    fun emojiFor(status: String): String = when (status) {
        EGG -> "🥚"
        CHICK -> "🐣"
        BIRD -> "🦅"
        else -> "🥚"
    }
}
