
package com.example.flashcardscreen


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.LongSummaryStatistics

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var currentScreen by remember{
                    mutableStateOf("flashcard")
                }
                var rememberedCount by remember {
                    mutableIntStateOf(0)
                }
                var quizScore by remember {
                    mutableIntStateOf(0)
                }
                val preferences = remember {
                    LearningPreferences(applicationContext)
                }

                val savedRememberedCount by
                preferences.rememberedCount.collectAsState(initial = 0)

                val savedQuizScore by
                preferences.quizScore.collectAsState(initial = 0)

                val scope = rememberCoroutineScope()

                var previousScreen by remember {
                    mutableStateOf("flashcard")
                }
                when(currentScreen) {
                    "flashcard" -> FlashcardScreen(
                        onFinish = { count ->
                            rememberedCount = count

                            scope.launch {
                                preferences.saveRememberedCount(count)
                            }

                            currentScreen = "result"
                        },
                        onStatistics = {
                            previousScreen = "flashcard"
                            currentScreen = "statistics"
                        }
                    )
                    "result" -> FlashcardResultScreen(
                        totalWords = 4,
                        rememberedCount = rememberedCount,
                        onReview = {
                            currentScreen = "flashcard"
                        },
                        onStartQuiz = {
                            currentScreen = "quiz"
                        }
                    )

                    "quiz" -> QuizScreen(
                        onFinish = { score ->
                            quizScore = score

                            scope.launch {
                                preferences.saveQuizScore(score)
                            }

                            currentScreen = "quizResult"
                        }
                    )

                    "quizResult" -> QuizResultScreen(
                        score = quizScore,
                        totalQuestions = 4,
                        onRetry = {
                            currentScreen = "quiz"
                        },
                        onStatistics = {
                            previousScreen = "quizResult"
                            currentScreen = "statistics"
                        }
                    )


                    "statistics" -> StatisticsScreen(
                        totalWords = 4,
                        rememberedCount = savedRememberedCount,
                        quizScore = savedQuizScore,
                        totalQuestions = 4,
                        onBack = {
                            currentScreen = previousScreen
                        }
                    )
                }
            }
        }
    }
}

// Dữ liệu của một từ vựng
data class Vocabulary(
    val word: String,
    val meaning: String
)


@Composable
fun FlashcardScreen(
    onFinish: (Int) -> Unit,
    onStatistics: () -> Unit
) {
    val words = remember {
        listOf(
            Vocabulary("airport", "Sân bay"),
            Vocabulary("apple", "Quả táo"),
            Vocabulary("book", "Cuốn sách"),
            Vocabulary("computer", "Máy tính")
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val rememberedWords = remember {
        mutableStateMapOf<Int, Boolean>()
    }

    val currentWord = words[currentIndex]
    fun nextCard() {
        if (currentIndex < words.lastIndex) {
            currentIndex++
            isFlipped = false
        } else {
            onFinish(
                rememberedWords.values.count { it }
            )
        }
    }

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
            text = "Học Flashcard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Thẻ ${currentIndex + 1}/${words.size}",
            fontSize = 16.sp
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .clickable { isFlipped = !isFlipped },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFlipped) currentWord.meaning else currentWord.word,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    rememberedWords[currentIndex] =
                        LearningLogic.updateWordStatus(
                            rememberedWords[currentIndex] ?: false,
                            false
                        )

                    nextCard()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE57373)
                )
            ) {
                Text("Chưa nhớ")
            }

            Button(
                onClick = {
                    rememberedWords[currentIndex] =
                        LearningLogic.updateWordStatus(
                            rememberedWords[currentIndex] ?: false,
                            true
                        )

                    nextCard()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Đã nhớ")
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Đã nhớ: ${rememberedWords.values.count { it }}/${words.size}"
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onStatistics,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4)
            )
        ) {
            Text("Xem thống kê")
        }
    }
}
