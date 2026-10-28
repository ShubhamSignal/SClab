package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.BankAccountData
import com.example.data.model.EarningLedgerEntry
import com.example.data.model.Membership
import com.example.data.model.WalletData
import com.example.data.model.WithdrawalRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WalletRepository(context: Context) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireUid(): String = auth.currentUser?.uid ?: error("User not authenticated")

    fun observeWallet(userId: String? = auth.currentUser?.uid): Flow<WalletData?> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("wallets").document(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(WalletData::class.java)
            else WalletData(id = uid, userId = uid)
        }
    }

    fun observeLedger(userId: String? = auth.currentUser?.uid): Flow<List<EarningLedgerEntry>> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("earningLedger")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(EarningLedgerEntry::class.java) }
    }

    fun observeBankAccount(userId: String? = auth.currentUser?.uid): Flow<BankAccountData?> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("bankAccounts").document(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(BankAccountData::class.java) else null
        }
    }

    suspend fun getWallet(userId: String? = auth.currentUser?.uid): WalletData {
        val uid = userId ?: auth.currentUser?.uid ?: return WalletData()
        if (uid.isBlank()) return WalletData()
        val snapshot = db.collection("wallets").document(uid).get().await()
        return if (snapshot.exists()) {
            snapshot.toObject(WalletData::class.java) ?: WalletData(id = uid, userId = uid)
        } else {
            val initial = WalletData(id = uid, userId = uid)
            db.collection("wallets").document(uid).set(initial).await()
            initial
        }
    }

    /**
     * Authoritative call earning calculation and immutable ledger recording.
     * ₹100 plan -> ₹4 (400 paise) per qualifying 15-second interval
     * ₹200 plan -> ₹10 (1000 paise) per qualifying 15-second interval
     * Founder Free -> ₹5 (500 paise) per qualifying 15-second interval
     */
    suspend fun creditCallEarning(
        callId: String,
        earningUserId: String,
        durationSeconds: Long
    ) {
        val qualifyingIntervals = durationSeconds / 15L
        if (qualifyingIntervals <= 0) return

        // Check earning member's active plan rate
        val membershipSnap = db.collection("memberships").document(earningUserId).get().await()
        val membership = membershipSnap.toObject(Membership::class.java)
        val ratePaise = membership?.earningRatePaisePer15s ?: 400L // Default ₹4/15s

        val earnedPaise = qualifyingIntervals * ratePaise
        val transactionId = "TXN_EARN_${callId.take(8)}_${UUID.randomUUID().toString().take(6)}"

        val ledgerEntry = EarningLedgerEntry(
            id = transactionId,
            userId = earningUserId,
            callId = callId,
            callerOrReceiver = "RECEIVER",
            durationSeconds = durationSeconds,
            qualifyingIntervals = qualifyingIntervals,
            ratePaisePerInterval = ratePaise,
            amountPaise = earnedPaise,
            status = "CREDITED",
            note = "Call earning for ${durationSeconds}s ($qualifyingIntervals intervals @ ₹${ratePaise / 100}/15s)",
            timestamp = Timestamp.now()
        )

        val batch = db.batch()
        val ledgerDoc = db.collection("earningLedger").document(transactionId)
        batch.set(ledgerDoc, ledgerEntry)

        val walletDoc = db.collection("wallets").document(earningUserId)
        batch.set(
            walletDoc,
            mapOf(
                "userId" to earningUserId,
                "availableBalancePaise" to FieldValue.increment(earnedPaise),
                "lifetimeEarningsPaise" to FieldValue.increment(earnedPaise),
                "updatedAt" to Timestamp.now()
            ),
            SetOptions.merge()
        )

        batch.commit().await()
    }

    suspend fun submitBankAccount(
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String
    ): Result<BankAccountData> {
        val uid = requireUid()
        val cleanedIfsc = ifscCode.trim().uppercase()
        val cleanedAcc = accountNumber.trim()

        if (cleanedAcc.length < 8) {
            return Result.failure(IllegalArgumentException("Account number must be at least 8 digits"))
        }
        val ifscRegex = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
        if (!ifscRegex.matches(cleanedIfsc)) {
            return Result.failure(IllegalArgumentException("Invalid Indian IFSC code format (e.g. SBIN0001234)"))
        }

        val masked = "••••••••" + cleanedAcc.takeLast(4)
        val bankData = BankAccountData(
            id = uid,
            userId = uid,
            accountHolderName = accountHolderName.trim(),
            bankName = bankName.trim(),
            accountNumberMasked = masked,
            ifscCode = cleanedIfsc,
            status = "PENDING",
            submittedAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        return try {
            db.collection("bankAccounts").document(uid).set(bankData, SetOptions.merge()).await()
            Result.success(bankData)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Failed to submit bank account", e)
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(amountPaise: Long): Result<WithdrawalRequest> {
        val uid = requireUid()
        val email = auth.currentUser?.email ?: ""

        if (amountPaise < 200000L) { // Min ₹2,000
            return Result.failure(IllegalArgumentException("Automatic withdrawal eligibility begins when available balance reaches ₹2,000."))
        }

        val wallet = getWallet(uid)
        if (wallet.availableBalancePaise < amountPaise) {
            return Result.failure(IllegalArgumentException("Insufficient available balance (₹${wallet.availableBalanceInr})."))
        }

        val bankSnapshot = db.collection("bankAccounts").document(uid).get().await()
        val bank = bankSnapshot.toObject(BankAccountData::class.java)
        if (bank == null) {
            return Result.failure(IllegalStateException("Please submit bank account details before requesting withdrawal."))
        }

        val requestId = "WD_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val request = WithdrawalRequest(
            id = requestId,
            userId = uid,
            userEmail = email,
            amountPaise = amountPaise,
            status = "PENDING",
            bankDetailsSnapshot = "${bank.bankName} (${bank.accountNumberMasked}) IFSC: ${bank.ifscCode}",
            requestedAt = Timestamp.now()
        )

        return try {
            val batch = db.batch()
            val requestDoc = db.collection("withdrawalRequests").document(requestId)
            batch.set(requestDoc, request)

            val walletDoc = db.collection("wallets").document(uid)
            batch.update(
                walletDoc,
                mapOf(
                    "availableBalancePaise" to FieldValue.increment(-amountPaise),
                    "totalWithdrawnPaise" to FieldValue.increment(amountPaise),
                    "updatedAt" to Timestamp.now()
                )
            )

            batch.commit().await()
            Result.success(request)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Failed to submit withdrawal request", e)
            Result.failure(e)
        }
    }
}
