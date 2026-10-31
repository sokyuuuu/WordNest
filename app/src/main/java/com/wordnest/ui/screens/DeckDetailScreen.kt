package com.wordnest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.data.local.entity.Word
import com.wordnest.data.local.entity.WordStatus
import com.wordnest.ui.components.ConfirmDialog
import com.wordnest.ui.components.EmptyState
import com.wordnest.ui.components.WordRow
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.ForestGreenLight
import com.wordnest.ui.theme.HoneyYellow
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.DeckViewModel
import com.wordnest.ui.viewmodel.WordEvent
import com.wordnest.ui.viewmodel.WordViewModel
import kotlin.math.roundToInt

@Composable
fun DeckDetailScreen(
    deckId: String,
    deckViewModel: DeckViewModel,
    wordViewModel: WordViewModel,
    onNavigateBack: () -> Unit,
    onEditDeck: (String) -> Unit,
    onAddWord: (String) -> Unit,
    onEditWord: (String, String) -> Unit,
    onOpenStudyMode: (String) -> Unit
) {
    val deckStatsFlow = remember(deckId) { deckViewModel.observeDeckWithStats(deckId) }
    val deckWithStats by deckStatsFlow.collectAsStateWithLifecycle(initialValue = null)
    val words by wordViewModel.words.collectAsStateWithLifecycle()
    val statusFilter by wordViewModel.statusFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var wordToDelete by remember { mutableStateOf<Word?>(null) }

    LaunchedEffect(Unit) {
        wordViewModel.events.collect { event ->
            if (event is WordEvent.ShowMessage) {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Top Header khớp Figma: Back, Tên Tổ + mô tả, nút Edit Tổ, nút + Thêm từ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HoneyYellow)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deckWithStats?.deck?.nameDeck ?: "Chi tiết Tổ",
                        style = MaterialTheme.typography.titleLarge,

                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val desc = deckWithStats?.deck?.description
                    if (!desc.isNullOrBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { onEditDeck(deckId) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardWarm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa Tổ",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { onAddWord(deckId) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HoneyYellow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm từ mới",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thẻ thống kê Tổ từ (Tổng số từ | Đã thuộc | Tiến độ)
            DeckStatsHeaderCard(deckWithStats = deckWithStats)

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Nút hành động học tập (Học thẻ từ, Luyện tập, Ghép cặp) -> Sắp ra mắt (P2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudyActionCard(
                    icon = Icons.Default.Style,
                    label = "Học thẻ từ",
                    onClick = { onOpenStudyMode("Học thẻ từ (Flashcard)") },
                    modifier = Modifier.weight(1f)
                )
                StudyActionCard(
                    icon = Icons.Default.Quiz,
                    label = "Luyện tập",
                    onClick = { onOpenStudyMode("Luyện tập trắc nghiệm (Quiz)") },
                    modifier = Modifier.weight(1f)
                )
                StudyActionCard(
                    icon = Icons.Default.Extension,
                    label = "Ghép cặp",
                    onClick = { onOpenStudyMode("Trò chơi Ghép cặp") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bộ lọc trạng thái từ vựng (Tất cả / Trứng / Chim non / Chim trưởng thành)
            val filters = listOf(
                null to "Tất cả",
                WordStatus.EGG to "🥚 Trứng",
                WordStatus.CHICK to "🐣 Chim non",
                WordStatus.BIRD to "🦅 Chim trưởng thành"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { (key, label) ->
                    val selected = statusFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { wordViewModel.onStatusFilterChange(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenLight,
                            selectedLabelColor = ForestGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách từ vựng (F06) hoặc EmptyState
            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Chưa có từ vựng nào",
                        subtitle = if (statusFilter == null) {
                            "Tổ từ này đang trống. Hãy thêm từ vựng đầu tiên vào Tổ nhé!"
                        } else {
                            "Không có từ nào ở trạng thái đang lọc."
                        },
                        actionLabel = if (statusFilter == null) "+ Thêm từ đầu tiên" else null,
                        onActionClick = if (statusFilter == null) ({ onAddWord(deckId) }) else null
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(words, key = { it.wordId }) { word ->
                        WordRow(
                            word = word,
                            onEdit = { onEditWord(deckId, word.wordId) },
                            onDelete = { wordToDelete = word },
                            onClick = { onEditWord(deckId, word.wordId) }
                        )
                    }
                }
            }



        }
    }

    wordToDelete?.let { word ->
        ConfirmDialog(
            title = "Xóa từ vựng?",
            message = "Bạn có chắc chắn muốn xóa từ \"${word.term}\" khỏi Tổ này không?",
            confirmText = "Xóa",
            onConfirm = {
                wordViewModel.deleteWord(word.wordId)
                wordToDelete = null
            },
            onDismiss = { wordToDelete = null }
        )
    }
}

@Composable
private fun DeckStatsHeaderCard(deckWithStats: DeckWithStats?) {
    val total = deckWithStats?.wordCount ?: 0
    val learned = deckWithStats?.learnedCount ?: 0
    val progress = deckWithStats?.progressPercent ?: 0f
    val percent = (progress * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(value = "$total", label = "Tổng số từ")
                StatColumn(value = "$learned", label = "Đã thuộc")
                StatColumn(value = "$percent%", label = "Tiến độ")
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ForestGreen,
                trackColor = BorderWarm
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun StudyActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ForestGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
