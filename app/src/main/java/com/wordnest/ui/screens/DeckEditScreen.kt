package com.wordnest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.ui.components.ColorPicker
import com.wordnest.ui.components.ConfirmDialog
import com.wordnest.ui.components.IconPicker
import com.wordnest.ui.components.PrimaryButton
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.DangerRed
import com.wordnest.ui.theme.DangerRedBg
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.DeckEvent
import com.wordnest.ui.viewmodel.DeckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditScreen(
    deckId: String?,
    viewModel: DeckViewModel,
    onNavigateBack: () -> Unit,
    onDeckDeleted: () -> Unit
) {
    val isEditMode = !deckId.isNullOrBlank()
    val existingDeckFlow = remember(deckId) {
        if (isEditMode) viewModel.observeDeckById(deckId!!) else null
    }
    val existingDeck by (existingDeckFlow?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf(null) })

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(DeckStyle.DEFAULT_ICON) }
    var selectedColor by remember { mutableStateOf(DeckStyle.DEFAULT_COLOR) }
    var initialized by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(existingDeck) {
        if (isEditMode && existingDeck != null && !initialized) {
            name = existingDeck!!.nameDeck
            description = existingDeck!!.description ?: ""
            selectedIcon = existingDeck!!.icon
            selectedColor = existingDeck!!.color
            initialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DeckEvent.ShowMessage && event.isError) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Chỉnh sửa Tổ" else "Tạo Tổ từ mới",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tên Tổ *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ví dụ: Từ vựng IELTS, Giao tiếp công sở...") },
                    singleLine = true,
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mô tả",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Mô tả ngắn về chủ đề bộ từ này (tùy chọn)") },
                    minLines = 3,
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



            Text(
                text = "Biểu tượng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            IconPicker(
                selectedIcon = selectedIcon,
                onIconSelected = { selectedIcon = it }
            )

            Text(
                text = "Màu sắc",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            ColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )



            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = if (isEditMode) "Lưu thay đổi" else "Tạo Tổ",
                enabled = name.isNotBlank(),
                onClick = {
                    val descOrNull = description.trim().ifEmpty { null }
                    if (isEditMode) {
                        viewModel.updateDeck(
                            deckId = deckId,
                            newName = name,
                            newDescription = descOrNull,
                            newIcon = selectedIcon,
                            newColor = selectedColor,
                            onSuccess = onNavigateBack
                        )
                    } else {
                        viewModel.createDeck(
                            name = name,
                            description = descOrNull,
                            icon = selectedIcon,
                            color = selectedColor,
                            onSuccess = { onNavigateBack() }
                        )
                    }
                }
            )

            if (isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DangerRedBg,
                        contentColor = DangerRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Text("Xóa Tổ này", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && deckId != null) {
        ConfirmDialog(
            title = "Xóa Tổ từ vựng?",
            message = "Tất cả từ vựng trong Tổ \"${name.ifBlank { "này" }}\" sẽ bị xóa vĩnh viễn. Hành động này không thể hoàn tác.",
            confirmText = "Xóa",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteDeck(deckId, onSuccess = onDeckDeleted)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
