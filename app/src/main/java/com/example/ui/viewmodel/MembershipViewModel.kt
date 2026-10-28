package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FounderMembershipRecord
import com.example.data.model.Membership
import com.example.data.model.MembershipTier
import com.example.data.model.PaymentOrder
import com.example.data.repository.FounderCheckResult
import com.example.data.repository.FounderRepository
import com.example.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MembershipUiState {
    object Loading : MembershipUiState()
    data class FounderFreeActive(val record: FounderMembershipRecord, val membership: Membership) : MembershipUiState()
    data class PaidTierActive(val membership: Membership) : MembershipUiState()
    data class SelectionRequired(
        val isFirstMemberEligible: Boolean,
        val plans: List<MembershipTier>
    ) : MembershipUiState()
    data class PaymentPending(val order: PaymentOrder) : MembershipUiState()
    data class Error(val message: String) : MembershipUiState()
}

class MembershipViewModel(
    private val membershipRepository: MembershipRepository,
    private val founderRepository: FounderRepository,
    private val userId: String,
    private val userEmail: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MembershipUiState>(MembershipUiState.Loading)
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    init {
        loadMembershipStatus()
    }

    fun loadMembershipStatus() {
        if (userId.isBlank()) {
            _uiState.value = MembershipUiState.SelectionRequired(
                isFirstMemberEligible = false,
                plans = listOf(MembershipTier.PLAN_A, MembershipTier.PLAN_B)
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = MembershipUiState.Loading
            try {
                // First, check if user already has an active membership
                val existing = membershipRepository.getMembership(userId)
                if (existing != null && existing.status == "ACTIVE") {
                    if (existing.tier == MembershipTier.FOUNDER_FREE.name) {
                        val founderRecord = founderRepository.checkFounderStatus(userId)
                        val record = if (founderRecord is FounderCheckResult.CurrentUserIsFounder) {
                            founderRecord.record
                        } else {
                            FounderMembershipRecord(isAssigned = true, userId = userId, userEmail = userEmail)
                        }
                        _uiState.value = MembershipUiState.FounderFreeActive(record, existing)
                        return@launch
                    } else {
                        _uiState.value = MembershipUiState.PaidTierActive(existing)
                        return@launch
                    }
                }

                // If not active, check if eligible for Founder Free
                val founderStatus = founderRepository.checkFounderStatus(userId)
                when (founderStatus) {
                    is FounderCheckResult.CurrentUserIsFounder -> {
                        // Activate free membership
                        val claim = founderRepository.claimFounderFreeMembership(userId, userEmail)
                        claim.onSuccess { rec ->
                            val updatedMem = membershipRepository.getMembership(userId)
                            if (updatedMem != null) {
                                _uiState.value = MembershipUiState.FounderFreeActive(rec, updatedMem)
                            }
                        }
                    }
                    is FounderCheckResult.AvailableForFirstUser -> {
                        // First registered user gets lifetime free membership!
                        val claim = founderRepository.claimFounderFreeMembership(userId, userEmail)
                        if (claim.isSuccess) {
                            val rec = claim.getOrThrow()
                            val updatedMem = membershipRepository.getMembership(userId)!!
                            _uiState.value = MembershipUiState.FounderFreeActive(rec, updatedMem)
                        } else {
                            _uiState.value = MembershipUiState.SelectionRequired(
                                isFirstMemberEligible = false,
                                plans = listOf(MembershipTier.PLAN_A, MembershipTier.PLAN_B)
                            )
                        }
                    }
                    is FounderCheckResult.AlreadyAssignedToOther -> {
                        // Regular user: Display Plan A (₹100) and Plan B (₹200)
                        _uiState.value = MembershipUiState.SelectionRequired(
                            isFirstMemberEligible = false,
                            plans = listOf(MembershipTier.PLAN_A, MembershipTier.PLAN_B)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MembershipUiState.Error(e.message ?: "Failed to load membership")
            }
        }
    }

    fun selectPaidPlan(context: Context, tier: MembershipTier) {
        viewModelScope.launch {
            try {
                val order = membershipRepository.createPaymentOrder(tier)
                _uiState.value = MembershipUiState.PaymentPending(order)

                // Open Razorpay hosted payment link
                val url = tier.paymentUrl
                if (!url.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                _uiState.value = MembershipUiState.Error(e.message ?: "Payment order creation failed")
            }
        }
    }

    fun verifyAndActivatePayment(orderId: String, paymentReference: String, tier: MembershipTier) {
        viewModelScope.launch {
            _uiState.value = MembershipUiState.Loading
            val result = membershipRepository.activatePaidMembership(orderId, paymentReference, tier)
            result.onSuccess { mem ->
                _uiState.value = MembershipUiState.PaidTierActive(mem)
            }.onFailure { err ->
                _uiState.value = MembershipUiState.Error("Payment verification failed: ${err.message}")
            }
        }
    }
}
