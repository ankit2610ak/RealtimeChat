package com.realtimechat.feature.conversations

import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.model.Conversation
import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.ConversationDto
import com.realtimechat.core.network.MessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationsViewModelTest {

    private class FakeDataSource : ChatRemoteDataSource {
        val conversations = mutableListOf(
            ConversationDto(id = "c1", title = "Alice", participants = listOf("u1", "u2"), unreadCounts = mapOf("u1" to 2))
        )

        override suspend fun sendMessage(conversationId: String, messageDto: MessageDto) = Result.success(Unit)
        override fun observeMessages(conversationId: String) = flowOf(emptyList<MessageDto>())
        override suspend fun fetchMessageHistory(conversationId: String, limit: Int, lastTimestamp: Long?) = Result.success(emptyList<MessageDto>())
        override suspend fun createConversation(conversationDto: ConversationDto) = Result.success(Unit)
        override fun observeConversations(userId: String) = flowOf(conversations)
        override suspend fun markConversationAsRead(conversationId: String, userId: String) = Result.success(Unit)
        override suspend fun getOrCreateDirectConversation(currentUserId: String, recipientUserId: String): Result<ConversationDto> {
            val conv = ConversationDto(id = "c_new", title = recipientUserId, participants = listOf(currentUserId, recipientUserId))
            conversations.add(conv)
            return Result.success(conv)
        }
    }

    @Test
    fun testStartDirectChatCreatesAndReturnsConversation() = runTest {
        val dataSource = FakeDataSource()
        val repository = ChatRepository(remoteDataSource = dataSource, mediaDataSource = null, cryptoEngine = CryptoEngine())
        val viewModel = ConversationsViewModel(repository)

        var createdConv: Conversation? = null
        viewModel.startDirectChat(
            currentUserId = "u1",
            recipientId = "u3",
            onSuccess = { createdConv = it },
            onError = {}
        )

        assertTrue(createdConv != null)
        assertEquals("u3", createdConv?.title)
    }
}
