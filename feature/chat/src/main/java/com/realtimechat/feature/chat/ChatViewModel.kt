package com.realtimechat.feature.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.data.MediaPipeline
import com.realtimechat.core.designsystem.UiState
import com.realtimechat.core.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messagesState: UiState<List<Message>> = UiState.Loading,
    val inputText: String = "",
    val uploadProgress: Float? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun observeMessages(conversationId: String, secretKey: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(messagesState = UiState.Loading) }
            chatRepository.observeMessages(conversationId, secretKey)
                .catch { e ->
                    _uiState.update { it.copy(messagesState = UiState.Error(e.message ?: "Failed to load messages")) }
                }
                .collect { messages ->
                    _uiState.update { it.copy(messagesState = UiState.Success(messages)) }
                }
        }
    }

    fun sendMessage(conversationId: String, senderId: String, secretKey: ByteArray) {
        val textToSend = _uiState.value.inputText.trim()
        if (textToSend.isEmpty()) return

        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(
                conversationId = conversationId,
                plaintext = textToSend,
                secretKey = secretKey,
                senderId = senderId
            )
        }
    }

    fun sendImage(conversationId: String, bitmap: Bitmap, senderId: String, secretKey: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(uploadProgress = 0.1f) }
            val compressedBytes = MediaPipeline.compressImage(bitmap)
            _uiState.update { it.copy(uploadProgress = 0.5f) }

            chatRepository.sendMessage(
                conversationId = conversationId,
                plaintext = "[Image Attachment]",
                secretKey = secretKey,
                senderId = senderId,
                mediaBytes = compressedBytes
            )
            _uiState.update { it.copy(uploadProgress = null) }
        }
    }
}
