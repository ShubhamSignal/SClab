package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WalletData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val availableBalancePaise: Long = 0L,
    val pendingEarningsPaise: Long = 0L,
    val lifetimeEarningsPaise: Long = 0L,
    val totalWithdrawnPaise: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableBalanceInr: Double get() = availableBalancePaise / 100.0
    val lifetimeEarningsInr: Double get() = lifetimeEarningsPaise / 100.0
    val pendingEarningsInr: Double get() = pendingEarningsPaise / 100.0
    val isWithdrawalEligible: Boolean get() = availableBalancePaise >= 200000L // ₹2,000 in paise
}

@IgnoreExtraProperties
data class EarningLedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callId: String = "",
    val callerOrReceiver: String = "RECEIVER",
    val durationSeconds: Long = 0L,
    val qualifyingIntervals: Long = 0L, // 15-second intervals
    val ratePaisePerInterval: Long = 0L,
    val amountPaise: Long = 0L,
    val status: String = "CREDITED", // "CREDITED", "FLAGGED", "REVERSED"
    val note: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    val amountInr: Double get() = amountPaise / 100.0
}

@IgnoreExtraProperties
data class BankAccountData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountNumberMasked: String = "", // e.g. "••••••••1234"
    val ifscCode: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED", "PAYOUT_ELIGIBLE"
    val rejectionReason: String = "",
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val amountPaise: Long = 0L,
    val status: String = "PENDING", // "PENDING", "APPROVED", "PROCESSED", "REJECTED"
    val bankDetailsSnapshot: String = "",
    val payoutReferenceId: String = "",
    @ServerTimestamp val requestedAt: Timestamp? = null,
    @ServerTimestamp val processedAt: Timestamp? = null
) {
    val amountInr: Double get() = amountPaise / 100.0
}
