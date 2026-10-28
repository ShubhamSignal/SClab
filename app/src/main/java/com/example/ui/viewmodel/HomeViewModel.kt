package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Membership
import com.example.data.model.PublicProfile
import com.example.data.model.UserData
import com.example.data.repository.MembershipRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    private val _currentUserData = MutableStateFlow<UserData?>(null)
    val currentUserData: StateFlow<UserData?> = _currentUserData.asStateFlow()

    private val _activeMembership = MutableStateFlow<Membership?>(null)
    val activeMembership: StateFlow<Membership?> = _activeMembership.asStateFlow()

    private val _discoverProfiles = MutableStateFlow<List<PublicProfile>>(emptyList())
    val discoverProfiles: StateFlow<List<PublicProfile>> = _discoverProfiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepository.getUserData()
                _currentUserData.value = user
                if (user != null) {
                    val membership = membershipRepository.getMembership(user.userId)
                    _activeMembership.value = membership
                    val profiles = userRepository.discoverPeople(user.gender)
                    _discoverProfiles.value = profiles
                }
            } catch (e: Exception) {
                // Ignore or log
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun blockUser(targetUserId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.blockUser(targetUserId)
            _discoverProfiles.value = _discoverProfiles.value.filter { it.userId != targetUserId }
            onSuccess()
        }
    }

    fun reportUser(targetUserId: String, reason: String, details: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.reportUser(targetUserId, reason, details)
            onSuccess()
        }
    }
}
