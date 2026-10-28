package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN", // "OPEN", "REVIEWED", "DISMISSED", "ACTIONED"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBlock(
    @DocumentId val id: String = "",
    val blockerId: String = "",
    val blockedUserId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetEntity: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
