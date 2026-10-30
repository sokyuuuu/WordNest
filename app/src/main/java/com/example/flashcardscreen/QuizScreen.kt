package com.example.flashcardscreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)

@Composable
fun QuizScreen(
    onFinish: (Int) -> Unit
) {
    val questions = remember {
        listOf(
            QuizQuestion(
                "Airport có nghĩa là gì?",
                listOf("Sân bay", "Bệnh viện", "Trường học", "Nhà ga"),
                0
            ),
            QuizQuestion(
                "Apple có nghĩa là gì?",
                listOf("Quả cam", "Quả táo", "Quả chuối", "Quả lê"),
                1
            ),
            QuizQuestion(
                "Book có nghĩa là gì?",
                listOf("Cây bút", "Cái bàn", "Cuốn sách", "Cái ghế"),
                2
            ),
            QuizQuestion(
                "Computer có nghĩa là gì?",
                listOf("Điện thoại", "Máy tính", "Tivi", "Đồng hồ"),
                1
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }

    val currentQuestion = questions[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kiểm tra từ vựng", fontSize = 24.sp)

        Spacer(Modifier.height(20.dp))

        Text("Câu ${currentIndex + 1}/${questions.size}")

        Spacer(Modifier.height(20.dp))

        Text(currentQuestion.question, fontSize = 20.sp)

        Spacer(Modifier.height(20.dp))



        currentQuestion.options.forEachIndexed { index, option ->
            val isSelected = selectedAnswer == index

            OutlinedButton(
                onClick = {
                    selectedAnswer = index
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected)
                        Color(0xFFE8DFFF)
                    else
                        Color.White
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected)
                        Color(0xFF7051A6)
                    else
                        Color.LightGray
                )
            ) {
                Text(
                    text = "${index + 1}. $option",
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(10.dp))
        }



        Button(
            onClick = {
                if (
                    LearningLogic.isCorrectAnswer(
                        selectedAnswer,
                        currentQuestion.correctAnswer
                    )
                ) {
                    score++
                }
                if (currentIndex < questions.lastIndex) {
                    currentIndex++
                    selectedAnswer = -1
                } else {
                    onFinish(score)
                }
            },
            enabled = selectedAnswer != -1
        ) {
            Text(
                if (currentIndex == questions.lastIndex)
                    "Nộp bài"
                else
                    "Câu tiếp theo →"
            )
        }
    }
}
