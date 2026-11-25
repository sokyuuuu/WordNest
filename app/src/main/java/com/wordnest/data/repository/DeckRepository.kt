package com.wordnest.data.repository

import com.wordnest.data.local.dao.DeckDao
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.dao.DeckWithWords
import com.wordnest.data.local.entity.Deck
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.util.DateTimeUtil
import com.wordnest.util.Validation
import com.wordnest.util.ValidationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository cho các nghiệp vụ Tổ từ:
 *  - F01: Tạo Tổ
 *  - F02: Sửa, xóa mềm Tổ (kèm toàn bộ từ con)
 *  - F03: Xem danh sách Tổ kèm tổng từ & số từ đến hạn
 *  - F07 (phần Tổ): Tìm kiếm Tổ theo tên / mô tả
 */
class DeckRepository(
    private val deckDao: DeckDao,
    private val nowProvider: () -> String = DateTimeUtil::nowIso,
    private val todayProvider: () -> String = DateTimeUtil::todayDate
) {

    /** F03: Danh sách Tổ kèm thống kê */
    fun observeDecksWithStats(userId: String): Flow<List<DeckWithStats>> =
        deckDao.observeDecksWithStats(userId, todayProvider())

    /** Thống kê chi tiết của 1 Tổ */
    fun observeDeckWithStats(deckId: String): Flow<DeckWithStats?> =
        deckDao.observeDeckWithStatsById(deckId, todayProvider())

    fun observeDeckById(deckId: String): Flow<Deck?> =
        deckDao.observeById(deckId)

    fun observeDeckWithWords(deckId: String): Flow<DeckWithWords?> =
        deckDao.observeDeckWithWords(deckId)

    suspend fun getDeckById(deckId: String): Deck? =
        deckDao.getById(deckId)

    /** F07: Tìm kiếm Tổ (nếu keyword rỗng trả về toàn bộ như F03) */
    fun observeSearchDecks(userId: String, keyword: String): Flow<List<DeckWithStats>> {
        val trimmed = keyword.trim()
        return if (trimmed.isEmpty()) {
            deckDao.observeDecksWithStats(userId, todayProvider())
        } else {
            deckDao.observeSearchDecks(userId, Validation.escapeLike(trimmed), todayProvider())
        }
    }

    /** F01: Tạo Tổ mới */
    suspend fun createDeck(
        userId: String,
        name: String,
        description: String? = null,
        icon: String = DeckStyle.DEFAULT_ICON,
        color: String = DeckStyle.DEFAULT_COLOR
    ): Result<Deck> = safeCall {
        Validation.validateDeckInput(name, description, icon, color)
        val cleanName = name.trim()
        val cleanDesc = description?.trim()?.takeIf { it.isNotEmpty() }

        if (deckDao.existsByName(userId, cleanName, excludeId = null)) {
            throw ValidationException("Tổ \"$cleanName\" đã tồn tại")
        }

        val now = nowProvider()
        val deck = Deck(
            deckId = UUID.randomUUID().toString(),
            userId = userId,
            nameDeck = cleanName,
            description = cleanDesc,
            createAt = now,
            icon = icon,
            color = color,
            updatedAt = now,
            deleted = false
        )
        deckDao.insert(deck)
        deck
    }

    /** F02: Sửa tên / mô tả / icon / color Tổ */
    suspend fun updateDeck(
        deckId: String,
        userId: String,
        newName: String,
        newDescription: String?,
        newIcon: String? = null,
        newColor: String? = null
    ): Result<Unit> = safeCall {
        val existing = deckDao.getById(deckId)
            ?: throw NoSuchElementException("Không tìm thấy Tổ (id=$deckId)")
        val resolvedIcon = newIcon ?: existing.icon
        val resolvedColor = newColor ?: existing.color

        Validation.validateDeckInput(newName, newDescription, resolvedIcon, resolvedColor)
        val cleanName = newName.trim()
        val cleanDesc = newDescription?.trim()?.takeIf { it.isNotEmpty() }

        if (deckDao.existsByName(userId, cleanName, excludeId = deckId)) {
            throw ValidationException("Tổ \"$cleanName\" đã tồn tại")
        }

        deckDao.updateInfo(deckId, cleanName, cleanDesc, resolvedIcon, resolvedColor, nowProvider())
    }

    /** F02: Xóa mềm Tổ và toàn bộ từ con (tuân thủ hợp đồng Firestore FB3) */
    suspend fun deleteDeck(deckId: String): Result<Unit> = safeCall {
        deckDao.getById(deckId)
            ?: throw NoSuchElementException("Không tìm thấy Tổ (id=$deckId)")
        deckDao.softDeleteWithWords(deckId, nowProvider())
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
