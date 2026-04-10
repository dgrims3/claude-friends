package com.grimsley.claudefriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grimsley.claudefriends.data.model.Friend
import com.grimsley.claudefriends.ui.MainViewModel
import com.grimsley.claudefriends.ui.screens.ChatScreen
import com.grimsley.claudefriends.ui.screens.FriendsListScreen
import com.grimsley.claudefriends.ui.screens.SettingsScreen
import com.grimsley.claudefriends.ui.theme.ClaudeFriendsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClaudeFriendsTheme {
                val viewModel: MainViewModel = viewModel()
                val navController = rememberNavController()

                val isConfigured by viewModel.isConfigured.collectAsState()
                val serverHost by viewModel.serverHost.collectAsState()
                val serverPort by viewModel.serverPort.collectAsState()
                val friends by viewModel.friends.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val isConnected by viewModel.isConnected.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val error by viewModel.error.collectAsState()

                // Track selected friend for navigation
                var selectedFriend by remember { mutableStateOf<Friend?>(null) }

                // Show errors as snackbar
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(error) {
                    error?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearError()
                    }
                }

                // Determine start destination based on configuration
                val startDestination = if (isConfigured) "friends" else "settings"

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable("settings") {
                            SettingsScreen(
                                currentHost = serverHost,
                                currentPort = serverPort,
                                isFirstLaunch = !isConfigured,
                                onSave = { host, port ->
                                    viewModel.saveSettings(host, port)
                                    navController.navigate("friends") {
                                        popUpTo("settings") { inclusive = true }
                                    }
                                },
                                onBack = if (isConfigured) {
                                    { navController.popBackStack() }
                                } else null,
                            )
                        }

                        composable("friends") {
                            // Load friends when we arrive at this screen
                            LaunchedEffect(Unit) {
                                viewModel.loadFriends()
                            }

                            FriendsListScreen(
                                friends = friends,
                                onFriendClick = { friend ->
                                    selectedFriend = friend
                                    viewModel.connectToFriend(friend)
                                    navController.navigate("chat")
                                },
                                onAddFriend = { name, path ->
                                    viewModel.addFriend(name, path)
                                },
                                onDeleteFriend = { friend ->
                                    viewModel.deleteFriend(friend)
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                            )
                        }

                        composable("chat") {
                            selectedFriend?.let { friend ->
                                ChatScreen(
                                    friend = friend,
                                    messages = messages,
                                    isConnected = isConnected,
                                    isLoading = isLoading,
                                    onSend = { viewModel.sendMessage(it) },
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
