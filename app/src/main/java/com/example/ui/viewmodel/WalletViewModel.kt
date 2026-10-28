package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountData
import com.example.data.model.EarningLedgerEntry
import com.example.data.model.WalletData
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class WalletActionState {
    object Idle : WalletActionState()
    object Loading : WalletActionState()
    data class Success(val message: String) : WalletActionState()
    data class Error(val message: String) : WalletActionState()
}

class WalletViewModel(
    private val walletRepository: WalletRepository,
    private val userId: String
) : ViewModel() {

    val wallet: StateFlow<WalletData?> = walletRepository.observeWallet(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ledger: StateFlow<List<EarningLedgerEntry>> = walletRepository.observeLedger(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccount: StateFlow<BankAccountData?> = walletRepository.observeBankAccount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _actionState = MutableStateFlow<WalletActionState>(WalletActionState.Idle)
    val actionState: StateFlow<WalletActionState> = _actionState.asStateFlow()

    fun submitBankDetails(
        holderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String
    ) {
        viewModelScope.launch {
            _actionState.value = WalletActionState.Loading
            val result = walletRepository.submitBankAccount(holderName, bankName, accountNumber, ifscCode)
            result.onSuccess {
                _actionState.value = WalletActionState.Success("Bank account details saved successfully. Status: PENDING review.")
            }.onFailure { err ->
                _actionState.value = WalletActionState.Error(err.message ?: "Failed to submit bank details.")
            }
        }
    }

    fun requestWithdrawal(amountInr: Double) {
        val amountPaise = (amountInr * 100).toLong()
        viewModelScope.launch {
            _actionState.value = WalletActionState.Loading
            val result = walletRepository.requestWithdrawal(amountPaise)
            result.onSuccess {
                _actionState.value = WalletActionState.Success("Withdrawal request for ₹${amountInr.toInt()} submitted successfully.")
            }.onFailure { err ->
                _actionState.value = WalletActionState.Error(err.message ?: "Withdrawal request failed.")
            }
        }
    }

    fun clearActionState() {
        _actionState.value = WalletActionState.Idle
    }
}
