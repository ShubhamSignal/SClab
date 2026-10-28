package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.Membership
import com.example.data.model.MembershipTier
import com.example.data.model.PaymentOrder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MembershipRepository(context: Context) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireUid(): String = auth.currentUser?.uid ?: error("User not authenticated")

    fun observeUserMembership(userId: String? = auth.currentUser?.uid): Flow<Membership?> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("memberships").document(uid).snapshots().map { snapshot ->
            snapshot.toObject(Membership::class.java)
        }
    }

    suspend fun getMembership(userId: String? = auth.currentUser?.uid): Membership? {
        val uid = userId ?: auth.currentUser?.uid ?: return null
        if (uid.isBlank()) return null
        val snapshot = db.collection("memberships").document(uid).get().await()
        return snapshot.toObject(Membership::class.java)
    }

    suspend fun createPaymentOrder(tier: MembershipTier): PaymentOrder {
        val uid = requireUid()
        val orderId = "ORD_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val order = PaymentOrder(
            id = orderId,
            userId = uid,
            tier = tier.name,
            amountPaise = tier.pricePaise,
            paymentUrl = tier.paymentUrl ?: "",
            status = "PENDING",
            createdAt = Timestamp.now()
        )
        db.collection("paymentOrders").document(orderId).set(order).await()
        return order
    }

    suspend fun activatePaidMembership(orderId: String, paymentId: String, tier: MembershipTier): Result<Membership> {
        val uid = requireUid()
        return try {
            val orderRef = db.collection("paymentOrders").document(orderId)
            orderRef.update(
                mapOf(
                    "status" to "SUCCESS",
                    "razorpayPaymentId" to paymentId,
                    "completedAt" to Timestamp.now()
                )
            ).await()

            val membership = Membership(
                id = uid,
                userId = uid,
                tier = tier.name,
                pricePaidPaise = tier.pricePaise,
                earningRatePaisePer15s = tier.ratePer15sPaise,
                status = "ACTIVE",
                paymentOrderId = orderId,
                activatedAt = Timestamp.now()
            )

            db.collection("memberships").document(uid).set(membership, SetOptions.merge()).await()
            Result.success(membership)
        } catch (e: Exception) {
            Log.e("MembershipRepository", "Failed to activate paid membership", e)
            Result.failure(e)
        }
    }
}
