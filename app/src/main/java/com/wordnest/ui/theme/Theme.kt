package com.wordnest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = HoneyYellow,
    onPrimary = Color(0xFF2B2118),
    secondary = Color(0xFF2E7D5B),
    onSecondary = Color.White,
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFF7EEDF),
    onSurfaceVariant = Color(0xFF7A6F63),
    error = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = HoneyYellow,
    onPrimary = Color(0xFF2B2118),
    secondary = Color(0xFF56B88A),
    onSecondary = Color(0xFF161310),
    background = Color(0xFF161310),
    onBackground = Color(0xFFF5EFE6),
    surface = Color(0xFF26211C),
    onSurface = Color(0xFFF5EFE6),
    surfaceVariant = Color(0xFF312A23),
    onSurfaceVariant = Color(0xFFC2B6A7),
    error = DangerRed
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)

@Composable
fun WordNestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    isDarkThemeActive = darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
