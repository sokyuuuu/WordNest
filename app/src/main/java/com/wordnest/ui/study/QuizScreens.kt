package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ButtonDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.viewmodel.StudyViewModel

/** Trắc nghiệm (F12): 4 đáp án, báo đúng/sai ngay sau khi chọn. */
@Composable
fun QuizScreen(viewModel: StudyViewModel) {
    val quizIndex by viewModel.quizIndex.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    val question = viewModel.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Câu ${quizIndex + 1}/${viewModel.quizTotal}",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = viewModel::showStatistics) { Text("Thống kê") }
        }

        Spacer(Modifier.height(24.dp))

        Text("Từ nào mang nghĩa \"${question.word.meaning}\"?", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(16.dp))

        question.options.forEachIndexed { optionIndex, option ->
            OutlinedButton(
                onClick = { viewModel.answerQuestion(optionIndex) },
                enabled = selected == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(option)
            }
        }

        Spacer(Modifier.height(16.dp))

        val answered = selected != null
        if (answered) {
            val isCorrect = selected == question.correctIndex
            Text(
                text = if (isCorrect) "Đúng!" else "Sai. Đáp án đúng: ${question.word.term} — ${question.word.meaning}",
                color = if (isCorrect) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::nextQuizQuestion) {
                Text(if (quizIndex < viewModel.quizTotal - 1) "Câu tiếp theo" else "Xem kết quả")
            }
        }
    }
}

/** Kết quả kiểm tra (F13): điểm và danh sách từ làm sai. */
@Composable
fun QuizResultScreen(viewModel: StudyViewModel) {
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
            Text(text = "Đúng: ${r.correctCount}/${r.totalCount} câu", fontSize = 22.sp)

            Spacer(Modifier.height(24.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Các từ làm sai:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    if (r.wrongWords.isEmpty()) {
                        Text("Bạn đã làm đúng hết!", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        r.wrongWords.forEach { word ->
                            Text("• ${word.term} — ${word.meaning}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::startQuiz) { Text("Làm lại") }
                OutlinedButton(onClick = viewModel::startTypeIn) { Text("Điền từ") }
                OutlinedButton(onClick = viewModel::showStatistics) { Text("Thống kê") }
            }
        }
    }
}
