
package com.wordnest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordnest.data.remote.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatMessage(
    val text: String,
    val isUser: Boolean
)

data class GeminiUiState(
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            text = "Xin chào! 👋 Mình là WordNest AI.\n" +
                    "Hôm nay bạn muốn học gì nào?",
            isUser = false
        )
    ),
    val input: String = "",
    val isLoading: Boolean = false
)

class GeminiViewModel : ViewModel() {

    private val repository = GeminiRepository()

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState = _uiState.asStateFlow()

    fun updateInput(text: String) {
        _uiState.update {
            it.copy(input = text)
        }
    }

    fun sendMessage() {
        val message = _uiState.value.input.trim()

        if (message.isBlank() || _uiState.value.isLoading) {
            return
        }

        submitMessage(message) {
            repository.sendMessage(message)
        }
    }

    fun generateExample(word: String) {
        val cleanWord = word.trim()

        if (cleanWord.isBlank()) return

        submitMessage(
            "Tạo câu ví dụ với từ: $cleanWord"
        ) {
            repository.generateExample(cleanWord)
        }
    }

    fun generateCloze(word: String) {
        val cleanWord = word.trim()

        if (cleanWord.isBlank()) return

        submitMessage(
            "Tạo bài tập điền từ với từ: $cleanWord"
        ) {
            repository.generateCloze(cleanWord)
        }
    }

    private fun submitMessage(
        userMessage: String,
        request: suspend () -> String
    ) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                messages = it.messages + AiChatMessage(
                    text = userMessage,
                    isUser = true
                ),
                input = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val response = request()

                _uiState.update {
                    it.copy(
                        messages = it.messages + AiChatMessage(
                            text = response,
                            isUser = false
                        )
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun clearChat() {
        if (_uiState.value.isLoading) return

        _uiState.value = GeminiUiState()
    }
}
