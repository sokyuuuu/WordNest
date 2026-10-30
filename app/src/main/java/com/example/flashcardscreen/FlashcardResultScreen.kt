package com.example.flashcardscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable

fun FlashcardResultScreen(
    totalWords: Int,
    rememberedCount: Int,
    onReview: () -> Unit,
    onStartQuiz: () -> Unit
){
    val notRemembered = totalWords - rememberedCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            //.statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Hoàn thành bài học!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        Text("Tổng số từ: $totalWords")
        Text("Đã nhớ: $rememberedCount")
        Text("Chưa nhớ: $notRemembered")

        Spacer(Modifier.height(24.dp))

        Button(onClick = onReview) {
            Text("Học lại")
        }
        Button(onClick = onStartQuiz) {
            Text("Bắt đầu kiểm tra")
        }
    }
}