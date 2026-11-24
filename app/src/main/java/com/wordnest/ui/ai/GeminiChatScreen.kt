
package com.wordnest.ui.ai

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordnest.viewmodel.AiChatMessage
import com.wordnest.viewmodel.GeminiViewModel

private val AiGreen = Color(0xFF287A53)
private val AiLightGreen = Color(0xFFD4EBDD)
private val AiYellow = Color(0xFFFFCA60)

@Composable
fun GeminiChatScreen(
    onBack: () -> Unit,
    viewModel: GeminiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val isNewChat = state.messages.size == 1 &&
            !state.isLoading

    LaunchedEffect(
        state.messages.size,
        state.isLoading,
        isNewChat
    ) {
        if (!isNewChat) {
            val lastIndex = if (state.isLoading) {
                state.messages.size
            } else {
                state.messages.lastIndex
            }

            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {

        // Thanh tieu de
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AiLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = AiGreen
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "WordNest AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Trợ lý học tiếng Anh",
                    style = MaterialTheme.typography.bodySmall,
                    color = AiGreen
                )
            }

            IconButton(
                onClick = viewModel::clearChat,
                enabled = !state.isLoading
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Cuộc trò chuyện mới"
                )
            }
        }

        HorizontalDivider()

        // Noi dung chat
        if (isNewChat) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    AiLightGreen,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = AiGreen,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Hôm nay học gì nào?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Cùng WordNest AI khám phá tiếng Anh mỗi ngày.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    AiSuggestionCard(
                        icon = Icons.Filled.MenuBook,
                        title = "Giải thích từ vựng",
                        description = "Tìm hiểu nghĩa và cách sử dụng",
                        onClick = {
                            viewModel.updateInput(
                                "Giải thích nghĩa của từ "
                            )
                        }
                    )
                }

                item {
                    AiSuggestionCard(
                        icon = Icons.Filled.Edit,
                        title = "Tạo câu ví dụ",
                        description = "Học từ vựng qua ngữ cảnh",
                        onClick = {
                            viewModel.updateInput(
                                "Tạo 3 câu ví dụ với từ "
                            )
                        }
                    )
                }

                item {
                    AiSuggestionCard(
                        icon = Icons.Filled.School,
                        title = "Luyện điền từ",
                        description = "Kiểm tra kiến thức của bạn",
                        onClick = {
                            viewModel.updateInput(
                                "Tạo bài tập điền từ ___ với từ "
                            )
                        }
                    )
                }
            }

        } else {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(state.messages) { message ->
                    AiMessageBubble(message)
                }

                if (state.isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AiGreen,
                                strokeWidth = 2.dp
                            )

                            Text(
                                text = "WordNest AI đang suy nghĩ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AiGreen
                            )
                        }
                    }
                }
            }
        }

        // Khung nhap tin nhan
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {

                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::updateInput,
                        placeholder = {
                            Text("Hỏi WordNest AI...")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(26.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AiGreen,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = AiGreen
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = viewModel::sendMessage,
                        enabled = state.input.isNotBlank() &&
                                !state.isLoading,
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AiYellow,
                            contentColor = Color(0xFF493C2D)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Gửi tin nhắn"
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "AI có thể mắc lỗi. Hãy kiểm tra thông tin quan trọng.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.Top
    ) {

        if (!message.isUser) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AiLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = AiGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {

            if (!message.isUser) {
                Text(
                    text = "WordNest AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AiGreen
                )

                Spacer(Modifier.height(6.dp))
            }

            Surface(
                color = if (message.isUser) {
                    AiYellow
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 18.dp
                )
            ) {
                Text(
                    text = if (message.isUser) {
                        buildAnnotatedString {
                            append(message.text)
                        }
                    } else {
                        formatAiMarkdown(message.text)
                    },                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        Color(0xFF493C2D)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        AiLightGreen,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AiGreen
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

    private fun formatAiMarkdown(content: String) =
        buildAnnotatedString {

            content.lines().forEachIndexed { index, rawLine ->

                val line = rawLine.trim()

                when {
                    line.matches(Regex("[-*_]{3,}")) -> {
                        append("\n")
                    }

                    line.startsWith("#") -> {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        ) {
                            append(
                                line.trimStart('#')
                                    .trim()
                                    .replace("**", "")
                            )
                        }
                    }

                    else -> {
                        val cleanLine = line.replace(
                            Regex("^[-*]\\s+"),
                            "• "
                        )

                        val parts = cleanLine.split("**")

                        parts.forEachIndexed { partIndex, part ->
                            val text = part.replace("`", "")

                            if (partIndex % 2 == 1) {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(text)
                                }
                            } else {
                                append(text)
                            }
                        }
                    }
                }

                if (index < content.lines().lastIndex) {
                    append("\n")
                }
            }
        }
