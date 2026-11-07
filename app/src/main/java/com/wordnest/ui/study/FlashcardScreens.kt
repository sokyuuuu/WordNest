package com.wordnest.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnGhostButton
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.ui.components.WnResultBanner
import com.wordnest.ui.components.WnStatCard
import com.wordnest.ui.components.WnSuccessButton
import com.wordnest.ui.components.WnTextLink
import com.wordnest.viewmodel.StudyViewModel

/**
 * Thẻ flashcard (F08): mặt trước từ + phiên âm, chạm để lật mặt nghĩa (F09),
 * hai nút "Đã thuộc" xanh / "Chưa thuộc" viền.
 */
@Composable
fun FlashcardScreen(viewModel: StudyViewModel, onExit: () -> Unit) {
    val index by viewModel.cardIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isFlipped.collectAsStateWithLifecycle()
    val knownCount = viewModel.knownCount
    val sessionSize = viewModel.sessionSize

    val word = viewModel.currentCard ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WnHeader(
            title = "Flashcard",
            onBack = onExit,
            trailing = { WnTextLink("Thống kê", onClick = viewModel::showStatistics) },
        )

        // Tiến độ phiên học
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tiến độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Thẻ ${index + 1}/$sessionSize",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            WnProgressBar(progress = if (sessionSize > 0) (index + 1f) / sessionSize else 0f)
        }

        // Thẻ từ: chạm để lật
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable { viewModel.flipCard() },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isFlipped) {
                        Text(
                            text = word.term,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        word.phonetic?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    } else {
                        Text(
                            text = word.term,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        word.phonetic?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Chạm để xem nghĩa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Đánh giá thẻ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WnGhostButton(
                text = "Chưa thuộc",
                onClick = { viewModel.markCard(false) },
                modifier = Modifier.weight(1f),
            )
            WnSuccessButton(
                text = "Đã thuộc",
                onClick = { viewModel.markCard(true) },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "Đã thuộc: $knownCount/$sessionSize",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/**
 * Kết quả buổi học (F10): banner hoàn thành, tiến độ, hai ô thống kê
 * "Đã thuộc / Chưa thuộc", nút học lại từ chưa thuộc (US10) và đi tiếp
 * trắc nghiệm/điền từ.
 */
@Composable
fun FlashcardResultScreen(viewModel: StudyViewModel, onHome: () -> Unit = {}) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        result?.let { r ->
            WnHeader(title = "Kết quả buổi học")

            WnResultBanner(
                title = "Hoàn thành buổi học!",
                subtitle = "Bạn đã học ${r.totalCount} từ",
            )

            WnProgressBar(
                progress = if (r.totalCount > 0) r.correctCount.toFloat() / r.totalCount else 0f,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WnStatCard(
                    value = r.correctCount.toString(),
                    label = "Đã thuộc",
                    icon = "✓",
                    modifier = Modifier.weight(1f),
                )
                WnStatCard(
                    value = (r.totalCount - r.correctCount).toString(),
                    label = "Chưa thuộc",
                    icon = "↺",
                    modifier = Modifier.weight(1f),
                )
            }

            // US10: nút học lại chỉ dạy các từ chưa thuộc, ẩn nếu không còn từ nào.
            if (viewModel.hasUnknownWordsToReview) {
                WnPrimaryButton(
                    text = "Học lại ${r.wrongWords.size} từ chưa thuộc",
                    onClick = viewModel::reviewUnknown,
                )
            }

            if (viewModel.canStartQuiz) {
                WnGhostButton(text = "Làm trắc nghiệm", onClick = viewModel::startQuiz)
                WnGhostButton(text = "Điền từ", onClick = viewModel::startTypeIn)
            } else {
                Text(
                    text = "Tổ cần ít nhất 4 từ để làm trắc nghiệm và điền từ. Hãy thêm từ vào Tổ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            WnGhostButton(text = "Xem thống kê", onClick = viewModel::showStatistics)
            WnTextLink(
                text = "Về trang chủ",
                onClick = onHome,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
