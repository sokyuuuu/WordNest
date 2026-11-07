package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnOptionButton
import com.wordnest.ui.components.WnOptionState
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.ui.components.WnResultBanner
import com.wordnest.ui.components.WnStatCard
import com.wordnest.ui.components.WnTextLink
import com.wordnest.viewmodel.StudyViewModel

/**
 * Trắc nghiệm (F12): mục tiêu "Chọn nghĩa đúng của từ sau.", thẻ từ kèm
 * phiên âm, 4 ô đáp án 2×2; báo đúng (✓ xanh) / sai (⊗ đỏ) ngay khi chọn.
 */
@Composable
fun QuizScreen(viewModel: StudyViewModel) {
    val quizIndex by viewModel.quizIndex.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    val question = viewModel.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WnHeader(title = "Trắc nghiệm", onBack = viewModel::showStatistics)

        // Tiến độ bài làm
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tiến độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Câu ${quizIndex + 1}/${viewModel.quizTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            val answered = if (selected != null) 1f else 0f
            WnProgressBar(
                progress = if (viewModel.quizTotal > 0) (quizIndex + answered) / viewModel.quizTotal else 0f,
            )
        }

        Text(
            text = "Chọn nghĩa đúng của từ sau.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Thẻ từ cần chọn nghĩa
        WnCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = question.word.term,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    question.word.phonetic?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Phát âm (đang phát triển)",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Ô đáp án 2×2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowOptions.forEach { optionLabel ->
                        // TC12-04: các đáp án không trùng nhau nên indexOf là duy nhất.
                        val optionIndex = question.options.indexOf(optionLabel)
                        val state = when {
                            selected == null -> WnOptionState.Normal
                            optionIndex == question.correctIndex -> WnOptionState.Correct
                            optionIndex == selected -> WnOptionState.Wrong
                            else -> WnOptionState.Normal
                        }
                        WnOptionButton(
                            text = optionLabel,
                            state = state,
                            onClick = { viewModel.answerQuestion(optionIndex) },
                            enabled = selected == null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        val answered = selected != null
        if (answered) {
            val isCorrect = selected == question.correctIndex
            Text(
                text = if (isCorrect) "🎉 Chính xác!" else "😢 Chưa chính xác!",
                style = MaterialTheme.typography.titleMedium,
                color = if (isCorrect) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (!isCorrect) {
                Text(
                    text = "Đáp án đúng: ${question.word.meaning}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WnPrimaryButton(
                text = if (quizIndex < viewModel.quizTotal - 1) "Câu tiếp theo" else "Xem kết quả",
                onClick = viewModel::nextQuizQuestion,
            )
        }
    }
}

/**
 * Kết quả bài kiểm tra / bài điền từ (F13): điểm số, hai ô "Câu đúng /
 * Câu sai", danh sách "Từ cần ôn lại" và nút làm lại.
 */
@Composable
fun QuizResultScreen(viewModel: StudyViewModel, onHome: () -> Unit = {}) {
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
            val isTypeInResult = r.title.contains("điền", ignoreCase = true)

            WnHeader(title = r.title)

            WnResultBanner(
                title = if (isTypeInResult) "Hoàn thành bài điền từ!" else "Hoàn thành bài kiểm tra!",
            )

            Text(
                text = "${r.correctCount} / ${r.totalCount}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Điểm số",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    label = "Câu đúng",
                    icon = "✓",
                    modifier = Modifier.weight(1f),
                )
                WnStatCard(
                    value = (r.totalCount - r.correctCount).toString(),
                    label = "Câu sai",
                    icon = "↺",
                    modifier = Modifier.weight(1f),
                )
            }

            if (r.wrongWords.isEmpty()) {
                Text(
                    text = "Bạn đã làm đúng hết, tiếp tục duy trì nhé! 🌱",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Từ cần ôn lại",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Start),
                    )
                    r.wrongWords.forEach { word ->
                        WnCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = word.term,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = word.meaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            WnPrimaryButton(
                text = "Làm lại bài",
                onClick = { if (isTypeInResult) viewModel.startTypeIn() else viewModel.startQuiz() },
            )
            WnTextLink(
                text = "Về trang chủ",
                onClick = onHome,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
