package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WebSocketChatDataSource : ChatRemoteDataSource {

    private val _messageFlow = MutableSharedFlow<List<MessageDto>>()
    private val conversationsMap = mutableMapOf<String, ConversationDto>()

    override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeMessages(conversationId: String): Flow<List<MessageDto>> {
        return _messageFlow.asSharedFlow()
    }

    override suspend fun fetchMessageHistory(
        conversationId: String,
        limit: Int,
        lastTimestamp: Long?
    ): Result<List<MessageDto>> {
        return Result.success(emptyList())
    }

    override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> {
        conversationsMap[conversationDto.id] = conversationDto
        return Result.success(Unit)
    }

    override fun observeConversations(userId: String): Flow<List<ConversationDto>> {
        return MutableSharedFlow<List<ConversationDto>>().asSharedFlow()
    }

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getOrCreateDirectConversation(currentUserId: String, recipientUserId: String): Result<ConversationDto> {
        val sortedParticipants = listOf(currentUserId, recipientUserId).sorted()
        val directChatId = "direct_${sortedParticipants[0]}_${sortedParticipants[1]}"
        val existing = conversationsMap[directChatId]
        if (existing != null) {
            return Result.success(existing)
        }
        val newConv = ConversationDto(
            id = directChatId,
            title = "Chat with $recipientUserId",
            isGroup = false,
            participants = sortedParticipants,
            updatedAt = System.currentTimeMillis()
        )
        conversationsMap[directChatId] = newConv
        return Result.success(newConv)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        conversationsMap.remove(conversationId)
        return Result.success(Unit)
    }
}
