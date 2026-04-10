package com.grimsley.claudefriends.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grimsley.claudefriends.data.SettingsRepository
import com.grimsley.claudefriends.data.model.ChatMessage
import com.grimsley.claudefriends.data.model.Friend
import com.grimsley.claudefriends.data.network.ApiClient
import com.grimsley.claudefriends.data.network.ChatWebSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    val settings = SettingsRepository(application)
    private val api = ApiClient()

    val isConfigured: StateFlow<Boolean> = settings.isConfigured
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val serverHost: StateFlow<String> = settings.serverHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val serverPort: StateFlow<String> = settings.serverPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, "3456")

    // Friends list
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // Current chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSocket: ChatWebSocket? = null
    private var currentFriendId: String? = null

    // ──────────────────────────────────────────────
    // Settings
    // ──────────────────────────────────────────────

    fun saveSettings(host: String, port: String) {
        viewModelScope.launch {
            settings.saveServer(host, port)
            api.baseUrl = settings.getServerUrl()
        }
    }

    private suspend fun ensureApiUrl() {
        if (api.baseUrl.isBlank()) {
            api.baseUrl = settings.getServerUrl()
        }
    }

    // ──────────────────────────────────────────────
    // Friends management
    // ──────────────────────────────────────────────

    fun loadFriends() {
        viewModelScope.launch {
            try {
                ensureApiUrl()
                _friends.value = api.getFriends()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Can't reach server: ${e.message}"
            }
        }
    }

    fun addFriend(name: String, path: String) {
        viewModelScope.launch {
            try {
                ensureApiUrl()
                api.addFriend(name, path)
                loadFriends()
            } catch (e: Exception) {
                _error.value = "Failed to add: ${e.message}"
            }
        }
    }

    fun deleteFriend(friend: Friend) {
        viewModelScope.launch {
            try {
                ensureApiUrl()
                if (currentFriendId == friend.id) {
                    disconnectChat()
                }
                api.deleteFriend(friend.id)
                loadFriends()
            } catch (e: Exception) {
                _error.value = "Failed to delete: ${e.message}"
            }
        }
    }

    // ──────────────────────────────────────────────
    // Chat session
    // ──────────────────────────────────────────────

    fun connectToFriend(friend: Friend) {
        // Disconnect previous if different
        if (currentFriendId != friend.id) {
            disconnectChat()
        }

        currentFriendId = friend.id
        _messages.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                ensureApiUrl()

                // Start the session on the server (creates claude process)
                api.startSession(friend.id)

                // Load any existing history
                try {
                    val historyJson = api.getHistory(friend.id)
                    val history = json.decodeFromString<List<ChatMessage>>(historyJson)
                    _messages.value = history
                } catch (_: Exception) {}

                // Connect WebSocket
                val wsUrl = settings.getWsUrl()
                val socket = ChatWebSocket(friend.id, wsUrl)
                currentSocket = socket

                // Collect incoming messages
                launch {
                    socket.messages.collect { msg ->
                        _messages.value = _messages.value + msg

                        // Update connection state
                        when (msg.type) {
                            "system" -> if (msg.content == "Connected") {
                                _isConnected.value = true
                                _isLoading.value = false
                            }
                            "error" -> _isConnected.value = false
                            "session_ended" -> {
                                _isConnected.value = false
                                _isLoading.value = false
                            }
                            // When assistant responds, loading is done
                            "assistant" -> _isLoading.value = false
                        }
                    }
                }

                socket.connect()

            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String) {
        // Handle /quit command
        if (content.trim() == "/quit") {
            currentSocket?.sendQuit()
            return
        }

        _isLoading.value = true
        currentSocket?.sendMessage(content)
    }

    fun disconnectChat() {
        currentSocket?.disconnect()
        currentSocket = null
        currentFriendId = null
        _isConnected.value = false
        _messages.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        // Don't kill the session — just disconnect the socket
        // The session lives on the server
        currentSocket?.disconnect()
        super.onCleared()
    }
}
