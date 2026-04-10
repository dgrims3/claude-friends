package com.grimsley.claudefriends.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val serverHostKey = stringPreferencesKey("server_host")
    private val serverPortKey = stringPreferencesKey("server_port")

    val serverHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverHostKey] ?: ""
    }

    val serverPort: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverPortKey] ?: "3456"
    }

    val isConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[serverHostKey].isNullOrBlank()
    }

    suspend fun saveServer(host: String, port: String) {
        context.dataStore.edit { prefs ->
            prefs[serverHostKey] = host.trim()
            prefs[serverPortKey] = port.trim().ifBlank { "3456" }
        }
    }

    suspend fun getServerUrl(): String {
        val host = context.dataStore.data.first()[serverHostKey] ?: ""
        val port = context.dataStore.data.first()[serverPortKey] ?: "3456"
        return "http://$host:$port"
    }

    suspend fun getWsUrl(): String {
        val host = context.dataStore.data.first()[serverHostKey] ?: ""
        val port = context.dataStore.data.first()[serverPortKey] ?: "3456"
        return "ws://$host:$port"
    }
}
