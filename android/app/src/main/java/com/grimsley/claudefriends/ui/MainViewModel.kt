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

    suspend fun saveSettings(host: String, port: String) {
        settings.saveServer(host, port)
        api.baseUrl = settings.getServerUrl()
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
                android.util.Log.d("ClaudeFriends", "baseUrl=${api.baseUrl}")

                // Start the session on the server
                android.util.Log.d("ClaudeFriends", "Starting session for ${friend.id}...")
                val startResp = api.startSession(friend.id)
                android.util.Log.d("ClaudeFriends", "startSession response: $startResp")

                // Load any existing history
                try {
                    val historyJson = api.getHistory(friend.id)
                    val history = json.decodeFromString<List<ChatMessage>>(historyJson)
                    _messages.value = history
                } catch (_: Exception) {}

                // Connect WebSocket
                val wsUrl = settings.getWsUrl()
                android.util.Log.d("ClaudeFriends", "Connecting WebSocket to $wsUrl")
                val socket = ChatWebSocket(friend.id, wsUrl)
                currentSocket = socket

                // Collect incoming messages
                launch {
                    socket.messages.collect { msg ->
                        // Update connection/loading state from control events
                        when (msg.type) {
                            "system" -> {
                                if (msg.content == "Connected") {
                                    _isConnected.value = true
                                    _isLoading.value = false
                                }
                                // Don't display system init or "Connected" messages
                            }
                            "error" -> {
                                _isConnected.value = false
                                _messages.value = _messages.value + msg
                            }
                            "session_ended" -> {
                                _isConnected.value = false
                                _isLoading.value = false
                                _messages.value = _messages.value + msg
                            }
                            "assistant" -> {
                                _isLoading.value = false
                                _messages.value = _messages.value + msg
                            }
                            "user" -> {
                                _messages.value = _messages.value + msg
                            }
                            "tool_use", "tool_result" -> {
                                _messages.value = _messages.value + msg
                            }
                            "status" -> {
                                // "thinking" / "ready" — drive loading indicator, don't display
                                _isLoading.value = msg.content == "thinking"
                            }
                            // Suppress: stderr, raw, result, rate_limit_event, system init
                        }
                    }
                }

                socket.connect()

            } catch (e: Exception) {
                android.util.Log.e("ClaudeFriends", "connectToFriend failed", e)
                _error.value = "Connection failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String): Boolean {
        val trimmed = content.trim()
        // Handle exit/quit commands — clear chat and signal caller to navigate back
        if (trimmed == "/quit" || trimmed == "/exit") {
            currentSocket?.sendQuit()
            disconnectChat()
            return true // indicates "go back"
        }

        _isLoading.value = true
        currentSocket?.sendMessage(content)
        return false
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
