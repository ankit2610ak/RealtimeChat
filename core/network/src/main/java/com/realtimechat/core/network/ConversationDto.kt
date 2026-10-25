package com.realtimechat.core.network

data class ConversationDto(
    val id: String = "",
    val title: String = "",
    val avatarUrl: String? = null,
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val lastMessageCiphertext: String? = null,
    val lastMessageIv: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderName: String? = null,
    val unreadCounts: Map<String, Int> = emptyMap()
)
