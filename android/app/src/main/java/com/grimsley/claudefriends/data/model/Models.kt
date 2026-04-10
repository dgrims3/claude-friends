package com.grimsley.claudefriends.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val id: String,
    val name: String,
    val path: String,
    val avatar: String? = null,  // hex color
    val active: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class ChatMessage(
    val type: String,           // "user", "assistant", "tool_use", "tool_result", "system", "error", "raw"
    val content: String? = null,
    val message: AssistantMessage? = null,
    val tool: String? = null,
    val timestamp: String = "",
) {
    /**
     * Extract display text from the various event formats
     * Claude Code stream-json has nested structures
     */
    fun displayText(): String = when (type) {
        "user" -> content ?: ""
        "assistant" -> message?.content?.firstOrNull { it.type == "text" }?.text
            ?: content ?: ""
        "tool_use" -> "Using tool: ${tool ?: "unknown"}"
        "tool_result" -> content ?: "Tool completed"
        "system" -> content ?: ""
        "error", "stderr" -> content ?: "Error"
        "raw" -> content ?: ""
        "session_ended" -> "Session ended"
        else -> content ?: "[${type}]"
    }

    fun isFromClaude(): Boolean = type in setOf("assistant", "tool_use", "tool_result", "system")
    fun isFromUser(): Boolean = type == "user"
    fun isMetadata(): Boolean = type in setOf("tool_use", "tool_result", "raw", "stderr")
}

@Serializable
data class AssistantMessage(
    val content: List<ContentBlock> = emptyList(),
)

@Serializable
data class ContentBlock(
    val type: String,     // "text", "tool_use", etc.
    val text: String? = null,
)

@Serializable
data class CreateFriendRequest(
    val name: String,
    val path: String,
    val avatar: String? = null,
)
