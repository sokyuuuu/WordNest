package com.wordnest.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnSectionLabel
import com.wordnest.ui.decks.components.EmptyState
import com.wordnest.ui.decks.components.WordRow

/**
 * Màn hình Tìm kiếm từ vựng toàn cục qua mọi Tổ (F07 — T047–T049, T051):
 * - Tìm không phân biệt hoa/thường theo một phần từ, nghĩa hoặc câu ví dụ; thoát an toàn ký tự `%`, `_`, `\`.
 * - Mỗi kết quả ghi rõ Tổ chứa từ và cho phép tap mở ngay chi tiết Tổ đó (`deck/{deckId}`).
 * - Có [imePadding] để bàn phím mềm không che khuất danh sách kết quả trên màn nhỏ (Redmi 9C).
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            WnHeader(
                title = "Tìm kiếm từ vựng",
                onBack = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onNavigateBack()
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text(
                        text = "Tìm theo từ, nghĩa hoặc ví dụ...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Xóa từ khóa tìm kiếm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = searchFieldColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isQueryBlank -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            emoji = "🔍",
                            title = "Tìm từ qua mọi Tổ",
                            subtitle = "Gõ một phần từ tiếng Anh (ví dụ: \"air\"), nghĩa tiếng Việt hoặc câu ví dụ để tìm nhanh.",
                        )
                    }
                }

                uiState.isSearching && uiState.results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                uiState.hasSearchedAndEmpty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            emoji = "🪺",
                            title = "Không tìm thấy từ nào",
                            subtitle = "Không có từ phù hợp với \"${uiState.query.trim()}\". Thử từ khóa khác nhé!",
                        )
                    }
                }

                else -> {
                    WnSectionLabel(text = "Kết quả (${uiState.results.size})")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(
                            items = uiState.results,
                            key = { it.word.wordId },
                        ) { item ->
                            WordRow(
                                word = item.word,
                                deckLabel = item.deckName,
                                deckIcon = item.deckIcon,
                                deckColor = item.deckColor,
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onOpenDeck(item.word.deckId)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
