package com.wordnest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnStatCard
import com.wordnest.ui.components.WnTextLink
import com.wordnest.viewmodel.HomeViewModel
import com.wordnest.util.WordStatus

private data class DeckSample(val title: String, val count: Int)

private val deckSamples = listOf(
    DeckSample("Du lịch", 24),
    DeckSample("IELTS", 40),
)

/**
 * Trang chủ (Figma: "Trang chủ") — lời chào, huy hiệu số ngày ấp, thẻ
 * "Hôm nay cần ôn", các Tủ mẫu và thống kê "Đàn chim".
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), onStudyClick: () -> Unit = {}) {
    val eggDay by viewModel.eggDay.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()
    val flockCounts by viewModel.flockCounts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Lời chào
        Column {
            Text(
                text = "Chào Khoa 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Hôm nay học gì nào?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Huy hiệu "N ngày ấp" trong pill xanh nhạt
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🥚")
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$eggDay ngày ấp",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        // Thẻ "Hôm nay cần ôn"
        WnCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hôm nay cần ôn",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "$dueCount từ",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                WnPrimaryButton(text = "Bắt đầu học", onClick = onStudyClick)
            }
        }

        // Các Tủ của bạn (dữ liệu mẫu chờ F01–F07)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Các Tủ của bạn",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            WnTextLink(text = "Xem tất cả", onClick = {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            deckSamples.forEach { deck ->
                WnCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = deck.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "${deck.count} từ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Đàn chim
        Text(
            text = "Đàn chim",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WnStatCard(
                value = (flockCounts[WordStatus.EGG] ?: 0).toString(),
                label = WordStatus.EGG.label,
                icon = "🥚",
                modifier = Modifier.weight(1f),
            )
            WnStatCard(
                value = (flockCounts[WordStatus.CHICK] ?: 0).toString(),
                label = WordStatus.CHICK.label,
                icon = "🐣",
                modifier = Modifier.weight(1f),
            )
            WnStatCard(
                value = (flockCounts[WordStatus.BIRD] ?: 0).toString(),
                label = WordStatus.BIRD.label,
                icon = "🕊️",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
