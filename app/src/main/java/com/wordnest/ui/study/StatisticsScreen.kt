package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.study.StudyWord
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnStatCard
import com.wordnest.util.WordStatus
import com.wordnest.viewmodel.StudyViewModel

/** Thống kê "Đàn chim của bạn" (F16): số trứng, chim non, chim bay. */
@Composable
fun StatisticsScreen(viewModel: StudyViewModel) {
    val words by viewModel.words.collectAsStateWithLifecycle()

    StatisticsScreenContent(words = words, onBack = viewModel::backFromStatistics)
}

/**
 * Nội dung thống kê dùng chung: màn trong luồng học (có nút quay về màn
 * trước) và tab "Thống kê" ở đáy app (không nút).
 */
@Composable
fun StatisticsScreenContent(
    words: List<StudyWord>,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val counts = words.groupingBy { it.status }.eachCount()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WnHeader(title = "Thống kê", onBack = onBack)

        Column {
            Text(
                text = "Tổng quan học tập",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Đàn chim của bạn: ${words.size} từ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WnStatCard(
                value = words.size.toString(),
                label = "Tổng từ",
                icon = "📚",
                modifier = Modifier.weight(1f),
            )
            WnStatCard(
                value = (counts[WordStatus.EGG] ?: 0).toString(),
                label = WordStatus.EGG.label,
                icon = "🥚",
                modifier = Modifier.weight(1f),
            )
            WnStatCard(
                value = (counts[WordStatus.BIRD] ?: 0).toString(),
                label = WordStatus.BIRD.label,
                icon = "🕊️",
                modifier = Modifier.weight(1f),
            )
        }

        WnCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "🐣 ${WordStatus.CHICK.label} đang luyện cánh",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${counts[WordStatus.CHICK] ?: 0} từ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
