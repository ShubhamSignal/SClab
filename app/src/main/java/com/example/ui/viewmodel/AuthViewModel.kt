package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserData
import com.example.data.repository.AuthRepository
import com.example.data.repository.FounderCheckResult
import com.example.data.repository.FounderRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: FirebaseUser, val isProfileComplete: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val founderRepository: FounderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isFounderEligible = MutableStateFlow(false)
    val isFounderEligible: StateFlow<Boolean> = _isFounderEligible.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    checkProfileAndFounder(user)
                } else {
                    _uiState.value = AuthUiState.Idle
                }
            }
        }
    }

    private suspend fun checkProfileAndFounder(user: FirebaseUser) {
        val profile = userRepository.getUserData(user.uid)
        val isComplete = profile != null && profile.fullName.isNotBlank() && profile.dob.isNotBlank()
        val founderResult = founderRepository.checkFounderStatus(user.uid)
        _isFounderEligible.value = when (founderResult) {
            is FounderCheckResult.AvailableForFirstUser -> true
            is FounderCheckResult.CurrentUserIsFounder -> true
            else -> false
        }
        _uiState.value = AuthUiState.Authenticated(user, isComplete)
    }

    fun signInWithGoogle(activity: Activity) {
        _uiState.value = AuthUiState.Loading
        authRepository.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {
                // Handled by authStateListener
            },
            onError = { error ->
                _uiState.value = AuthUiState.Error(error)
            },
            onCancelled = {
                _uiState.value = AuthUiState.Idle
            }
        )
    }

    fun completeRegistration(
        fullName: String,
        gender: String,
        dob: String,
        termsAccepted: Boolean,
        onSuccess: (isFounder: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // Check if this is the first registered user to award Founder & Admin access
            val preCheck = founderRepository.checkFounderStatus(authRepository.currentUserId())
            val isFirstUser = preCheck is FounderCheckResult.AvailableForFirstUser || preCheck is FounderCheckResult.CurrentUserIsFounder
            val initialRole = if (isFirstUser) "admin" else "user"

            val regResult = authRepository.registerUserProfile(fullName, gender, dob, termsAccepted, role = initialRole)
            regResult.onSuccess { userData ->
                userRepository.syncPublicProfile(userData)
                // Check if user is eligible for First Registered User Lifetime Free Membership and Admin access!
                val founderStatus = founderRepository.checkFounderStatus(userData.userId)
                var wasAssignedFounder = false
                if (founderStatus is FounderCheckResult.AvailableForFirstUser) {
                    val claimResult = founderRepository.claimFounderFreeMembership(userData.userId, userData.email)
                    if (claimResult.isSuccess) {
                        wasAssignedFounder = true
                    }
                } else if (founderStatus is FounderCheckResult.CurrentUserIsFounder) {
                    wasAssignedFounder = true
                }
                _uiState.value = AuthUiState.Authenticated(authRepository.currentUser.value!!, true)
                onSuccess(wasAssignedFounder)
            }.onFailure { err ->
                _uiState.value = AuthUiState.Error(err.message ?: "Registration failed")
                onError(err.message ?: "Registration failed")
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        authRepository.signOut(viewModelScope, onComplete)
    }
}
