package com.realtimechat.core.network

import com.realtimechat.core.model.SafeLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class WebSocketChatDataSource : ChatRemoteDataSource {

    private val _messageFlows = mutableMapOf<String, MutableStateFlow<List<MessageDto>>>()
    private val _conversationsFlow = MutableStateFlow<List<ConversationDto>>(emptyList())

    override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> {
        SafeLog.d(TAG, "sendMessage() conversationId: $conversationId, messageId: ${messageDto.messageId}")
        val flow = _messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        val current = flow.value.toMutableList()
        current.add(messageDto)
        flow.value = current

        // Update conversation snippet and timestamp
        val convs = _conversationsFlow.value.toMutableList()
        val index = convs.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val updated = convs[index].copy(
                updatedAt = messageDto.timestamp,
                lastMessageCiphertext = messageDto.payload,
                lastMessageTimestamp = messageDto.timestamp,
                lastMessageSenderName = messageDto.senderId
            )
            convs[index] = updated
            _conversationsFlow.value = convs.sortedByDescending { it.updatedAt }
        }
        return Result.success(Unit)
    }

    override fun observeMessages(conversationId: String): Flow<List<MessageDto>> {
        return _messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
    }

    override suspend fun fetchMessageHistory(
        conversationId: String,
        limit: Int,
        lastTimestamp: Long?
    ): Result<List<MessageDto>> {
        val list = _messageFlows[conversationId]?.value ?: emptyList()
        return Result.success(list.take(limit))
    }

    override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> {
        SafeLog.d(TAG, "createConversation() id: ${conversationDto.id}")
        val current = _conversationsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == conversationDto.id }
        if (index != -1) {
            current[index] = conversationDto
        } else {
            current.add(conversationDto)
        }
        _conversationsFlow.value = current.sortedByDescending { it.updatedAt }
        return Result.success(Unit)
    }

    override fun observeConversations(userId: String): Flow<List<ConversationDto>> {
        SafeLog.d(TAG, "observeConversations() observing for userId: $userId")
        return _conversationsFlow.map { list ->
            list.filter { it.participants.isEmpty() || it.participants.contains(userId) }
        }
    }

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> {
        val convs = _conversationsFlow.value.toMutableList()
        val index = convs.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val unread = convs[index].unreadCounts.toMutableMap()
            unread[userId] = 0
            convs[index] = convs[index].copy(unreadCounts = unread)
            _conversationsFlow.value = convs
        }
        return Result.success(Unit)
    }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        recipientUserId: String
    ): Result<ConversationDto> {
        val sortedParticipants = listOf(currentUserId, recipientUserId).sorted()
        val directChatId = "direct_${sortedParticipants[0]}_${sortedParticipants[1]}"
        SafeLog.d(TAG, "getOrCreateDirectConversation() ID: $directChatId for $currentUserId and $recipientUserId")

        val currentConvs = _conversationsFlow.value
        val existing = currentConvs.find { it.id == directChatId }
        if (existing != null) {
            SafeLog.d(TAG, "Found existing conversation in memory: $directChatId")
            return Result.success(existing)
        }

        val newConv = ConversationDto(
            id = directChatId,
            title = "Chat with $recipientUserId",
            isGroup = false,
            participants = sortedParticipants,
            updatedAt = System.currentTimeMillis(),
            unreadCounts = mapOf(currentUserId to 0, recipientUserId to 0)
        )
        val updatedList = (currentConvs.filterNot { it.id == directChatId } + newConv).sortedByDescending { it.updatedAt }
        _conversationsFlow.value = updatedList
        SafeLog.d(TAG, "Created new conversation $directChatId. Total conversations now: ${updatedList.size}")
        return Result.success(newConv)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        SafeLog.d(TAG, "deleteConversation() ID: $conversationId")
        val current = _conversationsFlow.value.filterNot { it.id == conversationId }
        _conversationsFlow.value = current
        return Result.success(Unit)
    }

    companion object {
        private const val TAG = "WebSocketChatDataSource"
    }
}
