package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.repository.CallRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CallUiState {
    object Idle : CallUiState()
    data class Outgoing(val session: CallSession) : CallUiState()
    data class Incoming(val session: CallSession) : CallUiState()
    data class Connected(
        val session: CallSession,
        val durationSeconds: Long,
        val isMuted: Boolean,
        val isSpeakerOn: Boolean,
        val isVideoEnabled: Boolean,
        val isFrontCamera: Boolean,
        val estimatedEarningsPaise: Long
    ) : CallUiState()
    data class Ended(val durationSeconds: Long, val earnedPaise: Long) : CallUiState()
}

class CallViewModel(
    private val callRepository: CallRepository,
    private val currentUserId: String,
    private val earningRatePaisePer15s: Long = 400L
) : ViewModel() {

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var durationTimerJob: Job? = null
    private var callSessionJob: Job? = null
    private var activeCallId: String? = null

    var isMuted = false
        private set
    var isSpeakerOn = true
        private set
    var isVideoEnabled = true
        private set
    var isFrontCamera = true
        private set

    init {
        // Observe incoming calls in real-time only if user is logged in
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                callRepository.observeIncomingCalls(currentUserId).collect { incomingList ->
                    if (_uiState.value is CallUiState.Idle && incomingList.isNotEmpty()) {
                        val firstIncoming = incomingList.first()
                        activeCallId = firstIncoming.id
                        _uiState.value = CallUiState.Incoming(firstIncoming)
                        observeActiveSession(firstIncoming.id)
                    }
                }
            }
        }
    }

    fun startCall(receiverId: String, receiverName: String, callerName: String, callType: String) {
        viewModelScope.launch {
            try {
                val session = callRepository.startCall(receiverId, receiverName, callerName, callType)
                activeCallId = session.id
                _uiState.value = CallUiState.Outgoing(session)
                observeActiveSession(session.id)
            } catch (e: Exception) {
                _uiState.value = CallUiState.Idle
            }
        }
    }

    private fun observeActiveSession(callId: String) {
        callSessionJob?.cancel()
        callSessionJob = viewModelScope.launch {
            callRepository.observeCall(callId).collect { session ->
                if (session == null) return@collect

                when (session.status) {
                    "CONNECTED" -> {
                        if (_uiState.value !is CallUiState.Connected) {
                            startDurationTimer(session)
                        }
                    }
                    "ENDED", "REJECTED" -> {
                        stopDurationTimer()
                        val duration = session.durationSeconds
                        val intervals = duration / 15L
                        val earned = intervals * earningRatePaisePer15s
                        _uiState.value = CallUiState.Ended(duration, earned)
                    }
                }
            }
        }
    }

    private fun startDurationTimer(session: CallSession) {
        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                delay(1000)
                seconds++
                val intervals = seconds / 15L
                val earned = if (session.receiverId == currentUserId) intervals * earningRatePaisePer15s else 0L

                _uiState.value = CallUiState.Connected(
                    session = session,
                    durationSeconds = seconds,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isVideoEnabled = isVideoEnabled,
                    isFrontCamera = isFrontCamera,
                    estimatedEarningsPaise = earned
                )
            }
        }
    }

    private fun stopDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = null
    }

    fun acceptIncomingCall() {
        val callId = activeCallId ?: return
        viewModelScope.launch {
            callRepository.acceptCall(callId)
        }
    }

    fun rejectIncomingCall() {
        val callId = activeCallId ?: return
        viewModelScope.launch {
            callRepository.rejectCall(callId)
            _uiState.value = CallUiState.Idle
            activeCallId = null
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        updateConnectedState()
    }

    fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        updateConnectedState()
    }

    fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        updateConnectedState()
    }

    fun switchCamera() {
        isFrontCamera = !isFrontCamera
        updateConnectedState()
    }

    private fun updateConnectedState() {
        val current = _uiState.value
        if (current is CallUiState.Connected) {
            _uiState.value = current.copy(
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                isVideoEnabled = isVideoEnabled,
                isFrontCamera = isFrontCamera
            )
        }
    }

    fun endCurrentCall() {
        val callId = activeCallId ?: return
        viewModelScope.launch {
            val duration = callRepository.endCall(callId)
            stopDurationTimer()
            val intervals = duration / 15L
            val earned = intervals * earningRatePaisePer15s
            _uiState.value = CallUiState.Ended(duration, earned)
        }
    }

    fun resetCallState() {
        stopDurationTimer()
        callSessionJob?.cancel()
        activeCallId = null
        _uiState.value = CallUiState.Idle
    }
}
