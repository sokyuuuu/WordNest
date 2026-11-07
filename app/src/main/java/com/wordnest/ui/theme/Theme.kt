package com.wordnest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 color scheme ánh tới token WordNest trong [WnColor].
 * `primary` là nút chính VÀNG #FFC857 với chữ nâu đậm (nội dung nút do
 * `onPrimary` quy định) — khớp Figma; green dùng cho màu "trúng"/progress
 * qua các ô ngoài scheme (WnColor.Green) hoặc tertiary.
 */
private val LightScheme = lightColorScheme(
    primary = WnColor.Amber,
    onPrimary = WnColor.Brown,
    primaryContainer = WnColor.Amber,
    onPrimaryContainer = WnColor.Brown,
    secondary = WnColor.Green,
    onSecondary = WnColor.White,
    secondaryContainer = WnColor.GreenSoft,
    onSecondaryContainer = WnColor.Brown,
    tertiary = WnColor.GreenDeep,
    onTertiary = WnColor.White,
    background = WnColor.Cream,
    onBackground = WnColor.Brown,
    surface = WnColor.White,
    onSurface = WnColor.Brown,
    surfaceVariant = WnColor.Cream,
    onSurfaceVariant = WnColor.BrownMuted,
    outline = WnColor.Beige,
    outlineVariant = WnColor.Beige,
    error = WnColor.Red,
    onError = WnColor.White,
)

private val DarkScheme = darkColorScheme(
    primary = WnColor.Amber,
    onPrimary = WnColor.Brown,
    primaryContainer = WnColor.Amber,
    onPrimaryContainer = WnColor.Brown,
    secondary = WnColor.GreenDark,
    onSecondary = WnColor.CreamDark,
    secondaryContainer = WnColor.CardDark,
    onSecondaryContainer = WnColor.BrownDark,
    tertiary = WnColor.GreenDark,
    onTertiary = WnColor.CreamDark,
    background = WnColor.CreamDark,
    onBackground = WnColor.BrownDark,
    surface = WnColor.CardDark,
    onSurface = WnColor.BrownDark,
    surfaceVariant = WnColor.CreamDark,
    onSurfaceVariant = WnColor.BrownMutedDark,
    outline = WnColor.BeigeDark,
    outlineVariant = WnColor.BeigeDark,
    error = WnColor.Red,
    onError = WnColor.White,
)

/**
 * Bo góc theo style guide mục 5: ô nhập 12, thẻ 16, hộp thoại 20,
 * nút chính bo tròn hoàn toàn (piston).
 */
private val WnShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(percent = 50),
)

/**
 * [darkTheme] được MainActivity truyền từ DataStore (F18). Trên các bề mặt
 * thiếu cá thể cài đặt (preview) dùng mặc định hệ thống.
 */
@Composable
fun WordNestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = WnTypography,
        shapes = WnShapes,
        content = content,
    )
}
