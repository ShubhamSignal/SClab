package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val status: String = "SENT", // "SENT", "DELIVERED", "READ"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val callType: String = "AUDIO", // "AUDIO" or "VIDEO"
    val status: String = "RINGING", // "RINGING", "CONNECTING", "CONNECTED", "ENDED", "REJECTED", "MISSED"
    val channelName: String = "",
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidates: List<String> = emptyList(),
    val durationSeconds: Long = 0L,
    val eligibleEarnedPaise: Long = 0L,
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val connectedAt: Timestamp? = null,
    @ServerTimestamp val endedAt: Timestamp? = null
)
