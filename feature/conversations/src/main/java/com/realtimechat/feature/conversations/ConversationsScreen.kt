package com.realtimechat.feature.conversations

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.realtimechat.core.data.ChatRepository
import com.realtimechat.core.designsystem.theme.RealtimeChatTheme
import com.realtimechat.core.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    currentUserId: String,
    viewModel: ConversationsViewModel,
    onConversationClicked: (Conversation) -> Unit,
    onNewDirectChatClicked: () -> Unit,
    onNewGroupChatClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("All") }

    val filterCategories = listOf("All", "Personal", "Design", "Work", "Favourites")

    LaunchedEffect(currentUserId) {
        viewModel.observeConversations(currentUserId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            val currentQuery = (uiState as? ConversationsUiState.Success)?.searchQuery.orEmpty()
                            OutlinedTextField(
                                value = currentQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = { Text("Search chats...") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        viewModel.onSearchQueryChanged("")
                                    }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "Chats",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search Conversations")
                            }
                        }
                    }
                )

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    filterCategories.forEach { category ->
                        FilterChip(
                            selected = selectedFilterCategory == category,
                            onClick = { selectedFilterCategory = category },
                            label = { Text(text = category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            CreateChatFab(
                onNewDirectChat = onNewDirectChatClicked,
                onNewGroupChat = onNewGroupChatClicked
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ConversationsUiState.Loading -> {
                    ConversationsShimmerList()
                }

                is ConversationsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the + button below to start an end-to-end encrypted chat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                is ConversationsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ConversationsUiState.Success -> {
                    val filteredByChip = if (selectedFilterCategory == "All") {
                        state.filteredConversations
                    } else {
                        state.filteredConversations.filter {
                            if (selectedFilterCategory == "Personal") !it.isGroup
                            else if (selectedFilterCategory == "Work" || selectedFilterCategory == "Design") it.isGroup
                            else true
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = filteredByChip,
                            key = { it.id }
                        ) { conversation ->
                            ConversationListItem(
                                conversation = conversation,
                                onClick = {
                                    viewModel.markAsRead(conversation.id, currentUserId)
                                    onConversationClicked(conversation)
                                }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConversationsScreen() {
    RealtimeChatTheme {
        ConversationsScreen(
            currentUserId = "usr_alice",
            viewModel = ConversationsViewModel(
                ChatRepository(
                    remoteDataSource = com.realtimechat.core.network.WebSocketChatDataSource(),
                    mediaDataSource = null
                )
            ),
            onConversationClicked = {},
            onNewDirectChatClicked = {},
            onNewGroupChatClicked = {}
        )
    }
}
