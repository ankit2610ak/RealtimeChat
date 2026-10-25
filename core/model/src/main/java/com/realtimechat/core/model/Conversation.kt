package com.realtimechat.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val participants: List<String> = emptyList()
)
