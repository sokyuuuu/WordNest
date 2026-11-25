package com.wordnest.ui.decks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.Word
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.theme.wnDeckColorForKey
import com.wordnest.util.WordStatus

/**
 * Huy hiệu trạng thái chim của một từ (🥚 Trứng / 🐣 Chim non / 🕊️ Chim bay).
 * Dùng chung 1 nguồn chân lý [WordStatus.emoji] & [WordStatus.label] và màu từ [MaterialTheme.colorScheme]
 * (tự thích ứng chế độ sáng/tối, không hard-code hex).
 */
@Composable
fun StatusChip(
    status: String?,
    modifier: Modifier = Modifier,
) {
    StatusChip(
        status = WordStatus.fromStorage(status),
        modifier = modifier,
    )
}

@Composable
fun StatusChip(
    status: WordStatus,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = when (status) {
        WordStatus.EGG -> MaterialTheme.colorScheme.primary
        WordStatus.CHICK -> if (isDark) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        WordStatus.BIRD -> MaterialTheme.colorScheme.secondary
    }
    val contentColor = when (status) {
        WordStatus.EGG -> MaterialTheme.colorScheme.onPrimary
        WordStatus.CHICK -> if (isDark) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
        WordStatus.BIRD -> MaterialTheme.colorScheme.onSecondary
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${status.emoji} ${status.label}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * Dòng hiển thị 1 từ vựng trong màn chi tiết Tổ (F06) hoặc kết quả tìm kiếm toàn cục (F07):
 * - Từ tiếng Anh + phiên âm (1 dòng ellipsis) + huy hiệu [StatusChip] 🥚🐣🕊️
 * - Nghĩa tiếng Việt, câu ví dụ (nếu có), ghi chú (nếu có)
 * - Huy hiệu Tổ chứa từ khi truyền [deckLabel] (dùng ở màn Tìm kiếm toàn cục)
 * - Menu tùy chọn Sửa / Xoá khi truyền [onEdit] / [onDelete].
 */
@Composable
fun WordRow(
    word: Word,
    modifier: Modifier = Modifier,
    deckLabel: String? = null,
    deckIcon: String? = null,
    deckColor: String? = null,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hasActions = onEdit != null || onDelete != null

    WnCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.term,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!word.phonetic.isNullOrBlank()) {
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = word.status)
                    if (hasActions) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Tùy chọn từ ${word.term}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Chỉnh sửa",
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground,
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onEdit()
                                        },
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Xóa từ",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!word.example.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${word.example}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!word.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 ${word.note}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!deckLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(10.dp))
                            .background(wnDeckColorForKey(deckColor ?: DeckStyle.DEFAULT_COLOR))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = deckIconVector(deckIcon ?: DeckStyle.DEFAULT_ICON),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tổ: $deckLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (onClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mở Tổ $deckLabel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
