package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(context: Context) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireUid(): String = auth.currentUser?.uid ?: error("User not authenticated")

    fun observeConversations(userId: String? = auth.currentUser?.uid): Flow<List<Conversation>> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java)
                    .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { it.toObjects(ChatMessage::class.java) }
    }

    suspend fun getOrCreateConversation(
        targetUserId: String,
        targetUserName: String,
        currentUserName: String
    ): String {
        val uid = requireUid()
        // Conversation id sorted deterministically
        val conversationId = if (uid < targetUserId) "${uid}_${targetUserId}" else "${targetUserId}_${uid}"
        val docRef = db.collection("conversations").document(conversationId)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            val conv = Conversation(
                id = conversationId,
                participantIds = listOf(uid, targetUserId),
                participantNames = mapOf(uid to currentUserName, targetUserId to targetUserName),
                lastMessageText = "Conversation started",
                lastSenderId = uid,
                lastMessageAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            docRef.set(conv).await()
        }
        return conversationId
    }

    suspend fun sendMessage(conversationId: String, text: String, senderName: String) {
        val uid = requireUid()
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = uid,
            senderName = senderName,
            text = text.trim(),
            status = "DELIVERED",
            createdAt = Timestamp.now()
        )

        val batch = db.batch()
        val messageDoc = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        batch.set(messageDoc, message)

        val convDoc = db.collection("conversations").document(conversationId)
        batch.update(
            convDoc,
            mapOf(
                "lastMessageText" to text.trim(),
                "lastSenderId" to uid,
                "lastMessageAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        )
        batch.commit().await()
    }
}
