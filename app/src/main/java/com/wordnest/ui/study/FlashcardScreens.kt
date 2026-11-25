
package com.wordnest.ui.study

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.remote.GeminiRepository
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnGhostButton
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.ui.components.WnResultBanner
import com.wordnest.ui.components.WnStatCard
import com.wordnest.ui.components.WnSuccessButton
import com.wordnest.ui.components.WnTextLink
import com.wordnest.viewmodel.StudyViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Flashcard (F08, F09).
 * Hiển thị từ vựng, lật thẻ và đánh giá ghi nhớ.
 */
@Composable
fun FlashcardScreen(
    viewModel: StudyViewModel,
    onExit: () -> Unit
) {
    val index by viewModel.cardIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isFlipped.collectAsStateWithLifecycle()

    val knownCount = viewModel.knownCount
    val sessionSize = viewModel.sessionSize

    val word = viewModel.currentCard ?: return

    var showAiDialog by remember(index, word.term, word.meaning) {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WnHeader(
            title = "Flashcard",
            onBack = onExit,
            trailing = {
                WnTextLink(
                    "Thống kê",
                    onClick = viewModel::showStatistics
                )
            },
        )

        // Tiến độ phiên học
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tiến độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = "Thẻ ${index + 1}/$sessionSize",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(6.dp))

            WnProgressBar(
                progress = if (sessionSize > 0) {
                    (index + 1f) / sessionSize
                } else {
                    0f
                }
            )
        }

        // Thẻ từ vựng
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable {
                    viewModel.flipCard()
                },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isFlipped) {

                        Text(
                            text = word.term,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        word.phonetic?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                    } else {

                        Text(
                            text = word.term,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        word.phonetic?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Chạm để xem nghĩa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }


        // Đánh giá thẻ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WnGhostButton(
                text = "Chưa thuộc",
                onClick = {
                    viewModel.markCard(false)
                },
                modifier = Modifier.weight(1f),
            )

            WnSuccessButton(
                text = "Đã thuộc",
                onClick = {
                    viewModel.markCard(true)
                },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Đã thuộc: $knownCount/$sessionSize",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallFloatingActionButton(
                onClick = {
                    showAiDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI hỗ trợ từ đang học"
                )
            }
        }

        if (showAiDialog) {
            key(index, word.term, word.meaning) {
                FlashcardAiDialog(
                    word = word.term,
                    meaning = word.meaning,
                    onDismiss = {
                        showAiDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FlashcardAiDialog(
    word: String,
    meaning: String,
    onDismiss: () -> Unit
) {
    val repository = remember { GeminiRepository() }
    val scope = rememberCoroutineScope()

    var answer by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun askAi(request: String) {
        if (isLoading || request.isBlank()) return

        isLoading = true
        answer = ""

        scope.launch {
            try {
                val prompt = """
                    Bạn là trợ lý học từ vựng WordNest.

                    Từ đang học: $word
                    Nghĩa tiếng Việt: $meaning

                    Yêu cầu của người học: $request

                    Chỉ tập trung vào từ đang học.
                    Trả lời ngắn gọn, dễ hiểu.
                    Tối đa 100 từ.
                    Không cần chào hỏi.
                    Không sử dụng Markdown.
                    Câu ví dụ tiếng Anh phải có
                    bản dịch tiếng Việt.
                """.trimIndent()

                answer = repository.sendMessage(prompt)
                    .replace("**", "")
                    .replace("###", "")
                    .replace("```", "")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                answer = "Không thể kết nối AI. Hãy thử lại."
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(8.dp))

                Text("WordNest AI")
            }
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bạn đang học từ",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Bạn cần mình hỗ trợ gì?",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = {
                        askAi("Tạo 2 câu ví dụ đơn giản.")
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tạo câu ví dụ")
                }

                OutlinedButton(
                    onClick = {
                        askAi("Giải thích cách sử dụng từ này.")
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Giải thích cách dùng")
                }

                OutlinedButton(
                    onClick = {
                        askAi("Cho 3 từ vựng liên quan.")
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Từ vựng liên quan")
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("AI đang trả lời...")
                    }
                }

                if (answer.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = answer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = {
                        Text("Hỏi thêm về từ $word...")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        val request = question.trim()
                        if (request.isNotBlank()) {
                            question = ""
                            askAi(request)
                        }
                    },
                    enabled = question.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gửi câu hỏi")
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tiếp tục học")
            }
        }
    )
}

@Composable
fun FlashcardResultScreen(
    viewModel: StudyViewModel,
    onHome: () -> Unit = {}
) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        result?.let { r ->

            WnHeader(
                title = "Kết quả buổi học"
            )

            WnResultBanner(
                title = "Hoàn thành buổi học!",
                subtitle = "Bạn đã học ${r.totalCount} từ",
            )

            WnProgressBar(
                progress = if (r.totalCount > 0) {
                    r.correctCount.toFloat() / r.totalCount
                } else {
                    0f
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {
                WnStatCard(
                    value = r.correctCount.toString(),
                    label = "Đã thuộc",
                    icon = "✓",
                    modifier = Modifier.weight(1f),
                )

                WnStatCard(
                    value = (
                            r.totalCount - r.correctCount
                            ).toString(),
                    label = "Chưa thuộc",
                    icon = "↺",
                    modifier = Modifier.weight(1f),
                )
            }

            // Học lại các từ chưa thuộc
            if (viewModel.hasUnknownWordsToReview) {
                WnPrimaryButton(
                    text = "Học lại ${r.wrongWords.size} từ chưa thuộc",
                    onClick = viewModel::reviewUnknown,
                )
            }

            if (viewModel.canStartQuiz) {

                WnGhostButton(
                    text = "Làm trắc nghiệm",
                    onClick = viewModel::startQuiz
                )

                WnGhostButton(
                    text = "Điền từ",
                    onClick = viewModel::startTypeIn
                )

            } else {

                Text(
                    text = "Tổ cần ít nhất 4 từ để làm trắc nghiệm và điền từ. Hãy thêm từ vào Tổ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            WnGhostButton(
                text = "Xem thống kê",
                onClick = viewModel::showStatistics
            )

            WnTextLink(
                text = "Về trang chủ",
                onClick = onHome,
                modifier = Modifier.align(
                    Alignment.CenterHorizontally
                ),
            )
        }
    }
}
