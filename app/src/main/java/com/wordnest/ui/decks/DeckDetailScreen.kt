package com.wordnest.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.local.entity.Word
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.ui.components.WnSectionLabel
import com.wordnest.ui.components.WnStatCard
import com.wordnest.ui.decks.components.ConfirmDialog
import com.wordnest.ui.decks.components.EmptyState
import com.wordnest.ui.decks.components.WordRow
import com.wordnest.util.WordStatus
import kotlin.math.roundToInt

/**
 * Màn hình chi tiết Tổ từ vựng (F06):
 * - [WnHeader] hiển thị tên Tổ, nút quay lại và nút sửa Tổ.
 * - Tóm tắt thống kê bằng [WnStatCard] (Tổng từ, Đã thuộc) + thẻ tiến độ % ([WnProgressBar] & đếm 🥚/🐣/🕊️).
 * - [LazyColumn] danh sách từ với [WordRow] kèm huy hiệu trạng thái chim, tự cập nhật qua Room Flow.
 * - Trạng thái trống [EmptyState] kèm nút "Thêm từ", hoặc FAB "Thêm từ" khi đã có từ.
 * - Tự động quay về danh sách khi Tổ bị xoá ở màn khác ([DeckDetailUiState.isMissing], T035).
 */
@Composable
fun DeckDetailScreen(
    viewModel: DeckDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditDeck: () -> Unit,
    onAddWord: () -> Unit,
    onEditWord: ((String) -> Unit)? = null,
    onDeleteWordRequest: ((Word) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var didPopOnMissing by remember(viewModel.deckId) { mutableStateOf(false) }
    var wordToDelete by remember { mutableStateOf<Word?>(null) }

    LaunchedEffect(uiState.isMissing) {
        if (uiState.isMissing && !didPopOnMissing) {
            didPopOnMissing = true
            onNavigateBack()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is DeckDetailEvent.ShowMessage) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val percent = (uiState.progressPercent * 100f).roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading && !uiState.isMissing && uiState.words.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddWord,
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text = "Thêm từ",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            WnHeader(
                title = uiState.deck?.nameDeck ?: "Chi tiết Tổ",
                onBack = onNavigateBack,
                trailing = {
                    if (uiState.deck != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onEditDeck,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Chỉnh sửa Tổ",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                },
            )

            when {
                uiState.isLoading || uiState.isMissing -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = if (uiState.words.isEmpty()) 16.dp else 88.dp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        item(key = "deck_summary") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val description = uiState.deck?.description
                                if (!description.isNullOrBlank()) {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    WnStatCard(
                                        value = uiState.totalWords.toString(),
                                        label = "Tổng từ",
                                        icon = "📚",
                                        modifier = Modifier.weight(1f),
                                    )
                                    WnStatCard(
                                        value = uiState.learnedWords.toString(),
                                        label = "Đã thuộc",
                                        icon = "✓",
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                WnCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Tiến độ Tổ",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                text = "$percent%",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                        WnProgressBar(progress = uiState.progressPercent)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "${WordStatus.EGG.emoji} ${uiState.eggCount} ${WordStatus.EGG.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "${WordStatus.CHICK.emoji} ${uiState.chickCount} ${WordStatus.CHICK.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "${WordStatus.BIRD.emoji} ${uiState.birdCount} ${WordStatus.BIRD.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.words.isEmpty()) {
                            item(key = "empty_words") {
                                Spacer(modifier = Modifier.height(8.dp))
                                EmptyState(
                                    emoji = WordStatus.EGG.emoji,
                                    title = "Tổ chưa có từ nào",
                                    subtitle = "Thêm từ vựng đầu tiên để bắt đầu ấp trứng cùng WordNest!",
                                    actionLabel = "Thêm từ",
                                    onActionClick = onAddWord,
                                )
                            }
                        } else {
                            item(key = "words_section_label") {
                                WnSectionLabel(text = "Danh sách từ (${uiState.totalWords})")
                            }
                            items(
                                items = uiState.words,
                                key = { it.wordId },
                            ) { word ->
                                WordRow(
                                    word = word,
                                    onClick = onEditWord?.let { callback -> { callback(word.wordId) } },
                                    onEdit = onEditWord?.let { callback -> { callback(word.wordId) } },
                                    onDelete = {
                                        if (onDeleteWordRequest != null) {
                                            onDeleteWordRequest(word)
                                        } else {
                                            wordToDelete = word
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    wordToDelete?.let { word ->
        ConfirmDialog(
            title = "Xóa từ vựng?",
            message = "Bạn có chắc muốn xóa từ \"${word.term}\" khỏi Tổ này? Từ sẽ biến khỏi danh sách, tìm kiếm và thống kê.",
            confirmText = "Xóa",
            dismissText = "Huỷ",
            onConfirm = {
                viewModel.deleteWord(word.wordId)
                wordToDelete = null
            },
            onDismiss = { wordToDelete = null },
        )
    }
}
