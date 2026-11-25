package com.wordnest.ui.decks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.decks.components.ConfirmDialog
import com.wordnest.util.Constants
import com.wordnest.util.Validation

/**
 * Form Thêm mới & Chỉnh sửa từ vựng dùng chung (F04, F05):
 * - Các trường: Từ* (≤ 100), Nghĩa* (≤ 500), Phiên âm (≤ 100), Câu ví dụ (≤ 500, multiline).
 * - Tích hợp gợi ý từ điển & dịch tự động kiểu Quizlet (autocomplete từ tiếng Anh, tự điền phiên âm IPA,
 *   chip chọn nghĩa tiếng Việt và thẻ câu ví dụ gợi ý), vẫn cho phép nhập/sửa tay toàn bộ trường.
 * - Hiển thị lỗi inline trên từng ô nhập liệu qua [Validation].
 * - Giữ nguyên nội dung đang nhập khi xoay màn hình nhờ [rememberSaveable] và tránh che bàn phím bằng [imePadding].
 */
@Composable
fun WordFormScreen(
    deckId: String,
    wordId: String?,
    viewModel: WordFormViewModel,
    onNavigateBack: () -> Unit,
) {
    val isEditMode = !wordId.isNullOrBlank()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val suggestState by viewModel.suggestState.collectAsStateWithLifecycle()

    val wordLoadStateFlow = remember(wordId, viewModel) {
        if (isEditMode) viewModel.observeWordLoadState(wordId!!) else null
    }
    val wordLoadState: WordLoadState = if (wordLoadStateFlow != null) {
        wordLoadStateFlow.collectAsStateWithLifecycle(initialValue = WordLoadState.Loading).value
    } else {
        WordLoadState.Loading
    }

    var term by rememberSaveable(deckId, wordId) { mutableStateOf("") }
    var meaning by rememberSaveable(deckId, wordId) { mutableStateOf("") }
    var phonetic by rememberSaveable(deckId, wordId) { mutableStateOf("") }
    var example by rememberSaveable(deckId, wordId) { mutableStateOf("") }
    var initialized by rememberSaveable(deckId, wordId) { mutableStateOf(false) }

    var termError by rememberSaveable(deckId, wordId) { mutableStateOf<String?>(null) }
    var meaningError by rememberSaveable(deckId, wordId) { mutableStateOf<String?>(null) }
    var phoneticError by rememberSaveable(deckId, wordId) { mutableStateOf<String?>(null) }
    var exampleError by rememberSaveable(deckId, wordId) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by rememberSaveable(deckId, wordId) { mutableStateOf(false) }
    var didPopOnMissing by remember(deckId, wordId) { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(wordLoadState) {
        if (!isEditMode) return@LaunchedEffect
        when (val state = wordLoadState) {
            is WordLoadState.Found -> {
                if (!initialized) {
                    term = state.word.term
                    meaning = state.word.meaning
                    phonetic = state.word.phonetic.orEmpty()
                    example = state.word.example.orEmpty()
                    viewModel.lockInitialTerm(
                        initialTerm = state.word.term,
                        hasExistingPhonetic = !state.word.phonetic.isNullOrBlank(),
                    )
                    initialized = true
                }
            }

            WordLoadState.Missing -> {
                if (!didPopOnMissing) {
                    didPopOnMissing = true
                    onNavigateBack()
                }
            }

            WordLoadState.Loading -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WordFormEvent.ApplySuggestions -> {
                    term = event.term
                    termError = null
                    event.phoneticToFill?.let { filledPhonetic ->
                        phonetic = filledPhonetic.take(Constants.MAX_WORD_PHONETIC_LENGTH)
                        phoneticError = null
                    }
                    event.meaningToFill?.let { filledMeaning ->
                        meaning = filledMeaning.take(Constants.MAX_WORD_MEANING_LENGTH)
                        meaningError = null
                    }
                }

                is WordFormEvent.ShowMessage -> {
                    if (event.isError) {
                        if (event.message.contains("đã có trong Tổ", ignoreCase = true)) {
                            termError = event.message
                        }
                        snackbarHostState.showSnackbar(event.message)
                    }
                }

                else -> Unit
            }
        }
    }

    fun submit() {
        viewModel.dismissSuggestions()
        val errTerm = Validation.validateWordTerm(term)
        val errMeaning = Validation.validateWordMeaning(meaning)
        val errPhonetic = Validation.validateWordPhonetic(phonetic)
        val errExample = Validation.validateWordExample(example)

        termError = errTerm
        meaningError = errMeaning
        phoneticError = errPhonetic
        exampleError = errExample

        if (errTerm != null || errMeaning != null || errPhonetic != null || errExample != null) {
            return
        }

        val cleanPhonetic = phonetic.trim().ifEmpty { null }
        val cleanExample = example.trim().ifEmpty { null }

        if (isEditMode) {
            val currentWord = (wordLoadState as? WordLoadState.Found)?.word ?: return
            viewModel.updateWord(
                word = currentWord,
                newTerm = term.trim(),
                newMeaning = meaning.trim(),
                newPhonetic = cleanPhonetic,
                newExample = cleanExample,
                onSuccess = { onNavigateBack() },
            )
        } else {
            viewModel.createWord(
                deckId = deckId,
                term = term.trim(),
                meaning = meaning.trim(),
                phonetic = cleanPhonetic,
                example = cleanExample,
                onSuccess = { onNavigateBack() },
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WnHeader(
                title = if (isEditMode) "Sửa từ vựng" else "Thêm từ mới",
                onBack = onNavigateBack,
            )

            // 1. Từ tiếng Anh (bắt buộc, ≤ 100 ký tự) + Autocomplete & nút "Tra & dịch"
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Từ tiếng Anh *",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(
                        onClick = {
                            viewModel.lookupAndTranslate(
                                rawTerm = term,
                                currentPhonetic = phonetic,
                                currentMeaning = meaning,
                            )
                        },
                        enabled = term.isNotBlank() && suggestState.status != WordSuggestStatus.Loading,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tra & dịch",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                OutlinedTextField(
                    value = term,
                    onValueChange = { input ->
                        val cleanInput = if (input.length <= Constants.MAX_WORD_TERM_LENGTH) {
                            termError = null
                            input
                        } else {
                            termError = "Từ tiếng Anh tối đa ${Constants.MAX_WORD_TERM_LENGTH} ký tự"
                            input.take(Constants.MAX_WORD_TERM_LENGTH)
                        }
                        term = cleanInput
                        viewModel.onTermInputChanged(cleanInput)
                    },
                    placeholder = {
                        Text(
                            text = "Ví dụ: apple, ocean, resilient...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    isError = termError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.lookupAndTranslate(
                                rawTerm = term,
                                currentPhonetic = phonetic,
                                currentMeaning = meaning,
                            )
                        },
                    ),
                    supportingText = {
                        FieldSupportingRow(
                            error = termError,
                            currentLength = term.trim().length,
                            maxLength = Constants.MAX_WORD_TERM_LENGTH,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (suggestState.suggestions.isNotEmpty()) {
                    SuggestionDropdown(
                        items = suggestState.suggestions,
                        onSelect = { selectedWord ->
                            term = selectedWord
                            termError = null
                            viewModel.onSuggestionSelected(
                                selectedWord = selectedWord,
                                currentPhonetic = phonetic,
                                currentMeaning = meaning,
                            )
                        },
                    )
                }

                SuggestStatusRow(status = suggestState.status)
            }

            // 2. Nghĩa tiếng Việt (bắt buộc, ≤ 500 ký tự) + Chip nghĩa gợi ý kiểu Quizlet
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nghĩa tiếng Việt *",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { input ->
                        val cleanInput = if (input.length <= Constants.MAX_WORD_MEANING_LENGTH) {
                            meaningError = null
                            input
                        } else {
                            meaningError = "Nghĩa tiếng Việt tối đa ${Constants.MAX_WORD_MEANING_LENGTH} ký tự"
                            input.take(Constants.MAX_WORD_MEANING_LENGTH)
                        }
                        meaning = cleanInput
                        viewModel.onMeaningInputChanged(cleanInput)
                    },
                    placeholder = {
                        Text(
                            text = "Ví dụ: Quả táo, đại dương...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    isError = meaningError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    supportingText = {
                        FieldSupportingRow(
                            error = meaningError,
                            currentLength = meaning.trim().length,
                            maxLength = Constants.MAX_WORD_MEANING_LENGTH,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (suggestState.meaningOptions.isNotEmpty()) {
                    Text(
                        text = "Nghĩa gợi ý (chạm để chọn / ghép nhiều nghĩa):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        suggestState.meaningOptions.forEach { option ->
                            val isSelected = suggestState.selectedMeanings.any {
                                it.equals(option, ignoreCase = true)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    meaning = viewModel.toggleMeaningChip(option, meaning)
                                    meaningError = null
                                },
                                label = { Text(text = option) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.secondary,
                                ),
                            )
                        }
                    }
                }
            }

            // 3. Phiên âm (tùy chọn, ≤ 100 ký tự)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Phiên âm",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { input ->
                        val cleanInput = if (input.length <= Constants.MAX_WORD_PHONETIC_LENGTH) {
                            phoneticError = null
                            input
                        } else {
                            phoneticError = "Phiên âm tối đa ${Constants.MAX_WORD_PHONETIC_LENGTH} ký tự"
                            input.take(Constants.MAX_WORD_PHONETIC_LENGTH)
                        }
                        phonetic = cleanInput
                        viewModel.onPhoneticInputChanged(term, cleanInput)
                    },
                    placeholder = {
                        Text(
                            text = "Ví dụ: /ˈæp.əl/ (tự điền khi tra từ)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    isError = phoneticError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    supportingText = {
                        FieldSupportingRow(
                            error = phoneticError,
                            currentLength = phonetic.trim().length,
                            maxLength = Constants.MAX_WORD_PHONETIC_LENGTH,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 4. Câu ví dụ (tùy chọn, ≤ 500 ký tự, multiline) + Thẻ câu ví dụ gợi ý
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Câu ví dụ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { input ->
                        if (input.length <= Constants.MAX_WORD_EXAMPLE_LENGTH) {
                            example = input
                            exampleError = null
                        } else {
                            example = input.take(Constants.MAX_WORD_EXAMPLE_LENGTH)
                            exampleError = "Câu ví dụ tối đa ${Constants.MAX_WORD_EXAMPLE_LENGTH} ký tự"
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Ví dụ: I eat an apple every day. (tùy chọn)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    isError = exampleError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    supportingText = {
                        FieldSupportingRow(
                            error = exampleError,
                            currentLength = example.trim().length,
                            maxLength = Constants.MAX_WORD_EXAMPLE_LENGTH,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (suggestState.exampleOptions.isNotEmpty()) {
                    Text(
                        text = "Câu ví dụ gợi ý (chạm để điền):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    suggestState.exampleOptions.forEach { suggestion ->
                        val isPicked = suggestion.equals(example.trim(), ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    example = suggestion.take(Constants.MAX_WORD_EXAMPLE_LENGTH)
                                    exampleError = null
                                },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPicked) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPicked) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                        ) {
                            Text(
                                text = "“$suggestion”",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = if (isPicked) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            WnPrimaryButton(
                text = if (isEditMode) "Lưu thay đổi" else "Lưu từ",
                enabled = !isSaving && (!isEditMode || wordLoadState is WordLoadState.Found),
                onClick = ::submit,
            )

            if (isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isSaving && wordLoadState is WordLoadState.Found,
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                        )
                        Text(
                            text = "Xóa từ này",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && isEditMode) {
        ConfirmDialog(
            title = "Xóa từ vựng?",
            message = "Bạn có chắc muốn xóa từ \"${term.ifBlank { "này" }}\" khỏi Tổ này? Từ sẽ biến khỏi danh sách, tìm kiếm và thống kê.",
            confirmText = "Xóa",
            dismissText = "Huỷ",
            onConfirm = {
                showDeleteConfirm = false
                didPopOnMissing = true
                viewModel.deleteWord(wordId!!, onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun SuggestionDropdown(
    items: List<String>,
    onSelect: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            items.forEachIndexed { index, suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SuggestStatusRow(status: WordSuggestStatus) {
    when (status) {
        WordSuggestStatus.Idle -> Unit

        WordSuggestStatus.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đang tra phiên âm, nghĩa & câu ví dụ...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        WordSuggestStatus.Empty -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chưa có gợi ý sẵn cho từ này — bạn vẫn có thể nhập tay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is WordSuggestStatus.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FieldSupportingRow(
    error: String?,
    currentLength: Int,
    maxLength: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = error.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$currentLength/$maxLength",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
