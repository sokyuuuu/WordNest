package com.wordnest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wordnest.R

/**
 * Font Nunito theo style guide (bo tròn, hỗ trợ tiếng Việt).
 * Trọng số: 400 Regular, 600 SemiBold, 700 Bold, 800 ExtraBold.
 */
private val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.W400),
    Font(R.font.nunito_semibold, FontWeight.W600),
    Font(R.font.nunito_bold, FontWeight.W700),
    Font(R.font.nunito_extrabold, FontWeight.W800),
)

private fun nunito(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    letterSpacing: Int = 0,
): TextStyle = TextStyle(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Các cỡ theo style guide mục 3:
 * - Tiêu đề màn hình 24 Bold, tiêu đề phần 18 Bold, nội dung 16 Regular,
 *   nhấn mạnh 16 SemiBold, chú thích 12 Regular, số lớn 32 ExtraBold.
 */
val WnTypography = Typography(
    displayLarge = nunito(36, FontWeight.ExtraBold, 44),
    displayMedium = nunito(32, FontWeight.ExtraBold, 40),
    displaySmall = nunito(28, FontWeight.ExtraBold, 36),
    headlineMedium = nunito(24, FontWeight.Bold, 32),
    headlineSmall = nunito(22, FontWeight.Bold, 30),
    titleLarge = nunito(20, FontWeight.Bold, 28),
    titleMedium = nunito(18, FontWeight.Bold, 26),
    titleSmall = nunito(16, FontWeight.SemiBold, 24),
    bodyLarge = nunito(16, FontWeight.SemiBold, 26),
    bodyMedium = nunito(16, FontWeight.Normal, 26),
    bodySmall = nunito(14, FontWeight.Normal, 22),
    labelLarge = nunito(16, FontWeight.Bold, 24),
    labelMedium = nunito(14, FontWeight.SemiBold, 20),
    labelSmall = nunito(12, FontWeight.Normal, 16),
)
