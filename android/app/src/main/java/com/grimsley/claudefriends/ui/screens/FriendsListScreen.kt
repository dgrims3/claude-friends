package com.grimsley.claudefriends.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grimsley.claudefriends.data.model.Friend
import com.grimsley.claudefriends.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onAddFriend: (name: String, path: String) -> Unit,
    onDeleteFriend: (Friend) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Claude Friends",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                        Text(
                            "${friends.count { it.active }} active",
                            fontSize = 12.sp,
                            color = MutedTeal,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = DimText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = ClaudeCream,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ClaudeOrange,
                contentColor = Charcoal,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Friend")
            }
        },
        containerColor = Charcoal,
    ) { padding ->
        if (friends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No agents yet", fontSize = 18.sp, color = DimText)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add a Claude Code agent",
                        fontSize = 14.sp,
                        color = DimText.copy(alpha = 0.6f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(friends, key = { it.id }) { friend ->
                    FriendRow(
                        friend = friend,
                        onClick = { onFriendClick(friend) },
                        onDelete = { onDeleteFriend(friend) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, path ->
                onAddFriend(name, path)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(friend.avatar ?: "#4ECDC4"))
    } catch (e: Exception) {
        MutedTeal
    }

    val statusColor by animateColorAsState(
        if (friend.active) MutedTeal else DimText.copy(alpha = 0.3f),
        label = "status",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar circle with initial
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = friend.name.first().uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = ClaudeCream,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = friend.path,
                    fontSize = 13.sp,
                    color = DimText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = DimText.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp),
        color = DimText.copy(alpha = 0.1f),
    )
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, path: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("~/projects/") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Add Agent", color = ClaudeCream)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("receipt-tracker") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Project path") },
                    placeholder = { Text("~/projects/my-agent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && path.isNotBlank()) onAdd(name, path) },
                colors = ButtonDefaults.textButtonColors(contentColor = ClaudeOrange),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DimText)
            }
        },
    )
}
