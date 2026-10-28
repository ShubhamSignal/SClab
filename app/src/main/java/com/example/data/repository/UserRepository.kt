package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.PublicProfile
import com.example.data.model.UserBlock
import com.example.data.model.UserData
import com.example.data.model.UserReport
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

class UserRepository(context: Context) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireUid(): String = auth.currentUser?.uid ?: error("User not authenticated")

    suspend fun getUserData(userId: String? = auth.currentUser?.uid): UserData? {
        val uid = userId ?: auth.currentUser?.uid ?: return null
        if (uid.isBlank()) return null
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(UserData::class.java)
    }

    fun observeUserData(userId: String? = auth.currentUser?.uid): Flow<UserData?> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("users").document(uid).snapshots().map { it.toObject(UserData::class.java) }
    }

    suspend fun syncPublicProfile(userData: UserData) {
        val age = calculateAge(userData.dob)
        val profile = PublicProfile(
            id = userData.userId,
            userId = userData.userId,
            displayName = userData.fullName,
            gender = userData.gender,
            age = age,
            avatarUrl = "",
            isOnline = true,
            bio = "Active on SClab • Voice & Video ready",
            updatedAt = Timestamp.now()
        )
        db.collection("publicProfiles").document(userData.userId).set(profile, SetOptions.merge()).await()
    }

    suspend fun discoverPeople(currentUserGender: String): List<PublicProfile> {
        val uid = requireUid()
        // Fetch users the current user has blocked
        val blockedSnapshot = db.collection("blocks")
            .whereEqualTo("blockerId", uid)
            .get()
            .await()
        val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedUserId") }.toSet()

        // Match opposite gender per requirements
        val targetGender = if (currentUserGender.equals("Male", ignoreCase = true)) "Female" else "Male"

        val querySnapshot = db.collection("publicProfiles")
            .whereEqualTo("gender", targetGender)
            .limit(30)
            .get()
            .await()

        val list = querySnapshot.toObjects(PublicProfile::class.java)
            .filter { it.userId != uid && !blockedIds.contains(it.userId) }

        // If target gender has few users on demo launch, include all other available profiles (excluding current user & blocked)
        if (list.isEmpty()) {
            val allSnapshot = db.collection("publicProfiles")
                .limit(20)
                .get()
                .await()
            return allSnapshot.toObjects(PublicProfile::class.java)
                .filter { it.userId != uid && !blockedIds.contains(it.userId) }
        }

        return list
    }

    suspend fun reportUser(reportedUserId: String, reason: String, details: String) {
        val uid = requireUid()
        val report = UserReport(
            id = UUID.randomUUID().toString(),
            reporterId = uid,
            reportedUserId = reportedUserId,
            reason = reason,
            details = details,
            status = "OPEN",
            createdAt = Timestamp.now()
        )
        db.collection("userReports").document(report.id).set(report).await()
    }

    suspend fun blockUser(blockedUserId: String) {
        val uid = requireUid()
        val blockId = "${uid}_${blockedUserId}"
        val block = UserBlock(
            id = blockId,
            blockerId = uid,
            blockedUserId = blockedUserId,
            createdAt = Timestamp.now()
        )
        db.collection("blocks").document(blockId).set(block).await()
    }

    fun calculateAge(dobString: String): Int {
        if (dobString.isBlank()) return 21
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = LocalDate.parse(dobString.trim(), formatter)
            val currentDate = LocalDate.now()
            Period.between(birthDate, currentDate).years.coerceAtLeast(18)
        } catch (e: Exception) {
            21
        }
    }
}
