package com.grimsley.claudefriends.data.network

import com.grimsley.claudefriends.data.model.CreateFriendRequest
import com.grimsley.claudefriends.data.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient(var baseUrl: String = "") {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getFriends(): List<Friend> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/friends")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"
        json.decodeFromString<List<Friend>>(body)
    }

    suspend fun addFriend(name: String, path: String): Friend = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(CreateFriendRequest(name, path))
        val request = Request.Builder()
            .url("$baseUrl/friends")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<Friend>(body)
    }

    suspend fun deleteFriend(id: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/friends/$id")
            .delete()
            .build()
        client.newCall(request).execute()
    }

    suspend fun startSession(id: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/sessions/$id/start")
            .post("".toRequestBody())
            .build()
        client.newCall(request).execute()
    }

    suspend fun getHistory(id: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/sessions/$id/history")
            .get()
            .build()
        val response = client.newCall(request).execute()
        response.body?.string() ?: "[]"
    }
}
