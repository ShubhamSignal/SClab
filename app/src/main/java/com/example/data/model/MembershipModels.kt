package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MembershipTier(
    val title: String,
    val subtitle: String,
    val pricePaise: Long,
    val priceDisplay: String,
    val ratePer15sPaise: Long,
    val rateDisplay: String,
    val paymentUrl: String?
) {
    FOUNDER_FREE(
        title = "Founder Free Membership — ₹0",
        subtitle = "First Member — Lifetime Free",
        pricePaise = 0L,
        priceDisplay = "₹0 Lifetime Free",
        ratePer15sPaise = 500L,
        rateDisplay = "₹5 / 15-sec call",
        paymentUrl = null
    ),
    PLAN_A(
        title = "Membership Plan A",
        subtitle = "One-Time Activation",
        pricePaise = 10000L,
        priceDisplay = "₹100 One-Time",
        ratePer15sPaise = 400L,
        rateDisplay = "₹4 / 15-sec call",
        paymentUrl = "https://rzp.io/rzp/yKaFCdCc"
    ),
    PLAN_B(
        title = "Membership Plan B",
        subtitle = "Premium High-Earning Tier",
        pricePaise = 20000L,
        priceDisplay = "₹200 One-Time",
        ratePer15sPaise = 1000L,
        rateDisplay = "₹10 / 15-sec call",
        paymentUrl = "https://rzp.io/rzp/1UabMOX"
    )
}

@IgnoreExtraProperties
data class FounderMembershipRecord(
    @DocumentId val id: String = "status",
    val isAssigned: Boolean = false,
    val userId: String = "",
    val userEmail: String = "",
    val earningRatePaisePer15s: Long = 500L,
    val auditEventId: String = "",
    @ServerTimestamp val assignedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Membership(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tier: String = MembershipTier.PLAN_A.name,
    val pricePaidPaise: Long = 0L,
    val earningRatePaisePer15s: Long = 400L,
    val status: String = "INACTIVE", // "INACTIVE", "ACTIVE", "SUSPENDED"
    val paymentOrderId: String = "",
    @ServerTimestamp val activatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tier: String = "",
    val amountPaise: Long = 0L,
    val paymentUrl: String = "",
    val status: String = "PENDING", // "PENDING", "SUCCESS", "FAILED", "CANCELLED"
    val razorpayPaymentId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val completedAt: Timestamp? = null
)
