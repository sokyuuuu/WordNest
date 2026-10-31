package com.wordnest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.R
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.entity.Word
import com.wordnest.data.repository.WordRepository
import com.wordnest.ui.components.ConfirmDialog
import com.wordnest.ui.components.DeckCard
import com.wordnest.ui.components.EmptyState
import com.wordnest.ui.components.WordRow
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.HoneyYellow
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.DeckViewModel
import com.wordnest.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    deckViewModel: DeckViewModel,
    wordRepository: WordRepository,
    onNavigateBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onEditDeck: (String) -> Unit,
    onEditWord: (deckId: String, wordId: String) -> Unit
) {
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val trimmedQuery = query.trim()
    val scope = rememberCoroutineScope()

    var deckPendingDelete by remember { mutableStateOf<DeckWithStats?>(null) }
    var wordPendingDelete by remember { mutableStateOf<Word?>(null) }

    DisposableEffect(Unit) {
        onDispose { searchViewModel.onQueryChange("") }
    }

    Scaffold(
        containerColor = CreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CreamBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HoneyYellow)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tìm kiếm",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = searchViewModel::onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = BorderWarm,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            val matchedDecks = results.decks
            val matchedWords = results.words

            if (trimmedQuery.isEmpty()) {
                EmptyState(
                    emoji = "🔍",
                    title = "Nhập từ khóa để tìm kiếm",
                    subtitle = "Tìm nhanh Tổ từ hoặc từ vựng theo tiếng Anh / nghĩa tiếng Việt."
                )
            } else if (matchedDecks.isEmpty() && matchedWords.isEmpty()) {
                EmptyState(
                    emoji = "🪺",
                    title = "Không tìm thấy kết quả",
                    subtitle = "Không có Tổ từ hoặc từ vựng nào khớp với \"$trimmedQuery\"."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (matchedDecks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tổ từ (${matchedDecks.size})",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        items(matchedDecks, key = { "deck_${it.deck.deckId}" }) { item ->
                            DeckCard(
                                deckWithStats = item,
                                onClick = { onOpenDeck(item.deck.deckId) },
                                onEdit = { onEditDeck(item.deck.deckId) },
                                onDelete = { deckPendingDelete = item }
                            )
                        }
                    }

                    if (matchedWords.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Từ vựng (${matchedWords.size})",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        items(matchedWords, key = { "word_${it.wordId}" }) { word ->
                            WordRow(
                                word = word,
                                onClick = { onOpenDeck(word.deckId) },
                                onEdit = { onEditWord(word.deckId, word.wordId) },
                                onDelete = { wordPendingDelete = word }
                            )
                        }
                    }
                }
            }
        }
    }

    deckPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Xóa Tổ \"${target.deck.nameDeck}\"?",
            message = "Tất cả ${target.wordCount} từ vựng trong Tổ này cũng sẽ bị xóa.",
            confirmText = "Xóa Tổ",
            onConfirm = {
                deckViewModel.deleteDeck(target.deck.deckId)
                deckPendingDelete = null
            },
            onDismiss = { deckPendingDelete = null }
        )
    }

    wordPendingDelete?.let { word ->
        ConfirmDialog(
            title = "Xóa từ \"${word.term}\"?",
            message = "Bạn có chắc chắn muốn xóa từ \"${word.term}\" khỏi Tổ từ không?",
            confirmText = "Xóa từ",
            onConfirm = {
                scope.launch {
                    wordRepository.deleteWord(word.wordId)
                }
                wordPendingDelete = null
            },
            onDismiss = { wordPendingDelete = null }
        )
    }
}
