package com.wordnest.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.viewmodel.StudyViewModel

/** Điền từ (F14): hiện nghĩa, người dùng gõ lại từ. */
@Composable
fun TypeInScreen(viewModel: StudyViewModel) {
    val index by viewModel.typeIndex.collectAsStateWithLifecycle()
    val typedCorrect by viewModel.typedCorrect.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    val word = viewModel.currentTypeWord ?: return

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
                text = "Từ ${index + 1}/${viewModel.typeTotal}",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = viewModel::showStatistics) { Text("Thống kê") }
        }

        Spacer(Modifier.height(24.dp))

        Text("Gõ lại từ có nghĩa là:", style = MaterialTheme.typography.bodyLarge)
        Text(text = word.meaning, fontSize = 30.sp, modifier = Modifier.padding(vertical = 16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Nhập từ") },
            singleLine = true,
            enabled = typedCorrect == null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        when (typedCorrect) {
            true -> Text(
                "Đúng!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
            false -> Text(
                "Sai. Từ đúng là: ${word.term}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
            null -> {}
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (typedCorrect) {
                null -> Button(
                    onClick = {
                        viewModel.submitTyped(input)
                    },
                    enabled = input.isNotBlank(),
                ) { Text("Gửi") }
                else -> Button(onClick = {
                    input = ""
                    viewModel.nextTypeWord()
                }) {
                    Text(if (index < viewModel.typeTotal - 1) "Từ tiếp theo" else "Xem kết quả")
                }
            }
        }
    }
}

