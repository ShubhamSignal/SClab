package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.AdminAuditLog
import com.example.data.model.FounderMembershipRecord
import com.example.data.model.Membership
import com.example.data.model.MembershipTier
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class FounderCheckResult {
    data class CurrentUserIsFounder(val record: FounderMembershipRecord) : FounderCheckResult()
    data class AlreadyAssignedToOther(val founderUserId: String) : FounderCheckResult()
    object AvailableForFirstUser : FounderCheckResult()
}

class FounderRepository(context: Context) {
    private val db: FirebaseFirestore = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private val founderDocRef = db.collection("founderMembership").document("status")

    /**
     * Checks the current authoritative assignment status in Firestore.
     */
    suspend fun checkFounderStatus(currentUserId: String? = auth.currentUser?.uid): FounderCheckResult {
        return try {
            val snapshot = founderDocRef.get().await()
            if (!snapshot.exists()) {
                FounderCheckResult.AvailableForFirstUser
            } else {
                val record = snapshot.toObject(FounderMembershipRecord::class.java)
                if (record != null && record.isAssigned) {
                    if (!currentUserId.isNullOrBlank() && record.userId == currentUserId) {
                        FounderCheckResult.CurrentUserIsFounder(record)
                    } else {
                        FounderCheckResult.AlreadyAssignedToOther(record.userId)
                    }
                } else {
                    FounderCheckResult.AvailableForFirstUser
                }
            }
        } catch (e: Exception) {
            Log.e("FounderRepository", "Error checking founder status", e)
            FounderCheckResult.AvailableForFirstUser
        }
    }

    /**
     * Atomically assigns the Founder Lifetime Free Membership to the current verified user.
     * Uses a Firestore transaction so simultaneous registrations cannot both receive the benefit.
     * Idempotent: If already assigned to the current user, succeeds safely.
     * If already assigned to someone else, fails safely without changing anything.
     */
    suspend fun claimFounderFreeMembership(
        userId: String,
        userEmail: String
    ): Result<FounderMembershipRecord> {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User must be signed in to claim founder membership"))
        }
        val effectiveUserId = if (userId.isNotBlank()) userId else currentUid
        val effectiveEmail = if (userEmail.isNotBlank()) userEmail else (auth.currentUser?.email ?: "founder@sclab.in")

        return try {
            val result = db.runTransaction { transaction ->
                val snapshot = transaction.get(founderDocRef)
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(FounderMembershipRecord::class.java)
                    if (existing != null && existing.isAssigned) {
                        if (existing.userId == effectiveUserId) {
                            // Already assigned to this exact user (idempotent retry)
                            return@runTransaction existing
                        } else {
                            throw IllegalStateException("Founder Free Membership has already been claimed by another user.")
                        }
                    }
                }

                // Not yet assigned: atomically assign to this user
                val auditId = UUID.randomUUID().toString()
                val founderRecord = FounderMembershipRecord(
                    id = "status",
                    isAssigned = true,
                    userId = effectiveUserId,
                    userEmail = effectiveEmail,
                    earningRatePaisePer15s = 500L, // Configured call-earning rate: ₹5 per 15-second interval
                    auditEventId = auditId,
                    assignedAt = Timestamp.now()
                )

                // 1. Write the permanent founder status record
                transaction.set(founderDocRef, founderRecord)

                // 2. Activate user's membership as FOUNDER_FREE (₹0 lifetime)
                val membershipDoc = db.collection("memberships").document(effectiveUserId)
                val membership = Membership(
                    id = effectiveUserId,
                    userId = effectiveUserId,
                    tier = MembershipTier.FOUNDER_FREE.name,
                    pricePaidPaise = 0L,
                    earningRatePaisePer15s = 500L,
                    status = "ACTIVE",
                    paymentOrderId = "FOUNDER_PROVISION_$auditId",
                    activatedAt = Timestamp.now()
                )
                transaction.set(membershipDoc, membership)

                // 3. Write immutable audit log
                val auditDoc = db.collection("adminAuditLogs").document(auditId)
                val auditLog = AdminAuditLog(
                    id = auditId,
                    adminId = effectiveUserId,
                    adminEmail = effectiveEmail,
                    action = "ASSIGN_FOUNDER_FREE_MEMBERSHIP",
                    targetEntity = "founderMembership",
                    targetId = effectiveUserId,
                    details = "First registered verified user $effectiveEmail assigned lifetime ₹0 founder membership.",
                    timestamp = Timestamp.now()
                )
                transaction.set(auditDoc, auditLog)

                founderRecord
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            Log.e("FounderRepository", "Transaction failed to claim founder membership", e)
            Result.failure(e)
        }
    }
}
