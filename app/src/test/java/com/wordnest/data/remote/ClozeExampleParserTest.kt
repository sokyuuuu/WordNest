package com.wordnest.data.remote

import org.junit.Assert.*
import org.junit.Test

class ClozeExampleParserTest {

    // TC01: JSON hợp lệ
    @Test
    fun validJson_returnsExample() {
        val raw = """
            {
                "example": "We arrived at the ___ early.",
                "translation": "Chúng tôi đến sân bay sớm."
            }
        """.trimIndent()

        val result = ClozeExampleParser.parse(raw, "airport")

        assertNotNull(result)
        assertEquals(
            "We arrived at the ___ early.",
            result?.example
        )
        assertEquals(
            "Chúng tôi đến sân bay sớm.",
            result?.translation
        )
    }

    // TC02: JSON có Markdown
    @Test
    fun markdownJson_returnsExample() {
        val raw = """
            ```json
            {
                "example": "I eat an ___ every day.",
                "translation": "Tôi ăn một quả táo mỗi ngày."
            }
            ```
        """.trimIndent()

        assertNotNull(
            ClozeExampleParser.parse(raw, "apple")
        )
    }

    // TC03: JSON không hợp lệ
    @Test
    fun invalidJson_returnsNull() {
        assertNull(
            ClozeExampleParser.parse(
                "Invalid JSON",
                "airport"
            )
        )
    }

    // TC04: Không có chỗ trống
    @Test
    fun missingBlank_returnsNull() {
        val raw = """
            {
                "example": "We arrived early.",
                "translation": "Chúng tôi đến sớm."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }

    // TC05: Có nhiều chỗ trống
    @Test
    fun multipleBlanks_returnsNull() {
        val raw = """
            {
                "example": "I went to ___ with ___.",
                "translation": "Tôi đã đi cùng bạn."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }

    // TC06: Câu hỏi chứa đáp án
    @Test
    fun exposedAnswer_returnsNull() {
        val raw = """
            {
                "example": "The airport is near the ___.",
                "translation": "Sân bay ở gần đó."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }

    // TC07: Thiếu bản dịch
    @Test
    fun missingTranslation_returnsNull() {
        val raw = """
            {
                "example": "I went to the ___."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }

    // TC08: Bản dịch rỗng
    @Test
    fun emptyTranslation_returnsNull() {
        val raw = """
            {
                "example": "I went to the ___.",
                "translation": ""
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }

    // TC09: Từ vựng đầu vào rỗng
    @Test
    fun emptyWord_returnsNull() {
        val raw = """
            {
                "example": "I went to the ___.",
                "translation": "Tôi đã đi đến đó."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "")
        )
    }

    // TC10: Đáp án viết hoa vẫn bị phát hiện
    @Test
    fun uppercaseAnswer_returnsNull() {
        val raw = """
            {
                "example": "AIRPORT is near the ___.",
                "translation": "Sân bay ở gần đó."
            }
        """.trimIndent()

        assertNull(
            ClozeExampleParser.parse(raw, "airport")
        )
    }
}
