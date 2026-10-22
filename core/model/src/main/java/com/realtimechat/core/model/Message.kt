package com.realtimechat.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val plaintext: String,
    val mediaUrl: String? = null,
    val timestamp: Long
)
