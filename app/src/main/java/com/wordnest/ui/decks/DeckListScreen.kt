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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.wordnest.data.local.entity.Deck
import com.wordnest.ui.decks.components.ConfirmDialog
import com.wordnest.ui.decks.components.DeckCard
import com.wordnest.ui.decks.components.EmptyState

/**
 * Màn hình danh sách Tổ từ vựng (F03): thay thế màn giữ chỗ ở tab "Các Tủ".
 * Hiển thị danh sách thẻ Tổ thật từ Room kèm thanh tiến độ [com.wordnest.ui.components.WnProgressBar],
 * trạng thái trống kèm nút "Tạo Tổ" và hộp thoại xác nhận xoá Tổ (F02).
 */
@Composable
fun DeckListScreen(
    viewModel: DeckViewModel,
    onCreateDeck: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onEditDeck: (String) -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is DeckEvent.ShowMessage) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Các Tủ của bạn",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Quản lý các Tổ từ vựng của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenSearch != null) {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Tìm kiếm từ vựng",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    IconButton(
                        onClick = onCreateDeck,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Tạo Tổ",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                uiState.decks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            title = "Chưa có Tổ nào",
                            subtitle = "Hãy tạo Tổ đầu tiên để bắt đầu nuôi lớn vốn từ mỗi ngày!",
                            actionLabel = "Tạo Tổ",
                            onActionClick = onCreateDeck,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(
                            items = uiState.decks,
                            key = { it.deck.deckId },
                        ) { item ->
                            DeckCard(
                                deckWithStats = item,
                                onClick = { onOpenDeck(item.deck.deckId) },
                                onEdit = { onEditDeck(item.deck.deckId) },
                                onDelete = { deckToDelete = item.deck },
                            )
                        }
                    }
                }
            }
        }
    }

    deckToDelete?.let { deck ->
        ConfirmDialog(
            title = "Xóa Tổ từ vựng?",
            message = "Xoá Tổ \"${deck.nameDeck}\" sẽ xoá cả toàn bộ từ bên trong. Bạn có chắc muốn tiếp tục?",
            confirmText = "Xóa",
            dismissText = "Huỷ",
            onConfirm = {
                viewModel.deleteDeck(deck.deckId)
                deckToDelete = null
            },
            onDismiss = { deckToDelete = null },
        )
    }
}
