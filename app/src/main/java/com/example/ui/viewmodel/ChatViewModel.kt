package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val currentUserId: String? = null
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _currentMessages.value = messages
            }
        }
    }

    fun startConversationWith(
        targetUserId: String,
        targetUserName: String,
        currentUserName: String,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            val convId = chatRepository.getOrCreateConversation(targetUserId, targetUserName, currentUserName)
            selectConversation(convId)
            onReady(convId)
        }
    }

    fun sendMessage(text: String, senderName: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(convId, text, senderName)
        }
    }
}
