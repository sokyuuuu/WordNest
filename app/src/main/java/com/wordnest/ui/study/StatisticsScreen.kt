package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.util.WordStatus
import com.wordnest.viewmodel.StudyViewModel

/** Thống kê "Đàn chim của bạn" (F16): số trứng, chim non, chim bay. */
@Composable
fun StatisticsScreen(viewModel: StudyViewModel) {
    val words by viewModel.words.collectAsStateWithLifecycle()

    val counts = words.groupingBy { it.status }.eachCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Thống kê học tập", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.padding(top = 8.dp))
        Text("Đàn chim của bạn: ${words.size} từ", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.padding(top = 24.dp))

        WordStatus.entries.forEach { status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = status.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${counts[status] ?: 0} từ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.padding(top = 32.dp))
        Button(onClick = viewModel::backFromStatistics) {
            Text("Quay lại")
        }
    }
}
