package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WebSocketChatDataSource : ChatRemoteDataSource {

    private val _messageFlow = MutableSharedFlow<List<MessageDto>>()

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
        return Result.success(Unit)
    }

    override fun observeConversations(userId: String): Flow<List<ConversationDto>> {
        return MutableSharedFlow<List<ConversationDto>>().asSharedFlow()
    }

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
