package com.realtimechat.feature.conversations.navigation

object ConversationsNavigation {

    const val CONVERSATIONS_ROUTE = "conversations_route"
    
    fun buildChatRoute(conversationId: String, title: String, isGroup: Boolean): String {
        return "chat_route/$conversationId/$title/$isGroup"
    }

    fun buildGroupInfoRoute(conversationId: String, title: String): String {
        return "group_info_route/$conversationId/$title"
    }
}
