package com.realtimechat.feature.conversations

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface ConversationsUiState {
    data object Loading : ConversationsUiState
    data object Empty : ConversationsUiState
    data class Success(
        val conversations: List<Conversation>,
        val filteredConversations: List<Conversation>,
        val searchQuery: String = ""
    ) : ConversationsUiState
    data class Error(val message: String) : ConversationsUiState
}

class ConversationsViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    fun observeConversations(userId: String, conversationKeys: Map<String, ByteArray> = emptyMap()) {
        viewModelScope.launch {
            _uiState.value = ConversationsUiState.Loading
            chatRepository.observeConversations(userId, conversationKeys)
                .catch { e ->
                    _uiState.value = ConversationsUiState.Error(e.message ?: "Failed to sync inbox")
                }
                .collect { list ->
                    if (list.isEmpty()) {
                        _uiState.value = ConversationsUiState.Empty
                    } else {
                        val currentQuery = (_uiState.value as? ConversationsUiState.Success)?.searchQuery.orEmpty()
                        val filtered = filterConversations(list, currentQuery)
                        _uiState.value = ConversationsUiState.Success(
                            conversations = list,
                            filteredConversations = filtered,
                            searchQuery = currentQuery
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is ConversationsUiState.Success) {
            val filtered = filterConversations(currentState.conversations, query)
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredConversations = filtered
            )
        }
    }

    fun markAsRead(conversationId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.markConversationAsRead(conversationId, userId)
        }
    }

    private fun filterConversations(list: List<Conversation>, query: String): List<Conversation> {
        if (query.isBlank()) return list
        return list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.lastMessageText.contains(query, ignoreCase = true)
        }
    }
}
