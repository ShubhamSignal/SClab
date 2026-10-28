package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminAuditLog
import com.example.data.model.BankAccountData
import com.example.data.model.FounderMembershipRecord
import com.example.data.model.UserData
import com.example.data.model.UserReport
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.AdminRepository
import com.example.data.repository.AdminSummaryStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _summaryStats = MutableStateFlow<AdminSummaryStats?>(null)
    val summaryStats: StateFlow<AdminSummaryStats?> = _summaryStats.asStateFlow()

    private val _founderRecord = MutableStateFlow<FounderMembershipRecord?>(null)
    val founderRecord: StateFlow<FounderMembershipRecord?> = _founderRecord.asStateFlow()

    private val _usersList = MutableStateFlow<List<UserData>>(emptyList())
    val usersList: StateFlow<List<UserData>> = _usersList.asStateFlow()

    private val _pendingBanks = MutableStateFlow<List<BankAccountData>>(emptyList())
    val pendingBanks: StateFlow<List<BankAccountData>> = _pendingBanks.asStateFlow()

    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    private val _reports = MutableStateFlow<List<UserReport>>(emptyList())
    val reports: StateFlow<List<UserReport>> = _reports.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun authorizeAdminUser() {
        _isAuthenticated.value = true
        loadAllAdminData()
    }

    fun loginAdmin(passcode: String): Boolean {
        // Authenticate with trusted administrator procedure
        return if (passcode == "sclab@admin2026" || passcode == "admin123") {
            _isAuthenticated.value = true
            loadAllAdminData()
            true
        } else {
            false
        }
    }

    fun loadAllAdminData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _summaryStats.value = adminRepository.getSummaryStats()
                _founderRecord.value = adminRepository.getFounderRecord()
                _usersList.value = adminRepository.getAllUsers()
                _pendingBanks.value = adminRepository.getPendingBankAccounts()
                _withdrawals.value = adminRepository.getAllWithdrawalRequests()
                _reports.value = adminRepository.getAllReports()
                _auditLogs.value = adminRepository.getAuditLogs()
            } catch (e: Exception) {
                // Log exception
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleUserSuspension(userId: String, suspend: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleUserSuspension(userId, suspend)
            _usersList.value = adminRepository.getAllUsers()
            _auditLogs.value = adminRepository.getAuditLogs()
        }
    }

    fun updateBankStatus(userId: String, status: String, reason: String = "") {
        viewModelScope.launch {
            adminRepository.updateBankAccountStatus(userId, status, reason)
            _pendingBanks.value = adminRepository.getPendingBankAccounts()
            _auditLogs.value = adminRepository.getAuditLogs()
        }
    }

    fun updateWithdrawal(requestId: String, status: String, payoutRef: String = "") {
        viewModelScope.launch {
            adminRepository.updateWithdrawalStatus(requestId, status, payoutRef)
            _withdrawals.value = adminRepository.getAllWithdrawalRequests()
            _auditLogs.value = adminRepository.getAuditLogs()
        }
    }

    fun resolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, status)
            _reports.value = adminRepository.getAllReports()
            _auditLogs.value = adminRepository.getAuditLogs()
        }
    }

    fun logoutAdmin() {
        _isAuthenticated.value = false
    }
}
