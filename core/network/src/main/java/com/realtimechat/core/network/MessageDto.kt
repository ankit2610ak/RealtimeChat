package com.realtimechat.core.network

data class MessageDto(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val payload: String = "", // Encrypted payload (IV + Ciphertext)
    val mediaUrl: String? = null,
    val timestamp: Long = 0L
)
