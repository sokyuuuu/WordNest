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
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.components.ConfirmDialog
import com.wordnest.ui.components.PrimaryButton
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.DangerRed
import com.wordnest.ui.theme.DangerRedBg
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.viewmodel.WordEvent
import com.wordnest.ui.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordEditScreen(
    wordId: String?,
    wordViewModel: WordViewModel,
    onNavigateBack: () -> Unit
) {
    val isEditMode = !wordId.isNullOrBlank()
    val existingWordFlow = remember(wordId) {
        if (isEditMode) wordViewModel.observeWordById(wordId!!) else null
    }
    val existingWord by (existingWordFlow?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf(null) })

    val isTranslating by wordViewModel.isTranslating.collectAsStateWithLifecycle()

    var term by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(existingWord) {
        if (isEditMode && existingWord != null && !initialized) {
            term = existingWord!!.term
            phonetic = existingWord!!.phonetic ?: ""
            meaning = existingWord!!.meaning
            example = existingWord!!.example ?: ""
            note = existingWord!!.note ?: ""
            initialized = true
        }
    }

    LaunchedEffect(Unit) {
        wordViewModel.events.collect { event ->
            if (event is WordEvent.ShowMessage && event.isError) {
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
                        text = if (isEditMode) "Chỉnh sửa từ" else "Thêm từ mới",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Từ tiếng Anh * + Nút Dịch tự động (Gist API)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Từ tiếng Anh *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = {
                            wordViewModel.translateTerm(term) { translated ->
                                meaning = translated
                            }
                        },
                        enabled = term.isNotBlank() && !isTranslating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = ForestGreen
                        )
                        Text(
                            text = if (isTranslating) " Đang dịch (DeepL)..." else " Dịch tự động (DeepL)",
                            color = ForestGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    placeholder = { Text("Ví dụ: serendipity") },
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

            // 2. Phiên âm
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Phiên âm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    placeholder = { Text("Ví dụ: /ˌserənˈdɪpəti/ (tùy chọn)") },
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

            // 3. Nghĩa tiếng Việt *
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nghĩa tiếng Việt *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    placeholder = { Text("Nhập nghĩa tiếng Việt của từ...") },
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

            // 4. Câu ví dụ
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Câu ví dụ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    placeholder = { Text("Ví dụ câu sử dụng từ trong ngữ cảnh (tùy chọn)") },
                    minLines = 2,
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

            // 5. Ghi chú
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ghi chú",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ghi chú mẹo nhớ, từ đồng nghĩa, giới từ đi kèm... (tùy chọn)") },
                    minLines = 2,
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

            Spacer(modifier = Modifier.height(6.dp))

            PrimaryButton(
                text = if (isEditMode) "Lưu thay đổi" else "Thêm vào Tổ",
                enabled = term.isNotBlank() && meaning.isNotBlank(),
                onClick = {
                    val phoneticOrNull = phonetic.trim().ifEmpty { null }
                    val exampleOrNull = example.trim().ifEmpty { null }
                    val noteOrNull = note.trim().ifEmpty { null }

                    if (isEditMode && existingWord != null) {
                        wordViewModel.updateWord(
                            word = existingWord!!,
                            newTerm = term,
                            newMeaning = meaning,
                            newPhonetic = phoneticOrNull,
                            newExample = exampleOrNull,
                            newNote = noteOrNull,
                            onSuccess = { onNavigateBack() }
                        )
                    } else {
                        wordViewModel.addWord(
                            term = term,
                            meaning = meaning,
                            phonetic = phoneticOrNull,
                            example = exampleOrNull,
                            note = noteOrNull,
                            onSuccess = { onNavigateBack() }
                        )
                    }
                }
            )

            if (isEditMode && existingWord != null) {
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
                        Text("Xóa từ này", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && existingWord != null) {
        ConfirmDialog(
            title = "Xóa từ vựng?",
            message = "Bạn có chắc chắn muốn xóa từ \"${existingWord!!.term}\" khỏi Tổ từ vựng không?",
            confirmText = "Xóa",
            onConfirm = {
                showDeleteConfirm = false
                wordViewModel.deleteWord(existingWord!!.wordId, onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
