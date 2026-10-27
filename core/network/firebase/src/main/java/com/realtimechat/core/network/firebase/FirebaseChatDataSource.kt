package com.realtimechat.core.network.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.ConversationDto
import com.realtimechat.core.network.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseChatDataSource(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : ChatRemoteDataSource {

    private val firestore: FirebaseFirestore
        get() = firestoreProvider()

    override suspend fun sendMessage(conversationId: String, messageDto: MessageDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val messageRef = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageDto.messageId.ifEmpty { firestore.collection("conversations").document().id })

                messageRef.set(messageDto.copy(messageId = messageRef.id, conversationId = conversationId)).await()

                // Update last message snippet on conversation
                firestore.collection("conversations")
                    .document(conversationId)
                    .update("updatedAt", messageDto.timestamp, "lastMessageSnippet", "[Encrypted Message]")
                    .await()
                Unit
            }
        }

    override fun observeMessages(conversationId: String): Flow<List<MessageDto>> = callbackFlow {
        val query = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MessageDto::class.java)
                }
                trySend(messages)
            }
        }

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun fetchMessageHistory(
        conversationId: String,
        limit: Int,
        lastTimestamp: Long?
    ): Result<List<MessageDto>> = withContext(Dispatchers.IO) {
        runCatching {
            var query = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MessageDto::class.java)
            }.reversed()
        }
    }

    override suspend fun createConversation(conversationDto: ConversationDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val docRef = firestore.collection("conversations")
                    .document(conversationDto.id.ifEmpty { firestore.collection("conversations").document().id })

                docRef.set(conversationDto.copy(id = docRef.id)).await()
                Unit
            }
        }

    override fun observeConversations(userId: String): Flow<List<ConversationDto>> = callbackFlow {
        val query = firestore.collection("conversations")
            .whereArrayContains("participants", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If permission denied or firestore not enabled, emit empty list gracefully
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val conversations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ConversationDto::class.java)
                }
                trySend(conversations)
            }
        }

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                firestore.collection("conversations")
                    .document(conversationId)
                    .update("unreadCounts.$userId", 0)
                    .await()
                Unit
            }
        }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        recipientUserId: String
    ): Result<ConversationDto> = withContext(Dispatchers.IO) {
        runCatching {
            val sortedParticipants = listOf(currentUserId, recipientUserId).sorted()
            val directChatId = "direct_${sortedParticipants[0]}_${sortedParticipants[1]}"

            val docRef = firestore.collection("conversations").document(directChatId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                snapshot.toObject(ConversationDto::class.java) ?: throw IllegalStateException("Failed to parse conversation")
            } else {
                val newConv = ConversationDto(
                    id = directChatId,
                    title = "Chat with $recipientUserId",
                    isGroup = false,
                    participants = sortedParticipants,
                    updatedAt = System.currentTimeMillis(),
                    unreadCounts = mapOf(currentUserId to 0, recipientUserId to 0)
                )
                docRef.set(newConv).await()
                newConv
            }
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                firestore.collection("conversations")
                    .document(conversationId)
                    .delete()
                    .await()
                Unit
            }
        }
}
