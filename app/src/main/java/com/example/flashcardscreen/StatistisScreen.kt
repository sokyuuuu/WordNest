
package com.example.flashcardscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsScreen(
    totalWords: Int,
    rememberedCount: Int,
    quizScore: Int,
    totalQuestions: Int,
    onBack: () -> Unit
) {
    val notRemembered = totalWords - rememberedCount

    val memoryRate = if (totalWords > 0) {
        rememberedCount * 100 / totalWords
    } else 0

    val quizRate = if (totalQuestions > 0) {
        quizScore * 100 / totalQuestions
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8EE))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Thống kê học tập",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        StatisticCard("Tổng số từ đã học", "$totalWords từ")
        StatisticCard("Đã nhớ", "$rememberedCount từ")
        StatisticCard("Chưa nhớ", "$notRemembered từ")

        Spacer(Modifier.height(20.dp))

        Text("Tỷ lệ ghi nhớ: $memoryRate%")

        LinearProgressIndicator(
            progress = { memoryRate / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        StatisticCard(
            "Kết quả kiểm tra gần nhất",
            "$quizScore/$totalQuestions câu"
        )

        Text("Độ chính xác: $quizRate%")

        LinearProgressIndicator(
            progress = { quizRate / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        Spacer(Modifier.height(30.dp))

        Button(onClick = onBack) {
            Text("Quay lại")
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Text(
                value,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7051A6)
            )
        }
    }
}
