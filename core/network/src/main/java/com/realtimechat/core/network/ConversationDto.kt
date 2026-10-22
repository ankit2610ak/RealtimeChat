package com.realtimechat.core.network

data class ConversationDto(
    val conversationId: String = "",
    val isGroup: Boolean = false,
    val title: String? = null,
    val participants: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val lastMessageSnippet: String? = null
)
