package com.grimsley.claudefriends.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grimsley.claudefriends.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentHost: String,
    currentPort: String,
    isFirstLaunch: Boolean,
    onSave: (host: String, port: String) -> Unit,
    onBack: (() -> Unit)?,
) {
    var host by remember { mutableStateOf(currentHost) }
    var port by remember { mutableStateOf(currentPort) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ClaudeCream,
                            )
                        }
                    }
                },
                title = {
                    Text(
                        if (isFirstLaunch) "Welcome to Claude Friends" else "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = ClaudeCream,
                ),
            )
        },
        containerColor = Charcoal,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            if (isFirstLaunch) {
                Text(
                    "Connect to your Claude Friends server",
                    fontSize = 16.sp,
                    color = DimText,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter the IP address and port of the machine running the Claude Friends server daemon.",
                    fontSize = 14.sp,
                    color = DimText.copy(alpha = 0.7f),
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(32.dp))
            }

            Text(
                "Server Host",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ClaudeCream,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                placeholder = { Text("192.168.1.100", color = DimText) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClaudeOrange,
                    unfocusedBorderColor = DimText.copy(alpha = 0.3f),
                    cursorColor = ClaudeOrange,
                    focusedTextColor = ClaudeCream,
                    unfocusedTextColor = ClaudeCream,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Server Port",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ClaudeCream,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                placeholder = { Text("3456", color = DimText) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClaudeOrange,
                    unfocusedBorderColor = DimText.copy(alpha = 0.3f),
                    cursorColor = ClaudeOrange,
                    focusedTextColor = ClaudeCream,
                    unfocusedTextColor = ClaudeCream,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (host.isNotBlank()) {
                        onSave(host.trim(), port.trim().ifBlank { "3456" })
                    }
                },
                enabled = host.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClaudeOrange,
                    contentColor = Charcoal,
                    disabledContainerColor = ClaudeOrange.copy(alpha = 0.3f),
                ),
            ) {
                Text(
                    if (isFirstLaunch) "Connect" else "Save",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            if (isFirstLaunch) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "You can change this later in Settings.",
                    fontSize = 12.sp,
                    color = DimText.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
