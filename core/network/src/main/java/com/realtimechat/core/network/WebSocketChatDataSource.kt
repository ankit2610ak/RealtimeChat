package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Custom WebSocket/Ktor remote data source implementation demonstrating zero-code UI change backend migration.
 */
class WebSocketChatDataSource : ChatRemoteDataSource {

    private val _messageFlow = MutableSharedFlow<List<MessageDto>>()

    override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> {
        // Send message payload over WebSocket binary/json frame
        return Result.success(Unit)
    }

    override fun observeMessages(conversationId: String): Flow<List<MessageDto>> {
        // Stream realtime WebSocket incoming frames
        return _messageFlow.asSharedFlow()
    }

    override suspend fun fetchMessageHistory(
        conversationId: String,
        limit: Int,
        lastTimestamp: Long?
    ): Result<List<MessageDto>> {
        // Fetch paginated message history over Ktor/Retrofit REST API
        return Result.success(emptyList())
    }

    override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeConversations(userId: String): Flow<List<ConversationDto>> {
        return MutableSharedFlow<List<ConversationDto>>().asSharedFlow()
    }
}
