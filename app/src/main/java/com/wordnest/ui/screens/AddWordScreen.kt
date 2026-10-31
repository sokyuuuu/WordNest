package com.wordnest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.R
import com.wordnest.ui.components.PrimaryButton
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.DangerRed
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.ForestGreenLight
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.AddWordViewModel
import com.wordnest.ui.viewmodel.SuggestStatus

/**
 * Màn hình "Thêm từ" kiểu Quizlet: autocomplete từ, tự điền phiên âm,
 * chip nghĩa gợi ý và ví dụ gợi ý. Mọi ô vẫn nhập/sửa tay bình thường.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddWordScreen(
    viewModel: AddWordViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Lưu xong -> quay lại màn trước.
    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_word_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_word_back),
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!state.autoSuggestEnabled) {
                StatusLine(
                    icon = { Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    text = stringResource(R.string.add_word_suggest_off),
                    color = TextSecondary
                )
            }

            // 1. Từ tiếng Anh + dropdown autocomplete
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.add_word_term_label))
                AppTextField(
                    value = state.term,
                    onValueChange = viewModel::onTermChange,
                    placeholder = stringResource(R.string.add_word_term_placeholder),
                    singleLine = true,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::onTermDone
                )
                if (state.suggestions.isNotEmpty()) {
                    SuggestionDropdown(
                        items = state.suggestions,
                        onSelect = viewModel::onSuggestionSelected
                    )
                }
            }

            // Trạng thái gợi ý: loading / không có / lỗi (không chặn nhập tay)
            if (state.autoSuggestEnabled) {
                SuggestStatusLine(state.uiState)
            }

            // 2. Phiên âm
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.add_word_phonetic_label))
                AppTextField(
                    value = state.phonetic,
                    onValueChange = viewModel::onPhoneticChange,
                    placeholder = stringResource(R.string.add_word_phonetic_placeholder),
                    singleLine = true
                )
            }

            // 3. Nghĩa + chip nghĩa gợi ý
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.add_word_meaning_label))
                AppTextField(
                    value = state.meaning,
                    onValueChange = viewModel::onMeaningChange,
                    placeholder = stringResource(R.string.add_word_meaning_placeholder)
                )
                if (state.meaningOptions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.add_word_meaning_suggestions),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.meaningOptions.forEach { option ->
                            val selected = option in state.selectedMeanings
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onMeaningToggled(option) },
                                label = { Text(option) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForestGreenLight,
                                    selectedLabelColor = ForestGreen,
                                    selectedLeadingIconColor = ForestGreen
                                )
                            )
                        }
                    }
                }
            }

            // 4. Ví dụ + ví dụ gợi ý
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.add_word_example_label))
                AppTextField(
                    value = state.example,
                    onValueChange = viewModel::onExampleChange,
                    placeholder = stringResource(R.string.add_word_example_placeholder),
                    minLines = 2
                )
                if (state.exampleOptions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.add_word_example_suggestions),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    state.exampleOptions.forEach { ex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onExampleSelected(ex) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ex == state.example) ForestGreenLight else CardWhite
                            )
                        ) {
                            Text(
                                text = "“$ex”",
                                fontStyle = FontStyle.Italic,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            PrimaryButton(
                text = stringResource(if (state.isSaving) R.string.add_word_saving else R.string.add_word_save),
                enabled = state.canSave,
                onClick = { viewModel.onSave() }
            )
        }
    }
}

@Composable
private fun SuggestionDropdown(items: List<String>, onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            items.forEachIndexed { index, word ->
                Text(
                    text = word,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(word) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (index < items.lastIndex) HorizontalDivider(color = BorderWarm)
            }
        }
    }
}

@Composable
private fun SuggestStatusLine(status: SuggestStatus) {
    when (status) {
        SuggestStatus.Idle -> Unit
        SuggestStatus.Loading -> StatusLine(
            icon = { CircularProgressIndicator(strokeWidth = 2.dp, color = ForestGreen, modifier = Modifier.size(16.dp)) },
            text = stringResource(R.string.add_word_loading),
            color = TextSecondary
        )
        SuggestStatus.Empty -> StatusLine(
            icon = { Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            text = stringResource(R.string.add_word_empty),
            color = TextSecondary
        )
        is SuggestStatus.Error -> StatusLine(
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(18.dp)) },
            text = stringResource(R.string.add_word_error, status.message),
            color = DangerRed
        )
    }
}

@Composable
private fun StatusLine(icon: @Composable () -> Unit, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
            focusedBorderColor = ForestGreen,
            unfocusedBorderColor = BorderWarm
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
