package com.wordnest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.wordnest.data.local.entity.DeckStyle

/**
 * Bảng màu WordNest khớp design Figma + style guide (design/style_guide.md):
 * - Sáng: nền kem #FFF8EC, thẻ trắng, chữ nâu đậm #3E2F1F, nút chính vàng #FFC857.
 * - Tối: nền #1E1B16, thẻ #2A2620, chữ #F5EBDD, chủ đạo xanh #6FCF97.
 */
object WnColor {
    // Light
    val Cream = Color(0xFFFFF8EC)      // nền màn hình
    val Brown = Color(0xFF3E2F1F)      // chữ chính
    val BrownMuted = Color(0xFF7A6A58) // chữ phụ, chú thích
    val Amber = Color(0xFFFFC857)      // nút chính, back button, huy hiệu
    val Beige = Color(0xFFEADFCB)      // viền thẻ, track progress, đường kẻ
    val Green = Color(0xFF4CAF7A)      // trúng, đang chọn, progress, switch
    val GreenDeep = Color(0xFF2E7D55)  // link, label tips (Xem tất cả)
    val GreenSoft = Color(0xFFCFEBD9)  // container icon/label đã chọn
    val Red = Color(0xFFD9534F)        // sai, xóa
    val White = Color(0xFFFFFFFF)      // thẻ, hộp thoại

    // Dark
    val CreamDark = Color(0xFF1E1B16)
    val CardDark = Color(0xFF2A2620)
    val BrownDark = Color(0xFFF5EBDD)
    val BrownMutedDark = Color(0xFFB8A892)
    val BeigeDark = Color(0xFF3A342B)
    val GreenDark = Color(0xFF6FCF97)

    // Deck pastel tokens (D12 — sáng & tối)
    val DeckMint = Color(0xFFA8E0C9)
    val DeckYellow = Color(0xFFF7D574)
    val DeckGrey = Color(0xFFD6DCE2)
    val DeckPink = Color(0xFFF2B8B5)
    val DeckTeal = Color(0xFF8EB8E0)

    val DeckMintDark = Color(0xFF27493B)
    val DeckYellowDark = Color(0xFF524118)
    val DeckGreyDark = Color(0xFF343A40)
    val DeckPinkDark = Color(0xFF4F2D2C)
    val DeckTealDark = Color(0xFF263F57)
}

/**
 * Ánh xạ mã màu Tổ (`mint`, `yellow`, `grey`, `pink`, `teal`) sang token pastel trong [WnColor],
 * tự thích ứng theo `MaterialTheme.colorScheme` (tôn trọng cả cài đặt chế độ tối trong DataStore).
 */
@Composable
fun wnDeckColorForKey(key: String?): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (key?.lowercase()) {
        DeckStyle.COLOR_MINT -> if (isDark) WnColor.DeckMintDark else WnColor.DeckMint
        DeckStyle.COLOR_YELLOW -> if (isDark) WnColor.DeckYellowDark else WnColor.DeckYellow
        DeckStyle.COLOR_GREY -> if (isDark) WnColor.DeckGreyDark else WnColor.DeckGrey
        DeckStyle.COLOR_PINK -> if (isDark) WnColor.DeckPinkDark else WnColor.DeckPink
        DeckStyle.COLOR_TEAL -> if (isDark) WnColor.DeckTealDark else WnColor.DeckTeal
        else -> if (isDark) WnColor.DeckMintDark else WnColor.DeckMint
    }
}
