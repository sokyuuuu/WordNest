
package com.wordnest.ui.study

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.remote.GeminiRepository
import com.wordnest.data.remote.ClozeExample
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

@Composable
fun TypeInScreen(viewModel: StudyViewModel) {

    val index by viewModel.typeIndex.collectAsStateWithLifecycle()
    val typedCorrect by viewModel.typedCorrect.collectAsStateWithLifecycle()

    val word = viewModel.currentTypeWord ?: return

    var input by remember(index, word.id) {
        mutableStateOf("")
    }

    val repository = remember { GeminiRepository() }
    val scope = rememberCoroutineScope()

    val exampleState = remember(index, word.id) {
        mutableStateOf<ClozeExample?>(null)
    }

    val loadingState = remember(index, word.id) {
        mutableStateOf(false)
    }

    val errorState = remember(index, word.id) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Tiêu đề
        WnHeader(
            title = "Điền từ",
            onBack = viewModel::showStatistics
        )

        // Tiến độ bài học
        Column {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tiến độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Từ ${index + 1}/${viewModel.typeTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            val answered = if (typedCorrect != null) 1f else 0f

            WnProgressBar(
                progress = if (viewModel.typeTotal > 0) {
                    (index + answered) / viewModel.typeTotal
                } else {
                    0f
                }
            )
        }

        Text(
            text = "Điền từ còn thiếu vào chỗ trống.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Thẻ câu hỏi và ô nhập đáp án
        WnCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Nghĩa của từ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "Nghĩa của từ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Ô nhập đáp án
                Text(
                    text = "Đáp án của bạn",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text("Nhập từ tiếng Anh...")
                    },
                    singleLine = true,
                    enabled = typedCorrect == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            MaterialTheme.colorScheme.secondary,

                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.outline,

                        disabledBorderColor = when (typedCorrect) {
                            true -> MaterialTheme.colorScheme.secondary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.outline
                        },

                        disabledTextColor =
                            MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // Thông báo kết quả
        when (typedCorrect) {

            true -> Text(
                text = "🌱 Chính xác! Tiếp tục duy trì nhé.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            false -> Text(
                text = "😢 Chưa chính xác! Đáp án đúng: ${word.term}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            null -> {}
        }

        // Nút kiểm tra hoặc chuyển câu
        when (typedCorrect) {

            null -> WnPrimaryButton(
                text = "Kiểm tra",
                onClick = {
                    viewModel.submitTyped(input)
                },
                enabled = input.isNotBlank()
            )

            else -> WnPrimaryButton(
                text = if (index < viewModel.typeTotal - 1) {
                    "Câu tiếp theo"
                } else {
                    "Xem kết quả"
                },
                onClick = {
                    input = ""
                    viewModel.nextTypeWord()
                }
            )
        }

        // Nút AI nằm dưới nút kiểm tra
        if (typedCorrect == null && exampleState.value == null) {

            OutlinedButton(
                onClick = {

                    loadingState.value = true
                    errorState.value = false

                    scope.launch {
                        try {

                            val result =
                                repository.generateClozeExample(
                                    word = word.term,
                                    meaning = word.meaning
                                )

                            exampleState.value = result
                            errorState.value = result == null

                        } finally {
                            loadingState.value = false
                        }
                    }
                },
                enabled = !loadingState.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {

                if (loadingState.value) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Đang tạo câu...")

                } else {

                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = if (errorState.value) {
                            "Thử lại"
                        } else {
                            "Tạo câu ví dụ"
                        }
                    )
                }
            }
        }

        // Hiển thị câu ví dụ do AI tạo
        exampleState.value?.let { example ->

            WnCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "WordNest AI",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = example.example,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Chỉ hiện bản dịch sau khi trả lời
                    if (typedCorrect != null) {

                        Text(
                            text = example.translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Thông báo khi AI gặp lỗi
        if (errorState.value && typedCorrect == null) {

            Text(
                text = "Chưa tạo được câu ví dụ. Bạn vẫn có thể tiếp tục làm bài.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
