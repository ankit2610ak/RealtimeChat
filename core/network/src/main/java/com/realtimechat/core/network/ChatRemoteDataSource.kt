package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit>
    fun observeMessages(conversationId: String): Flow<List<MessageDto>>
    suspend fun fetchMessageHistory(
        conversationId: String,
        limit: Int = 20,
        lastTimestamp: Long? = null
    ): Result<List<MessageDto>>
    suspend fun createConversation(conversationDto: ConversationDto): Result<Unit>
    fun observeConversations(userId: String): Flow<List<ConversationDto>>
    suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit>
    suspend fun getOrCreateDirectConversation(currentUserId: String, recipientUserId: String): Result<ConversationDto>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
