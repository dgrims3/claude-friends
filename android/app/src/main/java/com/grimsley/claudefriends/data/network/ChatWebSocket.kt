package com.grimsley.claudefriends.data.network

import com.grimsley.claudefriends.data.model.ChatMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class ChatWebSocket(private val friendId: String, private val wsUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)  // no timeout for WS
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val messageChannel = Channel<ChatMessage>(Channel.BUFFERED)

    val messages: Flow<ChatMessage> = messageChannel.receiveAsFlow()

    private var _isConnected = false
    val isConnected get() = _isConnected

    fun connect() {
        val url = "$wsUrl/sessions/$friendId/ws"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected = true
                messageChannel.trySend(
                    ChatMessage(type = "system", content = "Connected")
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<ChatMessage>(text)
                    messageChannel.trySend(msg)
                } catch (e: Exception) {
                    messageChannel.trySend(
                        ChatMessage(type = "raw", content = text)
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected = false
                messageChannel.trySend(
                    ChatMessage(type = "error", content = "Connection lost: ${t.message}")
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected = false
                messageChannel.trySend(
                    ChatMessage(type = "system", content = "Disconnected")
                )
            }
        })
    }

    fun sendMessage(content: String) {
        val escaped = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), kotlinx.serialization.json.JsonPrimitive(content))
        val payload = """{"type":"message","content":$escaped}"""
        webSocket?.send(payload)
    }

    fun sendQuit() {
        val payload = """{"type":"quit"}"""
        webSocket?.send(payload)
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
        _isConnected = false
    }
}
