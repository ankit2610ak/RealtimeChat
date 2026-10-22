package com.realtimechat.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val conversationId: String,
    val isGroup: Boolean,
    val title: String?,
    val participants: List<String>,
    val updatedAt: Long,
    val lastMessageSnippet: String?
)
