package com.vwatek.apply.data.repository

import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Shared utilities for API repositories
 */

// JSON parser for all API repositories
internal val apiJson = Json { 
    ignoreUnknownKeys = true
    isLenient = true
}

// Cached session DTOs (shared across all repositories)
@Serializable
internal data class ApiCachedSession(
    val user: ApiCachedUser,
    val token: String,
    val expiresAt: String
)

@Serializable
internal data class ApiCachedUser(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

/**
 * Get the current user ID from cached session
 */
internal fun getCurrentUserId(): String? {
    val sessionJson = localStorage.getItem("vwatek_auth_session") ?: return null
    return try {
        val session = apiJson.decodeFromString<ApiCachedSession>(sessionJson)
        session.user.id
    } catch (e: Exception) {
        console.error("Error getting user ID: ${e.message}")
        null
    }
}

/**
 * Get the current JWT auth token from cached session
 */
internal fun getCurrentAuthToken(): String? {
    val sessionJson = localStorage.getItem("vwatek_auth_session") ?: return null
    return try {
        val session = apiJson.decodeFromString<ApiCachedSession>(sessionJson)
        session.token
    } catch (e: Exception) {
        console.error("Error getting auth token: ${e.message}")
        null
    }
}

/**
 * Build standard headers with Bearer auth token
 */
internal fun authHeaders(extraHeaders: Map<String, String> = emptyMap()): dynamic {
    val token = getCurrentAuthToken()
    val headers = js("{}")
    headers["Content-Type"] = "application/json"
    if (token != null) {
        headers["Authorization"] = "Bearer $token"
    }
    for ((key, value) in extraHeaders) {
        headers[key] = value
    }
    return headers
}

/**
 * Get the API base URL based on current hostname
 * Uses centralized ApiConfig (Canadian region in production)
 */
internal fun getApiBaseUrl(): String {
    val hostname = kotlinx.browser.window.location.hostname
    return when {
        hostname == "localhost" || hostname == "127.0.0.1" -> "http://localhost:8090"
        else -> com.vwatek.apply.data.api.ApiConfig.baseUrl
    }
}

/**
 * Launch a coroutine for async operations
 */
internal fun launchAsync(block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch(Dispatchers.Main, block = block)
}
