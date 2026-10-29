package com.realtimechat.core.data

import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.ConversationDto
import com.realtimechat.core.network.MessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class ChatRepositoryTest {

    private val cryptoEngine = CryptoEngine()

    private class FakeChatRemoteDataSource : ChatRemoteDataSource {
        val sentMessages = mutableListOf<MessageDto>()

        override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> {
            sentMessages.add(messageDto)
            return Result.success(Unit)
        }

        override fun observeMessages(conversationId: String): Flow<List<MessageDto>> {
            return flowOf(sentMessages)
        }

        override suspend fun fetchMessageHistory(
            conversationId: String,
            limit: Int,
            lastTimestamp: Long?
        ): Result<List<MessageDto>> {
            return Result.success(sentMessages.take(limit))
        }

        override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> {
            return Result.success(Unit)
        }

        override fun observeConversations(userId: String): Flow<List<ConversationDto>> {
            return flowOf(emptyList())
        }

        override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun getOrCreateDirectConversation(currentUserId: String, recipientUserId: String): Result<ConversationDto> {
            return Result.success(ConversationDto(id = "c_123", title = recipientUserId, participants = listOf(currentUserId, recipientUserId)))
        }

        override suspend fun deleteConversation(conversationId: String): Result<Unit> {
            return Result.success(Unit)
        }
    }

    @Test
    fun testSendMessageAndObserveMessagesRoundtrip() = runTest {
        val fakeDataSource = FakeChatRemoteDataSource()
        val repository = ChatRepository(remoteDataSource = fakeDataSource, mediaDataSource = null, cryptoEngine = cryptoEngine)

        val secretKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val plaintext = "Hello Secret Message"
        val conversationId = "chat_123"
        val senderId = "usr_alice"

        // 1. Send encrypted message
        val sendResult = repository.sendMessage(conversationId, plaintext, secretKey, senderId)
        assertTrue(sendResult.isSuccess)
        assertEquals(1, fakeDataSource.sentMessages.size)

        // Verify sent message DTO payload is encrypted (not plaintext)
        val sentDto = fakeDataSource.sentMessages.first()
        assertTrue(sentDto.payload != plaintext)

        // 2. Observe and decrypt messages
        val messages = repository.observeMessages(conversationId, secretKey).first()
        assertEquals(1, messages.size)
        assertEquals(plaintext, messages.first().plaintext)
        assertEquals(senderId, messages.first().senderId)
    }

    @Test
    fun testOfflineMessageQueueDeduplicationAndFlush() = runTest {
        val fakeDataSource = FakeChatRemoteDataSource()
        val repository = ChatRepository(remoteDataSource = fakeDataSource, mediaDataSource = null, cryptoEngine = cryptoEngine)
        val offlineQueue = OfflineMessageQueue(repository)

        val secretKey = ByteArray(32) { 0x01 }
        val pendingMsg = PendingMessage(
            messageId = "msg_offline_1",
            conversationId = "chat_123",
            plaintext = "Offline message payload",
            senderId = "usr_alice",
            secretKey = secretKey
        )

        // Enqueue duplicate messages with same messageId
        offlineQueue.enqueueMessage(pendingMsg)
        offlineQueue.enqueueMessage(pendingMsg)

        // Idempotent queue size must be 1
        assertEquals(1, offlineQueue.queueState.value.size)

        // Flush queue
        val flushedCount = offlineQueue.flushPendingMessages()
        assertEquals(1, flushedCount)
        assertEquals(0, offlineQueue.queueState.value.size)
        assertEquals(1, fakeDataSource.sentMessages.size)
    }
}
