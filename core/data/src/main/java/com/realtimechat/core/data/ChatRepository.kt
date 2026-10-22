package com.realtimechat.core.data

import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.crypto.EncryptedPayload
import com.realtimechat.core.model.Message
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

    /**
     * Sends an encrypted 1:1 or Group message on Dispatchers.IO.
     * Plaintext payload is encrypted BEFORE writing to Firestore.
     */
    suspend fun sendMessage(
        conversationId: String,
        plaintext: String,
        secretKey: ByteArray,
        senderId: String,
        mediaBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
        }
    }

    /**
     * Listens for realtime encrypted messages and decrypts payloads on Dispatchers.IO.
     * Never logs plaintext messages or private keys.
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
}
