package com.grimsley.claudefriends.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Dark theme — terminal-inspired but warm
val Charcoal = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val ClaudeOrange = Color(0xFFE8956A)
val ClaudeCream = Color(0xFFF5E6D3)
val MutedTeal = Color(0xFF4ECDC4)
val SoftPurple = Color(0xFFA78BFA)
val DimText = Color(0xFF8B8FA3)
val UserBubble = Color(0xFF2D3250)
val ClaudeBubble = Color(0xFF1E293B)
val ErrorRed = Color(0xFFEF4444)
val ToolGray = Color(0xFF374151)

private val DarkColors = darkColorScheme(
    primary = ClaudeOrange,
    onPrimary = Charcoal,
    secondary = MutedTeal,
    tertiary = SoftPurple,
    background = Charcoal,
    surface = DarkSurface,
    onBackground = ClaudeCream,
    onSurface = ClaudeCream,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun ClaudeFriendsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
