package com.wordnest.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.wordnest.data.local.entity.DeckStyle

var isDarkThemeActive by mutableStateOf(false)

val CreamBackground: Color
    get() = if (isDarkThemeActive) Color(0xFF161310) else Color(0xFFFFF8EC)

val CreamSurface: Color
    get() = if (isDarkThemeActive) Color(0xFF211D18) else Color(0xFFFAF3E3)

val CardWhite: Color
    get() = if (isDarkThemeActive) Color(0xFF26211C) else Color(0xFFFFFFFF)

val CardWarm: Color
    get() = if (isDarkThemeActive) Color(0xFF312A23) else Color(0xFFF7EEDF)

val BorderWarm: Color
    get() = if (isDarkThemeActive) Color(0xFF423930) else Color(0xFFE8DDD0)

val HoneyYellow = Color(0xFFF2B93B)
val HoneyYellowDark = Color(0xFFD99A1E)

val ForestGreen: Color
    get() = if (isDarkThemeActive) Color(0xFF56B88A) else Color(0xFF2E7D5B)

val ForestGreenLight: Color
    get() = if (isDarkThemeActive) Color(0xFF203B2E) else Color(0xFFDCEEE3)

val TextPrimary: Color
    get() = if (isDarkThemeActive) Color(0xFFF5EFE6) else Color(0xFF2B2118)

val TextSecondary: Color
    get() = if (isDarkThemeActive) Color(0xFFC2B6A7) else Color(0xFF7A6F63)

val TextMuted: Color
    get() = if (isDarkThemeActive) Color(0xFF918579) else Color(0xFFA3988C)

val DangerRed = Color(0xFFD9534F)
val DangerRedBg: Color
    get() = if (isDarkThemeActive) Color(0xFF3B2020) else Color(0xFFFDECEA)

val StatusEggBg: Color
    get() = if (isDarkThemeActive) Color(0xFF38322C) else Color(0xFFF3EFE6)
val StatusEggText: Color
    get() = if (isDarkThemeActive) Color(0xFFD8CFC4) else Color(0xFF6E655C)

val StatusChickBg: Color
    get() = if (isDarkThemeActive) Color(0xFF423414) else Color(0xFFFFF2CC)
val StatusChickText: Color
    get() = if (isDarkThemeActive) Color(0xFFFFD666) else Color(0xFF9C6E00)

val StatusBirdBg: Color
    get() = if (isDarkThemeActive) Color(0xFF1F3B2E) else Color(0xFFDCEEE3)
val StatusBirdText: Color
    get() = if (isDarkThemeActive) Color(0xFF82E0B5) else Color(0xFF1E563F)

val DeckColorMint = Color(0xFFA8E0C9)
val DeckColorYellow = Color(0xFFF7D574)
val DeckColorGrey = Color(0xFFD6DCE2)
val DeckColorPink = Color(0xFFF2B8B5)
val DeckColorTeal = Color(0xFF8EB8E0)

fun deckColorForKey(key: String?): Color = when (key?.lowercase()) {
    DeckStyle.COLOR_MINT -> if (isDarkThemeActive) Color(0xFF27493B) else DeckColorMint
    DeckStyle.COLOR_YELLOW -> if (isDarkThemeActive) Color(0xFF524118) else DeckColorYellow
    DeckStyle.COLOR_GREY -> if (isDarkThemeActive) Color(0xFF343A40) else DeckColorGrey
    DeckStyle.COLOR_PINK -> if (isDarkThemeActive) Color(0xFF4F2D2C) else DeckColorPink
    DeckStyle.COLOR_TEAL -> if (isDarkThemeActive) Color(0xFF263F57) else DeckColorTeal
    else -> if (isDarkThemeActive) Color(0xFF27493B) else DeckColorMint
}
