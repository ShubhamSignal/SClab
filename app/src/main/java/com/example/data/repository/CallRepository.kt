package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.model.CallSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository(
    private val context: Context,
    private val walletRepository: WalletRepository
) {
    private val db = FirestoreProvider.get(context)
    private val auth = FirestoreProvider.auth

    private fun requireUid(): String = auth.currentUser?.uid ?: error("User not authenticated")

    fun observeCall(callId: String): Flow<CallSession?> {
        return db.collection("calls").document(callId).snapshots().map { it.toObject(CallSession::class.java) }
    }

    fun observeIncomingCalls(userId: String? = auth.currentUser?.uid): Flow<List<CallSession>> {
        val uid = userId ?: auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("calls")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("status", "RINGING")
            .snapshots()
            .map { it.toObjects(CallSession::class.java) }
    }

    suspend fun startCall(
        receiverId: String,
        receiverName: String,
        callerName: String,
        callType: String // "AUDIO" or "VIDEO"
    ): CallSession {
        val uid = requireUid()
        require(uid != receiverId) { "Self-calling is prohibited for anti-fraud security." }

        val callId = UUID.randomUUID().toString()
        val session = CallSession(
            id = callId,
            callerId = uid,
            callerName = callerName,
            receiverId = receiverId,
            receiverName = receiverName,
            callType = callType,
            status = "RINGING",
            channelName = "sclab_ch_$callId",
            sdpOffer = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=SClab Call\r\nt=0 0\r\nm=$callType 9 UDP/TLS/RTP/SAVPF 111\r\na=sendrecv",
            startedAt = Timestamp.now()
        )

        db.collection("calls").document(callId).set(session).await()
        return session
    }

    suspend fun acceptCall(callId: String) {
        val uid = requireUid()
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "CONNECTED",
                "connectedAt" to Timestamp.now(),
                "sdpAnswer" to "v=0\r\no=- 54321 2 IN IP4 127.0.0.1\r\ns=SClab Call Answer\r\nt=0 0\r\na=sendrecv"
            )
        ).await()
    }

    suspend fun rejectCall(callId: String) {
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "REJECTED",
                "endedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun endCall(callId: String): Long {
        val callSnapshot = db.collection("calls").document(callId).get().await()
        val call = callSnapshot.toObject(CallSession::class.java) ?: return 0L

        val connectedAtTime = call.connectedAt?.toDate()?.time
        val nowTime = System.currentTimeMillis()
        val durationSeconds = if (connectedAtTime != null && nowTime > connectedAtTime) {
            (nowTime - connectedAtTime) / 1000L
        } else {
            0L
        }

        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "ENDED",
                "endedAt" to Timestamp.now(),
                "durationSeconds" to durationSeconds
            )
        ).await()

        // Call-earning calculation: credit receiving member if duration > 0 and caller != receiver
        if (durationSeconds > 0 && call.callerId != call.receiverId) {
            try {
                walletRepository.creditCallEarning(
                    callId = callId,
                    earningUserId = call.receiverId,
                    durationSeconds = durationSeconds
                )
            } catch (e: Exception) {
                Log.e("CallRepository", "Failed to credit call earning", e)
            }
        }

        return durationSeconds
    }
}
