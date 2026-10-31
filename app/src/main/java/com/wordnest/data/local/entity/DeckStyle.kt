package com.wordnest.data.local.entity

object DeckStyle {
    const val ICON_BOOK = "book"
    const val ICON_PLANE = "plane"
    const val ICON_GLOBE = "globe"
    const val ICON_BULB = "bulb"
    const val ICON_BRIEFCASE = "briefcase"

    const val COLOR_MINT = "mint"
    const val COLOR_YELLOW = "yellow"
    const val COLOR_GREY = "grey"
    const val COLOR_PINK = "pink"
    const val COLOR_TEAL = "teal"

    const val DEFAULT_ICON = ICON_BOOK
    const val DEFAULT_COLOR = COLOR_MINT

    val AVAILABLE_ICONS = listOf(
        ICON_BOOK,
        ICON_PLANE,
        ICON_GLOBE,
        ICON_BULB,
        ICON_BRIEFCASE,
        "school",
        "code",
        "music",
        "favorite",
        "star"
    )

    val AVAILABLE_COLORS = listOf(
        COLOR_MINT,
        COLOR_YELLOW,
        COLOR_GREY,
        COLOR_PINK,
        COLOR_TEAL
    )

    fun isValidIcon(icon: String): Boolean = icon in AVAILABLE_ICONS
    fun isValidColor(color: String): Boolean = color in AVAILABLE_COLORS

    fun emojiForIcon(icon: String): String = when (icon) {
        ICON_BOOK -> "📚"
        ICON_PLANE -> "✈️"
        ICON_GLOBE -> "🌍"
        ICON_BULB -> "💡"
        ICON_BRIEFCASE -> "💼"
        "school" -> "🎓"
        "code" -> "💻"
        "music" -> "🎵"
        "favorite" -> "❤️"
        "star" -> "⭐"
        else -> "🪺"
    }
}
