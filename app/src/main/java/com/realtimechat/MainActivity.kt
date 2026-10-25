package com.realtimechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.designsystem.theme.RealtimeChatTheme
import com.realtimechat.core.network.ConversationDto
import com.realtimechat.core.network.MessageDto
import com.realtimechat.feature.chat.ChatScreen
import com.realtimechat.feature.chat.ChatViewModel
import com.realtimechat.feature.conversations.ConversationsScreen
import com.realtimechat.feature.conversations.ConversationsViewModel
import com.realtimechat.feature.group.GroupInfoScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cryptoEngine = CryptoEngine()
        val mockSecretKey = ByteArray(32) { 0x01 }

        val encryptedSample1 = runBlocking {
            cryptoEngine.encrypt("Late night, and passing mention it, flipped her.", mockSecretKey).payload
        }
        val encryptedSample2 = runBlocking {
            cryptoEngine.encrypt("Got a big plan, this mindset maybe its right.", mockSecretKey).payload
        }
        val encryptedSample3 = runBlocking {
            cryptoEngine.encrypt("At the right place and right time, maybe tonight..", mockSecretKey).payload
        }
        val encryptedSample4 = runBlocking {
            cryptoEngine.encrypt("Late night, and passing mention it.", mockSecretKey).payload
        }
        val initialChatMessage = runBlocking {
            cryptoEngine.encrypt("Hey Alice! Did you review the new UI changes?", mockSecretKey).payload
        }

        val chatMessagesFlow = MutableStateFlow<List<MessageDto>>(
            listOf(
                MessageDto(
                    messageId = "msg_1",
                    conversationId = "chat_1",
                    senderId = "usr_kathryn",
                    payload = initialChatMessage,
                    timestamp = System.currentTimeMillis() - (5 * 60 * 1000L)
                )
            )
        )

        val sampleDataSource = object : com.realtimechat.core.network.ChatRemoteDataSource {
            override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> {
                val current = chatMessagesFlow.value.toMutableList()
                current.add(messageDto)
                chatMessagesFlow.value = current
                return Result.success(Unit)
            }

            override fun observeMessages(conversationId: String): Flow<List<MessageDto>> = chatMessagesFlow

            override suspend fun fetchMessageHistory(conversationId: String, limit: Int, lastTimestamp: Long?): Result<List<MessageDto>> = Result.success(chatMessagesFlow.value)

            override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> = Result.success(Unit)

            override fun observeConversations(userId: String): Flow<List<ConversationDto>> = flowOf(
                listOf(
                    ConversationDto(
                        id = "chat_1",
                        title = "Kathryn Simmons",
                        isGroup = false,
                        lastMessageCiphertext = encryptedSample1,
                        lastMessageTimestamp = System.currentTimeMillis() - (5 * 60 * 1000L),
                        unreadCounts = mapOf("usr_alice" to 4)
                    ),
                    ConversationDto(
                        id = "chat_2",
                        title = "Albert Williamson",
                        isGroup = false,
                        lastMessageCiphertext = encryptedSample2,
                        lastMessageTimestamp = System.currentTimeMillis() - (20 * 60 * 1000L),
                        unreadCounts = mapOf("usr_alice" to 2)
                    ),
                    ConversationDto(
                        id = "chat_3",
                        title = "Serinton Williamson",
                        isGroup = false,
                        lastMessageCiphertext = encryptedSample3,
                        lastMessageTimestamp = System.currentTimeMillis() - (60 * 60 * 1000L),
                        unreadCounts = mapOf("usr_alice" to 0)
                    ),
                    ConversationDto(
                        id = "chat_4",
                        title = "Android Architects Group",
                        isGroup = true,
                        lastMessageSenderName = "Jorge Richards",
                        lastMessageCiphertext = encryptedSample4,
                        lastMessageTimestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000L),
                        unreadCounts = mapOf("usr_alice" to 24)
                    )
                )
            )

            override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> = Result.success(Unit)
        }

        val mockKeysMap = mapOf(
            "chat_1" to mockSecretKey,
            "chat_2" to mockSecretKey,
            "chat_3" to mockSecretKey,
            "chat_4" to mockSecretKey
        )

        val repository = ChatRepository(remoteDataSource = sampleDataSource, mediaDataSource = null, cryptoEngine = cryptoEngine)
        val conversationsViewModel = ConversationsViewModel(repository)
        val chatViewModel = ChatViewModel(repository)

        setContent {
            RealtimeChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.ConversationsList) }

                    when (val screen = currentScreen) {
                        is Screen.ConversationsList -> {
                            ConversationsScreen(
                                currentUserId = "usr_alice",
                                viewModel = conversationsViewModel,
                                conversationKeys = mockKeysMap,
                                onConversationClicked = { conversation ->
                                    currentScreen = Screen.ChatRoom(
                                        conversationId = conversation.id,
                                        title = conversation.title,
                                        isGroup = conversation.isGroup
                                    )
                                },
                                onNewDirectChatClicked = {
                                    currentScreen = Screen.ChatRoom(
                                        conversationId = "chat_direct_new",
                                        title = "Bob Smith",
                                        isGroup = false
                                    )
                                },
                                onNewGroupChatClicked = {
                                    currentScreen = Screen.GroupDetails(
                                        conversationId = "group_new",
                                        title = "New Group Chat"
                                    )
                                }
                            )
                        }

                        is Screen.ChatRoom -> {
                            BackHandler { currentScreen = Screen.ConversationsList }
                            ChatScreen(
                                currentUserId = "usr_alice",
                                conversationTitle = screen.title,
                                viewModel = chatViewModel,
                                conversationId = screen.conversationId,
                                secretKey = mockSecretKey,
                                onBackClicked = { currentScreen = Screen.ConversationsList }
                            )
                        }

                        is Screen.GroupDetails -> {
                            BackHandler { currentScreen = Screen.ConversationsList }
                            GroupInfoScreen(
                                groupTitle = screen.title,
                                groupFingerprint = "8f14 e2b0 9a01 4f33 1b0e 911f",
                                members = listOf("usr_alice", "usr_bob", "usr_charlie"),
                                onAddParticipant = {}
                            )
                        }
                    }
                }
            }
        }
    }

    private sealed interface Screen {
        data object ConversationsList : Screen
        data class ChatRoom(val conversationId: String, val title: String, val isGroup: Boolean) : Screen
        data class GroupDetails(val conversationId: String, val title: String) : Screen
    }
}
