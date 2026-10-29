package com.realtimechat.core.data

import com.realtimechat.core.model.SafeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class PendingMessage(
    val messageId: String,
    val conversationId: String,
    val plaintext: String,
    val senderId: String,
    val secretKey: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingMessage
        return messageId == other.messageId
    }

    override fun hashCode(): Int = messageId.hashCode()
}

class OfflineMessageQueue(
    private val chatRepository: ChatRepository
) {

    private val pendingQueue = ConcurrentHashMap<String, PendingMessage>()
    private val _queueState = MutableStateFlow<List<PendingMessage>>(emptyList())
    val queueState: StateFlow<List<PendingMessage>> = _queueState.asStateFlow()

    fun enqueueMessage(pendingMessage: PendingMessage) {
        // Idempotent deduplication: ignore if message ID already queued
        if (pendingQueue.putIfAbsent(pendingMessage.messageId, pendingMessage) == null) {
            SafeLog.d(TAG, "Enqueued offline message: ${pendingMessage.messageId} for conv: ${pendingMessage.conversationId}")
            _queueState.value = pendingQueue.values.toList()
        }
    }

    suspend fun flushPendingMessages(): Int {
        val pendingList = pendingQueue.values.toList()
        if (pendingList.isEmpty()) return 0

        SafeLog.d(TAG, "Flushing ${pendingList.size} pending offline messages...")
        var successCount = 0

        for (msg in pendingList) {
            val result = chatRepository.sendMessage(
                conversationId = msg.conversationId,
                plaintext = msg.plaintext,
                secretKey = msg.secretKey,
                senderId = msg.senderId
            )

            if (result.isSuccess) {
                pendingQueue.remove(msg.messageId)
                successCount++
                SafeLog.d(TAG, "Successfully flushed offline message: ${msg.messageId}")
            } else {
                SafeLog.w(TAG, "Failed to flush offline message: ${msg.messageId}")
            }
        }

        _queueState.value = pendingQueue.values.toList()
        return successCount
    }

    fun clearQueue() {
        pendingQueue.clear()
        _queueState.value = emptyList()
    }

    companion object {
        private const val TAG = "OfflineMessageQueue"
    }
}
