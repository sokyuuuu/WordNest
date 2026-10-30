
package com.example.flashcardscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizResultScreen(
    score: Int,
    totalQuestions: Int,
    onRetry: () -> Unit,
    onStatistics: () -> Unit
) {
    val percentage = if (totalQuestions > 0) {
        score * 100 / totalQuestions
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8EE))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Kết quả kiểm tra",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        Text("🎉", fontSize = 50.sp)

        Spacer(Modifier.height(16.dp))

        Text(
            "$score/$totalQuestions",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7051A6)
        )

        Text("Số câu trả lời đúng")

        Spacer(Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Độ chính xác: $percentage%")

        Spacer(Modifier.height(30.dp))

        Button(onClick = onRetry) {
            Text("Làm lại bài kiểm tra")
        }

        Button(onClick = onStatistics) {
            Text("Xem thống kê")
        }
    }
}

