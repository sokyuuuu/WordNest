package com.wordnest.ui.study

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.viewmodel.StudyViewModel

/** Thẻ flashcard: chạm để lật mặt nghĩa (F08), hai nút đánh dấu (F09). */
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onExit) { Text("Thoát") }
            Text(
                text = "Thẻ ${index + 1}/$sessionSize",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = viewModel::showStatistics) { Text("Thống kê") }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clickable { viewModel.flipCard() },
            shape = RoundedCornerShape(20.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isFlipped) {
                        Text(word.term, style = MaterialTheme.typography.titleSmall)
                        if (word.phonetic != null) {
                            Text(word.phonetic, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = word.meaning,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = word.term,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Chạm để lật thẻ",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.markCard(false) }) { Text("Chưa thuộc") }
                Button(onClick = { viewModel.markCard(true) }) { Text("Đã thuộc") }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Đã thuộc: $knownCount/$sessionSize",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Kết quả buổi học (F10): số từ đã thuộc, học lại từ chưa thuộc, đi tới trắc nghiệm. */
@Composable
fun FlashcardResultScreen(viewModel: StudyViewModel) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        result?.let { r ->
            Text(text = r.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text(text = "Đã thuộc: ${r.correctCount}/${r.totalCount}", fontSize = 22.sp)
            Spacer(Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // US10: nút học lại chỉ học từ chưa thuộc, ẩn nếu không còn từ nào.
                if (viewModel.hasUnknownWordsToReview) {
                    Button(onClick = viewModel::reviewUnknown) { Text("Học lại từ chưa thuộc") }
                    Spacer(Modifier.height(8.dp))
                }
                if (viewModel.canStartQuiz) {
                    Button(onClick = viewModel::startQuiz) { Text("Làm trắc nghiệm") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::startTypeIn) { Text("Điền từ") }
                } else {
                    Text(
                        "Tổ cần ít nhất 4 từ để làm trắc nghiệm và điền từ. Hãy thêm từ vào Tổ.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::showStatistics) { Text("Xem thống kê") }
            }
        }
    }
}
