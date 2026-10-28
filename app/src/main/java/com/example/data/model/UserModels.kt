package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val gender: String = "Male", // "Male" or "Female"
    val dob: String = "",        // YYYY-MM-DD
    val role: String = "user",   // "user" or "admin"
    val isEmailVerified: Boolean = false,
    val isSuspended: Boolean = false,
    val isDeleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val gender: String = "Male",
    val age: Int = 18,
    val avatarUrl: String = "",
    val isOnline: Boolean = false,
    val bio: String = "Hey there! I am on SClab.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
