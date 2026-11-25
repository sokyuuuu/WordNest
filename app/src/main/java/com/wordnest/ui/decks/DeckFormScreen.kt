package com.wordnest.ui.decks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.ui.components.WnHeader
import com.wordnest.ui.components.WnPrimaryButton
import com.wordnest.ui.decks.components.ColorPicker
import com.wordnest.ui.decks.components.ConfirmDialog
import com.wordnest.ui.decks.components.IconPicker
import com.wordnest.util.Constants
import com.wordnest.util.Validation

/**
 * Form Tạo mới & Chỉnh sửa Tổ từ vựng dùng chung (F01, F02).
 * - Giữ nguyên nội dung đang nhập khi xoay màn hình nhờ [rememberSaveable].
 * - Tự quay về danh sách nếu Tổ đang sửa đã bị xoá hoặc không tồn tại (T035).
 * - Hỗ trợ bàn phím mềm (`ImeAction.Done`, `imePadding`) và chế độ sáng/tối.
 */
@Composable
fun DeckFormScreen(
    deckId: String?,
    viewModel: DeckViewModel,
    onNavigateBack: () -> Unit,
) {
    val isEditMode = !deckId.isNullOrBlank()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val deckLoadStateFlow = remember(deckId, viewModel) {
        if (isEditMode) viewModel.observeDeckLoadState(deckId!!) else null
    }
    val deckLoadState: DeckLoadState = if (deckLoadStateFlow != null) {
        deckLoadStateFlow.collectAsStateWithLifecycle(initialValue = DeckLoadState.Loading).value
    } else {
        DeckLoadState.Loading
    }

    var name by rememberSaveable(deckId) { mutableStateOf("") }
    var description by rememberSaveable(deckId) { mutableStateOf("") }
    var selectedIcon by rememberSaveable(deckId) { mutableStateOf(DeckStyle.DEFAULT_ICON) }
    var selectedColor by rememberSaveable(deckId) { mutableStateOf(DeckStyle.DEFAULT_COLOR) }
    var initialized by rememberSaveable(deckId) { mutableStateOf(false) }
    var nameError by rememberSaveable(deckId) { mutableStateOf<String?>(null) }
    var descError by rememberSaveable(deckId) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by rememberSaveable(deckId) { mutableStateOf(false) }
    var didPopOnMissing by remember(deckId) { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deckLoadState) {
        if (!isEditMode) return@LaunchedEffect
        when (val state = deckLoadState) {
            is DeckLoadState.Found -> {
                if (!initialized) {
                    name = state.deck.nameDeck
                    description = state.deck.description.orEmpty()
                    selectedIcon = state.deck.icon
                    selectedColor = state.deck.color
                    initialized = true
                }
            }

            DeckLoadState.Missing -> {
                if (!didPopOnMissing) {
                    didPopOnMissing = true
                    onNavigateBack()
                }
            }

            DeckLoadState.Loading -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is DeckEvent.ShowMessage && event.isError) {
                if (event.message.contains("đã tồn tại", ignoreCase = true)) {
                    nameError = event.message
                }
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    fun submit() {
        val validatedNameError = Validation.validateDeckName(name)
        val validatedDescError = Validation.validateDeckDescription(description)
        nameError = validatedNameError
        descError = validatedDescError
        if (validatedNameError != null || validatedDescError != null) return

        val descOrNull = description.trim().ifEmpty { null }
        if (isEditMode) {
            viewModel.updateDeck(
                deckId = deckId!!,
                newName = name.trim(),
                newDescription = descOrNull,
                newIcon = selectedIcon,
                newColor = selectedColor,
                onSuccess = onNavigateBack,
            )
        } else {
            viewModel.createDeck(
                name = name.trim(),
                description = descOrNull,
                icon = selectedIcon,
                color = selectedColor,
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
                title = if (isEditMode) "Sửa Tổ" else "Tạo Tổ mới",
                onBack = onNavigateBack,
            )

            // Tên Tổ (bắt buộc, ≤ 100 ký tự)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tên Tổ *",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        if (input.length <= Constants.MAX_DECK_NAME_LENGTH) {
                            name = input
                            nameError = null
                        } else {
                            name = input.take(Constants.MAX_DECK_NAME_LENGTH)
                            nameError = "Tên Tổ tối đa ${Constants.MAX_DECK_NAME_LENGTH} ký tự"
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Ví dụ: IELTS, Giao tiếp công sở...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    isError = nameError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = nameError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${name.trim().length}/${Constants.MAX_DECK_NAME_LENGTH}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Mô tả (tùy chọn, ≤ 500 ký tự)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mô tả",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { input ->
                        if (input.length <= Constants.MAX_DECK_DESC_LENGTH) {
                            description = input
                            descError = null
                        } else {
                            description = input.take(Constants.MAX_DECK_DESC_LENGTH)
                            descError = "Mô tả Tổ tối đa ${Constants.MAX_DECK_DESC_LENGTH} ký tự"
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Mô tả ngắn về chủ đề bộ từ này (tùy chọn)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    isError = descError != null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = descError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${description.trim().length}/${Constants.MAX_DECK_DESC_LENGTH}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Biểu tượng",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconPicker(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Màu sắc",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            WnPrimaryButton(
                text = if (isEditMode) "Lưu thay đổi" else "Tạo Tổ",
                enabled = !isSaving,
                onClick = ::submit,
            )

            if (isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isSaving,
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
                            text = "Xóa Tổ này",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && deckId != null) {
        ConfirmDialog(
            title = "Xóa Tổ từ vựng?",
            message = "Xoá Tổ \"${name.ifBlank { "này" }}\" sẽ xoá cả toàn bộ từ bên trong. Bạn có chắc muốn tiếp tục?",
            confirmText = "Xóa",
            dismissText = "Huỷ",
            onConfirm = {
                showDeleteConfirm = false
                didPopOnMissing = true
                viewModel.deleteDeck(deckId, onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
