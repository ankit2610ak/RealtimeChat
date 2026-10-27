package com.realtimechat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.realtimechat.core.crypto.CryptoEngine
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.designsystem.theme.RealtimeChatTheme
import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.WebSocketChatDataSource
import com.realtimechat.core.network.firebase.FirebaseChatDataSource
import com.realtimechat.core.network.firebase.FirebaseMediaDataSource
import com.realtimechat.feature.chat.ChatScreen
import com.realtimechat.feature.chat.ChatViewModel
import com.realtimechat.feature.conversations.ConversationsScreen
import com.realtimechat.feature.conversations.ConversationsViewModel
import com.realtimechat.feature.group.GroupInfoScreen
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate() invoked.")

        val cryptoEngine = CryptoEngine()
        val remoteDataSource: ChatRemoteDataSource = try {
            val ds = FirebaseChatDataSource()
            Log.d(TAG, "Using FirebaseChatDataSource.")
            ds
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize FirebaseChatDataSource, falling back to WebSocketChatDataSource.", e)
            WebSocketChatDataSource()
        }

        val storageDataSource = FirebaseMediaDataSource()
        val repository = ChatRepository(
            remoteDataSource = remoteDataSource,
            mediaDataSource = storageDataSource,
            cryptoEngine = cryptoEngine
        )

        val conversationsViewModel = ConversationsViewModel(repository)
        val chatViewModel = ChatViewModel(repository)

        setContent {
            RealtimeChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentUserId by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            var currentUser = auth.currentUser
                            if (currentUser == null) {
                                Log.d(TAG, "No authenticated user found. Signing in anonymously...")
                                val authResult = auth.signInAnonymously().await()
                                currentUser = authResult.user
                                Log.d(TAG, "Anonymous sign-in successful: ${currentUser?.uid}")
                            } else {
                                Log.d(TAG, "Existing authenticated user found: ${currentUser.uid}")
                            }
                            currentUserId = currentUser?.uid ?: "usr_alice"
                        } catch (e: Exception) {
                            Log.w(TAG, "Firebase Auth offline/network error. Falling back to local offline user ID.", e)
                            currentUserId = "usr_alice"
                        }
                    }

                    if (currentUserId == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        MainAppContent(
                            currentUserId = currentUserId!!,
                            conversationsViewModel = conversationsViewModel,
                            chatViewModel = chatViewModel
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MainAppContent(
        currentUserId: String,
        conversationsViewModel: ConversationsViewModel,
        chatViewModel: ChatViewModel
    ) {
        val mockSecretKey = ByteArray(32) { 0x01 }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.ConversationsList) }

        when (val screen = currentScreen) {
            is Screen.ConversationsList -> {
                ConversationsScreen(
                    currentUserId = currentUserId,
                    viewModel = conversationsViewModel,
                    conversationKeys = emptyMap(),
                    onConversationClicked = { conversation ->
                        Log.d(TAG, "Navigating to ChatRoom for conversation: ${conversation.id}")
                        currentScreen = Screen.ChatRoom(
                            conversationId = conversation.id,
                            title = conversation.title,
                            isGroup = conversation.isGroup
                        )
                    },
                    onNewDirectChatClicked = {},
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
                    currentUserId = currentUserId,
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
                    members = listOf(currentUserId),
                    onAddParticipant = {}
                )
            }
        }
    }

    private sealed interface Screen {
        data object ConversationsList : Screen
        data class ChatRoom(val conversationId: String, val title: String, val isGroup: Boolean) : Screen
        data class GroupDetails(val conversationId: String, val title: String) : Screen
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
