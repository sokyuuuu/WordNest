package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.WnCard
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.components.WnProgressBar
import com.wordnest.viewmodel.StudyViewModel

/**
 * Điền từ (F14): hiện nghĩa của từ, người học gõ lại từ tiếng Anh;
 * ô nhập viền xanh khi đúng / đỏ khi sai, chấm ngay sau khi gửi.
 */
@Composable
fun TypeInScreen(viewModel: StudyViewModel) {
    val index by viewModel.typeIndex.collectAsStateWithLifecycle()
    val typedCorrect by viewModel.typedCorrect.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    val word = viewModel.currentTypeWord ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WnHeader(title = "Điền từ", onBack = viewModel::showStatistics)

        // Tiến độ bài làm
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tiến độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Từ ${index + 1}/${viewModel.typeTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            val answered = if (typedCorrect != null) 1f else 0f
            WnProgressBar(
                progress = if (viewModel.typeTotal > 0) (index + answered) / viewModel.typeTotal else 0f,
            )
        }

        Text(
            text = "Điền từ còn thiếu vào chỗ trống.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Thẻ nghĩa của từ phải điền
        WnCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Nghĩa của từ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Text(
            text = "Đáp án của bạn",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            enabled = typedCorrect == null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = when (typedCorrect) {
                    true -> MaterialTheme.colorScheme.secondary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.outline
                },
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        when (typedCorrect) {
            true -> Text(
                text = "🌱 Chính xác! Tiếp tục duy trì nhé.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            false -> Text(
                text = "😢 Chưa chính xác! Đáp án đúng: ${word.term}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            null -> {}
        }

        when (typedCorrect) {
            null -> WnPrimaryButton(
                text = "Kiểm tra",
                onClick = { viewModel.submitTyped(input) },
                enabled = input.isNotBlank(),
            )
            else -> WnPrimaryButton(
                text = if (index < viewModel.typeTotal - 1) "Câu tiếp theo" else "Xem kết quả",
                onClick = {
                    input = ""
                    viewModel.nextTypeWord()
                },
            )
        }
    }
}
