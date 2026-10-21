package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow

data class EncryptedMessageDto(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val ciphertext: String,
    val iv: String,
    val timestamp: Long
)

interface ChatRemoteDataSource {
    suspend fun sendMessage(chatId: String, message: EncryptedMessageDto): Result<Unit>
    fun observeMessages(chatId: String): Flow<List<EncryptedMessageDto>>
    suspend fun createChat(chatId: String, participants: List<String>): Result<Unit>
}
