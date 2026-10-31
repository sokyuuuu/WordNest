package com.wordnest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.local.entity.Deck
import com.wordnest.ui.components.ConfirmDialog
import com.wordnest.ui.components.DeckCard
import com.wordnest.ui.components.EmptyState
import com.wordnest.ui.theme.CardWarm
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.HoneyYellow
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.DeckEvent
import com.wordnest.ui.viewmodel.DeckViewModel

@Composable
fun DeckListScreen(
    viewModel: DeckViewModel,
    onOpenDeck: (String) -> Unit,
    onCreateDeck: () -> Unit,
    onEditDeck: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DeckEvent.ShowMessage) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header khớp Figma: "Tổ từ vựng" + "Quản lý các bộ từ của bạn" + nút Search & nút +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tổ từ vựng",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Quản lý các bộ từ của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CardWarm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onCreateDeck,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HoneyYellow)

                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tạo Tổ từ mới",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (decks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Chưa có Tổ từ vựng nào",
                        subtitle = "Hãy tạo Tổ đầu tiên để bắt đầu nuôi dưỡng vốn từ vựng của bạn!",
                        actionLabel = "+ Tạo Tổ từ mới",
                        onActionClick = onCreateDeck
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    items(decks, key = { it.deck.deckId }) { item ->
                        DeckCard(
                            deckWithStats = item,
                            onClick = { onOpenDeck(item.deck.deckId) },
                            onEdit = { onEditDeck(item.deck.deckId) },
                            onDelete = { deckToDelete = item.deck }
                        )
                    }
                }
            }
        }
    }

    deckToDelete?.let { deck ->
        ConfirmDialog(
            title = "Xóa Tổ từ vựng?",
            message = "Tất cả từ vựng trong Tổ \"${deck.nameDeck}\" sẽ bị xóa vĩnh viễn. Hành động này không thể hoàn tác.",
            confirmText = "Xóa Tổ",
            onConfirm = {
                viewModel.deleteDeck(deck.deckId)
                deckToDelete = null
            },
            onDismiss = { deckToDelete = null }
        )
    }
}
