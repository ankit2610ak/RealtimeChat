package com.realtimechat.feature.chat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.realtimechat.core.designsystem.ChatBubble
import com.realtimechat.core.designsystem.MessageInputBar
import com.realtimechat.core.designsystem.UiState
import com.realtimechat.core.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    currentUserId: String,
    conversationTitle: String,
    viewModel: ChatViewModel,
    conversationId: String,
    secretKey: ByteArray,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                viewModel.sendImage(conversationId, bitmap, currentUserId, secretKey)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = conversationTitle, fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            MessageInputBar(
                text = uiState.inputText,
                onTextChanged = viewModel::updateInputText,
                onSendClicked = { viewModel.sendMessage(conversationId, currentUserId, secretKey) },
                onMediaPickerClicked = { photoPickerLauncher.launch("image/*") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState.messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    val groupedMessages = state.data.groupBy {
                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.timestamp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedMessages.forEach { (dateHeader, messages) ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(messages, key = { it.messageId }) { message ->
                                ChatBubble(
                                    text = message.plaintext,
                                    isFromMe = message.senderId == currentUserId,
                                    timestamp = message.timestamp,
                                    uploadProgress = uiState.uploadProgress
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewChatScreenHeader() {
    MaterialTheme {
        TopAppBar(title = { Text("Alice Smith (E2EE Active)") })
    }
}
