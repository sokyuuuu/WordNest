package com.wordnest.ui.theme

import androidx.compose.ui.graphics.Color

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
}
