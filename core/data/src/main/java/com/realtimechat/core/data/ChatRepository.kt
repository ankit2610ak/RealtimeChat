package com.realtimechat.core.data

import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.crypto.EncryptedPayload
import com.realtimechat.core.model.Conversation
import com.realtimechat.core.model.Message
import com.realtimechat.core.model.SafeLog
import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.MediaRemoteDataSource
import com.realtimechat.core.network.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val remoteDataSource: ChatRemoteDataSource,
    private val mediaDataSource: MediaRemoteDataSource?,
    private val cryptoEngine: CryptoEngine = CryptoEngine()
) {

    private val defaultSecretKey = ByteArray(32) { 0x01 }

    /**
     * Observes real-time conversations for a user.
     * Decrypts last messages on Dispatchers.Default to prevent UI composition jank.
     */
    fun observeConversations(
        userId: String,
        conversationKeys: Map<String, ByteArray> = emptyMap()
    ): Flow<List<Conversation>> {
        SafeLog.d(TAG, "ChatRepository.observeConversations() called for userId: $userId")
        return remoteDataSource.observeConversations(userId)
            .map { dtoList ->
                SafeLog.d(TAG, "ChatRepository received ${dtoList.size} raw ConversationDtos from remoteDataSource")
                withContext(Dispatchers.Default) {
                    dtoList.map { dto ->
                        val secretKey = conversationKeys[dto.id] ?: defaultSecretKey
                        val ciphertext = dto.lastMessageCiphertext
                        val decryptedLastMsg = if (!ciphertext.isNullOrEmpty()) {
                            try {
                                val text = cryptoEngine.decrypt(EncryptedPayload(ciphertext), secretKey)
                                val senderName = dto.lastMessageSenderName
                                if (!senderName.isNullOrEmpty() && dto.isGroup) {
                                    "$senderName: $text"
                                } else {
                                    text
                                }
                            } catch (e: Exception) {
                                SafeLog.w(TAG, "Failed to decrypt last message for conversation ${dto.id}", e)
                                "[Encrypted Message]"
                            }
                        } else {
                            "No messages yet"
                        }

                        Conversation(
                            id = dto.id,
                            title = dto.title.ifEmpty { "Chat" },
                            avatarUrl = dto.avatarUrl,
                            isGroup = dto.isGroup,
                            lastMessageText = decryptedLastMsg,
                            lastMessageTimestamp = dto.lastMessageTimestamp.takeIf { it > 0 } ?: dto.updatedAt,
                            unreadCount = dto.unreadCounts[userId] ?: 0,
                            participants = dto.participants
                        ).also {
                            SafeLog.d(TAG, "Mapped Conversation -> ID: ${it.id}, title: ${it.title}, unread: ${it.unreadCount}, lastMsg: ${it.lastMessageText}")
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Connects with a user to get or create a direct 1:1 conversation.
     */
    suspend fun getOrCreateDirectConversation(currentUserId: String, recipientUserId: String): Result<Conversation> =
        withContext(Dispatchers.IO) {
            runCatching {
                SafeLog.d(TAG, "ChatRepository.getOrCreateDirectConversation() for currentUserId: $currentUserId, recipient: $recipientUserId")
                val dto = remoteDataSource.getOrCreateDirectConversation(currentUserId, recipientUserId).getOrThrow()
                Conversation(
                    id = dto.id,
                    title = dto.title.ifEmpty { "Chat with $recipientUserId" },
                    avatarUrl = dto.avatarUrl,
                    isGroup = dto.isGroup,
                    lastMessageText = dto.lastMessageCiphertext ?: "",
                    lastMessageTimestamp = dto.lastMessageTimestamp.takeIf { it > 0 } ?: dto.updatedAt,
                    unreadCount = dto.unreadCounts[currentUserId] ?: 0,
                    participants = dto.participants
                ).also {
                    SafeLog.d(TAG, "Successfully obtained direct conversation: ${it.id} (${it.title})")
                }
            }.onFailure { e ->
                SafeLog.e(TAG, "ChatRepository.getOrCreateDirectConversation() failed", e)
            }
        }

    /**
     * Resets unread count when a conversation is opened.
     */
    suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            remoteDataSource.markConversationAsRead(conversationId, userId)
        }

    /**
     * Deletes a conversation.
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            remoteDataSource.deleteConversation(conversationId)
        }

    /**
     * Sends an encrypted 1:1 or Group message on Dispatchers.IO.
     */
    suspend fun sendMessage(
        conversationId: String,
        plaintext: String,
        secretKey: ByteArray,
        senderId: String,
        mediaBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SafeLog.d(TAG, "ChatRepository.sendMessage() to conversationId: $conversationId, sender: $senderId")
            var mediaUrl: String? = null
            if (mediaBytes != null && mediaDataSource != null) {
                val mediaPath = "chats/$conversationId/media_${UUID.randomUUID()}.bin"
                val encryptedMedia = cryptoEngine.encrypt(Base64Encoder.encodeToString(mediaBytes), secretKey)
                mediaUrl = mediaDataSource.uploadEncryptedMedia(mediaPath, encryptedMedia.payload.toByteArray(Charsets.UTF_8)).getOrThrow()
            }

            val encryptedPayload = cryptoEngine.encrypt(plaintext, secretKey)
            val messageDto = MessageDto(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                payload = encryptedPayload.payload,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis()
            )

            remoteDataSource.sendMessage(conversationId, messageDto).getOrThrow()
            SafeLog.d(TAG, "ChatRepository.sendMessage() successfully sent message ${messageDto.messageId}")
            Unit
        }.onFailure { e ->
            SafeLog.e(TAG, "ChatRepository.sendMessage() failed", e)
        }
    }

    /**
     * Listens for realtime encrypted messages and decrypts payloads on Dispatchers.IO.
     */
    fun observeMessages(conversationId: String, secretKey: ByteArray): Flow<List<Message>> {
        return remoteDataSource.observeMessages(conversationId)
            .map { dtoList ->
                dtoList.mapNotNull { dto ->
                    try {
                        val decryptedText = cryptoEngine.decrypt(EncryptedPayload(dto.payload), secretKey)
                        Message(
                            messageId = dto.messageId,
                            conversationId = dto.conversationId,
                            senderId = dto.senderId,
                            plaintext = decryptedText,
                            mediaUrl = dto.mediaUrl,
                            timestamp = dto.timestamp
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Fetches paginated chat history on Dispatchers.IO.
     */
    suspend fun fetchMessageHistory(
        conversationId: String,
        secretKey: ByteArray,
        limit: Int = 20,
        lastTimestamp: Long? = null
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtoList = remoteDataSource.fetchMessageHistory(conversationId, limit, lastTimestamp).getOrThrow()
            dtoList.mapNotNull { dto ->
                try {
                    val decryptedText = cryptoEngine.decrypt(EncryptedPayload(dto.payload), secretKey)
                    Message(
                        messageId = dto.messageId,
                        conversationId = dto.conversationId,
                        senderId = dto.senderId,
                        plaintext = decryptedText,
                        mediaUrl = dto.mediaUrl,
                        timestamp = dto.timestamp
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private object Base64Encoder {
        fun encodeToString(bytes: ByteArray): String {
            return java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    companion object {
        private const val TAG = "ChatRepository"
    }
}
