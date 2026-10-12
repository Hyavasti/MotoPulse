package com.example.motorapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.ChatMessage
import com.example.motorapp.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ChatViewModel(
    val roomId: String,
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentUserId = repository.currentUserId

    init {
        fastLoadAndObserve()
    }

    private fun fastLoadAndObserve() {
        // Step 1: Immediate HTTP snapshot fetch (~150ms instant render)
        viewModelScope.launch {
            repository.getRecentMessages(roomId, limit = 50)
                .onSuccess { initialMessages ->
                    _uiState.update { current ->
                        // Don't overwrite if realtime stream already arrived
                        if (current.messages.isEmpty()) {
                            current.copy(
                                messages = initialMessages,
                                isLoading = false,
                                errorMessage = null
                            )
                        } else {
                            current.copy(isLoading = false)
                        }
                    }
                }
                .onFailure {
                    // Fail silently to let the realtime stream take over
                    _uiState.update { it.copy(isLoading = false) }
                }
        }

        // Step 2: Realtime WebSocket stream for ongoing incoming messages
        viewModelScope.launch {
            repository.getMessagesStream(roomId)
                .catch { ex ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = ex.localizedMessage ?: "Failed to connect to chat"
                        )
                    }
                }
                .collect { incomingMessages ->
                    _uiState.update { current ->
                        // Merge stream messages and preserve pending optimistic messages
                        val incomingIds = incomingMessages.map { it.id }.toSet()
                        val pendingOptimistic = current.messages.filter { localMsg ->
                            localMsg.id !in incomingIds && incomingMessages.none {
                                it.senderId == localMsg.senderId && it.content == localMsg.content
                            }
                        }

                        current.copy(
                            messages = incomingMessages + pendingOptimistic,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val uid = currentUserId ?: run {
            _uiState.update { it.copy(errorMessage = "User not authenticated") }
            return
        }

        // 1. OPTIMISTIC UPDATE: Instant display in 0 ms
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val optimisticMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderId = uid,
            content = trimmed,
            createdAt = nowIso
        )

        _uiState.update { current ->
            current.copy(messages = current.messages + optimisticMessage)
        }

        // 2. Network insert in background
        viewModelScope.launch {
            repository.sendMessage(roomId, trimmed)
                .onFailure { ex ->
                    // Revert the failed optimistic message and show error
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages.filter { it.id != optimisticMessage.id },
                            errorMessage = ex.localizedMessage ?: "Could not send message"
                        )
                    }
                }
        }
    }
}

class ChatViewModelFactory(
    val roomId: String,
    private val repository: ChatRepository = ChatRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(roomId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}