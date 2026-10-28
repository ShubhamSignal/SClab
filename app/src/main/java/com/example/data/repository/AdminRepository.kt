package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.AdminAuditLog
import com.example.data.model.BankAccountData
import com.example.data.model.CallSession
import com.example.data.model.FounderMembershipRecord
import com.example.data.model.PaymentOrder
import com.example.data.model.UserData
import com.example.data.model.UserReport
import com.example.data.model.WithdrawalRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class AdminSummaryStats(
    val totalUsers: Int = 0,
    val founderAssigned: Boolean = false,
    val founderEmail: String = "None",
    val totalRevenuePaise: Long = 0L,
    val pendingBankApprovals: Int = 0,
    val pendingWithdrawals: Int = 0,
    val openReports: Int = 0
)

class AdminRepository(context: Context) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireAdminEmail(): String = auth.currentUser?.email ?: "admin@sclab.in"
    private fun requireAdminUid(): String = auth.currentUser?.uid ?: "ADMIN_CONSOLE"

    suspend fun getSummaryStats(): AdminSummaryStats {
        val usersCount = db.collection("users").get().await().size()
        val founderSnap = db.collection("founderMembership").document("status").get().await()
        val founder = founderSnap.toObject(FounderMembershipRecord::class.java)

        val paymentsSnap = db.collection("paymentOrders").whereEqualTo("status", "SUCCESS").get().await()
        val totalRevenue = paymentsSnap.documents.sumOf { it.getLong("amountPaise") ?: 0L }

        val pendingBanks = db.collection("bankAccounts").whereEqualTo("status", "PENDING").get().await().size()
        val pendingWithdrawals = db.collection("withdrawalRequests").whereEqualTo("status", "PENDING").get().await().size()
        val openReports = db.collection("userReports").whereEqualTo("status", "OPEN").get().await().size()

        return AdminSummaryStats(
            totalUsers = usersCount,
            founderAssigned = founder?.isAssigned == true,
            founderEmail = if (founder?.isAssigned == true) founder.userEmail else "None",
            totalRevenuePaise = totalRevenue,
            pendingBankApprovals = pendingBanks,
            pendingWithdrawals = pendingWithdrawals,
            openReports = openReports
        )
    }

    suspend fun getAllUsers(): List<UserData> {
        val snap = db.collection("users").limit(50).get().await()
        return snap.toObjects(UserData::class.java)
    }

    suspend fun toggleUserSuspension(userId: String, suspend: Boolean) {
        db.collection("users").document(userId).update("isSuspended", suspend).await()
        recordAuditLog(
            action = if (suspend) "SUSPEND_USER" else "REINSTATE_USER",
            targetEntity = "users",
            targetId = userId,
            details = "User suspension status updated to $suspend"
        )
    }

    suspend fun getFounderRecord(): FounderMembershipRecord? {
        val snap = db.collection("founderMembership").document("status").get().await()
        return snap.toObject(FounderMembershipRecord::class.java)
    }

    suspend fun getPendingBankAccounts(): List<BankAccountData> {
        val snap = db.collection("bankAccounts").limit(50).get().await()
        return snap.toObjects(BankAccountData::class.java)
    }

    suspend fun updateBankAccountStatus(userId: String, status: String, reason: String = "") {
        db.collection("bankAccounts").document(userId).update(
            mapOf(
                "status" to status,
                "rejectionReason" to reason,
                "updatedAt" to Timestamp.now()
            )
        ).await()
        recordAuditLog(
            action = "UPDATE_BANK_STATUS_$status",
            targetEntity = "bankAccounts",
            targetId = userId,
            details = "Bank status set to $status. Reason: $reason"
        )
    }

    suspend fun getAllWithdrawalRequests(): List<WithdrawalRequest> {
        val snap = db.collection("withdrawalRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snap.toObjects(WithdrawalRequest::class.java)
    }

    suspend fun updateWithdrawalStatus(requestId: String, status: String, payoutRef: String = "") {
        db.collection("withdrawalRequests").document(requestId).update(
            mapOf(
                "status" to status,
                "payoutReferenceId" to payoutRef,
                "processedAt" to Timestamp.now()
            )
        ).await()
        recordAuditLog(
            action = "UPDATE_WITHDRAWAL_$status",
            targetEntity = "withdrawalRequests",
            targetId = requestId,
            details = "Withdrawal request marked as $status. Payout Ref: $payoutRef"
        )
    }

    suspend fun getAllReports(): List<UserReport> {
        val snap = db.collection("userReports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snap.toObjects(UserReport::class.java)
    }

    suspend fun resolveReport(reportId: String, status: String) {
        db.collection("userReports").document(reportId).update("status", status).await()
        recordAuditLog(
            action = "RESOLVE_REPORT_$status",
            targetEntity = "userReports",
            targetId = reportId,
            details = "Abuse report updated to $status"
        )
    }

    suspend fun getAuditLogs(): List<AdminAuditLog> {
        val snap = db.collection("adminAuditLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snap.toObjects(AdminAuditLog::class.java)
    }

    private suspend fun recordAuditLog(action: String, targetEntity: String, targetId: String, details: String) {
        val logId = UUID.randomUUID().toString()
        val log = AdminAuditLog(
            id = logId,
            adminId = requireAdminUid(),
            adminEmail = requireAdminEmail(),
            action = action,
            targetEntity = targetEntity,
            targetId = targetId,
            details = details,
            timestamp = Timestamp.now()
        )
        db.collection("adminAuditLogs").document(logId).set(log).await()
    }
}
